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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.DEFAULT_MAX_ZOOM;
import static com.nextgis.maplib.util.GeoConstants.MERCATOR_MAX;


public class GISDisplay
{
    protected final Bitmap mBkBitmap;
    protected final int mTileSize = 256;
    protected final Paint       mRasterPaint;
    protected       Canvas      mMainCanvas;
    protected       Canvas      mBackgroundCanvas;
    protected       Bitmap      mMainBitmap;
    protected       Bitmap      mBackgroundBitmap;
    protected       GeoEnvelope mFullBounds;
    protected       GeoEnvelope mGeoLimits;
    protected       GeoEnvelope mCurrentBounds;
    protected       GeoPoint    mCenter;
    protected       GeoPoint    mMapTileSize;
    protected       Matrix      mTransformMatrix;
    protected       Matrix      mInvertTransformMatrix;
    protected       float       mMinZoomLevel;
    protected       float       mMaxZoomLevel;
    protected       float       mZoomLevel;
    protected       double      mScale;
    protected       double      mInvertScale;
    protected       float       mMainBitmapOffsetX;
    protected       float       mMainBitmapOffsetY;
    protected       GeoEnvelope mLimits;
    protected       GeoEnvelope mScreenBounds;
    protected       GeoEnvelope mOffScreenBounds;
    protected       int         mLimitType;


    public GISDisplay(Bitmap backgroundTile)
    {
        mBkBitmap = backgroundTile;
        //set max zoom
        mMinZoomLevel = 0;
        mMaxZoomLevel = DEFAULT_MAX_ZOOM;
        mLimitType = MAP_LIMITS_Y;

        mMainBitmap = null;
        mBackgroundBitmap = null;

        //default extent
        mFullBounds = new GeoEnvelope(-MERCATOR_MAX, MERCATOR_MAX, -MERCATOR_MAX,
                                      MERCATOR_MAX); //set full Mercator bounds
        mGeoLimits = mFullBounds;
        mCenter = mGeoLimits.getCenter();
        //default transform matrix
        mTransformMatrix = new Matrix();
        mInvertTransformMatrix = new Matrix();
        mMapTileSize = new GeoPoint();

        setSize(100, 100);

        mRasterPaint = new Paint();
        mRasterPaint.setAntiAlias(true);
        mRasterPaint.setFilterBitmap(true);
        mRasterPaint.setDither(true);
    }


    public void setSize(
            int w,
            int h)
    {
        Log.d(TAG, "new size: " + w + " x " + h);

        mScreenBounds = new GeoEnvelope(0, w, 0, h);
        double extraX = (w * OFFSCREEN_EXTRASIZE_RATIO - w) * .5;
        double extraY = (h * OFFSCREEN_EXTRASIZE_RATIO - h) * .5;
        mOffScreenBounds = new GeoEnvelope(-extraX, w + extraX, -extraY, h + extraY);

        // calc min zoom for no limits scenario
        // we calc the zoom level for full cover the whole display with tiles
        // if the zoom level is already set - do nothing
        mMinZoomLevel = (float) Math.ceil((float) Math.min(w, h) / mTileSize);

        mBackgroundBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mBackgroundCanvas = new Canvas(mBackgroundBitmap);

        mMainBitmap = Bitmap.createBitmap((int) (w * OFFSCREEN_EXTRASIZE_RATIO),
                                          (int) (h * OFFSCREEN_EXTRASIZE_RATIO),
                                          Bitmap.Config.ARGB_8888);
        mMainCanvas = new Canvas(mMainBitmap);

        mMainBitmapOffsetX = (mMainBitmap.getWidth() - mBackgroundBitmap.getWidth()) * .5f;
        mMainBitmapOffsetY = (mMainBitmap.getHeight() - mBackgroundBitmap.getHeight()) * .5f;

        if (mZoomLevel < mMinZoomLevel) {
            mZoomLevel = mMinZoomLevel;
        }
        //default zoom and center
        setZoomAndCenter(mZoomLevel, mCenter);
    }


