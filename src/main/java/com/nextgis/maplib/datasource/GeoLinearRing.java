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

import java.util.List;


public class GeoLinearRing
        extends GeoLineString
{
    public GeoLinearRing()
    {
        super();
    }


    public GeoLinearRing(GeoLinearRing geoLineRing)
    {
        super(geoLineRing);
    }


    public boolean isClosed()
    {
        List<GeoPoint> points = getPoints();
        GeoPoint first = points.get(0);
        GeoPoint last = points.get(points.size() - 1);
        return first.equals(last);
    }


    @Override
    public boolean intersects(GeoEnvelope envelope)
    {
        return super.intersects(envelope);
        /*
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

        return intersection % 2 == 1;*/
    }


    @Override
    public GeoGeometry copy()
    {
        return new GeoLinearRing(this);
    }
}
