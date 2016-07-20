/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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

import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;


public class GeoMultiPolygon
        extends GeoGeometryCollection
{
    protected static final long serialVersionUID = -1241179697270831767L;


    public GeoMultiPolygon() {
        super();
    }


    public GeoMultiPolygon(GeoMultiPolygon multiPolygon) {
        this();

        for (int i = 0; i < multiPolygon.size(); i++)
            add(multiPolygon.get(i).copy());
    }


    @Override
    public void add(GeoGeometry geometry)
            throws ClassCastException
    {
        if (!(geometry instanceof GeoPolygon)) {
            throw new ClassCastException("GeoMultiPolygon: geometry is not GeoPolygon type.");
        }

        super.add(geometry);
    }


    @Override
    public GeoPolygon get(int index)
    {
        return (GeoPolygon) mGeometries.get(index);
    }


    @Override
    public int getType()
    {
        return GeoConstants.GTMultiPolygon;
    }


    @Override
    public void setCoordinatesFromJSON(JSONArray coordinates)
            throws JSONException
    {
        for (int i = 0; i < coordinates.length(); ++i) {
            GeoPolygon polygon = new GeoPolygon();
            polygon.setCoordinatesFromJSON(coordinates.getJSONArray(i));
            add(polygon);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void setCoordinatesFromJSONStream(JsonReader reader, int crs) throws IOException {
        setCRS(crs);
        reader.beginArray();
        while (reader.hasNext()){
            GeoPolygon polygon = new GeoPolygon();
            polygon.setCoordinatesFromJSONStream(reader, crs);
            mGeometries.add(polygon);
        }
        reader.endArray();
    }

    @Override
    public String toWKT(boolean full)
    {
        StringBuilder buf = new StringBuilder();
        if (full) {
            buf.append("MULTIPOLYGON ");
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
    public void setCoordinatesFromWKT(String wkt, int crs)
    {
        setCRS(crs);
        if (wkt.contains("EMPTY")) {
            return;
        }

        if (wkt.startsWith("(")) {
            wkt = wkt.substring(1, wkt.length() - 1);
        }

        int pos = wkt.indexOf("((");
        while (pos != Constants.NOT_FOUND) {
            wkt = wkt.substring(pos + 1, wkt.length());
            pos = wkt.indexOf("))") - 1;
            if (pos < 1) {
                return;
            }

            GeoPolygon polygon = new GeoPolygon();
            polygon.setCoordinatesFromWKT(wkt.substring(0, pos).trim(), crs);
            add(polygon);

            pos = wkt.indexOf("((");
        }
    }


    public void add(GeoPolygon polygon)
    {
        super.add(polygon);
    }

    @Override
    public GeoGeometry copy()
    {
        return new GeoMultiPolygon(this);
    }

    @Override
    protected GeoGeometryCollection getInstance() {
        return new GeoMultiPolygon();
    }

    public void closeRings() {
        for (GeoGeometry polygon : mGeometries)
            if (polygon instanceof GeoPolygon)
                ((GeoPolygon) polygon).closeRings();
    }

    public boolean isSelfIntersects() {
        for (GeoGeometry polygon : mGeometries)
            if (polygon instanceof GeoPolygon)
                if (((GeoPolygon) polygon).intersects())
                    return true;

        return false;
    }

    public boolean isHolesInside() {
        for (GeoGeometry polygon : mGeometries)
            if (polygon instanceof GeoPolygon)
                if (!((GeoPolygon) polygon).isHolesInside())
                    return false;

        return true;
    }

    public boolean isHolesIntersect() {
        for (GeoGeometry polygon : mGeometries)
            if (polygon instanceof GeoPolygon)
                if (((GeoPolygon) polygon).isHolesIntersect())
                    return true;

        return false;
    }

    public double getPerimeter() {
        double length = 0;

        for (GeoGeometry polygon : mGeometries)
            if (polygon instanceof GeoPolygon)
                length += ((GeoPolygon) polygon).getPerimeter();

        return length;
    }

    public double getArea() {
        double area = 0;

        for (GeoGeometry polygon : mGeometries)
            if (polygon instanceof GeoPolygon)
                area += ((GeoPolygon) polygon).getArea();

        return area;
    }
}
