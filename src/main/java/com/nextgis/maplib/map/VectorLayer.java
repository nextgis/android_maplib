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


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.Log;
import android.util.Pair;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.datasource.Geo;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.SimpleLineStyle;
import com.nextgis.maplib.display.SimpleMarkerStyle;
import com.nextgis.maplib.util.Feature;
import com.nextgis.maplib.util.VectorCacheItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.*;

public class VectorLayer extends Layer
{
    protected boolean mIsInitialized;
    protected int mGeometryType;
    protected List<VectorCacheItem> mVectorCacheItems;

    protected static final String JSON_GEOMETRY_TYPE_KEY   = "geometry_type";
    protected static final String JSON_IS_INITIALIZED_KEY   = "is_inited";

    protected static final String ID_FIELD   = "_ID";
    protected static final String GEOM_FIELD   = "_GEOM";

    protected int mLastId;

    public VectorLayer(
            Context context,
            File path)
    {
        super(context, path);

        mIsInitialized = false;
        mGeometryType = GTNone;

        mLastId = 0;
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
                    return  mContext.getString(R.string.error_crs_unsuported);
                }
                JSONObject crsPropertiesJSONObject =
                        crsJSONObject.getJSONObject(GEOJSON_PROPERTIES);
                String crsName = crsPropertiesJSONObject.getString(GEOJSON_NAME);
                if (crsName.equals("urn:ogc:def:crs:OGC:1.3:CRS84")) { // WGS84
                    isWGS84 = true;
                } else if (crsName.equals("urn:ogc:def:crs:EPSG::3857")
                           || crsName.equals("EPSG:3857")) { //Web Mercator
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
            List<Pair<String, Integer>> fields = new ArrayList<>();

            int geometryType = GTNone;
            for (int i = 0; i < geoJSONFeatures.length(); i++) {
                JSONObject jsonFeature = geoJSONFeatures.getJSONObject(i);
                //get geometry
                JSONObject jsonGeometry = jsonFeature.getJSONObject(GEOJSON_GEOMETRY);
                GeoGeometry geometry = GeoGeometry.fromJson(jsonGeometry);
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
                            if (fields.get(j).first.equals(key)) {
                                fieldIndex = j;
                            }
                        }
                        if (fieldIndex == NOT_FOUND) { //add new field
                            Pair<String, Integer> fieldKey = Pair.create(key, nType);
                            fieldIndex = fields.size();
                            fields.add(fieldKey);
                        }
                        feature.setFieldValue(fieldIndex, value);
                    }
                }
                features.add(feature);
            }

            String tableCreate = "CREATE TABLE IF NOT EXISTS " + mPath.getName() + " ( " + //table name is the same as the folder of the layer
                                 ID_FIELD + " INTEGER PRIMARY KEY, " +
                                 GEOM_FIELD + " BLOB";
            for(int i = 0; i < fields.size(); ++i)
            {
                Pair<String, Integer> field = fields.get(i);

                tableCreate += ", " + field.first + " ";
                switch (field.second)
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

            GeoEnvelope extents = new GeoEnvelope();
            for (Feature feature : features) {
                //update bbox
                extents.merge(feature.getGeometry().getEnvelope());
            }

            //1. create table and populate with values
            MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
            SQLiteDatabase db = map.getDatabase(true);
            db.execSQL(tableCreate);
            for(Feature feature : features) {
                ContentValues values = new ContentValues();
                values.put(ID_FIELD, feature.getId());
                try {
                    values.put(GEOM_FIELD, feature.getGeometry().toBlob());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for(int i = 0; i < fields.size(); ++i){
                    if(!feature.isValuePresent(i))
                        continue;
                    switch (fields.get(i).second)
                    {
                        case FTString:
                            values.put(fields.get(i).first, feature.getFieldValueAsString(i));
                            break;
                        case FTInteger:
                            values.put(fields.get(i).first, (int)feature.getFieldValue(i));
                            break;
                        case FTReal:
                            values.put(fields.get(i).first, (double)feature.getFieldValue(i));
                            break;
                        case FTDateTime:
                            values.put(fields.get(i).first, feature.getFieldValueAsString(i));
                            break;
                    }
                }
                db.insert(mPath.getName(), "", values);
            }

            //2. save the layer properties to config.json
            mGeometryType = geometryType;
            mExtents = extents;
            mIsInitialized = true;
            setDefaultRenderer();

            save();

            //3. fill the geometry and labels array
            mVectorCacheItems = new ArrayList<>();
            for(Feature feature : features){
                mVectorCacheItems.add(new VectorCacheItem(feature.getGeometry(), feature.getId()));
            }


            if(null != mParent){ //notify the load is over
                LayerGroup layerGroup = (LayerGroup)mParent;
                layerGroup.onLayerChanged(this);
            }

            return "";
        } catch (JSONException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
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

            //load vector cache
            MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
            SQLiteDatabase db = map.getDatabase(false);
            String[] columns = new String[]{ID_FIELD, GEOM_FIELD};
            Cursor cursor = db.query(mPath.getName(), columns, null, null, null, null, null);
            if(null != cursor && cursor.moveToFirst()) {
                mVectorCacheItems = new ArrayList<>();
                do{
                    try {
                        GeoGeometry geoGeometry = GeoGeometry.fromBlob(cursor.getBlob(1));
                        int nId = cursor.getInt(0);

                        if(mLastId <= nId)
                            mLastId = nId + 1;

                        mExtents.merge(geoGeometry.getEnvelope());
                        mVectorCacheItems.add(new VectorCacheItem(geoGeometry, nId));
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                while (cursor.moveToNext());
            }
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
}
