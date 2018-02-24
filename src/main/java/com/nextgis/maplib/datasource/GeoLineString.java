/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
 *
 * The simplify algorithm adopted from simplify-java project under the MIT license
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

import android.annotation.TargetApi;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.JsonReader;

import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;


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
        return GeoConstants.GTLineString;
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void setCoordinatesFromJSONStream(JsonReader reader, int crs) throws IOException {
        setCRS(crs);
        reader.beginArray();
        while (reader.hasNext()){
            GeoPoint pt = new GeoPoint();
            pt.setCoordinatesFromJSONStream(reader, crs);
            mPoints.add(pt);
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

        for (String token : wkt.split(",")) {
            GeoPoint point = new GeoPoint();
            point.setCoordinatesFromWKT(token.trim(), crs);
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
        if (mPoints.size() != otherLn.getPointCount())
            return false;

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

    // https://www.topcoder.com/community/data-science/data-science-tutorials/geometry-concepts-line-intersection-and-its-applications/
    /**
     * Detects line ab and cd intersection
     * @param   a   Start first line point point
     * @param   b   End first line point
     * @param   c   Start second line point
     * @param   d   End second line point
     * @return      Do lines intersect
     */
    public boolean intersects(GeoPoint a, GeoPoint b, GeoPoint c, GeoPoint d) {
        double A1 = b.getY() - a.getY();
        double B1 = a.getX() - b.getX();
        double C1 = A1 * a.getX() + B1 * a.getY();

        return intersects(a, b, c, d, A1, B1, C1);
    }

    /**
     * Detects line ab and cd intersection
     * @param   a   Start first line point point
     * @param   b   End first line point
     * @param   c   Start second line point
     * @param   d   End second line point
     * @param   A1  For speed, see method above
     * @param   B1  For speed, see method above
     * @param   C1  For speed, see method above
     * @return      Do lines intersect
     */
    public boolean intersects(GeoPoint a, GeoPoint b, GeoPoint c, GeoPoint d, double A1, double B1, double C1) {
        double A2 = d.getY() - c.getY();
        double B2 = c.getX() - d.getX();
        double C2 = A2 * c.getX() + B2 * c.getY();

        double det = A1 * B2 - A2 * B1;
        if (det != 0) {
            double x = (B2 * C1 - B1 * C2) / det;
            double y = (A1 * C2 - A2 * C1) / det;

            boolean xOnAB = Math.min(a.getX(), b.getX()) <= x && x <= Math.max(a.getX(), b.getX());
            boolean yOnAB = Math.min(a.getY(), b.getY()) <= y && y <= Math.max(a.getY(), b.getY());

            if (xOnAB && yOnAB) {
                boolean xOnCD = Math.min(c.getX(), d.getX()) <= x && x <= Math.max(c.getX(), d.getX());
                boolean yOnCD = Math.min(c.getY(), d.getY()) <= y && y <= Math.max(c.getY(), d.getY());

                if (xOnCD && yOnCD)
                    return true;
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


    @Override
    public GeoGeometry simplify(double tolerance){
        double sqTolerance = tolerance * tolerance;

        GeoEnvelope env = getEnvelope();
        double area = env.getArea() * Constants.SIMPLIFY_TOENV_AREA_MULTIPLY;
        if(sqTolerance > area * Constants.SIMPLIFY_SKIP_AREA_MULTIPLY) { //don't show this geometry on this zoom
            return null;
        }
        else if(sqTolerance > area){
            GeoLineString result = new GeoLineString();
            result.setCRS(getCRS());
            result.add(new GeoPoint(env.getMinX(), env.getMinY()));
            result.add(new GeoPoint(env.getMaxX(), env.getMaxY()));
            return result;
        }

        return simplifyRadialDistance(sqTolerance);
    }

    @Override
    public GeoGeometry clip(GeoEnvelope envelope) {
        if(mPoints.isEmpty())
            return null;
        GeoLineString result = new GeoLineString();
        clip(mPoints, result.mPoints, envelope, true);

        if(result.getPointCount() < 2)
            return null;
        return result;
    }

    protected void trimPoints(List<GeoPoint> pointsIn, List<GeoPoint> pointsOut, int pos, GeoEnvelope envelope, boolean shapeOpen ) {
        // The shapeOpen parameter selects whether this function treats the
        // shape as open or closed. False is appropriate for polygons and
        // true for polylines.

        int i1 = pointsIn.size() - 1; // start with last point

        // and compare to the first point initially.
        for ( int i2 = 0; i2 < pointsIn.size(); ++i2 ) { // look at each edge of the polygon in turn
            if ( envelope.isInside(pointsIn.get(i2), pos) ) { // end point of edge is inside boundary
                if(envelope.isInside(pointsIn.get(i1), pos)) {
                    pointsOut.add(pointsIn.get(i2));
                }
                else {
                    // edge crosses into the boundary, so trim back to the boundary, and
                    // store both ends of the new edge
                    if ( !( i2 == 0 && shapeOpen ) ) {
                        GeoPoint pointNew = solveIntersection(pointsIn.get(i1), pointsIn.get(i2), pos, envelope);
                        if (null != pointNew)
                            pointsOut.add(pointNew);
                    }
                    pointsOut.add(pointsIn.get(i2));
                }
            }
            else { // end point of edge is outside boundary
                // start point is in boundary, so need to trim back
                if ( envelope.isInside(pointsIn.get(i1), pos)) {
                    if ( !( i2 == 0 && shapeOpen ) ) {
                        GeoPoint pointNew = solveIntersection(pointsIn.get(i1), pointsIn.get(i2), pos, envelope);
                        if (null != pointNew)
                            pointsOut.add(pointNew);
                    }
                }
            }
            i1 = i2;
        }
    }

    protected GeoPoint solveIntersection(GeoPoint pt1, GeoPoint pt2, int pos, GeoEnvelope envelope) {
        double EPSILON = 0.0000000000000001;
        double r_n = EPSILON, r_d = EPSILON;

        switch ( pos )
        {
            case GeoEnvelope.enumGISPtPosRight: // x = MAX_X boundary
                r_n = -( pt1.getX() - envelope.getMaxX() ) * ( envelope.getMaxY() - envelope.getMinY() );
                r_d = ( pt2.getX() - pt1.getX() ) * ( envelope.getMaxY() - envelope.getMinY() );
                break;
            case GeoEnvelope.enumGISPtPosLeft: // x = MIN_X boundary
                r_n = -( pt1.getX() - envelope.getMinX() ) * ( envelope.getMaxY() - envelope.getMinY() );
                r_d = ( pt2.getX() - pt1.getX() ) * ( envelope.getMaxY() - envelope.getMinY() );
                break;
            case GeoEnvelope.enumGISPtPosTop: // y = MAX_Y boundary
                r_n = ( pt1.getY() - envelope.getMaxY() ) * ( envelope.getMaxX() - envelope.getMinX() );
                r_d = -( pt2.getY() - pt1.getY() ) * ( envelope.getMaxX() - envelope.getMinX() );
                break;
            case GeoEnvelope.enumGISPtPosBottom: // y = MIN_Y boundary
                r_n = ( pt1.getY() - envelope.getMinY() ) * ( envelope.getMaxX() - envelope.getMinX() );
                r_d = -( pt2.getY() - pt1.getY() ) * ( envelope.getMaxX() - envelope.getMinX() );
                break;
        }

        if ( Math.abs(r_d) > EPSILON && Math.abs(r_n) > EPSILON ) { // they cross
            GeoPoint out = new GeoPoint();
            double r = r_n / r_d;
            out.setX( pt1.getX() + r * ( pt2.getX() - pt1.getX() ) );
            out.setY( pt1.getY() + r * ( pt2.getY() - pt1.getY() ) );

            return out;
        }
        else {
            return null;
        }
    }

    /**
     * Sutherland-Hodgman Polygon Clipping
     * Adopted from (C) 2005 by Gavin Macaulay QGIS Project
     */
    protected void clip(List<GeoPoint> pointsIn, List<GeoPoint> pointsOut, GeoEnvelope envelope, boolean shapeOpen ){
        List<GeoPoint> tmpPointsOut = new LinkedList<>();
        List<GeoPoint> tmpPointsOutAdd = new LinkedList<>();
        trimPoints(pointsIn, tmpPointsOut, GeoEnvelope.enumGISPtPosRight, envelope, shapeOpen);
        trimPoints(tmpPointsOut, tmpPointsOutAdd, GeoEnvelope.enumGISPtPosTop, envelope, shapeOpen);
        tmpPointsOut.clear();
        trimPoints(tmpPointsOutAdd, tmpPointsOut, GeoEnvelope.enumGISPtPosLeft, envelope, shapeOpen);
        trimPoints(tmpPointsOut, pointsOut, GeoEnvelope.enumGISPtPosBottom, envelope, shapeOpen);
    }

    protected GeoLineString getInstance(){
        return new GeoLineString();
    }

    protected GeoLineString simplifyRadialDistance(double sqTolerance){
        if(mPoints.isEmpty())
            return null;

        GeoPoint point = null;
        GeoPoint prevPoint = mPoints.get(0);

        GeoLineString result = getInstance();
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


        GeoLineString result = getInstance();
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

    @Override
    public void write(DataOutputStream stream) throws IOException {
        super.write(stream);
        int pointCount = mPoints.size();
        stream.writeInt(pointCount);
        for (int i = 0; i < pointCount; i++){
            GeoPoint pt = mPoints.get(i);
            pt.write(stream);
        }
    }

    @Override
    public void read(DataInputStream stream) throws IOException {
        super.read(stream);
        int pointCount = stream.readInt();
        for (int i = 0; i < pointCount; i++){
            GeoGeometry geometry = GeoGeometryFactory.fromDataStream(stream);
            if(null != geometry && geometry instanceof GeoPoint)
                mPoints.add((GeoPoint) geometry);
        }
    }

    @Override
    public boolean isValid() {
        for (GeoPoint point : mPoints)
            if (!point.isValid())
                return false;

        return mPoints.size() > 1;
    }

    @Override
    public double distance(GeoGeometry geometry) {
        // TODO: 04.09.15 release this
        return 0;
    }

    public double getLength() {
        double length = 0;

        if (mPoints.size() < 2)
            return length;

        Location location1 = new Location(LocationManager.GPS_PROVIDER);
        GeoPoint point = (GeoPoint) mPoints.get(0).copy();
        point.setCRS(CRS_WEB_MERCATOR);
        point.project(CRS_WGS84);
        location1.setLongitude(point.getX());
        location1.setLatitude(point.getY());

        for (int i = 1; i < mPoints.size(); i++) {
            Location location2 = new Location(LocationManager.GPS_PROVIDER);
            point = (GeoPoint) mPoints.get(i).copy();
            point.setCRS(CRS_WEB_MERCATOR);
            point.project(CRS_WGS84);
            location2.setLongitude(point.getX());
            location2.setLatitude(point.getY());
            length += location1.distanceTo(location2);
            location1 = location2;
        }

        return length;
    }
}
