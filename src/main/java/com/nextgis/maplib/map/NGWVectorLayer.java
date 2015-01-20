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

import android.content.ContentUris;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.util.ChangeFeatureItem;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;


public class NGWVectorLayer extends VectorLayer implements INGWLayer
{
    protected String      mAccountName;
    protected String      mURL;
    protected long        mRemoteId;
    protected NetworkUtil mNet;
    protected String      mLogin;
    protected String      mPassword;
    protected int mSyncType; //0 - no sync, 1 - data, 2 - photo
    //protected int mSyncDirection; //1 - to server only, 2 - from server only, 3 - both directions
    //check where to sync on GSM/WI-FI for data/photo

    protected List<ChangeFeatureItem> mChanges;

    protected static final String JSON_ACCOUNT_KEY  = "account";
    protected static final String JSON_URL_KEY      = "url";
    protected static final String JSON_LOGIN_KEY    = "login";
    protected static final String JSON_PASSWORD_KEY = "password";
    protected static final String JSON_SYNC_TYPE_KEY = "sync_type";


    public NGWVectorLayer(
            Context context,
            File path)
    {
        super(context, path);

        mNet = new NetworkUtil(context);

        mChanges = new ArrayList<>();
        mSyncType = SYNC_NONE;
    }


    @Override
    public void setAccountName(String accountName)
    {
        mAccountName = accountName;
    }


    public void setURL(String URL)
    {
        mURL = URL;
    }


    public void setRemoteId(long remoteId)
    {
        mRemoteId = remoteId;
    }

    public void setLogin(String login)
    {
        mLogin = login;
    }


    public void setPassword(String password)
    {
        mPassword = password;
    }


    @Override
    public int getType()
    {
        return LAYERTYPE_NGW_VECTOR;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_ACCOUNT_KEY, mAccountName);
        rootConfig.put(JSON_ID_KEY, mRemoteId);
        rootConfig.put(JSON_URL_KEY, mURL);
        rootConfig.put(JSON_LOGIN_KEY, mLogin);
        rootConfig.put(JSON_PASSWORD_KEY, mPassword);
        rootConfig.put(JSON_SYNC_TYPE_KEY, mSyncType);
        JSONArray changes = new JSONArray();
        for (ChangeFeatureItem change : mChanges) {
            changes.put(change.toJSON());
        }
        rootConfig.put(JSON_CHANGES_KEY, changes);
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mAccountName = jsonObject.getString(JSON_ACCOUNT_KEY);
        mURL = jsonObject.getString(JSON_URL_KEY);
        mRemoteId = jsonObject.getLong(JSON_ID_KEY);
        if (jsonObject.has(JSON_LOGIN_KEY))
            mLogin = jsonObject.getString(JSON_LOGIN_KEY);
        if (jsonObject.has(JSON_PASSWORD_KEY))
            mPassword = jsonObject.getString(JSON_PASSWORD_KEY);
        //DEBUG:
        //if(jsonObject.has(JSON_SYNC_TYPE_KEY))
        //    mSyncType = jsonObject.getInt(JSON_SYNC_TYPE_KEY);
        mSyncType = SYNC_ALL;
        if (jsonObject.has(JSON_CHANGES_KEY)) {
            JSONArray array = jsonObject.getJSONArray(JSON_CHANGES_KEY);
            for (int i = 0; i < array.length(); i++) {
                JSONObject change = array.getJSONObject(i);
                ChangeFeatureItem item = new ChangeFeatureItem(0, 0);
                item.fromJSON(change);
                mChanges.add(item);
            }
        }

