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

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.util.FileUtil;

import java.io.File;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;


public class MapBase
        extends LayerGroup
{
    protected int mNewId;
    protected static MapBase mInstance = null;
    protected String mFileName;
    private boolean mDirty;

    public MapBase(
            Context context,
            File path,
            LayerFactory layerFactory)
    {
        super(context, path.getParentFile(), layerFactory);
        mNewId = 0;
        mId = NOT_FOUND;
        mInstance = this;
        mFileName = path.getName();
    }


    /**
     * The identificator generator
     *
     * @return new id
     */
    public int getNewId()
    {
        return mNewId++;
    }


    @Override
    protected void onLayerAdded(ILayer layer)
    {
        layer.setId(getNewId());
        super.onLayerAdded(layer);
    }


    public static MapBase getInstance()
    {
        if (mInstance == null) {
            throw new IllegalArgumentException(
                    "Impossible to get the instance. This class must be initialized before");
        }
        return mInstance;
    }


    public ILayer getLastLayer()
    {
        if (mLayers.size() == 0) {
            return null;
        }
        return mLayers.get(mLayers.size() - 1);
    }


    @Override
    public boolean delete()
    {
        for (ILayer layer : mLayers) {
            layer.setParent(null);
            layer.delete();
        }

        mLayers.clear();

        return FileUtil.deleteRecursive(getFileName());
    }


    @Override
    protected File getFileName()
    {
        return new File(getPath(), mFileName);
    }


    public void moveTo(File newPath)
    {

        if (mPath.equals(newPath)) {
            return;
        }

        clearLayers();

        if (FileUtil.move(mPath, newPath)) {
            //change path
            mPath = newPath;
        }
        load();
    }

    public GeoEnvelope getFullBounds(){
        if(null != mDisplay){
            return mDisplay.getFullBounds();
        }
        return new GeoEnvelope();
    }

    public GeoEnvelope getCurrentBounds()
    {
        if (mDisplay != null) {
            return mDisplay.getBounds();
        }
        return null;
    }

    @Override
    public void clearLayers() {
        super.clearLayers();
        mNewId = 0;
    }

    public void setDirty(boolean dirty) {
        mDirty = dirty;
    }

    public boolean isDirty() {
        return mDirty;
    }
}
