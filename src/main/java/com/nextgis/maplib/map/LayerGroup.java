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
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.ILayerView;
import com.nextgis.maplib.api.IRenderer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.display.GISDisplay;
import com.nextgis.maplib.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static com.nextgis.maplib.util.Constants.*;


public class LayerGroup
        extends Layer
{
    protected final List<ILayer> mLayers = new ArrayList<>();
    protected LayerFactory mLayerFactory;
    protected int          mLayerDrawIndex;
    protected GISDisplay   mDisplay;
    protected OnAllLayersAddedListener mOnAllLayersAddedListener;


    public interface OnAllLayersAddedListener
    {
        void onAllLayersAdded(List<ILayer> layers);
    }


    public LayerGroup(
            Context context,
            File path,
            LayerFactory layerFactory)
    {
        super(context, path);

        mLayerFactory = layerFactory;

        mLayerDrawIndex = 0;

        mLayerType = LAYERTYPE_GROUP;
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
        if (mId == id) {
            return this;
        }

        for (ILayer layer : mLayers) {
            if (layer.getId() == id) {
                return layer;
            }
        }
        return null;
    }


    /**
     * search layer by it human readabler name
     * @param name Name to search
     * @return ILayer or null
     */
    public ILayer getLayerByName(String name)
    {
        if (mName.equals(name)) {
            return this;
        }

        for (ILayer layer : mLayers) {
            if (layer.getName().equals(name)) {
                return layer;
            }
        }
        return null;
    }

    /**
     * Search layer by it folder name
     * @param name Name to search
     * @return ILayer or null
     */
    public ILayer getLayerByPathName(String name){
        if (getPath().getName().equals(name)) {
            return this;
        }

        for (ILayer layer : mLayers) {
            if (layer.getPath().getName().equals(name)) {
                return layer;
            }
        }
        return null;
    }


    /**
     * Get a list of specified type layers
     *
     * @param layerGroup
     *         to inspect for layers
     * @param types
     *         A layer type
     * @param layerList
     *         A list to fill with find layers
     */
    public static void getLayersByType(
            LayerGroup layerGroup,
            int types,
            List<ILayer> layerList)
    {
        for (int i = 0; i < layerGroup.getLayerCount(); i++) {
            ILayer layer = layerGroup.getLayer(i);

            if (0 != (types & layer.getType())) {
                layerList.add(layer);
            }

            if (layer instanceof LayerGroup) {
                getLayersByType((LayerGroup) layer, types, layerList);
            }
        }
    }


    public static void getVectorLayersByType(
            LayerGroup layerGroup,
            int types,
            List<ILayer> layerList)
    {
        for (int i = 0; i < layerGroup.getLayerCount(); i++) {
            ILayer layer = layerGroup.getLayer(i);

            if (layer instanceof VectorLayer) {
                VectorLayer vectorLayer = (VectorLayer) layer;
                if (0 != (types & 1 << vectorLayer.getGeometryType())) {
                    layerList.add(0, layer);
                }
            }

            if (layer instanceof LayerGroup) {
                getVectorLayersByType((LayerGroup) layer, types, layerList);
            }
        }
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


    public void insertLayer(
            int index,
            ILayer layer)
    {
        if (layer != null) {

            mLayers.add(index, layer);
            layer.setParent(this);
            onLayerAdded(layer);
        }
    }


    public void moveLayer(
            int newPosition,
            ILayer layer)
    {
        if (layer != null) {

            synchronized (this) {
                mLayers.remove(layer);
                mLayers.add(newPosition, layer);
            }
            onLayersReordered();
        }
    }


    public int removeLayer(ILayer layer)
    {
        synchronized (this) {
            int result = mLayers.size() - 1;

            if (layer != null) {
                result = mLayers.indexOf(layer);
                onLayerDeleted(layer.getId());
            }
            return result;
        }
    }


    @Override
    public void runDraw(GISDisplay display)
    {
        if (null != display && mDisplay != display) {
            mDisplay = display;
        }

        if (mLayers.size() == 0) {
            return;
        }

        //synchronized (this) {
            for (ILayer layer : mLayers) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                if (layer instanceof LayerGroup) {
                    LayerGroup layerGroup = (LayerGroup) layer;
                    layerGroup.runDraw(mDisplay);

                } else {

                    if (layer.isValid() && layer instanceof ILayerView) {

                        ILayerView layerView = (ILayerView) layer;
                        if (layerView.isVisible() && layer instanceof IRenderer &&
                                mDisplay.getZoomLevel() <= layerView.getMaxZoom() &&
                                mDisplay.getZoomLevel() >= layerView.getMinZoom()) {
                            // Log.d(Constants.TAG, "Layer Draw Index: " + mLayerDrawIndex);

                            IRenderer renderer = (IRenderer) layer;
                            renderer.runDraw(mDisplay);

                        }
                    }
                }
            }
        //}
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


    public int getVisibleTopLayerId()
    {
       for (int i = mLayers.size() - 1; i >= 0; i--) {
            ILayer layer = mLayers.get(i);
            if (layer instanceof LayerGroup) {
                LayerGroup layerGroup = (LayerGroup) layer;
                int visibleTopLayerId = layerGroup.getVisibleTopLayerId();
                if (Constants.NOT_FOUND != visibleTopLayerId) {
                    return visibleTopLayerId;
                }

            } else {
                if (layer.isValid() && layer instanceof ILayerView) {
                    ILayerView layerView = (ILayerView) layer;
                    if (layerView.isVisible()) {
                        return layer.getId();
                    }
                }
            }
        }

        return Constants.NOT_FOUND;
    }


    public int getVisibleLayerCount()
    {
        int visibleLayerCount = 0;

        for (int i = mLayers.size() - 1; i >= 0; i--) {
            ILayer layer = mLayers.get(i);
            if (layer instanceof LayerGroup) {
                LayerGroup layerGroup = (LayerGroup) layer;
                visibleLayerCount += layerGroup.getVisibleLayerCount();

            } else {
                if (layer.isValid() && layer instanceof ILayerView) {
                    ILayerView layerView = (ILayerView) layer;
                    if (layerView.isVisible()) {
                        ++visibleLayerCount;
                    }
                }
            }
        }

        return visibleLayerCount;
    }


    @Override
    public boolean delete()
    {
        for (ILayer layer : mLayers) {
            layer.setParent(null);
            layer.delete();
        }
        return super.delete();
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();

        JSONArray jsonArray = new JSONArray();
        rootConfig.put(JSON_LAYERS_KEY, jsonArray);
        for (ILayer layer : mLayers) {
            JSONObject layerObject = new JSONObject();
            layerObject.put(JSON_PATH_KEY, layer.getPath().getName());
            jsonArray.put(layerObject);
        }
        return rootConfig;
    }


    public void clearLayers()
    {
        for (ILayer layer : mLayers) {
            if (layer instanceof LayerGroup) {
                ((LayerGroup) layer).clearLayers();
            }
        }

        mLayers.clear();
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);

        clearLayers();

        final JSONArray jsonArray = jsonObject.getJSONArray(JSON_LAYERS_KEY);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonLayer = jsonArray.getJSONObject(i);
            String sPath = jsonLayer.getString(JSON_PATH_KEY);
            File inFile = new File(getPath(), sPath);
            if (inFile.exists()) {
                ILayer layer = mLayerFactory.createLayer(mContext, inFile);
                if (null != layer && layer.load()) {
                    addLayer(layer);
                }
            }
        }

        if (mOnAllLayersAddedListener != null)
            mOnAllLayersAddedListener.onAllLayersAdded(mLayers);
    }


    @Override
    public GeoEnvelope getExtents()
    {
        return mExtents;
    }


    @Override
    public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final int oldVersion, final int newVersion) {
        setOnAllLayersAddedListener(new OnAllLayersAddedListener() {
            @Override
            public void onAllLayersAdded(List<ILayer> layers) {
                for (ILayer layer : mLayers) {
                        layer.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
                        setOnAllLayersAddedListener(null);
                }
            }
        });
    }


    protected void setOnAllLayersAddedListener(OnAllLayersAddedListener listener) {
        mOnAllLayersAddedListener = listener;
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
        for (ILayer layer : mLayers) {
            if (layer.getId() == id) {
                mLayers.remove(layer);
                break;
            }
        }

        if (mParent != null && mParent instanceof LayerGroup) {
            LayerGroup group = (LayerGroup) mParent;
            group.onLayerDeleted(id);
        }
    }


    protected void onExtentChanged(
            float zoom,
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


    @Override
    public boolean save()
    {
        synchronized (this) {
            for (ILayer layer : mLayers) {
                layer.save();
            }
        }
        return super.save();
    }

    /**
     * Create the layer folder of specified name
     * @param layerName The name of folder
     * @return Path to the layer folder
     */
    public File createLayerStorage(String layerName)
    {
        if(TextUtils.isEmpty(layerName))
            return createLayerStorage();
        return new File(mPath, layerName);
    }

    /**
     * Create the layer folder of random name
     * @return Path to the layer folder
     */
    public File createLayerStorage()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String layerDir = LAYER_PREFIX + sdf.format(new Date()) + getLayerCount();
        final Random r = new Random();
        layerDir += r.nextInt(99);

        Log.d(Constants.TAG, "createLayerStorage: " + layerDir);
        return new File(mPath, layerDir);
    }


    @Override
    public void setViewSize(
            int w,
            int h)
    {
        super.setViewSize(w, h);

        for (ILayer layer : mLayers) {
            if (layer instanceof ILayerView) {
                ILayerView lv = (ILayerView) layer;
                lv.setViewSize(w, h);
            }
        }
    }


    public boolean isChanges()
    {
        for (ILayer layer : mLayers) {
            if (layer instanceof LayerGroup) {
                LayerGroup layerGroup = (LayerGroup) layer;
                if (layerGroup.isChanges()) {
                    return true;
                }
            } else if (layer instanceof VectorLayer) {
                VectorLayer vectorLayer = (VectorLayer) layer;
                if (vectorLayer.isChanges()) {
                    return true;
                }
            }
        }
        return false;
    }


    public boolean haveFeaturesNotSyncFlag()
    {
        for (ILayer layer : mLayers) {
            if (layer instanceof LayerGroup) {
                LayerGroup layerGroup = (LayerGroup) layer;
                if (layerGroup.haveFeaturesNotSyncFlag()) {
                    return true;
                }
            } else if (layer instanceof VectorLayer) {
                VectorLayer vectorLayer = (VectorLayer) layer;
                if (vectorLayer.haveFeaturesNotSyncFlag()) {
                    return true;
                }
            }
        }
        return false;
    }
}
