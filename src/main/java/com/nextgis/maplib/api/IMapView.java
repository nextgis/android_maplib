/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplib.api;

import android.graphics.Bitmap;
import com.nextgis.maplib.datasource.GeoPoint;


public interface IMapView
        extends IRenderer
{
    public Bitmap getView();

    public void setViewSize(
            int w,
            int h);

    public float getZoomLevel();

    /**
     * Set new map extent according zoom level and center
     *
     * @param zoom
     *         A zoom level
     * @param center
     *         A map center coordinates
     */
    public void setZoomAndCenter(
            float zoom,
            GeoPoint center);

    public GeoPoint getMapCenter();
}
