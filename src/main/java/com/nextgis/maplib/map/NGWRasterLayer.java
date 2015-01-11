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
import com.nextgis.maplib.api.INGWLayer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.nextgis.maplib.util.Constants.*;

public class NGWRasterLayer extends RemoteTMSLayer implements INGWLayer
{
    protected String mAccount;

    protected final static short MAX_THREAD_COUNT = 8;
    protected static final String JSON_ACCOUNT_KEY = "account";

    public NGWRasterLayer(
            Context context,
            File path)
    {
        super(context, path);
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_ACCOUNT_KEY, mAccount);
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mAccount = jsonObject.getString(JSON_ACCOUNT_KEY);
    }


    @Override
    public int getMaxThreadCount()
    {
        return MAX_THREAD_COUNT;
    }


    @Override
    public int getType()
    {
        return LAYERTYPE_NGW_RASTER;
    }


    @Override
    public String getAccount()
    {
        return mAccount;
    }


    @Override
    public void setAccount(String account)
    {
        mAccount = account;
    }
}
