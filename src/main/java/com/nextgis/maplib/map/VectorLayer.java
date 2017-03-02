/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

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
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoMultiPolygon;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;
import com.nextgis.maplib.datasource.GeometryRTree;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.display.FieldStyleRule;
import com.nextgis.maplib.display.RuleFeatureRenderer;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.SimpleLineStyle;
import com.nextgis.maplib.display.SimpleMarkerStyle;
import com.nextgis.maplib.display.SimplePolygonStyle;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.util.AttachItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FeatureChanges;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.GeoJSONUtil;
import com.nextgis.maplib.util.LayerUtil;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplib.util.NGWUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nextgis.maplib.util.Constants.CHANGE_OPERATION_CHANGED;
import static com.nextgis.maplib.util.Constants.CHANGE_OPERATION_DELETE;
import static com.nextgis.maplib.util.Constants.CHANGE_OPERATION_NEW;
import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;
import static com.nextgis.maplib.util.Constants.JSON_RENDERERPROPS_KEY;
import static com.nextgis.maplib.util.Constants.JSON_STYLE_RULE_KEY;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOCAL_VECTOR;
import static com.nextgis.maplib.util.Constants.MAX_CONTENT_LENGTH;
import static com.nextgis.maplib.util.Constants.MIN_LOCAL_FEATURE_ID;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.Constants.URI_ATTACH;
import static com.nextgis.maplib.util.Constants.URI_PARAMETER_NOT_SYNC;
import static com.nextgis.maplib.util.Constants.URI_PARAMETER_TEMP;
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
import static com.nextgis.maplib.util.GeoConstants.GTNone;
import static com.nextgis.maplib.util.GeoConstants.GTPoint;
import static com.nextgis.maplib.util.GeoConstants.GTPolygon;


/**
 * The vector layer class. It stores geometry and attributes.
 * <p/>
 * Here some examples of code to work with this layer.
 * <p/>
 * <b>To put image to the feature</b> <code> Uri newUri = ... content://com.nextgis.mobile.provider/layer_xxxxxxxxx/1/attach
 * Uri uri = getContentResolver().insert(newUri, null); try { OutputStream outStream =
 * getContentResolver().openOutputStream(uri); sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 50,
 * outStream); outStream.close(); } catch (Exception e) { Log.e(TAG, "exception while writing
 * image", e); } </code>
 * <p/>
 * <b>To get bitmap from uri</b> <code> private Bitmap getBitmapFromUri(Uri uri) throws IOException
 * { ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
 * FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor(); Bitmap image =
 * BitmapFactory.decodeFileDescriptor(fileDescriptor); parcelFileDescriptor.close(); return image; }
 * </code>
 * <p/>
 * <b>To get image using uri</b> <code> Uri featureUri = content://com.nextgis.mobile.provider/layer_xxxxxxxxx/1
 * Uri thisAttachUri = ContentUris.withAppendedId(featureUri, attachId); InputStream inStream =
 * null; try { inStream = resolver.openInputStream(thisAttachUri); // what to do with the stream is
 * up to you // I simply create a bitmap to display it Bitmap bm = BitmapFactory.decodeStream(inStream);
 * FrameLayout frame = (FrameLayout)findViewById(R.id.picture_frame); ImageView view = new
 * ImageView(getApplicationContext()); view.setImageBitmap(bm); frame.addView(view); } catch
 * (FileNotFoundException e) { Log.e(TAG, "file not found " + thisAttachUri, e); } finally { if
 * (inStream != null) { try { inStream.close(); } catch (IOException e) { Log.e(TAG, "could not
 * close stream", e); } } </code>
 * <p/>
 * <b>Also it can be used</b> <code> Uri newUri = ... content://com.nextgis.mobile.provider/layer_xxxxxxxxx/1/attach
 * Cursor cursor = resolver.query(newUri, {MediaStore.MediaColumns.DATA}, null....) File bitmapFile
 * = new File(cursor.getString(ATTACH_DATA)) and open using real path </code>
 *
 * @author Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 */

