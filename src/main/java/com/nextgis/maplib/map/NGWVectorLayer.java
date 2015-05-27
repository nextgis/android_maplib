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
import android.util.Log;
import android.widget.Toast;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.util.AttachItem;
import com.nextgis.maplib.util.FeatureChanges;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplib.util.VectorCacheItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;


public class NGWVectorLayer
        extends VectorLayer
        implements INGWLayer
{
    protected String mAccountName;
    protected String mCacheUrl;
    protected String mCacheLogin;
    protected String mCachePassword;

    protected long        mRemoteId;
    protected NetworkUtil mNet;
    protected int         mSyncType;
    //protected int mSyncDirection; //1 - to server only, 2 - from server only, 3 - both directions
    //check where to sync on GSM/WI-FI for data/attachments

    protected String mChangeTableName;

    protected static final String JSON_ACCOUNT_KEY   = "account";
    protected static final String JSON_SYNC_TYPE_KEY = "sync_type";


    public NGWVectorLayer(
            Context context,
            File path)
    {
        super(context, path);

        mNet = new NetworkUtil(context);

        // table name is the same as the folder name of the layer + "_changes"
        mChangeTableName = mPath.getName() + "_changes";
        mSyncType = SYNC_NONE;
        mLayerType = LAYERTYPE_NGW_VECTOR;
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


    public long getRemoteId()
    {
        return mRemoteId;
    }


    public void setRemoteId(long remoteId)
    {
        mRemoteId = remoteId;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_ACCOUNT_KEY, mAccountName);
        rootConfig.put(JSON_ID_KEY, mRemoteId);
        rootConfig.put(JSON_SYNC_TYPE_KEY, mSyncType);

        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException, SQLiteException
    {
        super.fromJSON(jsonObject);

        setAccountName(jsonObject.getString(JSON_ACCOUNT_KEY));

        mRemoteId = jsonObject.getLong(JSON_ID_KEY);
        if (jsonObject.has(JSON_SYNC_TYPE_KEY)) {
            mSyncType = jsonObject.getInt(JSON_SYNC_TYPE_KEY);
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
            String data = mNet.get(
                    NGWUtil.getResourceMetaUrl(mCacheUrl, mRemoteId), mCacheLogin, mCachePassword);
            if(null == data){
                return getContext().getString(R.string.error_download_data);
            }
            JSONObject geoJSONObject = new JSONObject(data);

            //fill field list
            JSONObject featureLayerJSONObject = geoJSONObject.getJSONObject("feature_layer");
            JSONArray fieldsJSONArray = featureLayerJSONObject.getJSONArray("fields");
            List<Field> fields = getFieldsFromJson(fieldsJSONArray);

            //fill SRS
            JSONObject vectorLayerJSONObject = null;
            if(geoJSONObject.has("vector_layer")){
                vectorLayerJSONObject = geoJSONObject.getJSONObject("vector_layer");
            }
            else if(geoJSONObject.has("postgis_layer")) {
                vectorLayerJSONObject = geoJSONObject.getJSONObject("postgis_layer");
            }
            if(null == vectorLayerJSONObject){
                return getContext().getString(R.string.error_download_data);
            }

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
            data = mNet.get(
                    NGWUtil.getFeaturesUrl(mCacheUrl, mRemoteId), mCacheLogin, mCachePassword);
            if(null == data){
                return getContext().getString(R.string.error_download_data);
            }

            JSONArray featuresJSONArray = new JSONArray(data);
            List<Feature> features = jsonToFeatures(featuresJSONArray, fields, nSRS);

            return initialize(fields, features, geomType);

        } catch (IOException e) {
            Log.d(
                    TAG, "Problem downloading GeoJSON: " + mCacheUrl + " Error: " +
                         e.getLocalizedMessage());
            return getContext().getString(R.string.error_download_data);
        } catch (JSONException | SQLiteException e) {
            e.printStackTrace();
            return getContext().getString(R.string.error_download_data);
        }
    }


    @Override
    public String initialize(
            List<Field> fields,
            List<Feature> features,
            int geometryType)
            throws SQLiteException
    {
        FeatureChanges.initialize(mChangeTableName);
        return super.initialize(fields, features, geometryType);
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
            long featureId,
            int operation)
    {
        if (0 == (mSyncType & SYNC_DATA)) {
            return;
        }

        boolean canAddChanges = true;

        // for delete operation
        if (operation == CHANGE_OPERATION_DELETE) {

            // if featureId == NOT_FOUND remove all changes for all features
            if (featureId == NOT_FOUND) {
                FeatureChanges.removeAllChanges(mChangeTableName);

                // if feature has changes then remove them for the feature
            } else if (FeatureChanges.isChanges(mChangeTableName, featureId)) {
                // if feature was new then just remove its changes
                canAddChanges = !FeatureChanges.isChanges(
                        mChangeTableName, featureId, CHANGE_OPERATION_NEW);
                FeatureChanges.removeChanges(mChangeTableName, featureId);
            }
        }

        // we are trying to re-create feature - warning
        if (operation == CHANGE_OPERATION_NEW &&
                FeatureChanges.isChanges(mChangeTableName, featureId)) {
            Log.w(TAG, "Something wrong. Should nether get here");
            canAddChanges = false;
        }

        // if can then add change
        if (canAddChanges) {
            FeatureChanges.add(mChangeTableName, featureId, operation);
        }
    }


    @Override
    protected void addChange(
            long featureId,
            long attachId,
            int attachOperation)
    {
        if (0 == (mSyncType & SYNC_ATTACH)) {
            return;
        }

        boolean canAddChanges = true;

        // for delete operation
        if (attachOperation == CHANGE_OPERATION_DELETE) {

            // if attachId == NOT_FOUND remove all attach changes for the feature
            if (attachId == NOT_FOUND) {
                FeatureChanges.removeAllAttachChanges(mChangeTableName, featureId);

                // if attachment has changes then remove them for the attachment
            } else if (FeatureChanges.isAttachChanges(mChangeTableName, featureId, attachId)) {
                // if attachment was new then just remove its changes
                canAddChanges = !FeatureChanges.isAttachChanges(
                        mChangeTableName, featureId, attachId, CHANGE_OPERATION_NEW);
                FeatureChanges.removeAttachChanges(mChangeTableName, featureId, attachId);
            }
        }

        // we are trying to re-create the attach - warning
        // TODO: replace to attachOperation == CHANGE_OPERATION_NEW ???
        if (0 != (attachOperation & CHANGE_OPERATION_NEW) &&
                FeatureChanges.isAttachChanges(mChangeTableName, featureId, attachId)) {
            Log.w(TAG, "Something wrong. Should nether get here");
            canAddChanges = false;
        }

        if (canAddChanges) {
            FeatureChanges.add(mChangeTableName, featureId, attachId, attachOperation);
        }
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

        // 1. get remote changes
        if (!getChangesFromServer(authority, syncResult)) {
            Log.d(TAG, "Get remote changes failed");
            return;
        }

        // 2. send current changes
        if (!sendLocalChanges(syncResult)) {
            Log.d(TAG, "Set local changes failed");
            return;
        }
    }


    protected boolean sendLocalChanges(SyncResult syncResult)
            throws SQLiteException
    {
        long changesCount = FeatureChanges.getChangeCount(mChangeTableName);
        Log.d(TAG, "sendLocalChanges: " + changesCount);

        if (0 == changesCount) {
            return true;
        }

        // get column's IDs, there is at least one entry
        Cursor changeCursor = FeatureChanges.getFirstChangeFromRecordId(mChangeTableName, 0);
        changeCursor.moveToFirst();

        int recordIdColumn = changeCursor.getColumnIndex(FIELD_ID);
        int featureIdColumn = changeCursor.getColumnIndex(FIELD_FEATURE_ID);
        int operationColumn = changeCursor.getColumnIndex(FIELD_OPERATION);
        int attachIdColumn = changeCursor.getColumnIndex(FIELD_ATTACH_ID);
        int attachOperationColumn = changeCursor.getColumnIndex(FIELD_ATTACH_OPERATION);

        long nextChangeRecordId = changeCursor.getLong(recordIdColumn);

        changeCursor.close();

        while (true) {

            changeCursor =
                    FeatureChanges.getFirstChangeFromRecordId(mChangeTableName, nextChangeRecordId);

            if (null == changeCursor) {
                break;
            }

            if (!changeCursor.moveToFirst()) {
                // no more change records
                changeCursor.close();
                break;
            }

            long changeRecordId = changeCursor.getLong(recordIdColumn);
            nextChangeRecordId = changeRecordId + 1;

            long changeFeatureId = changeCursor.getLong(featureIdColumn);
            int changeOperation = changeCursor.getInt(operationColumn);
            long changeAttachId = changeCursor.getLong(attachIdColumn);
            int changeAttachOperation = changeCursor.getInt(attachOperationColumn);

            changeCursor.close();

            long lastChangeRecordId = FeatureChanges.getLastChangeRecordId(mChangeTableName);

            if (0 == (changeOperation & CHANGE_OPERATION_ATTACH)) {

                if (0 != (changeOperation & CHANGE_OPERATION_DELETE)) {
                    if (deleteFeatureOnServer(changeFeatureId, syncResult)) {
                        FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                    } else {
                        Log.d(TAG, "proceed change failed");
                    }

                } else if (0 != (changeOperation & CHANGE_OPERATION_NEW)) {
                    if (addFeatureOnServer(changeFeatureId, syncResult)) {
                        FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                        FeatureChanges.removeChangesToLast(
                                mChangeTableName, changeFeatureId, CHANGE_OPERATION_CHANGED,
                                lastChangeRecordId);
                    } else {
                        Log.d(TAG, "proceed change failed");
                    }

                } else if (0 != (changeOperation & CHANGE_OPERATION_CHANGED)) {
                    if (changeFeatureOnServer(changeFeatureId, syncResult)) {
                        FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                        FeatureChanges.removeChangesToLast(
                                mChangeTableName, changeFeatureId, CHANGE_OPERATION_CHANGED,
                                lastChangeRecordId);
                    } else {
                        Log.d(TAG, "proceed change failed");
                    }
                }
            }

            //process attachments
            else { // 0 != (changeOperation & CHANGE_OPERATION_ATTACH)

                if (changeAttachOperation == CHANGE_OPERATION_DELETE) {
                    if (deleteAttachOnServer(changeFeatureId, changeAttachId, syncResult)) {
                        FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                    } else {
                        Log.d(TAG, "proceed change failed");
                    }

                } else if (changeAttachOperation == CHANGE_OPERATION_NEW) {
                    if (sendAttachOnServer(changeFeatureId, changeAttachId, syncResult)) {
                        FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                        FeatureChanges.removeAttachChangesToLast(
                                mChangeTableName, changeFeatureId, changeAttachId,
                                CHANGE_OPERATION_CHANGED, lastChangeRecordId);
                    } else {
                        Log.d(TAG, "proceed change failed");
                    }

                } else if (changeAttachOperation == CHANGE_OPERATION_CHANGED) {
                    if (changeAttachOnServer(changeFeatureId, changeAttachId, syncResult)) {
                        FeatureChanges.removeAttachChangesToLast(
                                mChangeTableName, changeFeatureId, changeAttachId,
                                CHANGE_OPERATION_CHANGED, lastChangeRecordId);
                    } else {
                        Log.d(TAG, "proceed change failed");
                    }
                }
            }
        }

        // check records count changing
        if (changesCount != FeatureChanges.getChangeCount(mChangeTableName)) {
            //notify to reload changes
            getContext().sendBroadcast(new Intent(SyncAdapter.SYNC_CHANGES));
        }

        return true;
    }


    private boolean changeAttachOnServer(
            long featureId,
            long attachId,
            SyncResult syncResult)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        AttachItem attach = getAttach("" + featureId, "" + attachId);
        if(null == attach) //just remove buggy item
            return true;

        try {
            JSONObject putData = new JSONObject();
            //putData.put(JSON_ID_KEY, attach.getAttachId());
            putData.put(JSON_NAME_KEY, attach.getDisplayName());
            //putData.put("mime_type", attach.getMimetype());
            putData.put("description", attach.getDescription());

            String data = mNet.put(
                    NGWUtil.getFeatureAttachmentUrl(mCacheUrl, mRemoteId, featureId) + attachId,
                    putData.toString(), mCacheLogin, mCachePassword);

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
            long featureId,
            long attachId,
            SyncResult syncResult)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        try{

            if (!mNet.delete(
                    NGWUtil.getFeatureAttachmentUrl(mCacheUrl, mRemoteId, featureId) + attachId,
                    mCacheLogin, mCachePassword)) {

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


    protected boolean sendAttachOnServer(long featureId,
            long attachId,
            SyncResult syncResult)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        AttachItem attach = getAttach("" + featureId, "" + attachId);
        if(null == attach) //just remove buggy item
            return true;

        String fileName = attach.getDisplayName();
        File filePath = new File(mPath, featureId + "/" + attach.getAttachId());
        String fileMime = attach.getMimetype();

        try {
            //1. upload file
            String data = mNet.postFile(
                    NGWUtil.getFileUploadUrl(mCacheUrl), fileName, filePath, fileMime, mCacheLogin,
                    mCachePassword);
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

            data = mNet.post(NGWUtil.getFeatureAttachmentUrl(mCacheUrl, mRemoteId, featureId),
                             postJsonData.toString(), mCacheLogin, mCachePassword);
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

        // read layer contents as string
        String data;
        try {
            data = mNet.get(
                    NGWUtil.getVectorDataUrl(mCacheUrl, mRemoteId), mCacheLogin, mCachePassword);
        }
        catch (IOException e) {
            e.printStackTrace();
            syncResult.stats.numParseExceptions++;
            return false;
        }

        if (null == data) {
            return false;
        }

        // parse layer contents to Feature list
        List<Feature> features;
        try {
            JSONArray featuresJSONArray = new JSONArray(data);
            features = jsonToFeatures(featuresJSONArray, getFields(), GeoConstants.CRS_WEB_MERCATOR);
        }
        catch (JSONException e) {
            e.printStackTrace();
            syncResult.stats.numParseExceptions++;
            return false;
        }

        Log.d(TAG, "Get " + features.size() + " feature(s) from server");

        // analyse feature
        for (Feature remoteFeature : features) {

            Cursor cursor = query(null, FIELD_ID + " = " + remoteFeature.getId(), null, null);
            //no local feature
            if (null == cursor || cursor.getCount() == 0) {

                //if we have changes (delete) not create new feature
                boolean createNewFeature =
                        !FeatureChanges.isChanges(mChangeTableName, remoteFeature.getId());

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
                // with the given ID (remoteFeature.getId()) must be only one feature
                Feature currentFeature = cursorToFeature(cursor);

                //compare features
                boolean eqData = remoteFeature.equalsData(currentFeature);
                boolean eqAttach = remoteFeature.equalsAttachments(currentFeature);

                //process data
                if (eqData) {
                    //remove from changes

                    if (FeatureChanges.isChanges(mChangeTableName, remoteFeature.getId())) {

                        if (eqAttach && !FeatureChanges.isAttachesForDelete(
                                mChangeTableName, remoteFeature.getId()) ||
                                !FeatureChanges.isAttachChanges(
                                        mChangeTableName, remoteFeature.getId())) {

                            FeatureChanges.removeChanges(mChangeTableName, remoteFeature.getId());
                        }
                    }

                } else {

                    // we have local changes ready for sent to server
                    boolean isChangedLocal =
                            FeatureChanges.isChanges(mChangeTableName, remoteFeature.getId());

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

                    if (FeatureChanges.isChanges(mChangeTableName, remoteFeature.getId()) &&
                            (eqData || FeatureChanges.isAttachChanges(
                                    mChangeTableName, remoteFeature.getId()))) {

                        Log.d(TAG, "The feature " + remoteFeature.getId() +
                                      " already changed on server. Remove changes for it");

                        FeatureChanges.removeChanges(mChangeTableName, remoteFeature.getId());
                    }

                } else {
                    boolean isChangedLocal =
                            FeatureChanges.isAttachChanges(mChangeTableName, remoteFeature.getId());

                    if (!isChangedLocal) {
                        Iterator<String> iterator =
                                currentFeature.getAttachments().keySet().iterator();

                        while (iterator.hasNext()) {
                            String attachId = iterator.next();

                            //delete attachment which not exist on server
                            if (!remoteFeature.getAttachments().containsKey(attachId)) {
                                iterator.remove();
                                saveAttach("" + currentFeature.getId());

                            } else { //or change attachment properties
                                AttachItem currentItem =
                                        currentFeature.getAttachments().get(attachId);
                                AttachItem remoteItem =
                                        remoteFeature.getAttachments().get(attachId);

                                if (null != currentItem && !currentItem.equals(remoteItem)) {
                                    long attachIdL = Long.parseLong(remoteItem.getAttachId());
                                    boolean changeOnServer = !FeatureChanges.isAttachChanges(
                                            mChangeTableName, remoteFeature.getId(), attachIdL);

                                    if (changeOnServer) {
                                        currentItem.setDescription(remoteItem.getDescription());
                                        currentItem.setMimetype(remoteItem.getMimetype());
                                        currentItem.setDisplayName(remoteItem.getDisplayName());
                                    }
                                }
                            }
                        }
                    }

                    saveAttach("" + currentFeature.getId());
                }
            }

            if (null != cursor) {
                cursor.close();
            }
        }

        // remove features not exist on server from local layer
        // if no operation is in changes array or change operation for local feature present

        try {

            for (VectorCacheItem item : mVectorCacheItems) {
                boolean bDeleteFeature = true;
                for (Feature remoteFeature : features) {
                    if (remoteFeature.getId() == item.getId()) {
                        bDeleteFeature = false;
                        break;
                    }
                }

                // if local item is in update list and state ADD_NEW skip delete
                bDeleteFeature = bDeleteFeature && !FeatureChanges.isChanges(
                        mChangeTableName, item.getId(), CHANGE_OPERATION_NEW);

                if (bDeleteFeature) {
                    Log.d(TAG, "Delete feature #" + item.getId() + " not exist on server");
                    delete(item.getId(), FIELD_ID + " = " + item.getId(), null);
                }
            }

            Cursor changeCursor = FeatureChanges.getChanges(mChangeTableName);

            // remove changes already applied on server (delete already deleted id or add already added)
            if (null != changeCursor) {

                if (changeCursor.moveToFirst()) {
                    int recordIdColumn = changeCursor.getColumnIndex(FIELD_ID);
                    int featureIdColumn = changeCursor.getColumnIndex(FIELD_FEATURE_ID);
                    int operationColumn = changeCursor.getColumnIndex(FIELD_OPERATION);

                    do {
                        long changeRecordId = changeCursor.getLong(recordIdColumn);
                        long changeFeatureId = changeCursor.getLong(featureIdColumn);
                        int changeOperation = changeCursor.getInt(operationColumn);

                        boolean bDeleteChange = true; // if feature not exist on server
                        for (Feature remoteFeature : features) {
                            if(remoteFeature.getId() == changeFeatureId){
                                if (0 != (changeOperation & CHANGE_OPERATION_NEW)) {
                                    // if feature already exist, just change it
                                    FeatureChanges.setOperation(
                                            mChangeTableName, changeRecordId,
                                            CHANGE_OPERATION_CHANGED);
                                }
                                bDeleteChange = false; // in other cases just apply
                                break;
                            }
                        }

                        if (0 != (changeOperation & CHANGE_OPERATION_NEW) && bDeleteChange) {
                            bDeleteChange = false;
                        }

                        if (bDeleteChange) {
                            Log.d(TAG,
                                  "Delete change for feature #" + changeFeatureId + " operation " +
                                          changeOperation);
                            // TODO: analise for operation, remove all equal
                            FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                        }

                    } while (changeCursor.moveToNext());
                }

                changeCursor.close();
            }
        }
        catch (ConcurrentModificationException e){
            e.printStackTrace();
            return false;
        }

        return true;
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
            if (null != cursor) {
                cursor.close();
            }
            return true; //just remove buggy data
        }

        try {
            String payload = cursorToJson(cursor);
            cursor.close();
            String data = mNet.post(
                    NGWUtil.getVectorDataUrl(mCacheUrl, mRemoteId), payload, mCacheLogin,
                    mCachePassword);
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
            if (!mNet.delete(
                    NGWUtil.getFeatureUrl(mCacheUrl, mRemoteId, featureId), mCacheLogin,
                    mCachePassword)) {

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
            if (null != cursor) {
                cursor.close();
            }
            return true; //just remove buggy data
        }

        try {
            String payload = cursorToJson(cursor);
            cursor.close();
            Log.d(TAG, "payload: " + payload);
            String data = mNet.put(
                    NGWUtil.getFeatureUrl(mCacheUrl, mRemoteId, featureId), payload, mCacheLogin,
                    mCachePassword);
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
            mSyncType = syncType;
            FeatureChanges.removeAllChanges(mChangeTableName);
        } else if (mSyncType == SYNC_NONE && 0 != (syncType & SYNC_DATA)) {
            mSyncType = syncType;
            for (VectorCacheItem cacheItem : mVectorCacheItems) {
                long id = cacheItem.getId();
                addChange(id, CHANGE_OPERATION_NEW);
                //add attach
                File attacheFolder = new File(mPath, "" + id);
                if(attacheFolder.isDirectory()){
                    for(File attach : attacheFolder.listFiles()){
                        String attachId = attach.getName();
                        if(attachId.equals(META))
                            continue;
                        Long attachIdL = Long.parseLong(attachId);
                        if (attachIdL >= 1000) {
                            addChange(id, attachIdL, CHANGE_OPERATION_NEW);
                        }
                    }
                }
            }
        }
        else {
            mSyncType = syncType;
        }
    }


    @Override
    public boolean delete()
            throws SQLiteException
    {
        FeatureChanges.delete(mChangeTableName);

        return super.delete();
    }
}
