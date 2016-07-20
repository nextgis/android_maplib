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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;

import static com.nextgis.maplib.util.GeoConstants.*;


public class GeoPoint
        extends GeoGeometry
{
    protected static final long serialVersionUID = -1241179697270831762L;
    protected double mX;
    protected double mY;


    public GeoPoint()
    {
        mX = mY = 0.0;
    }


    public GeoPoint(
            double x,
            double y)
    {
        this.mX = x;
        this.mY = y;
    }


    public GeoPoint(final GeoPoint point)
    {
        this.mX = point.mX;
        this.mY = point.mY;
        this.mCRS = point.mCRS;
    }


    public final double getX()
    {
        return mX;
    }


    public void setX(double x)
    {
        mX = x;
    }


    public final double getY()
    {
        return mY;
    }


    public void setY(double y)
    {
        mY = y;
    }


    public void setCoordinates(
            double x,
            double y)
    {
        mX = x;
        mY = y;
    }


    public boolean equals(GeoPoint point)
    {
        return mX == point.mX && mY == point.mY;
    }

    public int compareTo(GeoPoint p) {
        if (this.mX == p.mX) {
            return Double.compare(this.mY, p.mY);
        } else {
            return Double.compare(this.mX, p.mX);
        }
    }

    @Override
    protected boolean rawProject(int toCrs)
    {
        switch (toCrs) {
            case CRS_WEB_MERCATOR:
                Geo.wgs84ToMercatorSphere(this);
                return super.rawProject(toCrs);
            case CRS_WGS84:
                Geo.mercatorToWgs84Sphere(this);
                return super.rawProject(toCrs);
            default:
                return false;
        }
    }


    @Override
    public GeoEnvelope getEnvelope()
    {
        return new GeoEnvelope(mX, mX, mY, mY);
    }


    @Override
    public JSONArray coordinatesToJSON()
            throws JSONException
    {
        JSONArray coordinates = new JSONArray();
        coordinates.put(mX);
        coordinates.put(mY);

        return coordinates;
    }


    @Override
    public final int getType()
    {
        return GeoConstants.GTPoint;
    }


    @Override
    public void setCoordinatesFromJSON(JSONArray coordinates)
            throws JSONException
    {
        mX = coordinates.getDouble(0);
        mY = coordinates.getDouble(1);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void setCoordinatesFromJSONStream(JsonReader reader, int crs) throws IOException {
        setCRS(crs);
        reader.beginArray();
        int pos = 0;
        while (reader.hasNext()) {
            if(pos == 0)
                mX = reader.nextDouble();
            else if(pos == 1)
                mY = reader.nextDouble();
            else
                reader.skipValue();
            pos++;
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
        int pos = wkt.indexOf(" ");
        mX = Double.parseDouble(wkt.substring(0, pos).trim());
        mY = Double.parseDouble(wkt.substring(pos, wkt.length()).trim());
    }


    public String toString()
    {
        return "X: " + mX + ", Y: " + mY;
    }


    @Override
    public String toWKT(boolean full)
    {
        if (full) {
            return String.format(Locale.US, "POINT (%.8f %.8f)", mX, mY);//"POINT ( " + mX + " " + mY + " )";
        } else {
            return String.format(Locale.US, "%.8f %.8f", mX, mY);//mX + " " + mY;
        }

    }


    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        GeoPoint otherPt = (GeoPoint) o;
        return getX() == otherPt.getX() && getY() == otherPt.getY();
    }


    @Override
    public boolean intersects(GeoEnvelope envelope)
    {
        return envelope.contains(this);
    }


    @Override
    public GeoGeometry copy()
    {
        return new GeoPoint(this);
    }


    @Override
    public void clear()
    {
        mX = mY = 0.0;
    }

    @Override
    public GeoGeometry simplify(double tolerance) {
        return new GeoPoint(this);
    }

    @Override
    public GeoGeometry clip(GeoEnvelope envelope) {
        return new GeoPoint(this);
    }

    @Override
    public void write(DataOutputStream stream) throws IOException {
        super.write(stream);
        stream.writeDouble(mX);
        stream.writeDouble(mY);
    }

    @Override
    public void read(DataInputStream stream) throws IOException{
        super.read(stream);
        mX = stream.readDouble();
        mY = stream.readDouble();
    }

    @Override
    public boolean isValid() {
        return inBounds();
    }

    public boolean inBounds() {
        if (getEnvelope().isInit()) {
            switch (mCRS) {
                case CRS_WGS84:
                    return mX >= -WGS_LONG_MAX && mX <= WGS_LONG_MAX && mY >= -WGS_LAT_MAX && mY < WGS_LAT_MAX;
                case CRS_WEB_MERCATOR:
                    return mX >= -MERCATOR_MAX && mX <= MERCATOR_MAX && mY >= -MERCATOR_MAX && mY < MERCATOR_MAX;
            }
        }

        return false;
    }

    @Override
    public double distance(GeoGeometry geometry) {
        if(geometry.getType() == GTPoint){
            GeoPoint pt = (GeoPoint) geometry;
            return Math.sqrt((pt.getX() - mX)*(pt.getX() - mX) + (pt.getY() - mY)*(pt.getY() - mY));
        }
        // TODO: 04.09.15 release for other types of geometries
        return 0;
    }
}
