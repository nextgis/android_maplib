/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.display;

import android.graphics.Bitmap;
import com.nextgis.maplib.datasource.GeoEnvelope;

import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.TMSLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.VectorCacheItem;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import static com.nextgis.maplib.util.Constants.*;

public class SimpleFeatureRenderer extends Renderer{

    protected Style              mStyle;
    protected ThreadPoolExecutor mDrawThreadPool;

    public static final String JSON_STYLE_KEY = "style";


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
        GeoEnvelope env = display.getBounds();
        final VectorLayer vectorLayer = (VectorLayer) mLayer;
        GeoEnvelope layerEnv = vectorLayer.getExtents();

        if (null == env || null == layerEnv || !env.intersects(layerEnv)) {
            vectorLayer.onDrawFinished(vectorLayer.getId(), 1);
            return;
        }

        //add drawing routine
        final List<VectorCacheItem> cache = vectorLayer.getVectorCache();

        //TODO: more than one thread for drawing (divide the geometry cache array on several parts)
        //TODO: think about display syncronization in drawing points/lines/polygons

        mDrawThreadPool = new ThreadPoolExecutor(1, 1, KEEP_ALIVE_TIME,
                                                 KEEP_ALIVE_TIME_UNIT, new LinkedBlockingQueue<Runnable>(),
                                                 new RejectedExecutionHandler()
                                                 {
                                                     @Override
                                                     public void rejectedExecution(
                                                             Runnable r,
                                                             ThreadPoolExecutor executor)
                                                     {
                                                         try {
                                                             executor.getQueue().put(r);
                                                         } catch (InterruptedException e) {
                                                             e.printStackTrace();
                                                             //throw new RuntimeException("Interrupted while submitting task", e);
                                                         }
                                                     }
                                                 });

        if(cache.size() == 0){
            vectorLayer.onDrawFinished(vectorLayer.getId(), 1);
            return;
        }

        mDrawThreadPool.execute(new Runnable()
        {
            @Override
            public void run()
            {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                for(int i = 0; i < cache.size(); i++){
                    VectorCacheItem item = cache.get(i);

                    GeoGeometry geometry = item.getGeoGeometry();
                    mStyle.onDraw(geometry, display);

                    synchronized (mLayer) {
                        float percent = (float) i / cache.size();
                        vectorLayer.onDrawFinished(vectorLayer.getId(), percent);
                    }


                }

                vectorLayer.onDrawFinished(vectorLayer.getId(), 1);
                //Log.d(TAG, "percent: " + percent + " complete: " + mDrawThreadPool.getCompletedTaskCount() + " task count: " + mDrawThreadPool.getTaskCount());
            }
        });
    }


    @Override
    public void cancelDraw(){
        if (mDrawThreadPool != null) {
            mDrawThreadPool.shutdownNow();
        }
    }

    public Style getStyle() {
        return mStyle;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootJsonObject = new JSONObject();
        rootJsonObject.put(JSON_STYLE_KEY, mStyle.toJSON());
        return rootJsonObject;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        JSONObject styleJsonObject = jsonObject.getJSONObject(JSON_STYLE_KEY);
        String styleName = styleJsonObject.getString(JSON_NAME_KEY);
        switch (styleName)
        {
            case "SimpleMarkerStyle":
                mStyle = new SimpleMarkerStyle();
                mStyle.fromJSON(styleJsonObject);
                break;
            case "SimpleLineStyle":
                mStyle = new SimpleLineStyle();
                mStyle.fromJSON(styleJsonObject);
                break;
        }
    }
}
