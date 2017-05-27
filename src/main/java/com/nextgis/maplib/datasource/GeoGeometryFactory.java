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

import com.nextgis.maplib.util.GeoConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import static com.nextgis.maplib.util.GeoConstants.GEOJSON_COORDINATES;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_GEOMETRIES;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_GeometryCollection;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_LineString;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_MultiLineString;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_MultiPoint;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_MultiPolygon;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_Point;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_Polygon;
import static com.nextgis.maplib.util.GeoConstants.GTGeometryCollection;
import static com.nextgis.maplib.util.GeoConstants.GTLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPoint;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPolygon;
import static com.nextgis.maplib.util.GeoConstants.GTNone;
import static com.nextgis.maplib.util.GeoConstants.GTPoint;
import static com.nextgis.maplib.util.GeoConstants.GTPolygon;


/**
 * The class to create GeoGeometry instances from various sources
 */
public class GeoGeometryFactory
{
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static GeoGeometry fromJsonStream(JsonReader reader, int crs) throws IOException {
        GeoGeometry geometry = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if(name.equalsIgnoreCase(GeoConstants.GEOJSON_TYPE)){
                int type = typeFromString(reader.nextString());
                switch (type) {
                    case GTPoint:
                        geometry = new GeoPoint();
                        break;
                    case GTLineString:
                        geometry = new GeoLineString();
                        break;
                    case GTPolygon:
                        geometry = new GeoPolygon();
                        break;
                    case GTMultiPoint:
                        geometry = new GeoMultiPoint();
                        break;
                    case GTMultiLineString:
                        geometry = new GeoMultiLineString();
                        break;
                    case GTMultiPolygon:
                        geometry = new GeoMultiPolygon();
                        break;
                    case GTGeometryCollection:
                        geometry = new GeoGeometryCollection();
                    case GTNone:
                    default:
                        break;
                }
            } else if(name.equalsIgnoreCase(GeoConstants.GEOJSON_COORDINATES)) {
                if (geometry == null)
                    reader.skipValue();
                else
                    geometry.setCoordinatesFromJSONStream(reader, crs);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        if (geometry != null) {
            geometry.setCRS(crs);
            if (!geometry.isValid())
                return null;
        }

        return geometry;
    }

    public static GeoGeometry fromJson(JSONObject jsonObject)
            throws JSONException
    {
        String jsonType = jsonObject.getString(GEOJSON_TYPE);
        int type = typeFromString(jsonType);

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


    public static int typeFromString(String jsonType)
    {
        switch (jsonType) {
            case "POINT":
            case GEOJSON_TYPE_Point:
                return GTPoint;

            case "LINESTRING":
            case GEOJSON_TYPE_LineString:
                return GTLineString;

            case "POLYGON":
            case GEOJSON_TYPE_Polygon:
                return GTPolygon;

            case "MULTIPOINT":
            case GEOJSON_TYPE_MultiPoint:
                return GTMultiPoint;

            case "MULTILINESTRING":
            case GEOJSON_TYPE_MultiLineString:
                return GTMultiLineString;

            case "MULTIPOLYGON":
            case GEOJSON_TYPE_MultiPolygon:
                return GTMultiPolygon;

            case "GEOMETRYCOLLECTION":
            case GEOJSON_TYPE_GeometryCollection:
                return GTGeometryCollection;

            default:
                return GTNone;
        }
    }

    public static String typeToString(int type) {
        switch (type) {
            case GTPoint:
                return "POINT";

            case GTLineString:
                return "LINESTRING";

            case GTPolygon:
                return "POLYGON";

            case GTMultiPoint:
                return "MULTIPOINT";

            case GTMultiLineString:
                return "MULTILINESTRING";

            case GTMultiPolygon:
                return "MULTIPOLYGON";

            case GTGeometryCollection:
                return "GEOMETRYCOLLECTION";

            default:
                return "";
        }
    }

    public static GeoGeometry fromBlobOld(byte[] raw)
            throws IOException, ClassNotFoundException
    {
        if (null == raw) {
            return null;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(raw);
        ObjectInputStream is = new ObjectInputStream(in);
        return (GeoGeometry) is.readObject();
    }

    public static GeoGeometry fromBlob(byte[] raw)
            throws IOException, ClassNotFoundException
    {
        if (null == raw) {
            return null;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(raw);
        DataInputStream dataInputStream = new DataInputStream(in);
        return fromDataStream(dataInputStream);
    }


    public static GeoGeometry fromWKT(String wkt, int crs)
    {
        GeoGeometry output = null;
        wkt = wkt.trim();
        if (wkt.startsWith("POINT")) {
            output = new GeoPoint();
            output.setCoordinatesFromWKT(wkt.substring(5).trim(), crs);
        } else if (wkt.startsWith("LINESTRING")) {
            output = new GeoLineString();
            output.setCoordinatesFromWKT(wkt.substring(10).trim(), crs);
        } else if (wkt.startsWith("LINEARRING")) {
            output = new GeoLinearRing();
            output.setCoordinatesFromWKT(wkt.substring(10).trim(), crs);
        } else if (wkt.startsWith("POLYGON")) {
            output = new GeoPolygon();
            output.setCoordinatesFromWKT(wkt.substring(8, wkt.length() - 1).trim(), crs);
        } else if (wkt.startsWith("MULTIPOINT")) {
            output = new GeoMultiPoint();
            output.setCoordinatesFromWKT(wkt.substring(10).trim(), crs);
        } else if (wkt.startsWith("MULTILINESTRING")) {
            output = new GeoMultiLineString();
            output.setCoordinatesFromWKT(wkt.substring(15).trim(), crs);
        } else if (wkt.startsWith("MULTIPOLYGON")) {
            output = new GeoMultiPolygon();
            output.setCoordinatesFromWKT(wkt.substring(12).trim(), crs);
        } else if (wkt.startsWith("GEOMETRYCOLLECTION")) {
            output = new GeoGeometryCollection();
            output.setCoordinatesFromWKT(wkt.substring(18).trim(), crs);
        }

        if (output != null)
            output.setCRS(crs);

        return output;
    }

    public static GeoGeometry fromDataStream(DataInputStream stream) throws IOException {
        int geometryType = stream.readInt();
        GeoGeometry result = null;
        switch (geometryType){
            case GeoConstants.GTPoint:
                result = new GeoPoint();
                result.read(stream);
                break;
            case GeoConstants.GTLineString:
                result = new GeoLineString();
                result.read(stream);
                break;
            case GeoConstants.GTLinearRing:
                result = new GeoLinearRing();
                result.read(stream);
                break;
            case GeoConstants.GTPolygon:
                result = new GeoPolygon();
                result.read(stream);
                break;
            case GeoConstants.GTMultiPoint:
                result = new GeoMultiPoint();
                result.read(stream);
                break;
            case GeoConstants.GTMultiLineString:
                result = new GeoMultiLineString();
                result.read(stream);
                break;
            case GeoConstants.GTMultiPolygon:
                result = new GeoMultiPolygon();
                result.read(stream);
                break;
            case GeoConstants.GTGeometryCollection:
                result = new GeoGeometryCollection();
                result.read(stream);
                break;
        }
        return result;
    }
}
