/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplib.datasource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.GeoConstants.*;

public abstract class GeoGeometry {

    protected int mCRS;

    public abstract int getType();

    public static int typeFromJSON(String jsonType) {
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

        } else return GTNone;
    }

    public String typeToJSON() {
        switch (getType()) {
            case GTPoint:
                return GEOJSON_TYPE_Point;
            case GTLineString:
                return GEOJSON_TYPE_LineString;
            case GTPolygon:
                return GEOJSON_TYPE_Polygon;
            case GTMultiPoint:
                return GEOJSON_TYPE_MultiPoint;
            case GTMultiLineString:
                return GEOJSON_TYPE_MultiLineString;
            case GTMultiPolygon:
                return GEOJSON_TYPE_MultiPolygon;
            case GTGeometryCollection:
                return GEOJSON_TYPE_GeometryCollection;
            case GTNone:
            default:
                return "";
        }
    }

    public boolean project(int toCrs) {
        return (mCRS == CRS_WGS84 && toCrs == CRS_WEB_MERCATOR
                || mCRS == CRS_WEB_MERCATOR && toCrs == CRS_WGS84) && rawProject(toCrs);
    }

    protected abstract boolean rawProject(int toCrs);

    public abstract GeoEnvelope getEnvelope();

    public void setCRS(int crs) {
        mCRS = crs;
    }

    public static GeoGeometry fromJson(JSONObject jsonObject) throws JSONException {
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
                GeoGeometryCollection geometryCollection = new GeoGeometryCollection();
                JSONArray jsonGeometries = jsonObject.getJSONArray(GEOJSON_GEOMETRIES);

                for (int i = 0; i < jsonGeometries.length(); ++i) {
                    JSONObject jsonGeometry = jsonGeometries.getJSONObject(i);
                    GeoGeometry geometry = GeoGeometry.fromJson(jsonGeometry);
                    geometryCollection.add(geometry);
                }

                output = geometryCollection;
                break;

            case GTNone:
            default:
                break;
        }

        return output;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonOutObject = new JSONObject();
        jsonOutObject.put(GEOJSON_TYPE, typeToJSON());
        jsonOutObject.put(GEOJSON_COORDINATES, coordinatesToJSON());

        return jsonOutObject;
    }

    public abstract void setCoordinatesFromJSON(JSONArray coordinates) throws JSONException;
    public abstract JSONArray coordinatesToJSON() throws JSONException, ClassCastException;
}
