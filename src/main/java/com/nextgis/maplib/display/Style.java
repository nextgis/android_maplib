/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015, 2017 NextGIS, info@nextgis.com
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

package com.nextgis.maplib.display;

import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.datasource.GeoGeometry;

import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.Constants.JSON_ALPHA_KEY;
import static com.nextgis.maplib.util.Constants.JSON_COLOR_KEY;
import static com.nextgis.maplib.util.Constants.JSON_OUTALPHA_KEY;
import static com.nextgis.maplib.util.Constants.JSON_OUTCOLOR_KEY;

public abstract class Style implements IJSONStore, Cloneable {
    protected int mColor;
    protected int mOutColor;
    protected int mOuterAlpha = 255;
    protected int mInnerAlpha = 255;

    public Style() {

    }

    public Style clone() throws CloneNotSupportedException {
        Style obj = (Style) super.clone();
        obj.mColor = mColor;
        obj.mOutColor = mOutColor;
        obj.mInnerAlpha = mInnerAlpha;
        obj.mOuterAlpha = mOuterAlpha;
        return obj;
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor = color;
    }

    public int getOutColor() {
        return mOutColor;
    }

    public void setOutColor(int outColor) {
        mOutColor = outColor;
    }

    public Style(final int color, int outColor) {
        mColor = color;
        mOutColor = outColor;
    }

    public abstract void onDraw(GeoGeometry geoGeometry, GISDisplay display);

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject rootConfig = new JSONObject();
        rootConfig.put(JSON_COLOR_KEY, mColor);
        rootConfig.put(JSON_OUTCOLOR_KEY, mOutColor);
        rootConfig.put(JSON_ALPHA_KEY, mInnerAlpha);
        rootConfig.put(JSON_OUTALPHA_KEY, mOuterAlpha);
        return rootConfig;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        mColor = jsonObject.getInt(JSON_COLOR_KEY);
        mOutColor = jsonObject.getInt(JSON_OUTCOLOR_KEY);
        mInnerAlpha = jsonObject.getInt(JSON_ALPHA_KEY);
        mOuterAlpha = jsonObject.getInt(JSON_OUTALPHA_KEY);
    }

    public int getAlpha() {
        return mInnerAlpha;
    }

    public void setAlpha(int alpha) {
        mInnerAlpha = alpha;
    }

    public int getOutAlpha() {
        return mOuterAlpha;
    }

    public void setOutAlpha(int alpha) {
        mOuterAlpha = alpha;
    }
}
