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

package com.nextgis.maplib.map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.nextgis.maplib.util.Constants.*;

public class NGWRasterLayer extends RemoteTMSLayer implements INGWLayer
{
    protected String mAccountName;

    protected final static short  MAX_THREAD_COUNT = 8;
    protected static final String JSON_ACCOUNT_KEY = "account";


    public NGWRasterLayer(
            Context context,
            File path)
    {
        super(context, path);
        mLayerType = LAYERTYPE_NGW_RASTER;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_ACCOUNT_KEY, mAccountName);
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mAccountName = jsonObject.getString(JSON_ACCOUNT_KEY);
    }


    @Override
    public int getMaxThreadCount()
    {
        return MAX_THREAD_COUNT;
    }


    @Override
    public String getAccountName()
    {
        return mAccountName;
    }


    @Override
    public void setAccountName(String accountName)
    {
        mAccountName = accountName;
    }


    @Override
    public String getLogin()
    {
        IGISApplication app = (IGISApplication) mContext.getApplicationContext();
        AccountManager accountManager = AccountManager.get(mContext.getApplicationContext());
        Account account = app.getAccount(mAccountName);

        return accountManager.getUserData(account, "login");
    }


    @Override
    public void setLogin(String login)
    {
        throw new AssertionError("NGWRasterLayer.setLogin() can not be used");
    }


    @Override
    public String getPassword()
    {
        IGISApplication app = (IGISApplication) mContext.getApplicationContext();
        AccountManager accountManager = AccountManager.get(mContext.getApplicationContext());
        Account account = app.getAccount(mAccountName);

        return accountManager.getPassword(account);
    }


    @Override
    public void setPassword(String password)
    {
        throw new AssertionError("NGWRasterLayer.setPassword() can not be used");
    }
}
