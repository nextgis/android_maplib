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

import com.nextgis.maplib.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.GeoConstants.GTPolygon;


public class GeoPolygon
        extends GeoGeometry
{
    protected static final long serialVersionUID = -1241179697270831764L;
    protected GeoLinearRing       mOuterRing;
    protected List<GeoLinearRing> mInnerRings;


    public GeoPolygon()
    {
        mOuterRing = new GeoLinearRing();
        mInnerRings = new ArrayList<>();
    }


    public GeoPolygon(GeoPolygon polygon)
    {
        mOuterRing = (GeoLinearRing) polygon.mOuterRing.copy();
        mInnerRings = new ArrayList<>();
        for (GeoLinearRing ring : polygon.mInnerRings) {
            mInnerRings.add((GeoLinearRing) ring.copy());
        }
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
        if (mOuterRing.rawProject(toCrs)) {
            boolean isOk = true;
            for (GeoGeometry geometry : mInnerRings) {
                isOk = isOk && geometry.rawProject(toCrs);
            }
            if (isOk) {
                super.rawProject(toCrs);
            }
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

        for (GeoLinearRing innerRing : mInnerRings) {
            coordinates.put(innerRing.coordinatesToJSON());
        }

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
        JSONArray ringCoordinates = coordinates.getJSONArray(0);

        if (ringCoordinates.length() < 4) {
            throw new JSONException(
                    "For type \"Polygon\", the \"coordinates\" member must be an array of LinearRing coordinate arrays. A LinearRing must be with 4 or more positions.");
        }

        mOuterRing.setCoordinatesFromJSON(ringCoordinates);

        if (!getOuterRing().isClosed()) {
            throw new JSONException(
                    "For type \"Polygon\", the \"coordinates\" member must be an array of LinearRing coordinate arrays. The first and last positions of LinearRing must be equivalent (they represent equivalent points).");
        }

        GeoLinearRing innerRing;

        for (int i = 1; i < coordinates.length(); i++) {
            ringCoordinates = coordinates.getJSONArray(i);

            if (ringCoordinates.length() < 4) {
                throw new JSONException(
                        "For type \"Polygon\", the \"coordinates\" member must be an array of LinearRing coordinate arrays. A LinearRing must be with 4 or more positions.");
            }

            innerRing = new GeoLinearRing();
            innerRing.setCoordinatesFromJSON(ringCoordinates);

            if (!innerRing.isClosed()) {
                throw new JSONException(
                        "For type \"Polygon\", the \"coordinates\" member must be an array of LinearRing coordinate arrays. The first and last positions of LinearRing must be equivalent (they represent equivalent points).");
            }

            addInnerRing(innerRing);
        }
    }


    @Override
    public void setCoordinatesFromWKT(String wkt)
    {
        if (wkt.contains("EMPTY")) {
            return;
        }

        if (wkt.startsWith("(")) {
            wkt = wkt.substring(1, wkt.length() - 1);
        }
        //get outer ring
        int pos = wkt.indexOf(")");
        if (pos == Constants.NOT_FOUND) // no inner rings
        {
            mOuterRing.setCoordinatesFromWKT(wkt);
        } else {
            mOuterRing.setCoordinatesFromWKT(wkt.substring(0, pos));
        }
        pos = wkt.indexOf("(");
        while (pos != Constants.NOT_FOUND) {
            wkt = wkt.substring(pos + 1, wkt.length());
            pos = wkt.indexOf(")") - 1;
            if (pos < 1) {
                return;
            }

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
        if (full) {
            buf.append("POLYGON ");
        }
        if (mOuterRing.getPoints().size() == 0) {
            buf.append(" EMPTY");
        } else {
            buf.append("(");
            buf.append(mOuterRing.toWKT(false));
            if (mInnerRings.size() > 0) {
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
        if (!super.equals(o)) {
            return false;
        }
        GeoPolygon otherPlg = (GeoPolygon) o;
        if (!otherPlg.getOuterRing().equals(getOuterRing())) {
            return false;
        }
        for (int i = 0; i < mInnerRings.size(); i++) {
            GeoLinearRing ring = mInnerRings.get(i);
            GeoLinearRing otherRing = otherPlg.getInnerRing(i);
            if (!ring.equals(otherRing)) {
                return false;
            }
        }
        return true;
    }


    public GeoLinearRing getInnerRing(int index)
    {
        if (mInnerRings.size() > index) {
            return mInnerRings.get(index);
        }
        return null;
    }


    public int getInnerRingCount()
    {
        return mInnerRings.size();
    }


    public void removeInnerRing(int index)
    {
        mInnerRings.remove(index);
    }


    public void addInnerRing(GeoLinearRing ring)
    {
        mInnerRings.add(ring);
    }


    @Override
    public boolean intersects(GeoEnvelope envelope)
    {
        if (super.intersects(envelope)) {
            //check if inside outer ring but not in hole

            boolean intersects = mOuterRing.intersects(envelope);
            if (!intersects) {
                return false;
            }

            for (GeoLinearRing ring : mInnerRings) {
                if (ring.intersects(envelope)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    @Override
    public GeoGeometry copy()
    {
        return new GeoPolygon(this);
    }


    @Override
    public void clear()
    {
        mOuterRing.clear();
        mInnerRings.clear();
    }
}