public class VectorLayer
        extends Layer
{
    protected static final String JSON_GEOMETRY_TYPE_KEY = "geometry_type";
    protected static final String JSON_FIELDS_KEY        = "fields";

    protected static final String CONTENT_ATTACH_TYPE = "vnd.android.cursor.dir/*";
    protected static final String NO_SYNC             = "no_sync";

    protected static final int TYPE_TABLE     = 1;
    protected static final int TYPE_FEATURE   = 2;
    protected static final int TYPE_ATTACH    = 3;
    protected static final int TYPE_ATTACH_ID = 4;

    protected static final String META  = "meta.json";
    protected static final String RTREE = "rtree";

    public static final String ATTACH_DISPLAY_NAME = MediaStore.MediaColumns.DISPLAY_NAME;
    public static final String ATTACH_SIZE         = MediaStore.MediaColumns.SIZE;
    public static final String ATTACH_ID           = MediaStore.MediaColumns._ID;
    public static final String ATTACH_MIME_TYPE    = MediaStore.MediaColumns.MIME_TYPE;
    public static final String ATTACH_DATA         = MediaStore.MediaColumns.DATA;
    public static final String ATTACH_DATE_ADDED   = MediaStore.MediaColumns.DATE_ADDED;
    public static final String ATTACH_DESCRIPTION  = MediaStore.Images.ImageColumns.DESCRIPTION;

    public static final int COLUMN_TYPE_UNKNOWN = 0;
    public static final int COLUMN_TYPE_STRING  = 1;
    public static final int COLUMN_TYPE_LONG    = 2;

    protected static String CONTENT_TYPE;
    protected static String CONTENT_ITEM_TYPE;

    protected static String     mAuthority;
    protected static UriMatcher mUriMatcher;

    protected Map<String, Field> mFields;

    protected boolean mCacheLoaded, mIsCacheRebuilding;
    protected int     mGeometryType;
    protected long    mUniqId;
    protected boolean mIsLocked;

    /**
     * The geometry cache for fast querying and drawing
     */
    protected IGeometryCache mCache;
    protected List<Long>     mIgnoreFeatures;


    public VectorLayer(
            Context context,
            File path)
    {
        super(context, path);

        if (!(context instanceof IGISApplication)) {
            throw new IllegalArgumentException(
                    "The context should be the instance of IGISApplication");
        }

        mCacheLoaded = false;
        mGeometryType = GTNone;

        IGISApplication application = (IGISApplication) context;

        if (null == mAuthority) {
            mAuthority = application.getAuthority();
        }

        if (null == CONTENT_TYPE) {
            CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + mAuthority;
        }
        if (null == CONTENT_ITEM_TYPE) {
            CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + mAuthority;
        }

        if (null == mUriMatcher) {
            mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

            mUriMatcher.addURI(mAuthority, "*", TYPE_TABLE);          //get all rows
            mUriMatcher.addURI(mAuthority, "*/#", TYPE_FEATURE); //get single row
            mUriMatcher.addURI(
                    mAuthority, "*/#/" + URI_ATTACH, TYPE_ATTACH);      //get attaches for row
            mUriMatcher.addURI(
                    mAuthority, "*/#/" + URI_ATTACH + "/#", TYPE_ATTACH_ID); //get attach by id
        }

        mCache = createNewCache();
        mIgnoreFeatures = new LinkedList<>();

        mLayerType = LAYERTYPE_LOCAL_VECTOR;

        mUniqId = Constants.NOT_FOUND;
    }


    protected Uri getContentUri()
    {
        return Uri.parse("content://" + mAuthority + "/" + mPath.getName());
    }


    public void create(
            int geometryType,
            final List<Field> fields)
            throws SQLiteException
    {
        mGeometryType = geometryType;
        Log.d(TAG, "init layer " + getName());

        if (null == mFields) {
            mFields = new HashMap<>(fields.size());
        } else {
            mFields.clear();
        }

        //filter out forbidden fields
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            String fieldName = field.getName();
            if (!LayerUtil.isFieldNameValid(fieldName)) {
                fields.remove(i);
                i--;

                String warning = getContext().getString(R.string.warning_remove_field);
                reportError(String.format(warning, fieldName));
                continue;
            } else {
                field.setName(LayerUtil.normalizeFieldName(fieldName));
            }

            mFields.put(field.getName(), field);
        }

        String tableCreate = "CREATE TABLE IF NOT EXISTS " + mPath.getName() + " ( " +
                //table name is the same as the folder of the layer
                Constants.FIELD_ID + " INTEGER PRIMARY KEY, ";
        for (int i = 2; i <= GeoConstants.DEFAULT_CACHE_MAX_ZOOM; i += 2) {
            tableCreate += Constants.FIELD_GEOM_ + i + " BLOB, ";
        }
        tableCreate += Constants.FIELD_GEOM + " BLOB";
        for (Field field : mFields.values()) {
            tableCreate += ", " + field.getName();
            switch (field.getType()) {
                case FTString:
                    tableCreate += " TEXT";
                    break;
                case FTInteger:
                    tableCreate += " INTEGER";
                    break;
                case FTReal:
                    tableCreate += " REAL";
                    break;
                case FTDateTime:
                case FTDate:
                case FTTime:
                    tableCreate += " TIMESTAMP";
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

        save();
    }


    public void createFromGeoJson(
            Uri uri,
            IProgressor progressor)
            throws IOException, JSONException, NGException, SQLiteException
    {
        InputStream inputStream, is;
        if (android.util.Patterns.WEB_URL.matcher(uri.toString()).matches()) {
            inputStream = new URL(uri.toString()).openStream();
            is = new URL(uri.toString()).openStream();
        } else {
            inputStream = mContext.getContentResolver().openInputStream(uri);
            is = mContext.getContentResolver().openInputStream(uri);
        }

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
            BufferedReader streamReader =
                    new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),
                            Constants.IO_BUFFER_SIZE);
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                nIncrement += inputStr.length();
                if (null != progressor) {
                    if (progressor.isCanceled()) {
                        break;
                    }
                    progressor.setValue(nIncrement);
                }
                responseStrBuilder.append(inputStr);
                if (responseStrBuilder.length() > MAX_CONTENT_LENGTH) {
                    throw new NGException(mContext.getString(R.string.error_layer_create));
                }
            }

            JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
            GeoJSONUtil.createLayerFromGeoJSON(this, jsonObject, progressor);
        } else {
            if (null != progressor) {
                progressor.setMessage(mContext.getString(R.string.message_opening));
                progressor.setIndeterminate(true);
            }

            boolean isWGS84 = GeoJSONUtil.readGeoJSONCRS(is, getContext());
            GeoJSONUtil.createLayerFromGeoJSONStream(this, inputStream, progressor, isWGS84);
        }
    }


    public void fillFromGeoJson(
            Uri uri,
            int srs,
            IProgressor progressor)
            throws IOException, JSONException, NGException, SQLiteException
    {
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
            BufferedReader streamReader =
                    new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),
                            Constants.IO_BUFFER_SIZE);
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                nIncrement += inputStr.length();
                if (null != progressor) {
                    if (progressor.isCanceled()) {
                        break;
                    }
                    progressor.setValue(nIncrement);
                }
                responseStrBuilder.append(inputStr);
                if (responseStrBuilder.length() > MAX_CONTENT_LENGTH) {
                    throw new NGException(mContext.getString(R.string.error_layer_create));
                }
            }

            JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
            GeoJSONUtil.fillLayerFromGeoJSON(this, jsonObject, srs, progressor);
        } else {
            if (null != progressor) {
                progressor.setMessage(mContext.getString(R.string.create_features));
            }
            GeoJSONUtil.fillLayerFromGeoJSONStream(this, inputStream, srs, progressor);
        }
    }


    public void createFromGeoJson(
            File path,
            IProgressor progressor)
            throws IOException, JSONException, NGException
    {
        if (null != progressor) {
            progressor.setMessage(mContext.getString(R.string.message_opening));
            progressor.setIndeterminate(true);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            String jsonData = FileUtil.readFromFile(path);
            JSONObject jsonObject = new JSONObject(jsonData);
            GeoJSONUtil.createLayerFromGeoJSON(this, jsonObject, progressor);
        } else {
            FileInputStream inputStream = new FileInputStream(path);
            FileInputStream is = new FileInputStream(path);
            boolean isWGS84 = GeoJSONUtil.readGeoJSONCRS(is, getContext());
            GeoJSONUtil.createLayerFromGeoJSONStream(this, inputStream, progressor, isWGS84);
        }
    }


    public void fillFromGeoJson(
            File path,
            int srs,
            IProgressor progressor)
            throws IOException, JSONException, NGException
    {
        if (null != progressor) {
            progressor.setMessage(mContext.getString(R.string.create_features));
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            String jsonData = FileUtil.readFromFile(path);
            JSONObject jsonObject = new JSONObject(jsonData);
            GeoJSONUtil.fillLayerFromGeoJSON(this, jsonObject, srs, progressor);
        } else {
            FileInputStream inputStream = new FileInputStream(path);
            GeoJSONUtil.fillLayerFromGeoJSONStream(this, inputStream, srs, progressor);
        }
    }


    protected boolean checkGeometryType(Feature feature)
    {
        // check if geometry type is appropriate layer geometry type
        return feature.getGeometry().getType() == mGeometryType;
    }


    protected ContentValues getFeatureContentValues(Feature feature)
    {
        final ContentValues values = feature.getContentValues(true);
        try {
            prepareGeometry(values);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return values;
    }


    public long createFeature(Feature feature)
            throws SQLiteException
    {
        if (null == feature.getGeometry() || !checkGeometryType(feature)) {
            return NOT_FOUND;
        }

        if (!mCacheLoaded) {
            reloadCache();
        }

        // check if such id already used
        // maybe was added previous session
        if (mCache.getItem(feature.getId()) != null) {
            return NOT_FOUND;
        }

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(false);

        final ContentValues values = getFeatureContentValues(feature);

        if (Constants.DEBUG_MODE) {
            Log.d(TAG, "Inserting " + values);
        }
        long rowId = db.insert(mPath.getName(), "", values);
        if (rowId != Constants.NOT_FOUND) {
            //update bbox
            cacheGeometryEnvelope(rowId, feature.getGeometry());
            save();
        }

        return rowId;
    }


    public void createFeatureBatch(
            final Feature feature,
            final SQLiteDatabase db)
            throws SQLiteException
    {
        if (null == feature.getGeometry() || !checkGeometryType(feature)) {
            return;
        }

        final ContentValues values = getFeatureContentValues(feature);

        long rowId = db.insert(mPath.getName(), "", values);
        if (rowId != Constants.NOT_FOUND) {
            //update bbox
            cacheGeometryEnvelope(rowId, feature.getGeometry());
        }
    }


    public void createField(Field field)
            throws SQLiteException
    {
        if (null == mFields || mFields.isEmpty()) //the db table is not yet created
        {
            return;
        }
        if (mFields.containsKey(field.getName())) {
            return;
        }

        if (!LayerUtil.isFieldNameValid(field.getName())) {
            return;
        } else {
            field.setName(LayerUtil.normalizeFieldName(field.getName()));
        }

        mFields.put(field.getName(), field);

        String fieldCreate = "ALTER TABLE " + mPath.getName() + " ADD COLUMN " + field.getName();

        switch (field.getType()) {
            case FTString:
                fieldCreate += " TEXT";
                break;
            case FTInteger:
                fieldCreate += " INTEGER";
                break;
            case FTReal:
                fieldCreate += " REAL";
                break;
            case FTDateTime:
            case FTDate:
            case FTTime:
                fieldCreate += " TIMESTAMP";
                break;
        }

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(false);
        db.execSQL(fieldCreate);
    }


    protected void cacheGeometryEnvelope(
            final long rowId,
            final GeoGeometry geoGeometry)
    {
        GeoEnvelope envelope;
        if (geoGeometry.getType() == GeoConstants.GTPoint) {
            GeoPoint pt = (GeoPoint) geoGeometry;
            double delta = 0.5; // as this is 3857 - the 0.5 is meters
            envelope = new GeoEnvelope(pt.getX() - delta, pt.getX() + delta, pt.getY() - delta,
                    pt.getY() + delta);
        } else {
            envelope = geoGeometry.getEnvelope();
        }
        mExtents.merge(envelope);
        mCache.addItem(rowId, envelope);
    }


    protected boolean checkPointOverlaps(
            GeoPoint pt,
            double tolerance)
    {
        double halfTolerance = tolerance * 0.3; // 0.85?
        GeoEnvelope envelope = new GeoEnvelope(pt.getX() - halfTolerance, pt.getX() + halfTolerance,
                pt.getY() - halfTolerance, pt.getY() + halfTolerance);
        return !mCache.search(envelope).isEmpty();
    }


    protected void prepareGeometry(final ContentValues values)
            throws IOException, ClassNotFoundException
    {
        GeoGeometry geometry = GeoGeometryFactory.fromBlob(values.getAsByteArray(FIELD_GEOM));
        if (null == geometry) {
            return;
        }

        if (geometry.getType() == GeoConstants.GTPoint) {
            for (int zoom = GeoConstants.DEFAULT_CACHE_MAX_ZOOM;
                 zoom > GeoConstants.DEFAULT_MIN_ZOOM;
                 zoom -= 2) {
                if (!checkPointOverlaps((GeoPoint) geometry,
                        MapUtil.getPixelSize(zoom) * Constants.SAMPLE_DISTANCE_PX)) {
                    values.put(Constants.FIELD_GEOM_ + zoom, geometry.toBlob());
                }
            }
        } else if (geometry.getType() == GeoConstants.GTMultiPoint) {
            for (int zoom = GeoConstants.DEFAULT_CACHE_MAX_ZOOM;
                 zoom > GeoConstants.DEFAULT_MIN_ZOOM;
                 zoom -= 2) {
                GeoGeometry newGeometry = geometry.simplify(
                        MapUtil.getPixelSize(zoom) * Constants.SAMPLE_DISTANCE_PX); // 4 pixels;
                GeoMultiPoint multiPoint = (GeoMultiPoint) newGeometry;
                if (multiPoint.size() == 0) {
                    break;
                } else if (multiPoint.size() == 1) {
                    if (!checkPointOverlaps(multiPoint.get(0),
                            MapUtil.getPixelSize(zoom) * Constants.SAMPLE_DISTANCE_PX)) {
                        values.put(Constants.FIELD_GEOM_ + zoom, newGeometry.toBlob());
                    } else {
                        break;
                    }
                } else {
                    values.put(Constants.FIELD_GEOM_ + zoom, newGeometry.toBlob());
                }
                geometry = newGeometry;
            }
        } else {
            if (geometry.getType() == GeoConstants.GTPolygon) {
                ((GeoPolygon) geometry).closeRings();
            } else if (geometry.getType() == GeoConstants.GTMultiPolygon) {
                ((GeoMultiPolygon) geometry).closeRings();
            }

            for (int zoom = GeoConstants.DEFAULT_CACHE_MAX_ZOOM;
                 zoom > GeoConstants.DEFAULT_MIN_ZOOM;
                 zoom -= 2) {
                GeoGeometry newGeometry = geometry.simplify(
                        MapUtil.getPixelSize(zoom) * Constants.SAMPLE_DISTANCE_PX); // 4 pixels;
                if (null == newGeometry) {
                    break;
                }
                values.put(Constants.FIELD_GEOM_ + zoom, newGeometry.toBlob());
                geometry = newGeometry;
            }
        }
    }


    public Style getDefaultStyle()
            throws Exception
    {
        switch (mGeometryType) {

            case GTPoint:
            case GTMultiPoint:
                return new SimpleMarkerStyle(
                        Color.RED, Color.BLACK, 6, SimpleMarkerStyle.MarkerStyleCircle);

            case GTLineString:
            case GTMultiLineString:
                return new SimpleLineStyle(Color.GREEN, Color.BLUE, SimpleLineStyle.LineStyleSolid);

            case GTPolygon:
            case GTMultiPolygon:
                return new SimplePolygonStyle(Color.MAGENTA);

            default:
                throw new Exception("Unknown geometry type: " + mGeometryType);
        }

    }


    protected void setDefaultRenderer()
    {
        if (null != mRenderer) {
            return;
        }
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
        try {
            JSONObject jsonObject = new JSONObject(FileUtil.readFromFile(getFileName()));
            jsonObject = jsonObject.getJSONObject(JSON_RENDERERPROPS_KEY);
            jsonObject = jsonObject.getJSONObject(JSON_STYLE_RULE_KEY);
            FieldStyleRule rule = new FieldStyleRule(this);
            rule.fromJSON(jsonObject);
            return rule;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

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

        if (!mIsCacheRebuilding) {
            mCache.save(new File(mPath, RTREE));
        }

        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException, SQLiteException
    {
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

        if (jsonObject.has(Constants.JSON_BBOX_MAXX_KEY)) {
            mExtents.setMaxX(jsonObject.getDouble(Constants.JSON_BBOX_MAXX_KEY));
        }
        if (jsonObject.has(Constants.JSON_BBOX_MAXY_KEY)) {
            mExtents.setMaxY(jsonObject.getDouble(Constants.JSON_BBOX_MAXY_KEY));
        }
        if (jsonObject.has(Constants.JSON_BBOX_MINX_KEY)) {
            mExtents.setMinX(jsonObject.getDouble(Constants.JSON_BBOX_MINX_KEY));
        }
        if (jsonObject.has(Constants.JSON_BBOX_MINY_KEY)) {
            mExtents.setMinY(jsonObject.getDouble(Constants.JSON_BBOX_MINY_KEY));
        }

        reloadCache();

        if (jsonObject.has(Constants.JSON_RENDERERPROPS_KEY)) {
            setRenderer(jsonObject.getJSONObject(Constants.JSON_RENDERERPROPS_KEY));
        } else {
            setDefaultRenderer();
        }
    }


    protected synchronized void reloadCache()
            throws SQLiteException
    {
        //load vector cache
        mCacheLoaded = false;

        mCache.load(new File(mPath, RTREE));

        mCacheLoaded = true;
    }


    @Override
    public boolean delete()
            throws SQLiteException
    {
        try {
            //drop table
            MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
            SQLiteDatabase db = map.getDatabase(false);
            String tableDrop = "DROP TABLE IF EXISTS " + mPath.getName();
            db.execSQL(tableDrop);
        } catch (SQLiteFullException e) {
            e.printStackTrace();
        }

        return super.delete();
    }


    @Override
    public boolean isValid()
    {
        return mExtents.isInit();
    }


    /**
     * In a selection are allowed bbox question:
     * <pre>{@code
     * selection = "_id = 3 AND bbox=[123, 233, 3432, 23444] OR _id = 5";}</pre>
     * Are allowed:
     * <pre>{@code
     * "bbox=[-1,-2.3,34.2,56]" or "bbox =  [  -1 , -2.3, 34.2 ,56 ]"
     * in bbox --  "bbox=[..]", "bbox==[..]"
     * out bbox -- "bbox!=[..]", "bbox<>[..]"}</pre>
     */
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

        // work for bbox selection
        if (null != selection) {
            String bboxRegex = "(bbox\\s*((=)|(==)|(!=)|(<>))\\s*\\[" +
                    "\\s*\\-?\\d+(\\.\\d+)?\\s*," +
                    "\\s*\\-?\\d+(\\.\\d+)?\\s*," +
                    "\\s*\\-?\\d+(\\.\\d+)?\\s*," +
                    "\\s*\\-?\\d+(\\.\\d+)?\\s*\\])";

            Matcher selMatcher = Pattern.compile(bboxRegex).matcher(selection);

            if (selMatcher.find()) {

                String bboxStr = selMatcher.group();
                Matcher bboxMatcher = Pattern.compile("\\-?\\d+(\\.\\d+)?").matcher(bboxStr);

                Double minX = bboxMatcher.find() ? Double.parseDouble(bboxMatcher.group()) : null;
                Double minY = bboxMatcher.find() ? Double.parseDouble(bboxMatcher.group()) : null;
                Double maxX = bboxMatcher.find() ? Double.parseDouble(bboxMatcher.group()) : null;
                Double maxY = bboxMatcher.find() ? Double.parseDouble(bboxMatcher.group()) : null;

                GeoEnvelope envelope = new GeoEnvelope();

                if (minX != null && minY != null) {
                    envelope.setMin(minX, minY);
                }

                if (maxX != null && maxY != null) {
                    envelope.setMax(maxX, maxY);
                }

                if (!envelope.isInit()) {
                    throw new SQLiteException("bbox has bad format, " + bboxStr);
                }

                Matcher eqMatcher = Pattern.compile("((=)|(==)|(!=)|(<>))").matcher(bboxStr);
                boolean isNotIn = false;
                if (eqMatcher.find() && eqMatcher.group().matches("((!=)|(<>))")) {
                    isNotIn = true;
                }

                List<Long> ids = query(envelope);
                StringBuilder sb = new StringBuilder(1024);

                for (Long fid : ids) {
                    if (sb.length() == 0) {
                        sb.append(FIELD_ID);
                        if (isNotIn) {
                            sb.append(" NOT IN (");
                        } else {
                            sb.append(" IN (");
                        }
                    } else {
                        sb.append(",");
                    }
                    sb.append(fid);
                }

                if (sb.length() > 0) {
                    sb.append(")");
                    selection = selMatcher.replaceAll(sb.toString());
                } else {
                    selection = selMatcher.replaceAll(FIELD_ID + " == -98765"); // always is false
                }
            }
        }


        try {
            return db.query(mPath.getName(), projection, selection, selectionArgs, null, null,
                    sortOrder, limit);
        } catch (SQLiteException e) {
            Log.d(TAG, e.getLocalizedMessage());
            return null;
        }
    }


    final public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder,
            String limit)
    {
        int uriType = mUriMatcher.match(uri);
        return queryInternal(uri, uriType, projection, selection, selectionArgs, sortOrder, limit);
    }


    protected Cursor queryInternal(
            Uri uri,
            int uriType,
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

        switch (uriType) {

            case TYPE_TABLE:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = FIELD_ID + " ASC";
                }
                cursor = query(projection, selection, selectionArgs, sortOrder, limit);
                if (null != cursor) {
                    cursor.setNotificationUri(getContext().getContentResolver(), getContentUri());
                }
                return cursor;

            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();

                String changeSel = FIELD_ID + " = " + featureId;

                if (TextUtils.isEmpty(selection)) {
                    selection = changeSel;
                } else {
                    selection += " AND " + changeSel;
                }

                cursor = query(projection, selection, selectionArgs, sortOrder, limit);
                if (null != cursor) {
                    cursor.setNotificationUri(getContext().getContentResolver(), getContentUri());
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
                            int columnType = COLUMN_TYPE_UNKNOWN;

                            if (projection[sortIndex].compareToIgnoreCase(ATTACH_DISPLAY_NAME) == 0
                                    ||
                                    projection[sortIndex].compareToIgnoreCase(ATTACH_DATA) == 0 ||
                                    projection[sortIndex].compareToIgnoreCase(ATTACH_MIME_TYPE) == 0
                                    ||
                                    projection[sortIndex].compareToIgnoreCase(ATTACH_ID) == 0 ||
                                    projection[sortIndex].compareToIgnoreCase(ATTACH_DESCRIPTION)
                                            == 0) {

                                columnType = COLUMN_TYPE_STRING;

                            } else if (projection[sortIndex].compareToIgnoreCase(ATTACH_SIZE) == 0
                                    || projection[sortIndex].compareToIgnoreCase(ATTACH_DATE_ADDED)
                                    == 0) {

                                columnType = COLUMN_TYPE_LONG;
                            }

                            final int columnTypeF = columnType;
                            final int sortIndexF = sortIndex;

                            Collections.sort(rowArray, new Comparator<Object[]>()
                            {
                                @Override
                                public int compare(
                                        Object[] lhs,
                                        Object[] rhs)
                                {
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
                    projection = new String[] {
                            ATTACH_DISPLAY_NAME, ATTACH_SIZE, ATTACH_ID, ATTACH_MIME_TYPE};
                }
                matrixCursor = new MatrixCursor(projection);
                //get attach path
                AttachItem item = getAttach(featureId, attachId);
                if (null != item) {
                    File attachFile = new File(mPath, featureId + File.separator +
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


    /**
     * Insert new values and add information to changes table for sync purposes
     *
     * @param contentValues
     *         Values to add
     *
     * @return New row identifiactor or -1
     */
    public long insertAddChanges(ContentValues contentValues)
    {
        long rowId = insert(contentValues);
        if (rowId != NOT_FOUND) {
            addChange(rowId, CHANGE_OPERATION_NEW);
        }
        return rowId;
    }


    protected void updateUniqId(long id)
    {
        if (mUniqId <= id) {
            mUniqId = id + 1;
        }
    }


    protected long insert(ContentValues contentValues)
    {
        if (!contentValues.containsKey(Constants.FIELD_GEOM)) {
            return NOT_FOUND;
        }

        return insertInternal(contentValues);
    }


    protected long insertInternal(ContentValues contentValues)
    {
        if (contentValues.containsKey(Constants.FIELD_GEOM)) {
            try {
                prepareGeometry(contentValues);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }

        SQLiteDatabase db = map.getDatabase(false);
        long rowId = db.insert(mPath.getName(), null, contentValues);

        if (rowId != Constants.NOT_FOUND) {
            Intent notify = new Intent(Constants.NOTIFY_INSERT);
            notify.putExtra(FIELD_ID, rowId);
            notify.putExtra(Constants.NOTIFY_LAYER_NAME, mPath.getName()); // if we need mAuthority?
            getContext().sendBroadcast(notify);
        }

        updateUniqId(rowId);

        return rowId;
    }


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
                try {
                    FileUtil.createDir(attachFolder);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
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
        // http://stackoverflow.com/a/24055457
        String tempParam = uri.getQueryParameter(URI_PARAMETER_TEMP);
        String notSyncParam = uri.getQueryParameter(URI_PARAMETER_NOT_SYNC);

        Boolean tempFlag = null == tempParam ? null : Boolean.parseBoolean(tempParam);
        Boolean notSyncFlag = null == notSyncParam ? null : Boolean.parseBoolean(notSyncParam);
        boolean hasNotFlags =
                (null == tempFlag || !tempFlag) && (null == notSyncFlag || !notSyncFlag);

        int uriType = mUriMatcher.match(uri);

        switch (uriType) {

            case TYPE_TABLE:
                long rowID = hasNotFlags ? insert(contentValues) : insertInternal(contentValues);

                if (rowID != Constants.NOT_FOUND) {
                    Uri resultUri = ContentUris.withAppendedId(getContentUri(), rowID);
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(resultUri, null, false);

                    } else {
                        if (null != tempFlag) {
                            setFeatureTempFlag(rowID, tempFlag);
                        }

                        if (null != notSyncFlag) {
                            setFeatureNotSyncFlag(rowID, notSyncFlag);
                        }

                        if (hasNotFlags) {
                            addChange(rowID, CHANGE_OPERATION_NEW);
                        }

                        getContext().getContentResolver().notifyChange(resultUri, null, true);
                    }
                    return resultUri;
                }
                return null;

            case TYPE_ATTACH:
                List<String> pathSegments = uri.getPathSegments();
                String featureId = pathSegments.get(pathSegments.size() - 2);
                long attachIdL = insertAttach(featureId, contentValues);
                if (attachIdL != NOT_FOUND) {
                    Uri resultUri = ContentUris.withAppendedId(uri, attachIdL);
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(resultUri, null, false);

                    } else {
                        long featureIdL = Long.parseLong(featureId);

                        if (null != tempFlag) {
                            setAttachTempFlag(featureIdL, attachIdL, tempFlag);
                        }

                        if (null != notSyncFlag) {
                            setAttachNotSyncFlag(featureIdL, attachIdL, notSyncFlag);
                        }

                        if (hasNotFlags) {
                            addChange(featureIdL, attachIdL, CHANGE_OPERATION_NEW);
                        }

                        getContext().getContentResolver().notifyChange(resultUri, null, true);
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
     * @param id
     *         Feature identificator to delete
     *
     * @return Count of deleted features
     */
    public int deleteAddChanges(long id)
    {
        int result;
        if (id == Constants.NOT_FOUND) {
            result = delete(id, null, null);
        } else {
            result = delete(id, FIELD_ID + " = " + id, null);
        }

        if (result > 0) {
            addChange(id, CHANGE_OPERATION_DELETE);
        }

        return result;
    }


    protected int delete(
            long rowId,
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

            /* fill from notify if (rowId == Constants.NOT_FOUND) {
                mCache.clear();
            } else {
                mCache.removeItem(rowId);
            }*/

            Intent notify;
            if (rowId == Constants.NOT_FOUND) {
                notify = new Intent(Constants.NOTIFY_DELETE_ALL);
            } else {
                notify = new Intent(Constants.NOTIFY_DELETE);
                notify.putExtra(FIELD_ID, rowId);
            }
            notify.putExtra(Constants.NOTIFY_LAYER_NAME, mPath.getName()); // if we need mAuthority?
            getContext().sendBroadcast(notify);
        }
        return result;
    }


    final public int delete(
            Uri uri,
            String selection,
            String[] selectionArgs)
    {
        int uriType = mUriMatcher.match(uri);
        return deleteInternal(uri, uriType, selection, selectionArgs);
    }


    protected int deleteInternal(
            Uri uri,
            int uriType,
            String selection,
            String[] selectionArgs)
    {
        String featureId;
        long featureIdL;
        String attachId;
        List<String> pathSegments;
        int result;

        // http://stackoverflow.com/a/24055457
        String tempParam = uri.getQueryParameter(URI_PARAMETER_TEMP);
        String notSyncParam = uri.getQueryParameter(URI_PARAMETER_NOT_SYNC);

        Boolean tempFlag = null == tempParam ? null : Boolean.parseBoolean(tempParam);
        Boolean notSyncFlag = null == notSyncParam ? null : Boolean.parseBoolean(notSyncParam);
        boolean hasNotFlags = null == tempFlag && null == notSyncFlag;

        switch (uriType) {
            case TYPE_TABLE:
                result = delete(NOT_FOUND, selection, selectionArgs);
                if (result > 0) {
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    } else {
                        if (null != tempFlag) {
                            // setFeatureTempFlag(featureIdL, false); // TODO for table
                        }

                        if (null != notSyncFlag) {
                            // setFeatureNotSyncFlag(featureIdL, false); // TODO for table
                        }

                        if (hasNotFlags) {
                            addChange(NOT_FOUND, CHANGE_OPERATION_DELETE);
                        }
                        getContext().getContentResolver().notifyChange(uri, null, true);
                    }
                }
                return result;
            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();
                featureIdL = Long.parseLong(featureId);

                String changeSel = FIELD_ID + " = " + featureId;

                if (TextUtils.isEmpty(selection)) {
                    selection = changeSel;
                } else {
                    selection += " AND " + changeSel;
                }

                result = delete(featureIdL, selection, selectionArgs);

                if (result > 0) {
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    } else {
                        if (null != tempFlag) {
                            setFeatureTempFlag(featureIdL, false);
                        }

                        if (null != notSyncFlag) {
                            setFeatureNotSyncFlag(featureIdL, false);
                        }

                        if (hasNotFlags) {
                            addChange(featureIdL, CHANGE_OPERATION_DELETE);
                        }

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
                if (attachFolder.exists()) {
                    for (File attachFile : attachFolder.listFiles()) {
                        if (attachFile.delete()) {
                            result++;
                        }
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
                        if (null != tempFlag) {
                            setAttachesTempFlag(featureIdL, false);
                        }

                        if (null != notSyncFlag) {
                            setAttachesNotSyncFlag(featureIdL, false);
                        }

                        if (hasNotFlags) {
                            addChange(featureIdL, NOT_FOUND, CHANGE_OPERATION_DELETE);
                        }

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

                        if (null != tempFlag) {
                            setAttachTempFlag(featureIdL, attachIdL, false);
                        }

                        if (null != notSyncFlag) {
                            setAttachNotSyncFlag(featureIdL, attachIdL, false);
                        }

                        if (hasNotFlags) {
                            addChange(featureIdL, attachIdL, CHANGE_OPERATION_DELETE);
                        }
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
     * @param values
     *         New values to set
     * @param id
     *         Feature identificator
     *
     * @return Count of changed features
     */
    public int updateAddChanges(
            ContentValues values,
            long id)
    {
        int result = update(id, values, Constants.FIELD_ID + " = " + id, null);
        if (result > 0) {
            addChange(id, CHANGE_OPERATION_CHANGED);
        }
        return result;
    }


    protected int update(
            long rowId,
            ContentValues values,
            String selection,
            String[] selectionArgs)
    {
        if (null == values || values.size() < 1) {
            return 0;
        }

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }

        if (values.containsKey(Constants.FIELD_GEOM)) {
            try {
                // remove current cache item to not intersect with itself
//                mCache.removeItem(rowId);
                prepareGeometry(values);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        SQLiteDatabase db = map.getDatabase(false);
        int result = db.update(mPath.getName(), values, selection, selectionArgs);
        if (result > 0) {
            Intent notify;
            if (rowId == Constants.NOT_FOUND) {
                if (values.containsKey(Constants.FIELD_GEOM)) {
                    notify = new Intent(Constants.NOTIFY_UPDATE_ALL);
                    notify.putExtra(
                            Constants.NOTIFY_LAYER_NAME, mPath.getName()); // if we need mAuthority?
                    getContext().sendBroadcast(notify);
                }
            } else if (values.containsKey(Constants.FIELD_GEOM) || values.containsKey(
                    Constants.FIELD_ID)) {
                notify = new Intent(Constants.NOTIFY_UPDATE);
                boolean bNotify = false;
                if (values.containsKey(Constants.FIELD_GEOM)) {
                    notify.putExtra(Constants.FIELD_ID, rowId);
                    bNotify = true;
                }

                if (values.containsKey(Constants.FIELD_ID)) {
                    updateUniqId(values.getAsLong(Constants.FIELD_ID));

                    notify.putExtra(Constants.FIELD_OLD_ID, rowId);
                    notify.putExtra(Constants.FIELD_ID, values.getAsLong(Constants.FIELD_ID));
                    bNotify = true;
                }

                if (bNotify) {
                    notify.putExtra(Constants.ATTRIBUTES_ONLY, false);
                    notify.putExtra(
                            Constants.NOTIFY_LAYER_NAME, mPath.getName()); // if we need mAuthority?
                    getContext().sendBroadcast(notify);
                }

            } else {
                notify = new Intent(Constants.NOTIFY_UPDATE_FIELDS);
                notify.putExtra(Constants.FIELD_ID, rowId);
                notify.putExtra(Constants.ATTRIBUTES_ONLY, true);
                notify.putExtra(
                        Constants.NOTIFY_LAYER_NAME, mPath.getName()); // if we need mAuthority?
                getContext().sendBroadcast(notify);
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

        // http://stackoverflow.com/a/24055457
        String tempParam = uri.getQueryParameter(URI_PARAMETER_TEMP);
        String notSyncParam = uri.getQueryParameter(URI_PARAMETER_NOT_SYNC);

        Boolean tempFlag = null == tempParam ? null : Boolean.parseBoolean(tempParam);
        Boolean notSyncFlag = null == notSyncParam ? null : Boolean.parseBoolean(notSyncParam);
        boolean hasNotFlags = null == tempFlag && null == notSyncFlag;

        boolean resetFlags = null != tempFlag && !tempFlag && null != notSyncFlag && !notSyncFlag
                || null != tempFlag && !tempFlag && null == notSyncFlag
                || null == tempFlag && null != notSyncFlag && !notSyncFlag;

        int uriType = mUriMatcher.match(uri);

        switch (uriType) {

            case TYPE_TABLE:
                result = update(Constants.NOT_FOUND, values, selection, selectionArgs);

                if (result > 0) {
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);

                    } else {

                        // if (null != tempFlag) {
                        // setFeatureTempFlag(featureIdL, tempFlag); // TODO for table
                        // }

                        // if (null != notSyncFlag) {
                        // setFeatureNotSyncFlag(featureIdL, notSyncFlag); // TODO for table
                        // }

                        // if (resetFlags && !hasFeatureChanges(featureIdL)) {
                        // addChange(featureIdL, CHANGE_OPERATION_NEW); // TODO for table
                        // }

                        if (hasNotFlags) {
                            addChange(Constants.NOT_FOUND, CHANGE_OPERATION_CHANGED);
                        }

                        getContext().getContentResolver().notifyChange(uri, null);
                    }
                }

                return result;

            case TYPE_FEATURE:
                featureId = uri.getLastPathSegment();
                featureIdL = Long.parseLong(featureId);
                String changeSel = FIELD_ID + " = " + featureId;
                if (TextUtils.isEmpty(selection)) {
                    selection = changeSel;
                } else {
                    selection = selection + " AND " + changeSel;
                }

                result = update(featureIdL, values, selection, selectionArgs);

                if (result > 0) {
                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);

                    } else {
                        if (null != tempFlag) {
                            setFeatureTempFlag(featureIdL, tempFlag);
                        }

                        if (null != notSyncFlag) {
                            setFeatureNotSyncFlag(featureIdL, notSyncFlag);
                        }

                        if (resetFlags && !hasFeatureChanges(featureIdL)) {
                            addChange(featureIdL, CHANGE_OPERATION_NEW);
                        }

                        if (hasNotFlags) {
                            addChange(featureIdL, CHANGE_OPERATION_CHANGED);
                        }

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

                            if (null != tempFlag) {
                                setAttachTempFlag(featureIdL, attachIdL, tempFlag);
                            }

                            if (null != notSyncFlag) {
                                setAttachNotSyncFlag(featureIdL, attachIdL, notSyncFlag);
                            }

                            if (resetFlags && !hasAttachChanges(featureIdL, attachIdL)) {
                                addChange(featureIdL, CHANGE_OPERATION_NEW);
                            }

                            if (hasNotFlags) {
                                addChange(featureIdL, attachIdL, CHANGE_OPERATION_CHANGED);
                            }

                            ++changed;
                        }
                    }
                    return changed;
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


                    Map<String, AttachItem> attaches = getAttachMap(featureId);
                    if (null == attaches) {
                        return 0;
                    }
                    AttachItem item = attaches.get(attachId);
                    if (null == item) {
                        return 0;
                    }

                    boolean isItemChanged = false;
                    if (values.containsKey(ATTACH_DESCRIPTION)) {
                        item.setDescription(values.getAsString(ATTACH_DESCRIPTION));
                        isItemChanged = true;
                    }
                    if (values.containsKey(ATTACH_DISPLAY_NAME)) {
                        item.setDisplayName(values.getAsString(ATTACH_DISPLAY_NAME));
                        isItemChanged = true;
                    }
                    if (values.containsKey(ATTACH_MIME_TYPE)) {
                        item.setMimetype(values.getAsString(ATTACH_MIME_TYPE));
                        isItemChanged = true;
                    }
                    if (isItemChanged) {
                        // saveAttach() MUST be before setNewAttachId()
                        saveAttach(featureId, attaches);
                    }


                    if (values.containsKey(ATTACH_ID)) {
                        setNewAttachId(featureId, item, values.getAsString(ATTACH_ID));
                    }


                    String fragment = uri.getFragment();
                    boolean bFromNetwork = null != fragment && fragment.equals(NO_SYNC);
                    if (bFromNetwork) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    } else {

                        if (null != tempFlag) {
                            setAttachTempFlag(featureIdL, attachIdL, tempFlag);
                        }

                        if (null != notSyncFlag) {
                            setAttachNotSyncFlag(featureIdL, attachIdL, notSyncFlag);
                        }

                        if (resetFlags && !hasAttachChanges(featureIdL, attachIdL)) {
                            addChange(featureIdL, CHANGE_OPERATION_NEW);
                        }

                        if (hasNotFlags) {
                            addChange(featureIdL, attachIdL, CHANGE_OPERATION_CHANGED);
                        }

                        getContext().getContentResolver().notifyChange(uri, null);
                    }
                    return 1;
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
                        nMode = ParcelFileDescriptor.MODE_READ_WRITE
                                | ParcelFileDescriptor.MODE_APPEND;
                        break;
                    case "rwt":
                        nMode = ParcelFileDescriptor.MODE_READ_WRITE
                                | ParcelFileDescriptor.MODE_TRUNCATE;
                        break;
                }

                return ParcelFileDescriptor.open(
                        new File(mPath, featureId + File.separator + attachId), nMode);
            default:
                throw new FileNotFoundException();
        }
    }


    public void addChange(
            long featureId,
            int operation)
    {
        //nothing to do
    }


    public void addChange(
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


    public boolean isFieldsInitialized() {
        return mFields != null;
    }


    public List<Field> getFields()
    {
        if (null == mFields) {
            return new ArrayList<>();
        }
        return new LinkedList<>(mFields.values());
    }


    public Field getFieldByName(String name)
    {
        return mFields.get(name);
    }


    public int getCount()
    {
        if (!mCacheLoaded) {
            reloadCache();
        }

        return mCache.size();
    }


    public Feature cursorToFeature(Cursor cursor)
    {
        Feature out = new Feature((long) Constants.NOT_FOUND, getFields());
        out.fromCursor(cursor);
        //add extensions to feature
        out.addAttachments(getAttachMap("" + out.getId()));
        return out;
    }


    public Map<String, AttachItem> getAttachMap(String featureId)
    {
        return loadAttach(featureId);
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


    protected Map<String, AttachItem> loadAttach(String featureId)
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
            return attach;

        } catch (IOException | JSONException e) {
            // e.printStackTrace();
        }

        return null;
    }


    protected void saveAttach(
            String featureId,
            Map<String, AttachItem> attachMap)
    {
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
        File attachFolder = new File(mPath, featureId);
        FileUtil.renameAndDelete(attachFolder);
    }


    protected void deleteAttach(
            String featureId,
            String attachId)
    {
        Map<String, AttachItem> attachMap = getAttachMap(featureId);
        if (null != attachMap) {
            attachMap.remove(attachId);

            if (attachMap.size() > 0) {
                saveAttach(featureId, attachMap);
            } else {
                deleteAttaches(featureId);
            }
        }
    }


    protected void addAttach(
            String featureId,
            AttachItem item)
    {
        Map<String, AttachItem> attachMap = getAttachMap(featureId);
        if (null == attachMap) {
            attachMap = new HashMap<>();
        }

        attachMap.put(item.getAttachId(), item);
        saveAttach(featureId, attachMap);
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

        if (null == attaches) {
            attaches = new HashMap<>();
        } else {
            attaches.remove(item.getAttachId());
        }

        item.setAttachId(newAttachId);
        attaches.put(item.getAttachId(), item);
        saveAttach(featureId, attaches);
    }


    @Override
    public void notifyDelete(long rowId)
    {
        //remove cached item
        if (mCache.removeItem(rowId) != null) {
            save();
            notifyLayerChanged();
        }
    }


    @Override
    public void onUpgrade(
            SQLiteDatabase sqLiteDatabase,
            int oldVersion,
            int newVersion)
    {
        // upgrade db geometry storage
        if (oldVersion == 1) {
            // 1. alter table
            for (int i = 2; i <= GeoConstants.DEFAULT_CACHE_MAX_ZOOM; i += 2) {
                String tableAlter =
                        "ALTER TABLE " + mPath.getName() + " ADD COLUMN " + Constants.FIELD_GEOM_
                                + i + " BLOB;";
                try {
                    sqLiteDatabase.execSQL(tableAlter);
                } catch (SQLiteException e) {
                    e.printStackTrace();
                }
            }
            // 2. get geometry
            String[] columns = new String[] {FIELD_ID, FIELD_GEOM};
            Cursor cursor =
                    sqLiteDatabase.query(mPath.getName(), columns, null, null, null, null, null);
            List<Pair<Long, GeoGeometry>> changeValues = new LinkedList<>();
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    do {
                        try {
                            GeoGeometry geoGeometry =
                                    GeoGeometryFactory.fromBlobOld(cursor.getBlob(1));
                            if (null != geoGeometry) {
                                long rowId = cursor.getLong(0);
                                changeValues.add(new Pair<>(rowId, geoGeometry));
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            Log.d(Constants.TAG, "Layer: " + getName());
                            e.printStackTrace();
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            // 3. insert geometry
            for (Pair<Long, GeoGeometry> pair : changeValues) {
                String selection = FIELD_ID + " = " + pair.first;
                ContentValues values = new ContentValues();
                try {
                    values.put(FIELD_GEOM, pair.second.toBlob());
                    prepareGeometry(values);
                    int result = sqLiteDatabase.update(mPath.getName(), values, selection, null);
                    if (result > 0) {
                        cacheGeometryEnvelope(pair.first, pair.second);
                    }

                } catch (IOException | ClassNotFoundException | SQLiteException e) {
                    e.printStackTrace();
                }
            }
            // 4. save layer
            save();
        }
    }


    @Override
    public void notifyDeleteAll()
    {
        //clear cache
        mCache.clear();
        save();
        notifyLayerChanged();
    }


    @Override
    public void notifyInsert(long rowId)
    {

        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "notifyInsert id: " + rowId);
        }

        GeoGeometry geom = getGeometryForId(rowId);
        if (null != geom) {
            cacheGeometryEnvelope(rowId, geom);
            save();
            notifyLayerChanged();
        }
    }


    @Override
    public void notifyUpdate(
            long rowId,
            long oldRowId,
            boolean attributesOnly)
    {
        if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "notifyUpdate id: " + rowId + ", old_id: " + oldRowId);
        }

        boolean needSave = false;
        if (oldRowId != Constants.NOT_FOUND) {
            mCache.changeId(oldRowId, rowId);
            needSave = true;
        }

        GeoGeometry geom = getGeometryForId(rowId);
        if (null != geom && !attributesOnly) {
            mCache.removeItem(rowId);
            cacheGeometryEnvelope(rowId, geom);
            needSave = true;
        }

        if (needSave) {
            save();
        }

        notifyLayerChanged();
    }


    @Override
    public void notifyUpdateAll()
    {
        reloadCache();
        notifyLayerChanged();
    }


    public GeoGeometry getGeometryForId(long rowId)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }
        SQLiteDatabase db = map.getDatabase(true);
        String[] columns = new String[] {Constants.FIELD_GEOM};
        String selection = Constants.FIELD_ID + " = " + rowId;
        return getGeometryFromQuery(columns, selection, db);
    }


    public GeoGeometry getGeometryForId(
            long rowId,
            SQLiteDatabase db)
    {
        String[] columns = new String[] {Constants.FIELD_GEOM};
        String selection = Constants.FIELD_ID + " = " + rowId;
        return getGeometryFromQuery(columns, selection, db);
    }


    protected GeoGeometry getGeometryFromQuery(
            String[] columns,
            String selection,
            SQLiteDatabase db)
    {
        Cursor cursor = db.query(mPath.getName(), columns, selection, null, null, null, null);
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                try {
                    GeoGeometry result = GeoGeometryFactory.fromBlob(cursor.getBlob(0));
                    cursor.close();
                    return result;
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
            if (null != cursor) {
                try {
                    if (cursor.moveToFirst()) {
                        mUniqId = cursor.getLong(0) + 1;
                    }
                } catch (Exception e) {
                    //Log.d(TAG, e.getLocalizedMessage());
                } finally {
                    cursor.close();
                }
            }
        }

        return mUniqId;
    }


    public GeoGeometry getGeometryForId(
            long rowId,
            int zoom)
    {
        if (zoom > GeoConstants.DEFAULT_CACHE_MAX_ZOOM) {
            return getGeometryForId(rowId);
        }

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        if (null == map) {
            throw new IllegalArgumentException(
                    "The map should extends MapContentProviderHelper or inherited");
        }
        SQLiteDatabase db = map.getDatabase(true);
        String[] columns = new String[] {Constants.FIELD_GEOM_ + zoom};
        String selection = Constants.FIELD_ID + " = " + rowId;

        return getGeometryFromQuery(columns, selection, db);
    }


    public GeoGeometry getGeometryForId(
            long rowId,
            int zoom,
            SQLiteDatabase db)
    {
        if (zoom > GeoConstants.DEFAULT_CACHE_MAX_ZOOM) {
            return getGeometryForId(rowId, db);
        }

        String[] columns = new String[] {Constants.FIELD_GEOM_ + zoom};
        String selection = Constants.FIELD_ID + " = " + rowId;

        return getGeometryFromQuery(columns, selection, db);
    }


    public List<Long> query(GeoEnvelope env) {
        List<IGeometryCacheItem> items;
        if (null == env || !env.isInit() || !mExtents.isInit() || env.contains(mExtents))
            items = mCache.getAll();
        else
            items = mCache.search(env);

        List<Long> result = new ArrayList<>(items.size());
        for (IGeometryCacheItem item : items)
            result.add(item.getFeatureId());

        return result;
    }


    public void hideFeature(long featureId)
    {
        if (featureId != NOT_FOUND) {
            mIgnoreFeatures.add(featureId);
            notifyLayerChanged();
        }
    }


    public void showFeature(long featureId)
    {
        if (mIgnoreFeatures.isEmpty() || featureId == NOT_FOUND) {
            return;
        }

        mIgnoreFeatures.remove(featureId);
        notifyLayerChanged();
    }


    public void showAllFeatures()
    {
        if (mIgnoreFeatures.isEmpty()) {
            return;
        }
        mIgnoreFeatures.clear();
        notifyLayerChanged();
    }


    public boolean isFeatureHidden(long featureId)
    {
        return mIgnoreFeatures.contains(featureId);
    }


    public void swapFeaturesVisibility(
            long previousFeatureId,
            long featureId)
    {
        mIgnoreFeatures.remove(previousFeatureId);
        mIgnoreFeatures.add(featureId);
        notifyLayerChanged();
    }

    protected IGeometryCache createNewCache() {
        return new GeometryRTree();
    }

    public void rebuildCache(IProgressor progressor)
    {
        if (null != progressor) {
            progressor.setMessage(mContext.getString(R.string.rebuild_cache));
        }

        String columns[] = {FIELD_ID, FIELD_GEOM};
        Cursor cursor = query(columns, null, null, null, null);
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                if (null != progressor) {
                    progressor.setMax(cursor.getCount());
                }

                mIsCacheRebuilding = true;
                mCache = createNewCache();
                int counter = 0;
                do {
                    GeoGeometry geometry = null;
                    try {
                        geometry = GeoGeometryFactory.fromBlob(cursor.getBlob(1));
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    if (null != geometry) {
                        long rowId = cursor.getLong(0);
                        mCache.addItem(rowId, geometry.getEnvelope());
                    }

                    if (null != progressor) {
                        if (progressor.isCanceled()) {
                            break;
                        }
                        progressor.setValue(++counter);
                        progressor.setMessage(
                                mContext.getString(R.string.process_features) + ": " + counter);
                    }

                } while (cursor.moveToNext());

                mIsCacheRebuilding = false;
            }
            cursor.close();
            save();
        }
    }


    public boolean isChanges()
    {
        return false;
    }


    protected boolean hasFeatureChanges(long featureId)
    {
        return false;
    }


    protected boolean haveFeaturesNotSyncFlag()
    {
        return false;
    }


    protected boolean hasAttachChanges(
            long featureId,
            long attachId)
    {
        return false;
    }


    public boolean hasFeatureAttaches(long featureId)
    {
        Map<String, AttachItem> attachMap = getAttachMap("" + featureId);
        if (null == attachMap) {
            return false;
        }

        Set<String> attachIds = attachMap.keySet();

        return attachIds.size() > 0;
    }


    public Feature getFeature(long featureId)
    {
        Cursor cursor = query(null, FIELD_ID + " = " + featureId, null, null, null);
        if (null == cursor) {
            return null;
        }

        Feature feature = null;
        if (cursor.moveToFirst()) {
            feature = new Feature(featureId, getFields());
            feature.fromCursor(cursor);
        }

        cursor.close();

        return feature;
    }


    public Feature getFeatureWithAttaches(long featureId)
    {
        Feature feature = getFeature(featureId);

        if (null != feature) {
            feature.addAttachments(getAttachMap("" + feature.getId()));
        }

        return feature;
    }


    public Cursor queryFirstTempFeatureFlags()
    {
        // TODO: move work with temp features into VectorLayer
        return null;
    }


    public Cursor queryFirstTempAttachFlags()
    {
        // TODO: move work with temp features into VectorLayer
        return null;
    }


    public Feature getNewTempFeature()
    {
        Feature feature = new Feature(NOT_FOUND, getFields());
        Uri uri = insertTempFeature(feature);

        if (uri == null) {
            return null;
        }

        long featureId = Long.parseLong(uri.getLastPathSegment());
        feature.setId(featureId);

        return feature;
    }


    public AttachItem getNewTempAttach(Feature feature)
    {
        long featureId = feature.getId();
        AttachItem attachItem = new AttachItem("" + NOT_FOUND, "", "", "");
        Uri uri = insertTempAttach(featureId, attachItem);

        if (uri == null) {
            return null;
        }

        String attachId = uri.getLastPathSegment();
        attachItem = getAttach("" + featureId, attachId);
        feature.addAttachment(attachItem);

        return attachItem;
    }


    public boolean insertAttachFile(
            long featureId,
            long attachId,
            File attachFile)
    {
        Uri uri = Uri.parse("content://" + mAuthority + "/" + mPath.getName() +
                "/" + featureId + "/" + Constants.URI_ATTACH + "/" + attachId);
        try {
            OutputStream attachOutStream = mContext.getContentResolver().openOutputStream(uri);
            if (attachOutStream != null) {
                FileUtil.copy(new FileInputStream(attachFile), attachOutStream);
                attachOutStream.close();
            }

        } catch (IOException e) {
            Log.d(TAG, "create attach file failed, " + e.getLocalizedMessage());
            return false;
        }

        return true;
    }


    protected Uri insertTempFeature(Feature feature)
    {
        Uri uri = Uri.parse("content://" + mAuthority + "/" + mPath.getName());

        uri = uri.buildUpon()
                .appendQueryParameter(URI_PARAMETER_TEMP, Boolean.TRUE.toString())
                .build();

        Uri result = insert(uri, feature.getContentValues(false));

        if (result == null) {
            Log.d(TAG, "insert feature failed");
            return null;
        }

        return result;
    }


    protected Uri insertTempAttach(
            long featureId,
            AttachItem attachItem)
    {
        Uri uri = Uri.parse("content://" + mAuthority + "/" + mPath.getName() +
                "/" + featureId + "/" + Constants.URI_ATTACH);

        uri = uri.buildUpon()
                .appendQueryParameter(URI_PARAMETER_TEMP, Boolean.TRUE.toString())
                .build();

        Uri result = insert(uri, attachItem.getContentValues(false));

        if (result == null) {
            Log.d(TAG, "insert attach failed");
            return null;
        }

        return result;
    }


    public int updateFeatureWithFlags(Feature feature)
    {
        boolean tempFlag = hasFeatureTempFlag(feature.getId());
        boolean notSyncFlag = hasFeatureNotSyncFlag(feature.getId());

        if (!tempFlag && !notSyncFlag) {
            return 0;
        }

        String layerPathName = mPath.getName();

        Uri uri =
                Uri.parse("content://" + mAuthority + "/" + layerPathName + "/" + feature.getId());

        if (tempFlag) {
            uri = uri.buildUpon()
                    .appendQueryParameter(URI_PARAMETER_TEMP, Boolean.TRUE.toString())
                    .build();
        }

        if (notSyncFlag) {
            uri = uri.buildUpon()
                    .appendQueryParameter(URI_PARAMETER_NOT_SYNC, Boolean.TRUE.toString())
                    .build();
        }

        return update(uri, feature.getContentValues(false), null, null);
    }


    public int updateAttachWithFlags(
            Feature feature,
            AttachItem attachItem)
    {
        String layerPathName = mPath.getName();
        long featureIdL = feature.getId();
        long attachIdL = Long.parseLong(attachItem.getAttachId());

        boolean tempFlag = hasAttachTempFlag(featureIdL, attachIdL);
        boolean notSyncFlag = hasAttachNotSyncFlag(featureIdL, attachIdL);

        if (!tempFlag && !notSyncFlag) {
            return 0;
        }

        Uri uri = Uri.parse("content://" + mAuthority + "/" + layerPathName + "/" + featureIdL + "/"
                + Constants.URI_ATTACH + "/" + attachIdL);

        if (tempFlag) {
            uri = uri.buildUpon()
                    .appendQueryParameter(URI_PARAMETER_TEMP, Boolean.TRUE.toString())
                    .build();
        }

        if (notSyncFlag) {
            uri = uri.buildUpon()
                    .appendQueryParameter(URI_PARAMETER_NOT_SYNC, Boolean.TRUE.toString())
                    .build();
        }

        return update(uri, attachItem.getContentValues(false), null, null);
    }


    public int updateFeatureWithAttachesWithFlags(Feature feature)
    {
        int res = updateFeatureWithFlags(feature);

        long featureIdL = feature.getId();

        Map<String, AttachItem> attaches = getAttachMap("" + featureIdL);
        if (null != attaches) {
            for (AttachItem attachItem : attaches.values()) {
                res += updateAttachWithFlags(feature, attachItem);
            }
        }

        return res;
    }


    public int deleteAttachWithFlags(
            long featureId,
            long attachId)
    {
        boolean tempFlag = hasAttachTempFlag(featureId, attachId);
        boolean notSyncFlag = hasAttachNotSyncFlag(featureId, attachId);

        if (!tempFlag && !notSyncFlag) {
            return 0;
        }

        Uri uri = Uri.parse(
                "content://" + mAuthority + "/" + mPath.getName() + "/" + featureId + "/"
                        + Constants.URI_ATTACH + "/" + attachId);

        if (tempFlag) {
            uri = uri.buildUpon()
                    .appendQueryParameter(URI_PARAMETER_TEMP, Boolean.FALSE.toString())
                    .build();
        }

        if (notSyncFlag) {
            uri = uri.buildUpon()
                    .appendQueryParameter(URI_PARAMETER_NOT_SYNC, Boolean.FALSE.toString())
                    .build();
        }

        return delete(uri, null, null);
    }


    public void deleteAllTempFeatures()
    {
        String layerPathName = mPath.getName();

        while (true) {
            Cursor cursor = queryFirstTempFeatureFlags();
            Long featureId;

            if (null != cursor) {
                int featureIdColumn = cursor.getColumnIndex(Constants.FIELD_FEATURE_ID);
                featureId = cursor.getLong(featureIdColumn);
                cursor.close();
            } else {
                break;
            }

            // delete all feature's attaches
            Uri uri = Uri.parse(
                    "content://" + mAuthority + "/" + layerPathName + "/" + featureId + "/"
                            + URI_ATTACH);
            uri = uri.buildUpon()
                    .appendQueryParameter(URI_PARAMETER_TEMP, Boolean.FALSE.toString())
                    .build();
            delete(uri, null, null);

            // delete feature
            uri = Uri.parse("content://" + mAuthority + "/" + layerPathName + "/" + featureId);
            uri = uri.buildUpon()
                    .appendQueryParameter(URI_PARAMETER_TEMP, Boolean.FALSE.toString())
                    .build();
            delete(uri, null, null);
        }
    }


    public void deleteAllFeatures(IProgressor progressor)
    {
        String layerPathName = mPath.getName();
        List<Long> ids = query(null);
        if (progressor != null)
            progressor.setMax(ids.size());
        int c = 0;

        for (Long id : ids) {
            if (progressor != null) {
                progressor.setValue(c++);

                if (progressor.isCanceled())
                    break;
            }

            // delete all feature's attaches
            Uri uri = Uri.parse("content://" + mAuthority + "/" + layerPathName + "/" + id + "/" + URI_ATTACH);
            uri = uri.buildUpon().appendQueryParameter(URI_PARAMETER_TEMP, Boolean.FALSE.toString()).build();
            delete(uri, null, null);

            // delete feature
            uri = Uri.parse("content://" + mAuthority + "/" + layerPathName + "/" + id);
            uri = uri.buildUpon().appendQueryParameter(URI_PARAMETER_TEMP, Boolean.FALSE.toString()).build();
            delete(uri, null, null);
        }
    }


    public void deleteAllTempAttaches()
    {
        String layerPathName = mPath.getName();

        while (true) {
            Cursor cursor = queryFirstTempAttachFlags();
            Long featureId, attachId;

            if (null != cursor) {
                int featureIdColumn = cursor.getColumnIndex(Constants.FIELD_FEATURE_ID);
                int attachIdColumn = cursor.getColumnIndex(Constants.FIELD_ATTACH_ID);
                featureId = cursor.getLong(featureIdColumn);
                attachId = cursor.getLong(attachIdColumn);
                cursor.close();
            } else {
                break;
            }

            Uri uri = Uri.parse(
                    "content://" + mAuthority + "/" + layerPathName + "/" + featureId + "/"
                            + Constants.URI_ATTACH + "/" + attachId);
            uri = uri.buildUpon()
                    .appendQueryParameter(URI_PARAMETER_TEMP, Boolean.FALSE.toString())
                    .build();
            delete(uri, null, null);
        }
    }


    public void deleteAllTemps()
    {
        deleteAllTempAttaches();
        deleteAllTempFeatures();

        deleteAllTempAttachesFlags();
        deleteAllTempFeaturesFlags();
    }


    public int deleteAllTempFeaturesFlags()
    {
        // TODO: move work with temp features into VectorLayer
        return 0;
    }


    public int deleteAllTempAttachesFlags()
    {
        // TODO: move work with temp features into VectorLayer
        return 0;
    }


    public boolean hasFeatureTempFlag(long featureId)
    {
        // TODO: move work with temp features into VectorLayer
        return false;
    }


    public boolean hasFeatureNotSyncFlag(long featureId)
    {
        return false;
    }


    public boolean hasAttachTempFlag(
            long featureId,
            long attachId)
    {
        // TODO: move work with temp features into VectorLayer
        return false;
    }


    public boolean hasAttachNotSyncFlag(
            long featureId,
            long attachId)
    {
        return false;
    }


    public long setFeatureTempFlag(
            long featureId,
            boolean flag)
    {
        // TODO: move work with temp features into VectorLayer
        return 0;
    }


    public long setFeatureNotSyncFlag(
            long featureId,
            boolean flag)
    {
        return 0;
    }


    public long setAttachTempFlag(
            long featureId,
            long attachId,
            boolean flag)
    {
        // TODO: move work with temp features into VectorLayer
        return 0;
    }


    public long setAttachNotSyncFlag(
            long featureId,
            long attachId,
            boolean flag)
    {
        return 0;
    }


    public long setAttachesTempFlag(
            long featureId,
            boolean flag)
    {
        Map<String, AttachItem> attachMap = getAttachMap("" + featureId);
        if (null == attachMap) {
            return 0;
        }

        Set<String> attachIds = attachMap.keySet();
        long res = 0;

        for (String attachId : attachIds) {
            res += setAttachTempFlag(featureId, Long.parseLong(attachId), flag);
        }

        return res;
    }


    public long setAttachesNotSyncFlag(
            long featureId,
            boolean flag)
    {
        Map<String, AttachItem> attachMap = getAttachMap("" + featureId);
        if (null == attachMap) {
            return 0;
        }

        Set<String> attachIds = attachMap.keySet();
        long res = 0;

        for (String attachId : attachIds) {
            res += setAttachNotSyncFlag(featureId, Long.parseLong(attachId), flag);
        }

        return res;
    }


    public long setFeatureWithAttachesTempFlag(
            Feature feature,
            boolean flag)
    {
        return setFeatureWithAttachesTempFlag(feature.getId(), flag);
    }


    public long setFeatureWithAttachesTempFlag(
            long featureId,
            boolean flag)
    {
        return setFeatureTempFlag(featureId, flag) + setAttachesTempFlag(featureId, flag);
    }


    public long setFeatureWithAttachesNotSyncFlag(
            Feature feature,
            boolean flag)
    {
        return setFeatureWithAttachesNotSyncFlag(feature.getId(), flag);
    }


    public long setFeatureWithAttachesNotSyncFlag(
            long featureId,
            boolean flag)
    {
        return setFeatureNotSyncFlag(featureId, flag) + setAttachesNotSyncFlag(featureId, flag);
    }


    public SharedPreferences getPreferences()
    {
        return mContext.getSharedPreferences(getPath().getName(), Context.MODE_PRIVATE);
    }


    public void setLocked(boolean state)
    {
        mIsLocked = state;
    }


    public boolean isLocked()
    {
        return mIsLocked;
    }

    public void toNGW(Long id, String account, int syncType, Pair<Integer, Integer> ver) {
        if (id != null && id != NOT_FOUND) {
            mLayerType = Constants.LAYERTYPE_NGW_VECTOR;
            try {
                JSONObject rootConfig = toJSON();
                if (ver != null) {
                    rootConfig.put(NGWVectorLayer.JSON_NGW_VERSION_MAJOR_KEY, ver.first);
                    rootConfig.put(NGWVectorLayer.JSON_NGW_VERSION_MINOR_KEY, ver.second);
                }

                rootConfig.put(NGWVectorLayer.JSON_ACCOUNT_KEY, account);
                rootConfig.put(Constants.JSON_ID_KEY, id);
                rootConfig.put(NGWVectorLayer.JSON_SYNC_TYPE_KEY, syncType);
                rootConfig.put(NGWVectorLayer.JSON_NGWLAYER_TYPE_KEY, Connection.NGWResourceTypeVectorLayer);
                FileUtil.writeToFile(getFileName(), rootConfig.toString());
                MapBase map = MapDrawable.getInstance();
                map.load();
                new Sync().execute();
            } catch (IOException | JSONException ignored) { }
        }
    }

    class Sync extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                NGWVectorLayer layer = (NGWVectorLayer) MapDrawable.getInstance().getLayerByPathName(getPath().getName());
                FeatureChanges.initialize(layer.getChangeTableName());
                List<Long> ids = query(null);
                for (Long id : ids) {
                    Feature feature = getFeatureWithAttaches(id);
                    layer.addChange(feature.getId(), CHANGE_OPERATION_NEW);
                    Map<String, AttachItem> attaches = feature.getAttachments();
                    for (AttachItem attach : attaches.values())
                        layer.addChange(feature.getId(), Long.parseLong(attach.getAttachId()), CHANGE_OPERATION_NEW);
                }

                Pair<Integer, Integer> ver = NGWUtil.getNgwVersion(mContext, layer.getAccountName());
                layer.sync(mAuthority, ver, new SyncResult());
            } catch (Exception ignored) { }

            return null;
        }
    }
}
