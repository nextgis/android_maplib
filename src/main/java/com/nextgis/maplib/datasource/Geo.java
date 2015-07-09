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


public class Geo
{

    protected final static double mEarthMajorRadius = 6378137.0;
    protected final static double mEarthMinorRadius = 6356752.3142;
    protected final static double mE                = mEarthMinorRadius / mEarthMajorRadius;
    protected final static double mES               = 1.0 - (mE * mE);
    protected final static double mEccent           = Math.sqrt(mES);
    protected final static double mCom              = 0.5 * mEccent;


    public static GeoPoint mercatorToWgs84SphereRet(final GeoPoint point)
    {
        GeoPoint retPt = new GeoPoint();
        retPt.mX = mercatorToWgs84SphereX(point.mX);
        retPt.mY = mercatorToWgs84SphereY(point.mY);
        return retPt;
    }


    public static double mercatorToWgs84SphereX(final double x)
    {
        return Math.toDegrees(x / mEarthMajorRadius);
    }


    public static double mercatorToWgs84SphereY(final double y)
    {
        return Math.toDegrees(2 * Math.atan(Math.exp(y / mEarthMajorRadius)) - Math.PI / 2);
    }


    public static void mercatorToWgs84Sphere(GeoPoint point)
    {
        point.mX = mercatorToWgs84SphereX(point.mX);
        point.mY = mercatorToWgs84SphereY(point.mY);
    }


    public static GeoPoint mercatorToWgs84EllipseRet(final GeoPoint point)
    {
        GeoPoint retPt = new GeoPoint();
        retPt.mX = mercatorToWgs84EllipseX(point.mX);
        retPt.mY = mercatorToWgs84EllipseY(point.mY);
        return retPt;
    }


    public static double mercatorToWgs84EllipseX(final double x)
    {
        return Math.toDegrees(x / mEarthMajorRadius);
    }


    public static double mercatorToWgs84EllipseY(final double y)
    {
        double phi = Math.toRadians(y);
        double sinphi = Math.sin(phi);
        double con = mEccent * sinphi;
        con = Math.pow(((1.0 - con) / (1.0 + con)), mCom);
        double ts = Math.tan(0.5 * ((Math.PI * 0.5) - phi)) / con;
        return 0 - mEarthMajorRadius * Math.log(ts);
    }


    public static void mercatorToWgs84Ellipse(GeoPoint point)
    {
        point.mX = mercatorToWgs84EllipseX(point.mX);
        point.mY = mercatorToWgs84EllipseY(point.mY);
    }


    public static GeoPoint wgs84ToMercatorSphereRet(final GeoPoint point)
    {
        GeoPoint retPt = new GeoPoint();
        retPt.mX = wgs84ToMercatorSphereX(point.mX);
        retPt.mY = wgs84ToMercatorSphereY(point.mY);
        return retPt;
    }


    public static double wgs84ToMercatorSphereX(final double x)
    {
        return mEarthMajorRadius * Math.toRadians(x);
    }


    public static double wgs84ToMercatorSphereY(final double y)
    {
        return mEarthMajorRadius * Math.log(Math.tan(Math.PI / 4 + Math.toRadians(y) / 2));
    }


    public static void wgs84ToMercatorSphere(GeoPoint point)
    {
        point.mX = wgs84ToMercatorSphereX(point.mX);
        point.mY = wgs84ToMercatorSphereY(point.mY);
    }


    public static boolean isGeometryTypeSame(
            final int type1,
            final int type2)
    {
        return type1 == type2 ||
               (type1 <= GeoConstants.GTMultiPolygon && type2 <= GeoConstants.GTMultiPolygon &&
                Math.abs(type1 - type2) == 3);
    }
}
