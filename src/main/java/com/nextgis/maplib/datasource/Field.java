/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplib.datasource;

import android.text.TextUtils;
import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.util.Constants;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * A class to describe feature field
 */
public class Field
        implements IJSONStore
{
    protected int    mType;
    protected String mName;
    protected String mAlias;

    protected static final String JSON_ALIAS_KEY = "alias";


    public Field()
    {
    }


    public Field(
            int type,
            String name,
            String alias)
    {
        mType = type;
        mName = name;
        if (TextUtils.isEmpty(mAlias)) {
            mAlias = mName;
        } else {
            mAlias = alias;
        }
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootObject = new JSONObject();
        rootObject.put(Constants.JSON_TYPE_KEY, mType);
        rootObject.put(Constants.JSON_NAME_KEY, mName);
        rootObject.put(JSON_ALIAS_KEY, mAlias);
        return rootObject;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        mType = jsonObject.getInt(Constants.JSON_TYPE_KEY);
        mName = jsonObject.getString(Constants.JSON_NAME_KEY);
        if (jsonObject.has(JSON_ALIAS_KEY)) {
            mAlias = jsonObject.getString(JSON_ALIAS_KEY);
        }
    }


    public int getType()
    {
        return mType;
    }


    public String getName()
    {
        return mName;
    }


    public String getAlias()
    {
        return mAlias;
    }


    public void setName(String name)
    {
        mName = name;
    }


    public void setAlias(String alias)
    {
        mAlias = alias;
    }
}