    public void setZoomAndCenter(
            float zoom,
            GeoPoint center)
    {
        if (zoom > mMaxZoomLevel || zoom < mMinZoomLevel) {
            return;
        }
        mZoomLevel = zoom;

        mCenter = center;
        if (mCenter.getX() > MERCATOR_MAX * 2) {
            mCenter.setX(mCenter.getX() - MERCATOR_MAX * 2);
        }

        int nZoom = (int) Math.floor(zoom);
        Log.d(TAG, "Zoom: " + zoom + ", Center: " + center.toString());

        double mapTileSize = 1 << nZoom;
        mapTileSize *= 1 + zoom - nZoom;
        double mapPixelSize = mapTileSize * mTileSize;

        mMapTileSize.setCoordinates(mFullBounds.width() / mapTileSize,
                                    mFullBounds.height() / mapTileSize);

        double scaleX = mapPixelSize / mFullBounds.width();
        double scaleY = mapPixelSize / mFullBounds.height();

        mScale = (float) ((scaleX + scaleY) * .5);
        mInvertScale = 1 / mScale;

        //default transform matrix
        mTransformMatrix.reset();
        mTransformMatrix.postTranslate((float) -center.getX(), (float) -center.getY());
        mTransformMatrix.postScale((float) mScale, (float) -mScale);
        mTransformMatrix.postTranslate((float) (mBackgroundBitmap.getWidth() * .5),
                                       (float) (mBackgroundBitmap.getHeight() * .5));

        mInvertTransformMatrix.reset();
        mTransformMatrix.invert(mInvertTransformMatrix);

        Matrix matrix = new Matrix();
        matrix.postTranslate((float) -center.getX(), (float) -center.getY());
        matrix.postScale((float) mScale, (float) -mScale);
        matrix.postTranslate((float) (mMainBitmap.getWidth() * .5),
                             (float) (mMainBitmap.getHeight() * .5));
        mMainCanvas.setMatrix(matrix);

        RectF rect =
                new RectF(-mMainBitmapOffsetX, mBackgroundBitmap.getHeight() + mMainBitmapOffsetY,
                          mBackgroundBitmap.getWidth() + mMainBitmapOffsetX, -mMainBitmapOffsetY);
        mInvertTransformMatrix.mapRect(rect);

//        mCurrentBounds = new GeoEnvelope(rect.left, rect.right, rect.bottom, rect.top);
        mCurrentBounds =
                new GeoEnvelope(Math.min(rect.left, rect.right), Math.max(rect.left, rect.right),
                                Math.min(rect.bottom, rect.top), Math.max(rect.bottom, rect.top));
        //Log.d(TAG, "current: " + mCurrentBounds.toString());

        mLimits = mapToScreen(mGeoLimits);
        mLimits.fix();
        if (mLimitType == MAP_LIMITS_X || mLimitType == MAP_LIMITS_NO) {
            mLimits.setMinY(mLimits.getMinY() - mMainBitmap.getHeight() * 2);
            mLimits.setMaxY(mLimits.getMaxY() + mMainBitmap.getHeight() * 2);
        }

        if (mLimitType == MAP_LIMITS_Y || mLimitType == MAP_LIMITS_NO) {
            mLimits.setMinX(mLimits.getMinX() - mMainBitmap.getWidth() * 2);
            mLimits.setMaxX(mLimits.getMaxX() + mMainBitmap.getWidth() * 2);
        }
    }


    public GeoEnvelope mapToScreen(final GeoEnvelope env)
    {
        GeoEnvelope outEnv = new GeoEnvelope();
        RectF rect = new RectF();
        rect.set((float) env.getMinX(), (float) env.getMaxY(), (float) env.getMaxX(),
                 (float) env.getMinY());

        mTransformMatrix.mapRect(rect);
        outEnv.setMin(rect.left, rect.bottom);
        outEnv.setMax(rect.right, rect.top);

        return outEnv;
    }

