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

package com.nextgis.maplib.api;


import com.nextgis.maplib.datasource.GeoPoint;


public interface MapEventListener
{
    public abstract void onLayerAdded(int id);

    public abstract void onLayerDeleted(int id);

    public abstract void onLayerChanged(int id);

    public abstract void onExtentChanged(
            float zoom,
            GeoPoint center);

    public abstract void onLayersReordered();

    public abstract void onLayerDrawFinished(
            int id,
            float percent);
}
