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

package com.nextgis.maplib.display;


import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.map.RemoteTMSLayer;
import com.nextgis.maplib.map.TMSLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.MapUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
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
    protected static final String JSON_TMSRENDERER_ALPHA      = "alpha";
    protected ThreadPoolExecutor mDrawThreadPool;
    protected Paint              mRasterPaint;
    protected boolean            mAntiAlias;
    protected boolean            mFilterBitmap;
    protected boolean            mDither;
    protected float              mContrast;
    protected float              mBrightness;
    protected boolean            mForceToGrayScale;
    protected int                mAlpha;
    //protected final Object lock = new Object();


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
        mAlpha = 255;

        mRasterPaint.setAntiAlias(mAntiAlias);
        mRasterPaint.setFilterBitmap(mFilterBitmap);
        mRasterPaint.setDither(mDither);
        mRasterPaint.setAlpha(mAlpha);
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
        renderer.put(JSON_TMSRENDERER_ALPHA, mAlpha);
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

        if(jsonObject.has(JSON_TMSRENDERER_ALPHA))
            mAlpha = jsonObject.getInt(JSON_TMSRENDERER_ALPHA);
        else
            mAlpha = 255;
        mRasterPaint.setAntiAlias(mAntiAlias);
        mRasterPaint.setFilterBitmap(mFilterBitmap);
        mRasterPaint.setDither(mDither);
        mRasterPaint.setAlpha(mAlpha);

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
            cm = new ColorMatrix(
                    new float[] {
                            contrast * 0.299f,
                            0.587f,
                            0.114f,
                            0,
                            brightness,
                            0.299f,
                            0.587f * contrast,
                            0.114f,
                            0,
                            brightness,
                            0.299f,
                            0.587f,
                            0.114f * contrast,
                            0,
                            brightness,
                            0,
                            0,
                            0,
                            1,
                            0});
        } else {
            cm = new ColorMatrix(
                    new float[] {
                            contrast,
                            0,
                            0,
                            0,
                            brightness,
                            0,
                            contrast,
                            0,
                            0,
                            brightness,
                            0,
                            0,
                            contrast,
                            0,
                            brightness,
                            0,
                            0,
                            0,
                            1,
                            0});
        }
        if (mRasterPaint != null) {
            mRasterPaint.setColorFilter(new ColorMatrixColorFilter(cm));
        }
    }


    @Override
    public void runDraw(final GISDisplay display)
            throws NullPointerException
    {
        long startTime;
        if(Constants.DEBUG_MODE) {
            startTime = System.currentTimeMillis();
        }

        final double zoom = display.getZoomLevel();


        //get tiled for zoom and bounds
        final TMSLayer tmsLayer = (TMSLayer) getLayer();

        if (tmsLayer instanceof RemoteTMSLayer) {
            RemoteTMSLayer remoteTMSLayer = (RemoteTMSLayer) tmsLayer;
            remoteTMSLayer.onPrepare();
        }

        final List<TileItem> tiles = MapUtil.getTileItems(display.getBounds(), zoom, tmsLayer.getTMSType());
        if (tiles.size() == 0) {
            return;
        }

        cancelDraw();

        int threadCount = DRAWING_SEPARATE_THREADS;
        int coreCount = Runtime.getRuntime().availableProcessors();

        // FIXME more than 1 pool size causing strange behaviour on 6.0 -> tiles do not render from some threads, exception appears:
        // Fatal signal 11 (SIGSEGV), code 1, fault addr 0xX in tid X (pool-X-thread-X)
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M)
            coreCount = 1;
        // SOLUTION: Add syncronized in drawing raster

        //synchronized (lock) {
            mDrawThreadPool = new ThreadPoolExecutor(
                    coreCount, threadCount, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
                    new LinkedBlockingQueue<Runnable>(), new RejectedExecutionHandler()
            {
                @Override
                public void rejectedExecution(
                        Runnable r,
                        ThreadPoolExecutor executor)
                {
                    try {
                        executor.getQueue().put(r);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                }
            });
        //}

        // http://developer.android.com/reference/java/util/concurrent/ExecutorCompletionService.html
        int tilesSize = tiles.size();
        List<Future> futures = new ArrayList<>(tilesSize);

        for (int i = 0; i < tilesSize; ++i) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            final TileItem tile = tiles.get(i);

            futures.add(
                    mDrawThreadPool.submit(
                            new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    android.os.Process.setThreadPriority(
                                            Constants.DEFAULT_DRAW_THREAD_PRIORITY);

                                    final Bitmap bmp = tmsLayer.getBitmap(tile);
                                    if (bmp != null) {
                                        display.drawTile(bmp, tile.getPoint(), mRasterPaint);
                                    }
                                }
                            }));
        }

        // wait for draw ending
        int nStep = futures.size() / Constants.DRAW_NOTIFY_STEP_PERCENT;
        if(nStep == 0)
            nStep = 1;
        for (int i = 0, futuresSize = futures.size(); i < futuresSize; i++) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            try {
                Future future = futures.get(i);
                future.get(); // wait for task ending

                float percent = (float) i / futuresSize;
                if(i % nStep == 0) //0..10..20..30..40..50..60..70..80..90..100
                    tmsLayer.onDrawFinished(tmsLayer.getId(), percent);

                //Log.d(TAG, "TMS percent: " + percent + " complete: " + i +
                //       " tiles count: " + tilesSize + " layer: " + mLayer.getName());

            } catch (CancellationException | InterruptedException e) {
                //e.printStackTrace();
            } catch (ExecutionException e) {
                //e.printStackTrace();
            }
        }

        tmsLayer.onDrawFinished(tmsLayer.getId(), 1.0f);

        if(Constants.DEBUG_MODE) {
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;

            Log.d(TAG, "Raster layer " + tmsLayer.getName() + " exec time: " + elapsedTime);
        }
    }


    @Override
    public void cancelDraw()
    {
        if (mDrawThreadPool != null) {
            //synchronized (lock) {
                mDrawThreadPool.shutdownNow();
                try {
                    mDrawThreadPool.awaitTermination(TERMINATE_TIME, KEEP_ALIVE_TIME_UNIT);
                    //mDrawThreadPool.purge();
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            //}
        }
    }


    public boolean isForceToGrayScale()
    {
        return mForceToGrayScale;
    }


    public float getBrightness()
    {
        return mBrightness;
    }


    public float getContrast()
    {
        return mContrast;
    }

    public int getAlpha() {
        return mAlpha;
    }

    public void setAlpha(int alpha) {
        mAlpha = alpha;
        if (mRasterPaint != null) {
            mRasterPaint.setAlpha(mAlpha);
        }
    }

    public boolean isAntiAlias() {
        return mAntiAlias;
    }

    public void setAntiAlias(boolean antiAlias) {
        mAntiAlias = antiAlias;
        if (mRasterPaint != null) {
            mRasterPaint.setAntiAlias(mAntiAlias);
        }
    }
}
