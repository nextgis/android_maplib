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

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.api.IStyleRule;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.display.RuleFeatureRenderer;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.SimpleLineStyle;
import com.nextgis.maplib.display.SimpleMarkerStyle;
import com.nextgis.maplib.display.SimplePolygonStyle;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.util.AttachItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.VectorCacheItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.*;
import static com.nextgis.maplib.util.GeoConstants.FTDate;


public class VectorLayer
        extends Layer
{
    protected boolean               mIsInitialized;
    protected int                   mGeometryType;
    protected List<VectorCacheItem> mVectorCacheItems;
    protected Uri                   mContentUri;
    protected UriMatcher            mUriMatcher;
    protected String                mAuthority;
    protected Map<String, Field>    mFields;
    protected VectorCacheItem       mTempCacheItem;
    protected long                  mUniqId;
    protected boolean mCacheLoaded;

    protected static String CONTENT_TYPE;
    protected static String CONTENT_ITEM_TYPE;

    protected static final String JSON_GEOMETRY_TYPE_KEY  = "geometry_type";
    protected static final String JSON_IS_INITIALIZED_KEY = "is_inited";
    protected static final String JSON_FIELDS_KEY         = "fields";

    public static final String NOTIFY_DELETE            = "com.nextgis.maplib.notify_delete";
    public static final String NOTIFY_DELETE_ALL        = "com.nextgis.maplib.notify_delete_all";
    public static final String NOTIFY_INSERT            = "com.nextgis.maplib.notify_insert";
    public static final String NOTIFY_UPDATE            = "com.nextgis.maplib.notify_update";
    public static final String NOTIFY_UPDATE_ALL        = "com.nextgis.maplib.notify_update_all";
    public static final String NOTIFY_UPDATE_FIELDS     = "com.nextgis.maplib.notify_update_fields";
    public static final String NOTIFY_FEATURE_ID_CHANGE = "com.nextgis.maplib.notify_change_id";
    public static final String NOTIFY_LAYER_NAME        = "layer_name";


    protected static final String CONTENT_ATTACH_TYPE = "vnd.android.cursor.dir/*";
    protected static final String NO_SYNC             = "no_sync";

    protected static final int TYPE_TABLE     = 1;
    protected static final int TYPE_FEATURE   = 2;
    protected static final int TYPE_ATTACH    = 3;
    protected static final int TYPE_ATTACH_ID = 4;

    protected static final String META = "meta.json";

    public static final String ATTACH_DISPLAY_NAME = MediaStore.MediaColumns.DISPLAY_NAME;
    public static final String ATTACH_SIZE         = MediaStore.MediaColumns.SIZE;
    public static final String ATTACH_ID           = MediaStore.MediaColumns._ID;
    public static final String ATTACH_MIME_TYPE    = MediaStore.MediaColumns.MIME_TYPE;
    public static final String ATTACH_DATA         = MediaStore.MediaColumns.DATA;
    public static final String ATTACH_DATE_ADDED   = MediaStore.MediaColumns.DATE_ADDED;
    public static final String ATTACH_DESCRIPTION  = MediaStore.Images.ImageColumns.DESCRIPTION;

    public static final int COLUMNTYPE_UNKNOWN = 0;
    public static final int COLUMNTYPE_STRING  = 1;
    public static final int COLUMNTYPE_LONG    = 2;


    protected Map<String, Map<String, AttachItem>> mAttaches;


    public VectorLayer(
            Context context,
            File path)
    {
        super(context, path);

        mCacheLoaded = false;

        if (!(context instanceof IGISApplication)) {
            throw new IllegalArgumentException(
                    "The context should be the instance of IGISApplication");
        }


        mIsInitialized = false;
        mGeometryType = GTNone;

        IGISApplication application = (IGISApplication) context;
        mAuthority = application.getAuthority();
        mContentUri = Uri.parse("content://" + mAuthority + "/" + mPath.getName());
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        mUriMatcher.addURI(mAuthority, mPath.getName(), TYPE_TABLE);          //get all rows
        mUriMatcher.addURI(mAuthority, mPath.getName() + "/#", TYPE_FEATURE); //get single row
        mUriMatcher.addURI(
                mAuthority, mPath.getName() + "/#/attach", TYPE_ATTACH);      //get attaches for row
        mUriMatcher.addURI(
                mAuthority, mPath.getName() + "/#/attach/#", TYPE_ATTACH_ID); //get attach by id


        CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + application.getAuthority() + "." + mPath.getName();
        CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd." + application.getAuthority() + "." + mPath.getName();

        mVectorCacheItems = new ArrayList<>();
        mAttaches = new HashMap<>();

        mLayerType = LAYERTYPE_LOCAL_VECTOR;

        mUniqId = Constants.NOT_FOUND;
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
                    return mContext.getString(R.string.error_crs_unsupported);
                }
                JSONObject crsPropertiesJSONObject =
                        crsJSONObject.getJSONObject(GEOJSON_PROPERTIES);
                String crsName = crsPropertiesJSONObject.getString(GEOJSON_NAME);
                switch (crsName) {
                    case "urn:ogc:def:crs:OGC:1.3:CRS84":  // WGS84
                        isWGS84 = true;
                        break;
                    case "urn:ogc:def:crs:EPSG::3857":
                    case "EPSG:3857":  //Web Mercator
                        isWGS84 = false;
                        break;
                    default:
                        return mContext.getString(R.string.error_crs_unsupported);
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
                } else if (geometryType != geometry.getType()) {
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
                if (jsonFeature.has(GEOJSON_ID)) {
                    nId = jsonFeature.optInt(GEOJSON_ID, nId);
                }
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

                    if (nType != NOT_FOUND) {
                        int fieldIndex = NOT_FOUND;
                        for (int j = 0; j < fields.size(); j++) {
                            if (fields.get(j).getName().equals(key)) {
                                fieldIndex = j;
                                break;
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

            return initialize(fields, features, NOT_FOUND);
        } catch (JSONException | SQLiteException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }


    public String createFromGeoJSON(
            JSONObject geoJSONObject,
            List<Field> fields,
            int geometryType,
            int srs)
    {
        try {
            //check crs
            boolean isWGS84 = srs == GeoConstants.CRS_WGS84;
            if (!isWGS84 && srs != GeoConstants.CRS_WEB_MERCATOR) {
                return mContext.getString(R.string.error_crs_unsupported);
            }

            //load contents to memory and reproject if needed
            JSONArray geoJSONFeatures = geoJSONObject.getJSONArray(GEOJSON_TYPE_FEATURES);
            if (0 == geoJSONFeatures.length()) {
                return mContext.getString(R.string.error_empty_dataset);
            }

            List<Feature> features = new ArrayList<>();
            for (int i = 0; i < geoJSONFeatures.length(); i++) {
                JSONObject jsonFeature = geoJSONFeatures.getJSONObject(i);
                //get geometry
                JSONObject jsonGeometry = jsonFeature.getJSONObject(GEOJSON_GEOMETRY);
                GeoGeometry geometry = GeoGeometryFactory.fromJson(jsonGeometry);
                if (geometryType != geometry.getType()) {
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
                if (jsonFeature.has(GEOJSON_ID)) {
                    nId = jsonFeature.optInt(GEOJSON_ID, nId);
                }
                Feature feature = new Feature(nId, fields); // ID == i
                feature.setGeometry(geometry);

                //normalize attributes
                JSONObject jsonAttributes = jsonFeature.getJSONObject(GEOJSON_PROPERTIES);
                Iterator<String> iter = jsonAttributes.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    Object value = jsonAttributes.get(key);

                    int fieldIndex = NOT_FOUND;
                    for (int j = 0; j < fields.size(); j++) {
                        if (fields.get(j).getName().equals(key)) {
                            fieldIndex = j;
                            break;
                        }
                    }

                    if (fieldIndex != NOT_FOUND) {
                        value = parseDateTime(value, fields.get(fieldIndex).getType());
                        feature.setFieldValue(fieldIndex, value);
                    }
                }
                features.add(feature);
            }

            return initialize(fields, features, NOT_FOUND);
        } catch (JSONException | SQLiteException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }

    protected Object parseDateTime(Object value, int type) {
        SimpleDateFormat sdf = null;

        switch (type) {
            case FTDate:
                sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                break;
            case FTTime:
                sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                break;
            case FTDateTime:
                sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
                break;
        }

        if (sdf != null && value instanceof String)
            try {
                value = sdf.parse((String) value).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

        return value;
    }


    public String initialize(
            List<Field> fields,
            List<Feature> features,
            int geometryType)
            throws SQLiteException
    {
        Log.d(TAG, "init layer " + getName());

        //filter out forbidden fields

        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            String fieldName = field.getName();
            if (NGWUtil.containsCaseInsensitive(fieldName, VECTOR_FORBIDDEN_FIELDS) ||
                fieldName.startsWith("@")) {
                fields.remove(i);
                i--;

                String warning = getContext().getString(R.string.warning_remove_field);
                reportError(String.format(warning, fieldName));
            } else if (fieldName.contains(":")) {
                field.setName(fieldName.replace(":", "_"));
            }
        }

        String tableCreate = "CREATE TABLE IF NOT EXISTS " + mPath.getName() + " ( " +
                             //table name is the same as the folder of the layer
                             FIELD_ID + " INTEGER PRIMARY KEY, " +
                             FIELD_GEOM + " BLOB";
        for (int i = 0; i < fields.size(); ++i) {
            Field field = fields.get(i);

            tableCreate += ", " + field.getName() + " ";
            switch (field.getType()) {
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
                case FTDate:
                case FTTime:
                    tableCreate += "TIMESTAMP";
                    break;
            }
        }
        tableCreate += " );";

        Log.d(TAG, "create layer table: " + tableCreate);

        //1. create table and populate with values
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(false);
        db.execSQL(tableCreate);

        mGeometryType = geometryType;

        for (Feature feature : features) {
            createFeature(feature, fields, db);
        }

        mIsInitialized = true;
        setDefaultRenderer();

        mFields = new HashMap<>();
        for (Field field : fields) {
            mFields.put(field.getName(), field);
        }

        save();

        if (null != mParent && mParent instanceof LayerGroup) { //notify the load is over
            LayerGroup layerGroup = (LayerGroup) mParent;
            layerGroup.onLayerChanged(this);
        }

        return null;
    }

    protected void createFeature(Feature feature, List<Field> fields, SQLiteDatabase db)
            throws SQLiteException{
        //1. check if such id already used
        // maybe was added previous session

        if(!mCacheLoaded)
            reloadCache();

        for(VectorCacheItem item : mVectorCacheItems){
            if(item.getId() == feature.getId())
                return;
        }

        ContentValues values = new ContentValues();
        values.put(FIELD_ID, feature.getId());
        try {
            values.put(FIELD_GEOM, feature.getGeometry().toBlob());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < fields.size(); ++i) {
            if (!feature.isValuePresent(i)) {
                continue;
            }
            switch (fields.get(i).getType()) {
                case FTString:
                    values.put(fields.get(i).getName(), feature.getFieldValueAsString(i));
                    break;
                case FTInteger:
                    Object intVal = feature.getFieldValue(i);
                    if (intVal instanceof Integer) {
                        values.put(fields.get(i).getName(), (int) intVal);
                    } else if (intVal instanceof Long) {
                        values.put(fields.get(i).getName(), (long) intVal);
                    } else {
                        Log.d(TAG, "skip value: " + intVal.toString());
                    }
                    break;
                case FTReal:
                    Object realVal = feature.getFieldValue(i);
                    if (realVal instanceof Double) {
                        values.put(fields.get(i).getName(), (double) realVal);
                    } else if (realVal instanceof Float) {
                        values.put(fields.get(i).getName(), (float) realVal);
                    } else {
                        Log.d(TAG, "skip value: " + realVal.toString());
                    }
                    break;
                case FTDate:
                case FTTime:
                case FTDateTime:
                    Object dateVal = feature.getFieldValue(i);
                    if(dateVal instanceof Date){
                        Date date = (Date) dateVal;
                        values.put(fields.get(i).getName(), date.getTime());
                    }
                    else if(dateVal instanceof Long){
                        values.put(fields.get(i).getName(), (long) dateVal);
                    }
                    else if(dateVal instanceof Calendar){
                        Calendar cal = (Calendar)dateVal;
                        values.put(fields.get(i).getName(), cal.getTimeInMillis());
                    }
                    else{
                        Log.d(TAG, "skip value: " + dateVal.toString());
                    }
                    break;
            }
        }

        db.insert(mPath.getName(), "", values);

        if (null == feature.getGeometry()) {
            return;
        }
        //update bbox
        updateExtent(feature.getGeometry().getEnvelope());
        //add to cache
        mVectorCacheItems.add(new VectorCacheItem(feature.getGeometry(), feature.getId()));

        if (mGeometryType == NOT_FOUND || mGeometryType != feature.getGeometry().getType()) {
            mGeometryType = feature.getGeometry().getType();
        }

        save();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void loadFeaturesFromGeoJSONStream(InputStream in, List<Field> fields, SQLiteDatabase db) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        reader.beginArray();
        while (reader.hasNext()) {
            //TODO: download attachments if needed
            Feature feature = readGeoJSONFeature(reader, fields);
            createFeature(feature, fields, db);
        }
        reader.endArray();
        reader.close();
    }

    protected Feature readGeoJSONFeature(JsonReader reader, List<Field> fields){
        return null;
    }

    protected Style getDefaultStyle()
            throws Exception
    {
        switch (mGeometryType) {

            case GTPoint:
            case GTMultiPoint:
                return new SimpleMarkerStyle(
                        Color.RED, Color.BLACK, 6, SimpleMarkerStyle.MarkerStyleCircle);

            case GTLineString:
            case GTMultiLineString:
                return new SimpleLineStyle(
                        Color.GREEN, Color.BLUE, SimpleLineStyle.LineStyleSolid);

            case GTPolygon:
            case GTMultiPolygon:
                return new SimplePolygonStyle(Color.MAGENTA);

            default:
                throw new Exception("Unknown geometry type: " + mGeometryType);
        }
    }


    protected void setDefaultRenderer()
    {
        if(null != mRenderer)
            return;
        try {
            mRenderer = new SimpleFeatureRenderer(this, getDefaultStyle());
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            mRenderer = null;
        }
    }


    protected void setRenderer(JSONObject jsonObject)
            throws JSONException
    {
        String renderName = "";
        if (jsonObject.has(JSON_NAME_KEY)) {
            renderName = jsonObject.getString(JSON_NAME_KEY);
        }
        switch (renderName) {
            case "RuleFeatureRenderer":
                mRenderer = new RuleFeatureRenderer(this);
                break;
            default:
            case "SimpleFeatureRenderer":
                mRenderer = new SimpleFeatureRenderer(this);
                break;
        }

        IJSONStore jsonStore = (IJSONStore) mRenderer;
        jsonStore.fromJSON(jsonObject);

        if (mRenderer instanceof RuleFeatureRenderer) {
            IStyleRule rule = getStyleRule();
            if (null != rule) {
                RuleFeatureRenderer renderer = (RuleFeatureRenderer) mRenderer;
                renderer.setStyleRule(rule);
            }
        }
    }


    protected IStyleRule getStyleRule()
    {
        return null;
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

        if (null != mFields) {
            JSONArray fields = new JSONArray();
            for (Field field : mFields.values()) {
                JSONObject fieldJsonObject = field.toJSON();
                fields.put(fieldJsonObject);
            }
            rootConfig.put(JSON_FIELDS_KEY, fields);
        }

        if (null != mRenderer && mRenderer instanceof IJSONStore) {
            IJSONStore jsonStore = (IJSONStore) mRenderer;
            rootConfig.put(Constants.JSON_RENDERERPROPS_KEY, jsonStore.toJSON());
        }

        if(mCacheLoaded){
            mExtents.unInit();
            for(VectorCacheItem item : mVectorCacheItems){
                updateExtent(item.getGeoGeometry().getEnvelope());
            }
        }

        if(mExtents.isInit()) {
            rootConfig.put(Constants.JSON_BBOX_MAXX_KEY, mExtents.getMaxX());
            rootConfig.put(Constants.JSON_BBOX_MINX_KEY, mExtents.getMinX());
            rootConfig.put(Constants.JSON_BBOX_MAXY_KEY, mExtents.getMaxY());
            rootConfig.put(Constants.JSON_BBOX_MINY_KEY, mExtents.getMinY());
        }

        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException, SQLiteException
    {
        super.fromJSON(jsonObject);
        mGeometryType = jsonObject.getInt(JSON_GEOMETRY_TYPE_KEY);
        mIsInitialized = jsonObject.getBoolean(JSON_IS_INITIALIZED_KEY);

        if (jsonObject.has(JSON_FIELDS_KEY)) {
            mFields = new HashMap<>();
            JSONArray fields = jsonObject.getJSONArray(JSON_FIELDS_KEY);
            for (int i = 0; i < fields.length(); i++) {
                Field field = new Field();
                field.fromJSON(fields.getJSONObject(i));
                mFields.put(field.getName(), field);
            }
        }

        if (jsonObject.has(Constants.JSON_BBOX_MAXX_KEY))
            mExtents.setMaxX(jsonObject.getDouble(Constants.JSON_BBOX_MAXX_KEY));
        if (jsonObject.has(Constants.JSON_BBOX_MAXY_KEY))
            mExtents.setMaxY(jsonObject.getDouble(Constants.JSON_BBOX_MAXY_KEY));
        if (jsonObject.has(Constants.JSON_BBOX_MINX_KEY))
            mExtents.setMinX(jsonObject.getDouble(Constants.JSON_BBOX_MINX_KEY));
        if (jsonObject.has(Constants.JSON_BBOX_MINY_KEY))
            mExtents.setMinY(jsonObject.getDouble(Constants.JSON_BBOX_MINY_KEY));

        if (mIsInitialized) {
            if(mIsVisible) {
                // load the layer contents async
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        reloadCache();
                    }
                });
                t.setPriority(Constants.DEFAULT_LOAD_LAYER_THREAD_PRIORITY);
                t.start();
            }
        }

        if (jsonObject.has(JSON_RENDERERPROPS_KEY)) {
            setRenderer(jsonObject.getJSONObject(JSON_RENDERERPROPS_KEY));
        } else {
            setDefaultRenderer();
        }
    }


    protected synchronized void reloadCache() throws SQLiteException
    {
        if(!mIsInitialized)
            return;

        //load vector cache
        mExtents.unInit();
        mCacheLoaded = false;
        mVectorCacheItems.clear();
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(false);
        String[] columns = new String[] {FIELD_ID, FIELD_GEOM};
        Cursor cursor = db.query(mPath.getName(), columns, null, null, null, null, null);
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        GeoGeometry geoGeometry = GeoGeometryFactory.fromBlob(cursor.getBlob(1));
                        if (null != geoGeometry) {
                            long nId = cursor.getLong(0);
                            mVectorCacheItems.add(new VectorCacheItem(geoGeometry, nId));
                            updateExtent(geoGeometry.getEnvelope());
                            updateUniqId(nId);
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        // e.printStackTrace();
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        mCacheLoaded = true;
        notifyLayerChanged();
    }


    @Override
    public boolean delete()
            throws SQLiteException
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
        return mIsInitialized && mExtents.isInit();
    }


    public Cursor query(
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder,
            String limit)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            Log.d(TAG, "The map should extends MapContentProviderHelper or inherited");
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }

        SQLiteDatabase db = map.getDatabase(true);

        try {
            return db.query(
                    mPath.getName(), projection, selection, selectionArgs, null, null, sortOrder, limit);
        } catch (SQLiteException e) {
            Log.d(TAG, e.getLocalizedMessage());
            return null;
        }
    }


    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder,
            String limit)
    {
        Cursor cursor;
        MatrixCursor matrixCursor;
        String featureId;
        String attachId;
        List<String> pathSegments;

        int uriType = mUriMatcher.match(uri);
        switch (uriType) {
            case TYPE_TABLE:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = FIELD_ID + " ASC";
                }
                cursor = query(projection, selection, selectionArgs, sortOrder, limit);
                if (null != cursor) {
                    cursor.setNotificationUri(getContext().getContentResolver(), mContentUri);
                }
                return cursor;
            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = FIELD_ID + " = " + featureId;
                } else {
                    selection = selection + " AND " + FIELD_ID + " = " + featureId;
                }

                cursor = query(projection, selection, selectionArgs, sortOrder, limit);
                if (null != cursor) {
                    cursor.setNotificationUri(getContext().getContentResolver(), mContentUri);
                }
                return cursor;

            case TYPE_ATTACH:
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 2);

                if (projection == null) {
                    projection = new String[] {
                            ATTACH_DISPLAY_NAME, ATTACH_SIZE, ATTACH_ID, ATTACH_MIME_TYPE};
                }

                matrixCursor = new MatrixCursor(projection);
                Map<String, AttachItem> attach = getAttachMap(featureId);

                if (null != attach) {
                    //the attach store in id folder in layer folder
                    File attachFolder = new File(mPath, featureId);
                    ArrayList<Object[]> rowArray = new ArrayList<>(attach.size());

                    for (AttachItem item : attach.values()) {
                        File attachFile = new File(attachFolder, item.getAttachId());
                        Object[] row = new Object[projection.length];

                        for (int i = 0; i < projection.length; i++) {

                            if (projection[i].compareToIgnoreCase(ATTACH_DISPLAY_NAME) == 0) {
                                row[i] = item.getDisplayName();
                            } else if (projection[i].compareToIgnoreCase(ATTACH_SIZE) == 0) {
                                row[i] = attachFile.length();
                            } else if (projection[i].compareToIgnoreCase(ATTACH_DATA) == 0) {
                                row[i] = attachFile.getPath();
                            } else if (projection[i].compareToIgnoreCase(ATTACH_MIME_TYPE) == 0) {
                                row[i] = item.getMimetype();
                            } else if (projection[i].compareToIgnoreCase(ATTACH_DATE_ADDED) == 0) {
                                row[i] = attachFile.lastModified();
                            } else if (projection[i].compareToIgnoreCase(ATTACH_ID) == 0) {
                                row[i] = item.getAttachId();
                            } else if (projection[i].compareToIgnoreCase(ATTACH_DESCRIPTION) == 0) {
                                row[i] = item.getDescription();
                            }
                        }

                        rowArray.add(row);
                    }

                    // sorting rowArray
                    if (!TextUtils.isEmpty(sortOrder)) {
                        int sortIndex = -1;

                        for (int i = 0; i < projection.length; i++) {
                            if (projection[i].compareToIgnoreCase(sortOrder) == 0) {
                                sortIndex = i;
                                break;
                            }
                        }

                        if (-1 < sortIndex) {
                            int columnType = COLUMNTYPE_UNKNOWN;

                            if (projection[sortIndex].compareToIgnoreCase(
                                    ATTACH_DISPLAY_NAME) == 0 ||
                                projection[sortIndex].compareToIgnoreCase(
                                        ATTACH_DATA) == 0 ||
                                projection[sortIndex].compareToIgnoreCase(
                                        ATTACH_MIME_TYPE) == 0 ||
                                projection[sortIndex].compareToIgnoreCase(
                                        ATTACH_ID) == 0 ||
                                projection[sortIndex].compareToIgnoreCase(
                                        ATTACH_DESCRIPTION) == 0) {

                                columnType = COLUMNTYPE_STRING;

                            } else if (projection[sortIndex].compareToIgnoreCase(
                                    ATTACH_SIZE) == 0 || projection[sortIndex].compareToIgnoreCase(
                                    ATTACH_DATE_ADDED) == 0) {

                                columnType = COLUMNTYPE_LONG;
                            }

                            final int columnTypeF = columnType;
                            final int sortIndexF = sortIndex;

                            Collections.sort(
                                    rowArray, new Comparator<Object[]>()
                                    {
                                        @Override
                                        public int compare(
                                                Object[] lhs,
                                                Object[] rhs)
                                        {
                                            switch (columnTypeF) {
                                                case COLUMNTYPE_STRING:
                                                    return ((String) lhs[sortIndexF]).compareTo(
                                                            (String) rhs[sortIndexF]);

                                                case COLUMNTYPE_LONG:
                                                    return ((Long) lhs[sortIndexF]).compareTo(
                                                            (Long) rhs[sortIndexF]);

                                                case COLUMNTYPE_UNKNOWN:
                                                default:
                                                    return 0;
                                            }
                                        }
                                    });
                        }
                    }

                    for (Object[] row : rowArray) {
                        matrixCursor.addRow(row);
                    }

                    rowArray.clear();
                }

                return matrixCursor;

            case TYPE_ATTACH_ID:
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 3);
                attachId = uri.getLastPathSegment();
                if (projection == null) {
                    projection = new String[] {
                            ATTACH_DISPLAY_NAME, ATTACH_SIZE, ATTACH_ID, ATTACH_MIME_TYPE};
                }
                matrixCursor = new MatrixCursor(projection);
                //get attach path
                AttachItem item = getAttach(featureId, attachId);
                if (null != item) {
                    File attachFile = new File(
                            mPath, featureId + File.separator +
                                   item.getAttachId()); //the attaches store in id folder in layer folder
                    Object[] row = new Object[projection.length];
                    for (int i = 0; i < projection.length; i++) {
                        if (projection[i].compareToIgnoreCase(ATTACH_DISPLAY_NAME) == 0) {
                            row[i] = item.getDisplayName();
                        } else if (projection[i].compareToIgnoreCase(ATTACH_SIZE) == 0) {
                            row[i] = attachFile.length();
                        } else if (projection[i].compareToIgnoreCase(ATTACH_DATA) == 0) {
                            row[i] = attachFile.getPath();
                        } else if (projection[i].compareToIgnoreCase(ATTACH_MIME_TYPE) == 0) {
                            row[i] = item.getMimetype();
                        } else if (projection[i].compareToIgnoreCase(ATTACH_DATE_ADDED) == 0) {
                            row[i] = attachFile.lastModified();
                        } else if (projection[i].compareToIgnoreCase(ATTACH_ID) == 0) {
                            row[i] = item.getAttachId();
                        } else if (projection[i].compareToIgnoreCase(ATTACH_DESCRIPTION) == 0) {
                            row[i] = item.getDescription();
                        }
                    }
                    matrixCursor.addRow(row);
                }
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
            case TYPE_ATTACH:
                return CONTENT_ATTACH_TYPE;
            case TYPE_ATTACH_ID:
                List<String> pathSegments = uri.getPathSegments();
                String featureId = pathSegments.get(pathSegments.size() - 3);
                String attachId = uri.getLastPathSegment();
                AttachItem item = getAttach(featureId, attachId);
                if (null != item) {
                    return item.getMimetype();
                }
        }
        return null;
    }


    public String[] getStreamTypes(
            Uri uri,
            String mimeTypeFilter)
    {
        int uriType = mUriMatcher.match(uri);
        switch (uriType) {
            case TYPE_ATTACH_ID:
                List<String> pathSegments = uri.getPathSegments();
                String featureId = pathSegments.get(pathSegments.size() - 3);
                String attachId = uri.getLastPathSegment();
                AttachItem item = getAttach(featureId, attachId);
                if (null != item) {
                    return new String[] {item.getMimetype()};
                }
        }
        return null;
    }


    public long insertAddChanges(ContentValues contentValues)
    {
        long rowID = insert(contentValues);
        if (rowID != NOT_FOUND) {
            addChange(rowID, CHANGE_OPERATION_NEW);
        }
        return rowID;
    }


    protected void updateUniqId(long id)
    {
        if (mUniqId <= id) {
            mUniqId = id + 1;
        }
    }


    protected long insert(ContentValues contentValues)
    {
        if (!contentValues.containsKey(FIELD_GEOM)) {
            return NOT_FOUND;
        }

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
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


    protected void updateExtent(GeoEnvelope env)
    {
        //update extent
        if (mExtents.isInit()) {
            mExtents.merge(env);
        } else {
            mExtents = env;
        }
    }

    protected long insertAttach(String featureId, ContentValues contentValues){
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

    public Uri insert(
            Uri uri,
            ContentValues contentValues)
    {
        int uriType = mUriMatcher.match(uri);
        switch (uriType) {
            case TYPE_TABLE:
                long rowID = insert(contentValues);
                if (rowID != NOT_FOUND) {
                    Uri resultUri = ContentUris.withAppendedId(mContentUri, rowID);
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(resultUri, null, false);
                    } else {
                        addChange(rowID, CHANGE_OPERATION_NEW);
                        getContext().getContentResolver().notifyChange(resultUri, null, true);
                    }
                    return resultUri;
                }
                return null;
            case TYPE_ATTACH:
                List<String> pathSegments = uri.getPathSegments();
                String featureId = pathSegments.get(pathSegments.size() - 2);
                long attachID = insertAttach(featureId, contentValues);
                if(attachID != NOT_FOUND){
                    Uri resultUri = ContentUris.withAppendedId(uri, attachID);
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver()
                                .notifyChange(resultUri, null, false);
                    } else {
                        long featureIdL = Long.parseLong(featureId);
                        addChange(featureIdL, attachID, CHANGE_OPERATION_NEW);
                        getContext().getContentResolver()
                                .notifyChange(resultUri, null, true);
                    }
                    return resultUri;
                }
                return null;
            case TYPE_FEATURE:
            case TYPE_ATTACH_ID:
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
    }


    public int deleteAddChanges(long id)
    {
        int result = delete(id, FIELD_ID + " = " + id, null);
        if (result > 0) {
            addChange(id, CHANGE_OPERATION_DELETE);
        }
        return result;
    }


    protected int delete(
            long rowID,
            String selection,
            String[] selectionArgs)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }
        SQLiteDatabase db = map.getDatabase(false);
        int result = db.delete(mPath.getName(), selection, selectionArgs);
        if (result > 0) {
            Intent notify;
            if (rowID == NOT_FOUND) {
                notify = new Intent(NOTIFY_DELETE_ALL);
            } else {
                notify = new Intent(NOTIFY_DELETE);
                notify.putExtra(FIELD_ID, rowID);
            }
            notify.putExtra(NOTIFY_LAYER_NAME, mPath.getName()); // if we need mAuthority?
            getContext().sendBroadcast(notify);
        }
        return result;
    }


    public int delete(
            Uri uri,
            String selection,
            String[] selectionArgs)
    {
        String featureId;
        long featureIdL;
        String attachId;
        List<String> pathSegments;
        int result;

        int uriType = mUriMatcher.match(uri);
        switch (uriType) {
            case TYPE_TABLE:
                result = delete(NOT_FOUND, selection, selectionArgs);
                if (result > 0) {
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    } else {
                        addChange(NOT_FOUND, CHANGE_OPERATION_DELETE);
                        getContext().getContentResolver().notifyChange(uri, null, true);
                    }
                }
                return result;
            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();
                featureIdL = Long.parseLong(featureId);
                if (TextUtils.isEmpty(selection)) {
                    selection = FIELD_ID + " = " + featureId;
                } else {
                    selection = selection + " AND " + FIELD_ID + " = " + featureId;
                }
                result = delete(featureIdL, selection, selectionArgs);

                if (result > 0) {
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    } else {
                        addChange(featureIdL, CHANGE_OPERATION_DELETE);
                        getContext().getContentResolver().notifyChange(uri, null, true);
                    }
                }
                return result;
            case TYPE_ATTACH:
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 2);
                result = 0;
                //get attach path
                File attachFolder =
                        new File(mPath, featureId); //the attach store in id folder in layer folder
                for (File attachFile : attachFolder.listFiles()) {
                    if (attachFile.delete()) {
                        result++;
                    }
                }
                if (result > 0) {

                    deleteAttaches(featureId);

                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    } else {
                        featureIdL = Long.parseLong(featureId);
                        addChange(featureIdL, NOT_FOUND, CHANGE_OPERATION_DELETE);
                        getContext().getContentResolver().notifyChange(uri, null);
                    }
                }
                return result;
            case TYPE_ATTACH_ID:
                pathSegments = uri.getPathSegments();
                featureId = pathSegments.get(pathSegments.size() - 3);
                featureIdL = Long.parseLong(featureId);
                attachId = uri.getLastPathSegment();
                long attachIdL = Long.parseLong(attachId);

                //get attach path
                File attachFile = new File(mPath, featureId + File.separator + attachId);
                if (attachFile.delete()) {

                    deleteAttach(featureId, attachId);

                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    } else {
                        addChange(featureIdL, attachIdL, CHANGE_OPERATION_DELETE);
                        getContext().getContentResolver().notifyChange(uri, null);
                    }
                    return 1;
                }
                return 0;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
    }


    public int updateAddChanges(
            ContentValues values,
            long id)
    {
        int result = update(id, values, FIELD_ID + " = " + id, null);
        if (result > 0) {
            addChange(id, CHANGE_OPERATION_CHANGED);
        }
        return result;
    }


    protected int update(
            long rowID,
            ContentValues values,
            String selection,
            String[] selectionArgs)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }
        SQLiteDatabase db = map.getDatabase(false);
        int result = values != null && values.size() > 0 ? db.update(
                mPath.getName(), values, selection, selectionArgs) : 0;
        if (result > 0) {
            Intent notify;
            if (rowID == NOT_FOUND) {
                if (values.containsKey(FIELD_GEOM)) {
                    notify = new Intent(NOTIFY_UPDATE_ALL);
                    notify.putExtra(NOTIFY_LAYER_NAME, mPath.getName()); // if we need mAuthority?
                    getContext().sendBroadcast(notify);
                }
            } else {
                if (values.containsKey(FIELD_GEOM) || values.containsKey(FIELD_ID)) {
                    notify = new Intent(NOTIFY_UPDATE);
                    boolean bNotify = false;
                    if (values.containsKey(FIELD_GEOM)) {
                        notify.putExtra(FIELD_ID, rowID);
                        bNotify = true;
                    }

                    if (values.containsKey(FIELD_ID)) {
                        updateUniqId(values.getAsLong(FIELD_ID));

                        notify.putExtra(FIELD_OLD_ID, rowID);
                        notify.putExtra(FIELD_ID, values.getAsLong(FIELD_ID));
                        bNotify = true;
                    }

                    if (bNotify) {
                        notify.putExtra(
                                NOTIFY_LAYER_NAME, mPath.getName()); // if we need mAuthority?
                        getContext().sendBroadcast(notify);
                    }

                } else {
                    notify = new Intent(NOTIFY_UPDATE_FIELDS);
                    notify.putExtra(FIELD_ID, rowID);
                    getContext().sendBroadcast(notify);
                }
            }
        }
        return result;
    }


    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs)
    {
        String featureId;
        long featureIdL;
        String attachId;
        long attachIdL;
        List<String> pathSegments;

        int result;

        int uriType = mUriMatcher.match(uri);
        switch (uriType) {
            case TYPE_TABLE:
                result = update(NOT_FOUND, values, selection, selectionArgs);

                if (result > 0) {
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    } else {
                        addChange(NOT_FOUND, CHANGE_OPERATION_CHANGED);
                        getContext().getContentResolver().notifyChange(uri, null);
                    }
                }

                return result;
            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();
                featureIdL = Long.parseLong(featureId);
                if (TextUtils.isEmpty(selection)) {
                    selection = FIELD_ID + " = " + featureId;
                } else {
                    selection = selection + " AND " + FIELD_ID + " = " + featureId;
                }

                result = update(featureIdL, values, selection, selectionArgs);

                if (result > 0) {
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    } else {
                        addChange(featureIdL, CHANGE_OPERATION_CHANGED);
                        getContext().getContentResolver().notifyChange(uri, null);
                    }
                }
                return result;
            case TYPE_ATTACH:
                if (values.containsKey(ATTACH_DESCRIPTION)) {
                    //set the same description to all items
                    pathSegments = uri.getPathSegments();
                    featureId = pathSegments.get(pathSegments.size() - 2);
                    featureIdL = Long.parseLong(featureId);
                    Map<String, AttachItem> attaches = getAttachMap(featureId);
                    int changed = 0;
                    if (null != attaches) {
                        for (AttachItem item : attaches.values()) {
                            item.setDescription(values.getAsString(ATTACH_DESCRIPTION));
                            attachIdL = Long.parseLong(item.getAttachId());
                            addChange(featureIdL, attachIdL, CHANGE_OPERATION_CHANGED);
                            changed++;
                        }
                        return changed;
                    }
                    return 0;
                }
            case TYPE_ATTACH_ID:
                if (values.containsKey(ATTACH_ID) || values.containsKey(ATTACH_DESCRIPTION) ||
                    values.containsKey(ATTACH_DISPLAY_NAME) ||
                    values.containsKey(ATTACH_MIME_TYPE)) {
                    pathSegments = uri.getPathSegments();
                    featureId = pathSegments.get(pathSegments.size() - 3);
                    featureIdL = Long.parseLong(featureId);
                    attachId = uri.getLastPathSegment();
                    attachIdL = Long.parseLong(attachId);

                    AttachItem item = getAttach(featureId, attachId);
                    if (null != item) {
                        if (values.containsKey(ATTACH_DESCRIPTION)) {
                            item.setDescription(values.getAsString(ATTACH_DESCRIPTION));
                        }
                        if (values.containsKey(ATTACH_DISPLAY_NAME)) {
                            item.setDisplayName(values.getAsString(ATTACH_DISPLAY_NAME));
                        }
                        if (values.containsKey(ATTACH_MIME_TYPE)) {
                            item.setMimetype(values.getAsString(ATTACH_MIME_TYPE));
                        }
                        if (values.containsKey(ATTACH_ID)) {
                            setNewAttachId(featureId, item, values.getAsString(ATTACH_ID));
                        }
                        saveAttach(featureId);
                        String fragment = uri.getFragment();
                        boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                        if (bFromNetwork) {
                            getContext().getContentResolver().notifyChange(uri, null, false);
                        } else {
                            addChange(featureIdL, attachIdL, CHANGE_OPERATION_CHANGED);
                            getContext().getContentResolver().notifyChange(uri, null);
                        }
                        return 1;
                    }
                    return 0;
                }
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
    }


    public ParcelFileDescriptor openFile(
            Uri uri,
            String mode)
            throws FileNotFoundException
    {
        int uriType = mUriMatcher.match(uri);
        switch (uriType) {
            case TYPE_ATTACH_ID:
                List<String> pathSegments = uri.getPathSegments();
                String featureId = pathSegments.get(pathSegments.size() - 3);
                String attachId = uri.getLastPathSegment();
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

                return ParcelFileDescriptor.open(
                        new File(mPath, featureId + File.separator + attachId), nMode);
            default:
                throw new FileNotFoundException();
        }
    }

    /*
    To put image to the feature
    Uri newUri = ... content://com.nextgis.mobile.provider/layer_xxxxxxxxx/1/attach
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
    Uri thisAttachUri =
          ContentUris.withAppendedId(featureUri, attachId);
    InputStream inStream = null;
    try {
       inStream = resolver.openInputStream(thisAttachUri);

       // what to do with the stream is up to you
       // I simply create a bitmap to display it
       Bitmap bm = BitmapFactory.decodeStream(inStream);
       FrameLayout frame =
             (FrameLayout)findViewById(R.id.picture_frame);
       ImageView view = new ImageView(getApplicationContext());
       view.setImageBitmap(bm);
       frame.addView(view);
    } catch (FileNotFoundException e) {
       Log.e(TAG, "file not found " + thisAttachUri, e);
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
    Uri newUri = ... content://com.nextgis.mobile.provider/layer_xxxxxxxxx/1/attach
    Cursor cursor = resolver.query(newUri, {MediaStore.MediaColumns.DATA}, null....)
    File bitmapFile = new File(cursor.getString(ATTACH_DATA))
    and open using real path
    */


    protected void addChange(
            long featureId,
            int operation)
    {
        //nothing to do
    }


    protected void addChange(
            long featureId,
            long attachId,
            int attachOperation)
    {
        //nothing to do
    }


    public int getGeometryType()
    {
        return mGeometryType;
    }


    public List<Field> getFields()
    {
        return new ArrayList<>(mFields.values());
    }


    public int getCount()
    {
        if(!mCacheLoaded)
            reloadCache();

        return mVectorCacheItems.size();
    }


    public List<VectorCacheItem> query(GeoEnvelope envelope)
    {
        if(!mCacheLoaded)
            reloadCache();
        List<VectorCacheItem> ret = new ArrayList<>();
        for (VectorCacheItem cacheItem : mVectorCacheItems) {
            GeoGeometry geom = cacheItem.getGeoGeometry();
            if (null == geom) {
                continue;
            }
            if (geom.intersects(envelope)) {
                ret.add(cacheItem);
            }
        }
        return ret;
    }


    protected Map<String, AttachItem> getAttachMap(String featureId)
    {
        Map<String, AttachItem> attachMap = mAttaches.get(featureId);
        if (null == attachMap) {
            loadAttach(featureId);
            return mAttaches.get(featureId);
        } else {
            return attachMap;
        }
    }


    protected AttachItem getAttach(
            String featureId,
            String attachId)
    {
        Map<String, AttachItem> attachMap = getAttachMap(featureId);
        if (null == attachMap) {
            return null;
        }

        return attachMap.get(attachId);
    }


    protected void loadAttach(String featureId)
    {
        File attachFolder = new File(mPath, featureId);
        File meta = new File(attachFolder, META);
        try {
            String metaContent = FileUtil.readFromFile(meta);
            JSONArray jsonArray = new JSONArray(metaContent);
            Map<String, AttachItem> attach = new HashMap<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonValue = jsonArray.getJSONObject(i);
                AttachItem attachItem = new AttachItem();
                attachItem.fromJSON(jsonValue);
                attach.put(attachItem.getAttachId(), attachItem);
            }
            mAttaches.put(featureId, attach);
        } catch (IOException | JSONException e) {
            // e.printStackTrace();
        }

    }


    protected void saveAttach(String featureId)
    {
        Map<String, AttachItem> attachMap = mAttaches.get(featureId);
        if (null != attachMap) {
            JSONArray jsonArray = new JSONArray();

            for (AttachItem item : attachMap.values()) {
                try {
                    jsonArray.put(item.toJSON());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            String payload = jsonArray.toString();
            File attachFolder = new File(mPath, featureId);
            File meta = new File(attachFolder, META);
            try {
                FileUtil.writeToFile(meta, payload);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    protected void deleteAttaches(String featureId)
    {
        mAttaches.remove(featureId);
        saveAttach(featureId);
    }


    protected void deleteAttach(
            String featureId,
            String attachId)
    {
        Map<String, AttachItem> attachMap = getAttachMap(featureId);
        if (null != attachMap) {
            attachMap.remove(attachId);
            saveAttach(featureId);
        }
    }


    public VectorCacheItem getCacheItem(long id)
    {
        if(!mCacheLoaded)
            reloadCache();

        for (VectorCacheItem cacheItem : mVectorCacheItems) {
            if (cacheItem.getId() == id) {
                return cacheItem;
            }
        }
        return null;
    }


    public void deleteCacheItem(long id)
    {
        if(!mCacheLoaded)
            reloadCache();

        Iterator<VectorCacheItem> cacheItemIterator = mVectorCacheItems.iterator();
        while (cacheItemIterator.hasNext()) {
            VectorCacheItem cacheItem = cacheItemIterator.next();

            if (cacheItem.getId() == id) {
                mTempCacheItem = cacheItem;
                cacheItemIterator.remove();

                notifyLayerChanged();

                break;
            }
        }
    }


    public void restoreCacheItem()
    {
        if (mTempCacheItem != null) {
            mVectorCacheItems.add(mTempCacheItem);
            mTempCacheItem = null;
            notifyLayerChanged();
        }
    }


    protected void addAttach(
            String featureId,
            AttachItem item)
    {
        Map<String, AttachItem> attachMap = getAttachMap(featureId);
        if (null == attachMap) {
            attachMap = new HashMap<>();
            mAttaches.put(featureId, attachMap);
        }

        attachMap.put(item.getAttachId(), item);
        saveAttach(featureId);
    }


    protected void setNewAttachId(
            String featureId,
            AttachItem item,
            String newAttachId)
    {
        File attachFile = new File(mPath, featureId + File.separator + item.getAttachId());
        attachFile.renameTo(new File(attachFile.getParentFile(), newAttachId));

        //save changes to meta.json
        Map<String, AttachItem> attaches = getAttachMap(featureId);
        attaches.remove(item.getAttachId());
        item.setAttachId(newAttachId);
        attaches.put(item.getAttachId(), item);
        saveAttach(featureId);
    }


    public void notifyDelete(long rowID)
    {
        //remove cached item
        for (VectorCacheItem item : mVectorCacheItems) {
            if (item.getId() == rowID) {
                mVectorCacheItems.remove(item);
                break;
            }
        }
        notifyLayerChanged();
    }


    public void notifyDeleteAll()
    {
        //clear cache
        mVectorCacheItems.clear();
        notifyLayerChanged();
    }


    public void notifyInsert(long rowID)
    {
        GeoGeometry geom = getGeometryForId(rowID);
        if (null != geom) {
            mVectorCacheItems.add(new VectorCacheItem(geom, (int) rowID));

            updateExtent(geom.getEnvelope());

            notifyLayerChanged();
        }
    }


    public void notifyUpdate(
            long rowID,
            long rowOldID)
    {
        GeoGeometry geom = getGeometryForId(rowID);
        if (null != geom) {
            for (VectorCacheItem item : mVectorCacheItems) {
                if (rowOldID != NOT_FOUND) {
                    if (item.getId() == rowOldID) {
                        item.setId(rowID);
                        item.setGeoGeometry(geom);
                        break;
                    }
                } else {
                    if (item.getId() == rowID) {
                        item.setGeoGeometry(geom);
                        break;
                    }
                }
            }

            updateExtent(geom.getEnvelope());

            notifyLayerChanged();
        }
    }


    public void notifyUpdateAll()
    {
        reloadCache();
        notifyLayerChanged();
    }


    protected GeoGeometry getGeometryForId(long rowID)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(false);
        String[] columns = new String[] {FIELD_GEOM};
        String selection = FIELD_ID + " = " + rowID;
        Cursor cursor = db.query(mPath.getName(), columns, selection, null, null, null, null);
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                try {
                    return GeoGeometryFactory.fromBlob(cursor.getBlob(0));
                } catch (IOException | ClassNotFoundException e) {
                    // e.printStackTrace();
                }
            }
            cursor.close();
        }

        return null;
    }


    public long getUniqId()
    {
        if (Constants.NOT_FOUND == mUniqId) {
            String columns[] = {FIELD_ID};
            String sortOrder = FIELD_ID + " DESC";
            Cursor cursor = query(columns, null, null, sortOrder, "1");

            if (cursor.moveToFirst()) {
                mUniqId = cursor.getLong(0) + 1;
            }
        }

        return mUniqId;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if(mIsVisible) {
            // load the layer contents async
            Thread t = new Thread(new Runnable() {
                public void run() {
                    reloadCache();
                }
            });
            t.setPriority(Constants.DEFAULT_LOAD_LAYER_THREAD_PRIORITY);
            t.start();
        }
    }
}
