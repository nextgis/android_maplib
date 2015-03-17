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
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoMultiLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.*;

public class SimpleLineStyle  extends Style{
    protected float mWidth;

    public SimpleLineStyle()
    {
        super();
    }

    public SimpleLineStyle(int color) {
        super(color);
        mWidth = 3;
    }


    public void onDraw(
            GeoLineString lineString,
            GISDisplay display)
    {
        Paint lnPaint = new Paint();
        lnPaint.setColor(mColor);
        lnPaint.setStrokeWidth((float) (mWidth / display.getScale()));
        lnPaint.setStrokeCap(Paint.Cap.ROUND);
        lnPaint.setAntiAlias(true);

        List<GeoPoint> points = lineString.getPoints();
        for (int i = 1; i < points.size(); i++) {
            display.drawLine((float) points.get(i-1).getX(), (float) points.get(i-1).getY(),
                             (float) points.get(i).getX(), (float) points.get(i).getY(), lnPaint);
        }
//        float[] pts = new float[points.size() * 2];
//
//        int counter = 0;
//        for (GeoPoint pt : points) {
//            pts[counter++] = (float) pt.getX();
//            pts[counter++] = (float) pt.getY();
//        }
//
//        display.drawLines(pts, lnPaint);
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
            default:
                throw new IllegalArgumentException(
                        "The input geometry type is not support by this style");
        }


    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_WIDTH_KEY, mWidth);
        rootConfig.put(JSON_NAME_KEY, "SimpleLineStyle");
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mWidth = (float) jsonObject.getDouble(JSON_WIDTH_KEY);
    }
}