    /*public void setTransformMatrix(final double zoom, final GeoPoint center) {
        int nZoom = (int) Math.floor(zoom);

        double mapTileSize = 1 << nZoom;
        mapTileSize *= 1 + zoom - nZoom;
        double mapPixelSize = mapTileSize * mTileSize;

        double scaleX = mapPixelSize / mFullBounds.width();
        double scaleY = mapPixelSize / mFullBounds.height();
        double scale = (float) ((scaleX + scaleY) / 2.0);

        //default transform matrix
        mTransformMatrix.reset();
        mTransformMatrix.postTranslate((float) -center.getX(), (float) -center.getY());
        mTransformMatrix.postScale((float) scale, (float) -scale);
        mTransformMatrix.postTranslate(
                (float) mBackgroundBitmap.getWidth() / 2, (float) mBackgroundBitmap.getHeight() / 2);
    }*/


    public void clearLayer()
    {
        mMainBitmap.eraseColor(Color.TRANSPARENT);
    }


    public final GeoEnvelope getLimits()
    {
        return new GeoEnvelope(mLimits);
    }


    public final GeoEnvelope getScreenBounds()
    {
        return new GeoEnvelope(mScreenBounds);
    }


    public final GeoEnvelope getOffScreenBounds()
    {
        return new GeoEnvelope(mOffScreenBounds);
    }

    /*public synchronized Bitmap getEditDisplay() {
        return mBackgroundBitmap;
    }

    public synchronized Canvas getEditCanvas() {
        return mBackgroundCanvas;
    }*/


    public synchronized Bitmap getDisplay(boolean clearBackground)
    {
        return getDisplay(0, 0, clearBackground);
    }


    public synchronized Bitmap getDisplay(
            float x,
            float y,
            boolean clearBackground)
    {
        if (clearBackground) {
            clearBackground();
        }
        synchronized (mMainBitmap) {
            mBackgroundCanvas.drawBitmap(mMainBitmap, x - mMainBitmapOffsetX,
                                         y - mMainBitmapOffsetY, null);
        }
        return mBackgroundBitmap;
    }


    public synchronized void clearBackground()
    {
        for (int i = 0; i < mBackgroundBitmap.getWidth(); i += mBkBitmap.getWidth()) {
            for (int j = 0; j < mBackgroundBitmap.getHeight(); j += mBkBitmap.getHeight()) {
                mBackgroundCanvas.drawBitmap(mBkBitmap, i, j, null);
            }
        }
    }


    public synchronized Bitmap getDisplay(
            float x,
            float y,
            float scale)
    {
        clearBackground();

        GeoPoint pt = getScaledOffset(x, y, scale);

        float mainBitmapOffsetX = (float) pt.getX();
        float mainBitmapOffsetY = (float) pt.getY();

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postTranslate(-mainBitmapOffsetX, -mainBitmapOffsetY);
        //Log.d(TAG, "matix: " + matrix.toShortString());

        synchronized (mMainBitmap) {
            mBackgroundCanvas.drawBitmap(mMainBitmap, matrix, mRasterPaint);
        }
        return mBackgroundBitmap;
    }


    public GeoPoint getScaledOffset(
            float x,
            float y,
            float scale)
    {
        float dxOld = x - mBackgroundBitmap.getWidth() / 2;
        float dyOld = y - mBackgroundBitmap.getHeight() / 2;

        float scaledWidth = mMainBitmap.getWidth() * scale;
        float scaledHeight = mMainBitmap.getHeight() * scale;

        GeoPoint ret = new GeoPoint();
        ret.setX((scaledWidth - mBackgroundBitmap.getWidth()) / 2 - (1 - scale) * dxOld);
        ret.setY((scaledHeight - mBackgroundBitmap.getHeight()) / 2 - (1 - scale) * dyOld);
        return ret;
    }


