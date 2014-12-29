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

import com.nextgis.maplib.datasource.JSONStore;
import com.nextgis.maplib.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;

public class MapBase implements JSONStore {
    protected List<Layer> mLayers;
    protected File mMapPath;
    protected short mNewId;
    protected LayerFactory mLayerFactory;

    public MapBase(File mapPath, LayerFactory layerFactory){
        mNewId = 0;
        mLayers = new ArrayList<Layer>();
        mMapPath = mapPath;
        mLayerFactory = layerFactory;
    }

    /**
     * Get the map layers
     *
     * @return map layers list
     */
    public List<Layer> getLayers() {
        return mLayers;
    }

    public File getPath() {
        return mMapPath;
    }

    /**
     * The identificator generator
     *
     * @return new id
     */
    public short getNewId(){
        return mNewId++;
    }

    /**
     * Delete layer by identifictor
     *
     * @param id An identificator
     * @return true on success or false
     */
    public boolean deleteLayerById(int id){
        boolean bRes = false;

        for(Layer layer : mLayers) {
            if (layer.getId() == id) {
                layer.delete();
                bRes = mLayers.remove(layer);
                break;
            }
        }
        return bRes;
    }

    /**
     * Get layer by identificator
     *
     * @param id Layer identificator
     * @return Layer or null
     */
    public Layer getLayerById(int id){
        for(Layer layer : mLayers){
            if(layer.getId() == id)
                return layer;
        }
        return null;
    }

    public Layer getLayerByName(String name) {
        for (Layer layer : mLayers) {
            if (layer.getName().equals(name))
                return layer;
        }
        return null;
    }

    /**
     * Create existed layer from path and add it to the map
     *
     * @param layer A layer object
     */
    protected void addLayer(Layer layer){
        if(layer != null) {
            mLayers.add(layer);
        }
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject rootObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        rootObject.put(JSON_LAYERS_KEY, jsonArray);
        for(Layer layer : mLayers){
            JSONObject layerObject = new JSONObject();
            layerObject.put(JSON_PATH_KEY, layer.getPath());
            jsonArray.put(layerObject);
        }
        return rootObject;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        final JSONArray jsonArray = jsonObject.getJSONArray(JSON_LAYERS_KEY);
        for(int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonLayer = jsonArray.getJSONObject(i);
            String sPath = jsonLayer.getString(JSON_PATH_KEY);
            File inFile = new File(mMapPath.getParentFile(), sPath);
            if(inFile.exists()) {
                Layer layer = mLayerFactory.createLayer(inFile);
                if(layer.load())
                    addLayer(layer);
            }
        }
    }

    public boolean save(){
        for(Layer layer : mLayers){
            layer.save();
        }
        try {
            FileUtil.writeToFile(getPath(), toJSON().toString());
        } catch (IOException e) {
            return false;
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    public boolean load(){
        try {
            JSONObject jsonObject = new JSONObject(FileUtil.readFromFile(getPath()));
            fromJSON(jsonObject );
        } catch (JSONException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
