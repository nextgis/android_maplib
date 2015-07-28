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

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * The NGW lookup table class
 */
public class NGWLookupTable extends Table
        implements INGWLayer {

    protected boolean mIsInitialized;
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
    protected static final String JSON_IS_INITIALIZED_KEY = "is_inited";

    public NGWLookupTable(Context context, File path) {
        super(context, path);
        mIsInitialized = false;

        mNet = new NetworkUtil(context);
        mSyncType = Constants.SYNC_NONE;
        mLayerType = Constants.LAYERTYPE_LOOKUPTABLE;
    }

    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_IS_INITIALIZED_KEY, mIsInitialized);
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
        mData = new HashMap<>();

        super.fromJSON(jsonObject);
        mIsInitialized = jsonObject.getBoolean(JSON_IS_INITIALIZED_KEY);
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

        if (!mIsInitialized) {
            //init in separate thread
            downloadAsync();
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
    public void setSyncType(int syncType)
    {
        mSyncType = syncType;
    }

    @Override
    public void sync(String authority, SyncResult syncResult) {
        if (0 != (mSyncType & Constants.SYNC_NONE) || !mIsInitialized) {
            return;
        }

        Map<String, String> remoteData = new Hashtable<>();
        String result = download(remoteData);
        if(null != result){
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
        return mIsInitialized;
    }

    public String download(){
        if(null == mData)
            mData = new HashMap<>();
        return download(mData);
    }

    protected String download(Map<String, String> dataMap)
    {
        if (!mNet.isNetworkAvailable()) { //return tile from cache
            return getContext().getString(R.string.error_network_unavailable);
        }

        try {
            Log.d(Constants.TAG, "download layer " + getName());
            String data = mNet.get(
                    NGWUtil.getResourceMetaUrl(mCacheUrl, mRemoteId), mCacheLogin, mCachePassword);
            if (null == data) {
                return getContext().getString(R.string.error_download_data);
            }
            JSONObject geoJSONObject = new JSONObject(data);
            JSONObject lookupTable = geoJSONObject.getJSONObject("lookup_table");
            JSONObject itemsObject = lookupTable.getJSONObject("items");
            for (Iterator<String> iter = itemsObject.keys(); iter.hasNext(); ) {
                String key = iter.next();
                String value = itemsObject.getString(key);
                dataMap.put(key, value);
            }

            mIsInitialized = true;
            return null;
        } catch (IOException e) {
            Log.d(Constants.TAG, "Problem downloading GeoJSON: " + mCacheUrl + " Error: " +
                    e.getLocalizedMessage());
            return getContext().getString(R.string.error_download_data);
        } catch (JSONException | SQLiteException e) {
            e.printStackTrace();
            return getContext().getString(R.string.error_download_data);
        }
    }

    public void downloadAsync()
    {
        new DownloadTask().execute();
    }

    protected class DownloadTask
            extends AsyncTask<Void, Void, String>
    {

        @Override
        protected String doInBackground(Void... voids)
        {
            return download(mData);
        }


        @Override
        protected void onPostExecute(String error)
        {
            if (null != error && error.length() > 0) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Get key value map of entries
     * @return Map
     */
    public Map<String, String> getData() {
        return mData;
    }
}
