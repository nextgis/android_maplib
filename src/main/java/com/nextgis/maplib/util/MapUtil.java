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

import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.TileItem;

import java.util.LinkedList;
import java.util.List;

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

    public static double getPixelSize(int zoom){
        int tilesInMapOneDimension = 1 << zoom;
        long sizeOneDimensionPixels = tilesInMapOneDimension * Constants.DEFAULT_TILE_SIZE;
        return GeoConstants.MERCATOR_MAX * 2 / sizeOneDimensionPixels;
    }

    public static List<TileItem> getTileItems(GeoEnvelope bounds, double zoom, int tmsType) {
        int decimalZoom = (int) zoom;
        int tilesInMapOneDimension = 1 << decimalZoom;
        double halfTilesInMapOneDimension = tilesInMapOneDimension * 0.5;

        double tilesSizeOneDimension = GeoConstants.MERCATOR_MAX / halfTilesInMapOneDimension;
        final List<TileItem> result = new LinkedList<>();

        int begX = (int) Math.floor(bounds.getMinX() / tilesSizeOneDimension + halfTilesInMapOneDimension);
        int begY = (int) Math.floor(bounds.getMinY() / tilesSizeOneDimension + halfTilesInMapOneDimension);
        int endX = (int) Math.ceil(bounds.getMaxX() / tilesSizeOneDimension + halfTilesInMapOneDimension);
        int endY = (int) Math.ceil(bounds.getMaxY() / tilesSizeOneDimension + halfTilesInMapOneDimension);

        if(begY == endY)
            endY++;
        if(begX == endX)
            endX++;

        if (begY < 0) {
            begY = 0;
        }
        if (endY > tilesInMapOneDimension) {
            endY = tilesInMapOneDimension;
        }
        /* this block unlimited X scroll of the map
        if (begX < 0) {
            begX = 0;
        }
        if (endX > tilesInMapOneDimension) {
            endX = tilesInMapOneDimension;
        }
        */

        // normal fill from left bottom corner
        int realX, realY;
        double fullBoundsMinX = -GeoConstants.MERCATOR_MAX;
        double fullBoundsMinY = -GeoConstants.MERCATOR_MAX;
        for (int x = begX; x < endX; x++) {
            for (int y = begY; y < endY; y++) {
                realX = x;
                if (realX < 0) {
                    realX += tilesInMapOneDimension;
                } else if (realX >= tilesInMapOneDimension) {
                    realX -= tilesInMapOneDimension;
                }

                realY = y;
                if (tmsType == GeoConstants.TMSTYPE_OSM) {
                    realY = tilesInMapOneDimension - y - 1;
                }

                if (realY < 0 || realY >= tilesInMapOneDimension) {
                    continue;
                }

                double minX = fullBoundsMinX + x * tilesSizeOneDimension;
                double minY = fullBoundsMinY + y * tilesSizeOneDimension;
                final GeoEnvelope env = new GeoEnvelope(
                        minX,
                        minX + tilesSizeOneDimension,
                        minY,
                        minY + tilesSizeOneDimension);
                TileItem item = new TileItem(realX, realY, decimalZoom, env);
                result.add(item);

                if(result.size() > 10000) // some limits for tiles array size
                    return result;
            }
        }

        return result;
    }

}
