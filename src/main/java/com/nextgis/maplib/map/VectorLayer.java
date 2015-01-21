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
import android.graphics.Color;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.datasource.Geo;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.SimpleLineStyle;
import com.nextgis.maplib.display.SimpleMarkerStyle;
import com.nextgis.maplib.util.ChangeFeatureItem;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.util.VectorCacheItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.*;

public class VectorLayer extends Layer
{
    protected boolean               mIsInitialized;
    protected int                   mGeometryType;
    protected List<VectorCacheItem> mVectorCacheItems;
    protected Uri                   mContentUri;
    protected UriMatcher            mUriMatcher;
    protected String                mAuthority;
    protected Map<String, Field>    mFields;

    protected static String CONTENT_TYPE;
    protected static String CONTENT_ITEM_TYPE;

    protected static final String JSON_GEOMETRY_TYPE_KEY  = "geometry_type";
    protected static final String JSON_IS_INITIALIZED_KEY = "is_inited";
    protected static final String JSON_FIELDS_KEY = "fields";

    public static final String FIELD_ID   = "_ID";
    public static final String FIELD_GEOM = "_GEOM";

    protected static final String CONTENT_PHOTO_TYPE  = "image/jpeg";
    protected static final String CONTENT_PHOTOS_TYPE = "vnd.android.cursor.dir/image";
    protected static final String NO_SYNC             = "no_sync";

    protected static final int TYPE_TABLE    = 1;
    protected static final int TYPE_FEATURE  = 2;
    protected static final int TYPE_PHOTO    = 3;
    protected static final int TYPE_PHOTO_ID = 4;


    public VectorLayer(
            Context context,
            File path)
    {
        super(context, path);


        if (!(context instanceof IGISApplication))
            throw new IllegalArgumentException(
                    "The context should be the instance of IGISApplication");


        mIsInitialized = false;
        mGeometryType = GTNone;

        IGISApplication application = (IGISApplication) context;
        mAuthority = application.getAuthority();
        mContentUri = Uri.parse("content://" + mAuthority + "/" + mPath.getName());
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        mUriMatcher.addURI(mAuthority, mPath.getName(), TYPE_TABLE);          //get all rows
        mUriMatcher.addURI(mAuthority, mPath.getName() + "/#", TYPE_FEATURE); //get single row
        mUriMatcher.addURI(mAuthority, mPath.getName() + "/#/photos",
                           TYPE_PHOTO);      //get photos for row
        mUriMatcher.addURI(mAuthority, mPath.getName() + "/#/photos/#",
                           TYPE_PHOTO_ID); //get photo by name


        CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + application.getAuthority() + "." + mPath.getName();
        CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd." + application.getAuthority() + "." + mPath.getName();
    }


