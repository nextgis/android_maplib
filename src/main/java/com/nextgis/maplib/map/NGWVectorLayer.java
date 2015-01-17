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
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.util.ChangeFeatureItem;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplib.util.VectorCacheItem;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_VECTOR;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.Constants.TAG;


public class NGWVectorLayer extends VectorLayer implements INGWLayer
{
    protected String      mAccount;
    protected String      mURL;
    protected NetworkUtil mNet;
    protected String      mLogin;
    protected String      mPassword;

    protected List<ChangeFeatureItem> mChanges;

    protected static Uri        mContentUri;
    protected static UriMatcher mUriMatcher;

    protected static String CONTACT_CONTENT_TYPE;
    protected static String CONTACT_CONTENT_ITEM_TYPE;
    protected static final String CONTACT_CONTENT_PHOTO_TYPE  = "image/jpeg";
    protected static final String CONTACT_CONTENT_PHOTOS_TYPE = "vnd.android.cursor.dir/image";

    protected static final String JSON_ACCOUNT_KEY  = "account";
    protected static final String JSON_URL_KEY      = "url";
    protected static final String JSON_LOGIN_KEY    = "login";
    protected static final String JSON_PASSWORD_KEY = "password";
    protected static final String JSON_CHANGES_KEY  = "changes";

    protected static final int TYPE_TABLE    = 1;
    protected static final int TYPE_FEATURE  = 2;
    protected static final int TYPE_PHOTO    = 3;
    protected static final int TYPE_PHOTO_ID = 4;


    public NGWVectorLayer(
            Context context,
            File path)
    {
        super(context, path);

        if (!(context instanceof IGISApplication))
            throw new IllegalArgumentException(
                    "The context should be the instance of IGISApplication");


        mNet = new NetworkUtil(context);

        IGISApplication application = (IGISApplication) context;
        mContentUri = Uri.parse("content://" + application.getAuthority() + "/" + mPath.getName());
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        mUriMatcher.addURI(application.getAuthority(), mPath.getName(),
                           TYPE_TABLE);              //get all rows
        mUriMatcher.addURI(application.getAuthority(), mPath.getName() + "/#",
                           TYPE_FEATURE);     //get single row
        mUriMatcher.addURI(application.getAuthority(), mPath.getName() + "/#/photos",
                           TYPE_PHOTO); //get photos for row
        mUriMatcher.addURI(application.getAuthority(), mPath.getName() + "/#/photos/#",
                           TYPE_PHOTO_ID); //get photo by name


        CONTACT_CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + application.getAuthority() + "." + mPath.getName();
        CONTACT_CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd." + application.getAuthority() + "." + mPath.getName();

        mChanges = new ArrayList<>();
    }


    @Override
    public void setAccount(String account)
    {
        mAccount = account;
    }


