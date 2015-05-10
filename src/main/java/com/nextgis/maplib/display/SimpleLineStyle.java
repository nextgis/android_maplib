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
import android.graphics.Path;
import android.graphics.PathMeasure;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoMultiLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.GTLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiLineString;


public class SimpleLineStyle
        extends Style
{
    public final static int LineStyleSolid       = 1;
    public final static int LineStyleDash        = 2;
    public final static int LineStyleEdgingSolid = 3;

    protected int   mType;
    protected float mWidth;
    protected int   mOutColor;


    public SimpleLineStyle()
    {
        super();
    }


    public SimpleLineStyle(
            int fillColor,
            int outColor,
            int type)
    {
        super(fillColor);
        mType = type;
        mOutColor = outColor;
        mWidth = 3;
    }


    public void onDraw(
            GeoLineString lineString,
            GISDisplay display)
    {
        if (null == lineString) {
            return;
        }

        switch (mType) {
            case LineStyleSolid:
                drawSolidLine(lineString, display);
                break;

            case LineStyleDash:
                drawDashLine(lineString, display);
                break;

            case LineStyleEdgingSolid:
                drawSolidEdgingLine(lineString, display);
                break;
        }
    }


    @Override
    public void onDraw(
            GeoGeometry geoGeometry,
            GISDisplay display)
    {

        switch (geoGeometry.getType()) {
            case GTLineString:
                onDraw((GeoLineString) geoGeometry, display);
                break;

            case GTMultiLineString:
                GeoMultiLineString multiLineString = (GeoMultiLineString) geoGeometry;
                for (int i = 0; i < multiLineString.size(); i++) {
                    onDraw(multiLineString.get(i), display);
                }
                break;

                //throw new IllegalArgumentException(
                //        "The input geometry type is not support by this style");
        }


    }


    protected void drawSolidLine(
            GeoLineString lineString,
            GISDisplay display)
    {
        Paint paint = new Paint();
        paint.setColor(mColor);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth((float) (mWidth / display.getScale()));

        List<GeoPoint> points = lineString.getPoints();

        Path path = new Path();
        path.incReserve(points.size());

        path.moveTo((float) points.get(0).getX(), (float) points.get(0).getY());

        for (int i = 1; i < points.size(); ++i) {
            path.lineTo((float) points.get(i).getX(), (float) points.get(i).getY());
        }

        display.drawPath(path, paint);
    }


    protected void drawDashLine(
            GeoLineString lineString,
            GISDisplay display)
    {
        Paint paint = new Paint();
        paint.setColor(mColor);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth((float) (mWidth / display.getScale()));

        List<GeoPoint> points = lineString.getPoints();

        // workaround for "DashPathEffect/drawLine not working properly when hardwareAccelerated="true""
        // https://code.google.com/p/android/issues/detail?id=29944

        // get all points to the main path
        Path mainPath = new Path();
        mainPath.incReserve(points.size());

        mainPath.moveTo((float) points.get(0).getX(), (float) points.get(0).getY());

        for (int i = 1; i < points.size(); ++i) {
            mainPath.lineTo((float) points.get(i).getX(), (float) points.get(i).getY());
        }

        // draw along the main path
        PathMeasure pm = new PathMeasure(mainPath, false);
        float[] coordinates = new float[2];
        float length = pm.getLength();
        float dash = (float) (10 / display.getScale());
        float gap = (float) (5 / display.getScale());
        float distance = dash;
        boolean isDash = true;

        Path dashPath = new Path();
        dashPath.incReserve((int) (2 * length / (dash + gap)));

        dashPath.moveTo((float) points.get(0).getX(), (float) points.get(0).getY());

        while (distance < length) {
            // get a point from the main path
            pm.getPosTan(distance, coordinates, null);

            if (isDash) {
                dashPath.lineTo(coordinates[0], coordinates[1]);
                distance += gap;
            } else {
                dashPath.moveTo(coordinates[0], coordinates[1]);
                distance += dash;
            }

            isDash = !isDash;
        }

        // add a rest from the main path
        if (isDash) {
            distance = distance - dash;
            float rest = length - distance;

            if (rest > (float) (1 / display.getScale())) {
                distance = length - 1;
                pm.getPosTan(distance, coordinates, null);
                dashPath.lineTo(coordinates[0], coordinates[1]);
            }
        }

        display.drawPath(dashPath, paint);
    }


    protected void drawSolidEdgingLine(
            GeoLineString lineString,
            GISDisplay display)
    {
        double scaledWidth = mWidth / display.getScale();

        Paint mainPaint = new Paint();
        mainPaint.setColor(mColor);
        mainPaint.setAntiAlias(true);
        mainPaint.setStyle(Paint.Style.STROKE);
        mainPaint.setStrokeCap(Paint.Cap.BUTT);
        mainPaint.setStrokeWidth((float) (scaledWidth));

        Paint edgingPaint = new Paint(mainPaint);
        edgingPaint.setColor(mOutColor);
        edgingPaint.setStrokeCap(Paint.Cap.BUTT);
        edgingPaint.setStrokeWidth((float) (scaledWidth * 3));

        List<GeoPoint> points = lineString.getPoints();

        Path path = new Path();
        path.incReserve(points.size());

        path.moveTo((float) points.get(0).getX(), (float) points.get(0).getY());

        for (int i = 1; i < points.size(); ++i) {
            path.lineTo((float) points.get(i).getX(), (float) points.get(i).getY());
        }

        display.drawPath(path, edgingPaint);
        display.drawPath(path, mainPaint);
    }


    public int getType()
    {
        return mType;
    }


    public void setType(int type)
    {
        mType = type;
    }


    public float getWidth()
    {
        return mWidth;
    }


    public void setWidth(float width)
    {
        mWidth = width;
    }


    public int getOutColor()
    {
        return mOutColor;
    }


    public void setOutColor(int outColor)
    {
        mOutColor = outColor;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_NAME_KEY, "SimpleLineStyle");
        rootConfig.put(JSON_TYPE_KEY, mType);
        rootConfig.put(JSON_WIDTH_KEY, mWidth);
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
        mOutColor = jsonObject.getInt(JSON_OUTCOLOR_KEY);
    }
}
