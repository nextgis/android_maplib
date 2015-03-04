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

import com.nextgis.maplib.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;

import static com.nextgis.maplib.util.GeoConstants.*;


public class GeoPoint
        extends GeoGeometry
{
    protected static final long serialVersionUID =-1241179697270831762L;
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
        return GTPoint;
    }


    @Override
    public void setCoordinatesFromJSON(JSONArray coordinates)
            throws JSONException
    {
        mX = coordinates.getDouble(0);
        mY = coordinates.getDouble(1);
    }


    @Override
    public void setCoordinatesFromWKT(String wkt)
    {
        if(wkt.contains("EMPTY"))
            return;

        if(wkt.startsWith("("))
            wkt = wkt.substring(1, wkt.length() - 1);
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
        if(full)
            return "POINT ( " + mX + " " + mY + " )";
        else
            return mX + " " + mY;
    }


    @Override
    public boolean equals(Object o)
    {
        if(! super.equals(o) )
            return false;
        GeoPoint otherPt = (GeoPoint)o;
        return getX() == otherPt.getX() && getY() == otherPt.getY();
    }


    @Override
    public boolean intersects(GeoEnvelope envelope)
    {
        return super.intersects(envelope) && envelope.getMinX() < getX() &&
               envelope.getMaxX() > getX() && envelope.getMinY() < getY() &&
               envelope.getMaxY() > getY();
    }
}
