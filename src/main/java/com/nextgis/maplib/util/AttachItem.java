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

import com.nextgis.maplib.api.IJSONStore;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Class describes attach item
 */
public class AttachItem implements IJSONStore
{
    String mDescription;
    String mDisplayName;
    String mMimetype;
    String mAttachId;

    protected static final String JSON_ID_KEY           = "id";
    protected static final String JSON_DESCRIPTION_KEY  = "desc";
    protected static final String JSON_MIME_KEY         = "mime";
    protected static final String JSON_DISPLAY_NAME_KEY = "display_name";


    public AttachItem()
    {
    }

    public AttachItem(
            String attachId,
            String displayName,
            String mimetype,
            String description
            )
    {
        mDescription = description;
        mDisplayName = displayName;
        mMimetype = mimetype;
        mAttachId = attachId;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject out = new JSONObject();
        out.put(JSON_ID_KEY, mAttachId);
        out.put(JSON_DESCRIPTION_KEY, mDescription);
        out.put(JSON_MIME_KEY, mMimetype);
        out.put(JSON_DISPLAY_NAME_KEY, mDisplayName);
        return out;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        mAttachId = jsonObject.getString(JSON_ID_KEY);
        mDisplayName = jsonObject.getString(JSON_DISPLAY_NAME_KEY);
        mMimetype = jsonObject.getString(JSON_MIME_KEY);
        mDescription = jsonObject.getString(JSON_DESCRIPTION_KEY);
    }


    public String getDescription()
    {
        return mDescription;
    }


    public String getDisplayName()
    {
        return mDisplayName;
    }


    public String getMimetype()
    {
        return mMimetype;
    }


    public String getAttachId()
    {
        return mAttachId;
    }


    public void setDescription(String description)
    {
        mDescription = description;
    }


    public void setDisplayName(String displayName)
    {
        mDisplayName = displayName;
    }


    public void setMimetype(String mimetype)
    {
        mMimetype = mimetype;
    }


    public void setAttachId(String attachId)
    {
        mAttachId = attachId;
    }
}