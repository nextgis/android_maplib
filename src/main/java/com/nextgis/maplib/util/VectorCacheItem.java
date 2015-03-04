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

package com.nextgis.maplib.util;

import com.nextgis.maplib.datasource.GeoGeometry;


public class VectorCacheItem
{
    protected GeoGeometry mGeoGeometry;
    protected String mLabel;
    protected long mId;


    public VectorCacheItem(
            GeoGeometry geoGeometry,
            long id)
    {
        mGeoGeometry = geoGeometry;
        mId = id;
    }


    public GeoGeometry getGeoGeometry()
    {
        return mGeoGeometry;
    }


    public void setGeoGeometry(GeoGeometry geoGeometry)
    {
        mGeoGeometry = geoGeometry;
    }


    public String getLabel()
    {
        return mLabel;
    }


    public void setLabel(String label)
    {
        mLabel = label;
    }


    public long getId()
    {
        return mId;
    }


    public void setId(long id)
    {
        mId = id;
    }
}
