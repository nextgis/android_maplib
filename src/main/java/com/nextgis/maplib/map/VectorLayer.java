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
import android.util.Log;

import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.IGeometryCache;
import com.nextgis.maplib.api.IGeometryCacheItem;
import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.api.IStyleRule;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.GeometryRTree;
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
import com.nextgis.maplib.util.GeoJSONUtil;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplib.util.NGWUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.CHANGE_OPERATION_CHANGED;
import static com.nextgis.maplib.util.Constants.CHANGE_OPERATION_DELETE;
import static com.nextgis.maplib.util.Constants.CHANGE_OPERATION_NEW;
import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.FIELD_OLD_ID;
import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOCAL_VECTOR;
import static com.nextgis.maplib.util.Constants.MAX_CONTENT_LENGTH;
import static com.nextgis.maplib.util.Constants.MIN_LOCAL_FEATURE_ID;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.FTDate;
import static com.nextgis.maplib.util.GeoConstants.FTDateTime;
import static com.nextgis.maplib.util.GeoConstants.FTInteger;
import static com.nextgis.maplib.util.GeoConstants.FTReal;
import static com.nextgis.maplib.util.GeoConstants.FTString;
import static com.nextgis.maplib.util.GeoConstants.FTTime;
import static com.nextgis.maplib.util.GeoConstants.GTLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPoint;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPolygon;
import static com.nextgis.maplib.util.GeoConstants.GTPolygon;
import static com.nextgis.maplib.util.GeoConstants.GTNone;
import static com.nextgis.maplib.util.GeoConstants.GTPoint;

/**
 * The vector layer class. It stores geometry and attributes.
 *
 * Here some examples of code to work with this layer.
 *
 * <b>To put image to the feature</b>
 * <code>
 *     Uri newUri = ... content://com.nextgis.mobile.provider/layer_xxxxxxxxx/1/attach
 *     Uri uri = getContentResolver().insert(newUri, null);
 *     try {
 *          OutputStream outStream = getContentResolver().openOutputStream(uri);
 *          sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outStream);
 *          outStream.close();
 *     } catch (Exception e) {
 *          Log.e(TAG, "exception while writing image", e);
 *     }
 * </code>
 *
 * <b>To get bitmap from uri</b>
 * <code>
 *     private Bitmap getBitmapFromUri(Uri uri) throws IOException {
 *          ParcelFileDescriptor parcelFileDescriptor =
 *          getContentResolver().openFileDescriptor(uri, "r");
 *          FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
 *          Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
 *          parcelFileDescriptor.close();
 *          return image;
 *     }
 * </code>
 *
 * <b>To get image using uri</b>
 * <code>
 *     Uri featureUri = content://com.nextgis.mobile.provider/layer_xxxxxxxxx/1
 *     Uri thisAttachUri =
 *     ContentUris.withAppendedId(featureUri, attachId);
 *     InputStream inStream = null;
 *     try {
 *          inStream = resolver.openInputStream(thisAttachUri);
 *          // what to do with the stream is up to you
 *          // I simply create a bitmap to display it
 *          Bitmap bm = BitmapFactory.decodeStream(inStream);
 *          FrameLayout frame =
 *          (FrameLayout)findViewById(R.id.picture_frame);
 *          ImageView view = new ImageView(getApplicationContext());
 *          view.setImageBitmap(bm);
 *          frame.addView(view);
 *      } catch (FileNotFoundException e) {
 *          Log.e(TAG, "file not found " + thisAttachUri, e);
 *      }
 *      finally {
 *          if (inStream != null) {
 *          try {
 *              inStream.close();
 *          } catch (IOException e) {
 *              Log.e(TAG, "could not close stream", e);
 *          }
 *      }
 * </code>
 *
 * <b>Also it can be used</b>
 * <code>
 *     Uri newUri = ... content://com.nextgis.mobile.provider/layer_xxxxxxxxx/1/attach
 *     Cursor cursor = resolver.query(newUri, {MediaStore.MediaColumns.DATA}, null....)
 *     File bitmapFile = new File(cursor.getString(ATTACH_DATA))
 *          and open using real path
 * </code>
 *
 * @author Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 */

