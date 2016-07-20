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

import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;


public class GeoMultiLineString
        extends GeoGeometryCollection
{
    protected static final long serialVersionUID = -1241179697270831766L;


    public GeoMultiLineString() {
        super();
    }


    public GeoMultiLineString(GeoMultiLineString geoMultiLineString) {
        super(geoMultiLineString);
    }


    @Override
    public void add(GeoGeometry geometry)
            throws ClassCastException
    {
        if (!(geometry instanceof GeoLineString)) {
            throw new ClassCastException("GeoMultiLineString: geometry is not GeoLineString type.");
        }

        super.add(geometry);
    }


    @Override
    public GeoLineString get(int index)
    {
        return (GeoLineString) mGeometries.get(index);
    }


    @Override
    public int getType()
    {
        return GeoConstants.GTMultiLineString;
    }


    @Override
    public void setCoordinatesFromJSON(JSONArray coordinates)
            throws JSONException
    {
        for (int i = 0; i < coordinates.length(); ++i) {
            GeoLineString lineString = new GeoLineString();
            lineString.setCoordinatesFromJSON(coordinates.getJSONArray(i));
            add(lineString);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void setCoordinatesFromJSONStream(JsonReader reader, int crs) throws IOException {
        setCRS(crs);
        reader.beginArray();
        while (reader.hasNext()){
            GeoLineString line = new GeoLineString();
            line.setCoordinatesFromJSONStream(reader, crs);
            mGeometries.add(line);
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

        int pos = wkt.indexOf("(");
        while (pos != Constants.NOT_FOUND) {
            wkt = wkt.substring(pos + 1, wkt.length());
            pos = wkt.indexOf(")") - 1;
            if (pos < 1) {
                return;
            }

            GeoLineString lineString = new GeoLineString();
            lineString.setCoordinatesFromWKT(wkt.substring(0, pos).trim(), crs);
            add(lineString);

            pos = wkt.indexOf("(");
        }
    }


    public void add(GeoLineString lineString)
    {
        super.add(lineString);
    }


    @Override
    public String toWKT(boolean full)
    {
        StringBuilder buf = new StringBuilder();
        if (full) {
            buf.append("MULTILINESTRING ");
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
        return new GeoMultiLineString();
    }

    @Override
    public GeoGeometry copy()
    {
        return new GeoMultiLineString(this);
    }

    public double getLength() {
        double length = 0;

        if (mGeometries.size() < 1)
            return length;

        for (int i = 0; i < size(); i++)
            length += get(i).getLength();

        return length;
    }
}
