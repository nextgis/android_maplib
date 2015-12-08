/*
 *  Project:  NextGIS Mobile
 *  Purpose:  Mobile GIS for Android.
 *  Author:   Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 * ****************************************************************************
 *  Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.util;

import com.nextgis.maplib.datasource.GeoLinearRing;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by bishop on 08.12.15.
 */
public class GeoUtil {

    public static GeoPolygon convexHull(List<GeoPoint> points){
        List<GeoPoint> sortedPoints = new ArrayList<>(points);
        Collections.sort(sortedPoints, new GeoPointsCompare());

        int n = sortedPoints.size();

        GeoPoint[] lUpper = new GeoPoint[n];

        lUpper[0] = sortedPoints.get(0);
        lUpper[1] = sortedPoints.get(1);

        int lUpperSize = 2;

        for (int i = 2; i < n; i++)
        {
            lUpper[lUpperSize] = sortedPoints.get(i);
            lUpperSize++;

            while (lUpperSize > 2 && !rightTurn(lUpper[lUpperSize - 3], lUpper[lUpperSize - 2], lUpper[lUpperSize - 1]))
            {
                // Remove the middle point of the three last
                lUpper[lUpperSize - 2] = lUpper[lUpperSize - 1];
                lUpperSize--;
            }
        }

        GeoPoint[] lLower = new GeoPoint[n];

        lLower[0] = sortedPoints.get(n - 1);
        lLower[1] = sortedPoints.get(n - 2);

        int lLowerSize = 2;

        for (int i = n - 3; i >= 0; i--)
        {
            lLower[lLowerSize] = sortedPoints.get(i);
            lLowerSize++;

            while (lLowerSize > 2 && !rightTurn(lLower[lLowerSize - 3], lLower[lLowerSize - 2], lLower[lLowerSize - 1]))
            {
                // Remove the middle point of the three last
                lLower[lLowerSize - 2] = lLower[lLowerSize - 1];
                lLowerSize--;
            }
        }

        GeoPolygon polygon = new GeoPolygon();
        for (int i = 0; i < lUpperSize; i++)
        {
            polygon.add(lUpper[i]);
        }

        for (int i = 1; i < lLowerSize - 1; i++)
        {
            polygon.add(lLower[i]);
        }

        return polygon;

    }

    protected static boolean rightTurn(GeoPoint a, GeoPoint b, GeoPoint c)
    {
        return (b.getX() - a.getX())*(c.getY() - a.getY()) - (b.getY() - a.getY())*(c.getX() - a.getX()) > 0;
    }


    protected static class GeoPointsCompare implements Comparator<GeoPoint>
    {
        @Override
        public int compare(GeoPoint o1, GeoPoint o2)
        {
            return o1.compareTo(o2);
        }
    }

}
