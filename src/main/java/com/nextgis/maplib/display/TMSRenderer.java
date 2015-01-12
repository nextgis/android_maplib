/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

package com.nextgis.maplib.display;


import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.map.TMSLayer;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static com.nextgis.maplib.util.Constants.*;


public class TMSRenderer
        extends Renderer
{
    protected static final String JSON_TMSRENDERER_TYPE       = "type";
    protected static final String JSON_TMSRENDERER_ANTIALIAS  = "antialias";
    protected static final String JSON_TMSRENDERER_FILTERBMP  = "filterbitmap";
    protected static final String JSON_TMSRENDERER_DITHER     = "dither";
    protected static final String JSON_TMSRENDERER_CONTRAST   = "contrast";
    protected static final String JSON_TMSRENDERER_BRIGHTNESS = "brightness";
    protected static final String JSON_TMSRENDERER_GRAYSCALE  = "greyscale";
    protected ThreadPoolExecutor mDrawThreadPool;
    protected Paint              mRasterPaint;
    protected boolean            mAntiAlias;
    protected boolean            mFilterBitmap;
    protected boolean            mDither;
    protected float              mContrast;
    protected float              mBrightness;
    protected boolean            mForceToGrayScale;


    public TMSRenderer(ILayer layer)
    {
        super(layer);
        mRasterPaint = new Paint();

        mAntiAlias = true;
        mFilterBitmap = true;
        mDither = true;
        mContrast = 1;
        mBrightness = 0;
        mForceToGrayScale = false;

        mRasterPaint.setAntiAlias(mAntiAlias);
        mRasterPaint.setFilterBitmap(mFilterBitmap);
        mRasterPaint.setDither(mDither);


    }

    protected void createPoolExecutor(TMSLayer tmsLayer)
    {
        if(null == tmsLayer)
            return;

        synchronized (this) {
            int threadCount = tmsLayer.getMaxThreadCount();
            if (threadCount > 0)
                mDrawThreadPool = new ThreadPoolExecutor(threadCount, threadCount, KEEP_ALIVE_TIME,
                                                         KEEP_ALIVE_TIME_UNIT, new LinkedBlockingQueue<Runnable>(),
                                                         new RejectedExecutionHandler()
                                                         {
                                                             @Override
                                                             public void rejectedExecution(
                                                                     Runnable r,
                                                                     ThreadPoolExecutor executor)
                                                             {
                                                                 try {
                                                                     executor.getQueue().put(r);
                                                                 } catch (InterruptedException e) {
                                                                     e.printStackTrace();
                                                                     //throw new RuntimeException("Interrupted while submitting task", e);
                                                                 }
                                                             }
                                                         });
        }
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject renderer = new JSONObject();
        renderer.put(JSON_TMSRENDERER_TYPE, "tms_renderer");
        renderer.put(JSON_TMSRENDERER_ANTIALIAS, mAntiAlias);
        renderer.put(JSON_TMSRENDERER_FILTERBMP, mFilterBitmap);
        renderer.put(JSON_TMSRENDERER_DITHER, mDither);
        renderer.put(JSON_TMSRENDERER_CONTRAST, mContrast);
        renderer.put(JSON_TMSRENDERER_BRIGHTNESS, mBrightness);
        renderer.put(JSON_TMSRENDERER_GRAYSCALE, mForceToGrayScale);
        return renderer;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        mAntiAlias = jsonObject.getBoolean(JSON_TMSRENDERER_ANTIALIAS);
        mFilterBitmap = jsonObject.getBoolean(JSON_TMSRENDERER_FILTERBMP);
        mDither = jsonObject.getBoolean(JSON_TMSRENDERER_DITHER);
        mContrast = (float) jsonObject.getDouble(JSON_TMSRENDERER_CONTRAST);
        mBrightness = (float) jsonObject.getDouble(JSON_TMSRENDERER_BRIGHTNESS);
        mForceToGrayScale = jsonObject.getBoolean(JSON_TMSRENDERER_GRAYSCALE);

        mRasterPaint.setAntiAlias(mAntiAlias);
        mRasterPaint.setFilterBitmap(mFilterBitmap);
        mRasterPaint.setDither(mDither);

        setContrastBrightness(mContrast, mBrightness, mForceToGrayScale);
    }


    /**
     * @param contrast
     *         0..10 1 is default
     * @param brightness
     *         -255..255 0 is default
     */
    public void setContrastBrightness(
            float contrast,
            float brightness,
            boolean bToGreyScale)
    {

        mContrast = contrast;
        mBrightness = brightness;
        mForceToGrayScale = bToGreyScale;

        ColorMatrix cm;
        if (bToGreyScale) {
            cm = new ColorMatrix(new float[] {
                contrast * 0.299f, 0.587f, 0.114f, 0, brightness,
                0.299f, 0.587f * contrast, 0.114f, 0, brightness,
                0.299f, 0.587f, 0.114f * contrast, 0, brightness,
                0, 0, 0, 1, 0});
        } else {
            cm = new ColorMatrix(new float[] {
                contrast, 0, 0, 0, brightness,
                0, contrast, 0, 0, brightness,
                0, 0, contrast, 0, brightness,
                0, 0, 0, 1, 0});
        }
        if (mRasterPaint != null) {
            mRasterPaint.setColorFilter(new ColorMatrixColorFilter(cm));
        }
    }


    @Override
    public void runDraw(final GISDisplay display)
            throws NullPointerException
    {
        final double zoom = display.getZoomLevel();

        GeoEnvelope env = display.getBounds();
        //get tiled for zoom and bounds
        final TMSLayer tmsLayer = (TMSLayer) mLayer;
        final List<TileItem> tiles = tmsLayer.getTielsForBounds(display, env, zoom);

        createPoolExecutor(tmsLayer);

        for (int i = 0; i < tiles.size(); ++i) {
            final TileItem tile = tiles.get(i);
            mDrawThreadPool.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                    final Bitmap bmp = tmsLayer.getBitmap(tile);
                    if (bmp != null) {
                        display.drawTile(bmp, tile.getPoint(), mRasterPaint);
                    }

                    float percent = 100;
                    synchronized (mLayer) {
                        if (mDrawThreadPool.getTaskCount() > 1)
                            percent = (float) (mDrawThreadPool.getCompletedTaskCount() + 1) /
                                      mDrawThreadPool.getTaskCount();
                        tmsLayer.onDrawFinished(tmsLayer.getId(), percent);
                    }
                    //Log.d(TAG, "percent: " + percent + " complete: " + mDrawThreadPool.getCompletedTaskCount() + " task count: " + mDrawThreadPool.getTaskCount());
                }
            });
        }
    }


    @Override
    public void cancelDraw()
    {
        synchronized (this) {
            if (mDrawThreadPool != null) {
                mDrawThreadPool.shutdownNow();
            }
        }
    }
}
