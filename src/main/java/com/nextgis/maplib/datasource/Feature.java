/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016, 2018 NextGIS, info@nextgis.com
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

package com.nextgis.maplib.datasource;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.util.AttachItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.*;


public class Feature
        implements IJSONStore
{
    protected long                    mId;
    protected GeoGeometry             mGeometry;
    protected List<Object>            mFieldValues;
    protected List<Field>             mFields;
    protected Map<String, AttachItem> mAttachments;


    public Feature()
    {
        mId = Constants.NOT_FOUND;
        mFields = new ArrayList<>();
        mFieldValues = new ArrayList<>();
        mAttachments = new HashMap<>();
    }


    public Feature(
            long id,
            List<Field> fields)
    {
        mId = id;
        mFields = fields;
        mFieldValues = new ArrayList<>(fields.size());
        mAttachments = new HashMap<>();
    }


    public Feature(Feature other)
    {
        mId = other.getId();
        mGeometry = other.getGeometry();
        mFields = other.getFields();
        mFieldValues = other.getFieldValues();
        mAttachments = other.getAttachments();
    }


    public long getId()
    {
        return mId;
    }


    public void setId(long id)
    {
        mId = id;
    }


    public void setGeometry(GeoGeometry geometry)
    {
        mGeometry = geometry;
    }


    public GeoGeometry getGeometry()
    {
        return mGeometry;
    }


    public boolean setFieldValue(
            int index,
            Object value)
    {
        if (index < 0 || index >= mFields.size()) {
            return false;
        }
        if (mFieldValues.size() <= index) {
            for (int i = mFieldValues.size(); i <= index; i++) {
                mFieldValues.add(null);
            }
        }
        mFieldValues.set(index, value);
        return true;
    }


    public boolean setFieldValue(
            String fieldName,
            Object value)
    {
        int index = getFieldValueIndex(fieldName);
        return setFieldValue(index, value);
    }


    public boolean isValuePresent(int index)
    {
        return getFieldValue(index) != null;
    }


    public Object getFieldValue(int index)
    {
        if (mFields.isEmpty() || mFieldValues.isEmpty() || index < 0 || index >= mFields.size() ||
                index >= mFieldValues.size()) {
            return null;
        }
        return mFieldValues.get(index);
    }


    public Object getFieldValue(String fieldName)
    {
        int index = getFieldValueIndex(fieldName);
        return getFieldValue(index);
    }


    public int getFieldValueIndex(String fieldName)
    {
        for (int i = 0; i < mFields.size(); i++) {
            if (mFields.get(i).getName().equals(fieldName)) {
                return i;
            }
        }
        return NOT_FOUND;
    }


    public int getFieldValueAsInteger(String fieldName)
    {
        int index = getFieldValueIndex(fieldName);
        return getFieldValueAsInteger(index);
    }


    public int getFieldValueAsInteger(int index)
    {
        if (mFields.isEmpty() || mFieldValues.isEmpty() || index < 0 || index >= mFields.size() ||
                index >= mFieldValues.size()) {
            return Integer.MAX_VALUE;
        }

        Object val = mFieldValues.get(index);
        if (null == val) {
            return Integer.MAX_VALUE;
        }

        if (val instanceof Long) {
            Long lval = (Long) val;
            return lval.intValue();
        } else if (val instanceof Integer) {
            return (int) val;
        } else {
            return Integer.MAX_VALUE;
        }
    }


    public String getFieldValueAsString(String fieldName)
    {
        int index = getFieldValueIndex(fieldName);
        return getFieldValueAsString(index);
    }


    public String getFieldValueAsString(int index)
    {
        if (mFields.isEmpty() || mFieldValues.isEmpty() || index < 0 || index >= mFields.size() ||
                index >= mFieldValues.size()) {
            return "";
        }
        Object val = mFieldValues.get(index);
        if (null == val) {
            return "";
        }
        switch (mFields.get(index).getType()) {
            case FTString:
            case FTReal:
            case FTInteger:
                return val.toString();
            case FTDate:
                if (val instanceof Long) {
                    DateFormat dateFormat = DateFormat.getDateInstance();
                    return dateFormat.format(new Date((Long) val));
                }
                break;
            case FTTime:
                if (val instanceof Long) {
                    DateFormat dateFormat = DateFormat.getTimeInstance();
                    return dateFormat.format(new Date((Long) val));
                }
                break;
            case FTDateTime:
                if (val instanceof Long) {
                    DateFormat dateFormat = DateFormat.getDateTimeInstance();
                    return dateFormat.format(new Date((Long) val));
                }
                break;
        }
        return "";
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject oJSONOut = new JSONObject();
        oJSONOut.put(GEOJSON_TYPE, GEOJSON_TYPE_Feature);
        oJSONOut.put(GEOJSON_FEATURE_ID, mId);
        oJSONOut.put(GEOJSON_GEOMETRY, mGeometry.toJSON());
        JSONObject oJSONProp = new JSONObject();
        for (int i = 0; i < mFieldValues.size(); i++) {
            String key = mFields.get(i).getName();
            oJSONProp.put(key, mFieldValues.get(i));
        }
        oJSONOut.put(GEOJSON_PROPERTIES, oJSONProp);
        return oJSONOut;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        if (!jsonObject.getString(GEOJSON_TYPE).equals(GEOJSON_TYPE_Feature)) {
            throw new JSONException("not valid geojson feature");
        }
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


    public List<Field> getFields()
    {
        return mFields;
    }


    public List<Object> getFieldValues()
    {
        return mFieldValues;
    }


    public ContentValues getContentValues(boolean withId)
    {
        ContentValues values = new ContentValues();
        if (withId) {
            if (mId != Constants.NOT_FOUND) {
                values.put(Constants.FIELD_ID, mId);
            } else {
                values.putNull(Constants.FIELD_ID);
            }
        }

        try {
            if (null != mGeometry) {
                values.put(FIELD_GEOM, mGeometry.toBlob());
            }
        } catch (IOException e) { //if exception - not create geom
            e.printStackTrace();
        }
        if (!values.containsKey(FIELD_GEOM)) {
            values.putNull(FIELD_GEOM);
        }

        for (int i = 0; i < mFields.size(); i++) {
            Field field = mFields.get(i);

            if (!isValuePresent(i)) {
                values.putNull(field.getName());
                continue;
            }

            switch (field.getType()) {
                case FTString:
                    values.put(field.getName(), getFieldValueAsString(i));
                    break;

                case FTInteger:
                    Object intVal = getFieldValue(i);
                    if (intVal instanceof Integer) {
                        values.put(field.getName(), (int) intVal);
                    } else if (intVal instanceof Long) {
                        values.put(field.getName(), (long) intVal);
                    } else {
                        Log.d(TAG, "skip value: " + intVal.toString());
                    }
                    break;

                case FTReal:
                    Object realVal = getFieldValue(i);
                    if (realVal instanceof Double) {
                        values.put(field.getName(), (double) realVal);
                    } else if (realVal instanceof Float) {
                        values.put(field.getName(), (float) realVal);
                    } else {
                        Log.d(TAG, "skip value: " + realVal.toString());
                    }
                    break;

                case FTDate:
                case FTTime:
                case FTDateTime:
                    Object dateVal = getFieldValue(i);
                    if (dateVal instanceof Date) {
                        Date date = (Date) dateVal;
                        values.put(field.getName(), date.getTime());
                    } else if (dateVal instanceof Long) {
                        values.put(field.getName(), (long) dateVal);
                    } else if (dateVal instanceof Calendar) {
                        Calendar cal = (Calendar) dateVal;
                        values.put(field.getName(), cal.getTimeInMillis());
                    } else {
                        Log.d(TAG, "skip value: " + dateVal.toString());
                    }
                    break;
            }
        }

        return values;
    }


    public void fromCursor(Cursor cursor)
    {
        if (null == cursor) {
            return;
        }
        mId = cursor.getLong(cursor.getColumnIndex(FIELD_ID));

        try {
            mGeometry =
                    GeoGeometryFactory.fromBlob(cursor.getBlob(cursor.getColumnIndex(FIELD_GEOM)));
        } catch (ClassNotFoundException | IOException e) { //let it be empty geometry
            e.printStackTrace();
        }

        for (int i = 0; i < mFields.size(); i++) {
            Field field = mFields.get(i);
            int index = cursor.getColumnIndex(field.getName());
            if (cursor.isNull(index)) {
                setFieldValue(i, null);
            } else {
                if (index != NOT_FOUND) {
                    switch (field.getType()) {
                        case FTString:
                            setFieldValue(i, cursor.getString(index));
                            break;
                        case FTInteger:
                            setFieldValue(i, cursor.getLong(index));
                            break;
                        case FTReal:
                            setFieldValue(i, cursor.getDouble(index));
                            break;
                        case FTDate:
                        case FTTime:
                        case FTDateTime:
                            TimeZone timeZone = TimeZone.getDefault();
                            timeZone.setRawOffset(0); // set to UTC
                            Calendar calendar = Calendar.getInstance(timeZone);
                            calendar.setTimeInMillis(cursor.getLong(index));
                            setFieldValue(i, calendar.getTimeInMillis());
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }


    public boolean equalsData(Feature f)
    {
        if (null == f) {
            return false;
        }
        //compare attributes
        Log.d(TAG, "Feature id:" + mId + " compare attributes");
        for (int i = 0; i < mFields.size(); i++) {
            Field field = mFields.get(i);

            Object value = getFieldValue(i);
            Object valueOther = f.getFieldValue(field.getName());

            //Log.d(TAG, value + "<->" + valueOther);

            if (null == value) {
                if (null != valueOther) {
                    if (field.getType() == FTDateTime) {
                        long lValue = dateObjectToLong(valueOther);
                        if (lValue > 0) {
                            Log.d(TAG, value + "<->" + valueOther);
                            return false;
                        }
                    } else {
                        Log.d(TAG, value + "<->" + valueOther);
                        return false;
                    }
                }
            } else if (null == valueOther) {
                Log.d(TAG, value + "<->" + valueOther);
                return false;
            } else {
                if (field.getType() == GeoConstants.FTInteger) {
                    if (!checkIntegerEqual(value, valueOther)) {
                        Log.d(TAG, value + "<->" + valueOther);
                        return false;
                    }
                } else if (field.getType() == GeoConstants.FTReal) {
                    if (!checkRealEqual(value, valueOther)) {
                        Log.d(TAG, value + "<->" + valueOther);
                        return false;
                    }
                } else if (field.getType() == GeoConstants.FTDate ||
                        field.getType() == GeoConstants.FTTime ||
                        field.getType() == GeoConstants.FTDateTime) {
                    if (!checkDateEqual(value, valueOther)) {
                        Log.d(TAG, value + "<->" + valueOther);
                        return false;
                    }
                } else if (!value.equals(valueOther)) { // any other cases
                    Log.d(TAG, value + "<->" + valueOther);
                    return false;
                }
            }
        }

        //compare geometry
        Log.d(TAG, "Feature id:" + mId + " compare geometry");
        if (null == mGeometry) {
            return null == f.getGeometry();
        }
        return mGeometry.equals(f.getGeometry());
    }


    private long dateObjectToLong(Object value)
    {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Date) {
            Date date = (Date) value;
            return date.getTime();
        } else if (value instanceof Calendar) {
            Calendar cal = (Calendar) value;
            return cal.getTimeInMillis();
        }

        return Constants.NOT_FOUND;
    }


    private boolean checkDateEqual(
            Object value,
            Object valueOther)
    {
        return dateObjectToLong(value) == dateObjectToLong(valueOther);
    }


    private boolean checkRealEqual(
            Object value,
            Object valueOther)
    {
        if (value instanceof Float && valueOther instanceof Double) {
            Float vlong = (Float) value;
            Double ovlong = (Double) valueOther;
            return vlong == ovlong.floatValue();
        } else if (value instanceof Double && valueOther instanceof Float) {
            Double vlong = (Double) value;
            Float ovlong = (Float) valueOther;
            return vlong.floatValue() == ovlong;
        } else {
            return value.equals(valueOther);
        }
    }


    private boolean checkIntegerEqual(
            Object value,
            Object valueOther)
    {
        if (value instanceof Integer && valueOther instanceof Long) {
            Integer vlong = (Integer) value;
            Long ovlong = (Long) valueOther;
            return vlong == ovlong.intValue();
        } else if (value instanceof Long && valueOther instanceof Integer) {
            Long vlong = (Long) value;
            Integer ovlong = (Integer) valueOther;
            return vlong.intValue() == ovlong;
        } else {
            return value.equals(valueOther);
        }
    }


    public boolean equalsAttachments(Feature f)
    {
        if (null == f) {
            return false;
        }
        //compare attachments
        Map<String, AttachItem> attachments = f.getAttachments();
        if (mAttachments.size() != attachments.size()) {
            return false;
        }
        for (AttachItem item : mAttachments.values()) {
            AttachItem otherItem = attachments.get(item.getAttachId());
            if (null == otherItem) {
                return false;
            }
            if (!item.equals(otherItem)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean equals(Object o)
    {
        if (super.equals(o)) //if same pointers
        {
            return true;
        }

        Feature other = (Feature) o;
        // go deeper
        return equalsData(other) && equalsAttachments(other);
    }


    public void addAttachment(AttachItem item)
    {
        mAttachments.put(item.getAttachId(), item);
    }


    public void addAttachments(Map<String, AttachItem> attachments)
    {
        if (null == attachments) {
            return;
        }
        mAttachments = attachments;
    }


    public Map<String, AttachItem> getAttachments()
    {
        return mAttachments;
    }


    public void clearAttachments()
    {
        mAttachments.clear();
    }


    public long getMaxAttachId()
    {
        long maxAttachId = 0;

        if (null == mAttachments) {
            return maxAttachId;
        }

        for (String key : mAttachments.keySet()) {
            long keyL = Long.parseLong(key);
            maxAttachId = Math.max(maxAttachId, keyL);
        }

        return maxAttachId;
    }
}
