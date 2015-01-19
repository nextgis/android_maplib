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

import static com.nextgis.maplib.util.GeoConstants.*;


public class GeoPoint
        extends GeoGeometry
{

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
}
