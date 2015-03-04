/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

package com.nextgis.maplib.display;

import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.datasource.GeoGeometry;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.Constants.JSON_COLOR_KEY;


public abstract class Style implements IJSONStore
{
    protected int mColor;


    public Style()
    {

    }


    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor = color;
    }

    public Style(final int color){
        mColor = color;
    }

    public abstract void onDraw(GeoGeometry geoGeometry, GISDisplay display);

    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = new JSONObject();
        rootConfig.put(JSON_COLOR_KEY, mColor);
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        mColor = jsonObject.getInt(JSON_COLOR_KEY);
    }
}