    public void setURL(String URL)
    {
        mURL = URL;
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
        rootConfig.put(JSON_ACCOUNT_KEY, mAccount);
        rootConfig.put(JSON_URL_KEY, mURL);
        rootConfig.put(JSON_LOGIN_KEY, mLogin);
        rootConfig.put(JSON_PASSWORD_KEY, mPassword);
        JSONArray changes = new JSONArray();
        for(ChangeFeatureItem change : mChanges){
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
        mAccount = jsonObject.getString(JSON_ACCOUNT_KEY);
        mURL = jsonObject.getString(JSON_URL_KEY);
        if (jsonObject.has(JSON_LOGIN_KEY))
            mLogin = jsonObject.getString(JSON_LOGIN_KEY);
        if (jsonObject.has(JSON_PASSWORD_KEY))
            mPassword = jsonObject.getString(JSON_PASSWORD_KEY);
        if(jsonObject.has(JSON_CHANGES_KEY)){
            JSONArray array = jsonObject.getJSONArray(JSON_CHANGES_KEY);
            for(int i =0; i < array.length(); i++){
                JSONObject change = array.getJSONObject(i);
                ChangeFeatureItem item = new ChangeFeatureItem(0 ,0);
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
    public String getAccount()
    {
        return mAccount;
    }


    public void downloadAsync()
    {
        new DownloadTask().execute();
    }


    public String download()
    {
        if (!mNet.isNetworkAvailable()) { //return tile from cache
            return getContext().getString(R.string.error_network_unavailable);
        }

        try {

            final HttpGet get = new HttpGet(mURL);
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

    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder)
    {
        MapContentProviderHelper map = (MapContentProviderHelper)MapBase.getInstance();
        if(null == map)
            throw new IllegalArgumentException("The map should extends MapContentProviderHelper or inherited");

        SQLiteDatabase db;
        Cursor cursor;
        MatrixCursor matrixCursor;
        String featureId;
        String photoName;
        List<String> pathSegments;

        int uriType = mUriMatcher.match(uri);
        switch (uriType)
        {
            case TYPE_TABLE:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = ID_FIELD + " ASC";
                }
                db = map.getDatabase(true);
                cursor = db.query(mPath.getName(), projection, selection, selectionArgs, null, null, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(), mContentUri);
                return cursor;
            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = ID_FIELD + " = " + featureId;
                } else {
                    selection = selection + " AND " + ID_FIELD + " = " + featureId;
                }
                db = map.getDatabase(true);
                cursor = db.query(mPath.getName(), projection, selection, selectionArgs, null, null,
                                  sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(), mContentUri);
                return cursor;
            case TYPE_PHOTO:
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 2);
                if (projection == null) {
                    projection = new String[] {
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            MediaStore.MediaColumns.SIZE,
                            MediaStore.MediaColumns._ID,
                            MediaStore.MediaColumns.MIME_TYPE};
                }
                matrixCursor = new MatrixCursor(projection);
                //get photo path
                File photoFolder = new File(mPath, featureId); //the photos store in id folder in layer folder
                for (File photoFile : photoFolder.listFiles()) {
                    if(photoFile.getName().endsWith("jpg")){
                        Object[] row = new Object[projection.length];
                        for (int i = 0; i < projection.length; i++) {

                            if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.DISPLAY_NAME) == 0) {
                                row[i] = photoFile.getName();
                            } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.SIZE) == 0) {
                                row[i] = photoFile.length();
                            } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.DATA) == 0) {
                                row[i] = photoFile;
                            } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.MIME_TYPE)==0) {
                                row[i] = CONTACT_CONTENT_PHOTO_TYPE;
                            } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.DATE_ADDED)==0 ||
                                       projection[i].compareToIgnoreCase(MediaStore.MediaColumns.DATE_MODIFIED)==0 ||
                                       projection[i].compareToIgnoreCase("datetaken")==0) {
                                row[i] = photoFile.lastModified();
                            } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns._ID)==0) {
                                row[i] = photoFile.getName();
                            } else if (projection[i].compareToIgnoreCase("orientation")==0) {
                                row[i] = "vertical";
                            }
                        }
                        matrixCursor.addRow(row);
                    }
                }
                return matrixCursor;
            case TYPE_PHOTO_ID:
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 3);
                photoName = uri.getLastPathSegment();
                if (projection == null) {
                    projection = new String[] {
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            MediaStore.MediaColumns.SIZE,
                            MediaStore.MediaColumns._ID,
                            MediaStore.MediaColumns.MIME_TYPE};
                }
                matrixCursor = new MatrixCursor(projection);
                if(!photoName.endsWith("jpg"))
                    photoName += ".jpg";
                //get photo path
                File photoFile = new File(mPath, featureId + "/" + photoName); //the photos store in id folder in layer folder
                Object[] row = new Object[projection.length];
                for (int i = 0; i < projection.length; i++) {

                    if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.DISPLAY_NAME) == 0) {
                        row[i] = photoFile.getName();
                    } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.SIZE) == 0) {
                        row[i] = photoFile.length();
                    } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.DATA) == 0) {
                        row[i] = photoFile;
                    } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.MIME_TYPE)==0) {
                        row[i] = CONTACT_CONTENT_PHOTO_TYPE;
                    } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.DATE_ADDED)==0 ||
                               projection[i].compareToIgnoreCase(MediaStore.MediaColumns.DATE_MODIFIED)==0 ||
                               projection[i].compareToIgnoreCase("datetaken")==0) {
                        row[i] = photoFile.lastModified();
                    } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns._ID)==0) {
                        row[i] = photoFile.getName();
                    } else if (projection[i].compareToIgnoreCase("orientation")==0) {
                        row[i] = "vertical";
                    }
                }
                matrixCursor.addRow(row);
                return matrixCursor;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
    }


    public String getType(Uri uri)
    {
        int uriType = mUriMatcher.match(uri);
        switch (uriType) {
            case TYPE_TABLE:
                return CONTACT_CONTENT_TYPE;
            case TYPE_FEATURE:
                return CONTACT_CONTENT_ITEM_TYPE;
            case TYPE_PHOTO:
                return CONTACT_CONTENT_PHOTOS_TYPE;
            case TYPE_PHOTO_ID:
                return CONTACT_CONTENT_PHOTO_TYPE;
        }
        return null;
    }


    public String[] getStreamTypes(
            Uri uri,
            String mimeTypeFilter)
    {
        int uriType = mUriMatcher.match(uri);
        switch (uriType) {
            case TYPE_PHOTO_ID:
                return new String[]{CONTACT_CONTENT_PHOTO_TYPE};
        }
        return null;
    }


    public Uri insert(
            Uri uri,
            ContentValues contentValues)
    {
        MapContentProviderHelper map = (MapContentProviderHelper)MapBase.getInstance();
        if(null == map)
            throw new IllegalArgumentException("The map should extends MapContentProviderHelper or inherited");

        SQLiteDatabase db;

        int uriType = mUriMatcher.match(uri);
        switch (uriType)
        {
            case TYPE_TABLE:
                db = map.getDatabase(false);
                long rowID = db.insert(mPath.getName(), null, contentValues);
                if(rowID != NOT_FOUND) {
                    if(contentValues.containsKey(GEOM_FIELD)){
                        try {
                            GeoGeometry geom = GeoGeometry.fromBlob(contentValues.getAsByteArray(GEOM_FIELD));
                            mVectorCacheItems.add(new VectorCacheItem(geom, (int)rowID));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    addChange("" + rowID, ChangeFeatureItem.TYPE_NEW);
                    Uri resultUri = ContentUris.withAppendedId(mContentUri, rowID);
                    getContext().getContentResolver().notifyChange(resultUri, null);
                    return resultUri;
                }
                return null;
            case TYPE_PHOTO:
            case TYPE_FEATURE:
            case TYPE_PHOTO_ID:
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
    }


    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        MapContentProviderHelper map = (MapContentProviderHelper)MapBase.getInstance();
        if(null == map)
            throw new IllegalArgumentException("The map should extends MapContentProviderHelper or inherited");

        SQLiteDatabase db;
        String featureId;
        String photoName;
        List<String> pathSegments;
        int result;

        int uriType = mUriMatcher.match(uri);
        switch (uriType)
        {
            case TYPE_TABLE:
                db = map.getDatabase(false);
                result = db.delete(mPath.getName(), selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                if(result > 0){
                    addChange("" + NOT_FOUND, ChangeFeatureItem.TYPE_DELETE);
                    //clear cache
                    mVectorCacheItems.clear();
                }
                return result;
            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = ID_FIELD + " = " + featureId;
                } else {
                    selection = selection + " AND " + ID_FIELD + " = " + featureId;
                }
                db = map.getDatabase(false);
                result = db.delete(mPath.getName(), selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                if(result > 0){
                    //remove cached item
                    int id = Integer.parseInt(featureId);
                    for(VectorCacheItem item : mVectorCacheItems){
                        if(item.getId() == id){
                            mVectorCacheItems.remove(item);
                            break;
                        }
                    }
                    addChange(featureId, ChangeFeatureItem.TYPE_DELETE);
                }
                return result;
            case TYPE_PHOTO:
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 2);
                result = 0;
                //get photo path
                File photoFolder = new File(mPath, featureId); //the photos store in id folder in layer folder
                for (File photoFile : photoFolder.listFiles()) {
                    if(photoFile.getName().endsWith("jpg")){
                        if(photoFile.delete()){
                            result++;
                        }
                    }
                }
                if(result > 0){
                    addChange(featureId, "" + NOT_FOUND, ChangeFeatureItem.TYPE_PHOTO_DELETE);
                }
                return result;
            case TYPE_PHOTO_ID:
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 3);
                photoName = uri.getLastPathSegment();

                if(!photoName.endsWith("jpg"))
                    photoName += ".jpg";
                //get photo path
                File photoFile = new File(mPath, featureId + "/" + photoName); //the photos store in id folder in layer folder
                if(photoFile.delete()){
                    addChange(featureId, photoName, ChangeFeatureItem.TYPE_PHOTO_DELETE);
                    return 1;
                }
                return 0;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
    }


    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        MapContentProviderHelper map = (MapContentProviderHelper)MapBase.getInstance();
        if(null == map)
            throw new IllegalArgumentException("The map should extends MapContentProviderHelper or inherited");

        SQLiteDatabase db;
        String featureId;
        String photoName;
        List<String> pathSegments;
        int result;

        int uriType = mUriMatcher.match(uri);
        switch (uriType)
        {
            case TYPE_TABLE:
                db = map.getDatabase(false);
                result = db.update(mPath.getName(), values, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                if(result > 0){
                    addChange("" + NOT_FOUND, ChangeFeatureItem.TYPE_CHANGED);
                    //clear cache
                    mVectorCacheItems.clear();
                }
                return result;
            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = ID_FIELD + " = " + featureId;
                } else {
                    selection = selection + " AND " + ID_FIELD + " = " + featureId;
                }
                db = map.getDatabase(false);
                result = db.delete(mPath.getName(), selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                if(result > 0){
                    //remove cached item
                    int id = Integer.parseInt(featureId);
                    for(VectorCacheItem item : mVectorCacheItems){
                        if(item.getId() == id){
                            mVectorCacheItems.remove(item);
                            break;
                        }
                    }
                    addChange(featureId, ChangeFeatureItem.TYPE_DELETE);
                }
                return result;
            case TYPE_PHOTO:
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 2);
                result = 0;
                //get photo path
                File photoFolder = new File(mPath, featureId); //the photos store in id folder in layer folder
                for (File photoFile : photoFolder.listFiles()) {
                    if(photoFile.getName().endsWith("jpg")){
                        if(photoFile.delete()){
                            result++;
                        }
                    }
                }
                if(result > 0){
                    addChange(featureId, "" + NOT_FOUND, ChangeFeatureItem.TYPE_PHOTO_DELETE);
                }
                return result;
            case TYPE_PHOTO_ID:
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 3);
                photoName = uri.getLastPathSegment();

                if(!photoName.endsWith("jpg"))
                    photoName += ".jpg";
                //get photo path
                File photoFile = new File(mPath, featureId + "/" + photoName); //the photos store in id folder in layer folder
                if(photoFile.delete()){
                    addChange(featureId, photoName, ChangeFeatureItem.TYPE_PHOTO_DELETE);
                    return 1;
                }
                return 0;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException
    {
        int uriType = mUriMatcher.match(uri);
        switch (uriType) {
            case TYPE_PHOTO_ID:
                List<String> pathSegments = uri.getPathSegments();
                String featureId = pathSegments.get(pathSegments.size() - 3);
                String photoName = uri.getLastPathSegment();
                if(!photoName.endsWith("jpg"))
                    photoName += ".jpg";
                return ParcelFileDescriptor.open(new File(mPath, featureId + "/" + photoName), ParcelFileDescriptor.MODE_READ_ONLY);
            default:
                throw new FileNotFoundException();
        }
    }

    protected void addChange(String featureId, int operation)
    {
        //mChanges.add(new ChangeFeatureItem(-1, ChangeFeatureItem.TYPE_DELETE));
    }

    protected void addChange(String featureId, String photoName, int operation)
    {
        //mChanges.add(new ChangeFeatureItem(-1, ChangeFeatureItem.TYPE_DELETE));
    }


}
