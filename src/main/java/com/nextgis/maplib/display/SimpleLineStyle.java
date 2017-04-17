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

package com.nextgis.maplib.display;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.text.TextUtils;

import com.nextgis.maplib.api.ITextStyle;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoMultiLineString;
import com.nextgis.maplib.datasource.GeoPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplib.util.Constants.JSON_DISPLAY_NAME;
import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;
import static com.nextgis.maplib.util.Constants.JSON_TYPE_KEY;
import static com.nextgis.maplib.util.Constants.JSON_VALUE_KEY;
import static com.nextgis.maplib.util.GeoConstants.GTLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiLineString;

public class SimpleLineStyle extends Style implements ITextStyle {
    public final static int LineStyleSolid = 1;
    public final static int LineStyleDash = 2;
    public final static int LineStyleEdgingSolid = 3;

    protected int mType;
    protected Paint.Cap mStrokeCap;
    protected String mField;
    protected String mText;

    public SimpleLineStyle() {
        super();
        mStrokeCap = Paint.Cap.BUTT;
    }

    public SimpleLineStyle(int fillColor, int outColor, int type) {
        super(fillColor, outColor);
        mType = type;
        mStrokeCap = Paint.Cap.BUTT;
    }

    @Override
    public SimpleLineStyle clone() throws CloneNotSupportedException {
        SimpleLineStyle obj = (SimpleLineStyle) super.clone();
        obj.mType = mType;
        obj.mStrokeCap = mStrokeCap;
        obj.mText = mText;
        obj.mField = mField;
        return obj;
    }

    public void onDraw(GeoLineString lineString, GISDisplay display) {
        if (null == lineString) {
            return;
        }

        float scaledWidth = (float) (mWidth / display.getScale());
        Path mainPath = null;
        switch (mType) {
            case LineStyleSolid:
                mainPath = drawSolidLine(scaledWidth, lineString, display);
                break;

            case LineStyleDash:
                mainPath = drawDashLine(scaledWidth, lineString, display);
                break;

            case LineStyleEdgingSolid:
                mainPath = drawSolidEdgingLine(scaledWidth, lineString, display);
                break;
        }

        drawText(scaledWidth, mainPath, display);
    }

    protected void drawText(float scaledWidth, Path mainPath, GISDisplay display) {
        if (TextUtils.isEmpty(mText) || mainPath == null)
            return;

        Paint textPaint = new Paint();
        textPaint.setColor(mOutColor);
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setStrokeWidth(scaledWidth);

        float textSize = 12 * scaledWidth;
        textPaint.setTextSize(textSize);
        float textWidth = textPaint.measureText(mText);
        float vOffset = (float) (textSize / 2.7);

        // draw text along the main path
        PathMeasure pm = new PathMeasure(mainPath, false);
        float length = pm.getLength();
        float gap = textPaint.measureText("_");
        float period = textWidth + gap;
        float startD = gap;
        float stopD = startD + period;

        Path textPath = new Path();

        while (stopD < length) {
            textPath.reset();
            pm.getSegment(startD, stopD, textPath, true);
            textPath.rLineTo(0, 0); // workaround for API <= 19

            display.drawTextOnPath(mText, textPath, 0, vOffset, textPaint);

            startD += period;
            stopD += period;
        }

        stopD = startD;
        float rest = length - stopD;

        if (rest > gap * 2) {
            stopD = length - gap;

            textPath.reset();
            pm.getSegment(startD, stopD, textPath, true);
            textPath.rLineTo(0, 0); // workaround for API <= 19

            display.drawTextOnPath(mText, textPath, 0, vOffset, textPaint);
        }
    }

    @Override
    public void onDraw(GeoGeometry geoGeometry, GISDisplay display) {
        mColor = Color.argb(mInnerAlpha, Color.red(mColor), Color.green(mColor), Color.blue(mColor));
        mOutColor = Color.argb(mOuterAlpha, Color.red(mOutColor), Color.green(mOutColor), Color.blue(mOutColor));

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

    protected Path drawSolidLine(float scaledWidth, GeoLineString lineString, GISDisplay display) {
        Paint paint = new Paint();
        paint.setColor(mColor);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(mStrokeCap);
        paint.setStrokeWidth(scaledWidth);

        List<GeoPoint> points = lineString.getPoints();

        Path path = new Path();
        path.incReserve(points.size());

        path.moveTo((float) points.get(0).getX(), (float) points.get(0).getY());

        for (int i = 1; i < points.size(); ++i) {
            path.lineTo((float) points.get(i).getX(), (float) points.get(i).getY());
        }

        display.drawPath(path, paint);

        return path;
    }

    protected Path drawDashLine(float scaledWidth, GeoLineString lineString, GISDisplay display) {
        Paint paint = new Paint();
        paint.setColor(mColor);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(scaledWidth);

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

        return mainPath;
    }

    protected Path drawSolidEdgingLine(float scaledWidth, GeoLineString lineString, GISDisplay display) {
        Paint mainPaint = new Paint();
        mainPaint.setColor(mColor);
        mainPaint.setAntiAlias(true);
        mainPaint.setStyle(Paint.Style.STROKE);
        mainPaint.setStrokeCap(Paint.Cap.BUTT);
        mainPaint.setStrokeWidth(scaledWidth);

        Paint edgingPaint = new Paint(mainPaint);
        edgingPaint.setColor(mOutColor);
        edgingPaint.setStrokeCap(Paint.Cap.BUTT);
        edgingPaint.setStrokeWidth(scaledWidth * 3);

        List<GeoPoint> points = lineString.getPoints();

        Path path = new Path();
        path.incReserve(points.size());

        path.moveTo((float) points.get(0).getX(), (float) points.get(0).getY());

        for (int i = 1; i < points.size(); ++i) {
            path.lineTo((float) points.get(i).getX(), (float) points.get(i).getY());
        }

        display.drawPath(path, edgingPaint);
        display.drawPath(path, mainPaint);

        return path;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }

    public String getField() {
        return mField;
    }

    public void setField(String field) {
        mField = field;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        if (!TextUtils.isEmpty(text))
            mText = text;
        else
            mText = null;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_NAME_KEY, "SimpleLineStyle");
        rootConfig.put(JSON_TYPE_KEY, mType);

        if (null != mText) {
            rootConfig.put(JSON_DISPLAY_NAME, mText);
        }
        if (null != mField) {
            rootConfig.put(JSON_VALUE_KEY, mField);
        }

        return rootConfig;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        super.fromJSON(jsonObject);
        mType = jsonObject.getInt(JSON_TYPE_KEY);

        if (jsonObject.has(JSON_DISPLAY_NAME)) {
            mText = jsonObject.getString(JSON_DISPLAY_NAME);
        }
        if (jsonObject.has(JSON_VALUE_KEY)) {
            mField = jsonObject.getString(JSON_VALUE_KEY);
        }
    }
}
