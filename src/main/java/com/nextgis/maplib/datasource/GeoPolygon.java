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
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;

public class GeoPolygon
        extends GeoGeometry
{
    protected static final long serialVersionUID = -1241179697270831764L;
    protected GeoLinearRing       mOuterRing;
    protected List<GeoLinearRing> mInnerRings;


    public GeoPolygon()
    {
        mOuterRing = new GeoLinearRing();
        mInnerRings = new LinkedList<>();
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
        return GeoConstants.GTPolygon;
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void setCoordinatesFromJSONStream(JsonReader reader, int crs) throws IOException {
        setCRS(crs);
        boolean outerRingFilled = false;
        reader.beginArray();
        while (reader.hasNext()){
            if (!outerRingFilled) {
                mOuterRing.setCoordinatesFromJSONStream(reader, crs);
                outerRingFilled = true;
            } else {
                GeoLinearRing ring = new GeoLinearRing();
                ring.setCoordinatesFromJSONStream(reader, crs);
                mInnerRings.add(ring);
            }
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
        //get outer ring
        int pos = wkt.indexOf(")");
        if (pos == Constants.NOT_FOUND) // no inner rings
        {
            mOuterRing.setCoordinatesFromWKT(wkt, crs);
        } else {
            mOuterRing.setCoordinatesFromWKT(wkt.substring(0, pos), crs);
        }
        pos = wkt.indexOf("(");
        while (pos != Constants.NOT_FOUND) {
            wkt = wkt.substring(pos + 1, wkt.length());
            pos = wkt.indexOf(")") - 1;
            if (pos < 1) {
                return;
            }

            GeoLinearRing innerRing = new GeoLinearRing();
            innerRing.setCoordinatesFromWKT(wkt.substring(0, pos), crs);
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
                if (ring.contains(envelope)) {
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

    @Override
    public GeoGeometry simplify(double tolerance) {
        GeoGeometry outerRingSimple = mOuterRing.simplify(tolerance);
        if(outerRingSimple == null) // this is point
            return null;

        GeoPolygon result = new GeoPolygon();
        result.setCRS(mCRS);
        result.mOuterRing = (GeoLinearRing) outerRingSimple;
        for(GeoLinearRing ring : mInnerRings){
            GeoLinearRing innerRingSimple = ring.simplify(tolerance, true);
            if(innerRingSimple == null) // skip points
                continue;
            result.addInnerRing(innerRingSimple);
        }

        return result;
    }

    @Override
    public GeoGeometry clip(GeoEnvelope envelope) {
        GeoPolygon result = new GeoPolygon();
        result.setCRS(mCRS);
        result.mOuterRing = (GeoLinearRing) mOuterRing.clip(envelope);

        if(result.mOuterRing.getPointCount() < 4)
            return null;

        for(GeoLinearRing ring : mInnerRings){
            GeoLinearRing geometry = (GeoLinearRing) ring.clip(envelope);
            if(null != geometry)
                result.addInnerRing(geometry);
        }

        return result;
    }

    @Override
    public void read(DataInputStream stream) throws IOException {
        super.read(stream);
        mOuterRing = (GeoLinearRing) GeoGeometryFactory.fromDataStream(stream);
        int innerRingCount = stream.readInt();
        for (int i = 0; i < innerRingCount; i++){
            GeoGeometry geometry = GeoGeometryFactory.fromDataStream(stream);
            if(null != geometry && geometry instanceof GeoLinearRing){
                mInnerRings.add((GeoLinearRing) geometry);
            }
        }
    }

    @Override
    public boolean isValid() {
        for (GeoLinearRing ring : mInnerRings)
            if (!ring.isValid())
                return false;

        return mOuterRing.isValid();
    }

    @Override
    public double distance(GeoGeometry geometry) {
        return mOuterRing.distance(geometry);
    }

    @Override
    public void write(DataOutputStream stream) throws IOException {
        super.write(stream);
        mOuterRing.write(stream);
        int innerRingCount = mInnerRings.size();
        stream.writeInt(innerRingCount);
        for (int i = 0; i < innerRingCount; i++){
            GeoLinearRing ring = mInnerRings.get(i);
            ring.write(stream);
        }
    }

    public void closeRings() {
        mOuterRing.closeRing();

        for (int i = 0; i < getInnerRingCount(); i++)
            getInnerRing(i).closeRing();
    }

    public boolean intersects() {
        if (mOuterRing.intersects())
            return true;

        for (GeoLinearRing ring : mInnerRings)
            if (ring.intersects())
                return true;

        return false;
    }

    public boolean isHolesInside() {
        mOuterRing.closeRing();

        for (GeoLinearRing ring: mInnerRings) {
            ring.closeRing();

            if (!mOuterRing.getEnvelope().contains(ring.getEnvelope()))
                return false;

            for (int i = 0; i < ring.getPointCount(); i++)
                if (!contains(ring.getPoint(i)))
                    return false;
        }

        return true;
    }

    public boolean isHolesIntersect() {
        for (int i = 0; i < mInnerRings.size() - 1; i++)
            for (int j = i + 1; j < mInnerRings.size(); j++)
                if (mInnerRings.get(i).intersects(mInnerRings.get(j)))
                    return true;

        return false;
    }

    // https://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
    // http://stackoverflow.com/a/2922778
    public boolean contains(GeoPoint point) {
        int i, j;
        double vertiy, vertix, vertjy, vertjx;
        boolean c = false;

        for (i = 0, j = mOuterRing.getPointCount() - 1; i < mOuterRing.getPointCount(); j = i++) {
            vertix = mOuterRing.getPoints().get(i).getX();
            vertiy = mOuterRing.getPoints().get(i).getY();
            vertjx = mOuterRing.getPoints().get(j).getX();
            vertjy = mOuterRing.getPoints().get(j).getY();

            if (((vertiy > point.getY()) != (vertjy > point.getY())) &&
                    (point.getX() < (vertjx - vertix) * (point.getY() - vertiy) / (vertjy - vertiy) + vertix))
                c = !c;
        }

        return c;
    }

    public double getPerimeter() {
        if (mOuterRing != null)
            return mOuterRing.getLength();
        else
            return 0;
    }

    public double getArea() {
        return getArea(this);
    }

    private static double getArea(GeoPolygon polygon) {
        double area = getArea(polygon.getOuterRing());

        for (GeoLinearRing ring : polygon.mInnerRings)
            area -= getArea(ring);

        return area;
    }

    // based on https://github.com/googlemaps/android-maps-utils/blob/master/library/src/com/google/maps/android/SphericalUtil.java
    /**
     * Returns the signed area of a closed path on a sphere of given radius.
     * The computed area uses the same units as the radius squared.
     * Used by SphericalUtilTest.
     */
    private static double getArea(GeoLinearRing ring) {
        if (ring == null)
            return 0;

        List<GeoPoint> coordinates = ring.getPoints();
        int size = coordinates.size();
        if (size < 3)
            return 0;

        double total = 0;
        GeoPoint p = (GeoPoint) coordinates.get(size - 1).copy();
        p.setCRS(CRS_WEB_MERCATOR);
        p.project(CRS_WGS84);

        double prevTanLat = Math.tan((Math.PI / 2 - Math.toRadians(p.getY())) / 2);
        double prevLng = Math.toRadians(p.getX());
        // For each edge, accumulate the signed area of the triangle formed by the North Pole
        // and that edge ("polar triangle").
        for (GeoPoint point : coordinates) {
            p = (GeoPoint) point.copy();
            p.setCRS(CRS_WEB_MERCATOR);
            p.project(CRS_WGS84);

            double tanLat = Math.tan((Math.PI / 2 - Math.toRadians(p.getY())) / 2);
            double lng = Math.toRadians(p.getX());
            total += polarTriangleArea(tanLat, lng, prevTanLat, prevLng);
            prevTanLat = tanLat;
            prevLng = lng;
        }

        return Math.abs(total) * (6378137f * 6378137f);
    }

    /**
     * Returns the signed area of a triangle which has North Pole as a vertex.
     * Formula derived from "Area of a spherical triangle given two edges and the included angle"
     * as per "Spherical Trigonometry" by Todhunter, page 71, section 103, point 2.
     * See http://books.google.com/books?id=3uBHAAAAIAAJ&pg=PA71
     * The arguments named "tan" are tan((pi/2 - latitude)/2).
     */
    private static double polarTriangleArea(double tan1, double lng1, double tan2, double lng2) {
        double deltaLng = lng1 - lng2;
        double t = tan1 * tan2;
        return 2 * Math.atan2(t * Math.sin(deltaLng), 1 + t * Math.cos(deltaLng));
    }

    public void setInnerRing(int location, GeoLinearRing ring) {
        mInnerRings.set(location, ring);
    }

    public void setOuterRing(GeoLinearRing ring) {
        mOuterRing = ring;
    }
}
