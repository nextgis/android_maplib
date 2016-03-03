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
import android.graphics.Path;

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
    protected Paint mOutPaint;
    protected Paint mFillPaint;


    public SimpleMarkerStyle()
    {
        super();
        initPaints();
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

        initPaints();
        setPaintsColors();
    }


    private void initPaints() {
        mFillPaint = new Paint();
        mFillPaint.setStrokeCap(Paint.Cap.ROUND);

        mOutPaint = new Paint();
        mOutPaint.setStyle(Paint.Style.STROKE);
        mOutPaint.setAntiAlias(true);
    }


    private void setPaintsColors() {
        mFillPaint.setColor(mColor);
        mOutPaint.setColor(mOutColor);
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

        float scaledSize = (float) (mSize / display.getScale());
        switch (mType) {
            case MarkerStylePoint:
                drawPointMarker(pt, display);
                break;

            case MarkerStyleCircle:
                drawCircleMarker(scaledSize, pt, display);
                break;

            case MarkerStyleDiamond:
                drawDiamondMarker(scaledSize, pt, display);
                break;

            case MarkerStyleCross:
                // TODO
                break;

            case MarkerStyleTriangle:
                drawTriangleMarker(scaledSize, pt, display);
                break;

            case MarkerStyleBox:
                drawBoxMarker(scaledSize, pt, display);
                break;

            case MarkerStyleCrossedBox:
                drawCrossedBoxMarker(scaledSize, pt, display);
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
        mOutPaint.setColor(mColor);
        mOutPaint.setStrokeWidth((float) (mSize / display.getScale()));
        display.drawPoint((float) pt.getX(), (float) pt.getY(), mOutPaint);
        mOutPaint.setColor(mOutColor);
    }


    protected void drawCircleMarker(
            float scaledSize, GeoPoint pt,
            GISDisplay display)
    {
        if (scaledSize < 2) {
            mOutPaint.setColor(mColor);
            mOutPaint.setStrokeWidth(scaledSize);
            display.drawCircle((float) pt.getX(), (float) pt.getY(), mOutPaint);
        } else
            display.drawCircle((float) pt.getX(), (float) pt.getY(), scaledSize, mFillPaint);

        mOutPaint.setStrokeWidth((float) (mWidth / display.getScale()));
        if (scaledSize >= 2) {
            mOutPaint.setColor(mOutColor);
            display.drawCircle((float) pt.getX(), (float) pt.getY(), scaledSize, mOutPaint);
        }
    }


    protected void drawDiamondMarker(
            float scaledSize, GeoPoint pt,
            GISDisplay display)
    {
        Path path = new Path();
        path.moveTo((float) pt.getX() + scaledSize, (float) pt.getY());
        path.lineTo((float) pt.getX(), (float) pt.getY() + scaledSize);
        path.lineTo((float) pt.getX() - scaledSize, (float) pt.getY());
        path.lineTo((float) pt.getX(), (float) pt.getY() - scaledSize);
        path.close();

        drawPath(path, display);
    }


    protected void drawTriangleMarker(
            float scaledSize, GeoPoint pt,
            GISDisplay display)
    {
        Path path = new Path();
        path.moveTo((float) pt.getX() + scaledSize, (float) pt.getY() - scaledSize);
        path.lineTo((float) pt.getX(), (float) pt.getY() + scaledSize);
        path.lineTo((float) pt.getX() - scaledSize, (float) pt.getY() - scaledSize);
        path.close();

        drawPath(path, display);
    }


    protected void drawPath(Path path, GISDisplay display) {
        display.drawPath(path, mFillPaint);
        mOutPaint.setStrokeWidth((float) (mWidth / display.getScale()));
        display.drawPath(path, mOutPaint);
    }


    protected void drawBoxMarker(
            float scaledSize, GeoPoint pt,
            GISDisplay display)
    {
        display.drawBox((float) pt.getX(), (float) pt.getY(), scaledSize, mFillPaint);

        mOutPaint.setStrokeWidth((float) (mWidth / display.getScale()));
        display.drawBox((float) pt.getX(), (float) pt.getY(), scaledSize, mOutPaint);
    }


    protected void drawCrossedBoxMarker(
            float scaledSize, GeoPoint pt,
            GISDisplay display)
    {
        display.drawBox((float) pt.getX(), (float) pt.getY(), scaledSize, mFillPaint);

        mOutPaint.setStrokeWidth((float) (mWidth / display.getScale()));
        display.drawCrossedBox((float) pt.getX(), (float) pt.getY(), scaledSize, mOutPaint);
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
        setPaintsColors();
    }
}
