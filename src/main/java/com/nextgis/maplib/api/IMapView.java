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

package com.nextgis.maplib.api;

import android.graphics.Canvas;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;

/**
 * Interface that should implement each map view.
 * @author Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 */
public interface IMapView
        extends IRenderer
{
    /**
     * Draw map
     * @param canvas Canvas to draw layers
     * @param clearBackground True to clear background or false
     */
    void draw(
            Canvas canvas,
            boolean clearBackground);

    /**
     * Draw map
     * @param canvas Canvas to draw layers
     * @param x Offset in pixels by x coordinate
     * @param y Offset in pixels by y coordinate
     * @param clearBackground True to clear background or false
     */
    void draw(
            Canvas canvas,
            float x,
            float y,
            boolean clearBackground);

    /**
     * Draw map
     * @param canvas Canvas to draw layers
     * @param x Offset in pixels by x coordinate
     * @param y Offset in pixels by y coordinate
     * @param scale Scale to set then draw map
     */
    void draw(
            Canvas canvas,
            float x,
            float y,
            float scale);

    /**
     * Set view size in pixels while map window resized.
     * @param w New width
     * @param h New height
     */
    void setViewSize(
            int w,
            int h);

    /**
     * Save current map to offscreen buffer.
     * @param x Offset in pixels by x coordinate
     * @param y Offset in pixels by y coordinate
     * @param scale Scale to set then draw map
     */
    void buffer(
            float x,
            float y,
            float scale);

    /**
     * @return Current map zoom level
     */
    float getZoomLevel();

    /**
     * Set new map extent according to the zoom level and coordinates of center
     * @param zoom A zoom level
     * @param center A map center coordinates
     */
    void setZoomAndCenter(
            float zoom,
            GeoPoint center);

    /**
     * Zoom to fill provided extent
     * @param envelope Extent to zoom
     */
    void zoomToExtent(GeoEnvelope envelope);

    /**
     * @return Map center coordinates
     */
    GeoPoint getMapCenter();

    /**
     * @return Get full map bounds in map coordinates
     */
    GeoEnvelope getFullBounds();

    /**
     * @return Get current map bounds in map coordinates
     */
    GeoEnvelope getCurrentBounds();

    /**
     * @return Current limits of this map
     */
    GeoEnvelope getLimits();

    /**
     * Set the map limits. The map cannot be scrolled out of this limits
     * @param limits Envelope of limits
     * @param limitsType The limits type (maybe Constants.MAP_LIMITS_NO, Constants.MAP_LIMITS_X,
     *                   Constants.MAP_LIMITS_Y or Constants.MAP_LIMITS_XY)
     * @see com.nextgis.maplib.datasource.GeoEnvelope
     */
    void setLimits(
            GeoEnvelope limits,
            int limitsType);

    /**
     * Transform point from screen to map
     * @param pt Point to transform
     * @return Point in map coordinates
     * @see com.nextgis.maplib.datasource.GeoPoint
     */
    GeoPoint screenToMap(final GeoPoint pt);

    /**
     * Transform point from map to scree
     * @param pt Point to transform
     * @return Point in screen coordinates
     * @see com.nextgis.maplib.datasource.GeoPoint
     */
    GeoPoint mapToScreen(final GeoPoint pt);

    /**
     * Transform from map to screen coordinates
     * @param geoPoints Array of geopoints
     * @return Array of points in format  x1,y1, ... xn, yn
     * @see com.nextgis.maplib.datasource.GeoPoint
     */
    float[] mapToScreen(final GeoPoint[] geoPoints);

    /**
     * Transform provided envelope from screen to map coordinates
     * @param env Envelope
     * @return Transformed envelope
     * @see com.nextgis.maplib.datasource.GeoEnvelope
     */
    GeoEnvelope screenToMap(final GeoEnvelope env);

    /**
     * Transform from screen to map coordinates
     * @param points Array of points in format  x1,y1, ... xn, yn
     * @return Array of transformed points
     */
    GeoPoint[] screenToMap(final float[] points);
}
