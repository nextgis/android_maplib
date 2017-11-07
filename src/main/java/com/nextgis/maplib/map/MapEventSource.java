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

package com.nextgis.maplib.map;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.MapEventListener;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.util.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.TAG;


public class MapEventSource
        extends MapContentProviderHelper
{
    protected static final String BUNDLE_ID_KEY             = "id";
    protected static final String BUNDLE_TYPE_KEY           = "type";
    protected static final String BUNDLE_DONE_KEY           = "done";
    protected static final String BUNDLE_ZOOM_KEY           = "zoom";
    protected static final String BUNDLE_X_KEY              = "x";
    protected static final String BUNDLE_Y_KEY              = "y";
    protected final static int    EVENT_onLayerAdded        = 1;
    protected final static int    EVENT_onLayerDeleted      = 2;
    protected final static int    EVENT_onLayerChanged      = 3;
    protected final static int    EVENT_onExtentChanged     = 4;
    protected final static int    EVENT_onLayersReordered   = 5;
    protected final static int    EVENT_onLayerDrawFinished = 6;
    protected final static int    EVENT_onLayerDrawStarted  = 7;
    protected        List<MapEventListener> mListeners;
    protected static Handler                mHandler;
    protected        boolean                mFreeze;

    protected Map<Integer, Long> mLastMessages;
    //skip event timeout ms
    public static final int SKIP_TIMEOUT = 450;

    public MapEventSource(
            Context context,
            File mapPath,
            LayerFactory layerFactory)
    {
        super(context, mapPath, layerFactory);
        mListeners = new ArrayList<>();
        mFreeze = false;
        mLastMessages = new HashMap<>();

        createHandler();
    }


    /**
     * Add new listener for map events
     *
     * @param listener
     *         A listener class implements MapEventListener adding to listeners array
     */
    public void addListener(MapEventListener listener)
    {
        if (mListeners != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }


    /**
     * Remove listener from listeners
     *
     * @param listener
     *         A listener class implements MapEventListener removing from listeners array
     */
    public void removeListener(MapEventListener listener)
    {
        if (mListeners != null) {
            mListeners.remove(listener);
        }
    }


    @Override
    public void onDrawFinished(
            int id,
            float percent)
    {
        super.onDrawFinished(id, percent);
        onLayerDrawFinished(id, percent);
    }


    /**
     * Send layer added event to all listeners
     *
     * @param layer
     *         A new layer
     */
    @Override
    protected void onLayerAdded(ILayer layer)
    {
        super.onLayerAdded(layer);
        if (mListeners == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_ID_KEY, layer.getId());
        bundle.putInt(BUNDLE_TYPE_KEY, EVENT_onLayerAdded);

        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    /**
     * Send layer changed event to all listeners
     *
     * @param layer
     *         A changed layer
     */
    @Override
    protected void onLayerChanged(ILayer layer)
    {
        super.onLayerChanged(layer);
        if (mListeners == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_ID_KEY, layer.getId());
        bundle.putInt(BUNDLE_TYPE_KEY, EVENT_onLayerChanged);

        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    /**
     * Send layer delete event to all listeners
     *
     * @param id
     *         A deleted layer identificator
     */
    @Override
    protected void onLayerDeleted(int id)
    {
        super.onLayerDeleted(id);
        if (mListeners == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_ID_KEY, id);
        bundle.putInt(BUNDLE_TYPE_KEY, EVENT_onLayerDeleted);

        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    /**
     * Send extent change event to all listeners
     *
     * @param zoom
     *         A zoom level
     * @param center
     *         A map center coordinates
     */
    @Override
    protected void onExtentChanged(
            float zoom,
            GeoPoint center)
    {
        super.onExtentChanged(zoom, center);
        if (mListeners == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putFloat(BUNDLE_ZOOM_KEY, zoom);
        bundle.putDouble(BUNDLE_X_KEY, center.getX());
        bundle.putDouble(BUNDLE_Y_KEY, center.getY());
        bundle.putInt(BUNDLE_TYPE_KEY, EVENT_onExtentChanged);

        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    /**
     * Send layers reordered event to all listeners
     */
    @Override
    protected void onLayersReordered()
    {
        super.onLayersReordered();
        if (mListeners == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_TYPE_KEY, EVENT_onLayersReordered);

        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    /**
     * Send layers draw finished event to all listeners
     */
    protected void onLayerDrawFinished(
            int id,
            float percent)
    {
        if (mListeners == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_TYPE_KEY, EVENT_onLayerDrawFinished);
        bundle.putInt(BUNDLE_ID_KEY, id);
        bundle.putFloat(BUNDLE_DONE_KEY, percent);

        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    /**
     * Send layers draw started event to all listeners
     */
    protected void onLayerDrawStarted()
    {
        if (mListeners == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_TYPE_KEY, EVENT_onLayerDrawStarted);

        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    /**
     * Create handler for messages
     */
    protected void createHandler()
    {
        mHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                super.handleMessage(msg);

                if (mFreeze) {
                    return;
                }

                Bundle resultData = msg.getData();

                Long lastTime = mLastMessages.get(resultData.getInt(BUNDLE_TYPE_KEY));
                if(lastTime != null && System.currentTimeMillis() - lastTime < SKIP_TIMEOUT){
                    boolean filter = !(EVENT_onExtentChanged == resultData.getInt(BUNDLE_TYPE_KEY) ||
                            EVENT_onLayerDrawFinished == resultData.getInt(BUNDLE_TYPE_KEY));
                    if(filter) {
                        if(Constants.DEBUG_MODE) {
                            Log.d(TAG, "handleMessage: skip event: " + resultData.getInt(BUNDLE_TYPE_KEY));
                        }
                        return;
                    }
//                    else{
//                        if(resultData.getFloat(BUNDLE_DONE_KEY) < 1){
//
//                            if(Constants.DEBUG_MODE) {
//                                Log.d(TAG, "handleMessage: skip event: " + resultData.getInt(BUNDLE_TYPE_KEY));
//                            }
//
//                            return;
//                        }
//                    }
                }
                mLastMessages.put(resultData.getInt(BUNDLE_TYPE_KEY), System.currentTimeMillis());

                for (MapEventListener listener : mListeners) {
                    switch (resultData.getInt(BUNDLE_TYPE_KEY)) {
                        case EVENT_onLayerAdded:
                            listener.onLayerAdded(resultData.getInt(BUNDLE_ID_KEY));
                            break;
                        case EVENT_onLayerDeleted:
                            listener.onLayerDeleted(resultData.getInt(BUNDLE_ID_KEY));
                            break;
                        case EVENT_onLayerChanged:
                            listener.onLayerChanged(resultData.getInt(BUNDLE_ID_KEY));
                            break;
                        case EVENT_onExtentChanged:
                            listener.onExtentChanged(
                                    resultData.getFloat(BUNDLE_ZOOM_KEY), new GeoPoint(
                                            resultData.getDouble(BUNDLE_X_KEY),
                                            resultData.getDouble(BUNDLE_Y_KEY)));
                            break;
                        case EVENT_onLayerDrawFinished:
                            listener.onLayerDrawFinished(
                                    resultData.getInt(BUNDLE_ID_KEY),
                                    resultData.getFloat(BUNDLE_DONE_KEY));
                            break;
                        case EVENT_onLayersReordered:
                            listener.onLayersReordered();
                            break;
                        case EVENT_onLayerDrawStarted:
                            listener.onLayerDrawStarted();
                            break;
                    }
                }
            }
        };
    }


    public void freeze()
    {
        mFreeze = true;
    }


    public void thaw()
    {
        mFreeze = false;
    }
}
