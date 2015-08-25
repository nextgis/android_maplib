/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * The simplify algorithm adapted from simplify-java project under the MIT license
 * Copyright (c) 2013 Heinrich Göbl
 * @see https://github.com/hgoebl/simplify-java
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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import static com.nextgis.maplib.util.GeoConstants.GTLineString;


public class GeoLineString
        extends GeoGeometry
{
    protected static final long serialVersionUID = -1241179697270831763L;
    protected List<GeoPoint> mPoints;


    public GeoLineString()
    {
        mPoints = new LinkedList<>();
    }


    public GeoLineString(GeoLineString geoLineString)
    {
        mPoints = new LinkedList<>();
        for (GeoPoint point : geoLineString.mPoints) {
            mPoints.add((GeoPoint) point.copy());
        }
    }


    public int getPointCount()
    {
        if (null == mPoints) {
            return 0;
        }
        return mPoints.size();
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
        if (isOk) {
            super.rawProject(toCrs);
        }
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
        if (wkt.contains("EMPTY")) {
            return;
        }

        if (wkt.startsWith("(")) {
            wkt = wkt.substring(1, wkt.length() - 1);
        }

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
        if (full) {
            buf.append("LINESTRING ");
        }
        if (mPoints.size() == 0) {
            buf.append(" EMPTY");
        } else {
            buf.append("(");
            for (int i = 0; i < mPoints.size(); i++) {
                if (i > 0) {
                    buf.append(", ");
                }
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
        if (!super.equals(o)) {
            return false;
        }
        GeoLineString otherLn = (GeoLineString) o;
        for (int i = 0; i < mPoints.size(); i++) {
            GeoPoint pt = mPoints.get(i);
            GeoPoint otherPt = otherLn.getPoint(i);
            if (!pt.equals(otherPt)) {
                return false;
            }
        }
        return true;
    }


    public GeoPoint getPoint(int index)
    {
        if (index < mPoints.size()) {
            return mPoints.get(index);
        }
        return null;
    }


    @Override
    public boolean intersects(GeoEnvelope envelope)
    {
        if (super.intersects(envelope)) {

            if(envelope.contains(getEnvelope()))
                return true;

            GeoPoint pte1 = new GeoPoint(envelope.getMinX(), envelope.getMaxY());
            GeoPoint pte2 = new GeoPoint(envelope.getMaxX(), envelope.getMaxY());
            GeoPoint pte3 = new GeoPoint(envelope.getMaxX(), envelope.getMinY());
            GeoPoint pte4 = new GeoPoint(envelope.getMinX(), envelope.getMinY());

            for (int i = 0; i < mPoints.size() - 1; i++) {
                GeoPoint pt1 = mPoints.get(i);
                GeoPoint pt2 = mPoints.get(i + 1);

                //test top
                if (linesIntersect(
                        pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY(), pte1.getX(), pte1.getY(),
                        pte2.getX(), pte2.getY())) {
                    return true;
                }
                //test left
                if (linesIntersect(
                        pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY(), pte2.getX(), pte2.getY(),
                        pte3.getX(), pte3.getY())) {
                    return true;
                }
                //test right
                if (linesIntersect(
                        pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY(), pte3.getX(), pte3.getY(),
                        pte4.getX(), pte4.getY())) {
                    return true;
                }
                //test bottom
                if (linesIntersect(
                        pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY(), pte4.getX(), pte4.getY(),
                        pte1.getX(), pte1.getY())) {
                    return true;
                }
            }
        }
        return false;
    }


    protected boolean linesIntersect(
            double x1,
            double y1,
            double x2,
            double y2,
            double x3,
            double y3,
            double x4,
            double y4)
    {
        // Return false if either of the lines have zero length
        if (x1 == x2 && y1 == y2 || x3 == x4 && y3 == y4) {
            return false;
        }
        // Fastest method, based on Franklin Antonio's "Faster Line Segment Intersection" topic "in Graphics Gems III" book (http://www.graphicsgems.org/)
        double ax = x2 - x1;
        double ay = y2 - y1;
        double bx = x3 - x4;
        double by = y3 - y4;
        double cx = x1 - x3;
        double cy = y1 - y3;

        double alphaNumerator = by * cx - bx * cy;
        double commonDenominator = ay * bx - ax * by;
        if (commonDenominator > 0) {
            if (alphaNumerator < 0 || alphaNumerator > commonDenominator) {
                return false;
            }
        } else if (commonDenominator < 0) {
            if (alphaNumerator > 0 || alphaNumerator < commonDenominator) {
                return false;
            }
        }
        double betaNumerator = ax * cy - ay * cx;
        if (commonDenominator > 0) {
            if (betaNumerator < 0 || betaNumerator > commonDenominator) {
                return false;
            }
        } else if (commonDenominator < 0) {
            if (betaNumerator > 0 || betaNumerator < commonDenominator) {
                return false;
            }
        }
        if (commonDenominator == 0) {
            // This code wasn't in Franklin Antonio's method. It was added by Keith Woodward.
            // The lines are parallel.
            // Check if they're collinear.
            double y3LessY1 = y3 - y1;
            double collinearityTestForP3 = x1 * (y2 - y3) + x2 * (y3LessY1) + x3 * (y1 -
                                                                                    y2);   // see http://mathworld.wolfram.com/Collinear.html
            // If p3 is collinear with p1 and p2 then p4 will also be collinear, since p1-p2 is parallel with p3-p4
            if (collinearityTestForP3 == 0) {
                // The lines are collinear. Now check if they overlap.
                if (x1 >= x3 && x1 <= x4 || x1 <= x3 && x1 >= x4 ||
                    x2 >= x3 && x2 <= x4 || x2 <= x3 && x2 >= x4 ||
                    x3 >= x1 && x3 <= x2 || x3 <= x1 && x3 >= x2) {
                    if (y1 >= y3 && y1 <= y4 || y1 <= y3 && y1 >= y4 ||
                        y2 >= y3 && y2 <= y4 || y2 <= y3 && y2 >= y4 ||
                        y3 >= y1 && y3 <= y2 || y3 <= y1 && y3 >= y2) {
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }


    @Override
    public GeoGeometry copy()
    {
        return new GeoLineString(this);
    }


    @Override
    public void clear()
    {
        mPoints.clear();
    }


    public GeoLineString simplify(double tolerance){
        double sqTolerance = tolerance * tolerance;
        return simplifyRadialDistance(sqTolerance);
    }

    protected GeoLineString simplifyRadialDistance(double sqTolerance){
        if(mPoints.isEmpty())
            return null;

        GeoPoint point = null;
        GeoPoint prevPoint = mPoints.get(0);

        GeoLineString result = new GeoLineString();
        result.setCRS(mCRS);
        result.add(prevPoint);

        for (int i = 1; i < mPoints.size(); ++i) {
            point = mPoints.get(i);

            if (getSquareDistance(point, prevPoint) > sqTolerance) {
                result.add(point);
                prevPoint = point;
            }
        }

        if (prevPoint != point) {
            result.add(point);
        }

        return result;
    }

    protected double getSquareDistance(GeoPoint p1, GeoPoint p2){
        double dx = p1.getX() - p2.getX();
        double dy = p1.getY() - p2.getY();

        return dx * dx + dy * dy;
    }


    protected GeoLineString simplifyDouglasPeucker(double sqTolerance){
        BitSet bitSet = new BitSet(mPoints.size());
        bitSet.set(0);
        bitSet.set(mPoints.size() - 1);

        List<Range> stack = new LinkedList<>();
        stack.add(new Range(0, mPoints.size() - 1));

        while (!stack.isEmpty()) {
            Range range = stack.remove(stack.size() - 1);

            int index = -1;
            double maxSqDist = 0f;

            // find index of point with maximum square distance from first and last point
            for (int i = range.first + 1; i < range.last; ++i) {
                double sqDist = getSquareSegmentDistance(mPoints.get(i), mPoints.get(range.first), mPoints.get(range.last));

                if (sqDist > maxSqDist) {
                    index = i;
                    maxSqDist = sqDist;
                }
            }

            if (maxSqDist > sqTolerance) {
                bitSet.set(index);

                stack.add(new Range(range.first, index));
                stack.add(new Range(index, range.last));
            }
        }


        GeoLineString result = new GeoLineString();
        result.setCRS(mCRS);
        for (int index = bitSet.nextSetBit(0); index >= 0; index = bitSet.nextSetBit(index + 1)) {
            result.add(mPoints.get(index));
        }

        return result;
    }

    protected double getSquareSegmentDistance(GeoPoint p0, GeoPoint p1, GeoPoint p2){
        double x0, y0, x1, y1, x2, y2, dx, dy, t;

        x1 = p1.getX();
        y1 = p1.getY();
        x2 = p2.getX();
        y2 = p2.getY();
        x0 = p0.getX();
        y0 = p0.getY();

        dx = x2 - x1;
        dy = y2 - y1;

        if (dx != 0.0d || dy != 0.0d) {
            t = ((x0 - x1) * dx + (y0 - y1) * dy)
                    / (dx * dx + dy * dy);

            if (t > 1.0d) {
                x1 = x2;
                y1 = y2;
            } else if (t > 0.0d) {
                x1 += dx * t;
                y1 += dy * t;
            }
        }

        dx = x0 - x1;
        dy = y0 - y1;

        return dx * dx + dy * dy;
    }

    protected static class Range {
        private Range(int first, int last) {
            this.first = first;
            this.last = last;
        }

        int first;
        int last;
    }
}
