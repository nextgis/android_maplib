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

import java.util.ArrayList;
import java.util.List;


/**
 * A class to store changes in feature of vector layer
 */
public class ChangeFeatureItem implements IJSONStore
{
    public static final int TYPE_NEW = 1;
    public static final int TYPE_CHANGED = 2;
    public static final int TYPE_DELETE = 3;
    public static final int TYPE_PHOTO_NEW = 11;
    public static final int TYPE_PHOTO_DELETE = 31;

    protected int mFeatureId;
    protected int mOperation;
    protected List<ChangePhotoItem> mPhotoItems;


    public ChangeFeatureItem(
            int featureId,
            int operation)
    {
        mFeatureId = featureId;
        mOperation = operation;

        mPhotoItems = new ArrayList<>();
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        return null;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {

    }

    protected class ChangePhotoItem implements IJSONStore
    {
        protected String mName;

        @Override
        public JSONObject toJSON()
                throws JSONException
        {
            return null;
        }


        @Override
        public void fromJSON(JSONObject jsonObject)
                throws JSONException
        {

        }
    }
}
