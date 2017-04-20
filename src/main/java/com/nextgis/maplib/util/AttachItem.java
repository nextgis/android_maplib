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

package com.nextgis.maplib.util;

import android.content.ContentValues;

import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.map.VectorLayer;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class describes attach item
 */
public class AttachItem implements IJSONStore {
    String mDescription;
    String mDisplayName;
    String mMimetype;
    String mAttachId;
    int mSize;

    protected static final String JSON_ID_KEY = "id";
    protected static final String JSON_DESCRIPTION_KEY = "desc";
    protected static final String JSON_MIME_KEY = "mime";
    protected static final String JSON_DISPLAY_NAME_KEY = "display_name";
    protected static final String JSON_SIZE_KEY = Constants.JSON_SIZE_KEY;

    public AttachItem() {
    }

    public AttachItem(String attachId, String displayName, String mimetype, String description) {
        mDescription = description;
        mDisplayName = displayName;
        mMimetype = mimetype;
        mAttachId = attachId;
    }

    public AttachItem(String attachId, String displayName, String mimetype, String description, int size) {
        mDescription = description;
        mDisplayName = displayName;
        mMimetype = mimetype;
        mAttachId = attachId;
        mSize = size;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject out = new JSONObject();
        out.put(JSON_ID_KEY, mAttachId);
        out.put(JSON_DESCRIPTION_KEY, mDescription);
        out.put(JSON_MIME_KEY, mMimetype);
        out.put(JSON_DISPLAY_NAME_KEY, mDisplayName);
        out.put(JSON_SIZE_KEY, mSize);
        return out;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        mAttachId = jsonObject.getString(JSON_ID_KEY);
        mDisplayName = jsonObject.getString(JSON_DISPLAY_NAME_KEY);
        mMimetype = jsonObject.getString(JSON_MIME_KEY);
        mDescription = jsonObject.getString(JSON_DESCRIPTION_KEY);
        mSize = jsonObject.optInt(JSON_SIZE_KEY);
    }

    public String getDescription() {
        return mDescription;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getMimetype() {
        return mMimetype;
    }

    public String getAttachId() {
        return mAttachId;
    }

    public int getSize() {
        return mSize;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public void setMimetype(String mimetype) {
        mMimetype = mimetype;
    }

    public void setAttachId(String attachId) {
        mAttachId = attachId;
    }

    public void setSize(int size) {
        mSize = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AttachItem that = (AttachItem) o;

        return that.mSize == mSize && mAttachId.equals(that.mAttachId) && !(mDescription != null ? !mDescription
                .equals(that.mDescription) : that.mDescription != null) && !(mDisplayName != null ? !mDisplayName
                .equals(that.mDisplayName) : that.mDisplayName != null) && !(mMimetype != null ? !mMimetype.equals(that.mMimetype) : that.mMimetype != null);

    }

    @Override
    public int hashCode() {
        return mAttachId.hashCode();
    }

    public ContentValues getContentValues(boolean withAttachId) {
        ContentValues returnValues = new ContentValues();
        returnValues.put(VectorLayer.ATTACH_DISPLAY_NAME, mDisplayName);
        returnValues.put(VectorLayer.ATTACH_MIME_TYPE, mMimetype);
        returnValues.put(VectorLayer.ATTACH_DESCRIPTION, mDescription);
        returnValues.put(VectorLayer.ATTACH_SIZE, mSize);

        if (withAttachId) {
            returnValues.put(VectorLayer.ATTACH_ID, mAttachId);
        }

        return returnValues;
    }
}