    public void drawTile(
            final Bitmap bitmap,
            final GeoPoint pt,
            Paint paint)
    {
        Matrix matrix = new Matrix();

        matrix.postScale((float) mInvertScale, (float) -mInvertScale);
        matrix.postTranslate((float) pt.getX(), (float) pt.getY());

        float scale = (float) (1 + mZoomLevel - Math.floor(mZoomLevel));

        if (bitmap.getWidth() != mTileSize) {
            scale = (float) mTileSize / bitmap.getWidth() * scale;
        }
        Matrix matrix1 = new Matrix();
        matrix1.postScale(scale, scale);
        matrix.preConcat(matrix1);


        synchronized (mMainBitmap) {
            if (paint == null) {
                mMainCanvas.drawBitmap(bitmap, matrix, mRasterPaint);
            } else {
                mMainCanvas.drawBitmap(bitmap, matrix, paint);
            }
        }
    }


    public void drawPoint(
            float x,
            float y,
            Paint paint)
    {
        mMainCanvas.drawPoint(x, y, paint);
    }


    public void drawLines(
            float[] pts,
            Paint paint)
    {
        mMainCanvas.drawLines(pts, paint);
    }


    public void drawCircle(
            float x,
            float y,
            float radius,
            Paint paint)
    {
        mMainCanvas.drawCircle(x, y, (float) (radius / mScale), paint);
    }


    public final double getScale()
    {
        return mScale;
    }


    public final GeoEnvelope getBounds()
    {
        return new GeoEnvelope(mCurrentBounds);
    }


    public final GeoEnvelope getFullBounds()
    {
        return new GeoEnvelope(mFullBounds);
    }


    public GeoPoint getTileSize()
    {
        return new GeoPoint(mMapTileSize);

        //RectF rect = new RectF(0, 0, mTileSize - 1, mTileSize - 1);
        //mInvertTransformMatrix.mapRect(rect);
        //return new double[] {rect.width(), rect.height()};
    }


    public GeoPoint screenToMap(final GeoPoint pt)
    {
        float points[] = new float[2];
        points[0] = (float) pt.getX();
        points[1] = (float) pt.getY();
        mInvertTransformMatrix.mapPoints(points);

        return new GeoPoint(points[0], points[1]);
    }


    public GeoPoint mapToScreen(final GeoPoint pt)
    {
        float points[] = new float[2];
        points[0] = (float) pt.getX();
        points[1] = (float) pt.getY();
        mTransformMatrix.mapPoints(points);

        return new GeoPoint(points[0], points[1]);
    }


    public GeoEnvelope screenToMap(final GeoEnvelope env)
    {
        GeoEnvelope outEnv = new GeoEnvelope();
        RectF rect = new RectF();
        rect.set((float) env.getMinX(), (float) env.getMaxY(), (float) env.getMaxX(),
                 (float) env.getMinY());

        mInvertTransformMatrix.mapRect(rect);
        outEnv.setMin(rect.left, rect.top); // screen axis Y and geo axis Y are inverse
        outEnv.setMax(rect.right, rect.bottom);


        return outEnv;
    }


    public float getMinZoomLevel()
    {
        return mMinZoomLevel;
    }


    public float getMaxZoomLevel()
    {
        return mMaxZoomLevel;
    }


    public GeoPoint getCenter()
    {
        return new GeoPoint(mCenter);
    }


    public void setGeoLimits(
            GeoEnvelope geoLimits,
            int limitType)
    {
        mGeoLimits = geoLimits;
        mLimitType = limitType;

        mLimits = mapToScreen(mGeoLimits);
        mLimits.fix();
        mLimits.adjust(mScreenBounds.width() / mScreenBounds.height());

        double scale = (float) (mScreenBounds.width()) / mLimits.width();
        double zoom = Math.log(scale) / Math.log(2.0);

        mMinZoomLevel = (float) Math.ceil(getZoomLevel() + zoom);
        mCenter = mGeoLimits.getCenter();
        setZoomAndCenter(mMinZoomLevel, mCenter);
        Log.d(TAG, "min zoom level: " + mMinZoomLevel + ", center:" + mCenter.toString());
    }


    public final float getZoomLevel()
    {
        return mZoomLevel;
    }


    public int getLimitType()
    {
        return mLimitType;
    }
}