public class VectorLayer
        extends Layer {
    protected boolean mCacheLoaded;
    protected int mGeometryType;
    protected Uri mContentUri;
    protected UriMatcher mUriMatcher;
    protected String mAuthority;
    protected Map<String, Field> mFields;
    protected long mUniqId;

    protected static String CONTENT_TYPE;
    protected static String CONTENT_ITEM_TYPE;

    protected static final String JSON_GEOMETRY_TYPE_KEY = "geometry_type";
    protected static final String JSON_FIELDS_KEY = "fields";

    public static final String NOTIFY_DELETE = "com.nextgis.maplib.notify_delete";
    public static final String NOTIFY_DELETE_ALL = "com.nextgis.maplib.notify_delete_all";
    public static final String NOTIFY_INSERT = "com.nextgis.maplib.notify_insert";
    public static final String NOTIFY_UPDATE = "com.nextgis.maplib.notify_update";
    public static final String NOTIFY_UPDATE_ALL = "com.nextgis.maplib.notify_update_all";
    public static final String NOTIFY_UPDATE_FIELDS = "com.nextgis.maplib.notify_update_fields";
    public static final String NOTIFY_FEATURE_ID_CHANGE = "com.nextgis.maplib.notify_change_id";
    public static final String NOTIFY_LAYER_NAME = "layer_name";


    protected static final String CONTENT_ATTACH_TYPE = "vnd.android.cursor.dir/*";
    protected static final String NO_SYNC = "no_sync";

    protected static final int TYPE_TABLE = 1;
    protected static final int TYPE_FEATURE = 2;
    protected static final int TYPE_ATTACH = 3;
    protected static final int TYPE_ATTACH_ID = 4;

    protected static final String META = "meta.json";
    protected static final String CACHE = "cache";
    protected static final String SAMPLE_EXT = ".samp";
    protected static final String RTREE = "rtree";

    public static final String ATTACH_DISPLAY_NAME = MediaStore.MediaColumns.DISPLAY_NAME;
    public static final String ATTACH_SIZE = MediaStore.MediaColumns.SIZE;
    public static final String ATTACH_ID = MediaStore.MediaColumns._ID;
    public static final String ATTACH_MIME_TYPE = MediaStore.MediaColumns.MIME_TYPE;
    public static final String ATTACH_DATA = MediaStore.MediaColumns.DATA;
    public static final String ATTACH_DATE_ADDED = MediaStore.MediaColumns.DATE_ADDED;
    public static final String ATTACH_DESCRIPTION = MediaStore.Images.ImageColumns.DESCRIPTION;

    public static final int COLUMN_TYPE_UNKNOWN = 0;
    public static final int COLUMN_TYPE_STRING = 1;
    public static final int COLUMN_TYPE_LONG = 2;

    /**
     * The geometry cache for fast querying and drawing
     */

    protected IGeometryCache mCache;
    protected Map<String, Map<String, AttachItem>> mAttaches;

    public VectorLayer(
            Context context,
            File path) {
        super(context, path);

        if (!(context instanceof IGISApplication)) {
            throw new IllegalArgumentException(
                    "The context should be the instance of IGISApplication");
        }

        mCacheLoaded = false;
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

        mAttaches = new HashMap<>();
        mCache = new GeometryRTree();

        mLayerType = LAYERTYPE_LOCAL_VECTOR;

        mUniqId = Constants.NOT_FOUND;
    }

    public void create(int geometryType, final List<Field> fields) throws SQLiteException {
        mGeometryType = geometryType;
        Log.d(TAG, "init layer " + getName());

        if(null == mFields)
            mFields = new HashMap<>();
        else
            mFields.clear();

        //filter out forbidden fields
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            String fieldName = field.getName();
            if (NGWUtil.containsCaseInsensitive(fieldName, Constants.VECTOR_FORBIDDEN_FIELDS) ||
                    fieldName.startsWith("@")) {
                fields.remove(i);
                i--;

                String warning = getContext().getString(R.string.warning_remove_field);
                reportError(String.format(warning, fieldName));
                continue;
            } else if (fieldName.contains(":")) {
                field.setName(fieldName.replace(":", "_"));
            }

            mFields.put(field.getName(), field);
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
        setDefaultRenderer();
    }

    public void createFromNGFP(Uri uri, IProgressor progressor) {
        /*

         */
    }

    public void createFromGeoJson(Uri uri, IProgressor progressor) throws IOException, JSONException, NGException {
        InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new NGException(mContext.getString(R.string.error_download_data));
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            int nSize = inputStream.available();

            if (null != progressor) {
                progressor.setMessage(mContext.getString(R.string.message_loading));
                progressor.setMax(nSize);
            }

            int nIncrement = 0;
            BufferedReader streamReader = new BufferedReader(
                    new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                nIncrement += inputStr.length();
                if (null != progressor) {
                    if (progressor.isCanceled())
                        break;
                    progressor.setValue(nIncrement);
                }
                responseStrBuilder.append(inputStr);
                if (responseStrBuilder.length() > MAX_CONTENT_LENGTH)
                    throw new NGException(mContext.getString(R.string.error_layer_create));
            }

            JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
            GeoJSONUtil.createLayerFromGeoJSON(this, jsonObject, progressor);
        } else {
            if (null != progressor) {
                progressor.setMessage(mContext.getString(R.string.create_features));
            }
            GeoJSONUtil.createLayerFromGeoJSONStream(this, inputStream, progressor);
        }
    }

    public void createFromGeoJson(File path, IProgressor progressor) throws IOException, JSONException, NGException {
        if (null != progressor) {
            progressor.setMessage(mContext.getString(R.string.create_features));
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            String jsonData = FileUtil.readFromFile(path);
            JSONObject jsonObject = new JSONObject(jsonData);
            GeoJSONUtil.createLayerFromGeoJSON(this, jsonObject, progressor);
        } else {
            FileInputStream inputStream = new FileInputStream(path);
            GeoJSONUtil.createLayerFromGeoJSONStream(this, inputStream, progressor);
        }
    }

    public void createFeature(Feature feature)
            throws SQLiteException {
        // check if geometry type is appropriate layer geometry type
        if (null == feature.getGeometry() || feature.getGeometry().getType() != mGeometryType) {
            return;
        }

        // check if such id already used
        // maybe was added previous session

        if (!mCacheLoaded)
            reloadCache();

        if (mCache.getItem(feature.getId()) != null) {
            return;
        }

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(false);

        final ContentValues values = new ContentValues();
        values.put(FIELD_ID, feature.getId());
        try {
            values.put(FIELD_GEOM, feature.getGeometry().toBlob());
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Field> fields = feature.getFields();
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
                    if (dateVal instanceof Date) {
                        Date date = (Date) dateVal;
                        values.put(fields.get(i).getName(), date.getTime());
                    } else if (dateVal instanceof Long) {
                        values.put(fields.get(i).getName(), (long) dateVal);
                    } else if (dateVal instanceof Calendar) {
                        Calendar cal = (Calendar) dateVal;
                        values.put(fields.get(i).getName(), cal.getTimeInMillis());
                    } else {
                        Log.d(TAG, "skip value: " + dateVal.toString());
                    }
                    break;
            }
        }

        Log.d(TAG, "Inserting " + values);
        if (db.insert(mPath.getName(), "", values) > 0) {

            if (null == feature.getGeometry()) {
                return;
            }

            addGeometryToCache(feature.getId(), feature.getGeometry());

            //update bbox
            mExtents.merge(feature.getGeometry().getEnvelope());

            save();
        }
    }

    protected void saveGeometryToFile(GeoGeometry geometry, File file) {
        try {
            FileUtil.createDir(file.getParentFile());
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
            geometry.write(dataOutputStream);
            dataOutputStream.flush();
            dataOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected GeoGeometry loadGeometryFromFile(File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            DataInputStream dataInputStream = new DataInputStream(fileInputStream);
            GeoGeometry geometry = GeoGeometryFactory.fromDataStream(dataInputStream);

            dataInputStream.close();
            fileInputStream.close();

            return geometry;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected void addGeometryToCache(long featureId, GeoGeometry geometry) {
        // TODO: 04.09.15 filter out small lines and polygons, points close each other
        mCache.addItem(featureId, geometry.getEnvelope());
        File cachePath = new File(mPath, CACHE);
        for (int zoom = GeoConstants.DEFAULT_CACHE_MAX_ZOOM; zoom > GeoConstants.DEFAULT_MIN_ZOOM; zoom -= 2) {
            GeoGeometry newGeometry = geometry.simplify(MapUtil.getPixelSize(zoom) * Constants.SAMPLE_DISTANCE_PX); // 4 pixels;
            saveGeometryToFile(newGeometry, new File(cachePath, zoom + "/" + featureId + SAMPLE_EXT));
            geometry = newGeometry;
        }
    }

    protected void deleteGeometryFromCache(long featureId) {
        mCache.removeItem(featureId);
        File cachePath = new File(mPath, CACHE);
        for (int zoom = GeoConstants.DEFAULT_CACHE_MAX_ZOOM; zoom > GeoConstants.DEFAULT_MIN_ZOOM; zoom -= 2) {
            FileUtil.deleteRecursive(new File(cachePath, zoom + "/" + featureId + SAMPLE_EXT));
        }
    }

    protected void changeGeometryInCache(long featureId, GeoGeometry newGeometry) {
        deleteGeometryFromCache(featureId);
        addGeometryToCache(featureId, newGeometry);
    }

    protected void changeGeometryIdInTiles(long oldFeatureId, long newFeatureId) {
        mCache.changeId(oldFeatureId, newFeatureId);
        File cachePath = new File(mPath, CACHE);
        for (int zoom = GeoConstants.DEFAULT_CACHE_MAX_ZOOM; zoom > GeoConstants.DEFAULT_MIN_ZOOM; zoom -= 2) {
            File from = new File(cachePath, zoom + "/" + oldFeatureId + SAMPLE_EXT);
            if(from.exists()) {
                File to = new File(cachePath, zoom + "/" + newFeatureId + SAMPLE_EXT);
                from.renameTo(to);
            }
        }
    }

    protected Style getDefaultStyle()
            throws Exception {
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

    protected void setDefaultRenderer() {
        if (null != mRenderer)
            return;
        try {
            mRenderer = new SimpleFeatureRenderer(this, getDefaultStyle());
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            mRenderer = null;
        }
    }

    protected void setRenderer(JSONObject jsonObject)
            throws JSONException {
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

    protected IStyleRule getStyleRule() {
        return null;
    }


    protected void reportError(String error) {
        Log.w(TAG, error);
    }

    @Override
    public JSONObject toJSON()
            throws JSONException {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_GEOMETRY_TYPE_KEY, mGeometryType);

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

        if (mExtents.isInit()) {
            rootConfig.put(Constants.JSON_BBOX_MAXX_KEY, mExtents.getMaxX());
            rootConfig.put(Constants.JSON_BBOX_MINX_KEY, mExtents.getMinX());
            rootConfig.put(Constants.JSON_BBOX_MAXY_KEY, mExtents.getMaxY());
            rootConfig.put(Constants.JSON_BBOX_MINY_KEY, mExtents.getMinY());
        }

        mCache.save(new File(mPath, RTREE));

        return rootConfig;
    }

    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException, SQLiteException {
        super.fromJSON(jsonObject);
        mGeometryType = jsonObject.getInt(JSON_GEOMETRY_TYPE_KEY);

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

        setVisible(mIsVisible);

        if (jsonObject.has(Constants.JSON_RENDERERPROPS_KEY)) {
            setRenderer(jsonObject.getJSONObject(Constants.JSON_RENDERERPROPS_KEY));
        } else {
            setDefaultRenderer();
        }
    }


    protected synchronized void reloadCache() throws SQLiteException {
        if (null == mFields || mFields.isEmpty()) {
            return;
        }

        //load vector cache
        mCacheLoaded = false;

        mCache.load(new File(mPath, RTREE));

        mCacheLoaded = true;
    }

    @Override
    public boolean delete()
            throws SQLiteException {
        //drop table
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(false);
        String tableDrop = "DROP TABLE IF EXISTS " + mPath.getName();
        db.execSQL(tableDrop);

        return super.delete();
    }

    @Override
    public boolean isValid() {
        return mExtents.isInit();
    }

    public Cursor query(
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder,
            String limit) {
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
            String limit) {
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
                    projection = new String[]{
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
                            int columnType = COLUMN_TYPE_UNKNOWN;

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

                                columnType = COLUMN_TYPE_STRING;

                            } else if (projection[sortIndex].compareToIgnoreCase(
                                    ATTACH_SIZE) == 0 || projection[sortIndex].compareToIgnoreCase(
                                    ATTACH_DATE_ADDED) == 0) {

                                columnType = COLUMN_TYPE_LONG;
                            }

                            final int columnTypeF = columnType;
                            final int sortIndexF = sortIndex;

                            Collections.sort(
                                    rowArray, new Comparator<Object[]>() {
                                        @Override
                                        public int compare(
                                                Object[] lhs,
                                                Object[] rhs) {
                                            switch (columnTypeF) {
                                                case COLUMN_TYPE_STRING:
                                                    return ((String) lhs[sortIndexF]).compareTo(
                                                            (String) rhs[sortIndexF]);

                                                case COLUMN_TYPE_LONG:
                                                    return ((Long) lhs[sortIndexF]).compareTo(
                                                            (Long) rhs[sortIndexF]);

                                                case COLUMN_TYPE_UNKNOWN:
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
                    projection = new String[]{
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


    public String getType(Uri uri) {
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
            String mimeTypeFilter) {
        int uriType = mUriMatcher.match(uri);
        switch (uriType) {
            case TYPE_ATTACH_ID:
                List<String> pathSegments = uri.getPathSegments();
                String featureId = pathSegments.get(pathSegments.size() - 3);
                String attachId = uri.getLastPathSegment();
                AttachItem item = getAttach(featureId, attachId);
                if (null != item) {
                    return new String[]{item.getMimetype()};
                }
        }
        return null;
    }

    /**
     * Insert new values and add information to changes table for sync purposes
     *
     * @param contentValues Values to add
     * @return New row identifiactor or -1
     */
    public long insertAddChanges(ContentValues contentValues) {
        long rowId = insert(contentValues);
        if (rowId != NOT_FOUND) {
            addChange(rowId, CHANGE_OPERATION_NEW);
        }
        return rowId;
    }


    protected void updateUniqId(long id) {
        if (mUniqId <= id) {
            mUniqId = id + 1;
        }
    }


    protected long insert(ContentValues contentValues) {
        if (!contentValues.containsKey(FIELD_GEOM)) {
            return NOT_FOUND;
        }

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }

        SQLiteDatabase db = map.getDatabase(false);
        long rowId = db.insert(mPath.getName(), null, contentValues);

        if (rowId != NOT_FOUND) {

            // add geometry to tiles
            if (contentValues.containsKey(FIELD_GEOM)) {
                try {
                    GeoGeometry geometry = GeoGeometryFactory.fromBlob(contentValues.getAsByteArray(FIELD_GEOM));
                    addGeometryToCache(rowId, geometry);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            Intent notify = new Intent(NOTIFY_INSERT);
            notify.putExtra(FIELD_ID, rowId);
            notify.putExtra(NOTIFY_LAYER_NAME, mPath.getName()); // if we need mAuthority?
            getContext().sendBroadcast(notify);
        }

        updateUniqId(rowId);

        return rowId;
    }

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

    public Uri insert(
            Uri uri,
            ContentValues contentValues) {
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
                if (attachID != NOT_FOUND) {
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

    /**
     * Delete feature and add information to changes table for sync purposes
     *
     * @param id Feature identificator to delete
     * @return Count of deleted features
     */
    public int deleteAddChanges(long id) {
        int result;
        if (id == Constants.NOT_FOUND)
            result = delete(id, null, null);
        else
            result = delete(id, FIELD_ID + " = " + id, null);

        if (result > 0) {
            addChange(id, CHANGE_OPERATION_DELETE);
        }

        return result;
    }


    protected int delete(
            long rowId,
            String selection,
            String[] selectionArgs) {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }

        SQLiteDatabase db = map.getDatabase(false);
        int result = db.delete(mPath.getName(), selection, selectionArgs);
        if (result > 0) {

            if (rowId == Constants.NOT_FOUND) {
                FileUtil.deleteRecursive(new File(mPath, CACHE));
            } else {
                deleteGeometryFromCache(rowId);
            }

            Intent notify;
            if (rowId == NOT_FOUND) {
                notify = new Intent(NOTIFY_DELETE_ALL);
            } else {
                notify = new Intent(NOTIFY_DELETE);
                notify.putExtra(FIELD_ID, rowId);
            }
            notify.putExtra(NOTIFY_LAYER_NAME, mPath.getName()); // if we need mAuthority?
            getContext().sendBroadcast(notify);
        }
        return result;
    }


    public int delete(
            Uri uri,
            String selection,
            String[] selectionArgs) {
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

    /**
     * Change feature and add information to changes table for sync purposes
     *
     * @param values New values to set
     * @param id     Feature identificator
     * @return Count of changed features
     */
    public int updateAddChanges(
            ContentValues values,
            long id) {
        int result = update(id, values, FIELD_ID + " = " + id, null);
        if (result > 0) {
            addChange(id, CHANGE_OPERATION_CHANGED);
        }
        return result;
    }


    protected int update(
            long rowId,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        if (null == values || values.size() > 0)
            return 0;

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }

        GeoEnvelope env = null;
        GeoGeometry newGeometry = null;
        if (values.containsKey(FIELD_GEOM) && values.containsKey(FIELD_ID)) {
            GeoGeometry geometry = getGeometryForId(values.getAsLong(FIELD_ID));
            env = geometry.getEnvelope();
            try {
                newGeometry = GeoGeometryFactory.fromBlob(values.getAsByteArray(FIELD_GEOM));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        SQLiteDatabase db = map.getDatabase(false);
        int result = db.update(mPath.getName(), values, selection, selectionArgs);
        if (result > 0) {

            if (null != env && null != newGeometry) {
                changeGeometryInCache(rowId, newGeometry);
            }

            Intent notify;
            if (rowId == NOT_FOUND) {
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
                        notify.putExtra(FIELD_ID, rowId);
                        bNotify = true;
                    }

                    if (values.containsKey(FIELD_ID)) {
                        updateUniqId(values.getAsLong(FIELD_ID));

                        notify.putExtra(FIELD_OLD_ID, rowId);
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
                    notify.putExtra(FIELD_ID, rowId);
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
            String[] selectionArgs) {
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
            throws FileNotFoundException {
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

    protected void addChange(
            long featureId,
            int operation) {
        //nothing to do
    }


    protected void addChange(
            long featureId,
            long attachId,
            int attachOperation) {
        //nothing to do
    }


    public int getGeometryType() {
        return mGeometryType;
    }


    public List<Field> getFields() {
        return new LinkedList<>(mFields.values());
    }


    public int getCount() {
        if (!mCacheLoaded)
            reloadCache();

        return mCache.size();
    }

    protected Map<String, AttachItem> getAttachMap(String featureId) {
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
            String attachId) {
        Map<String, AttachItem> attachMap = getAttachMap(featureId);
        if (null == attachMap) {
            return null;
        }

        return attachMap.get(attachId);
    }


    protected void loadAttach(String featureId) {
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


    protected void saveAttach(String featureId) {
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


    protected void deleteAttaches(String featureId) {
        mAttaches.remove(featureId);
        saveAttach(featureId);
    }


    protected void deleteAttach(
            String featureId,
            String attachId) {
        Map<String, AttachItem> attachMap = getAttachMap(featureId);
        if (null != attachMap) {
            attachMap.remove(attachId);
            saveAttach(featureId);
        }
    }

    protected void addAttach(
            String featureId,
            AttachItem item) {
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
            String newAttachId) {
        File attachFile = new File(mPath, featureId + File.separator + item.getAttachId());
        attachFile.renameTo(new File(attachFile.getParentFile(), newAttachId));

        //save changes to meta.json
        Map<String, AttachItem> attaches = getAttachMap(featureId);
        attaches.remove(item.getAttachId());
        item.setAttachId(newAttachId);
        attaches.put(item.getAttachId(), item);
        saveAttach(featureId);
    }


    public void notifyDelete(long rowId) {
        //remove cached item
        if (mCache.removeItem(rowId) != null) {
            notifyLayerChanged();
        }
    }


    public void notifyDeleteAll() {
        //clear cache
        mCache.clear();
        notifyLayerChanged();
    }


    public void notifyInsert(long rowId) {
        GeoGeometry geom = getGeometryForId(rowId);
        if (null != geom) {
            mCache.addItem(rowId, geom.getEnvelope());
            mExtents.merge(geom.getEnvelope());
            notifyLayerChanged();
        }
    }


    public void notifyUpdate(
            long rowId,
            long oldRowId) {
        GeoGeometry geom = getGeometryForId(rowId);
        if (null != geom) {
            if (oldRowId != NOT_FOUND) {
                mCache.removeItem(oldRowId);
            } else {
                mCache.removeItem(rowId);
            }

            mCache.addItem(rowId, geom.getEnvelope());
            mExtents.merge(geom.getEnvelope());

            notifyLayerChanged();
        }
    }


    public void notifyUpdateAll() {
        reloadCache();
        notifyLayerChanged();
    }


    public GeoGeometry getGeometryForId(long rowId) {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        String[] columns = new String[]{FIELD_GEOM};
        String selection = FIELD_ID + " = " + rowId;
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


    public long getUniqId() {
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
        if (mIsVisible == visible && mCacheLoaded)
            return;

        super.setVisible(visible);
        if (mIsVisible) {
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

    public GeoGeometry getGeometryForId(long featureId, int zoom) {
        if(zoom > 18)
            return getGeometryForId(featureId);
        GeoGeometry geometry = loadGeometryFromFile(new File(mPath, CACHE + "/" + zoom + "/" + featureId + SAMPLE_EXT));
        if(null != geometry)
            return geometry;
        return getGeometryForId(featureId);
    }

    public List<Long> query(GeoEnvelope env) {
        List<IGeometryCacheItem> items;
        if(null == env || env.contains(mExtents))
            items = mCache.getAll();
        else
            items = mCache.search(env);
        List<Long> result = new ArrayList<>(items.size());
        for(IGeometryCacheItem item : items)
            result.add(item.getFeatureId());
        return result;
    }
}
