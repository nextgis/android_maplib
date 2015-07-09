/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

import android.graphics.Paint;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPoint;
import static com.nextgis.maplib.util.GeoConstants.GTPoint;


public class SimpleMarkerStyle
        extends Style
{
    public final static int MarkerStylePoint      = 1;
    public final static int MarkerStyleCircle     = 2;
    public final static int MarkerStyleDiamond    = 3;
    public final static int MarkerStyleCross      = 4;
    public final static int MarkerStyleTriangle   = 5;
    public final static int MarkerStyleBox        = 6;
    public final static int MarkerEditStyleCircle = 7;
    public final static int MarkerStyleCrossedBox = 8;

    protected int   mType;
    protected float mSize;
    protected float mWidth;
    protected int   mOutColor;


    public SimpleMarkerStyle()
    {
        super();
    }


    public SimpleMarkerStyle(
            int fillColor,
            int outColor,
            float size,
            int type)
    {
        super(fillColor);
        mType = type;
        mSize = size;
        mOutColor = outColor;
        mWidth = 1;
    }


    @Override
    public SimpleMarkerStyle clone()
            throws CloneNotSupportedException
    {
        SimpleMarkerStyle obj = (SimpleMarkerStyle) super.clone();
        obj.mType = mType;
        obj.mSize = mSize;
        obj.mWidth = mWidth;
        obj.mOutColor = mOutColor;
        return obj;
    }


    protected void onDraw(
            GeoPoint pt,
            GISDisplay display)
    {
        if (null == pt) {
            return;
        }

        switch (mType) {
            case MarkerStylePoint:
                drawPointMarker(pt, display);
                break;

            case MarkerStyleCircle:
                drawCircleMarker(pt, display);
                break;

            case MarkerStyleDiamond:
                break;

            case MarkerStyleCross:
                break;

            case MarkerStyleTriangle:
                break;

            case MarkerStyleBox:
                drawBoxMarker(pt, display);
                break;

            case MarkerStyleCrossedBox:
                drawCrossedBoxMarker(pt, display);
                break;
        }
    }


    @Override
    public void onDraw(
            GeoGeometry geoGeometry,
            GISDisplay display)
    {
        switch (geoGeometry.getType()) {
            case GTPoint:
                GeoPoint pt = (GeoPoint) geoGeometry;
                onDraw(pt, display);
                break;
            case GTMultiPoint:
                GeoMultiPoint multiPoint = (GeoMultiPoint) geoGeometry;
                for (int i = 0; i < multiPoint.size(); i++) {
                    onDraw(multiPoint.get(i), display);
                }
                break;

            //throw new IllegalArgumentException(
            //        "The input geometry type is not support by this style");
        }
    }


    protected void drawPointMarker(
            GeoPoint pt,
            GISDisplay display)
    {
        Paint paint = new Paint();
        paint.setColor(mColor);
        paint.setStrokeWidth((float) (mSize / display.getScale()));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);

        display.drawPoint((float) pt.getX(), (float) pt.getY(), paint);
    }


    protected void drawCircleMarker(
            GeoPoint pt,
            GISDisplay display)
    {
        float scaledSize = (float) (mSize / display.getScale());

        Paint fillPaint = new Paint();
        fillPaint.setColor(mColor);
        fillPaint.setStrokeCap(Paint.Cap.ROUND);

        display.drawCircle((float) pt.getX(), (float) pt.getY(), scaledSize, fillPaint);

        Paint outPaint = new Paint();
        outPaint.setColor(mOutColor);
        outPaint.setStrokeWidth((float) (mWidth / display.getScale()));
        outPaint.setStyle(Paint.Style.STROKE);
        outPaint.setAntiAlias(true);

        display.drawCircle((float) pt.getX(), (float) pt.getY(), scaledSize, outPaint);
    }


    protected void drawBoxMarker(
            GeoPoint pt,
            GISDisplay display)
    {
        float scaledSize = (float) (mSize / display.getScale());

        Paint fillPaint = new Paint();
        fillPaint.setColor(mColor);
        fillPaint.setStrokeCap(Paint.Cap.ROUND);

        display.drawBox((float) pt.getX(), (float) pt.getY(), scaledSize, fillPaint);

        Paint outPaint = new Paint();
        outPaint.setColor(mOutColor);
        outPaint.setStrokeWidth((float) (mWidth / display.getScale()));
        outPaint.setStyle(Paint.Style.STROKE);
        outPaint.setAntiAlias(true);

        display.drawBox((float) pt.getX(), (float) pt.getY(), scaledSize, outPaint);
    }


    protected void drawCrossedBoxMarker(
            GeoPoint pt,
            GISDisplay display)
    {
        float scaledSize = (float) (mSize / display.getScale());

        Paint fillPaint = new Paint();
        fillPaint.setColor(mColor);
        fillPaint.setStrokeCap(Paint.Cap.ROUND);

        display.drawBox((float) pt.getX(), (float) pt.getY(), scaledSize, fillPaint);

        Paint outPaint = new Paint();
        outPaint.setColor(mOutColor);
        outPaint.setStrokeWidth((float) (mWidth / display.getScale()));
        outPaint.setStyle(Paint.Style.STROKE);
        outPaint.setAntiAlias(true);

        display.drawCrossedBox((float) pt.getX(), (float) pt.getY(), scaledSize, outPaint);
    }


    public int getType()
    {
        return mType;
    }


    public void setType(int type)
    {
        mType = type;
    }


    public float getSize()
    {
        return mSize;
    }


    public void setSize(float size)
    {
        mSize = size;
    }


    public float getWidth()
    {
        return mWidth;
    }


    public void setWidth(float width)
    {
        mWidth = width;
    }


    public int getOutlineColor()
    {
        return mOutColor;
    }


    public void setOutlineColor(int outColor)
    {
        mOutColor = outColor;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_NAME_KEY, "SimpleMarkerStyle");
        rootConfig.put(JSON_TYPE_KEY, mType);
        rootConfig.put(JSON_WIDTH_KEY, mWidth);
        rootConfig.put(JSON_SIZE_KEY, mSize);
        rootConfig.put(JSON_OUTCOLOR_KEY, mOutColor);
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mType = jsonObject.getInt(JSON_TYPE_KEY);
        mWidth = (float) jsonObject.getDouble(JSON_WIDTH_KEY);
        mSize = (float) jsonObject.getDouble(JSON_SIZE_KEY);
        mOutColor = jsonObject.getInt(JSON_OUTCOLOR_KEY);
    }
}
