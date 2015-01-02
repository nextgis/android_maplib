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
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.ILayerView;
import com.nextgis.maplib.api.IRenderer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.display.GISDisplay;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.DEFAULT_MAX_ZOOM;
import static com.nextgis.maplib.util.GeoConstants.DEFAULT_MIN_ZOOM;


public class LayerGroup
        extends Layer
{
    protected List<ILayer> mLayers;
    protected LayerFactory mLayerFactory;
    protected int        mLayerDrawId;
    protected GISDisplay mDisplay;


    public LayerGroup(
            Context context,
            File path,
            LayerFactory layerFactory)
    {
        super(context, path);

        mLayerFactory = layerFactory;
        mLayers = new ArrayList<>();

        mLayerDrawId = 0;
    }


    /**
     * Get layer by identificator
     *
     * @param id
     *         Layer identificator
     *
     * @return Layer or null
     */
    public ILayer getLayerById(int id)
    {
        for (ILayer layer : mLayers) {
            if (layer.getId() == id) {
                return layer;
            }
        }
        return null;
    }


    public ILayer getLayerByName(String name)
    {
        for (ILayer layer : mLayers) {
            if (layer.getName().equals(name)) {
                return layer;
            }
        }
        return null;
    }


    /**
     * Create existed layer from path and add it to the map
     *
     * @param layer
     *         A layer object
     */
    public void addLayer(ILayer layer)
    {
        if (layer != null) {
            mLayers.add(layer);
            layer.setParent(this);
            onLayerAdded(layer);
        }
    }


    /**
     * Delete layer
     *
     * @param layer
     *         An layer
     *
     * @return true on success or false
     */
    public boolean deleteLayer(ILayer layer)
    {
        if (layer != null) {
            short nId = layer.getId();
            layer.delete();
            if (mLayers.remove(layer)) {
                onLayerDeleted(nId);
                return true;
            }
        }
        return false;
    }


    @Override
    public void runDraw(GISDisplay display)
    {
        if (mDisplay == null) {
            mDisplay = display;
        }
        if (mDisplay != display) {
            mDisplay = display;
        }
        mLayerDrawId = 0;
        drawNext(display);
    }


    @Override
    public void cancelDraw()
    {
        for (ILayer layer : mLayers) {
            if (layer instanceof IRenderer) {
                IRenderer renderer = (IRenderer) layer;
                renderer.cancelDraw();
            }
        }
    }


    @Override
    public boolean isVisible()
    {
        for (ILayer layer : mLayers) {
            if (layer instanceof ILayerView) {
                ILayerView layerView = (ILayerView) layer;
                if (layerView.isVisible()) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void setVisible(boolean visible)
    {
        for (ILayer layer : mLayers) {
            if (layer instanceof ILayerView) {
                ILayerView layerView = (ILayerView) layer;
                layerView.setVisible(visible);
            }
        }
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = new JSONObject();
        rootConfig.put(JSON_NAME_KEY, getName());
        rootConfig.put(JSON_TYPE_KEY, getType());
        rootConfig.put(JSON_MAXLEVEL_KEY, getMaxZoom());
        rootConfig.put(JSON_MINLEVEL_KEY, getMinZoom());
        rootConfig.put(JSON_VISIBILITY_KEY, isVisible());

        JSONArray jsonArray = new JSONArray();
        rootConfig.put(JSON_LAYERS_KEY, jsonArray);
        for (ILayer layer : mLayers) {
            JSONObject layerObject = new JSONObject();
            layerObject.put(JSON_PATH_KEY, layer.getPath());
            jsonArray.put(layerObject);
        }

        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        mLayerType = jsonObject.getInt(JSON_TYPE_KEY);
        mName = jsonObject.getString(JSON_NAME_KEY);
        if (jsonObject.has(JSON_MAXLEVEL_KEY)) {
            mMaxZoom = jsonObject.getInt(JSON_MAXLEVEL_KEY);
        } else {
            mMaxZoom = DEFAULT_MAX_ZOOM;
        }
        if (jsonObject.has(JSON_MINLEVEL_KEY)) {
            mMinZoom = jsonObject.getInt(JSON_MINLEVEL_KEY);
        } else {
            mMinZoom = DEFAULT_MIN_ZOOM;
        }

        mIsVisible = jsonObject.getBoolean(JSON_VISIBILITY_KEY);

        final JSONArray jsonArray = jsonObject.getJSONArray(JSON_LAYERS_KEY);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonLayer = jsonArray.getJSONObject(i);
            String sPath = jsonLayer.getString(JSON_PATH_KEY);
            File inFile = new File(getPath(), sPath);
            if (inFile.exists()) {
                ILayer layer = mLayerFactory.createLayer(mContext, inFile);
                if (layer.load()) {
                    addLayer(layer);
                }
            }
        }
    }


    @Override
    public void onDrawFinished(
            int id,
            float percent)
    {
        if (mLayers.size() >= mLayerDrawId) {
            if (mParent != null && mParent instanceof ILayerView) {
                ILayerView renderer = (ILayerView) mParent;
                renderer.onDrawFinished(getId(), 100);
            }
        } else {
            drawNext(mDisplay);
            if (mParent instanceof ILayerView) {
                ILayerView layerView = (ILayerView) mParent;
                layerView.onDrawFinished(id, percent);
            }
        }
    }


    @Override
    public GeoEnvelope getExtents()
    {
        return mExtents;
    }


    protected void drawNext(final GISDisplay display)
    {
        ILayer layer = mLayers.get(mLayerDrawId);
        if (layer instanceof IRenderer) {
            IRenderer renderer = (IRenderer) layer;
            renderer.runDraw(display);
        }
        mLayerDrawId++;
    }


    protected void onLayerAdded(ILayer layer)
    {
        if (mParent != null && mParent instanceof LayerGroup) {
            LayerGroup group = (LayerGroup) mParent;
            group.onLayerAdded(layer);
        }
    }


    protected void onLayerChanged(ILayer layer)
    {
        if (mParent != null && mParent instanceof LayerGroup) {
            LayerGroup group = (LayerGroup) mParent;
            group.onLayerChanged(layer);
        }
    }


    protected void onLayerDeleted(int id)
    {
        if (mParent != null && mParent instanceof LayerGroup) {
            LayerGroup group = (LayerGroup) mParent;
            group.onLayerDeleted(id);
        }
    }


    protected void onExtentChanged(
            int zoom,
            GeoPoint center)
    {
        if (mParent != null && mParent instanceof LayerGroup) {
            LayerGroup group = (LayerGroup) mParent;
            group.onExtentChanged(zoom, center);
        }
    }


    protected void onLayersReordered()
    {
        if (mParent != null && mParent instanceof LayerGroup) {
            LayerGroup group = (LayerGroup) mParent;
            group.onLayersReordered();
        }
    }


    public int getLayerCount()
    {
        return mLayers.size();
    }


    public ILayer getLayer(int index)
    {
        return mLayers.get(index);
    }


    public LayerFactory getLayerFactory()
    {
        return mLayerFactory;
    }
}