    public String createFromGeoJSON(JSONObject geoJSONObject)
    {
        try {
            //check crs
            boolean isWGS84 = true; //if no crs tag - WGS84 CRS
            if (geoJSONObject.has(GEOJSON_CRS)) {
                JSONObject crsJSONObject = geoJSONObject.getJSONObject(GEOJSON_CRS);
                //the link is unsupported yet.
                if (!crsJSONObject.getString(GEOJSON_TYPE).equals(GEOJSON_NAME)) {
                    return mContext.getString(R.string.error_crs_unsuported);
                }
                JSONObject crsPropertiesJSONObject =
                        crsJSONObject.getJSONObject(GEOJSON_PROPERTIES);
                String crsName = crsPropertiesJSONObject.getString(GEOJSON_NAME);
                if (crsName.equals("urn:ogc:def:crs:OGC:1.3:CRS84")) { // WGS84
                    isWGS84 = true;
                } else if (crsName.equals("urn:ogc:def:crs:EPSG::3857") ||
                           crsName.equals("EPSG:3857")) { //Web Mercator
                    isWGS84 = false;
                } else {
                    return mContext.getString(R.string.error_crs_unsuported);
                }
            }

            //load contents to memory and reproject if needed
            JSONArray geoJSONFeatures = geoJSONObject.getJSONArray(GEOJSON_TYPE_FEATURES);
            if (0 == geoJSONFeatures.length()) {
                return mContext.getString(R.string.error_empty_dataset);
            }

            List<Feature> features = new ArrayList<>();
            List<Field> fields = new ArrayList<>();

            int geometryType = GTNone;
            for (int i = 0; i < geoJSONFeatures.length(); i++) {
                JSONObject jsonFeature = geoJSONFeatures.getJSONObject(i);
                //get geometry
                JSONObject jsonGeometry = jsonFeature.getJSONObject(GEOJSON_GEOMETRY);
                GeoGeometry geometry = GeoGeometryFactory.fromJson(jsonGeometry);
                if (geometryType == GTNone) {
                    geometryType = geometry.getType();
                } else if (!Geo.isGeometryTypeSame(geometryType, geometry.getType())) {
                    //skip different geometry type
                    continue;
                }

                //reproject if needed
                if (isWGS84) {
                    geometry.setCRS(CRS_WGS84);
                    geometry.project(CRS_WEB_MERCATOR);
                } else {
                    geometry.setCRS(CRS_WEB_MERCATOR);
                }

                int nId = i;
                if(jsonFeature.has(GEOJSON_ID))
                    nId = jsonFeature.getInt(GEOJSON_ID);
                Feature feature = new Feature(nId, fields); // ID == i
                feature.setGeometry(geometry);

                //normalize attributes
                JSONObject jsonAttributes = jsonFeature.getJSONObject(GEOJSON_PROPERTIES);
                Iterator<String> iter = jsonAttributes.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    Object value = jsonAttributes.get(key);
                    int nType = NOT_FOUND;
//check type
                    if (value instanceof Integer || value instanceof Long) {
                        nType = FTInteger;
                    } else if (value instanceof Double || value instanceof Float) {
                        nType = FTReal;
                    } else if (value instanceof Date) {
                        nType = FTDateTime;
                    } else if (value instanceof String) {
                        nType = FTString;
                    } else if (value instanceof JSONObject) {
                        nType = NOT_FOUND;
//the some list - need to check it type FTIntegerList, FTRealList, FTStringList
                    }

                    if(nType != NOT_FOUND) {
                        int fieldIndex = NOT_FOUND;
                        for (int j = 0; j < fields.size(); j++) {
                            if (fields.get(j).getName().equals(key)) {
                                fieldIndex = j;
                            }
                        }
                        if (fieldIndex == NOT_FOUND) { //add new field
                            Field field = new Field(nType, key, null);
                            fieldIndex = fields.size();
                            fields.add(field);
                        }
                        feature.setFieldValue(fieldIndex, value);
                    }
                }
                features.add(feature);
            }

            return initialize(features);
        } catch (JSONException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }

    protected String initialize(List<Feature> features)
    {
        Log.d(TAG, "init layer " + getName());
        if(features.isEmpty())
            return null;

        Feature firstFeature = features.get(0);
        List<Field> fields = firstFeature.getFields();

        String tableCreate = "CREATE TABLE IF NOT EXISTS " + mPath.getName() + " ( " + //table name is the same as the folder of the layer
                             FIELD_ID + " INTEGER PRIMARY KEY, " +
                             FIELD_GEOM + " BLOB";
        for(int i = 0; i < fields.size(); ++i)
        {
            Field field = fields.get(i);

            tableCreate += ", " + field.getName() + " ";
            switch (field.getType())
            {
                case FTString:
                    tableCreate += "TEXT";
                    break;
                case FTInteger:
                    tableCreate += "INTEGER";
                    break;
                case FTReal:
                    tableCreate += "REAL";
                    break;
                case FTDateTime:
                    tableCreate += "TIMESTAMP";
                    break;
            }
        }
        tableCreate += " );";

        Log.d(TAG, "create layer table: " + tableCreate);

        //1. create table and populate with values
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        db.execSQL(tableCreate);
        for(Feature feature : features) {
            ContentValues values = new ContentValues();
            values.put(FIELD_ID, feature.getId());
            try {
                values.put(FIELD_GEOM, feature.getGeometry().toBlob());
            } catch (IOException e) {
                e.printStackTrace();
            }
            for(int i = 0; i < fields.size(); ++i){
                if(!feature.isValuePresent(i))
                    continue;
                switch (fields.get(i).getType())
                {
                    case FTString:
                        values.put(fields.get(i).getName(), feature.getFieldValueAsString(i));
                        break;
                    case FTInteger:
                        Object intVal = feature.getFieldValue(i);
                        if(intVal instanceof Integer)
                            values.put(fields.get(i).getName(), (int)intVal);
                        else if(intVal instanceof Long)
                            values.put(fields.get(i).getName(), (long)intVal);
                        else
                            Log.d(TAG, "skip value: " + intVal.toString());
                        break;
                    case FTReal:
                        Object realVal = feature.getFieldValue(i);
                        if(realVal instanceof Double)
                            values.put(fields.get(i).getName(), (double)realVal);
                        else if(realVal instanceof Float)
                            values.put(fields.get(i).getName(), (float)realVal);
                        else
                            Log.d(TAG, "skip value: " + realVal.toString());
                        break;
                    case FTDateTime:
                        values.put(fields.get(i).getName(), feature.getFieldValueAsString(i));
                        break;
                }
            }
            db.insert(mPath.getName(), "", values);
        }

        //fill the geometry and labels array
        GeoEnvelope extents = new GeoEnvelope();
        mVectorCacheItems = new ArrayList<>();
        int geometryType = NOT_FOUND;
        for (Feature feature : features) {
            if(null == feature.getGeometry())
                continue;
            //update bbox
            extents.merge(feature.getGeometry().getEnvelope());
            //add to cache
            mVectorCacheItems.add(new VectorCacheItem(feature.getGeometry(), feature.getId()));

            if(geometryType == NOT_FOUND){
                geometryType = feature.getGeometry().getType();
            }
        }

        //2. save the layer properties to config.json
        mGeometryType = geometryType;
        mExtents = extents;
        mIsInitialized = true;
        setDefaultRenderer();

        mFields = new HashMap<>();
        for (Field field : fields){
            mFields.put(field.getName(), field);
        }

        save();

        if(null != mParent){ //notify the load is over
            LayerGroup layerGroup = (LayerGroup)mParent;
            layerGroup.onLayerChanged(this);
        }

        return null;
    }


