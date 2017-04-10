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

package com.nextgis.maplib.datasource;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class to describe feature field
 */
public class Field implements IJSONStore, Parcelable {
    protected int mType;
    protected String mName;
    protected String mAlias;

    protected static final String JSON_ALIAS_KEY = "alias";

    public Field() { }

    public Field(int type, String name, String alias) {
        mType = type;
        mName = name;
        if (TextUtils.isEmpty(alias)) {
            mAlias = mName;
        } else {
            mAlias = alias;
        }
    }

    private Field(Parcel in) {
        this();
        mType = in.readInt();
        mName = in.readString();
        mAlias = in.readString();
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject rootObject = new JSONObject();
        rootObject.put(Constants.JSON_TYPE_KEY, mType);
        rootObject.put(Constants.JSON_NAME_KEY, mName);
        rootObject.put(JSON_ALIAS_KEY, mAlias);
        return rootObject;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        mType = jsonObject.getInt(Constants.JSON_TYPE_KEY);
        mName = jsonObject.getString(Constants.JSON_NAME_KEY);
        if (jsonObject.has(JSON_ALIAS_KEY)) {
            mAlias = jsonObject.getString(JSON_ALIAS_KEY);
        }
    }

    public int getType() {
        return mType;
    }

    public String getName() {
        return mName;
    }

    public String getAlias() {
        return mAlias;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setAlias(String alias) {
        mAlias = alias;
    }

    public static final Creator<Field> CREATOR = new Creator<Field>() {
        @Override
        public Field createFromParcel(Parcel in) {
            return new Field(in);
        }

        @Override
        public Field[] newArray(int size) {
            return new Field[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mType);
        parcel.writeString(mName);
        parcel.writeString(mAlias);
    }
}
