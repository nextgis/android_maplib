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

import android.graphics.Paint;
import android.util.Log;
import android.util.Pair;

import com.nextgis.maplib.api.IGeometryCacheItem;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;

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

    protected Style              mStyle, mPointStyle, mLineStyle;
    protected ThreadPoolExecutor mDrawThreadPool;
    //protected final Object lock = new Object();

    public static final String JSON_STYLE_KEY = "style";
    protected static final int GEOMETRY_PER_TASK = 5;


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
        initStyles();
    }

    protected void initStyles(){
        final VectorLayer vectorLayer = (VectorLayer) mLayer;
        if(vectorLayer.getGeometryType() == GeoConstants.GTPolygon ||
                vectorLayer.getGeometryType() == GeoConstants.GTMultiPolygon){
            float width = 1.0f;
            if(mStyle instanceof SimplePolygonStyle){
                SimplePolygonStyle polyStyle = (SimplePolygonStyle) mStyle;
                width = polyStyle.mWidth * 2;
                if(width < 1)
                    width = 3;
            }
            mPointStyle = new SimpleMarkerStyle(mStyle.getColor(), mStyle.getColor(), width,
                    SimpleMarkerStyle.MarkerStylePoint);
            SimpleLineStyle lineStyle = new SimpleLineStyle(mStyle.getColor(), mStyle.getColor(),
                    SimpleLineStyle.LineStyleSolid);
            lineStyle.mWidth = width;
            lineStyle.mStrokeCap = Paint.Cap.ROUND;

            mLineStyle = lineStyle;
        }
        else if(vectorLayer.getGeometryType() == GeoConstants.GTLineString ||
                vectorLayer.getGeometryType() == GeoConstants.GTMultiLineString){
            float width = 1.0f;
            if(mStyle instanceof SimpleLineStyle){
                SimpleLineStyle lineStyle = (SimpleLineStyle) mStyle;
                width = lineStyle.mWidth * 2;
                if(width < 1)
                    width = 3;
            }
            mPointStyle = new SimpleMarkerStyle(mStyle.getColor(), mStyle.getColor(), width,
                    SimpleMarkerStyle.MarkerStylePoint);
        }

    }

    @Override
    public void runDraw(final GISDisplay display)
    {
        if (null == mStyle) {
            Log.d(TAG, "mStyle == null");
            return;
        }


        GeoEnvelope env = display.getBounds();
        final VectorLayer vectorLayer = (VectorLayer) mLayer;
        GeoEnvelope layerEnv = vectorLayer.getExtents();

        if (null == layerEnv || !env.intersects(layerEnv)) {
            return;
        }

        cancelDraw();

        //add drawing routine
        final List<IGeometryCacheItem> cache = vectorLayer.query(env);

        if (cache.size() == 0) {
            return;
        }

        int threadCount = DRAWING_SEPARATE_THREADS;
        //synchronized (lock) {
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
        //}

        long startTime = System.currentTimeMillis();

        // http://developer.android.com/reference/java/util/concurrent/ExecutorCompletionService.html
        int cacheSize = cache.size() / GEOMETRY_PER_TASK;
        List<Future> futures = new ArrayList<>(cacheSize);

        int counter = 0;
        double pointArea = display.screenToMap(new GeoEnvelope(0, 4, 0, 4)).getArea(); // 4 x 4 screen pixels is point
        double lineArea =  display.screenToMap(new GeoEnvelope(0, 8, 0, 8)).getArea(); // 8 x 8 screen pixels is line

        for (int i = 0; i < cache.size(); i += GEOMETRY_PER_TASK) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            DrawTask task = new DrawTask(display);

            for(int j = 0; j < GEOMETRY_PER_TASK; j++){
                counter++;

                if(counter >= cache.size())
                    break;

                final IGeometryCacheItem item = cache.get(counter);

                //check if geometry can be represent as primitive point or line
                final GeoEnvelope envelope = item.getEnvelope();
                double area = envelope.getArea();
                if(pointArea > area){
                    GeoPoint pt = envelope.getCenter();
                    task.addTaskData(pt, mPointStyle);
                }
                else if(lineArea > area){
                    GeoLineString line = new GeoLineString();
                    line.add(new GeoPoint(envelope.getMinX(), envelope.getMinY()));
                    line.add(new GeoPoint(envelope.getMaxX(), envelope.getMaxY()));
                    task.addTaskData(line, mLineStyle);
                }
                else {

                    final Style style = getStyle(item.getFeatureId());
                    task.addTaskData(vectorLayer.getGeometryForId(item.getFeatureId()), style);
                }

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
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

                //Log.d(TAG, "Vector percent: " + percent + " complete: " + i + " geom count: " +
                //        cacheSize + " layer :" + mLayer.getName());

            } catch (CancellationException | InterruptedException e) {
                //e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        vectorLayer.onDrawFinished(vectorLayer.getId(), 1.0f);

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        Log.d(TAG, "Vector layer exec time: " + elapsedTime);
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
                mStyle.fromJSON(styleJsonObject);
                break;
            case "SimpleTextMarkerStyle":
                mStyle = new SimpleTextMarkerStyle();
                mStyle.fromJSON(styleJsonObject);
                break;
            case "SimpleLineStyle":
                mStyle = new SimpleLineStyle();
                mStyle.fromJSON(styleJsonObject);
                break;
            case "SimpleTextLineStyle":
                mStyle = new SimpleTextLineStyle();
                mStyle.fromJSON(styleJsonObject);
                break;
            case "SimplePolygonStyle":
                mStyle = new SimplePolygonStyle();
                mStyle.fromJSON(styleJsonObject);
                break;
            default:
                throw new JSONException("Unknown style type: " + styleName);
        }

        initStyles();
    }

    protected class DrawTask implements Runnable {
        protected final GISDisplay mDisplay;
        protected final List<Pair<GeoGeometry, Style>> mTaskData = new ArrayList<>(GEOMETRY_PER_TASK);

        public DrawTask(GISDisplay display) {
            mDisplay = display;
        }

        public void addTaskData(final GeoGeometry geometry, final Style style){
            mTaskData.add(new Pair<>(geometry, style));
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(
                    Constants.DEFAULT_DRAW_THREAD_PRIORITY);

            for(Pair<GeoGeometry, Style> p : mTaskData) {
                if (null != p.first) {
                    p.second.onDraw(p.first, mDisplay);
                }
            }
        }
    }
}
