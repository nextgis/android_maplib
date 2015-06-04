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

import android.graphics.Canvas;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;


public interface IMapView
        extends IRenderer
{
    void draw(
            Canvas canvas,
            boolean clearBackground);

    void draw(
            Canvas canvas,
            float x,
            float y,
            boolean clearBackground);

    void draw(
            Canvas canvas,
            float x,
            float y,
            float scale);

    void setViewSize(
            int w,
            int h);

    void buffer(
            float x,
            float y,
            float scale);

    float getZoomLevel();

    /**
     * Set new map extent according zoom level and center
     *
     * @param zoom
     *         A zoom level
     * @param center
     *         A map center coordinates
     */
    void setZoomAndCenter(
            float zoom,
            GeoPoint center);

    GeoPoint getMapCenter();

    GeoEnvelope getFullBounds();

    GeoEnvelope getCurrentBounds();

    GeoEnvelope getLimits();

    void setLimits(
            GeoEnvelope limits,
            int limitsType);

    GeoPoint screenToMap(final GeoPoint pt);

    GeoPoint mapToScreen(final GeoPoint pt);

    float[] mapToScreen(final GeoPoint[] geoPoints);

    GeoEnvelope screenToMap(final GeoEnvelope env);

    GeoPoint[] screenToMap(final float[] points);
}
