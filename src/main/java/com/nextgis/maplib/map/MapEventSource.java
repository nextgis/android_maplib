/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplib.map;

import android.content.Context;

import com.nextgis.maplib.api.IEventSource;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.MapEventListener;
import com.nextgis.maplib.datasource.GeoPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MapEventSource extends MapBase implements IEventSource{
    protected List<MapEventListener> mListeners;
    //protected Handler mHandler;

    public MapEventSource(Context context, File mapPath, LayerFactory layerFactory) {
        super(context, mapPath, layerFactory);
        mListeners = new ArrayList<MapEventListener>();

        //createHandler();
    }

    /**
     * Add new listener for map events
     *
     * @param listener A listener class implements MapEventListener adding to listeners array
     */
    @Override
    public void addListener(MapEventListener listener){
        if(mListeners != null && !mListeners.contains(listener)){
            mListeners.add(listener);
        }
    }

    /**
     * Remove listener from listeners
     *
     * @param listener A listener class implements MapEventListener removing from listeners array
     */
    @Override
    public void removeListener(MapEventListener listener){
        if(mListeners != null){
            mListeners.remove(listener);
        }
    }

    /**
     * Send layer added event to all listeners
     *
     * @param layer A new layer
     */
    protected void onLayerAdded(ILayer layer){
        if(mListeners == null)
            return;
        for (MapEventListener listener : mListeners)
            listener.onLayerAdded(layer);
    }

    /**
     * Send layer changed event to all listeners
     *
     * @param layer A changed layer
     */
    protected void onLayerChanged(ILayer layer){
        if(mListeners == null)
            return;
        for (MapEventListener listener : mListeners)
            listener.onLayerChanged(layer);
    }

    /**
     * Send layer delete event to all listeners
     *
     * @param id A deleted layer identificator
     */
    protected void onLayerDeleted(int id){
        if(mListeners == null)
            return;
        for (MapEventListener listener : mListeners)
            listener.onLayerDeleted(id);
    }

    /**
     * Send extent change event to all listeners
     * @param zoom A zoom level
     * @param center A map center coordinates
     */
    protected void onExtentChanged(int zoom, GeoPoint center){
        if(mListeners == null)
            return;
        for (MapEventListener listener : mListeners)
            listener.onExtentChanged(zoom, center);
    }

    /**
     * Send layers reordered event to all listeners
     */
    protected void onLayersReordered(){
        if(mListeners == null)
            return;
        for (MapEventListener listener : mListeners)
            listener.onLayersReordered();
    }

    /**
     * Send layers draw finished event to all listeners
     */
    protected void onLayerDrawFinished(int id, float percent){
        if(mListeners == null)
            return;
        for (MapEventListener listener : mListeners)
            listener.onLayerDrawFinished(id, percent);
    }

    @Override
    public void onDrawFinished(int id, float percent) {
        onLayerDrawFinished(id, percent);
    }

    /*

    /**
     * Create handler for messages
     *//*
    protected void createHandler(){
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Bundle resultData = msg.getData();
                boolean bHasErr = resultData.getBoolean(BUNDLE_HASERROR_KEY);
                if(bHasErr){
                    reportError(resultData.getString(BUNDLE_MSG_KEY));
                }
                else{
                    processMessage(resultData);
                }
            }
        };
    }

    /**
     * Process message received by handler
     *
     * @param bundle A message payload
     *//*
    protected void processMessage(Bundle bundle){
        switch (bundle.getInt(BUNDLE_TYPE_KEY)){
            case MSGTYPE_DRAWING_DONE:
                onLayerDrawFinished(bundle.getInt(BUNDLE_LAYERNO_KEY), bundle.getFloat(BUNDLE_DONE_KEY));
                break;
            default:
                break;
        }
    }
     */
}
