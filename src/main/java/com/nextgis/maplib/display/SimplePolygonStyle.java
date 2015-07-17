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
import com.nextgis.maplib.datasource.GeoMultiPolygon;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;
import static com.nextgis.maplib.util.Constants.JSON_WIDTH_KEY;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPolygon;
import static com.nextgis.maplib.util.GeoConstants.GTPolygon;


public class SimplePolygonStyle
        extends Style
{
    protected float mWidth;
    protected boolean mFill;

    protected String JSON_FILL_KEY = "fill";


    public SimplePolygonStyle()
    {
        super();
    }


    public SimplePolygonStyle(int color)
    {
        super(color);
        mWidth = 3;
        mFill = true;
    }


    @Override
    public SimplePolygonStyle clone()
            throws CloneNotSupportedException
    {
        SimplePolygonStyle obj = (SimplePolygonStyle) super.clone();
        obj.mWidth = mWidth;
        obj.mFill = mFill;
        return obj;
    }


    public void onDraw(
            GeoPolygon polygon,
            GISDisplay display)
    {
        Paint lnPaint = new Paint();
        lnPaint.setColor(mColor);
        lnPaint.setStrokeWidth((float) (mWidth / display.getScale()));
        lnPaint.setStrokeCap(Paint.Cap.ROUND);
        lnPaint.setAntiAlias(true);

        Path polygonPath = getPath(polygon);

        lnPaint.setStyle(Paint.Style.STROKE);
        lnPaint.setAlpha(128);
        display.drawPath(polygonPath, lnPaint);

        if(mFill) {
            lnPaint.setStyle(Paint.Style.FILL);
            lnPaint.setAlpha(64);
            display.drawPath(polygonPath, lnPaint);
        }
    }

    public boolean isFill() {
        return mFill;
    }

    public void setFill(boolean fill) {
        mFill = fill;
    }

    @Override
    public void onDraw(
            GeoGeometry geoGeometry,
            GISDisplay display)
    {
        switch (geoGeometry.getType()) {
            case GTPolygon:
                onDraw((GeoPolygon) geoGeometry, display);
                break;
            case GTMultiPolygon:
                GeoMultiPolygon multiPolygon = (GeoMultiPolygon) geoGeometry;

                for (int i = 0; i < multiPolygon.size(); i++) {
                    onDraw(multiPolygon.get(i), display);
                }
                break;

            //throw new IllegalArgumentException(
            //        "The input geometry type is not support by this style");
        }
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_WIDTH_KEY, mWidth);
        rootConfig.put(JSON_FILL_KEY, mFill);
        rootConfig.put(JSON_NAME_KEY, "SimplePolygonStyle");
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        if(jsonObject.has(JSON_WIDTH_KEY))
            mWidth = (float) jsonObject.getDouble(JSON_WIDTH_KEY);
        if(jsonObject.has(JSON_FILL_KEY))
            mFill = jsonObject.getBoolean(JSON_FILL_KEY);
    }


    protected Path getPath(GeoPolygon polygon)
    {
        List<GeoPoint> points = polygon.getOuterRing().getPoints();
        Path polygonPath = new Path();
        appendPath(polygonPath, points);

        for (int i = 0; i < polygon.getInnerRingCount(); i++) {
            points = polygon.getInnerRing(i).getPoints();
            appendPath(polygonPath, points);
        }

        polygonPath.setFillType(Path.FillType.EVEN_ODD);

        return polygonPath;
    }


    protected void appendPath(
            Path polygonPath,
            List<GeoPoint> points)
    {
        float x0, y0;

        if (points.size() > 0) {
            x0 = (float) points.get(0).getX();
            y0 = (float) points.get(0).getY();
            polygonPath.moveTo(x0, y0);

            for (int i = 1; i < points.size(); i++) {
                x0 = (float) points.get(i).getX();
                y0 = (float) points.get(i).getY();

                polygonPath.lineTo(x0, y0);
            }

            polygonPath.close();
        }
    }
}
