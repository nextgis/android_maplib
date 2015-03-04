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

import android.graphics.Paint;

import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;
import static com.nextgis.maplib.util.Constants.JSON_TYPE_KEY;
import static com.nextgis.maplib.util.Constants.JSON_WIDTH_KEY;
import static com.nextgis.maplib.util.GeoConstants.*;


public class SimpleMarkerStyle extends Style{
    protected int mType;
    protected float mSize;
    protected float mWidth;
    protected int mOutColor;

    public final static int MarkerStylePoint = 1;
    public final static int MarkerStyleCircle = 2;
    public final static int MarkerStyleDiamond = 3;
    public final static int MarkerStyleCross = 4;
    public final static int MarkerStyleTriangle = 5;
    public final static int MarkerStyleBox = 6;
    public final static int MarkerEditStyleCircle = 7;

    public static final String JSON_OUTCOLOR_KEY = "out_color";
    public static final String JSON_SIZE_KEY     = "size";

    public SimpleMarkerStyle()
    {
        super();
    }

    public SimpleMarkerStyle(int fillColor, int outColor, float size, int type) {
        super(fillColor);
        mType = type;
        mSize = size;
        mOutColor = outColor;
        mWidth = 1;
    }

    protected void onDraw(GeoPoint pt, GISDisplay display)
    {
        if(null == pt)
            return;

        switch (mType){
            case MarkerStylePoint:
                Paint ptPaint = new Paint();
                ptPaint.setColor(mColor);
                ptPaint.setStrokeWidth((float) (mSize / display.getScale()));
                ptPaint.setStrokeCap(Paint.Cap.ROUND);
                ptPaint.setAntiAlias(true);

                display.drawPoint((float)pt.getX(), (float)pt.getY(), ptPaint);
                break;
            case MarkerStyleCircle:
                Paint fillCirclePaint = new Paint();
                fillCirclePaint.setColor(mColor);
                fillCirclePaint.setStrokeCap(Paint.Cap.ROUND);

                display.drawCircle((float)pt.getX(), (float)pt.getY(), mSize, fillCirclePaint);

                Paint outCirclePaint = new Paint();
                outCirclePaint.setColor(mOutColor);
                outCirclePaint.setStrokeWidth((float) (mWidth / display.getScale()));
                outCirclePaint.setStyle(Paint.Style.STROKE);
                outCirclePaint.setAntiAlias(true);
                display.drawCircle((float)pt.getX(), (float)pt.getY(), mSize, outCirclePaint);

                break;
            case MarkerStyleDiamond:
                break;
            case MarkerStyleCross:
                break;
            case MarkerStyleTriangle:
                break;
            case MarkerStyleBox:
                break;
        }
    }

    @Override
    public void onDraw(GeoGeometry geoGeometry, GISDisplay display) {
        switch (geoGeometry.getType())
        {
            case GTPoint:
                GeoPoint pt = (GeoPoint) geoGeometry;
                onDraw(pt, display);
                break;
            case GTMultiPoint:
                GeoMultiPoint multiPoint = (GeoMultiPoint) geoGeometry;
                for(int i = 0; i < multiPoint.size(); i++)
                {
                    onDraw(multiPoint.get(i), display);
                }
                break;
            default:
                throw new IllegalArgumentException("The input geometry type is not support by this style");
        }
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }

    public float getSize() {
        return mSize;
    }

    public void setSize(float size) {
        mSize = size;
    }

    public float getWidth() {
        return mWidth;
    }

    public void setWidth(float width) {
       mWidth = width;
    }

    public int getOutlineColor() {
        return mOutColor;
    }

    public void setOutlineColor(int outColor) {
        mOutColor = outColor;
    }

    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_OUTCOLOR_KEY, mOutColor);
        rootConfig.put(JSON_TYPE_KEY, mType);
        rootConfig.put(JSON_WIDTH_KEY, mWidth);
        rootConfig.put(JSON_SIZE_KEY, mSize);
        rootConfig.put(JSON_NAME_KEY, "SimpleMarkerStyle");
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mOutColor = jsonObject.getInt(JSON_OUTCOLOR_KEY);
        mType = jsonObject.getInt(JSON_TYPE_KEY);
        mWidth = (float) jsonObject.getDouble(JSON_WIDTH_KEY);
        mSize = (float) jsonObject.getDouble(JSON_SIZE_KEY);
    }
}
