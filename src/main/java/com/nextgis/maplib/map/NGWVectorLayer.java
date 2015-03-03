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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.util.AttachItem;
import com.nextgis.maplib.util.ChangeFeatureItem;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplib.util.VectorCacheItem;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;


public class NGWVectorLayer
        extends VectorLayer
        implements INGWLayer
{
    protected String      mAccountName;
    protected String      mURL;
    protected long        mRemoteId;
    protected NetworkUtil mNet;
    protected String      mLogin;
    protected String      mPassword;
    protected int         mSyncType;
    //protected int mSyncDirection; //1 - to server only, 2 - from server only, 3 - both directions
    //check where to sync on GSM/WI-FI for data/attachments

    protected List<ChangeFeatureItem> mChanges;

    protected static final String JSON_ACCOUNT_KEY   = "account";
    protected static final String JSON_URL_KEY       = "url";
    protected static final String JSON_LOGIN_KEY     = "login";
    protected static final String JSON_PASSWORD_KEY  = "password";
    protected static final String JSON_SYNC_TYPE_KEY = "sync_type";


    public NGWVectorLayer(
            Context context,
            File path)
    {
        super(context, path);

        mNet = new NetworkUtil(context);

        mChanges = new ArrayList<>();
        mSyncType = SYNC_NONE;
        mLayerType = LAYERTYPE_NGW_VECTOR;
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


    public long getRemoteId()
    {
        return mRemoteId;
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
            throws JSONException, SQLiteException
    {
        super.fromJSON(jsonObject);
        mAccountName = jsonObject.getString(JSON_ACCOUNT_KEY);
        mURL = jsonObject.getString(JSON_URL_KEY);
        mRemoteId = jsonObject.getLong(JSON_ID_KEY);
        if (jsonObject.has(JSON_LOGIN_KEY)) {
            mLogin = jsonObject.getString(JSON_LOGIN_KEY);
        }
        if (jsonObject.has(JSON_PASSWORD_KEY)) {
            mPassword = jsonObject.getString(JSON_PASSWORD_KEY);
        }
        if (jsonObject.has(JSON_SYNC_TYPE_KEY)) {
            mSyncType = jsonObject.getInt(JSON_SYNC_TYPE_KEY);
        }

        loadChanges(jsonObject);

        if (!mIsInitialized) {
            //init in separate thread
            downloadAsync();
        }
    }


    protected void loadChanges(JSONObject jsonObject)
            throws JSONException, SQLiteException
    {
        if (jsonObject.has(JSON_CHANGES_KEY)) {
            mChanges.clear();
            JSONArray array = jsonObject.getJSONArray(JSON_CHANGES_KEY);
            for (int i = 0; i < array.length(); i++) {
                JSONObject change = array.getJSONObject(i);
                ChangeFeatureItem item = new ChangeFeatureItem(0, 0);
                item.fromJSON(change);
                mChanges.add(item);
            }
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


    public static List<Field> getFieldsFromJson(JSONArray fieldsJSONArray)
            throws JSONException
    {
        List<Field> fields = new ArrayList<>();
        for (int i = 0; i < fieldsJSONArray.length(); i++) {
            JSONObject fieldJSONObject = fieldsJSONArray.getJSONObject(i);
            String type = fieldJSONObject.getString("datatype");
            String alias = fieldJSONObject.getString("display_name");
            String name = fieldJSONObject.getString("keyname");

            int nType = stringToType(type);
            if (NOT_FOUND != nType) {
                fields.add(new Field(nType, name, alias));
            }
        }
        return fields;
    }


    /**
     * download and create new NGW layer from GeoJSON data
     *
     * @return the error message or null if everything is ok
     */
    public String download()
    {
        if (!mNet.isNetworkAvailable()) { //return tile from cache
            return getContext().getString(R.string.error_network_unavailable);
        }

        try {
            Log.d(TAG, "download layer " + getName());
            String data = mNet.get(NGWUtil.getResourceMetaUrl(mURL, mRemoteId), mLogin, mPassword);
            if(null == data){
                return getContext().getString(R.string.error_download_data);
            }
            JSONObject geoJSONObject = new JSONObject(data);

            //fill field list
            JSONObject featureLayerJSONObject = geoJSONObject.getJSONObject("feature_layer");
            JSONArray fieldsJSONArray = featureLayerJSONObject.getJSONArray("fields");
            List<Field> fields = getFieldsFromJson(fieldsJSONArray);

            //fill SRS
            JSONObject vectorLayerJSONObject = geoJSONObject.getJSONObject("vector_layer");
            String geomTypeString = vectorLayerJSONObject.getString("geometry_type");
            int geomType = GeoGeometryFactory.typeFromString(geomTypeString);
            if (geomType < 4) {
                geomType += 3;
            }
            JSONObject srs = vectorLayerJSONObject.getJSONObject("srs");
            int nSRS = srs.getInt("id");
            if (nSRS != GeoConstants.CRS_WEB_MERCATOR && nSRS != GeoConstants.CRS_WGS84) {
                return getContext().getString(R.string.error_crs_unsupported);
            }

            //get layer data
            data = mNet.get(NGWUtil.getFeaturesUrl(mURL, mRemoteId), mLogin, mPassword);
            if(null == data){
                return getContext().getString(R.string.error_download_data);
            }

            JSONArray featuresJSONArray = new JSONArray(data);
            List<Feature> features = jsonToFeatures(featuresJSONArray, fields, nSRS);

            return initialize(fields, features, geomType);

        } catch (IOException e) {
            Log.d(
                    TAG, "Problem downloading GeoJSON: " + mURL + " Error: " +
                         e.getLocalizedMessage());
            return getContext().getString(R.string.error_download_data);
        } catch (JSONException | SQLiteException e) {
            e.printStackTrace();
            return getContext().getString(R.string.error_download_data);
        }
    }


    protected List<Feature> jsonToFeatures(
            JSONArray featuresJSONArray,
            List<Field> fields,
            int nSRS)
            throws JSONException
    {
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < featuresJSONArray.length(); i++) {
            JSONObject featureJSONObject = featuresJSONArray.getJSONObject(i);
            long id = featureJSONObject.getLong(JSON_ID_KEY);
            String wkt = featureJSONObject.getString("geom");
            JSONObject fieldsJSONObject = featureJSONObject.getJSONObject(JSON_FIELDS_KEY);
            Feature feature = new Feature(id, fields);
            GeoGeometry geom = GeoGeometryFactory.fromWKT(wkt);
            if (null == geom) {
                continue;
            }
            geom.setCRS(nSRS);
            if (nSRS != GeoConstants.CRS_WEB_MERCATOR) {
                geom.project(GeoConstants.CRS_WEB_MERCATOR);
            }
            feature.setGeometry(geom);

            for (Field field : fields) {
                if (field.getType() == GeoConstants.FTDateTime) {
                    if (!fieldsJSONObject.isNull(field.getName())) {
                        JSONObject dateJson = fieldsJSONObject.getJSONObject(field.getName());
                        int nYear = dateJson.getInt("year");
                        int nMonth = dateJson.getInt("month");
                        int nDay = dateJson.getInt("day");
                        Calendar calendar = new GregorianCalendar(nYear, nMonth - 1, nDay);
                        feature.setFieldValue(field.getName(), calendar.getTime());
                    }
                } else {
                    if (!fieldsJSONObject.isNull(field.getName())) {
                        feature.setFieldValue(
                                field.getName(), fieldsJSONObject.get(field.getName()));
                    }
                }
            }

            //add extensions
            if(featureJSONObject.has("extensions")){
                JSONObject ext = featureJSONObject.getJSONObject("extensions");
                //get attachment & description
                if(!ext.isNull("attachment")) {
                    JSONArray attachment = ext.getJSONArray("attachment");
                    for (int j = 0; j < attachment.length(); j++) {
                        JSONObject jsonAttachmentDetails = attachment.getJSONObject(j);
                        String attachId = "" + jsonAttachmentDetails.getLong(JSON_ID_KEY);
                        String name = jsonAttachmentDetails.getString(JSON_NAME_KEY);
                        String mime = jsonAttachmentDetails.getString("mime_type");
                        String descriptionText = jsonAttachmentDetails.getString("description");
                        AttachItem item = new AttachItem(attachId, name, mime, descriptionText);
                        feature.addAttachment(item);
                    }
                }
            }

            features.add(feature);
        }
        return features;
    }


    protected static int stringToType(String type)
    {
        switch (type) {
            case "STRING":
                return GeoConstants.FTString;
            case "INTEGER":
                return GeoConstants.FTInteger;
            case "DATE":
                return GeoConstants.FTDateTime;
            case "REAL":
                return GeoConstants.FTReal;
            default:
                return NOT_FOUND;
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
            if (null != error && error.length() > 0) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void addChange(
            String featureId,
            int operation)
    {
        if (0 == (mSyncType & SYNC_DATA)) {
            return;
        }

        //1. if featureId == NOT_FOUND remove all changes and add this one
        if (featureId.equals("" + NOT_FOUND) && operation == ChangeFeatureItem.TYPE_DELETE) {
            mChanges.clear();
            mChanges.add(new ChangeFeatureItem(NOT_FOUND, operation));
        } else {
            int id = Integer.parseInt(featureId);
            for (int i = 0; i < mChanges.size(); i++) {
                ChangeFeatureItem item = mChanges.get(i);
                if (item.getFeatureId() == id) {
                    //2. if featureId == some id and op is delete - remove and other operations
                    if (operation == ChangeFeatureItem.TYPE_DELETE) {
                        if (0 != (item.getOperation() & ChangeFeatureItem.TYPE_DELETE)) {
                            return;
                        }
                        mChanges.remove(i);
                        if (0 != (item.getOperation() & ChangeFeatureItem.TYPE_NEW)) {
                            save();
                            return;
                        }
                        i--;
                    }
                    //3. if featureId == some id and op is update and previous op was add or update - skip
                    else if (operation == ChangeFeatureItem.TYPE_CHANGED) {
                        if (0 != (item.getOperation() & ChangeFeatureItem.TYPE_CHANGED) ||
                            0 != (item.getOperation() & ChangeFeatureItem.TYPE_NEW)) {
                            return;
                        } else {
                            item.setOperation(operation);
                            save();
                            return;
                        }
                    }
                    //4. if featureId == some id and op is add and value present - warning
                    else if (operation == ChangeFeatureItem.TYPE_NEW) {
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
    protected void addChange(
            String featureId,
            String attachId,
            int operation)
    {
        if (0 == (mSyncType & SYNC_ATTACH)) {
            return;
        }

        int id = Integer.parseInt(featureId);
        for (int i = 0; i < mChanges.size(); i++) {
            ChangeFeatureItem item = mChanges.get(i);
            if (item.getFeatureId() == id) {
                if (item.getOperation() == ChangeFeatureItem.TYPE_DELETE) {
                    return;
                } else {
                    item.addAttachChange(attachId, operation);
                    save();
                    return;
                }
            }
        }
        ChangeFeatureItem item = new ChangeFeatureItem(id, ChangeFeatureItem.TYPE_ATTACH);
        item.addAttachChange(attachId, operation);
        mChanges.add(item);
        save();
    }


    /**
     * Synchronize changes with NGW. Should be run from non UI thread.
     *
     * @param authority
     * @param syncResult
     *         - report some errors via this parameter
     */
    public void sync(
            String authority,
            SyncResult syncResult)
    {
        if (0 != (mSyncType & SYNC_NONE) || !mIsInitialized) {
            return;
        }

        try {
            //0. get changes from layer config file
            JSONObject jsonObject = new JSONObject(FileUtil.readFromFile(getFileName()));
            loadChanges(jsonObject);

            //1. get remote changes
            if (!getChangesFromServer(authority, syncResult)) {
                Log.d(TAG, "Get remote changes failed");
                return;
            }

            //2. send current changes
            if (!sendLocalChanges(syncResult)) {
                Log.d(TAG, "Set local changes failed");
                return;
            }

            Log.d(TAG, "save sendLocalChanges: " + mChanges.size());
            save();

        } catch (JSONException | IOException | SQLiteException e) {
            e.printStackTrace();
        }
    }


    protected boolean sendLocalChanges(SyncResult syncResult)
            throws SQLiteException
    {
        int changesCount = mChanges.size();
        Log.d(TAG, "sendLocalChanges: " + changesCount);
        for (int i = 0; i < mChanges.size(); i++) {
            ChangeFeatureItem change = mChanges.get(i);
            if(0 != (change.getOperation() & ChangeFeatureItem.TYPE_NEW)){
                if (!addFeatureOnServer(change.getFeatureId(), syncResult)) {
                    Log.d(TAG, "proceed change failed");
                    continue;
                }
            }
            else if(0 != (change.getOperation() & ChangeFeatureItem.TYPE_CHANGED)) {
                if (!changeFeatureOnServer(change.getFeatureId(), syncResult)) {
                    Log.d(TAG, "proceed change failed");
                    continue;
                }
            }
            else if(0 != (change.getOperation() & ChangeFeatureItem.TYPE_DELETE)) {
                if (!deleteFeatureOnServer(change.getFeatureId(), syncResult)) {
                    Log.d(TAG, "proceed change failed");
                    continue;
                }
            }
            //process attachments
            if(0 != (change.getOperation() & ChangeFeatureItem.TYPE_ATTACH)) {
                boolean hasErrors = false;
                List<ChangeFeatureItem.ChangeAttachItem> attachItems = change.getAttachItems();
                for(int j = 0; j < attachItems.size(); j++){
                    ChangeFeatureItem.ChangeAttachItem attachItem = attachItems.get(j);
                    if(attachItem.getOperation() == ChangeFeatureItem.TYPE_NEW){
                        if(!sendAttachOnServer(change.getFeatureId(), attachItem, syncResult)){
                            hasErrors = true;
                            Log.d(TAG, "proceed change failed");
                        }
                    }
                    else if(attachItem.getOperation() == ChangeFeatureItem.TYPE_DELETE){
                        if(!deleteAttachOnServer(change.getFeatureId(), attachItem, syncResult)){
                            hasErrors = true;
                            Log.d(TAG, "proceed change failed");
                        }
                    }
                    else if(attachItem.getOperation() == ChangeFeatureItem.TYPE_CHANGED){
                        if(!changeAttachOnServer(change.getFeatureId(), attachItem, syncResult)){
                            hasErrors = true;
                            Log.d(TAG, "proceed change failed");
                        }
                    }

                    if(hasErrors)
                        break;
                    attachItems.remove(j);
                    j--;
                    Log.d(TAG, "proceed attache change. leave " + attachItems.size());
                }

                if(hasErrors)
                    continue;
            }

            mChanges.remove(i);
            i--;
            Log.d(TAG, "proceed change. leave " + mChanges.size());
        }

        if (changesCount != mChanges.size()) {
            //notify to reload changes
            getContext().sendBroadcast(new Intent(SyncAdapter.SYNC_CHANGES));
        }

        return true;
    }


    private boolean changeAttachOnServer(
            int featureId,
            ChangeFeatureItem.ChangeAttachItem attachItem,
            SyncResult syncResult)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        AttachItem attach = getAttach("" + featureId, "" + attachItem.getAttachId());
        if(null == attach) //just remove buggy item
            return true;

        try {
            JSONObject putData = new JSONObject();
            //putData.put(JSON_ID_KEY, attach.getAttachId());
            putData.put(JSON_NAME_KEY, attach.getDisplayName());
            //putData.put("mime_type", attach.getMimetype());
            putData.put("description", attach.getDescription());

            String data = mNet.put(NGWUtil.getFeatureAttachmentUrl(mURL, mRemoteId, featureId) + attachItem.getAttachId(),
                                   putData.toString(), mLogin, mPassword);
            if(null == data){
                syncResult.stats.numIoExceptions++;
                return false;
            }

            return true;
        }
        catch (JSONException | IOException e) {
            e.printStackTrace();
            Log.d(TAG, e.getLocalizedMessage());
            syncResult.stats.numIoExceptions++;
            return false;
        }
    }


    private boolean deleteAttachOnServer(
            int featureId,
            ChangeFeatureItem.ChangeAttachItem attachItem,
            SyncResult syncResult)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        try{
            if(!mNet.delete(NGWUtil.getFeatureAttachmentUrl(mURL, mRemoteId, featureId) + attachItem.getAttachId(), mLogin, mPassword)){
                syncResult.stats.numIoExceptions++;
                return false;
            }

            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, e.getLocalizedMessage());
            syncResult.stats.numIoExceptions++;
            return false;
        }
    }


    protected boolean sendAttachOnServer(int featureId,
            ChangeFeatureItem.ChangeAttachItem attachItem,
            SyncResult syncResult)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        AttachItem attach = getAttach("" + featureId, "" + attachItem.getAttachId());
        if(null == attach) //just remove buggy item
            return true;
        String fileName = attach.getDisplayName();
        File filePath = new File(mPath, featureId + "/" + attach.getAttachId());
        String fileMime = attach.getMimetype();
        try {
            //1. upload file
            String data = mNet.postFile(NGWUtil.getFileUploadUrl(mURL), fileName, filePath, fileMime,
                                        mLogin, mPassword);
            if(null == data) {
                syncResult.stats.numIoExceptions++;
                return false;
            }
            JSONObject result = new JSONObject(data);
            if(!result.has("upload_meta")){
                syncResult.stats.numIoExceptions++;
                return false;
            }

            JSONArray uploadMetaArray = result.getJSONArray("upload_meta");
            if(uploadMetaArray.length() == 0){
                syncResult.stats.numIoExceptions++;
                return false;
            }
            //2. add attachment to row
            JSONObject postJsonData = new JSONObject();
            postJsonData.put("file_upload", uploadMetaArray.get(0));
            postJsonData.put("description", attach.getDescription());

            data = mNet.post(NGWUtil.getFeatureAttachmentUrl(mURL, mRemoteId, featureId),
                             postJsonData.toString(), mLogin, mPassword);
            if(null == data) {
                syncResult.stats.numIoExceptions++;
                return false;
            }

            result = new JSONObject(data);
            if(!result.has(JSON_ID_KEY)) {
                syncResult.stats.numIoExceptions++;
                return false;
            }

            long newAttachId = result.getLong(JSON_ID_KEY);
            setNewAttachId("" + featureId, attach, "" + newAttachId);

            return true;

        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Log.d(TAG, e.getLocalizedMessage());
            syncResult.stats.numIoExceptions++;
            return false;
        }
    }


    protected void changeFeatureId(
            long oldFeatureId,
            long newFeatureId)
    {
        if (oldFeatureId == newFeatureId) {
            return;
        }

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }
        //update id in DB
        Log.d(TAG, "old id: " + oldFeatureId + " new id: " + newFeatureId);
        SQLiteDatabase db = map.getDatabase(false);
        ContentValues values = new ContentValues();
        values.put(FIELD_ID, newFeatureId);
        if (db.update(mPath.getName(), values, FIELD_ID + " = " + oldFeatureId, null) != 1) {
            Log.d(TAG, "failed to set new id");
        }

        //update id in cache
        for (VectorCacheItem cacheItem : mVectorCacheItems) {
            if (cacheItem.getId() == oldFeatureId) {
                cacheItem.setId(newFeatureId);
            }
        }

        //rename photo id folder if exist
        File photoFolder = new File(mPath, "" + oldFeatureId);
        if (photoFolder.exists()) {
            if (photoFolder.renameTo(new File(mPath, "" + newFeatureId))) {
                Log.d(TAG, "rename photo folder " + oldFeatureId + "failed");
            }
        }
    }


    protected boolean getChangesFromServer(
            String authority,
            SyncResult syncResult)
            throws SQLiteException
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        Log.d(TAG, "The network is available. Get changes from server");

        try {
            String data = mNet.get(NGWUtil.getVectorDataUrl(mURL, mRemoteId), mLogin, mPassword);
            JSONArray featuresJSONArray = new JSONArray(data);
            List<Feature> features =
                    jsonToFeatures(featuresJSONArray, getFields(), GeoConstants.CRS_WEB_MERCATOR);
            Log.d(TAG, "Get " + features.size() + " feature(s) from server");

            for (Feature remoteFeature : features) {
                Cursor cursor = query(
                        null, VectorLayer.FIELD_ID + " = " + remoteFeature.getId(), null, null);
                //no local feature
                if (null == cursor || cursor.getCount() == 0) {

                    boolean createNewFeature = true;
                    for (ChangeFeatureItem change : mChanges) {
                        if (change.getFeatureId() == remoteFeature.getId()) {
                            createNewFeature = false; //if have changes (delete) not create new feature
                            break;
                        }
                    }
                    //create new feature with remoteId
                    if (createNewFeature) {
                        ContentValues values = remoteFeature.getContentValues(true);
                        Uri uri = Uri.parse("content://" + authority + "/" + getPath().getName());
                        //prevent add changes and events
                        uri = uri.buildUpon().fragment(NO_SYNC).build();
                        Uri newFeatureUri = insert(uri, values);
                        Log.d(TAG, "Add new feature from server - " + newFeatureUri.toString());
                    }
                } else {
                    cursor.moveToFirst();
                    Feature currentFeature = cursorToFeature(cursor);
                    //compare features

                    boolean eqData = remoteFeature.equalsData(currentFeature);
                    boolean eqAttach = remoteFeature.equalsArrachments(currentFeature);

                    //process data
                    if (eqData) {
                        //remove from changes
                        for (int i = 0; i < mChanges.size(); i++) {
                            ChangeFeatureItem change = mChanges.get(i);
                            if (change.getFeatureId() == remoteFeature.getId() && (eqAttach || 0 == (change.getOperation() & ChangeFeatureItem.TYPE_ATTACH))) {
                                boolean isDelete = false;
                                for(ChangeFeatureItem.ChangeAttachItem attachItem : change.getAttachItems()){
                                    if(attachItem.getOperation() == ChangeFeatureItem.TYPE_DELETE){
                                        isDelete = true;
                                        break;
                                    }
                                }
                                if(!isDelete) {
                                    Log.d(TAG, "The feature " + change.getFeatureId() +
                                               " already changed on server. Remove change set #" +
                                               i);
                                    mChanges.remove(i);
                                    i--;
                                }
                            }
                        }
                    } else {
                        boolean isChangedLocal = false;
                        for (ChangeFeatureItem change : mChanges) {
                            if (change.getFeatureId() == remoteFeature.getId()) {
                                isChangedLocal = true; //we have local changes ready for sent to server
                                break;
                            }
                        }

                        //no local changes - update local feature
                        if (!isChangedLocal) {
                            ContentValues values = remoteFeature.getContentValues(false);

                            Uri uri =
                                    Uri.parse("content://" + authority + "/" + getPath().getName());
                            Uri updateUri = ContentUris.withAppendedId(uri, remoteFeature.getId());
                            updateUri = updateUri.buildUpon().fragment(NO_SYNC).build();
                            //prevent add changes
                            int count = update(updateUri, values, null, null);
                            Log.d(TAG, "Update feature (" + count + ") from server - " +
                                         remoteFeature.getId());
                        }
                    }
                    //process attachments
                    if(eqAttach){
                        for (int i = 0; i < mChanges.size(); i++) {
                            ChangeFeatureItem change = mChanges.get(i);
                            if (change.getFeatureId() == remoteFeature.getId() && (eqData || change.getOperation() == ChangeFeatureItem.TYPE_ATTACH)) {
                                Log.d(TAG, "The feature " + change.getFeatureId() +
                                           " already changed on server. Remove change set #" + i);
                                mChanges.remove(i);
                                i--;
                            }
                        }
                    }
                    else {
                        for(String attachId : currentFeature.getAttachments().keySet()){
                            //delete attachment which not exist on server
                            if(!remoteFeature.getAttachments().containsKey(attachId)){
                                deleteAttach("" + currentFeature.getId(), attachId);
                            }
                            //or change attachment properties
                            else{
                                AttachItem currentItem = currentFeature.getAttachments().get(attachId);
                                AttachItem remoteItem = remoteFeature.getAttachments().get(attachId);
                                if(null != currentItem && !currentItem.equals(remoteItem)){
                                    boolean changeOnServer = true;
                                    for (ChangeFeatureItem change : mChanges) {
                                        if(change.getFeatureId() == remoteFeature.getId() && 0 != (change.getOperation() & ChangeFeatureItem.TYPE_ATTACH)){
                                            for(ChangeFeatureItem.ChangeAttachItem attachItem : change.getAttachItems()){
                                                if(remoteItem.getAttachId().equals(""+attachItem.getAttachId())){
                                                    changeOnServer = false;
                                                }
                                            }
                                        }
                                        if(!changeOnServer)
                                            break;
                                    }
                                    if(changeOnServer) {
                                        currentItem.setDescription(remoteItem.getDescription());
                                        currentItem.setMimetype(remoteItem.getMimetype());
                                        currentItem.setDisplayName(remoteItem.getDisplayName());
                                    }
                                }
                            }
                        }
                        saveAttach("" + currentFeature.getId());
                    }

                }
            }
            //TODO: remove features not exist on server from local layer if no operation is in changes array
            return true;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            syncResult.stats.numParseExceptions++;
        }
        return false;
    }


    protected Feature cursorToFeature(Cursor cursor)
    {
        Feature out = new Feature((long) NOT_FOUND, getFields());
        out.fromCursor(cursor);
        //add extensions to feature
        out.addAttachments(getAttachMap("" + out.getId()));
        return out;
    }


    protected boolean addFeatureOnServer(
            long featureId,
            SyncResult syncResult)
            throws SQLiteException
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }
        Uri uri = ContentUris.withAppendedId(mContentUri, featureId);
        uri = uri.buildUpon().fragment(NO_SYNC).build();

        Cursor cursor = query(uri, null, null, null, null);
        if (null == cursor || !cursor.moveToFirst()) {
            Log.d(TAG, "addFeatureOnServer: Get cursor failed");
            return true; //just remove buggy data
        }

        try {
            String payload = cursorToJson(cursor);
            String data = mNet.post(NGWUtil.getVectorDataUrl(mURL, mRemoteId), payload, mLogin, mPassword);
            if(null == data){
                syncResult.stats.numIoExceptions++;
                return false;
            }
            //set new id from server! {"id": 24}
            JSONObject result = new JSONObject(data);
            if (result.has(JSON_ID_KEY)) {
                long id = result.getLong(JSON_ID_KEY);
                changeFeatureId(featureId, id);
            }

            return true;
        } catch (ClassNotFoundException | JSONException | IOException e) {
            e.printStackTrace();
            Log.d(TAG, e.getLocalizedMessage());
            return false;
        }
    }


    protected boolean deleteFeatureOnServer(
            long featureId,
            SyncResult syncResult)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        try {
            if(!mNet.delete(NGWUtil.getFeatureUrl(mURL, mRemoteId, featureId), mLogin, mPassword)){
                syncResult.stats.numIoExceptions++;
                return false;
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    protected boolean changeFeatureOnServer(
            long featureId,
            SyncResult syncResult)
            throws SQLiteException
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }
        Uri uri = ContentUris.withAppendedId(mContentUri, featureId);
        uri = uri.buildUpon().fragment(NO_SYNC).build();

        Cursor cursor = query(uri, null, null, null, null);
        if (null == cursor || !cursor.moveToFirst()) {
            Log.d(TAG, "empty cursor for uri: " + uri);
            return true; //just remove buggy data
        }

        try {
            String payload = cursorToJson(cursor);
            Log.d(TAG, "payload: " + payload);
            String data = mNet.put(NGWUtil.getFeatureUrl(mURL, mRemoteId, featureId), payload, mLogin, mPassword);
            if(null == data){
                syncResult.stats.numIoExceptions++;
                return false;
            }

            return true;
        } catch (JSONException | ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected String cursorToJson(Cursor cursor)
            throws JSONException, IOException, ClassNotFoundException
    {
        JSONObject rootObject = new JSONObject();
        if (0 != (mSyncType & SYNC_ATTRIBUTES)) {
            JSONObject valueObject = new JSONObject();
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                String name = cursor.getColumnName(i);
                if (name.equals(FIELD_ID) || name.equals(FIELD_GEOM)) {
                    continue;
                }

                Field field = mFields.get(cursor.getColumnName(i));
                if (null == field) {
                    continue;
                }

                switch (field.getType()) {
                    case GeoConstants.FTReal:
                        valueObject.put(name, cursor.getFloat(i));
                        break;
                    case GeoConstants.FTInteger:
                        valueObject.put(name, cursor.getInt(i));
                        break;
                    case GeoConstants.FTString:
                        String stringVal = cursor.getString(i);
                        if (null != stringVal && !stringVal.equals("null")) {
                            valueObject.put(name, stringVal);
                        }
                        break;
                    case GeoConstants.FTDateTime:
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(cursor.getLong(i));
                        int nYear = calendar.get(Calendar.YEAR);
                        int nMonth = calendar.get(Calendar.MONTH) + 1;
                        int nDay = calendar.get(Calendar.DAY_OF_MONTH);
                        JSONObject date = new JSONObject();
                        date.put("year", nYear);
                        date.put("month", nMonth);
                        date.put("day", nDay);
                        valueObject.put(name, date);
                        break;
                    default:
                        break;
                }
            }
            rootObject.put("fields", valueObject);
        }

        if (0 != (mSyncType & SYNC_GEOMETRY)) {
            //may be found geometry in cache by id is faster
            GeoGeometry geometry =
                    GeoGeometryFactory.fromBlob(cursor.getBlob(cursor.getColumnIndex(FIELD_GEOM)));

            rootObject.put("geom", geometry.toWKT(true));
            //rootObject.put("id", cursor.getLong(cursor.getColumnIndex(FIELD_ID)));
        }

        return rootObject.toString();
    }


    /**
     * get synchronization type
     *
     * @return the synchronization type - the OR of this values: SYNC_NONE - no synchronization
     * SYNC_DATA - synchronize only data
     * SYNC_ATTACH - synchronize only attachments
     * SYNC_ALL - synchronize everything
     */
    public int getSyncType()
    {
        return mSyncType;
    }


    public void setSyncType(int syncType)
    {
        if (mSyncType == syncType) {
            return;
        }
        if (syncType == SYNC_NONE) {
            mChanges.clear();
        } else if (mSyncType == SYNC_NONE && 0 != (syncType & SYNC_DATA)) {
            for (VectorCacheItem cacheItem : mVectorCacheItems) {
                long id = cacheItem.getId();
                addChange("" + id, ChangeFeatureItem.TYPE_NEW);
                //add attach
                File attacheFolder = new File(mPath, "" + id);
                if(attacheFolder.isDirectory()){
                    for(File attach : attacheFolder.listFiles()){
                        String attachId = attach.getName();
                        if(attachId.equals(META))
                            continue;
                        Long attachIdL = Long.parseLong(attachId);
                        if(attachIdL >= 1000)
                            addChange("" + id, attachId, ChangeFeatureItem.TYPE_NEW);
                    }
                }
            }
        }

        mSyncType = syncType;
    }
}
