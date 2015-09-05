/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.display;

import android.util.Log;
import android.util.Pair;

import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import static com.nextgis.maplib.util.Constants.DRAWING_SEPARATE_THREADS;
import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;
import static com.nextgis.maplib.util.Constants.KEEP_ALIVE_TIME;
import static com.nextgis.maplib.util.Constants.KEEP_ALIVE_TIME_UNIT;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.Constants.TERMINATE_TIME;


public class SimpleFeatureRenderer
        extends Renderer
{

    protected Style              mStyle;
    protected ThreadPoolExecutor mDrawThreadPool;
    //protected final Object lock = new Object();

    public static final String JSON_STYLE_KEY = "style";
    protected static final int GEOMETRY_PER_TASK = 50;


    public SimpleFeatureRenderer(Layer layer)
    {
        super(layer);
        mStyle = null;
    }


    public SimpleFeatureRenderer(
            Layer layer,
            Style style)
    {
        super(layer);
        mStyle = style;
    }

    @Override
    public void runDraw(final GISDisplay display)
    {
        long startTime;
        if(Constants.DEBUG_MODE) {
            startTime = System.currentTimeMillis();
        }

        if (null == mStyle) {
            Log.d(TAG, "mStyle == null");
            return;
        }
        final double zoom = display.getZoomLevel();

        GeoEnvelope env = display.getBounds();
        final VectorLayer vectorLayer = (VectorLayer) mLayer;

        //GeoEnvelope layerEnv = vectorLayer.getExtents();
        //if (null == layerEnv || !env.intersects(layerEnv)) {
        //    return;
        //}

        int decimalZoom = (int) zoom;
        if(decimalZoom % 2 != 0)
            decimalZoom++;

        List<Long> featureIds = vectorLayer.query(env);

        cancelDraw();

        int threadCount = DRAWING_SEPARATE_THREADS;
        mDrawThreadPool = new ThreadPoolExecutor(
                threadCount, threadCount, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<Runnable>(), new RejectedExecutionHandler()
        {
            @Override
            public void rejectedExecution(
                    Runnable r,
                    ThreadPoolExecutor executor)
            {
                try {
                    executor.getQueue().put(r);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        });

        if(Constants.DEBUG_MODE) {
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;

            Log.d(TAG, "Vector layer " + mLayer.getName() + " prepare time: " + elapsedTime);
        }

        // http://developer.android.com/reference/java/util/concurrent/ExecutorCompletionService.html
        int tilesSize = featureIds.size() / GEOMETRY_PER_TASK + 1;
        List<Future> futures = new ArrayList<>(tilesSize);

        final int finalDecimalZoom = decimalZoom;
        int counter = 0;
        for (int i = 0; i < featureIds.size(); i += GEOMETRY_PER_TASK) {

            DrawTask task = new DrawTask(finalDecimalZoom, vectorLayer, display);

            for(int j = 0; j < GEOMETRY_PER_TASK; j++) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                counter++;
                if (counter >= featureIds.size())
                    break;

                final long featureId = featureIds.get(counter);

                task.addTaskData(featureId);
            }

            futures.add(mDrawThreadPool.submit(task));
            vectorLayer.onDrawFinished(vectorLayer.getId(), 0.01f);
        }

        // wait for draw ending
        int nStep = futures.size() / Constants.DRAW_NOTIFY_STEP_PERCENT;
        if(nStep == 0)
            nStep = 1;
        for (int i = 0, futuresSize = futures.size(); i < futuresSize; i++) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            try {
                Future future = futures.get(i);
                future.get(); // wait for task ending

                float percent = (float) i / futuresSize;
                if(i % nStep == 0) //0..10..20..30..40..50..60..70..80..90..100
                    vectorLayer.onDrawFinished(vectorLayer.getId(), percent);

                //Log.d(TAG, "TMS percent: " + percent + " complete: " + i +
                //       " tiles count: " + tilesSize + " layer: " + mLayer.getName());

            } catch (CancellationException | InterruptedException e) {
                //e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        vectorLayer.onDrawFinished(vectorLayer.getId(), 1.0f);

        if(Constants.DEBUG_MODE) {
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;

            Log.d(TAG, "Vector layer " + mLayer.getName() + " exec time: " + elapsedTime);
        }
    }


    /**
     * If subclass's getStyle(long featureId) changes style params then must be so in the method
     * body:
     * <pre> {@code
     * try {
     *     Style styleClone = mStyle.clone();
     *     // changing styleClone params
     *     return styleClone;
     * } catch (CloneNotSupportedException e) {
     *     e.printStackTrace();
     *     return mStyle;
     *     // or treat this situation instead
     * }
     * } </pre>
     * or
     * <pre> {@code
     * Style style = new Style(); // or from subclass of Style
     * // changing style params
     * return style;
     * } </pre>
     */
    protected Style getStyle(long featureId)
    {
        return mStyle;
    }


    @Override
    public void cancelDraw()
    {
        if (mDrawThreadPool != null) {
            //synchronized (lock) {
                mDrawThreadPool.shutdownNow();
            //}
            try {
                mDrawThreadPool.awaitTermination(TERMINATE_TIME, KEEP_ALIVE_TIME_UNIT);
                //mDrawThreadPool.purge();
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }
    }


    public Style getStyle()
    {
        return mStyle;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootJsonObject = new JSONObject();
        rootJsonObject.put(JSON_NAME_KEY, "SimpleFeatureRenderer");

        if (null != mStyle) {
            rootJsonObject.put(JSON_STYLE_KEY, mStyle.toJSON());
        }

        return rootJsonObject;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        JSONObject styleJsonObject = jsonObject.getJSONObject(JSON_STYLE_KEY);
        String styleName = styleJsonObject.getString(JSON_NAME_KEY);
        switch (styleName) {
            case "SimpleMarkerStyle":
                mStyle = new SimpleMarkerStyle();
                break;
            case "SimpleTextMarkerStyle":
                mStyle = new SimpleTextMarkerStyle();
                break;
            case "SimpleLineStyle":
                mStyle = new SimpleLineStyle();
                break;
            case "SimpleTextLineStyle":
                mStyle = new SimpleTextLineStyle();
                break;
            case "SimplePolygonStyle":
                mStyle = new SimplePolygonStyle();
                break;
            case "SimpleTiledPolygonStyle":
                mStyle = new SimpleTiledPolygonStyle();
                break;
            default:
                throw new JSONException("Unknown style type: " + styleName);
        }
        mStyle.fromJSON(styleJsonObject);
    }

    protected class DrawTask implements Runnable {
        protected final GISDisplay mDisplay;
        protected final int mZoom;
        protected final VectorLayer mLayer;
        protected final List<Long> mFeatureIds = new ArrayList<>(GEOMETRY_PER_TASK);

        public DrawTask(final int zoom, final VectorLayer layer, final GISDisplay display) {
            mDisplay = display;
            mZoom = zoom;
            mLayer = layer;
        }

        public void addTaskData(final Long featureId){
            mFeatureIds.add(featureId);
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(
                    Constants.DEFAULT_DRAW_THREAD_PRIORITY);

            for(Long id : mFeatureIds) {
                final GeoGeometry geometry = mLayer.getGeometryForId(id, mZoom);
                if (geometry != null) {
                    final Style style = getStyle(id);
                    style.onDraw(geometry, mDisplay);
                }
            }
        }
    }
}
