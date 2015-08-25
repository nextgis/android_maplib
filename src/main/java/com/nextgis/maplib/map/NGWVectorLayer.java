/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.widget.Toast;

import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.IGeometryCacheItem;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.util.AttachItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FeatureChanges;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
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
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.MIN_LOCAL_FEATURE_ID;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;

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
    protected int         mNGWLayerType;
    protected String mServerWhere;
    //protected int mSyncDirection; //1 - to server only, 2 - from server only, 3 - both directions
    //check where to sync on GSM/WI-FI for data/attachments

    protected String mChangeTableName;

    protected static final String JSON_ACCOUNT_KEY   = "account";
    protected static final String JSON_SYNC_TYPE_KEY = "sync_type";
    protected static final String JSON_NGWLAYER_TYPE_KEY = "ngw_layer_type";
    protected static final String JSON_SERVERWHERE_KEY   = "server_where";

    public NGWVectorLayer(
            Context context,
            File path)
    {
        super(context, path);

        mNet = new NetworkUtil(context);

        // table name is the same as the folder name of the layer + "_changes"
        mChangeTableName = mPath.getName() + Constants.CHANGES_NAME_POSTFIX;
        mSyncType = Constants.SYNC_NONE;
        mLayerType = Constants.LAYERTYPE_NGW_VECTOR;
        mNGWLayerType = Connection.NGWResourceTypeNone;
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


    public String getServerWhere() {
        return mServerWhere;
    }

    public void setServerWhere(String serverWhere) {
        mServerWhere = serverWhere;
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

        if(jsonObject.has(JSON_NGWLAYER_TYPE_KEY)) {
            mNGWLayerType = jsonObject.getInt(JSON_NGWLAYER_TYPE_KEY);
        }

        if(jsonObject.has(JSON_SERVERWHERE_KEY)){
            mServerWhere = jsonObject.getString(JSON_SERVERWHERE_KEY);
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
            if (Constants.NOT_FOUND != nType) {
                fields.add(new Field(nType, name, alias));
            }
        }
        return fields;
    }

    @Override
    protected long insert(ContentValues contentValues) {
        if (!contentValues.containsKey(FIELD_GEOM)) {
            return NOT_FOUND;
        }

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }

        if (!contentValues.containsKey(FIELD_ID)) {

            long id = getUniqId();
            if (MIN_LOCAL_FEATURE_ID > id) {
                id = MIN_LOCAL_FEATURE_ID;
            }
            contentValues.put(FIELD_ID, id);
        }

        SQLiteDatabase db = map.getDatabase(false);
        long rowID = db.insert(mPath.getName(), null, contentValues);

        if (rowID != NOT_FOUND) {
            Intent notify = new Intent(NOTIFY_INSERT);
            notify.putExtra(FIELD_ID, rowID);
            notify.putExtra(NOTIFY_LAYER_NAME, mPath.getName()); // if we need mAuthority?
            getContext().sendBroadcast(notify);
        }

        updateUniqId(rowID);

        return rowID;
    }

    @Override
    protected long insertAttach(String featureId, ContentValues contentValues) {
        if (contentValues.containsKey(ATTACH_DISPLAY_NAME) &&
                contentValues.containsKey(ATTACH_MIME_TYPE)) {
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
     *
     * @return the error message or null if everything is ok
     */
    public String download()
    {
        if(null == mCacheUrl)
            return getContext().getString(R.string.error_download_data);

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

            //fill field list
            JSONObject featureLayerJSONObject = geoJSONObject.getJSONObject("feature_layer");
            JSONArray fieldsJSONArray = featureLayerJSONObject.getJSONArray(NGWUtil.NGWKEY_FIELDS);
            List<Field> fields = getFieldsFromJson(fieldsJSONArray);

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

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {

                //get layer data
                data = mNet.get(NGWUtil.getFeaturesUrl(mCacheUrl, mRemoteId, mServerWhere),
                            mCacheLogin, mCachePassword);
                if (null == data) {
                    return getContext().getString(R.string.error_download_data);
                }

                JSONArray featuresJSONArray = new JSONArray(data);
                List<Feature> features = jsonToFeatures(featuresJSONArray, fields, nSRS);

                Log.d(Constants.TAG, "feature count: " + features.size());

                return initialize(fields, features, geomType);
            }
            else{
                // init empty layer
                String initStatus = initialize(fields, new ArrayList<Feature>(), geomType);
                if(initStatus != null)
                    return initStatus;

                mIsInitialized = false;

                // get features and fill them
                URL url = new URL(NGWUtil.getFeaturesUrl(mCacheUrl, mRemoteId, mServerWhere));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                final String basicAuth = NetworkUtil.getHTTPBaseAuth(mCacheLogin, mCachePassword);
                if(null != basicAuth)
                    urlConnection.setRequestProperty("Authorization", basicAuth);
                MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
                SQLiteDatabase db = map.getDatabase(false);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                loadFeaturesFromNGWStream(in, fields, db, nSRS);
                urlConnection.disconnect();

                mIsInitialized = true;
                return null;
            }

        } catch (IOException e) {
            Log.d(Constants.TAG, "Problem downloading GeoJSON: " + mCacheUrl + " Error: " +
                         e.getLocalizedMessage());
            return getContext().getString(R.string.error_download_data);
        } catch (JSONException | SQLiteException e) {
            e.printStackTrace();
            return getContext().getString(R.string.error_download_data);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected List<Feature> loadFeaturesFromNGWStream(InputStream in, List<Field> fields, int nSRS) throws IOException {
        List<Feature> result = new ArrayList<>();
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        reader.beginArray();
        while (reader.hasNext()) {
            Feature feature = readNGWFeature(reader, fields, nSRS);
            result.add(feature);
        }
        reader.endArray();
        reader.close();

        return result;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void loadFeaturesFromNGWStream(InputStream in, List<Field> fields, SQLiteDatabase db, int nSRS) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        reader.beginArray();
        int featureCount = 0;
        while (reader.hasNext()) {
            //TODO: download attachments if needed
            Feature feature = readNGWFeature(reader, fields, nSRS);
            createFeature(feature, fields, db);
            ++featureCount;
        }

        Log.d(Constants.TAG, "feature count: " + featureCount);

        reader.endArray();
        reader.close();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected Feature readNGWFeature(JsonReader reader, List<Field> fields, int nSRS) throws IOException {
        Feature feature = new Feature(Constants.NOT_FOUND, fields);

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(NGWUtil.NGWKEY_ID)) {
                feature.setId(reader.nextLong());
            } else if (name.equals(NGWUtil.NGWKEY_GEOM)) {
                String wkt = reader.nextString();
                GeoGeometry geom = GeoGeometryFactory.fromWKT(wkt);
                geom.setCRS(nSRS);
                if (nSRS != GeoConstants.CRS_WEB_MERCATOR) {
                    geom.project(GeoConstants.CRS_WEB_MERCATOR);
                }
                feature.setGeometry(geom);
            }
            else if(name.equals(NGWUtil.NGWKEY_FIELDS)){
                readNGWFeatureFields(feature, reader, fields);
            }
            else if(name.equals(NGWUtil.NGWKEY_EXTENSIONS)){
                if (reader.peek() != JsonToken.NULL) {
                    readNGWFeatureAttachments(feature, reader);
                }
            }
            else
                reader.skipValue();
        }
        reader.endObject();
        return  feature;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void readNGWFeatureFields(Feature feature, JsonReader reader, List<Field> fields) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if(reader.peek() == JsonToken.NULL)
                reader.skipValue();
            else {
                boolean bAdded = false;
                for (Field field : fields) {
                    if (field.getName().equals(name)) {
                        switch (field.getType()) {
                            case GeoConstants.FTReal:
                                feature.setFieldValue(field.getName(), reader.nextDouble());
                                bAdded = true;
                                break;
                            case GeoConstants.FTInteger:
                                feature.setFieldValue(field.getName(), reader.nextInt());
                                bAdded = true;
                                break;
                            case GeoConstants.FTString:
                                feature.setFieldValue(field.getName(), reader.nextString());
                                bAdded = true;
                                break;
                            case GeoConstants.FTDateTime:
                                readNGWDate(feature, reader, field.getName());
                                bAdded = true;
                                break;
                            default:
                                break;
                        }
                        break;
                    }
                }
                if(!bAdded)
                    reader.skipValue();
            }
        }
        reader.endObject();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void readNGWDate(Feature feature, JsonReader reader, String fieldName) throws IOException {
        reader.beginObject();
        int nYear = 1900;
        int nMonth = 1;
        int nDay = 1;
        int nHour = 0;
        int nMinute = 0;
        int nSecond = 0;
        while (reader.hasNext()){
            String name = reader.nextName();
            if(name.equals(NGWUtil.NGWKEY_YEAR)){
                nYear = reader.nextInt();
            }
            else if(name.equals(NGWUtil.NGWKEY_MONTH)){
                nMonth = reader.nextInt();
            }
            else if(name.equals(NGWUtil.NGWKEY_DAY)){
                nDay = reader.nextInt();
            }
            else if(name.equals(NGWUtil.NGWKEY_HOUR)){
                nHour = reader.nextInt();
            }
            else if(name.equals(NGWUtil.NGWKEY_MINUTE)){
                nMinute = reader.nextInt();
            }
            else if(name.equals(NGWUtil.NGWKEY_SECOND)){
                nSecond = reader.nextInt();
            }
            else {
                reader.skipValue();
            }
        }

        Calendar calendar = new GregorianCalendar(nYear, nMonth - 1, nDay, nHour, nMinute, nSecond);
        feature.setFieldValue(fieldName, calendar.getTimeInMillis());

        reader.endObject();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void readNGWFeatureAttachments(Feature feature, JsonReader reader) throws IOException {
        //add extensions
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if(name.equals("attachment") && reader.peek() != JsonToken.NULL){
                reader.beginArray();
                while (reader.hasNext()) {
                    readNGWFeatureAttachment(feature, reader);
                }
                reader.endArray();
            }
            else {
                reader.skipValue();
            }
        }

        reader.endObject();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void readNGWFeatureAttachment(Feature feature, JsonReader reader) throws IOException {
        reader.beginObject();
        String attachId = "";
        String name = "";
        String mime = "";
        String descriptionText = "";
        while (reader.hasNext()) {
            String keyName = reader.nextName();
            if(reader.peek() == JsonToken.NULL){
                reader.skipValue();
                continue;
            }

            if(keyName.equals(NGWUtil.NGWKEY_ID)){
                attachId += reader.nextLong();
            }
            else if(keyName.equals(NGWUtil.NGWKEY_NAME)){
                name += reader.nextString();
            }
            else if(keyName.equals(NGWUtil.NGWKEY_MIME)){
                mime += reader.nextString();
            }
            else if(keyName.equals(NGWUtil.NGWKEY_DESCRIPTION)){
                descriptionText += reader.nextString();
            }
            else{
                reader.skipValue();
            }
        }
        AttachItem item = new AttachItem(attachId, name, mime, descriptionText);
        feature.addAttachment(item);

        reader.endObject();
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
            long id = featureJSONObject.getLong(NGWUtil.NGWKEY_ID);
            String wkt = featureJSONObject.getString(NGWUtil.NGWKEY_GEOM);
            JSONObject fieldsJSONObject = featureJSONObject.getJSONObject(NGWUtil.NGWKEY_FIELDS);
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
            if (featureJSONObject.has("extensions")) {
                JSONObject ext = featureJSONObject.getJSONObject("extensions");
                //get attachment & description
                if (!ext.isNull("attachment")) {
                    JSONArray attachment = ext.getJSONArray("attachment");
                    for (int j = 0; j < attachment.length(); j++) {
                        JSONObject jsonAttachmentDetails = attachment.getJSONObject(j);
                        String attachId = "" + jsonAttachmentDetails.getLong(Constants.JSON_ID_KEY);
                        String name = jsonAttachmentDetails.getString(Constants.JSON_NAME_KEY);
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
            case "REAL":
                return GeoConstants.FTReal;
            case "DATETIME":
                return GeoConstants.FTDateTime;
            case "DATE":
                return GeoConstants.FTDate;
            case "TIME":
                return GeoConstants.FTTime;
            default:
                return Constants.NOT_FOUND;
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
        if (0 == (mSyncType & Constants.SYNC_DATA)) {
            return;
        }

        boolean canAddChanges = true;

        // for delete operation
        if (operation == Constants.CHANGE_OPERATION_DELETE) {

            // if featureId == NOT_FOUND remove all changes for all features
            if (featureId == Constants.NOT_FOUND) {
                FeatureChanges.removeAllChanges(mChangeTableName);

                // if feature has changes then remove them for the feature
            } else if (FeatureChanges.isChanges(mChangeTableName, featureId)) {
                // if feature was new then just remove its changes
                canAddChanges = !FeatureChanges.isChanges(
                        mChangeTableName, featureId, Constants.CHANGE_OPERATION_NEW);
                FeatureChanges.removeChanges(mChangeTableName, featureId);
            }
        }

        // we are trying to re-create feature - warning
        if (operation == Constants.CHANGE_OPERATION_NEW &&
            FeatureChanges.isChanges(mChangeTableName, featureId)) {
            Log.w(Constants.TAG, "Something wrong. Should nether get here");
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
        if (0 == (mSyncType & Constants.SYNC_ATTACH)) {
            return;
        }

        boolean canAddChanges = true;

        // for delete operation
        if (attachOperation == Constants.CHANGE_OPERATION_DELETE) {

            // if attachId == NOT_FOUND remove all attach changes for the feature
            if (attachId == Constants.NOT_FOUND) {
                FeatureChanges.removeAllAttachChanges(mChangeTableName, featureId);

                // if attachment has changes then remove them for the attachment
            } else if (FeatureChanges.isAttachChanges(mChangeTableName, featureId, attachId)) {
                // if attachment was new then just remove its changes
                canAddChanges = !FeatureChanges.isAttachChanges(
                        mChangeTableName, featureId, attachId, Constants.CHANGE_OPERATION_NEW);
                FeatureChanges.removeAttachChanges(mChangeTableName, featureId, attachId);
            }
        }

        // we are trying to re-create the attach - warning
        // TODO: replace to attachOperation == CHANGE_OPERATION_NEW ???
        if (0 != (attachOperation & Constants.CHANGE_OPERATION_NEW) &&
            FeatureChanges.isAttachChanges(mChangeTableName, featureId, attachId)) {
            Log.w(Constants.TAG, "Something wrong. Should nether get here");
            canAddChanges = false;
        }

        if (canAddChanges) {
            FeatureChanges.add(mChangeTableName, featureId, attachId, attachOperation);
        }
    }


    /**
     * Synchronize changes with NGW. Should be run from non UI thread.
     *
     * @param authority - a content resolver authority (i.e. com.nextgis.mobile.provider)
     * @param syncResult
     *         - report some errors via this parameter
     */
    public void sync(
            String authority,
            SyncResult syncResult)
    {
        syncResult.clear();
        if (0 != (mSyncType & Constants.SYNC_NONE) || !mIsInitialized) {
            Log.d(Constants.TAG, "Layer " + getName() + " is not checked to sync or not inited");
            return;
        }

        // 1. get remote changes
        if (!getChangesFromServer(authority, syncResult)) {
            Log.d(Constants.TAG, "Get remote changes failed");
            return;
        }

        if(isRemoteReadOnly()) {
            return;
        }

        // 2. send current changes
        if (!sendLocalChanges(syncResult)) {
            Log.d(Constants.TAG, "Set local changes failed");
            //return;
        }
    }


    public boolean sendLocalChanges(SyncResult syncResult)
    {
        long changesCount = FeatureChanges.getChangeCount(mChangeTableName);
        Log.d(Constants.TAG, "sendLocalChanges: " + changesCount);

        if (0 == changesCount) {
            return true;
        }

        boolean isError = false;

        try {
            // get column's IDs, there is at least one entry
            Cursor changeCursor = FeatureChanges.getFirstChangeFromRecordId(mChangeTableName, 0);
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

                changeCursor = FeatureChanges.getFirstChangeFromRecordId(
                        mChangeTableName, nextChangeRecordId);

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

                if (0 == (changeOperation & Constants.CHANGE_OPERATION_ATTACH)) {

                    if (0 != (changeOperation & Constants.CHANGE_OPERATION_DELETE)) {
                        if (deleteFeatureOnServer(changeFeatureId, syncResult)) {
                            FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            Log.d(Constants.TAG, "proceed deleteFeatureOnServer() failed");
                        }

                    } else if (0 != (changeOperation & Constants.CHANGE_OPERATION_NEW)) {
                        if (addFeatureOnServer(changeFeatureId, syncResult)) {
                            FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                            FeatureChanges.removeChangesToLast(
                                    mChangeTableName, changeFeatureId,
                                    Constants.CHANGE_OPERATION_CHANGED, lastChangeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            Log.d(Constants.TAG, "proceed addFeatureOnServer() failed");
                        }

                    } else if (0 != (changeOperation & Constants.CHANGE_OPERATION_CHANGED)) {
                        if (changeFeatureOnServer(changeFeatureId, syncResult)) {
                            FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                            FeatureChanges.removeChangesToLast(
                                    mChangeTableName, changeFeatureId,
                                    Constants.CHANGE_OPERATION_CHANGED, lastChangeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            Log.d(Constants.TAG, "proceed changeFeatureOnServer() failed");
                        }
                    }
                }

                //process attachments
                else { // 0 != (changeOperation & CHANGE_OPERATION_ATTACH)

                    if (changeAttachOperation == Constants.CHANGE_OPERATION_DELETE) {
                        if (deleteAttachOnServer(changeFeatureId, changeAttachId, syncResult)) {
                            FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            Log.d(Constants.TAG, "proceed deleteAttachOnServer() failed");
                        }

                    } else if (changeAttachOperation == Constants.CHANGE_OPERATION_NEW) {
                        if (sendAttachOnServer(changeFeatureId, changeAttachId, syncResult)) {
                            FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                            FeatureChanges.removeAttachChangesToLast(
                                    mChangeTableName, changeFeatureId, changeAttachId,
                                    Constants.CHANGE_OPERATION_CHANGED, lastChangeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            Log.d(Constants.TAG, "proceed sendAttachOnServer() failed");
                        }

                    } else if (changeAttachOperation == Constants.CHANGE_OPERATION_CHANGED) {
                        if (changeAttachOnServer(changeFeatureId, changeAttachId, syncResult)) {
                            FeatureChanges.removeAttachChangesToLast(
                                    mChangeTableName, changeFeatureId, changeAttachId,
                                    Constants.CHANGE_OPERATION_CHANGED, lastChangeRecordId);
                        } else {
                            isError = true;
                            syncResult.stats.numIoExceptions++;
                            Log.d(Constants.TAG, "proceed changeAttachOnServer() failed");
                        }
                    }
                }
            }

            // check records count changing
            if (changesCount != FeatureChanges.getChangeCount(mChangeTableName)) {
                //notify to reload changes
                getContext().sendBroadcast(new Intent(SyncAdapter.SYNC_CHANGES));
            }

        } catch (SQLiteException e) {
            isError = true;
            syncResult.stats.numConflictDetectedExceptions++;
            Log.d(Constants.TAG, "proceed sendLocalChanges() failed");
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

        try {
            JSONObject putData = new JSONObject();
            //putData.put(JSON_ID_KEY, attach.getAttachId());
            putData.put(Constants.JSON_NAME_KEY, attach.getDisplayName());
            //putData.put("mime_type", attach.getMimetype());
            putData.put("description", attach.getDescription());

            String data = mNet.put(
                    NGWUtil.getFeatureAttachmentUrl(mCacheUrl, mRemoteId, featureId) + attachId,
                    putData.toString(), mCacheLogin, mCachePassword);

            if (null == data) {
                syncResult.stats.numIoExceptions++;
                return false;
            }

            return true;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Log.d(Constants.TAG, e.getLocalizedMessage());
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

        try {

            if (!mNet.delete(
                    NGWUtil.getFeatureAttachmentUrl(mCacheUrl, mRemoteId, featureId) + attachId,
                    mCacheLogin, mCachePassword)) {

                syncResult.stats.numIoExceptions++;
                return false;
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(Constants.TAG, e.getLocalizedMessage());
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

        try {
            //1. upload file
            String data = mNet.postFile(
                    NGWUtil.getFileUploadUrl(mCacheUrl), fileName, filePath, fileMime, mCacheLogin,
                    mCachePassword);
            if (null == data) {
                syncResult.stats.numIoExceptions++;
                return false;
            }
            JSONObject result = new JSONObject(data);
            if (!result.has("upload_meta")) {
                Log.d(Constants.TAG, "Problem sendAttachOnServer(), result has not upload_meta, result: " +
                        result.toString());
                syncResult.stats.numIoExceptions++;
                return false;
            }

            JSONArray uploadMetaArray = result.getJSONArray("upload_meta");
            if (uploadMetaArray.length() == 0) {
                Log.d(Constants.TAG, "Problem sendAttachOnServer(), result upload_meta length() == 0");
                syncResult.stats.numIoExceptions++;
                return false;
            }
            //2. add attachment to row
            JSONObject postJsonData = new JSONObject();
            postJsonData.put("file_upload", uploadMetaArray.get(0));
            postJsonData.put("description", attach.getDescription());

            String postload = postJsonData.toString();
            Log.d(Constants.TAG, "postload: " + postload);

            data = mNet.post(
                    NGWUtil.getFeatureAttachmentUrl(mCacheUrl, mRemoteId, featureId),
                    postload, mCacheLogin, mCachePassword);
            if (null == data) {
                syncResult.stats.numIoExceptions++;
                return false;
            }

            result = new JSONObject(data);
            if (!result.has(Constants.JSON_ID_KEY)) {
                Log.d(Constants.TAG,
                        "Problem sendAttachOnServer(), result has not ID key, result: " +
                        result.toString());
                syncResult.stats.numIoExceptions++;
                return false;
            }

            long newAttachId = result.getLong(Constants.JSON_ID_KEY);
            setNewAttachId("" + featureId, attach, "" + newAttachId);

            return true;

        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Log.d(Constants.TAG, e.getLocalizedMessage());
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
        Log.d(Constants.TAG, "old id: " + oldFeatureId + " new id: " + newFeatureId);
        SQLiteDatabase db = map.getDatabase(false);
        ContentValues values = new ContentValues();
        values.put(Constants.FIELD_ID, newFeatureId);
        if (db.update(mPath.getName(), values, Constants.FIELD_ID + " = " + oldFeatureId, null) != 1) {
            Log.d(Constants.TAG, "failed to set new id");
        }

        //update id in cache
        final IGeometryCacheItem cacheItem = mGeometryCache.getItem(oldFeatureId);
        if(null != cacheItem)
            cacheItem.setFeatureId(newFeatureId);

        //rename photo id folder if exist
        File photoFolder = new File(mPath, "" + oldFeatureId);
        if (photoFolder.exists()) {
            if (photoFolder.renameTo(new File(mPath, "" + newFeatureId))) {

                int chRes = FeatureChanges.changeFeatureIdForAttaches(
                        mChangeTableName, oldFeatureId, newFeatureId);
                if (chRes <= 0) {
                    Log.d(
                            Constants.TAG,
                            "Feature ID for attaches not changed, oldFeatureId: " + oldFeatureId +
                                    ", newFeatureId: " + newFeatureId);
                }

            } else {
                Log.d(Constants.TAG, "rename photo folder " + oldFeatureId + "failed");
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

        Log.d(Constants.TAG, "The network is available. Get changes from server");
        List<Feature> features;
        // read layer contents as string
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {

            String data;
            try {
                data = mNet.get( NGWUtil.getFeaturesUrl(mCacheUrl, mRemoteId, mServerWhere),
                        mCacheLogin, mCachePassword);
            } catch (IOException e) {
                e.printStackTrace();
                syncResult.stats.numParseExceptions++;
                return false;
            }

            if (null == data) {
                syncResult.stats.numIoExceptions++;
                return false;
            }

            // parse layer contents to Feature list
            try {
                JSONArray featuresJSONArray = new JSONArray(data);
                features =
                        jsonToFeatures(featuresJSONArray, getFields(), GeoConstants.CRS_WEB_MERCATOR);
            } catch (JSONException e) {
                e.printStackTrace();
                syncResult.stats.numParseExceptions++;
                return false;
            }
        }
        else{
            try {
                URL url = new URL(NGWUtil.getFeaturesUrl(mCacheUrl, mRemoteId, mServerWhere));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                final String basicAuth = NetworkUtil.getHTTPBaseAuth(mCacheLogin, mCachePassword);
                if(null != basicAuth)
                    urlConnection.setRequestProperty("Authorization", basicAuth);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                features = loadFeaturesFromNGWStream(in, getFields(), GeoConstants.CRS_WEB_MERCATOR);
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

            Log.d(Constants.TAG, "Get " + features.size() + " feature(s) from server");

            // analyse feature
            for (Feature remoteFeature : features) {

                Cursor cursor = query(
                        null, Constants.FIELD_ID + " = " + remoteFeature.getId(), null, null, null);
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
                        Log.d(
                                Constants.TAG,
                                "Add new feature from server - " + newFeatureUri.toString());
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

                                FeatureChanges.removeChanges(
                                        mChangeTableName, remoteFeature.getId());
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
                            Log.d(
                                    Constants.TAG, "Update feature (" + count + ") from server - " +
                                            remoteFeature.getId());
                        }
                    }

                    //process attachments
                    if (eqAttach) {

                        if (FeatureChanges.isChanges(mChangeTableName, remoteFeature.getId()) &&
                                (eqData || FeatureChanges.isAttachChanges(
                                        mChangeTableName, remoteFeature.getId()))) {

                            Log.d(
                                    Constants.TAG, "The feature " + remoteFeature.getId() +
                                            " already changed on server. Remove changes for it");

                            FeatureChanges.removeChanges(mChangeTableName, remoteFeature.getId());
                        }

                    } else {
                        boolean isChangedLocal = FeatureChanges.isAttachChanges(
                                mChangeTableName, remoteFeature.getId());

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

            List<Long> deleteItems = new ArrayList<>();

            for(Feature remoteFeature : features) {
                if(mGeometryCache.isItemExist(remoteFeature.getId())){
                    if(!FeatureChanges.isChanges(
                            mChangeTableName, remoteFeature.getId(), Constants.CHANGE_OPERATION_NEW)){
                        deleteItems.add(remoteFeature.getId());
                    }
                }
            }

            for(long itemId : deleteItems){
                Log.d(
                        Constants.TAG,
                        "Delete feature #" + itemId + " not exist on server");
                delete(itemId, Constants.FIELD_ID + " = " + itemId, null);
            }

            Cursor changeCursor = FeatureChanges.getChanges(mChangeTableName);

            // remove changes already applied on server (delete already deleted id or add already added)
            if (null != changeCursor) {

                if (changeCursor.moveToFirst()) {
                    int recordIdColumn = changeCursor.getColumnIndex(Constants.FIELD_ID);
                    int featureIdColumn = changeCursor.getColumnIndex(Constants.FIELD_FEATURE_ID);
                    int operationColumn = changeCursor.getColumnIndex(Constants.FIELD_OPERATION);
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
                                    FeatureChanges.setOperation(
                                            mChangeTableName, changeRecordId,
                                            Constants.CHANGE_OPERATION_CHANGED);
                                }
                                bDeleteChange = false; // in other cases just apply
                                break;
                            }
                        }

                        if ((0 != (changeOperation & Constants.CHANGE_OPERATION_NEW) ||
                                0 != (attachChangeOperation & Constants.CHANGE_OPERATION_NEW)) &&
                                bDeleteChange) {

                            bDeleteChange = false;
                        }

                        if (bDeleteChange) {
                            Log.d(
                                    Constants.TAG, "Delete change for feature #" + changeFeatureId +
                                            ", changeOperation " + changeOperation +
                                            ", attachChangeOperation " + attachChangeOperation);
                            // TODO: analise for operation, remove all equal
                            FeatureChanges.removeChangeRecord(mChangeTableName, changeRecordId);
                        }

                    } while (changeCursor.moveToNext());
                }

                changeCursor.close();
            }

        } catch (SQLiteException | ConcurrentModificationException e) {
            syncResult.stats.numConflictDetectedExceptions++;
            Log.d(Constants.TAG, "proceed getChangesFromServer() failed");
            e.printStackTrace();
            return false;
        }

        return true;
    }


    protected Feature cursorToFeature(Cursor cursor)
    {
        Feature out = new Feature((long) Constants.NOT_FOUND, getFields());
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

        Cursor cursor = query(uri, null, null, null, null, null);
        if (null == cursor || !cursor.moveToFirst()) {
            Log.d(Constants.TAG, "addFeatureOnServer: Get cursor failed");
            if (null != cursor) {
                cursor.close();
            }
            return true; //just remove buggy data
        }

        try {
            String payload = cursorToJson(cursor);
            cursor.close();
            Log.d(Constants.TAG, "payload: " + payload);
            String data = mNet.post(
                    NGWUtil.getFeaturesUrl(mCacheUrl, mRemoteId, mServerWhere), payload, mCacheLogin,
                    mCachePassword);
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
        } catch (SQLiteConstraintException | ClassNotFoundException | JSONException | IOException e) {
            e.printStackTrace();
            Log.d(Constants.TAG, e.getLocalizedMessage());
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

        Cursor cursor = query(uri, null, null, null, null, null);
        if (null == cursor || !cursor.moveToFirst()) {
            Log.d(Constants.TAG, "empty cursor for uri: " + uri);
            if (null != cursor) {
                cursor.close();
            }
            return true; //just remove buggy data
        }

        try {
            String payload = cursorToJson(cursor);
            cursor.close();
            Log.d(Constants.TAG, "payload: " + payload);
            String data = mNet.put(
                    NGWUtil.getFeatureUrl(mCacheUrl, mRemoteId, featureId), payload, mCacheLogin,
                    mCachePassword);
            if (null == data) {
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
            GeoGeometry geometry =
                    GeoGeometryFactory.fromBlob(cursor.getBlob(cursor.getColumnIndex(Constants.FIELD_GEOM)));

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
    public int getSyncType()
    {
        return mSyncType;
    }

    protected synchronized void applySync(int syncType){
        if(syncType == Constants.SYNC_NONE) {
            FeatureChanges.removeAllChanges(mChangeTableName);
        }
        else{
            if(mCacheLoaded)
                reloadCache();

            final List<IGeometryCacheItem> cacheItems = mGeometryCache.getAll();
            for (IGeometryCacheItem cacheItem : cacheItems) {
                long id = cacheItem.getFeatureId();
                addChange(id, Constants.CHANGE_OPERATION_NEW);
                //add attach
                File attacheFolder = new File(mPath, "" + id);
                if (attacheFolder.isDirectory()) {
                    for (File attach : attacheFolder.listFiles()) {
                        String attachId = attach.getName();
                        if (attachId.equals(META)) {
                            continue;
                        }
                        Long attachIdL = Long.parseLong(attachId);
                        if (attachIdL >= Constants.MIN_LOCAL_FEATURE_ID) {
                            addChange(id, attachIdL, Constants.CHANGE_OPERATION_NEW);
                        }
                    }
                }
            }
        }
    }

    public void setSyncType(int syncType)
    {
        if( !isSyncable() ) {
            return;
        }

        if (mSyncType == syncType) {
            return;
        }

        if (syncType == Constants.SYNC_NONE) {
            mSyncType = syncType;

            new Thread( new Runnable()
            {
                public void run()
                {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                    applySync(Constants.SYNC_NONE);
                }
            }).start();

        } else if (mSyncType == Constants.SYNC_NONE && 0 != (syncType & Constants.SYNC_DATA)) {
            mSyncType = syncType;

            new Thread( new Runnable()
            {
                public void run()
                {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
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
        FeatureChanges.delete(mChangeTableName);

        return super.delete();
    }


    /**
     * Indicate if layer can sync data with remote server
     * @return true if layer can sync or false
     */
    public boolean isSyncable(){
        return true;
    }


    /**
     * Indicate if layer can send changes to remote server
     * @return true if layer can send changes to remote server or false
     */
    public boolean isRemoteReadOnly(){
        return !(mNGWLayerType == Connection.NGWResourceTypeVectorLayer);
    }
}
