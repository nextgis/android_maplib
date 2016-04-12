/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
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
import android.os.Build;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.util.AttachItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.DatabaseContext;
import com.nextgis.maplib.util.FeatureChanges;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplib.util.ProgressBufferedInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import static com.nextgis.maplib.util.Constants.*;


public class NGWVectorLayer
        extends VectorLayer
        implements INGWLayer
{
    protected static final String JSON_ACCOUNT_KEY       = "account";
    protected static final String JSON_SYNC_TYPE_KEY     = "sync_type";
    protected static final String JSON_NGWLAYER_TYPE_KEY = "ngw_layer_type";
    protected static final String JSON_SERVERWHERE_KEY   = "server_where";

    protected static final int TYPE_CHANGES_TABLE     = 125;
    protected static final int TYPE_CHANGES_FEATURE   = 126;
    protected static final int TYPE_CHANGES_ATTACH    = 127;
    protected static final int TYPE_CHANGES_ATTACH_ID = 128;

    protected static boolean mIsAddedToUriMatcher = false;

    protected NetworkUtil mNet;

    protected String mAccountName;
    protected long   mRemoteId;
    protected int    mSyncType;
    protected int    mNGWLayerType;
    protected String mServerWhere;
    //protected int mSyncDirection; //1 - to server only, 2 - from server only, 3 - both directions
    //check where to sync on GSM/WI-FI for data/attachments


    public NGWVectorLayer(
            Context context,
            File path)
    {
        super(context, path);

        if (null == mNet) {
            mNet = new NetworkUtil(context);
        }

        mSyncType = Constants.SYNC_NONE;
        mLayerType = Constants.LAYERTYPE_NGW_VECTOR;
        mNGWLayerType = Connection.NGWResourceTypeNone;

        if (!mIsAddedToUriMatcher) {
            // get changes for all rows
            mUriMatcher.addURI(mAuthority, "*/" + URI_CHANGES, TYPE_CHANGES_TABLE);

            // get changes for single row
            mUriMatcher.addURI(mAuthority, "*/" + URI_CHANGES + "/#", TYPE_CHANGES_FEATURE);

            //get changes for all attaches of row
            mUriMatcher.addURI(
                    mAuthority, "*/" + URI_CHANGES + "/#/" + URI_ATTACH, TYPE_CHANGES_ATTACH);

            //get changes for single attach by id
            mUriMatcher.addURI(mAuthority, "*/" + URI_CHANGES + "/#/" + URI_ATTACH + "/#",
                    TYPE_CHANGES_ATTACH_ID);

            mIsAddedToUriMatcher = true;
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


    public String getRemoteUrl()
    {
        AccountData accountData = getAccountData(mContext, mAccountName);
        return NGWUtil.getResourceUrl(accountData.url, mRemoteId);
    }


    @Override
    public void setRemoteId(long remoteId)
    {
        mRemoteId = remoteId;
    }


    public String getServerWhere()
    {
        return mServerWhere;
    }


    public void setServerWhere(String serverWhere)
    {
        mServerWhere = serverWhere;
    }


    public String getChangeTableName()
    {
        return mPath.getName() + Constants.CHANGES_NAME_POSTFIX;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_ACCOUNT_KEY, mAccountName);
        rootConfig.put(Constants.JSON_ID_KEY, mRemoteId);
        rootConfig.put(JSON_SYNC_TYPE_KEY, mSyncType);
        rootConfig.put(JSON_NGWLAYER_TYPE_KEY, mNGWLayerType);
        rootConfig.put(JSON_SERVERWHERE_KEY, mServerWhere);

        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException, SQLiteException
    {
        super.fromJSON(jsonObject);

        setAccountName(jsonObject.getString(JSON_ACCOUNT_KEY));

        mRemoteId = jsonObject.getLong(Constants.JSON_ID_KEY);
        if (jsonObject.has(JSON_SYNC_TYPE_KEY)) {
            mSyncType = jsonObject.getInt(JSON_SYNC_TYPE_KEY);
        }

        if (jsonObject.has(JSON_NGWLAYER_TYPE_KEY)) {
            mNGWLayerType = jsonObject.getInt(JSON_NGWLAYER_TYPE_KEY);
        }

        if (jsonObject.has(JSON_SERVERWHERE_KEY)) {
            mServerWhere = jsonObject.getString(JSON_SERVERWHERE_KEY);
        }
    }


    @Override
    public void setAccountCacheData()
    {
        // do nothing
    }


    @Override
    protected long insertInternal(ContentValues contentValues)
    {
        if (!contentValues.containsKey(Constants.FIELD_ID)) {
            long id = getUniqId();
            if (MIN_LOCAL_FEATURE_ID > id) {
                id = MIN_LOCAL_FEATURE_ID;
            }
            contentValues.put(FIELD_ID, id);
        }

        return super.insertInternal(contentValues);
    }


    @Override
    protected long insertAttach(
            String featureId,
            ContentValues contentValues)
    {
        if (contentValues.containsKey(ATTACH_DISPLAY_NAME) && contentValues.containsKey(
                ATTACH_MIME_TYPE)) {
            //get attach path
            File attachFolder = new File(mPath, featureId);
            //we start files from MIN_LOCAL_FEATURE_ID to not overlap with NGW files id's
            long maxId = MIN_LOCAL_FEATURE_ID;
            if (attachFolder.isDirectory()) {
                for (File attachFile : attachFolder.listFiles()) {
                    if (attachFile.getName().equals(META)) {
                        continue;
                    }
                    long val = Long.parseLong(attachFile.getName());
                    if (val >= maxId) {
                        maxId = val + 1;
                    }
                }
            } else {
                FileUtil.createDir(attachFolder);
            }

            File attachFile = new File(attachFolder, "" + maxId);
            try {
                if (attachFile.createNewFile()) {
                    //create new record in attaches - description, mime_type, ext
                    String displayName = contentValues.getAsString(ATTACH_DISPLAY_NAME);
                    String mimeType = contentValues.getAsString(ATTACH_MIME_TYPE);
                    String description = "";

                    if (contentValues.containsKey(ATTACH_DESCRIPTION)) {
                        description = contentValues.getAsString(ATTACH_DESCRIPTION);
                    }

                    AttachItem item =
                            new AttachItem("" + maxId, displayName, mimeType, description);
                    addAttach(featureId, item);

                    return maxId;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return NOT_FOUND;
    }


    /**
     * download and create new NGW layer from GeoJSON data
     */
    public void createFromNGW(IProgressor progressor)
            throws NGException, IOException, JSONException, SQLiteException
    {
        AccountData accountData = getAccountData(mContext, mAccountName);

        if (null == accountData.url) {
            throw new NGException(getContext().getString(R.string.error_download_data));
        }

        if (!mNet.isNetworkAvailable()) { //return tile from cache
            throw new NGException(getContext().getString(R.string.error_network_unavailable));
        }

        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "download layer " + getName());
        }
        String data = NetworkUtil.get(NGWUtil.getResourceMetaUrl(accountData.url, mRemoteId),
                accountData.login, accountData.password);
        if (null == data) {
            throw new NGException(getContext().getString(R.string.error_download_data));
        }
        JSONObject geoJSONObject = new JSONObject(data);

        //fill field list
        JSONObject featureLayerJSONObject = geoJSONObject.getJSONObject("feature_layer");
        JSONArray fieldsJSONArray = featureLayerJSONObject.getJSONArray(NGWUtil.NGWKEY_FIELDS);
        List<Field> fields = NGWUtil.getFieldsFromJson(fieldsJSONArray);

        //fill SRS
        JSONObject vectorLayerJSONObject = null;
        if (geoJSONObject.has("vector_layer")) {
            vectorLayerJSONObject = geoJSONObject.getJSONObject("vector_layer");
            mNGWLayerType = Connection.NGWResourceTypeVectorLayer;
        } else if (geoJSONObject.has("postgis_layer")) {
            vectorLayerJSONObject = geoJSONObject.getJSONObject("postgis_layer");
            mNGWLayerType = Connection.NGWResourceTypePostgisLayer;
        }
        if (null == vectorLayerJSONObject) {
            throw new NGException(getContext().getString(R.string.error_download_data));
        }

        String geomTypeString = vectorLayerJSONObject.getString("geometry_type");
        int geomType = GeoGeometryFactory.typeFromString(geomTypeString);
        JSONObject srs = vectorLayerJSONObject.getJSONObject("srs");
        int nSRS = srs.getInt("id");
        if (nSRS != GeoConstants.CRS_WEB_MERCATOR && nSRS != GeoConstants.CRS_WGS84) {
            throw new NGException(getContext().getString(R.string.error_crs_unsupported));
        }

        create(geomType, fields);

        String sURL = NGWUtil.getFeaturesUrl(accountData.url, mRemoteId, mServerWhere);
        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "download features from: " + sURL);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {

            //get layer data
            data = NetworkUtil.get(sURL, accountData.login, accountData.password);
            if (null == data) {
                throw new NGException(getContext().getString(R.string.error_download_data));
            }

            JSONArray featuresJSONArray = new JSONArray(data);
            if (null != progressor) {
                progressor.setMessage(getContext().getString(R.string.parse_features));
            }
            List<Feature> features =
                    NGWUtil.jsonToFeatures(featuresJSONArray, fields, nSRS, progressor);

            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "feature count: " + features.size());
            }

            if (null != progressor) {
                progressor.setMessage(getContext().getString(R.string.create_features));
                progressor.setMax(features.size());
                progressor.setIndeterminate(false);
            }
            int featureCount = 0;

            for (Feature feature : features) {
                createFeature(feature);
                if (null != progressor) {
                    if (progressor.isCanceled()) {
                        break;
                    }
                    progressor.setValue(featureCount++);
                    progressor.setMessage(
                            getContext().getString(R.string.processed) + " " + featureCount + " "
                                    + getContext().getString(R.string.of) + " " + features.size());
                }
            }

            notifyLayerChanged();
        } else {
            // get features and fill them
            URL url = new URL(sURL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            final String basicAuth =
                    NetworkUtil.getHTTPBaseAuth(accountData.login, accountData.password);
            if (null != basicAuth) {
                urlConnection.setRequestProperty("Authorization", basicAuth);
            }

            InputStream in = new ProgressBufferedInputStream(urlConnection.getInputStream(),
                    urlConnection.getContentLength());
            JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            reader.beginArray();

            SQLiteDatabase db = DatabaseContext.getDbForLayer(this);

            int streamSize = in.available();

            if (null != progressor) {
                progressor.setIndeterminate(false);
                progressor.setMax(streamSize);
                progressor.setMessage(
                        getContext().getString(R.string.start_fill_layer) + " " + getName());
            }

            int featureCount = 0;
            while (reader.hasNext()) {
                final Feature feature = NGWUtil.readNGWFeature(reader, fields, nSRS);
                createFeatureBatch(feature, db);

                if (null != progressor) {
                    if (progressor.isCanceled()) {
                        save();
                        return;
                    }
                    progressor.setValue(streamSize - in.available());
                    progressor.setMessage(getContext().getString(R.string.process_features) + ": " +
                            featureCount);
                }

                ++featureCount;
            }
            reader.endArray();
            reader.close();
            //db.close();

            urlConnection.disconnect();

            save();

            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "feature count: " + featureCount);
            }
        }
    }


    @Override
    public void create(
            int geometryType,
            List<Field> fields)
            throws SQLiteException
    {
//        if(geometryType < 4 && mNGWLayerType == Connection.NGWResourceTypeVectorLayer) // to multi
//            geometryType += 3;

        super.create(geometryType, fields);
        FeatureChanges.initialize(getChangeTableName());
    }


    @Override
    public void addChange(
            long featureId,
            int operation)
    {
        if (0 == (mSyncType & Constants.SYNC_DATA)) {
            return;
        }

        String changeTableName = getChangeTableName();
        boolean canAddChanges = true;

        // for delete operation
        if (operation == Constants.CHANGE_OPERATION_DELETE) {

            // if featureId == NOT_FOUND remove all changes for all features
            if (featureId == Constants.NOT_FOUND) {
                FeatureChanges.removeAllChanges(changeTableName);

                // if feature has changes then remove them for the feature
            } else if (FeatureChanges.isChanges(changeTableName, featureId)) {
                // if feature was new then just remove its changes
                canAddChanges = !FeatureChanges.isChanges(changeTableName, featureId,
                        Constants.CHANGE_OPERATION_NEW);
                FeatureChanges.removeChanges(changeTableName, featureId);
            }
        }

        // we are trying to re-create feature - warning
        if (operation == Constants.CHANGE_OPERATION_NEW && FeatureChanges.isChanges(
                changeTableName, featureId)) {
            Log.w(Constants.TAG, "Something wrong. Should nether get here");
            canAddChanges = false;
        }

        // if can then add change
        if (canAddChanges) {
            FeatureChanges.add(changeTableName, featureId, operation);
        }
    }


    @Override
    public void addChange(
            long featureId,
            long attachId,
            int attachOperation)
    {
        if (0 == (mSyncType & Constants.SYNC_ATTACH)) {
            return;
        }

        String changeTableName = getChangeTableName();
        boolean canAddChanges = true;

        // for delete operation
        if (attachOperation == Constants.CHANGE_OPERATION_DELETE) {

            // if attachId == NOT_FOUND remove all attach changes for the feature
            if (attachId == Constants.NOT_FOUND) {
                FeatureChanges.removeAllAttachChanges(changeTableName, featureId);

                // if attachment has changes then remove them for the attachment
            } else if (FeatureChanges.isAttachChanges(changeTableName, featureId, attachId)) {
                // if attachment was new then just remove its changes
                canAddChanges =
                        !FeatureChanges.isAttachChanges(changeTableName, featureId, attachId,
                                Constants.CHANGE_OPERATION_NEW);
                FeatureChanges.removeAttachChanges(changeTableName, featureId, attachId);
            }
        }

        // we are trying to re-create the attach - warning
        // TODO: replace to attachOperation == CHANGE_OPERATION_NEW ???
        if (0 != (attachOperation & Constants.CHANGE_OPERATION_NEW)
                && FeatureChanges.isAttachChanges(changeTableName, featureId, attachId)) {
            Log.w(Constants.TAG, "Something wrong. Should nether get here");
            canAddChanges = false;
        }

        if (canAddChanges) {
            FeatureChanges.add(changeTableName, featureId, attachId, attachOperation);
        }
    }


    /**
     * Synchronize changes with NGW. Should be run from non UI thread.
     *
     * @param authority
     *         - a content resolver authority (i.e. com.nextgis.mobile.provider)
     * @param syncResult
     *         - report some errors via this parameter
     */
    @Override
    public void sync(
            String authority,
            SyncResult syncResult)
    {
        syncResult.clear();
        if (0 != (mSyncType & Constants.SYNC_NONE) || mFields == null || mFields.isEmpty()) {
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG,
                        "Layer " + getName() + " is not checked to sync or not inited");
            }
            return;
        }

        // 1. get remote changes
        if (!getChangesFromServer(authority, syncResult)) {
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "Get remote changes failed");
            }
            return;
        }

        if (isRemoteReadOnly()) {
            return;
        }

        // 2. send current changes
        if (!sendLocalChanges(syncResult)) {
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "Set local changes failed");
            }
            //return;
        }
    }


    public boolean sendLocalChanges(SyncResult syncResult)
    {
        String changeTableName = getChangeTableName();
        long changesCount = FeatureChanges.getChangeCount(changeTableName);
        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "sendLocalChanges: " + changesCount);
        }

        if (0 == changesCount) {
            return true;
        }

        boolean isError = false;

        try {
            // get column's IDs, there is at least one entry
            Cursor changeCursor = FeatureChanges.getFirstChangeFromRecordId(changeTableName, 0);
            changeCursor.moveToFirst();

            int recordIdColumn = changeCursor.getColumnIndex(Constants.FIELD_ID);
            int featureIdColumn = changeCursor.getColumnIndex(Constants.FIELD_FEATURE_ID);
            int operationColumn = changeCursor.getColumnIndex(Constants.FIELD_OPERATION);
            int attachIdColumn = changeCursor.getColumnIndex(Constants.FIELD_ATTACH_ID);
            int attachOperationColumn =
                    changeCursor.getColumnIndex(Constants.FIELD_ATTACH_OPERATION);

            long nextChangeRecordId = changeCursor.getLong(recordIdColumn);

            changeCursor.close();

            while (true) {

                changeCursor = FeatureChanges.getFirstChangeFromRecordId(changeTableName,
                        nextChangeRecordId);

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

                long lastChangeRecordId = FeatureChanges.getLastChangeRecordId(changeTableName);

                if (0 == (changeOperation & Constants.CHANGE_OPERATION_ATTACH)) {

                    if (0 != (changeOperation & Constants.CHANGE_OPERATION_DELETE)) {
                        if (deleteFeatureOnServer(changeFeatureId, syncResult)) {
                            FeatureChanges.removeChangeRecord(changeTableName, changeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            if (Constants.DEBUG_MODE) {
                                Log.d(Constants.TAG, "proceed deleteFeatureOnServer() failed");
                            }
                        }

                    } else if (0 != (changeOperation & Constants.CHANGE_OPERATION_NEW)) {
                        if (addFeatureOnServer(changeFeatureId, syncResult)) {
                            FeatureChanges.removeChangeRecord(changeTableName, changeRecordId);
                            FeatureChanges.removeChangesToLast(changeTableName, changeFeatureId,
                                    Constants.CHANGE_OPERATION_CHANGED, lastChangeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            if (Constants.DEBUG_MODE) {
                                Log.d(Constants.TAG, "proceed addFeatureOnServer() failed");
                            }
                        }

                    } else if (0 != (changeOperation & Constants.CHANGE_OPERATION_CHANGED)) {
                        if (changeFeatureOnServer(changeFeatureId, syncResult)) {
                            FeatureChanges.removeChangeRecord(changeTableName, changeRecordId);
                            FeatureChanges.removeChangesToLast(changeTableName, changeFeatureId,
                                    Constants.CHANGE_OPERATION_CHANGED, lastChangeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            if (Constants.DEBUG_MODE) {
                                Log.d(Constants.TAG, "proceed changeFeatureOnServer() failed");
                            }
                        }
                    }
                }

                //process attachments
                else { // 0 != (changeOperation & CHANGE_OPERATION_ATTACH)

                    if (changeAttachOperation == Constants.CHANGE_OPERATION_DELETE) {
                        if (deleteAttachOnServer(changeFeatureId, changeAttachId, syncResult)) {
                            FeatureChanges.removeChangeRecord(changeTableName, changeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            if (Constants.DEBUG_MODE) {
                                Log.d(Constants.TAG, "proceed deleteAttachOnServer() failed");
                            }
                        }

                    } else if (changeAttachOperation == Constants.CHANGE_OPERATION_NEW) {
                        if (sendAttachOnServer(changeFeatureId, changeAttachId, syncResult)) {
                            FeatureChanges.removeChangeRecord(changeTableName, changeRecordId);
                            FeatureChanges.removeAttachChangesToLast(changeTableName,
                                    changeFeatureId, changeAttachId,
                                    Constants.CHANGE_OPERATION_CHANGED, lastChangeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            if (Constants.DEBUG_MODE) {
                                Log.d(Constants.TAG, "proceed sendAttachOnServer() failed");
                            }
                        }

                    } else if (changeAttachOperation == Constants.CHANGE_OPERATION_CHANGED) {
                        if (changeAttachOnServer(changeFeatureId, changeAttachId, syncResult)) {
                            FeatureChanges.removeAttachChangesToLast(changeTableName,
                                    changeFeatureId, changeAttachId,
                                    Constants.CHANGE_OPERATION_CHANGED, lastChangeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            if (Constants.DEBUG_MODE) {
                                Log.d(Constants.TAG, "proceed changeAttachOnServer() failed");
                            }
                        }
                    }
                }
            }

            // check records count changing
            if (changesCount != FeatureChanges.getChangeCount(changeTableName)) {
                mCache.save(new File(mPath, RTREE));
                //notify to reload changes
                getContext().sendBroadcast(new Intent(SyncAdapter.SYNC_CHANGES));
            }

        } catch (SQLiteException e) {
            isError = true;
            syncResult.stats.numConflictDetectedExceptions++;
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "proceed sendLocalChanges() failed");
            }
            e.printStackTrace();
        }

        return !isError;
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
        if (null == attach) //just remove buggy item
        {
            return true;
        }

        AccountData accountData = getAccountData(mContext, mAccountName);

        try {
            JSONObject putData = new JSONObject();
            //putData.put(JSON_ID_KEY, attach.getAttachId());
            putData.put(Constants.JSON_NAME_KEY, attach.getDisplayName());
            //putData.put("mime_type", attach.getMimetype());
            putData.put("description", attach.getDescription());

            String data = NetworkUtil.put(
                    NGWUtil.getFeatureAttachmentUrl(accountData.url, mRemoteId, featureId)
                            + attachId, putData.toString(), accountData.login,
                    accountData.password);

            if (null == data) {
                syncResult.stats.numIoExceptions++;
                return false;
            }

            return true;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, e.getLocalizedMessage());
            }
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

        AccountData accountData = getAccountData(mContext, mAccountName);

        try {

            if (!NetworkUtil.delete(
                    NGWUtil.getFeatureAttachmentUrl(accountData.url, mRemoteId, featureId)
                            + attachId, accountData.login, accountData.password)) {

                syncResult.stats.numIoExceptions++;
                return false;
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, e.getLocalizedMessage());
            }
            syncResult.stats.numIoExceptions++;
            return false;
        }
    }


    protected boolean sendAttachOnServer(
            long featureId,
            long attachId,
            SyncResult syncResult)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        AttachItem attach = getAttach("" + featureId, "" + attachId);
        if (null == attach) //just remove buggy item
        {
            return true;
        }

        String fileName = attach.getDisplayName();
        File filePath = new File(mPath, featureId + File.separator + attach.getAttachId());
        String fileMime = attach.getMimetype();

        AccountData accountData = getAccountData(mContext, mAccountName);

        try {
            //1. upload file
            String data = NetworkUtil.postFile(NGWUtil.getFileUploadUrl(accountData.url), fileName,
                    filePath, fileMime, accountData.login, accountData.password);
            if (null == data) {
                syncResult.stats.numIoExceptions++;
                return false;
            }
            JSONObject result = new JSONObject(data);
            if (!result.has("upload_meta")) {
                if (Constants.DEBUG_MODE) {
                    Log.d(
                            Constants.TAG,
                            "Problem sendAttachOnServer(), result has not upload_meta, result: "
                                    + result.toString());
                }
                syncResult.stats.numIoExceptions++;
                return false;
            }

            JSONArray uploadMetaArray = result.getJSONArray("upload_meta");
            if (uploadMetaArray.length() == 0) {
                if (Constants.DEBUG_MODE) {
                    Log.d(
                            Constants.TAG,
                            "Problem sendAttachOnServer(), result upload_meta length() == 0");
                }
                syncResult.stats.numIoExceptions++;
                return false;
            }
            //2. add attachment to row
            JSONObject postJsonData = new JSONObject();
            postJsonData.put("file_upload", uploadMetaArray.get(0));
            postJsonData.put("description", attach.getDescription());

            String postload = postJsonData.toString();
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "postload: " + postload);
            }

            data = NetworkUtil.post(
                    NGWUtil.getFeatureAttachmentUrl(accountData.url, mRemoteId, featureId),
                    postload, accountData.login, accountData.password);
            if (null == data) {
                syncResult.stats.numIoExceptions++;
                return false;
            }

            result = new JSONObject(data);
            if (!result.has(Constants.JSON_ID_KEY)) {
                if (Constants.DEBUG_MODE) {
                    Log.d(
                            Constants.TAG,
                            "Problem sendAttachOnServer(), result has not ID key, result: " + result
                                    .toString());
                }
                syncResult.stats.numIoExceptions++;
                return false;
            }

            long newAttachId = result.getLong(Constants.JSON_ID_KEY);
            setNewAttachId("" + featureId, attach, "" + newAttachId);

            return true;

        } catch (JSONException | IOException e) {
            e.printStackTrace();
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, e.getLocalizedMessage());
            }
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
        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "old id: " + oldFeatureId + " new id: " + newFeatureId);
        }
        SQLiteDatabase db = map.getDatabase(false);
        ContentValues values = new ContentValues();
        values.put(Constants.FIELD_ID, newFeatureId);
        if (db.update(mPath.getName(), values, Constants.FIELD_ID + " = " + oldFeatureId, null)
                != 1) {
            Log.w(Constants.TAG, "failed to set new id");
        }

        //update id in cache
        Intent notify = new Intent(Constants.NOTIFY_UPDATE);
        notify.putExtra(Constants.FIELD_OLD_ID, oldFeatureId);
        notify.putExtra(Constants.FIELD_ID, newFeatureId);
        notify.putExtra(Constants.ATTRIBUTES_ONLY, true);
        notify.putExtra(Constants.NOTIFY_LAYER_NAME, mPath.getName());
        getContext().sendBroadcast(notify);

        //rename photo id folder if exist
        File photoFolder = new File(mPath, "" + oldFeatureId);
        if (photoFolder.exists()) {
            if (photoFolder.renameTo(new File(mPath, "" + newFeatureId))) {

                int chRes = FeatureChanges.changeFeatureIdForAttaches(getChangeTableName(),
                        oldFeatureId, newFeatureId);
                if (chRes <= 0) {
                    if (Constants.DEBUG_MODE) {
                        Log.d(Constants.TAG,
                                "Feature ID for attaches not changed, oldFeatureId: " + oldFeatureId
                                        + ", newFeatureId: " + newFeatureId);
                    }
                }

            } else {
                if (Constants.DEBUG_MODE) {
                    Log.d(Constants.TAG, "rename photo folder " + oldFeatureId + "failed");
                }
            }
        }
    }


    public boolean getChangesFromServer(
            String authority,
            SyncResult syncResult)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        AccountData accountData = getAccountData(mContext, mAccountName);

        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "The network is available. Get changes from server");
        }
        List<Feature> features;
        // read layer contents as string
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {

            String data;
            try {
                data = NetworkUtil.get(
                        NGWUtil.getFeaturesUrl(accountData.url, mRemoteId, mServerWhere),
                        accountData.login, accountData.password);
            } catch (IOException e) {
                e.printStackTrace();
                syncResult.stats.numParseExceptions++;
                return false;
            } catch (NGException e) {
                e.printStackTrace();
                syncResult.stats.numIoExceptions++;
                return false;
            }

            if (null == data) {
                syncResult.stats.numIoExceptions++;
                return false;
            }

            // parse layer contents to Feature list
            try {
                JSONArray featuresJSONArray = new JSONArray(data);
                features = NGWUtil.jsonToFeatures(featuresJSONArray, getFields(),
                        GeoConstants.CRS_WEB_MERCATOR, null);
            } catch (JSONException e) {
                e.printStackTrace();
                syncResult.stats.numParseExceptions++;
                return false;
            }
        } else {
            try {
                URL url = new URL(NGWUtil.getFeaturesUrl(accountData.url, mRemoteId, mServerWhere));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                final String basicAuth =
                        NetworkUtil.getHTTPBaseAuth(accountData.login, accountData.password);
                if (null != basicAuth) {
                    urlConnection.setRequestProperty("Authorization", basicAuth);
                }

                InputStream in = new ProgressBufferedInputStream(urlConnection.getInputStream(),
                        urlConnection.getContentLength());
                JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
                features = new LinkedList<>();
                reader.beginArray();
                while (reader.hasNext()) {
                    final Feature feature = NGWUtil.readNGWFeature(reader, getFields(),
                            GeoConstants.CRS_WEB_MERCATOR);
                    features.add(feature);
                }
                reader.endArray();
                reader.close();

                urlConnection.disconnect();

            } catch (MalformedURLException e) {
                e.printStackTrace();
                syncResult.stats.numParseExceptions++;
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                syncResult.stats.numParseExceptions++;
                return false;
            }
        }


        try {
            if (!mCacheLoaded) {
                reloadCache();
            }

            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "Get " + features.size() + " feature(s) from server");
            }

            String changeTableName = getChangeTableName();

            // analyse feature
            for (Feature remoteFeature : features) {

                Cursor cursor =
                        query(null, Constants.FIELD_ID + " = " + remoteFeature.getId(), null, null,
                                null);
                try {
                    //no local feature
                    if (null == cursor || cursor.getCount() == 0) {

                        //if we have changes (delete) not create new feature
                        boolean createNewFeature =
                                !FeatureChanges.isChanges(changeTableName, remoteFeature.getId());

                        //create new feature with remoteId
                        if (createNewFeature) {
                            ContentValues values = remoteFeature.getContentValues(true);
                            Uri uri =
                                    Uri.parse("content://" + authority + "/" + getPath().getName());
                            //prevent add changes and events
                            uri = uri.buildUpon().fragment(NO_SYNC).build();
                            Uri newFeatureUri = insert(uri, values);
                            if (Constants.DEBUG_MODE) {
                                Log.d(
                                        Constants.TAG, "Add new feature from server - "
                                                + newFeatureUri.toString());
                            }
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

                            if (FeatureChanges.isChanges(changeTableName, remoteFeature.getId())) {

                                if (eqAttach && !FeatureChanges.isAttachesForDelete(
                                        changeTableName, remoteFeature.getId())
                                        || !FeatureChanges.isAttachChanges(
                                        changeTableName, remoteFeature.getId())) {

                                    FeatureChanges.removeChanges(
                                            changeTableName, remoteFeature.getId());
                                }
                            }

                        } else {

                            // we have local changes ready for sent to server
                            boolean isChangedLocal = FeatureChanges.isChanges(changeTableName,
                                    remoteFeature.getId());

                            //no local changes - update local feature
                            if (!isChangedLocal) {
                                ContentValues values = remoteFeature.getContentValues(false);

                                Uri uri = Uri.parse(
                                        "content://" + authority + "/" + getPath().getName());
                                Uri updateUri =
                                        ContentUris.withAppendedId(uri, remoteFeature.getId());
                                updateUri = updateUri.buildUpon().fragment(NO_SYNC).build();
                                //prevent add changes
                                int count = update(updateUri, values, null, null);
                                if (Constants.DEBUG_MODE) {
                                    Log.d(Constants.TAG,
                                            "Update feature (" + count + ") from server - " +
                                                    remoteFeature.getId());
                                }
                            }
                        }

                        //process attachments
                        if (eqAttach) {

                            if (FeatureChanges.isChanges(changeTableName, remoteFeature.getId())
                                    && (eqData || FeatureChanges.isAttachChanges(
                                    changeTableName, remoteFeature.getId()))) {

                                if (Constants.DEBUG_MODE) {
                                    Log.d(Constants.TAG, "The feature " + remoteFeature.getId() +
                                            " already changed on server. Remove changes for it");
                                }

                                FeatureChanges.removeChanges(
                                        changeTableName, remoteFeature.getId());
                            }

                        } else {
                            boolean isChangedLocal = FeatureChanges.isAttachChanges(changeTableName,
                                    remoteFeature.getId());

                            if (!isChangedLocal) {
                                Iterator<String> iterator =
                                        currentFeature.getAttachments().keySet().iterator();

                                while (iterator.hasNext()) {
                                    String attachId = iterator.next();

                                    //delete attachment which not exist on server
                                    if (!remoteFeature.getAttachments().containsKey(attachId)) {
                                        iterator.remove();
                                        saveAttach("" + currentFeature.getId(),
                                                currentFeature.getAttachments());

                                    } else { //or change attachment properties
                                        AttachItem currentItem =
                                                currentFeature.getAttachments().get(attachId);
                                        AttachItem remoteItem =
                                                remoteFeature.getAttachments().get(attachId);

                                        if (null != currentItem && !currentItem.equals(
                                                remoteItem)) {
                                            long attachIdL =
                                                    Long.parseLong(remoteItem.getAttachId());
                                            boolean changeOnServer =
                                                    !FeatureChanges.isAttachChanges(changeTableName,
                                                            remoteFeature.getId(), attachIdL);

                                            if (changeOnServer) {
                                                currentItem.setDescription(
                                                        remoteItem.getDescription());
                                                currentItem.setMimetype(remoteItem.getMimetype());
                                                currentItem.setDisplayName(
                                                        remoteItem.getDisplayName());
                                                saveAttach("" + currentFeature.getId(),
                                                        currentFeature.getAttachments());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    //Log.d(TAG, e.getLocalizedMessage());
                } finally {
                    if (null != cursor) {
                        cursor.close();
                    }
                }
            }

            // remove features not exist on server from local layer
            // if no operation is in changes array or change operation for local feature present

            List<Long> deleteItems = new ArrayList<>();

            for (Long featureId : query(null)) {
                boolean bDeleteFeature = true;
                for (Feature remoteFeature : features) {
                    if (remoteFeature.getId() == featureId) {
                        bDeleteFeature = false;
                        break;
                    }
                }

                // if local item is in update list and state ADD_NEW skip delete
                bDeleteFeature =
                        bDeleteFeature && !FeatureChanges.isChanges(changeTableName, featureId,
                                Constants.CHANGE_OPERATION_NEW) &&
                                !FeatureChanges.hasFeatureFlags(changeTableName, featureId);

                if (bDeleteFeature) {
                    deleteItems.add(featureId);
                }
            }

            for (long itemId : deleteItems) {
                if (Constants.DEBUG_MODE) {
                    Log.d(Constants.TAG, "Delete feature #" + itemId + " not exist on server");
                }
                delete(itemId, Constants.FIELD_ID + " = " + itemId, null);
            }

            Cursor changeCursor = FeatureChanges.getChanges(changeTableName);

            // remove changes already applied on server (delete already deleted id or add already added)
            if (null != changeCursor) {
                try {

                    if (changeCursor.moveToFirst()) {
                        int recordIdColumn = changeCursor.getColumnIndex(Constants.FIELD_ID);
                        int featureIdColumn =
                                changeCursor.getColumnIndex(Constants.FIELD_FEATURE_ID);
                        int operationColumn =
                                changeCursor.getColumnIndex(Constants.FIELD_OPERATION);
                        int attachOperationColumn =
                                changeCursor.getColumnIndex(Constants.FIELD_ATTACH_OPERATION);

                        do {
                            long changeRecordId = changeCursor.getLong(recordIdColumn);
                            long changeFeatureId = changeCursor.getLong(featureIdColumn);
                            int changeOperation = changeCursor.getInt(operationColumn);
                            int attachChangeOperation = changeCursor.getInt(attachOperationColumn);

                            boolean bDeleteChange = true; // if feature not exist on server
                            for (Feature remoteFeature : features) {
                                if (remoteFeature.getId() == changeFeatureId) {
                                    if (0 != (changeOperation & Constants.CHANGE_OPERATION_NEW)) {
                                        // if feature already exist, just change it
                                        FeatureChanges.setOperation(changeTableName, changeRecordId,
                                                Constants.CHANGE_OPERATION_CHANGED);
                                    }
                                    bDeleteChange = false; // in other cases just apply
                                    break;
                                }
                            }

                            if ((0 != (changeOperation & Constants.CHANGE_OPERATION_NEW) || 0 != (
                                    attachChangeOperation & Constants.CHANGE_OPERATION_NEW))
                                    && bDeleteChange) {

                                bDeleteChange = false;
                            }

                            if (bDeleteChange) {
                                if (Constants.DEBUG_MODE) {
                                    Log.d(Constants.TAG,
                                            "Delete change for feature #" + changeFeatureId +
                                                    ", changeOperation " + changeOperation +
                                                    ", attachChangeOperation " +
                                                    attachChangeOperation);
                                }
                                // TODO: analise for operation, remove all equal
                                FeatureChanges.removeChangeRecord(changeTableName, changeRecordId);
                            }

                        } while (changeCursor.moveToNext());
                    }

                } catch (Exception e) {
                    //Log.d(TAG, e.getLocalizedMessage());
                } finally {
                    changeCursor.close();
                }
            }

        } catch (SQLiteException | ConcurrentModificationException e) {
            syncResult.stats.numConflictDetectedExceptions++;
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "proceed getChangesFromServer() failed");
            }
            e.printStackTrace();
            return false;
        }

        return true;
    }


    protected boolean addFeatureOnServer(
            long featureId,
            SyncResult syncResult)
            throws SQLiteException
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }
        Uri uri = ContentUris.withAppendedId(getContentUri(), featureId);
        uri = uri.buildUpon().fragment(NO_SYNC).build();

        Cursor cursor = query(uri, null, null, null, null, null);
        if (null == cursor) {
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "addFeatureOnServer: Get cursor failed");
            }
            return true; //just remove buggy data
        }

        AccountData accountData = getAccountData(mContext, mAccountName);

        try {
            if (cursor.moveToFirst()) {

                String payload = cursorToJson(cursor);
                if (Constants.DEBUG_MODE) {
                    Log.d(Constants.TAG, "payload: " + payload);
                }
                String data = NetworkUtil.post(NGWUtil.getFeaturesUrl(accountData.url, mRemoteId),
                        payload, accountData.login, accountData.password);
                if (null == data) {
                    syncResult.stats.numIoExceptions++;
                    return false;
                }
                //set new id from server! {"id": 24}
                JSONObject result = new JSONObject(data);
                if (result.has(Constants.JSON_ID_KEY)) {
                    long id = result.getLong(Constants.JSON_ID_KEY);
                    changeFeatureId(featureId, id);
                }

                return true;

            } else {
                Log.d(Constants.TAG, "addFeatureOnServer: Get cursor failed");
                return true; //just remove buggy data
            }

        } catch (Exception e) {
//        } catch (SQLiteConstraintException | ClassNotFoundException | JSONException | IOException e) {
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, e.getLocalizedMessage());
                e.printStackTrace();
            }
            return false;
        } finally {
            cursor.close();
        }
    }


    protected boolean deleteFeatureOnServer(
            long featureId,
            SyncResult syncResult)
    {
        if (!mNet.isNetworkAvailable()) {
            return false;
        }

        AccountData accountData = getAccountData(mContext, mAccountName);

        try {
            if (!NetworkUtil.delete(NGWUtil.getFeatureUrl(accountData.url, mRemoteId, featureId),
                    accountData.login, accountData.password)) {

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
        Uri uri = ContentUris.withAppendedId(getContentUri(), featureId);
        uri = uri.buildUpon().fragment(NO_SYNC).build();

        Cursor cursor = query(uri, null, null, null, null, null);
        if (null == cursor) {
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "empty cursor for uri: " + uri);
            }
            return true; //just remove buggy data
        }

        AccountData accountData = getAccountData(mContext, mAccountName);

        try {
            if (cursor.moveToFirst()) {
                String payload = cursorToJson(cursor);
                if (Constants.DEBUG_MODE) {
                    Log.d(Constants.TAG, "payload: " + payload);
                }
                String data = NetworkUtil.put(
                        NGWUtil.getFeatureUrl(accountData.url, mRemoteId, featureId), payload,
                        accountData.login, accountData.password);
                if (null == data) {
                    syncResult.stats.numIoExceptions++;
                    return false;
                }

                return true;
            } else {
                Log.d(Constants.TAG, "changeFeatureOnServer(), empty cursor for uri: " + uri);
                return true; //just remove buggy data
            }
        } catch (Exception e) {
//        } catch (JSONException | ClassNotFoundException | IOException e) {
            if (Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, e.getLocalizedMessage());
                e.printStackTrace();
            }
            return false;
        } finally {
            cursor.close();
        }
    }


    protected String cursorToJson(Cursor cursor)
            throws JSONException, IOException, ClassNotFoundException
    {
        JSONObject rootObject = new JSONObject();
        if (0 != (mSyncType & Constants.SYNC_ATTRIBUTES)) {
            JSONObject valueObject = new JSONObject();
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                String name = cursor.getColumnName(i);
                if (name.equals(Constants.FIELD_ID) || name.equals(Constants.FIELD_GEOM)) {
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
                        TimeZone timeZoneDT = TimeZone.getDefault();
                        timeZoneDT.setRawOffset(0); // set to UTC
                        Calendar calendarDT = Calendar.getInstance(timeZoneDT);
                        calendarDT.setTimeInMillis(cursor.getLong(i));
                        JSONObject jsonDateTime = new JSONObject();
                        jsonDateTime.put("year", calendarDT.get(Calendar.YEAR));
                        jsonDateTime.put("month", calendarDT.get(Calendar.MONTH) + 1);
                        jsonDateTime.put("day", calendarDT.get(Calendar.DAY_OF_MONTH));
                        jsonDateTime.put("hour", calendarDT.get(Calendar.HOUR_OF_DAY));
                        jsonDateTime.put("minute", calendarDT.get(Calendar.MINUTE));
                        jsonDateTime.put("second", calendarDT.get(Calendar.SECOND));
                        valueObject.put(name, jsonDateTime);
                        break;
                    case GeoConstants.FTDate:
                        TimeZone timeZoneD = TimeZone.getDefault();
                        timeZoneD.setRawOffset(0); // set to UTC
                        Calendar calendarD = Calendar.getInstance(timeZoneD);
                        calendarD.setTimeInMillis(cursor.getLong(i));
                        JSONObject jsonDate = new JSONObject();
                        jsonDate.put("year", calendarD.get(Calendar.YEAR));
                        jsonDate.put("month", calendarD.get(Calendar.MONTH) + 1);
                        jsonDate.put("day", calendarD.get(Calendar.DAY_OF_MONTH));
                        valueObject.put(name, jsonDate);
                        break;
                    case GeoConstants.FTTime:
                        TimeZone timeZoneT = TimeZone.getDefault();
                        timeZoneT.setRawOffset(0); // set to UTC
                        Calendar calendarT = Calendar.getInstance(timeZoneT);
                        calendarT.setTimeInMillis(cursor.getLong(i));
                        JSONObject jsonTime = new JSONObject();
                        jsonTime.put("hour", calendarT.get(Calendar.HOUR_OF_DAY));
                        jsonTime.put("minute", calendarT.get(Calendar.MINUTE));
                        jsonTime.put("second", calendarT.get(Calendar.SECOND));
                        valueObject.put(name, jsonTime);
                        break;
                    default:
                        break;
                }
            }
            rootObject.put(NGWUtil.NGWKEY_FIELDS, valueObject);
        }

        if (0 != (mSyncType & Constants.SYNC_GEOMETRY)) {
            //may be found geometry in cache by id is faster
            GeoGeometry geometry = GeoGeometryFactory.fromBlob(
                    cursor.getBlob(cursor.getColumnIndex(Constants.FIELD_GEOM)));

            rootObject.put(NGWUtil.NGWKEY_GEOM, geometry.toWKT(true));
            //rootObject.put("id", cursor.getLong(cursor.getColumnIndex(FIELD_ID)));
        }

        return rootObject.toString();
    }


    /**
     * get synchronization type
     *
     * @return the synchronization type - the OR of this values: SYNC_NONE - no synchronization
     * SYNC_DATA - synchronize only data SYNC_ATTACH - synchronize only attachments SYNC_ALL -
     * synchronize everything
     */
    @Override
    public int getSyncType()
    {
        return mSyncType;
    }


    protected synchronized void applySync(int syncType)
    {
        if (syncType == Constants.SYNC_NONE) {
            FeatureChanges.removeAllChanges(getChangeTableName());
        } else {
            for (Long featureId : query(null)) {
                addChange(featureId, Constants.CHANGE_OPERATION_NEW);
                //add attach
                File attacheFolder = new File(mPath, "" + featureId);
                if (attacheFolder.isDirectory()) {
                    for (File attach : attacheFolder.listFiles()) {
                        String attachId = attach.getName();
                        if (attachId.equals(META)) {
                            continue;
                        }
                        Long attachIdL = Long.parseLong(attachId);
                        if (attachIdL >= Constants.MIN_LOCAL_FEATURE_ID) {
                            addChange(featureId, attachIdL, Constants.CHANGE_OPERATION_NEW);
                        }
                    }
                }
            }
        }
    }


    @Override
    public void setSyncType(int syncType)
    {
        if (!isSyncable()) {
            return;
        }

        if (mSyncType == syncType) {
            return;
        }

        if (syncType == Constants.SYNC_NONE) {
            mSyncType = syncType;

            new Thread(new Runnable()
            {
                public void run()
                {
                    android.os.Process.setThreadPriority(
                            android.os.Process.THREAD_PRIORITY_BACKGROUND);
                    applySync(Constants.SYNC_NONE);
                }
            }).start();

        } else if (mSyncType == Constants.SYNC_NONE && 0 != (syncType & Constants.SYNC_DATA)) {
            mSyncType = syncType;

            new Thread(new Runnable()
            {
                public void run()
                {
                    android.os.Process.setThreadPriority(
                            android.os.Process.THREAD_PRIORITY_BACKGROUND);
                    applySync(Constants.SYNC_ALL);
                }
            }).start();


        } else {
            mSyncType = syncType;
        }
    }


    @Override
    public boolean delete()
            throws SQLiteException
    {
        FeatureChanges.delete(getChangeTableName());

        return super.delete();
    }


    /**
     * Indicate if layer can sync data with remote server
     *
     * @return true if layer can sync or false
     */
    public boolean isSyncable()
    {
        return true;
    }


    /**
     * Indicate if layer can send changes to remote server
     *
     * @return true if layer can send changes to remote server or false
     */
    public boolean isRemoteReadOnly()
    {
        return !(mNGWLayerType == Connection.NGWResourceTypeVectorLayer);
    }


    @Override
    protected Cursor queryInternal(
            Uri uri,
            int uriType,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder,
            String limit)
    {
        String featureId;
        String attachId;
        List<String> pathSegments;

        String changeTableName = getChangeTableName();

        switch (uriType) {
            case TYPE_CHANGES_TABLE: {
                return FeatureChanges.query(
                        changeTableName, projection, selection, selectionArgs, sortOrder, limit);
            }

            case TYPE_CHANGES_FEATURE: {
                featureId = uri.getLastPathSegment();

                String changeSel = FIELD_FEATURE_ID + " = " + featureId;

                if (TextUtils.isEmpty(selection)) {
                    selection = changeSel;
                } else {
                    selection += " AND " + changeSel;
                }

                return FeatureChanges.query(
                        changeTableName, projection, selection, selectionArgs, sortOrder, limit);
            }

            case TYPE_CHANGES_ATTACH: {
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 2);

                String changeSel = FIELD_FEATURE_ID + " = " + featureId + " AND " + "( 0 != ( "
                        + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) )";

                if (TextUtils.isEmpty(selection)) {
                    selection = changeSel;
                } else {
                    selection += " AND " + changeSel;
                }

                return FeatureChanges.query(
                        changeTableName, projection, selection, selectionArgs, sortOrder, limit);
            }

            case TYPE_CHANGES_ATTACH_ID: {
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 3);
                attachId = uri.getLastPathSegment();

                String changeSel = FIELD_FEATURE_ID + " = " + featureId + " AND " + "( 0 != ( "
                        + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) ) AND "
                        + FIELD_ATTACH_ID + " = " + attachId;

                if (TextUtils.isEmpty(selection)) {
                    selection = changeSel;
                } else {
                    selection += " AND " + changeSel;
                }

                return FeatureChanges.query(
                        changeTableName, projection, selection, selectionArgs, sortOrder, limit);
            }

            default: {
                return super.queryInternal(
                        uri, uriType, projection, selection, selectionArgs, sortOrder, limit);
            }
        }
    }


    @Override
    protected int deleteInternal(
            Uri uri,
            int uriType,
            String selection,
            String[] selectionArgs)
    {
        String featureId;
        String attachId;
        List<String> pathSegments;

        String changeTableName = getChangeTableName();

        switch (uriType) {

            case TYPE_CHANGES_TABLE: {
                return FeatureChanges.delete(changeTableName, selection, selectionArgs);
            }

            case TYPE_CHANGES_FEATURE: {
                featureId = uri.getLastPathSegment();

                String changeSel = FIELD_FEATURE_ID + " = " + featureId;

                if (TextUtils.isEmpty(selection)) {
                    selection = changeSel;
                } else {
                    selection += " AND " + changeSel;
                }

                return FeatureChanges.delete(changeTableName, selection, selectionArgs);
            }

            case TYPE_CHANGES_ATTACH: {
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 2);

                String changeSel = FIELD_FEATURE_ID + " = " + featureId + " AND " + "( 0 != ( "
                        + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) )";

                if (TextUtils.isEmpty(selection)) {
                    selection = changeSel;
                } else {
                    selection += " AND " + changeSel;
                }

                return FeatureChanges.delete(changeTableName, selection, selectionArgs);
            }

            case TYPE_CHANGES_ATTACH_ID: {
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 3);
                attachId = uri.getLastPathSegment();

                String changeSel = FIELD_FEATURE_ID + " = " + featureId + " AND " + "( 0 != ( "
                        + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) ) AND "
                        + FIELD_ATTACH_ID + " = " + attachId;

                if (TextUtils.isEmpty(selection)) {
                    selection = changeSel;
                } else {
                    selection += " AND " + changeSel;
                }

                return FeatureChanges.delete(changeTableName, selection, selectionArgs);
            }

            default: {
                return super.deleteInternal(uri, uriType, selection, selectionArgs);
            }
        }
    }


    @Override
    public boolean isChanges()
    {
        return FeatureChanges.isChanges(getChangeTableName());
    }


    @Override
    protected boolean haveFeaturesNotSyncFlag()
    {
        return FeatureChanges.haveFeaturesNotSyncFlag(getChangeTableName());
    }


    @Override
    protected boolean hasFeatureChanges(long featureId)
    {
        return FeatureChanges.isChanges(getChangeTableName(), featureId);
    }


    @Override
    protected boolean hasAttachChanges(
            long featureId,
            long attachId)
    {
        return FeatureChanges.isAttachChanges(getChangeTableName(), featureId, attachId);
    }


    @Override
    public Cursor queryFirstTempFeatureFlags()
    {
        // TODO: move work with temp features into VectorLayer
        String selection = "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_TEMP + " ) )";

        Cursor cursor =
                FeatureChanges.query(getChangeTableName(), selection, FIELD_ID + " ASC", "1");

        if (null != cursor) {

            if (cursor.moveToFirst()) {
                return cursor;
            }

            cursor.close();
        }

        return null;
    }


    @Override
    public Cursor queryFirstTempAttachFlags()
    {
        // TODO: move work with temp features into VectorLayer
        String selection = "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + CHANGE_OPERATION_TEMP + " ) )";

        Cursor cursor =
                FeatureChanges.query(getChangeTableName(), selection, FIELD_ID + " ASC", "1");

        if (null != cursor) {

            if (cursor.moveToFirst()) {
                return cursor;
            }

            cursor.close();
        }

        return null;
    }


    @Override
    public boolean hasFeatureTempFlag(long featureId)
    {
        // TODO: move work with temp features into VectorLayer
        return FeatureChanges.hasFeatureTempFlag(getChangeTableName(), featureId);
    }


    @Override
    public boolean hasFeatureNotSyncFlag(long featureId)
    {
        return FeatureChanges.hasFeatureNotSyncFlag(getChangeTableName(), featureId);
    }


    @Override
    public boolean hasAttachTempFlag(
            long featureId,
            long attachId)
    {
        // TODO: move work with temp features into VectorLayer
        return FeatureChanges.hasAttachTempFlag(getChangeTableName(), featureId, attachId);
    }


    @Override
    public boolean hasAttachNotSyncFlag(
            long featureId,
            long attachId)
    {
        return FeatureChanges.hasAttachNotSyncFlag(getChangeTableName(), featureId, attachId);
    }


    @Override
    public long setFeatureTempFlag(
            long featureId,
            boolean flag)
    {
        // TODO: move work with temp features into VectorLayer
        if (flag) {
            return FeatureChanges.setFeatureTempFlag(getChangeTableName(), featureId);
        } else {
            return FeatureChanges.deleteFeatureTempFlag(getChangeTableName(), featureId);
        }
    }


    @Override
    public long setFeatureNotSyncFlag(
            long featureId,
            boolean flag)
    {
        if (flag) {
            return FeatureChanges.setFeatureNotSyncFlag(getChangeTableName(), featureId);
        } else {
            return FeatureChanges.deleteFeatureNotSyncFlag(getChangeTableName(), featureId);
        }
    }


    @Override
    public long setAttachTempFlag(
            long featureId,
            long attachId,
            boolean flag)
    {
        // TODO: move work with temp features into VectorLayer
        if (flag) {
            return FeatureChanges.setAttachTempFlag(getChangeTableName(), featureId, attachId);
        } else {
            return FeatureChanges.deleteAttachTempFlag(getChangeTableName(), featureId, attachId);
        }
    }


    @Override
    public long setAttachNotSyncFlag(
            long featureId,
            long attachId,
            boolean flag)
    {
        if (flag) {
            return FeatureChanges.setAttachNotSyncFlag(getChangeTableName(), featureId, attachId);
        } else {
            return FeatureChanges.deleteAttachNotSyncFlag(
                    getChangeTableName(), featureId, attachId);
        }
    }


    @Override
    public int deleteAllTempFeaturesFlags()
    {
        // TODO: move work with temp features into VectorLayer
        String selection = "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_TEMP + " ) )";

        return FeatureChanges.delete(getChangeTableName(), selection);
    }


    @Override
    public int deleteAllTempAttachesFlags()
    {
        // TODO: move work with temp features into VectorLayer
        String selection = "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + CHANGE_OPERATION_TEMP + " ) )";

        return FeatureChanges.delete(getChangeTableName(), selection);
    }


    protected AccountData getAccountData(
            Context context,
            String accountName)
    {
        IGISApplication app = (IGISApplication) context.getApplicationContext();
        Account account = app.getAccount(accountName);

        AccountData accountData = new AccountData();

        accountData.url = app.getAccountUrl(account);
        accountData.login = app.getAccountLogin(account);
        accountData.password = app.getAccountPassword(account);

        return accountData;
    }


    protected class AccountData
    {
        String url;
        String login;
        String password;
    }
}
