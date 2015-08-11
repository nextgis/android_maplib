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

package com.nextgis.maplib.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.nextgis.maplib.api.IMapView;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.display.GISDisplay;
import com.nextgis.maplib.util.Constants;

import java.io.File;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import static com.nextgis.maplib.util.Constants.MAP_LIMITS_Y;
import static com.nextgis.maplib.util.GeoConstants.DEFAULT_MAX_ZOOM;


public class MapDrawable
        extends MapEventSource
        implements IMapView
{
    protected int  mLimitsType;

    protected RunnableFuture<Void> mDrawThreadTask;


    public MapDrawable(
            Bitmap backgroundTile,
            Context context,
            File mapPath,
            LayerFactory layerFactory)
    {
        super(context, mapPath, layerFactory);

        //initialise display
        mDisplay = new GISDisplay(backgroundTile);

        mLimitsType = MAP_LIMITS_Y;
    }


    @Override
    public void draw(
            Canvas canvas,
            boolean clearBackground)
    {
        if (mDisplay != null) {
            mDisplay.draw(canvas, clearBackground);
        }
    }


    @Override
    public void draw(
            Canvas canvas,
            float x,
            float y,
            boolean clearBackground)
    {
        if (mDisplay != null) {
            mDisplay.draw(canvas, x, y, clearBackground);
        }
    }


    @Override
    public void draw(
            Canvas canvas,
            float x,
            float y,
            float scale)
    {
        if (mDisplay != null) {
            mDisplay.draw(canvas, x, y, scale);
        }
    }


    @Override
    public void buffer(
            float x,
            float y,
            float scale)
    {
        if (mDisplay != null) {
            mDisplay.buffer(x, y, scale);
        }
    }


    @Override
    public void setViewSize(
            int w,
            int h)
    {
        super.setViewSize(w, h);

        if (mDisplay != null) {
            mDisplay.setSize(w, h);
            onExtentChanged((int) mDisplay.getZoomLevel(), mDisplay.getCenter());
        }
    }


    @Override
    public float getZoomLevel()
    {
        if (mDisplay != null) {
            return mDisplay.getZoomLevel();
        }
        return 0;
    }


    /**
     * Set new map extent according zoom level and center
     *
     * @param zoom
     *         A zoom level
     * @param center
     *         A map center coordinates
     */
    @Override
    public void setZoomAndCenter(
            float zoom,
            GeoPoint center)
    {
        if (mDisplay != null) {
            float newZoom = zoom;
            if (zoom < mDisplay.getMinZoomLevel()) {
                newZoom = mDisplay.getMinZoomLevel();
            } else if (zoom > mDisplay.getMaxZoomLevel()) {
                newZoom = mDisplay.getMaxZoomLevel();
            }

            mDisplay.setZoomAndCenter(newZoom, center);
            onExtentChanged((int) newZoom, center);
        }
    }


    @Override
    public GeoPoint getMapCenter()
    {
        if (mDisplay != null) {
            return mDisplay.getCenter();
        }
        return new GeoPoint();
    }


    public GeoEnvelope getFullScreenBounds()
    {
        if (mDisplay != null) {
            return mDisplay.getScreenBounds();
        }
        return null;
    }

    @Override
    public GeoEnvelope getLimits()
    {
        if (mDisplay != null) {
            return mDisplay.getLimits();
        }
        return null;
    }


    @Override
    public void setLimits(
            GeoEnvelope limits,
            int limitsType)
    {
        if (mDisplay != null) {
            mDisplay.setGeoLimits(limits, limitsType);
        }
    }


    @Override
    public GeoPoint screenToMap(GeoPoint pt)
    {
        if (mDisplay != null) {
            return mDisplay.screenToMap(pt);
        }
        return null;
    }


    @Override
    public GeoPoint mapToScreen(GeoPoint pt)
    {
        if (mDisplay != null) {
            return mDisplay.mapToScreen(pt);
        }
        return null;
    }


    @Override
    public float[] mapToScreen(GeoPoint[] geoPoints)
    {
        if (mDisplay != null) {
            return mDisplay.mapToScreen(geoPoints);
        }
        return null;
    }


    @Override
    public GeoEnvelope screenToMap(GeoEnvelope env)
    {
        if (mDisplay != null) {
            return mDisplay.screenToMap(env);
        }
        return null;
    }


    @Override
    public GeoPoint[] screenToMap(float[] points)
    {
        if (mDisplay != null) {
            return mDisplay.screenToMap(points);
        }
        return null;
    }

    @Override
    public void runDraw(final GISDisplay display)
    {
        cancelDraw();

        onLayerDrawStarted();

        if (null != display && mDisplay != display) {
            mDisplay = display;
        }

        mDisplay.clearLayer();

        mDrawThreadTask = new FutureTask<Void>(
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        android.os.Process.setThreadPriority(
                                Constants.DEFAULT_DRAW_THREAD_PRIORITY);
                        MapDrawable.super.runDraw(mDisplay);
                    }

                }, null)
        {
            @Override
            protected void done()
            {
                super.done();
                //if (!isCancelled()) {
                    onDrawFinished(MapDrawable.this.getId(), 1.0f);
                //}
            }
        };

        new Thread(mDrawThreadTask).start();
    }


    @Override
    public void cancelDraw()
    {
        super.cancelDraw();
        if (null != mDrawThreadTask) {
            mDrawThreadTask.cancel(true);
        }
    }


    @Override
    public float getMaxZoom()
    {
        if (mDisplay != null) {
            return mDisplay.getMaxZoomLevel();
        }
        return DEFAULT_MAX_ZOOM;
    }


    @Override
    public float getMinZoom()
    {
        if (mDisplay != null) {
            return mDisplay.getMinZoomLevel();
        }
        return 0;
    }


    public void clearBackground(Canvas canvas)
    {
        if (null != mDisplay) {
            mDisplay.clearBackground(canvas);
        }
    }
}