    protected void setDefaultRenderer()
    {
        switch (mGeometryType)
        {
            case GTPoint:
            case GTMultiPoint:
                mRenderer = new SimpleFeatureRenderer(this, new SimpleMarkerStyle(Color.RED, Color.BLACK, 6, SimpleMarkerStyle.MarkerStyleCircle));
                break;
            case GTLineString:
            case GTMultiLineString:
                mRenderer = new SimpleFeatureRenderer(this, new SimpleLineStyle(Color.GREEN));
                break;
            case GTPolygon:
            case GTMultiPolygon:
            default:
                mRenderer = null;
        }
    }


    protected void reportError(String error)
    {
        Log.w(TAG, error);
    }

    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_GEOMETRY_TYPE_KEY, mGeometryType);
        rootConfig.put(JSON_IS_INITIALIZED_KEY, mIsInitialized);

        if(null != mFields) {
            JSONArray fields = new JSONArray();
            for(Field field : mFields.values()){
                JSONObject fieldJsonObject = field.toJSON();
                fields.put(fieldJsonObject);
            }
            rootConfig.put(JSON_FIELDS_KEY, fields);
        }

        if (null != mRenderer && mRenderer instanceof IJSONStore) {
            IJSONStore jsonStore = (IJSONStore) mRenderer;
            rootConfig.put(JSON_RENDERERPROPS_KEY, jsonStore.toJSON());
        }
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mGeometryType = jsonObject.getInt(JSON_GEOMETRY_TYPE_KEY);
        mIsInitialized = jsonObject.getBoolean(JSON_IS_INITIALIZED_KEY);

        if(jsonObject.has(JSON_FIELDS_KEY)) {
            mFields = new HashMap<>();
            JSONArray fields = jsonObject.getJSONArray(JSON_FIELDS_KEY);
            for(int i = 0; i < fields.length(); i++){
                Field field = new Field();
                field.fromJSON(fields.getJSONObject(i));
                mFields.put(field.getName(), field);
            }
        }

        if (jsonObject.has(JSON_RENDERERPROPS_KEY)) {
            setDefaultRenderer();

            if (null != mRenderer && mRenderer instanceof IJSONStore) {
                IJSONStore jsonStore = (IJSONStore) mRenderer;
                jsonStore.fromJSON(jsonObject.getJSONObject(JSON_RENDERERPROPS_KEY));
            }
        }

        if(mIsInitialized)
        {
            mExtents = new GeoEnvelope();

            reloadCache();
        }
    }

    protected void reloadCache()
    {
        //load vector cache
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(false);
        String[] columns = new String[]{FIELD_ID, FIELD_GEOM};
        Cursor cursor = db.query(mPath.getName(), columns, null, null, null, null, null);
        if(null != cursor && cursor.moveToFirst()) {
            mVectorCacheItems = new ArrayList<>();
            do{
                try {
                    GeoGeometry geoGeometry = GeoGeometryFactory.fromBlob(cursor.getBlob(1));
                    int nId = cursor.getInt(0);
                    mExtents.merge(geoGeometry.getEnvelope());
                    mVectorCacheItems.add(new VectorCacheItem(geoGeometry, nId));
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            while (cursor.moveToNext());
        }
    }

    @Override
    public boolean delete()
    {
        //drop table
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        String tableDrop = "DROP TABLE IF EXISTS " + mPath.getName();
        db.execSQL(tableDrop);

        return super.delete();
    }


    public List<VectorCacheItem> getVectorCache()
    {
        return mVectorCacheItems;
    }

    @Override
    public boolean isValid()
    {
        return mIsInitialized;
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
                    sortOrder = FIELD_ID + " ASC";
                }
                db = map.getDatabase(true);
                cursor = db.query(mPath.getName(), projection, selection, selectionArgs, null, null, sortOrder);
                cursor.setNotificationUri(getContext().getContentResolver(), mContentUri);
                return cursor;
            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = FIELD_ID + " = " + featureId;
                } else {
                    selection = selection + " AND " + FIELD_ID + " = " + featureId;
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
                                row[i] = photoFile.getPath();
                            } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.MIME_TYPE)==0) {
                                row[i] = CONTENT_PHOTO_TYPE;
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
                        row[i] = photoFile.getPath();
                    } else if (projection[i].compareToIgnoreCase(MediaStore.MediaColumns.MIME_TYPE)==0) {
                        row[i] = CONTENT_PHOTO_TYPE;
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
                return CONTENT_TYPE;
            case TYPE_FEATURE:
                return CONTENT_ITEM_TYPE;
            case TYPE_PHOTO:
                return CONTENT_PHOTOS_TYPE;
            case TYPE_PHOTO_ID:
                return CONTENT_PHOTO_TYPE;
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
                return new String[]{CONTENT_PHOTO_TYPE};
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
                    if(contentValues.containsKey(FIELD_GEOM)){
                        try {
                            GeoGeometry geom = GeoGeometryFactory.fromBlob(contentValues.getAsByteArray(
                                    FIELD_GEOM));
                            mVectorCacheItems.add(new VectorCacheItem(geom, (int)rowID));
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    Uri resultUri = ContentUris.withAppendedId(mContentUri, rowID);
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if(bFromNetwork) {
                        getContext().getContentResolver().notifyChange(resultUri, null, false);
                        notifyLayerChanged();
                    }
                    else {
                        addChange("" + rowID, ChangeFeatureItem.TYPE_NEW);
                        getContext().getContentResolver().notifyChange(resultUri, null, true);
                        notifyLayerChanged();
                    }
                    return resultUri;
                }
                return null;
            case TYPE_PHOTO:
                List<String> pathSegments = uri.getPathSegments();
                String featureId = pathSegments.get(pathSegments.size() - 2);
                //get photo path
                File photoFolder = new File(mPath, featureId);
                long maxId = 1000; //we start files from 1000 to not overlap with NGW files id's
                for(File file : photoFolder.listFiles()){
                    long val = Long.parseLong(file.getName().replace(".jpg", ""));
                    if(val >= maxId)
                        maxId = val + 1;
                }
                String fileName = maxId + ".jpg";
                File photo = new File(photoFolder, fileName);
                try {
                    if(photo.createNewFile()) {
                        Uri resultUri = ContentUris.withAppendedId(uri, maxId);
                        String fragment = uri.getFragment();
                        boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                        if(bFromNetwork) {
                            getContext().getContentResolver().notifyChange(resultUri, null, false);
                        }
                        else {
                            addChange(featureId, fileName, ChangeFeatureItem.TYPE_NEW);
                            getContext().getContentResolver().notifyChange(resultUri, null, true);
                        }

                        return resultUri;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
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

                if(result > 0){
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if(bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                        notifyLayerChanged();
                    }
                    else {
                        addChange("" + NOT_FOUND, ChangeFeatureItem.TYPE_DELETE);
                        getContext().getContentResolver().notifyChange(uri, null, true);
                        notifyLayerChanged();
                    }

                    //clear cache
                    mVectorCacheItems.clear();
                }
                return result;
            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = FIELD_ID + " = " + featureId;
                } else {
                    selection = selection + " AND " + FIELD_ID + " = " + featureId;
                }
                db = map.getDatabase(false);
                result = db.delete(mPath.getName(), selection, selectionArgs);

                if(result > 0){
                    //remove cached item
                    int id = Integer.parseInt(featureId);
                    for(VectorCacheItem item : mVectorCacheItems){
                        if(item.getId() == id){
                            mVectorCacheItems.remove(item);
                            break;
                        }
                    }

                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if(bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                        notifyLayerChanged();
                    }
                    else {
                        addChange(featureId, ChangeFeatureItem.TYPE_DELETE);
                        getContext().getContentResolver().notifyChange(uri, null, true);
                        notifyLayerChanged();
                    }
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
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if(bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    }
                    else {
                        addChange(featureId, "" + NOT_FOUND, ChangeFeatureItem.TYPE_DELETE);
                        getContext().getContentResolver().notifyChange(uri, null);
                    }
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
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if(bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    }
                    else {
                        addChange(featureId, photoName, ChangeFeatureItem.TYPE_DELETE);
                        getContext().getContentResolver().notifyChange(uri, null);
                    }
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
        int result;

        int uriType = mUriMatcher.match(uri);
        switch (uriType)
        {
            case TYPE_TABLE:
                db = map.getDatabase(false);
                result = db.update(mPath.getName(), values, selection, selectionArgs);

                if(result > 0){
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if(bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                        notifyLayerChanged();
                    }
                    else {
                        addChange("" + NOT_FOUND, ChangeFeatureItem.TYPE_CHANGED);
                        getContext().getContentResolver().notifyChange(uri, null);
                        notifyLayerChanged();
                    }
                }
                if(values.containsKey(FIELD_GEOM))
                    reloadCache();
                return result;
            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = FIELD_ID + " = " + featureId;
                } else {
                    selection = selection + " AND " + FIELD_ID + " = " + featureId;
                }
                db = map.getDatabase(false);
                result = db.update(mPath.getName(), values, selection, selectionArgs);

                if(result > 0){
                    //change cached item
                    if(values.containsKey(FIELD_GEOM)) {
                        int id = Integer.parseInt(featureId);
                        for (VectorCacheItem item : mVectorCacheItems) {
                            if (item.getId() == id) {
                                try {
                                    item.setGeoGeometry(GeoGeometryFactory.fromBlob(values.getAsByteArray(FIELD_GEOM)));

                                } catch (IOException | ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    }
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if(bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                        notifyLayerChanged();
                    }
                    else {
                        addChange(featureId, ChangeFeatureItem.TYPE_CHANGED);
                        getContext().getContentResolver().notifyChange(uri, null);
                        notifyLayerChanged();
                    }
                }
                return result;
            case TYPE_PHOTO:
            case TYPE_PHOTO_ID:
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
                int nMode = ParcelFileDescriptor.MODE_READ_ONLY;
                //mode 	May be "w", "wa", "rw", or "rwt".
                switch (mode) {
                    case "w":
                    case "rw":
                        nMode = ParcelFileDescriptor.MODE_READ_WRITE;
                        break;
                    case "wa":
                        nMode = ParcelFileDescriptor.MODE_READ_WRITE |
                                ParcelFileDescriptor.MODE_APPEND;
                        break;
                    case "rwt":
                        nMode = ParcelFileDescriptor.MODE_READ_WRITE |
                                ParcelFileDescriptor.MODE_TRUNCATE;
                        break;
                }

                return ParcelFileDescriptor.open(new File(mPath, featureId + "/" + photoName), nMode);
            default:
                throw new FileNotFoundException();
        }
    }

    /*
    To put image to the feature
    Uri newUri = ... content://com.nextgis.mobile.provider/layer_xxxxxxxxx/1/photos
    Uri uri = getContentResolver().insert(newUri, null);
    try {
        OutputStream outStream = getContentResolver().openOutputStream(uri);
        sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outStream);
        outStream.close();
    } catch (Exception e) {
        Log.e(TAG, "exception while writing image", e);
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    To get image using uri

    Uri featureUri = content://com.nextgis.mobile.provider/layer_xxxxxxxxx/1
    Uri thisPhotoUri =
          ContentUris.withAppendedId(featureUri, photoId);
    InputStream inStream = null;
    try {
       inStream = resolver.openInputStream(thisPhotoUri);

       // what to do with the stream is up to you
       // I simply create a bitmap to display it
       Bitmap bm = BitmapFactory.decodeStream(inStream);
       FrameLayout frame =
             (FrameLayout)findViewById(R.id.picture_frame);
       ImageView view = new ImageView(getApplicationContext());
       view.setImageBitmap(bm);
       frame.addView(view);
    } catch (FileNotFoundException e) {
       Log.e(TAG, "file not found " + thisPhotoUri, e);
    }
    finally {
       if (inStream != null) {
          try {
             inStream.close();
          } catch (IOException e) {
             Log.e(TAG, "could not close stream", e);
          }
       }
    }

    also it can be used
    Uri newUri = ... content://com.nextgis.mobile.provider/layer_xxxxxxxxx/1/photos
    Cursor cursor = resolver.query(newUri, {MediaStore.MediaColumns.DATA}, null....)
    File bitmapFile = new File(cursor.getString(MediaStore.MediaColumns.DATA))
    and open using real path
    */

    protected void addChange(String featureId, int operation)
    {
        //nothing to do
    }

    protected void addChange(String featureId, String photoName, int operation)
    {
        //nothing to do
    }
}
