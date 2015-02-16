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

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.GeoConstants.GTLineString;


public class GeoLineString
        extends GeoGeometry
{

    protected List<GeoPoint> mPoints;


    public GeoLineString()
    {
        mPoints = new ArrayList<GeoPoint>();
    }


    public List<GeoPoint> getPoints()
    {
        return mPoints;
    }


    public GeoPoint remove(int index)
    {
        return mPoints.remove(index);
    }


    @Override
    protected boolean rawProject(int toCrs)
    {
        boolean isOk = true;
        for (GeoPoint point : mPoints) {
            isOk = isOk && point.rawProject(toCrs);
        }
        if(isOk)
            super.rawProject(toCrs);
        return isOk;
    }


    @Override
    public GeoEnvelope getEnvelope()
    {
        GeoEnvelope envelope = new GeoEnvelope();

        for (GeoPoint point : mPoints) {
            envelope.merge(point.getEnvelope());
        }

        return envelope;
    }


    @Override
    public JSONArray coordinatesToJSON()
            throws JSONException
    {
        JSONArray coordinates = new JSONArray();

        for (GeoPoint point : this.mPoints) {
            coordinates.put(point.coordinatesToJSON());
        }

        return coordinates;
    }


    @Override
    public int getType()
    {
        return GTLineString;
    }


    @Override
    public void setCoordinatesFromJSON(JSONArray coordinates)
            throws JSONException
    {
        if (coordinates.length() < 2) {
            throw new JSONException(
                    "For type \"LineString\", the \"coordinates\" member must be an array of two or more positions.");
        }

        for (int i = 0; i < coordinates.length(); ++i) {
            GeoPoint point = new GeoPoint();
            point.setCoordinatesFromJSON(coordinates.getJSONArray(i));
            add(point);
        }
    }


    @Override
    public void setCoordinatesFromWKT(String wkt)
    {
        if(wkt.contains("EMPTY"))
            return;

        if(wkt.startsWith("("))
            wkt = wkt.substring(1, wkt.length() - 1);

        for (String token : wkt.split(",")) {
            GeoPoint point = new GeoPoint();
            point.setCoordinatesFromWKT(token.trim());
            add(point);
        }
    }


    public void add(GeoPoint point)
            throws IllegalArgumentException
    {
        if (point == null) {
            throw new IllegalArgumentException("GeoLineString: point == null.");
        }

        mPoints.add(point);
    }


    @Override
    public String toWKT(boolean full)
    {
        StringBuilder buf = new StringBuilder();
        if(full)
            buf.append("LINESTRING ");
        if (mPoints.size() == 0)
            buf.append(" EMPTY");
        else {
            buf.append("(");
            for (int i = 0; i < mPoints.size(); i++) {
                if (i > 0)
                    buf.append(", ");
                GeoPoint pt = mPoints.get(i);
                buf.append(pt.toWKT(false));
            }
            buf.append(")");
        }
        return buf.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o))
            return false;
        GeoLineString otherLn = (GeoLineString) o;
        for(int i = 0; i < mPoints.size(); i++){
            GeoPoint pt = mPoints.get(i);
            GeoPoint otherPt = otherLn.getPoint(i);
            if(!pt.equals(otherPt))
                return false;
        }
        return true;
    }


    public GeoPoint getPoint(int index)
    {
        if(index < mPoints.size())
            return mPoints.get(index);
        return null;
    }
}
