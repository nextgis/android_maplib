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

package com.nextgis.maplib.map;

import android.content.Context;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.util.FileUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.nextgis.maplib.util.Constants.*;

public class MapBase
        extends LayerGroup
{
    protected short mNewId;
    protected static MapBase mInstance = null;
    protected String mFileName;


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
    public short getNewId()
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
        if(mInstance == null)
            throw new IllegalArgumentException("Impossible to get the instance. This class must be initialized before");
        return mInstance;
    }

    public ILayer getLastLayer()
    {
        if(mLayers.size() == 0)
            return null;
        return mLayers.get(mLayers.size() - 1);
    }

    @Override
    public boolean delete()
    {
        return FileUtil.deleteRecursive(getFileName());
    }


    @Override
    protected File getFileName()
    {
        return new File(getPath(), mFileName);
    }
}
