/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_COORDINATES;
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public abstract void setCoordinatesFromJSONStream(JsonReader reader, int crs)
            throws IOException;

    public abstract void setCoordinatesFromWKT(String wkt, int crs);


    public byte[] toBlobOld()
            throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(this);
        return out.toByteArray();
    }


    public byte[] toBlob()
            throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(out);
        write(dataOutputStream);
        return out.toByteArray();
    }


    public abstract String toWKT(boolean full);


    public boolean equals(Object o)
    {
        if (super.equals(o)) {
            return true;
        }
        GeoGeometry other = (GeoGeometry) o;
        return null != other && getType() == other.getType() && mCRS == other.getCRS();
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

    public abstract GeoGeometry simplify(double tolerance);

    public abstract GeoGeometry clip(GeoEnvelope envelope);

    public void write(DataOutputStream stream) throws IOException{
        stream.writeInt(getType());
        stream.writeInt(mCRS);
    }

    public void read(DataInputStream stream) throws IOException{
        mCRS = stream.readInt();
    }

    public abstract boolean isValid();

    public abstract double distance(GeoGeometry geometry);
}
