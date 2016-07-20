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
package com.nextgis.maplib.datasource;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.JsonReader;

import com.nextgis.maplib.util.GeoConstants;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;


public class GeoMultiPoint
        extends GeoGeometryCollection
{
    protected static final long serialVersionUID = -1241179697270831765L;

    public GeoMultiPoint(GeoMultiPoint geoMultiPoint) {
        super(geoMultiPoint);
    }

    public GeoMultiPoint() {
        super();
    }


    @Override
    public void add(GeoGeometry geometry)
            throws ClassCastException
    {
        if (!(geometry instanceof GeoPoint)) {
            throw new ClassCastException("GeoMultiPoint: geometry is not GeoPoint type.");
        }

        super.add(geometry);
    }


    @Override
    public GeoPoint get(int index)
    {
        return (GeoPoint) mGeometries.get(index);
    }


    @Override
    public int getType()
    {
        return GeoConstants.GTMultiPoint;
    }


    @Override
    public void setCoordinatesFromJSON(JSONArray coordinates)
            throws JSONException
    {
        for (int i = 0; i < coordinates.length(); ++i) {
            GeoPoint point = new GeoPoint();
            point.setCoordinatesFromJSON(coordinates.getJSONArray(i));
            add(point);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void setCoordinatesFromJSONStream(JsonReader reader, int crs) throws IOException {
        setCRS(crs);
        reader.beginArray();
        while (reader.hasNext()){
            GeoPoint pt = new GeoPoint();
            pt.setCoordinatesFromJSONStream(reader, crs);
            mGeometries.add(pt);
        }
        reader.endArray();
    }

    @Override
    public void setCoordinatesFromWKT(String wkt, int crs)
    {
        setCRS(crs);
        if (wkt.contains("EMPTY")) {
            return;
        }

        if (wkt.startsWith("(")) {
            wkt = wkt.substring(1, wkt.length() - 1);
        }
        for (String wktPt : wkt.split(",")) {
            GeoPoint pt = new GeoPoint();
            pt.setCoordinatesFromWKT(wktPt.trim(), crs);
            add(pt);
        }
    }

    @Override
    public GeoGeometry copy()
    {
        return new GeoMultiPoint(this);
    }

    public void add(GeoPoint point)
    {
        super.add(point);
    }


    @Override
    public String toWKT(boolean full)
    {
        StringBuilder buf = new StringBuilder();
        if (full) {
            buf.append("MULTIPOINT ");
        }
        if (mGeometries.size() == 0) {
            buf.append(" EMPTY");
        } else {
            buf.append("(");
            for (int i = 0; i < mGeometries.size(); i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                GeoGeometry geom = mGeometries.get(i);
                buf.append(geom.toWKT(false));
            }
            buf.append(")");
        }
        return buf.toString();
    }

    @Override
    protected GeoGeometryCollection getInstance() {
        return new GeoMultiPoint();
    }

    @Override
    public GeoGeometry simplify(double tolerance) {
        GeoMultiPoint result = (GeoMultiPoint) copy();
        double checkTolerance = tolerance + tolerance;
        for(int i = 0; i < result.mGeometries.size(); i++){

            for(int j = 0; j < result.mGeometries.size(); j++){
                if(i == j)
                    continue;
                GeoPoint pt1 = (GeoPoint) result.mGeometries.get(i);
                GeoPoint pt2 = (GeoPoint) result.mGeometries.get(j);

                if(Math.abs(pt1.distance(pt2)) < checkTolerance) {
                    result.mGeometries.remove(j);
                    j--;
                }
            }
        }
        return result;
    }
}
