/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
import com.nextgis.maplib.datasource.GeoPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplib.util.Constants.*;

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

    @Override
    public void onDraw(GeoGeometry geoGeometry, GISDisplay display) {
        GeoLineString line = (GeoLineString) geoGeometry;
        Paint lnPaint = new Paint();
        lnPaint.setColor(mColor);
        lnPaint.setStrokeWidth((float) (mWidth / display.getScale()));
        lnPaint.setStrokeCap(Paint.Cap.ROUND);
        lnPaint.setAntiAlias(true);

        List<GeoPoint> points = line.getPoints();
        float [] pts = new float[points.size() * 2];

        int counter = 0;
        for(GeoPoint pt : points){
            pts[counter++] = (float)pt.getX();
            pts[counter++] = (float)pt.getY();
        }

        display.drawLines(pts, lnPaint);
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
