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

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.GeoConstants.GTPolygon;


public class GeoPolygon
        extends GeoGeometry
{
    protected static final long serialVersionUID =-1241179697270831764L;
    protected GeoLinearRing mOuterRing;
    protected List<GeoLinearRing> mInnerRings;


    public GeoPolygon()
    {
        mOuterRing = new GeoLinearRing();
        mInnerRings = new ArrayList<>();
    }


    public void add(GeoPoint point)
    {
        mOuterRing.add(point);
    }


    public GeoPoint remove(int index)
    {
        return mOuterRing.remove(index);
    }


    @Override
    protected boolean rawProject(int toCrs)
    {
        if( mOuterRing.rawProject(toCrs) )
        {
            boolean isOk = true;
            for(GeoGeometry geometry : mInnerRings){
                isOk = isOk && geometry.rawProject(toCrs);
            }
            if(isOk)
                super.rawProject(toCrs);
            return isOk;
        }
        return false;
    }


    @Override
    public GeoEnvelope getEnvelope()
    {
        return mOuterRing.getEnvelope();
    }


    @Override
    public JSONArray coordinatesToJSON()
            throws JSONException
    {
        JSONArray coordinates = new JSONArray();
        coordinates.put(mOuterRing.coordinatesToJSON());
//TODO: add inner rings
        return coordinates;
    }


    @Override
    public int getType()
    {
        return GTPolygon;
    }


    @Override
    public void setCoordinatesFromJSON(JSONArray coordinates)
            throws JSONException
    {
        JSONArray outerRingCoordinates = coordinates.getJSONArray(0);

        if (outerRingCoordinates.length() < 4) {
            throw new JSONException(
                    "For type \"Polygon\", the \"coordinates\" member must be an array of LinearRing coordinate arrays. A LinearRing must be with 4 or more positions.");
        }

        mOuterRing.setCoordinatesFromJSON(outerRingCoordinates);

        if (!getOuterRing().isClosed()) {
            throw new JSONException(
                    "For type \"Polygon\", the \"coordinates\" member must be an array of LinearRing coordinate arrays. The first and last positions of LinearRing must be equivalent (they represent equivalent points).");
        }

        //TODO: add inner rings
    }


    @Override
    public void setCoordinatesFromWKT(String wkt)
    {
        if(wkt.contains("EMPTY"))
            return;

        if(wkt.startsWith("("))
            wkt = wkt.substring(1, wkt.length() - 1);
        //get outer ring
        int pos = wkt.indexOf(")");
        if(pos == Constants.NOT_FOUND) // no inner rings
            mOuterRing.setCoordinatesFromWKT(wkt);
        else
            mOuterRing.setCoordinatesFromWKT(wkt.substring(0, pos));
        pos = wkt.indexOf("(");
        while(pos != Constants.NOT_FOUND) {
            wkt = wkt.substring(pos + 1, wkt.length());
            pos = wkt.indexOf(")") - 1;
            if(pos < 1)
                return;

            GeoLinearRing innerRing = new GeoLinearRing();
            innerRing.setCoordinatesFromWKT(wkt.substring(0, pos));
            mInnerRings.add(innerRing);

            pos = wkt.indexOf("(");
        }
    }


    public GeoLinearRing getOuterRing()
    {
        return mOuterRing;
    }


    @Override
    public String toWKT(boolean full)
    {
        StringBuilder buf = new StringBuilder();
        if(full)
            buf.append("POLYGON ");
        if (mOuterRing.getPoints().size() == 0)
            buf.append(" EMPTY");
        else {
            buf.append("(");
            buf.append(mOuterRing.toWKT(false));
            if(mInnerRings.size() > 0) {
                buf.append(", ");
                for (int i = 0; i < mInnerRings.size(); i++) {
                    GeoLinearRing ring = mInnerRings.get(i);
                    buf.append(ring.toWKT(false));
                }
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
        GeoPolygon otherPlg = (GeoPolygon) o;
        if(!otherPlg.getOuterRing().equals(getOuterRing()))
            return false;
        for(int i = 0; i < mInnerRings.size(); i++){
            GeoLinearRing ring = mInnerRings.get(i);
            GeoLinearRing otherRing = otherPlg.getInnerRing(i);
            if(!ring.equals(otherRing))
                return false;
        }
        return true;
    }


    public GeoLinearRing getInnerRing(int index)
    {
        if(mInnerRings.size() > index)
            return mInnerRings.get(index);
        return null;
    }

    public int getInnerRingCount(){
        return mInnerRings.size();
    }


    @Override
    public boolean intersects(GeoEnvelope envelope)
    {
        if(super.intersects(envelope)){
            if(mOuterRing.intersects(envelope))
                return true;
            for(GeoLinearRing ring : mInnerRings){
                if(ring.intersects(envelope))
                    return true;
            }
        }
        return false;
    }
}
