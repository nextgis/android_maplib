/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2015. NextGIS, info@nextgis.com
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

/**
 * Created by bishop on 20.08.15.
 */
public class MapUtil {
    public static double lg(double x)
    {
        return Math.log(x) / Math.log(2.0);
    }

    public static float getZoomForScaleFactor(double scale, float currentZoom)
    {
        float zoom = currentZoom;
        if (scale > 1) {
            zoom = (float) (currentZoom + MapUtil.lg(scale));
        } else if (scale < 1) {
            zoom = (float) (currentZoom - MapUtil.lg(1 / scale));
        }
        return zoom;
    }
}
