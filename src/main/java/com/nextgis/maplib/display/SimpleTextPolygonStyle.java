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
import com.nextgis.maplib.datasource.GeoPolygon;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;


public class SimpleTextPolygonStyle
        extends SimplePolygonStyle
{
    protected String mText     = "A";
    protected float  mTextSize = 25;


    public SimpleTextPolygonStyle()
    {
        super();
    }


    public SimpleTextPolygonStyle(int color)
    {
        super(color);
    }


    @Override
    public SimpleTextPolygonStyle clone()
            throws CloneNotSupportedException
    {
        SimpleTextPolygonStyle obj = (SimpleTextPolygonStyle) super.clone();
        obj.mText = mText;
        obj.mTextSize = mTextSize;
        return obj;
    }


    public void drawPolygon(
            GeoPolygon polygon,
            GISDisplay display)
    {
        float scaledWidth = (float) (mWidth / display.getScale());

        Paint lnPaint = new Paint();
        lnPaint.setColor(mColor);
        lnPaint.setStrokeWidth(scaledWidth);
        lnPaint.setStrokeCap(Paint.Cap.ROUND);
        lnPaint.setAntiAlias(true);

        Path polygonPath = getPath(polygon);

        lnPaint.setStyle(Paint.Style.STROKE);
        lnPaint.setAlpha(128);
        display.drawPath(polygonPath, lnPaint);

        if (mFill) {
            lnPaint.setStyle(Paint.Style.FILL);
            lnPaint.setAlpha(64);
            display.drawPath(polygonPath, lnPaint);
        }


        GeoPoint center = polygon.getEnvelope().getCenter();

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setAlpha(128);

        float scaledTextSize = (float) (mTextSize / display.getScale());

        Rect textRect = new Rect();
        textPaint.setTextSize(scaledTextSize);
        textPaint.getTextBounds(mText, 0, mText.length(), textRect);

        float halfW = textRect.width() / 2;
        float halfH = textRect.height() / 2;

        float textX = (float) (center.getX() - halfW);
        float textY = (float) (center.getY() + halfH);

        Path textPath = new Path();
        textPaint.getTextPath(mText, 0, mText.length(), textX, textY, textPath);
        textPath.close();

        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(1, -1, (float) center.getX(), (float) center.getY());
        textPath.transform(matrix);

        display.drawPath(textPath, textPaint);
    }


    public String getText()
    {
        return mText;
    }


    public void setText(String text)
    {
        mText = text;
    }


    public float getTextSize()
    {
        return mTextSize;
    }


    public void setTextSize(float textSize)
    {
        mTextSize = textSize;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_NAME_KEY, "SimpleTextPolygonStyle");
        return rootConfig;
    }
}