        if (!mIsInitialized) {
            //init in separate thread
            downloadAsync();
        }
    }


    @Override
    public String getAccountName()
    {
        return mAccountName;
    }


    public void downloadAsync()
    {
        new DownloadTask().execute();
    }


    /**
     * download and create new NGW layer from GeoJSON data
     * @return the error message or empty string if everything is ok
     */
    public String download()
    {
        if (!mNet.isNetworkAvailable()) { //return tile from cache
            return getContext().getString(R.string.error_network_unavailable);
        }

        try {

            final HttpGet get = new HttpGet(NGWUtil.getGeoJSONUrl(mURL, mRemoteId)); //get as GeoJSON
            //basic auth
            if (null != mLogin && mLogin.length() > 0 && null != mPassword && mPassword.length() > 0){
                get.setHeader("Accept", "*/*");
                final String basicAuth = "Basic " + Base64.encodeToString(
                        (mLogin + ":" + mPassword).getBytes(), Base64.NO_WRAP);
                get.setHeader("Authorization", basicAuth);
            }

            final DefaultHttpClient HTTPClient = mNet.getHttpClient();
            final HttpResponse response = HTTPClient.execute(get);

            // Check to see if we got success
            final org.apache.http.StatusLine line = response.getStatusLine();
            if (line.getStatusCode() != 200) {
                Log.d(TAG, "Problem downloading GeoJSON: " + mURL + " HTTP response: " +
                           line);
                return getContext().getString(R.string.error_download_data);
            }

            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                Log.d(TAG, "No content downloading GeoJSON: " + mURL);
                return getContext().getString(R.string.error_download_data);
            }

            String data = EntityUtils.toString(entity);
            JSONObject geoJSONObject = new JSONObject(data);

            return createFromGeoJSON(geoJSONObject);

        } catch (IOException e) {
            Log.d(TAG, "Problem downloading GeoJSON: " + mURL + " Error: " +
                       e.getLocalizedMessage());
            return getContext().getString(R.string.error_download_data);
        }
        catch (JSONException e) {
            e.printStackTrace();
            return getContext().getString(R.string.error_download_data);
        }
    }

    protected class DownloadTask
            extends AsyncTask<Void, Void, String>
    {

        @Override
        protected String doInBackground(Void... voids)
        {
            return download();
        }

        @Override
        protected void onPostExecute(String error)
        {
            if(null != error && error.length() > 0){
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT)
                     .show();
            }
        }
    }

    @Override
    protected void addChange(String featureId, int operation)
    {
        if(0 == (mSyncType & SYNC_DATA))
            return;

        //1. if featureId == NOT_FOUND remove all changes and add this one
        if(featureId.equals("" + NOT_FOUND) && operation == ChangeFeatureItem.TYPE_DELETE) {
            mChanges.clear();
            mChanges.add(new ChangeFeatureItem(NOT_FOUND, operation));
        }
        else {
            int id = Integer.parseInt(featureId);
            for(int i = 0; i < mChanges.size(); i++) {
                ChangeFeatureItem item = mChanges.get(i);
                if(item.getFeatureId() == id){
                    //2. if featureId == some id and op is delete - remove and other operations
                    if(operation == ChangeFeatureItem.TYPE_DELETE) {
                        if(item.getOperation() == ChangeFeatureItem.TYPE_DELETE){
                            return;
                        }
                        mChanges.remove(i);
                        if(item.getOperation() == ChangeFeatureItem.TYPE_NEW){
                            save();
                            return;
                        }
                        i--;
                    }
                    //3. if featureId == some id and op is update and previous op was add or update - skip
                    else if(operation == ChangeFeatureItem.TYPE_CHANGED){
                        if(item.getOperation() == ChangeFeatureItem.TYPE_CHANGED || item.getOperation() == ChangeFeatureItem.TYPE_NEW){
                            return;
                        }
                        else{
                            item.setOperation(operation);
                            save();
                            return;
                        }
                    }
                    //4. if featureId == some id and op is add and value present - warning
                    else if(operation == ChangeFeatureItem.TYPE_NEW){
                        Log.w(TAG, "Something wrong. Should nether get here");
                        return;
                    }
                }
            }
            mChanges.add(new ChangeFeatureItem(id, operation));
        }
        save();
    }

    @Override
    protected void addChange(String featureId, String photoName, int operation)
    {
        if(0 == (mSyncType & SYNC_PHOTO))
            return;

        int id = Integer.parseInt(featureId);
        for(int i = 0; i < mChanges.size(); i++){
            ChangeFeatureItem item = mChanges.get(i);
            if(item.getFeatureId() == id){
                if(item.getOperation() == ChangeFeatureItem.TYPE_DELETE) {
                    return;
                }
                else{
                    item.addPhotoChange(photoName, operation);
                    save();
                    return;
                }
            }
        }
        ChangeFeatureItem item = new ChangeFeatureItem(id, ChangeFeatureItem.TYPE_PHOTO);
        item.addPhotoChange(photoName, operation);
        mChanges.add(item);
        save();
    }


    /**
     * Synchronize changes with NGW. Should be run from non UI thread.
     * @param syncResult - report some errors via this parameter
     */
    public void sync(SyncResult syncResult)
    {
        if(0 != (mSyncType & SYNC_NONE)) {
            return;
        }

        //1. get remote changes
        getChangesFromServer(syncResult);

        //2. send current changes
        sendLocalChanges(syncResult);
    }

    protected void sendLocalChanges(SyncResult syncResult)
    {
        for (int i = 0; i < mChanges.size(); i++) {
            ChangeFeatureItem change = mChanges.get(i);
            switch (change.getOperation()){
                case ChangeFeatureItem.TYPE_NEW:
                    if(addFeatureOnServer(change.getFeatureId(), syncResult))
                    {
                        mChanges.remove(i);
                        i--;
                    }
                    break;
                case ChangeFeatureItem.TYPE_CHANGED:
                    if(changeFeatureOnServer(change.getFeatureId(), syncResult))
                    {
                        mChanges.remove(i);
                        i--;
                    }
                    break;
                case ChangeFeatureItem.TYPE_DELETE:
                    if(deleteFeatureOnServer(change.getFeatureId(), syncResult))
                    {
                        mChanges.remove(i);
                        i--;
                    }
                    break;
                case ChangeFeatureItem.TYPE_PHOTO:
                    if(sendPhotosOnServer(change.getFeatureId(), syncResult))
                    {
                        mChanges.remove(i);
                        i--;
                    }
                    break;
            }
        }
    }


    protected void getChangesFromServer(SyncResult syncResult)
    {
        if(!mNet.isNetworkAvailable()){
            return;
        }

        try {
            final HttpGet get = new HttpGet(NGWUtil.getVectorDataUrl(mURL, mRemoteId));
            //basic auth
            if (null != mLogin && mLogin.length() > 0 && null != mPassword && mPassword.length() > 0){
                get.setHeader("Accept", "*/*");
                final String basicAuth = "Basic " + Base64.encodeToString(
                        (mLogin + ":" + mPassword).getBytes(), Base64.NO_WRAP);
                get.setHeader("Authorization", basicAuth);
            }

            final DefaultHttpClient HTTPClient = mNet.getHttpClient();
            final HttpResponse response = HTTPClient.execute(get);

            // Check to see if we got success
            final org.apache.http.StatusLine line = response.getStatusLine();
            if (line.getStatusCode() != 200) {
                Log.d(TAG, "Problem downloading GeoJSON: " + mURL + " HTTP response: " +
                           line);
                syncResult.stats.numIoExceptions++;
                return;
            }

            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                Log.d(TAG, "No content downloading GeoJSON: " + mURL);
                syncResult.stats.numIoExceptions++;
                return;
            }

            String data = EntityUtils.toString(entity);
            JSONArray remoteLayerContents = new JSONArray(data);

            //compare

        } catch (IOException e) {
            Log.d(TAG, "Problem downloading GeoJSON: " + mURL + " Error: " +
                       e.getLocalizedMessage());
            syncResult.stats.numIoExceptions++;
        }
        catch (JSONException e) {
            e.printStackTrace();
            syncResult.stats.numParseExceptions++;
        }
    }

    protected boolean addFeatureOnServer(long featureId, SyncResult syncResult)
    {
        if(!mNet.isNetworkAvailable()){
            return false;
        }
        Uri uri = ContentUris.withAppendedId(mContentUri, featureId);
        uri = uri.buildUpon().fragment(NO_SYNC).build();

        Cursor cursor = query(uri, null, null, null, null);
        if(!cursor.moveToFirst())
            return false;

        try {
            String payload = cursorToJson(cursor);

            final HttpPost post = new HttpPost(NGWUtil.getVectorDataUrl(mURL, mRemoteId));
            //basic auth
            if (null != mLogin && mLogin.length() > 0 && null != mPassword &&
                mPassword.length() > 0) {
                post.setHeader("Accept", "*/*");
                final String basicAuth = "Basic " + Base64.encodeToString((mLogin + ":" + mPassword).getBytes(), Base64.NO_WRAP);
                post.setHeader("Authorization", basicAuth);
            }

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("json", payload));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            Log.d(TAG, "payload: " + payload);


            final DefaultHttpClient HTTPClient = mNet.getHttpClient();
            final HttpResponse response = HTTPClient.execute(post);

            // Check to see if we got success
            final org.apache.http.StatusLine line = response.getStatusLine();
            if (line.getStatusCode() != 200) {
                Log.d(TAG, "Problem downloading GeoJSON: " + mURL + " HTTP response: " +
                           line);
                syncResult.stats.numIoExceptions++;
                return false;
            }

            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                Log.d(TAG, "No content downloading GeoJSON: " + mURL);
                syncResult.stats.numIoExceptions++;
                return false;
            }

            String data = EntityUtils.toString(entity);
            Log.d(TAG, data);
            //TODO: set new id from server!


            return true;
        }
        catch (ClassNotFoundException | JSONException | IOException e){
            e.printStackTrace();
            return false;
        }
    }

    protected boolean deleteFeatureOnServer(long featureId, SyncResult syncResult)
    {
        if(!mNet.isNetworkAvailable()){
            return false;
        }

        try {

            final HttpDelete delete = new HttpDelete(NGWUtil.getFeatureUrl(mURL, mRemoteId, featureId));
            //basic auth
            if (null != mLogin && mLogin.length() > 0 && null != mPassword &&
                mPassword.length() > 0) {
                delete.setHeader("Accept", "*/*");
                final String basicAuth = "Basic " + Base64.encodeToString((mLogin + ":" + mPassword).getBytes(), Base64.NO_WRAP);
                delete.setHeader("Authorization", basicAuth);
            }

            final DefaultHttpClient HTTPClient = mNet.getHttpClient();
            final HttpResponse response = HTTPClient.execute(delete);

            // Check to see if we got success
            final org.apache.http.StatusLine line = response.getStatusLine();
            if (line.getStatusCode() != 200) {
                Log.d(TAG, "Problem downloading GeoJSON: " + mURL + " HTTP response: " +
                           line);
                syncResult.stats.numIoExceptions++;
                return false;
            }

            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                Log.d(TAG, "No content downloading GeoJSON: " + mURL);
                syncResult.stats.numIoExceptions++;
                return false;
            }

            String data = EntityUtils.toString(entity);
            Log.d(TAG, data);

            return true;
        }
        catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }

    protected boolean changeFeatureOnServer(long featureId, SyncResult syncResult)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }
        Uri uri = ContentUris.withAppendedId(mContentUri, featureId);
        uri = uri.buildUpon().fragment(NO_SYNC).build();

        Cursor cursor = query(uri, null, null, null, null);
        if (!cursor.moveToFirst())
            return false;

        try {
            String payload = cursorToJson(cursor);

            final HttpPut put = new HttpPut(NGWUtil.getFeatureUrl(mURL, mRemoteId, featureId));
            //basic auth
            if (null != mLogin && mLogin.length() > 0 && null != mPassword &&
                mPassword.length() > 0) {
                put.setHeader("Accept", "*/*");
                final String basicAuth = "Basic " + Base64.encodeToString((mLogin + ":" + mPassword).getBytes(), Base64.NO_WRAP);
                put.setHeader("Authorization", basicAuth);
            }

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("json", payload));
            put.setEntity(new UrlEncodedFormEntity(nameValuePairs));


            final DefaultHttpClient HTTPClient = mNet.getHttpClient();
            final HttpResponse response = HTTPClient.execute(put);

            // Check to see if we got success
            final org.apache.http.StatusLine line = response.getStatusLine();
            if (line.getStatusCode() != 200) {
                Log.d(TAG, "Problem downloading GeoJSON: " + mURL + " HTTP response: " +
                           line);
                syncResult.stats.numIoExceptions++;
                return false;
            }

            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                Log.d(TAG, "No content downloading GeoJSON: " + mURL);
                syncResult.stats.numIoExceptions++;
                return false;
            }

            String data = EntityUtils.toString(entity);
            Log.d(TAG, data);
            //TODO: set new id from server!


            return true;
        } catch (JSONException | ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected boolean sendPhotosOnServer(long featureId, SyncResult syncResult)
    {
        if(!mNet.isNetworkAvailable()){
            return false;
        }

        //TODO:

        return true;
    }

    protected String cursorToJson(Cursor cursor)
            throws JSONException, IOException, ClassNotFoundException
    {
        JSONObject rootObject = new JSONObject();
        JSONObject valueObject = new JSONObject();
        for(int i = 0; i < cursor.getColumnCount(); i++){
            String name = cursor.getColumnName(i);
            if(name.equals(FIELD_ID) || name.equals(FIELD_GEOM))
                continue;

            switch (mFields.get(cursor.getColumnName(i))) {
                case GeoConstants.FTReal:
                    valueObject.put(name, cursor.getFloat(i));
                    break;
                case GeoConstants.FTInteger:
                    valueObject.put(name, cursor.getInt(i));
                    break;
                case GeoConstants.FTString:
                    valueObject.put(name, cursor.getString(i));
                    break;
                case GeoConstants.FTDateTime:
                    valueObject.put(name, cursor.getLong(i));
                    break;
                default:
                    continue;
            }
        }
        rootObject.put("fields", valueObject);

        //may be found geometry in cache by id is faster
        GeoGeometry geometry = GeoGeometryFactory.fromBlob(
                cursor.getBlob(cursor.getColumnIndex(FIELD_GEOM)));

        rootObject.put("geom", geometry.toWKT(true));

        return rootObject.toString();
    }

    /**
     * get synchronization type
     * @return the synchronization type - the OR of this values:
     * SYNC_NONE - no synchronization
     * SYNC_DATA - synchronize only data
     * SYNC_PHOTO - synchronize only photo
     * SYNC_ALL - synchronize everything
     */
    public int getSyncType()
    {
        return mSyncType;
    }


    public void setSyncType(int syncType)
    {
        mSyncType = syncType;
    }
}
