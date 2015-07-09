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
import android.graphics.PathMeasure;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;


public class SimpleTextLineStyle
        extends SimpleLineStyle
{
    public final static int LineStyleTextSolid = 104;

    protected String mLineText = "?";


    public SimpleTextLineStyle()
    {
        super();
    }


    public SimpleTextLineStyle(
            int fillColor,
            int outColor,
            int type)
    {
        super(fillColor, outColor, type);
    }


    @Override
    public SimpleTextLineStyle clone()
            throws CloneNotSupportedException
    {
        SimpleTextLineStyle obj = (SimpleTextLineStyle) super.clone();
        obj.mLineText = mLineText;
        return obj;
    }


    public void onDraw(
            GeoLineString lineString,
            GISDisplay display)
    {
        if (null == lineString) {
            return;
        }

        super.onDraw(lineString, display);

        switch (mType) {
            case LineStyleTextSolid:
                drawTextSolidLine(lineString, display);
                break;
        }
    }


    protected void drawTextSolidLine(
            GeoLineString lineString,
            GISDisplay display)
    {
        float scaledWidth = (float) (mWidth / display.getScale());

        Paint mainPaint = new Paint();
        mainPaint.setColor(mColor);
        mainPaint.setAntiAlias(true);
        mainPaint.setStyle(Paint.Style.STROKE);
        mainPaint.setStrokeCap(Paint.Cap.BUTT);
        mainPaint.setStrokeWidth(scaledWidth);

        Paint textPaint = new Paint();
        textPaint.setColor(mOutColor);
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setStrokeWidth(scaledWidth);

        float textSize = 12 * scaledWidth;
        textPaint.setTextSize(textSize);
        float textWidth = textPaint.measureText(mLineText);
        float vOffset = (float) (textSize / 2.7);

        // get all points to the main path
        List<GeoPoint> points = lineString.getPoints();
        Path mainPath = new Path();
        mainPath.incReserve(points.size());

        mainPath.moveTo((float) points.get(0).getX(), (float) points.get(0).getY());

        for (int i = 1; i < points.size(); ++i) {
            mainPath.lineTo((float) points.get(i).getX(), (float) points.get(i).getY());
        }

        // draw the main path
        display.drawPath(mainPath, mainPaint);

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

            display.drawTextOnPath(mLineText, textPath, 0, vOffset, textPaint);

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

            display.drawTextOnPath(mLineText, textPath, 0, vOffset, textPaint);
        }
    }


    public String getLineText()
    {
        return mLineText;
    }


    public void setLineText(String lineText)
    {
        mLineText = lineText;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_NAME_KEY, "SimpleTextLineStyle");
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
    }
}
