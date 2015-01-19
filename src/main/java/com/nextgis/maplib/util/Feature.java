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

package com.nextgis.maplib.util;

import android.util.Pair;
import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.*;

public class Feature implements IJSONStore
{
    protected int mId;
    protected GeoGeometry  mGeometry;
    protected List<Object> mFieldValues;
    protected List<Pair<String, Integer>> mFieldKeys;

    public Feature(int id, List<Pair<String, Integer>> fieldKeys) {
        mId = id;
        mFieldKeys = fieldKeys;
        mFieldValues = new ArrayList<>(fieldKeys.size());
    }

    public int getId() {
        return mId;
    }


    public void setId(int id)
    {
        mId = id;
    }

    public void setGeometry(GeoGeometry geometry){
        mGeometry = geometry;
    }

    public GeoGeometry getGeometry(){
        return mGeometry;
    }

    public boolean setFieldValue(int index, Object value){
        if(index < 0 || index >= mFieldKeys.size())
            return false;
        if(mFieldValues.size() <= index){
            for(int i = mFieldValues.size(); i <= index; i++){
                mFieldValues.add(null);
            }
        }
        mFieldValues.set(index, value);
        return true;
    }

    public boolean setFieldValue(String fieldName, Object value){
        int index = getFieldValueIndex(fieldName);
        return setFieldValue(index, value);
    }

    public boolean isValuePresent(int index)
    {
        return getFieldValue(index) != null;
    }

    public Object getFieldValue(int index) {
        if (index < 0 || index >= mFieldKeys.size() || index >= mFieldValues.size())
            return null;
        return mFieldValues.get(index);
    }

    public Object getFieldValue(String fieldName){
        int index = getFieldValueIndex(fieldName);
        return getFieldValue(index);
    }

    public int getFieldValueIndex(String fieldName){
        for(int i = 0; i < mFieldKeys.size(); i++){
            if(mFieldKeys.get(i).first.equals(fieldName))
                return i;
        }
        return NOT_FOUND;
    }

    public String getFieldValueAsString(int index) {
        if (index < 0 || index >= mFieldKeys.size() || index >= mFieldValues.size())
            return null;
        Object val = mFieldValues.get(index);
        if(null == val)
            return "";
        switch (mFieldKeys.get(index).second)
        {
            case FTString:
            case FTReal:
            case FTInteger:
                return val.toString();
            case FTDateTime:
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                return dateFormat.format((Date)val);
        }
        return "";
    }


    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject oJSONOut = new JSONObject();
        oJSONOut.put(GEOJSON_TYPE, GEOJSON_TYPE_Feature);
        oJSONOut.put(GEOJSON_FEATURE_ID, mId);
        oJSONOut.put(GEOJSON_GEOMETRY, mGeometry.toJSON());
        JSONObject oJSONProp = new JSONObject();
        for(int i = 0; i < mFieldValues.size(); i++){
            String key = mFieldKeys.get(i).first;
            oJSONProp.put(key, mFieldValues.get(i));
        }
        oJSONOut.put(GEOJSON_PROPERTIES, oJSONProp);
        return oJSONOut;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        if(!jsonObject.getString(GEOJSON_TYPE).equals(GEOJSON_TYPE_Feature))
            throw new JSONException("not valid geojson feature");
        mId = jsonObject.getInt(GEOJSON_FEATURE_ID);
        JSONObject oJSONGeom = jsonObject.getJSONObject(GEOJSON_GEOMETRY);
        mGeometry = GeoGeometryFactory.fromJson(oJSONGeom);
        JSONObject jsonAttributes = jsonObject.getJSONObject(GEOJSON_PROPERTIES);
        Iterator<String> iter = jsonAttributes.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            Object value = jsonAttributes.get(key);
            mFieldValues.add(value);
        }
    }
}
