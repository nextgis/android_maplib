/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
import android.database.sqlite.SQLiteException;
import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.util.FileUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import static com.nextgis.maplib.util.Constants.*;

/**
 * Plain table without geometry
 */
public class Table implements ILayer, IJSONStore {

    protected String  mName;
    protected long    mId;
    protected File    mPath;
    protected int     mLayerType;
    protected ILayer  mParent;
    protected Context mContext;

    public Table(
            Context context,
            File path)
    {
        mPath = path;
        mContext = context;
    }

    @Override
    public String getName()
    {
        return mName;
    }


    @Override
    public void setName(String newName)
    {
        this.mName = newName;
        notifyLayerChanged();
    }


    @Override
    public long getId()
    {
        return mId;
    }


    @Override
    public int getType()
    {
        return mLayerType;
    }


    @Override
    public boolean delete()
    {
        FileUtil.deleteRecursive(mPath);
        if (mParent != null && mParent instanceof LayerGroup) {
            LayerGroup group = (LayerGroup) mParent;
            group.onLayerDeleted(mId);
        }
        return true;
    }

    protected void notifyLayerChanged()
    {
        if (mParent != null && mParent instanceof LayerGroup) {
            LayerGroup group = (LayerGroup) mParent;
            group.onLayerChanged(this);
        }
    }

    @Override
    public File getPath()
    {
        return mPath;
    }

    protected File getFileName()
    {
        FileUtil.createDir(getPath());
        return new File(getPath(), CONFIG);
    }

    @Override
    public boolean save()
    {
        try {
            FileUtil.writeToFile(getFileName(), toJSON().toString());
        } catch (IOException e) {
            return false;
        } catch (JSONException e) {
            return false;
        }
        return true;
    }


    @Override
    public boolean load()
    {
        try {
            JSONObject jsonObject = new JSONObject(FileUtil.readFromFile(getFileName()));
            fromJSON(jsonObject);
        } catch (JSONException | IOException | SQLiteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = new JSONObject();
        rootConfig.put(JSON_NAME_KEY, getName());
        rootConfig.put(JSON_TYPE_KEY, getType());
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        mLayerType = jsonObject.getInt(JSON_TYPE_KEY);
        mName = jsonObject.getString(JSON_NAME_KEY);
    }


    @Override
    public void setParent(ILayer layer)
    {
        mParent = layer;
    }

    @Override
    public ILayer getParent() {
        return mParent;
    }

    @Override
    public void setId(short id)
    {
        mId = id;
    }


    public Context getContext()
    {
        return mContext;
    }


    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public GeoEnvelope getExtents()
    {
        return null;
    }
}
