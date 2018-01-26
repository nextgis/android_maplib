/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016 NextGIS, info@nextgis.com
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
import android.content.Context;
import android.content.SyncResult;
import android.util.Log;
import android.util.Pair;

import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The NGW lookup table class
 */
public class NGWLookupTable extends Table
        implements INGWLayer {

    protected String mAccountName;
    protected String mCacheUrl;
    protected String mCacheLogin;
    protected String mCachePassword;
    protected Map<String, String> mData;

    protected long        mRemoteId;
    protected NetworkUtil mNet;
    protected int         mSyncType;

    protected static final String JSON_ACCOUNT_KEY = "account";
    protected static final String JSON_LT_DATA_KEY = "lookup_table";
    protected static final String JSON_SYNC_TYPE_KEY = "sync_type";

    public NGWLookupTable(Context context, File path) {
        super(context, path);

        mNet = new NetworkUtil(context);
        mSyncType = Constants.SYNC_NONE;
        mLayerType = Constants.LAYERTYPE_LOOKUPTABLE;
    }

    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_ACCOUNT_KEY, mAccountName);
        rootConfig.put(Constants.JSON_ID_KEY, mRemoteId);
        rootConfig.put(JSON_SYNC_TYPE_KEY, mSyncType);

        JSONObject dataArray = new JSONObject();

        for (Map.Entry<String, String> entry : mData.entrySet()) {
            dataArray.put(entry.getKey(), entry.getValue());
        }

        rootConfig.put(JSON_LT_DATA_KEY, dataArray);

        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        mData = new LinkedHashMap<>();

        super.fromJSON(jsonObject);
        setAccountName(jsonObject.getString(JSON_ACCOUNT_KEY));
        JSONObject dataArray = jsonObject.getJSONObject(JSON_LT_DATA_KEY);

        setAccountName(jsonObject.getString(JSON_ACCOUNT_KEY));

        mRemoteId = jsonObject.getLong(Constants.JSON_ID_KEY);
        mSyncType = jsonObject.getInt(JSON_SYNC_TYPE_KEY);

        for (Iterator<String> iter = dataArray.keys(); iter.hasNext(); ) {
            String key = iter.next();
            String value = dataArray.getString(key);
            mData.put(key, value);
        }
    }


    public void setAccountCacheData()
    {
        IGISApplication app = (IGISApplication) mContext.getApplicationContext();
        Account account = app.getAccount(mAccountName);

        if (null != account) {
            mCacheUrl = app.getAccountUrl(account);
            mCacheLogin = app.getAccountLogin(account);
            mCachePassword = app.getAccountPassword(account);
        }
    }

    @Override
    public int getSyncType()
    {
        return mSyncType;
    }

    @Override
    public void setSyncType(int syncType) {
        mSyncType = syncType;
    }

    @Override
    public void sync(String authority, Pair<Integer, Integer> ver, SyncResult syncResult) {
        if (0 != (mSyncType & Constants.SYNC_NONE)) {
            return;
        }

        Map<String, String> remoteData = new Hashtable<>();

        try {
            fillFromNGW(remoteData, null);
        } catch (IOException | NGException | JSONException e) {
            e.printStackTrace();
            String locMsg = e.getLocalizedMessage();
            if(locMsg != null) {
                Log.d(Constants.TAG, locMsg);
            }
            syncResult.stats.numParseExceptions++;
            return;
        }

        if(remoteData.size() != mData.size()){
            mData.clear();
            mData.putAll(remoteData);
            save();
            Log.d(Constants.TAG, "Update lookup table " + getName() + " from server");
            return;
        }

        // check key and values
        boolean isSame = true;
        for (Map.Entry<String, String> entry : remoteData.entrySet()) {
            if(!mData.get(entry.getKey()).equals(entry.getValue())){
                isSame = false;
                break;
            }
        }

        if(!isSame){
            mData.clear();
            mData.putAll(remoteData);
            save();
            Log.d(Constants.TAG, "Update lookup table " + getName() + " from server");
            return;
        }
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
        setAccountCacheData();
    }

    @Override
    public long getRemoteId()
    {
        return mRemoteId;
    }

    @Override
    public void setRemoteId(long remoteId)
    {
        mRemoteId = remoteId;
    }

    @Override
    public boolean isValid() {
        return null != mData;
    }

    public void fillFromNGW(IProgressor progressor) throws JSONException, NGException, IOException {
        if(null == mData)
            mData = new LinkedHashMap<>();
        fillFromNGW(mData, progressor);
    }

    protected void fillFromNGW(Map<String, String> dataMap, IProgressor progressor) throws IOException, NGException, JSONException {
        if (!mNet.isNetworkAvailable()) { //return tile from cache
            throw new NGException(getContext().getString(R.string.error_network_unavailable));
        }

        if(null != progressor){
            progressor.setMessage(getContext().getString(R.string.message_loading));
        }

        Log.d(Constants.TAG, "download layer " + getName());
        HttpResponse response =
                NetworkUtil.get(NGWUtil.getResourceMetaUrl(mCacheUrl, mRemoteId), mCacheLogin,
                        mCachePassword, false);
        if (!response.isOk()) {
            throw new NGException(NetworkUtil.getError(mContext, response.getResponseCode()));
        }
        JSONObject geoJSONObject = new JSONObject(response.getResponseBody());
        JSONObject lookupTable = geoJSONObject.getJSONObject("lookup_table");
        JSONObject itemsObject = lookupTable.getJSONObject("items");
        for (Iterator<String> iter = itemsObject.keys(); iter.hasNext(); ) {
            String key = iter.next();
            String value = itemsObject.getString(key);
            dataMap.put(key, value);
        }
    }

    /**
     * Get key value map of entries
     * @return Map
     */
    public Map<String, String> getData() {
        return mData;
    }

    public void setData(Map<String, String> data) {
        mData = new LinkedHashMap<>();
        if (data != null) {
            mData.putAll(data);
        }
    }
}
