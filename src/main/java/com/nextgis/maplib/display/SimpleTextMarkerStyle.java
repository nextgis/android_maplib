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

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import com.nextgis.maplib.datasource.GeoPoint;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;


public class SimpleTextMarkerStyle
        extends SimpleMarkerStyle
{
    public final static int MarkerStyleTextCircle = 209;


    protected String mMarkerText = "A";


    public SimpleTextMarkerStyle()
    {
        super();
    }


    public SimpleTextMarkerStyle(
            int fillColor,
            int outColor,
            float size,
            int type)
    {
        super(fillColor, outColor, size, type);
    }


    @Override
    public SimpleTextMarkerStyle clone()
            throws CloneNotSupportedException
    {
        SimpleTextMarkerStyle obj = (SimpleTextMarkerStyle) super.clone();
        obj.mMarkerText = mMarkerText;
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
            case MarkerStyleTextCircle:
                drawTextCircleMarker(pt, display);
                break;
        }
    }


    protected void drawTextCircleMarker(
            GeoPoint pt,
            GISDisplay display)
    {
        float scaledWidth = (float) (mWidth / display.getScale());
        float radius = (float) (mSize / display.getScale());

        Paint fillPaint = new Paint();
        fillPaint.setColor(mColor);
        fillPaint.setStrokeCap(Paint.Cap.ROUND);

        display.drawCircle((float) pt.getX(), (float) pt.getY(), radius, fillPaint);

        Paint outPaint = new Paint();
        outPaint.setColor(mOutColor);
        outPaint.setStrokeWidth(scaledWidth);
        outPaint.setStyle(Paint.Style.STROKE);
        outPaint.setAntiAlias(true);

        display.drawCircle((float) pt.getX(), (float) pt.getY(), radius, outPaint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setStrokeCap(Paint.Cap.ROUND);

        float gap = (float) (1 / display.getScale());
        float innerRadius = radius - scaledWidth - gap;
        float textSize = 2 * innerRadius; // initial text size

        Rect textRect = new Rect();
        textPaint.setTextSize(textSize);
        textPaint.getTextBounds(mMarkerText, 0, mMarkerText.length(), textRect);

        float halfW = textRect.width() / 2;
        float halfH = textRect.height() / 2;
        float outerTextRadius = (float) Math.sqrt(halfH * halfH + halfW * halfW);
        float textScale = innerRadius / outerTextRadius;

        float textX = (float) (pt.getX() - halfW);
        float textY = (float) (pt.getY() + halfH);

        Path textPath = new Path();
        textPaint.getTextPath(mMarkerText, 0, mMarkerText.length(), textX, textY, textPath);
        textPath.close();

        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(textScale, -textScale, (float) pt.getX(), (float) pt.getY());
        textPath.transform(matrix);

        display.drawPath(textPath, textPaint);
    }


    public String getMarkerText()
    {
        return mMarkerText;
    }


    public void setMarkerText(String markerText)
    {
        mMarkerText = markerText;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_NAME_KEY, "SimpleTextMarkerStyle");
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
    }

}
