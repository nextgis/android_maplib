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

import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.JSONStore;
import com.nextgis.maplib.display.Renderer;
import com.nextgis.maplib.util.FileUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.*;

public class Layer implements JSONStore{
    protected String mName;
    protected boolean mIsVisible;
    protected short mId;
    protected int mMaxZoom;
    protected int mMinZoom;
    protected File mPath;
    protected int mLayerType;
    protected Renderer mRenderer;
    protected GeoEnvelope mExtents;

    public Layer(File path){
        mPath = path;
    }

    public final String getName() {
        return mName;
    }

    public void setName(String newName) {
        this.mName = newName;
    }

    public final short getId(){
        return mId;
    }

    public int getType(){
        return mLayerType;
    }

    public final boolean isVisible(){
        return mIsVisible;
    }

    public void setVisible(boolean visible){
        mIsVisible = visible;
    }

    public boolean delete(){
        return FileUtil.deleteRecursive(mPath);
    }

    public int getMaxZoom() {
        return mMaxZoom;
    }

    public void setMaxZoom(int maxZoom) {
        mMaxZoom = maxZoom;
    }

    public int getMinZoom() {
        return mMinZoom;
    }

    public void setMinZoom(int minZoom) {
        mMinZoom = minZoom;
    }

    public File getPath() {
        return mPath;
    }

    public boolean save(){
        try {
            FileUtil.createDir(getPath());
            File config_file = new File(getPath(), LAYER_CONFIG);
            FileUtil.writeToFile(config_file, toJSON().toString());
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

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject rootConfig = new JSONObject();
        rootConfig.put(JSON_NAME_KEY, getName());
        rootConfig.put(JSON_TYPE_KEY, getType());
        rootConfig.put(JSON_MAXLEVEL_KEY, getMaxZoom());
        rootConfig.put(JSON_MINLEVEL_KEY, getMinZoom());
        rootConfig.put(JSON_VISIBILITY_KEY, isVisible());
        return rootConfig;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        mLayerType = jsonObject.getInt(JSON_TYPE_KEY);
        mName = jsonObject.getString(JSON_NAME_KEY);
        if(jsonObject.has(JSON_MAXLEVEL_KEY))
            mMaxZoom = jsonObject.getInt(JSON_MAXLEVEL_KEY);
        else
            mMaxZoom = DEFAULT_MAX_ZOOM;
        if(jsonObject.has(JSON_MINLEVEL_KEY))
            mMinZoom = jsonObject.getInt(JSON_MINLEVEL_KEY);
        else
            mMinZoom = DEFAULT_MIN_ZOOM;

        mIsVisible = jsonObject.getBoolean(JSON_VISIBILITY_KEY);
    }

    public void runDraw() throws NullPointerException {
        if (mRenderer != null) {
            mRenderer.runDraw();
        }
    }

    public void cancelDraw(){
        if(mRenderer != null){
            mRenderer.cancelDraw();
        }
    }

    public Renderer getRenderer() {
        return mRenderer;
    }

    public GeoEnvelope getExtents() {
        return mExtents;
    }
}
