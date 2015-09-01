/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

import com.nextgis.maplib.util.GeoConstants;

import java.util.LinkedList;
import java.util.List;


public class GeoLinearRing
        extends GeoLineString
{
    protected static final long serialVersionUID = -1241179697270831769L;

    public GeoLinearRing()
    {
        super();
    }


    public GeoLinearRing(GeoLinearRing geoLinearRing)
    {
        super(geoLinearRing);
    }


    public boolean isClosed()
    {
        List<GeoPoint> points = getPoints();
        GeoPoint first = points.get(0);
        GeoPoint last = points.get(points.size() - 1);
        return first.equals(last);
    }

    public GeoPoint getCentroid(){
        return getCentroidOfFiniteSetOfPoints();
    }

    /**
     * @see https://en.wikipedia.org/wiki/Centroid#Of_a_finite_set_of_points
     * @return Centroid of ring
     */
    protected GeoPoint getCentroidOfFiniteSetOfPoints(){
        double x = 0.;
        double y = 0.;
        int pointCount = mPoints.size();
        for (int i = 0; i < pointCount - 1; i++){
            final GeoPoint point = mPoints.get(i);
            x += point.getX();
            y += point.getY();
        }

        x = x/pointCount;
        y = y/pointCount;

        GeoPoint result = new GeoPoint(x, y);
        result.setCRS(mCRS);
        return result;
    }

    /**
     * @see https://en.wikipedia.org/wiki/Centroid#Of_a_finite_set_of_points
     * @return Centroid of ring
     */
    protected GeoPoint getCentroidPolygon(){
        double x = 0.;
        double y = 0.;
        double area = 0.;

        for (int i = 0; i < mPoints.size() - 1; i++) {
            final GeoPoint point = mPoints.get(i);
            final GeoPoint pointN = mPoints.get(i + 1);

            final double temp = point.getX() * pointN.getY() - pointN.getX() * point.getY();
            x += (point.getX() + pointN.getX()) * temp;
            y += (point.getY() + pointN.getY()) * temp;

            area += temp;
        }

        area *= 0.5;

        x *= 1 / 6.0 * area;
        y *= 1 / 6.0 * area;

        GeoPoint result = new GeoPoint(x, y);
        result.setCRS(mCRS);
        return result;
    }

/*
    @Override
    public boolean intersects(GeoEnvelope envelope)
    {
        return super.intersects(envelope);

        if (super.intersects(envelope)) {
            return true;
        }
        int intersection = 0;

        //create the ray
        GeoPoint center = envelope.getCenter();
        double x1 = center.getX();
        double y1 = center.getY();
        double x2 = getEnvelope().getMaxX() * 2;
        double y2 = center.getY();
        //count intersects
        for (int i = 0; i < mPoints.size() - 1; i++) {
            GeoPoint pt1 = mPoints.get(i);
            GeoPoint pt2 = mPoints.get(i + 1);

            if (pt1.getX() < x1 && pt2.getX() < x2) {
                continue;
            }

            if (linesIntersect(x1, y1, x2, y2, pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY())) {
                intersection++;
            }
        }

        return intersection % 2 == 1;
    }
    */


    @Override
    public GeoGeometry copy()
    {
        return new GeoLinearRing(this);
    }

    public boolean contains(GeoEnvelope envelope) {
        //check each corner, not exactly accurate, but fast
        GeoPoint pt = new GeoPoint(envelope.getMinX(), envelope.getMinY());
        if(!intersects(pt.getEnvelope()))
            return false;

        pt.setCoordinates(envelope.getMinX(), envelope.getMaxY());
        if(!intersects(pt.getEnvelope()))
            return false;

        pt.setCoordinates(envelope.getMaxX(), envelope.getMaxY());
        if(!intersects(pt.getEnvelope()))
            return false;

        pt.setCoordinates(envelope.getMaxX(), envelope.getMinY());
        if(!intersects(pt.getEnvelope()))
            return false;

        pt.setCoordinates(envelope.getMinX(), envelope.getMinY());
        if(!intersects(pt.getEnvelope()))
            return false;

        return true;
    }

    @Override
    protected GeoLineString getInstance() {
        return new GeoLinearRing();
    }

    @Override
    public GeoGeometry simplify(double tolerance){
        double sqTolerance = tolerance * tolerance;

        GeoEnvelope env = getEnvelope();
        double area = env.getArea() * 2;
        if(sqTolerance > area){
            GeoLinearRing result = new GeoLinearRing();
            result.setCRS(getCRS());
            result.add(new GeoPoint(env.getMinX(), env.getMinY()));
            result.add(new GeoPoint(env.getMinX(), env.getMaxY()));
            result.add(new GeoPoint(env.getMaxX(), env.getMaxY()));
            result.add(new GeoPoint(env.getMaxX(), env.getMinY()));
            result.add(new GeoPoint(env.getMinX(), env.getMinY()));
            return result;
        }

        return simplifyRadialDistance(sqTolerance);
    }

    public GeoLinearRing simplify(double tolerance, boolean canNull){
        double sqTolerance = tolerance * tolerance;

        GeoEnvelope env = getEnvelope();
        double area = env.getArea() * 2;
        if(sqTolerance > area && canNull){
            return null;
        }
        if(sqTolerance > area){
            GeoLinearRing result = new GeoLinearRing();
            result.setCRS(getCRS());
            result.add(new GeoPoint(env.getMinX(), env.getMinY()));
            result.add(new GeoPoint(env.getMinX(), env.getMaxY()));
            result.add(new GeoPoint(env.getMaxX(), env.getMaxY()));
            result.add(new GeoPoint(env.getMinX(), env.getMinY()));
            return result;
        }

        return (GeoLinearRing) super.simplify(sqTolerance);
    }

    @Override
    public GeoGeometry clip(GeoEnvelope envelope) {
        if(mPoints.isEmpty())
            return null;
        GeoLinearRing result = new GeoLinearRing();
        clip(mPoints, result.mPoints, envelope, true);

        if(result.getPointCount() < 4)
            return null;
        return result;
    }

    @Override
    public int getType() {
        return GeoConstants.GTLinearRing;
    }

    public void clipForTiled(GeoLinearRing newRing, GeoLineString newBorder, GeoEnvelope envelope) {
        List<GeoPoint> tmpPointsOut = new LinkedList<>();
        trimPoints(mPoints, tmpPointsOut, GeoEnvelope.enumGISPtPosRight, envelope, false);
        if(tmpPointsOut.size() < 4)
            return;
        List<GeoPoint> tmpPointsOutAdd = new LinkedList<>();
        trimPoints(tmpPointsOut, tmpPointsOutAdd, GeoEnvelope.enumGISPtPosTop, envelope, false);
        if(tmpPointsOutAdd.size() < 4)
            return;
        tmpPointsOut.clear();
        trimPoints(tmpPointsOutAdd, tmpPointsOut, GeoEnvelope.enumGISPtPosLeft, envelope, false);
        if(tmpPointsOut.size() < 4)
            return;
        trimPoints(tmpPointsOut, newRing.mPoints, GeoEnvelope.enumGISPtPosLeft, envelope, false);
        //trimPoints(tmpPointsOut, newRing.mPoints, newBorder.mPoints, GeoEnvelope.enumGISPtPosBottom, envelope);
    }

    protected void trimPoints(List<GeoPoint> pointsIn, List<GeoPoint> pointsOut, List<GeoPoint> borderOut, int pos, GeoEnvelope envelope) {
        // The shapeOpen parameter selects whether this function treats the
        // shape as open or closed. False is appropriate for polygons and
        // true for polylines.

        int i1 = pointsIn.size() - 1; // start with last point

        // and compare to the first point initially.
        for ( int i2 = 0; i2 < pointsIn.size(); ++i2 ) { // look at each edge of the polygon in turn
            if ( envelope.isInside(pointsIn.get(i2), pos) ) { // end point of edge is inside boundary
                if(envelope.isInside(pointsIn.get(i1), pos)) {
                    pointsOut.add(pointsIn.get(i2));
                    borderOut.add(pointsIn.get(i2));
                }
                else {
                    // edge crosses into the boundary, so trim back to the boundary, and
                    // store both ends of the new edge
                    if ( !( i2 == 0 ) ) {
                        GeoPoint pointNew = solveIntersection(pointsIn.get(i1), pointsIn.get(i2), pos, envelope);
                        if (null != pointNew) {
                            pointsOut.add(pointNew);
                        }
                    }
                    pointsOut.add(pointsIn.get(i2));
                    borderOut.add(pointsIn.get(i2));
                }
            }
            else { // end point of edge is outside boundary
                // start point is in boundary, so need to trim back
                if ( envelope.isInside(pointsIn.get(i1), pos)) {
                    if ( !( i2 == 0 ) ) {
                        GeoPoint pointNew = solveIntersection(pointsIn.get(i1), pointsIn.get(i2), pos, envelope);
                        if (null != pointNew)
                            pointsOut.add(pointNew);
                    }
                }
            }
            i1 = i2;
        }
    }
}
