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

package com.nextgis.maplib.datasource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import static com.nextgis.maplib.util.GeoConstants.*;


/**
 * The class to create GeoGeometry instances from various sources
 */
public class GeoGeometryFactory
{
    public static GeoGeometry fromJson(JSONObject jsonObject)
            throws JSONException
    {
        String jsonType = jsonObject.getString(GEOJSON_TYPE);
        int type = typeFromJSON(jsonType);

        GeoGeometry output = null;
        switch (type) {
            case GTPoint:
                output = new GeoPoint();
                break;
            case GTLineString:
                output = new GeoLineString();
                break;
            case GTPolygon:
                output = new GeoPolygon();
                break;
            case GTMultiPoint:
                output = new GeoMultiPoint();
                break;
            case GTMultiLineString:
                output = new GeoMultiLineString();
                break;
            case GTMultiPolygon:
                output = new GeoMultiPolygon();
                break;
            case GTGeometryCollection:
                output = new GeoGeometryCollection();
            case GTNone:
            default:
                break;
        }

        switch (type) {
            case GTPoint:
            case GTLineString:
            case GTPolygon:
            case GTMultiPoint:
            case GTMultiLineString:
            case GTMultiPolygon:
                JSONArray coordinates = jsonObject.getJSONArray(GEOJSON_COORDINATES);
                output.setCoordinatesFromJSON(coordinates);
                break;
            case GTGeometryCollection:
                JSONArray jsonGeometries = jsonObject.getJSONArray(GEOJSON_GEOMETRIES);
                output.setCoordinatesFromJSON(jsonGeometries);
                break;
            case GTNone:
            default:
                break;
        }

        return output;
    }

    public static int typeFromJSON(String jsonType)
    {
        if (jsonType.equals(GEOJSON_TYPE_Point)) {
            return GTPoint;

        } else if (jsonType.equals(GEOJSON_TYPE_LineString)) {
            return GTLineString;

        } else if (jsonType.equals(GEOJSON_TYPE_Polygon)) {
            return GTPolygon;

        } else if (jsonType.equals(GEOJSON_TYPE_MultiPoint)) {
            return GTMultiPoint;

        } else if (jsonType.equals(GEOJSON_TYPE_MultiLineString)) {
            return GTMultiLineString;

        } else if (jsonType.equals(GEOJSON_TYPE_MultiPolygon)) {
            return GTMultiPolygon;

        } else if (jsonType.equals(GEOJSON_TYPE_GeometryCollection)) {
            return GTGeometryCollection;

        } else {
            return GTNone;
        }
    }

    public static GeoGeometry fromBlob(byte[] raw)
            throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream in = new ByteArrayInputStream(raw);
        ObjectInputStream is = new ObjectInputStream(in);
        return (GeoGeometry) is.readObject();
    }

    public static GeoGeometry fromWKT(String wkt)
    {
        return null;
    }
}
