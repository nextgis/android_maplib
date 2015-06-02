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
package com.nextgis.maplib.datasource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static com.nextgis.maplib.util.GeoConstants.*;


public abstract class GeoGeometry
        implements Serializable
{
    protected static final long serialVersionUID = -1241179697270831761L;
    protected int mCRS;


    public boolean project(int toCrs)
    {
        return (mCRS == CRS_WGS84 && toCrs == CRS_WEB_MERCATOR ||
                mCRS == CRS_WEB_MERCATOR && toCrs == CRS_WGS84) && rawProject(toCrs);
    }


    protected boolean rawProject(int toCrs)
    {
        mCRS = toCrs;
        return true;
    }


    public abstract GeoEnvelope getEnvelope();


    public void setCRS(int crs)
    {
        mCRS = crs;
    }


    public int getCRS()
    {
        return mCRS;
    }


    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject jsonOutObject = new JSONObject();
        jsonOutObject.put(GEOJSON_TYPE, typeToJSON());
        jsonOutObject.put(GEOJSON_COORDINATES, coordinatesToJSON());

        return jsonOutObject;
    }


    public String typeToJSON()
    {
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


    public abstract JSONArray coordinatesToJSON()
            throws JSONException, ClassCastException;


    public abstract int getType();


    public abstract void setCoordinatesFromJSON(JSONArray coordinates)
            throws JSONException;

    public abstract void setCoordinatesFromWKT(String wkt);


    public byte[] toBlob()
            throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(this);
        return out.toByteArray();
    }


    public abstract String toWKT(boolean full);


    public boolean equals(Object o)
    {
        if (super.equals(o)) {
            return true;
        }
        GeoGeometry other = (GeoGeometry) o;
        return null != other && getType() == other.getType();
    }


    public boolean intersects(GeoEnvelope envelope)
    {
        return getEnvelope().intersects(envelope);
    }


    /**
     * Make deep copy of geometry
     *
     * @return The geometry copy
     */
    public abstract GeoGeometry copy();

    /**
     * remove all points from geometry
     */
    public abstract void clear();
}
