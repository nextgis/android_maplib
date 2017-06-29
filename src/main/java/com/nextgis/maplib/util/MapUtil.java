/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2017 NextGIS, info@nextgis.com
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

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.nextgis.maplib.R;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoMultiPolygon;
import com.nextgis.maplib.datasource.GeoPolygon;
import com.nextgis.maplib.datasource.TileItem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MapUtil {
    private static final long MAX_INTERNAL_CACHE_SIZE = 1048576; // 1MB
    private static final long MAX_EXTERNAL_CACHE_SIZE = 5242880; // 5MB

    public static File prepareTempDir(Context context, String path) {
        boolean clearCached;
        File temp = context.getExternalCacheDir();

        if (temp == null) {
            temp = context.getCacheDir();
            clearCached = FileUtil.getDirectorySize(temp) > MAX_INTERNAL_CACHE_SIZE;
        } else {
            clearCached = FileUtil.getDirectorySize(temp) > MAX_EXTERNAL_CACHE_SIZE;
        }

        if (clearCached)
            FileUtil.deleteRecursive(temp);

        if (path != null)
            temp = new File(temp, path);

        try {
            FileUtil.createDir(temp);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return temp;
    }

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

    public static long getTileCount(AsyncTask task, GeoEnvelope bounds, double zoom, int tmsType) {
        int decimalZoom = (int) zoom;
        int tilesInMapOneDimension = 1 << decimalZoom;

        double halfTilesInMapOneDimension = tilesInMapOneDimension * 0.5;
        double tilesSizeOneDimension = GeoConstants.MERCATOR_MAX / halfTilesInMapOneDimension;

        int begX = (int) Math.floor(bounds.getMinX() / tilesSizeOneDimension + halfTilesInMapOneDimension);
        int begY = (int) Math.floor(bounds.getMinY() / tilesSizeOneDimension + halfTilesInMapOneDimension);
        int endX = (int) Math.ceil(bounds.getMaxX() / tilesSizeOneDimension + halfTilesInMapOneDimension);
        int endY = (int) Math.ceil(bounds.getMaxY() / tilesSizeOneDimension + halfTilesInMapOneDimension);

        if (begY == endY)
            endY++;

        if (begX == endX)
            endX++;

        if (begY < 0)
            begY = 0;

        if (endY > tilesInMapOneDimension)
            endY = tilesInMapOneDimension;

        // normal fill from left bottom corner
        long total = 0;
        int realY;
        for (int x = begX; x < endX; x++) {
            if (task != null && task.isCancelled())
                return total;

            for (int y = begY; y < endY; y++) {
                realY = y;
                if (tmsType == GeoConstants.TMSTYPE_OSM)
                    realY = tilesInMapOneDimension - y - 1;

                if (realY < 0 || realY >= tilesInMapOneDimension)
                    continue;

                total++;
            }
        }

        return total;
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
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            for (int y = begY; y < endY; y++) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
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

                if(result.size() > Constants.MAX_TILES_COUNT) // some limits for tiles array size
                    return result;
            }
        }

        return result;
    }

    public static boolean isZippedGeoJSON(Context context, AtomicReference<Uri> uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri.get());
            if (inputStream == null)
                return false;

            byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry ze;

            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().toLowerCase().endsWith(".geojson")) {
                    File temp = prepareTempDir(context, null);
                    FileUtil.unzipEntry(zis, ze, buffer, temp);
                    temp = new File(temp, ze.getName());
                    uri.set(Uri.fromFile(temp));
                    zis.closeEntry();
                    return true;
                }
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean isParsable(String string) {
        try {
            //noinspection ResultOfMethodCallIgnored
            Long.parseLong(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String makePlaceholders(int size) {
        if (size <= 0)
            return "";

        StringBuilder sb = new StringBuilder(size * 2 - 1);
        sb.append("?");

        for (int i = 1; i < size; i++) {
            sb.append(",?");
        }

        return sb.toString();
    }

    public static double getScaleInCm(Activity activity, int zoom) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        double ppcm = dm.density * dm.xdpi / 2.54; // get px per cm on display
        ppcm = 256 / ppcm; // get cm on display per 256 px tile
        ppcm = 40075160 / ppcm; // get real m per cm on display
        return ppcm / Math.pow(2, zoom); // take zoom in count
    }


    public static boolean isGeometryIntersects(Context context, GeoGeometry geometry) {
        if (geometry instanceof GeoPolygon) {
            if (((GeoPolygon) geometry).intersects()) {
                Toast.makeText(context, R.string.self_intersection, Toast.LENGTH_SHORT).show();
                return true;
            }

            if (!((GeoPolygon) geometry).isHolesInside()) {
                Toast.makeText(context, R.string.ring_outside, Toast.LENGTH_SHORT).show();
                return true;
            }

            if (((GeoPolygon) geometry).isHolesIntersect()) {
                Toast.makeText(context, R.string.rings_intersection, Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        if (geometry instanceof GeoMultiPolygon) {
            if (((GeoMultiPolygon) geometry).isSelfIntersects()) {
                Toast.makeText(context, R.string.self_intersection, Toast.LENGTH_SHORT).show();
                return true;
            }

            if (!((GeoMultiPolygon) geometry).isHolesInside()) {
                Toast.makeText(context, R.string.ring_outside, Toast.LENGTH_SHORT).show();
                return true;
            }

            if (((GeoMultiPolygon) geometry).isHolesIntersect()) {
                Toast.makeText(context, R.string.rings_intersection, Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        return false;
    }
}
