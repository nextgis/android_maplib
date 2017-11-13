/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.Log;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.util.Constants;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.DEFAULT_MAX_ZOOM;
import static com.nextgis.maplib.util.GeoConstants.MERCATOR_MAX;


public class GISDisplay
{
    protected       Bitmap mBkBitmap;
    protected final int mTileSize = DEFAULT_TILE_SIZE;
    protected final Paint       mRasterPaint;
    protected       Canvas      mMainCanvas;
    protected       Bitmap      mMainBitmap;
    protected       Canvas      mDoubleBufferCanvas;
    protected       Bitmap      mDoubleBufferBitmap;
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
    protected       int         mWidth, mHeight;


    public GISDisplay(Bitmap backgroundTile)
    {
        mBkBitmap = backgroundTile;
        //set max zoom
        mMinZoomLevel = 0;
        mMaxZoomLevel = DEFAULT_MAX_ZOOM;
        mLimitType = MAP_LIMITS_Y;

        // default extent
        // set full Mercator bounds
        mFullBounds = new GeoEnvelope(-MERCATOR_MAX, MERCATOR_MAX, -MERCATOR_MAX, MERCATOR_MAX);
        mGeoLimits = mFullBounds;
        mCenter = mGeoLimits.getCenter();

        //default transform matrix
        mTransformMatrix = new Matrix();
        mInvertTransformMatrix = new Matrix();
        mMapTileSize = new GeoPoint();

        setSize(300, 300);

        mRasterPaint = new Paint();
        mRasterPaint.setAntiAlias(true);
        mRasterPaint.setFilterBitmap(true);
        mRasterPaint.setDither(true);
    }


    public boolean setSize(
            int w,
            int h)
    {
        if ((mWidth == w && mHeight == h) || w < 1 || h < 1) {
            return false;
        }
        mMainBitmap = null;
        mMainCanvas = null;
        mDoubleBufferBitmap = null;
        mDoubleBufferCanvas = null;

        mWidth = w;
        mHeight = h;

        if(Constants.DEBUG_MODE)
            Log.d(TAG, "new size: " + w + " x " + h);

        mScreenBounds = new GeoEnvelope(0, w, 0, h);
        double extraX = (w * OFFSCREEN_EXTRASIZE_RATIO - w) * .5;
        double extraY = (h * OFFSCREEN_EXTRASIZE_RATIO - h) * .5;
        mOffScreenBounds = new GeoEnvelope(-extraX, w + extraX, -extraY, h + extraY);

        // calc min zoom for no limits scenario
        // we calc the zoom level for full cover the whole display with tiles
        // if the zoom level is already set - do nothing
        float zoom = (float) Math.max(w, h) / mTileSize;
        mMinZoomLevel = (float) Math.ceil(Math.log(zoom) / Math.log(2.0));

        if (mMinZoomLevel < 0) {
            mMinZoomLevel = 0;
        }

        mMainBitmap = Bitmap.createBitmap(
                (int) (w * OFFSCREEN_EXTRASIZE_RATIO), (int) (h * OFFSCREEN_EXTRASIZE_RATIO),
                Bitmap.Config.ARGB_8888);
        mMainCanvas = new Canvas(mMainBitmap);

        mMainBitmapOffsetX = (mMainBitmap.getWidth() - w) * .5f;
        mMainBitmapOffsetY = (mMainBitmap.getHeight() - h) * .5f;

        if (mZoomLevel < mMinZoomLevel) {
            mZoomLevel = mMinZoomLevel;
        }

        // create double buffer
        mDoubleBufferBitmap = Bitmap.createBitmap(
                (int) (w * OFFSCREEN_EXTRASIZE_RATIO), (int) (h * OFFSCREEN_EXTRASIZE_RATIO),
                Bitmap.Config.ARGB_8888);
        if(null != mDoubleBufferBitmap)
            mDoubleBufferCanvas = new Canvas(mDoubleBufferBitmap);

        //default zoom and center
        setZoomAndCenter(mZoomLevel, mCenter);

        return true;
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
        if (mCenter.getX() > MERCATOR_MAX) {
            mCenter.setX(mCenter.getX() - MERCATOR_MAX*2);
        } else if (mCenter.getX() < -MERCATOR_MAX) {
            mCenter.setX(mCenter.getX() + MERCATOR_MAX*2);
        }

        int nZoom = (int) Math.floor(zoom);
        if(Constants.DEBUG_MODE)
            Log.d(TAG, "Zoom: " + zoom + ", Center: " + center.toString());

        double mapTileSize = 1 << nZoom;
        mapTileSize *= 1 + zoom - nZoom;
        double mapPixelSize = mapTileSize * mTileSize;

        mMapTileSize.setCoordinates(
                mFullBounds.width() / mapTileSize, mFullBounds.height() / mapTileSize);

        double scaleX = mapPixelSize / mFullBounds.width();
        double scaleY = mapPixelSize / mFullBounds.height();

        mScale = (float) ((scaleX + scaleY) * .5);
        mInvertScale = 1 / mScale;

        //default transform matrix
        mTransformMatrix.reset();
        mTransformMatrix.postTranslate((float) -mCenter.getX(), (float) -mCenter.getY());
        mTransformMatrix.postScale((float) mScale, (float) -mScale);
        mTransformMatrix.postTranslate((float) (mWidth * .5), (float) (mHeight * .5));

        mInvertTransformMatrix.reset();
        boolean operationSuccess = mTransformMatrix.invert(mInvertTransformMatrix);
        //found that matrix may not be inverted on some devices, so look for zoom there it inverts successfully
        if (!operationSuccess) {
            if (zoom >= mMinZoomLevel) {
                mMinZoomLevel += .5;
                if (mMinZoomLevel >= mMaxZoomLevel - 1) {
                    throw new IllegalArgumentException("The transformation matrix is invalid");
                }
                setZoomAndCenter(zoom + .5f, center);
                return;
            }
        }

        Matrix matrix = new Matrix();
        matrix.postTranslate((float) -mCenter.getX(), (float) -mCenter.getY());
        matrix.postScale((float) mScale, (float) -mScale);
        matrix.postTranslate(
                (float) (mMainBitmap.getWidth() * .5), (float) (mMainBitmap.getHeight() * .5));
        mMainCanvas.setMatrix(matrix);

        RectF rect = new RectF(
                -mMainBitmapOffsetX, mHeight + mMainBitmapOffsetY, mWidth + mMainBitmapOffsetX,
                -mMainBitmapOffsetY);
        mInvertTransformMatrix.mapRect(rect);

        mCurrentBounds = new GeoEnvelope(
                Math.min(rect.left, rect.right), Math.max(rect.left, rect.right),
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
        rect.set(
                (float) env.getMinX(), (float) env.getMaxY(), (float) env.getMaxX(),
                (float) env.getMinY());

        mTransformMatrix.mapRect(rect);
        outEnv.setMin(rect.left, rect.bottom);
        outEnv.setMax(rect.right, rect.top);

        return outEnv;
    }


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


    public void draw(
            Canvas canvas,
            boolean clearBackground)
    {
        draw(canvas, 0, 0, clearBackground);
    }


    public synchronized void draw(
            Canvas canvas,
            float x,
            float y,
            boolean clearBackground)
    {
        if (clearBackground) {
            clearBackground(canvas);
        }

        if (null == canvas) {
            return;
        }

        if (mDoubleBufferBitmap == null || mDoubleBufferBitmap.isRecycled())
            return;

        canvas.drawBitmap(
                mDoubleBufferBitmap, x - mMainBitmapOffsetX, y - mMainBitmapOffsetY, null);
    }


    public void clearBackground(Canvas canvas)
    {
        if (null == canvas) {
            return;
        }

        //Log.d(TAG, "clearBackground(), w: " + mBackgroundBitmap.getWidth() + " h: " + mBackgroundBitmap.getHeight());

        for (int i = 0; i < mWidth; i += mBkBitmap.getWidth()) {
            for (int j = 0; j < mHeight; j += mBkBitmap.getHeight()) {
                canvas.drawBitmap(mBkBitmap, i, j, null);
            }
        }
    }


    public synchronized void draw(
            Canvas canvas,
            float x,
            float y,
            float scale)
    {
        clearBackground(canvas);

        if (null == canvas) {
            return;
        }

        GeoPoint pt = getScaledOffset(x, y, scale);

        float mainBitmapOffsetX = (float) pt.getX();
        float mainBitmapOffsetY = (float) pt.getY();

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postTranslate(-mainBitmapOffsetX, -mainBitmapOffsetY);
        //Log.d(TAG, "matix: " + matrix.toShortString());

        canvas.drawBitmap(mDoubleBufferBitmap, matrix, mRasterPaint);
    }


    public GeoPoint getScaledOffset(
            float x,
            float y,
            float scale)
    {
        float dxOld = x - mWidth / 2;
        float dyOld = y - mHeight / 2;

        float scaledWidth = mMainBitmap.getWidth() * scale;
        float scaledHeight = mMainBitmap.getHeight() * scale;

        GeoPoint ret = new GeoPoint();
        ret.setX((scaledWidth - mWidth) / 2 - (1 - scale) * dxOld);
        ret.setY((scaledHeight - mHeight) / 2 - (1 - scale) * dyOld);
        return ret;
    }


    public void drawTile(
            final Bitmap bitmap,
            final GeoPoint pt,
            Paint paint)
    {
        if (null == mMainCanvas || null == pt || null == bitmap) {
            return;
        }

        Matrix matrix = new Matrix();

        matrix.postScale((float) mInvertScale, (float) -mInvertScale);
        matrix.postTranslate((float) pt.getX(), (float) pt.getY());

        float scale = (float) (1.0036 + mZoomLevel -
                               Math.floor(mZoomLevel)); // 1.0036 ~ 256 / 255 - add one extra pixel

        if (bitmap.getWidth() != mTileSize) {
            scale = (float) mTileSize / bitmap.getWidth() * scale;
        }
        Matrix matrix1 = new Matrix();
        matrix1.postScale(scale, scale);
        matrix.preConcat(matrix1);

        synchronized(this) {
            if (paint == null) {
                mMainCanvas.drawBitmap(bitmap, matrix, mRasterPaint);
            } else {
                mMainCanvas.drawBitmap(bitmap, matrix, paint);
            }
        }
    }


    public void drawBitmap(
            Bitmap bitmap,
            GeoPoint point,
            float offsetX,
            float offsetY)
    {
        if (null == mMainCanvas) {
            return;
        }

        Matrix matrix = new Matrix();
        matrix.postScale((float) mInvertScale, (float) -mInvertScale);
        matrix.postTranslate((float) point.getX(), (float) point.getY());
        matrix.postTranslate((float) (offsetX / mScale), (float) (offsetY / mScale));
        mMainCanvas.drawBitmap(bitmap, matrix, new Paint(Paint.ANTI_ALIAS_FLAG));
    }


    public void drawTextOnPath(
            String text,
            Path path,
            float hOffset,
            float vOffset,
            Paint paint)
    {
        if (null == mMainCanvas) {
            return;
        }

        PathMeasure pm = new PathMeasure(path, false);
        Matrix matrix = new Matrix();
        Path charPath = new Path();
        Path textPath = new Path();

        float pathLength = pm.getLength();
        float coordinates[] = new float[2];
        float tangent[] = new float[2];

        int i = 0;
        float position = hOffset;

        while (i < text.length()) {
            String ch = text.substring(i, i + 1);
            float charWidth = paint.measureText(ch);

            float nextPosition = position + charWidth;
            if (nextPosition > pathLength) {
                break;
            }

            pm.getPosTan(position + charWidth / 2, coordinates, tangent);
            float rotateAngle = (float) Math.toDegrees(
                    Math.atan2((double) tangent[1], (double) tangent[0]));

            charPath.reset();
            paint.getTextPath(ch, 0, ch.length(), -charWidth / 2, vOffset, charPath);
            charPath.close(); // workaround

            matrix.reset();
            matrix.postScale(1, -1, 0, 0);
            matrix.postRotate(rotateAngle, 0, 0);
            matrix.postTranslate(coordinates[0], coordinates[1]);

            textPath.addPath(charPath, matrix);

            ++i;
            position = nextPosition;
        }

        mMainCanvas.drawPath(textPath, paint);

        // for debug
        //mMainCanvas.drawTextOnPath(text, path, hOffset, vOffset, paint);
    }


    public void drawPoint(
            float x,
            float y,
            Paint paint)
    {
        if (null == mMainCanvas) {
            return;
        }
        mMainCanvas.drawPoint(x, y, paint);
    }


    public void drawLine(
            float x0,
            float y0,
            float x1,
            float y1,
            Paint paint)
    {
        if (null == mMainCanvas) {
            return;
        }
        checkStrokeWidth(paint);
        mMainCanvas.drawLine(x0, y0, x1, y1, paint);
    }


    public void drawLines(
            float[] pts,
            Paint paint)
    {
        if (null == mMainCanvas) {
            return;
        }
        checkStrokeWidth(paint);
        mMainCanvas.drawLines(pts, paint);
    }


    public void drawCircle(
            float x,
            float y,
            Paint paint)
    {
        if (null == mMainCanvas) {
            return;
        }

        paint.setAntiAlias(true);
        float r = (float) Math.ceil(paint.getStrokeWidth() * 1.5);
        if (r > 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(r);
        }
        mMainCanvas.drawPoint(x, y, paint);
    }


    public void drawCircle(
            float x,
            float y,
            float radius,
            Paint paint)
    {
        if (null == mMainCanvas) {
            return;
        }
        checkStrokeWidth(paint);
        mMainCanvas.drawCircle(x, y, radius, paint);
    }


    public void drawBox(
            float x,
            float y,
            float size,
            Paint paint)
    {
        if (null == mMainCanvas) {
            return;
        }
        checkStrokeWidth(paint);
        mMainCanvas.drawRect(x - size, y - size, x + size, y + size, paint);
    }


    public void drawCrossedBox(
            float x,
            float y,
            float size,
            Paint paint)
    {
        if (null == mMainCanvas) {
            return;
        }
        mMainCanvas.drawRect(x - size, y - size, x + size, y + size, paint);
        drawCross(x, y, size, paint);
    }


    public void drawCross(
            float x,
            float y,
            float size,
            Paint paint)
    {
        if (null == mMainCanvas) {
            return;
        }
        mMainCanvas.drawLine(x - size, y - size, x + size, y + size, paint);
        mMainCanvas.drawLine(x - size, y + size, x + size, y - size, paint);
    }


    public void drawPath(
            Path path,
            Paint paint)
    {
        if (null == mMainCanvas) {
            return;
        }
        checkStrokeWidth(paint);
        mMainCanvas.drawPath(path, paint);
    }


    private void checkStrokeWidth(Paint paint) {
        if (paint.getStyle() == Paint.Style.STROKE && paint.getStrokeWidth() < 1f)
            paint.setStrokeWidth(0);
    }


    public final double getScale()
    {
        return mScale;
    }


    public final GeoEnvelope getBounds() {
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


    public GeoPoint[] screenToMap(final float[] points)
    {
        mInvertTransformMatrix.mapPoints(points);
        GeoPoint[] ret = new GeoPoint[points.length / 2];
        int count = 0;
        for (int i = 0; i < points.length - 1; i += 2) {
            GeoPoint point = new GeoPoint(points[i], points[i + 1]);
            point.setCRS(CRS_WEB_MERCATOR);
            ret[count++] = point;
        }
        return ret;
    }


    public GeoPoint mapToScreen(final GeoPoint pt)
    {
        float points[] = new float[2];
        points[0] = (float) pt.getX();
        points[1] = (float) pt.getY();
        mTransformMatrix.mapPoints(points);

        return new GeoPoint(points[0], points[1]);
    }


    public float[] mapToScreen(final GeoPoint[] points)
    {
        if (null == points) {
            return null;
        }
        float dfPoints[] = new float[points.length * 2];
        for (int i = 0; i < points.length; i++) {
            int pos = i * 2;
            dfPoints[pos] = (float) points[i].getX();
            dfPoints[pos + 1] = (float) points[i].getY();
        }

        mTransformMatrix.mapPoints(dfPoints);

        return dfPoints;
    }


    public GeoEnvelope screenToMap(final GeoEnvelope env)
    {
        GeoEnvelope outEnv = new GeoEnvelope();
        RectF rect = new RectF();
        rect.set(
                (float) env.getMinX(), (float) env.getMaxY(), (float) env.getMaxX(),
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
        if(Constants.DEBUG_MODE)
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


    public void buffer(
            float x,
            float y,
            float scale)
    {
        mDoubleBufferBitmap.eraseColor(Color.TRANSPARENT);

        if (scale == 1) {
            mDoubleBufferCanvas.drawBitmap(mMainBitmap, x, y, null);
        } else {
            GeoPoint pt = getScaledOffset(x, y, scale);

            float mainBitmapOffsetX = (float) pt.getX();
            float mainBitmapOffsetY = (float) pt.getY();

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            matrix.postTranslate(-mainBitmapOffsetX, -mainBitmapOffsetY);

            mDoubleBufferCanvas.drawBitmap(mMainBitmap, matrix, mRasterPaint);
        }
    }


    public void setBackground(Bitmap bkBitmap) {
        mBkBitmap = bkBitmap;
    }

    public void setMaxZoomLevel(float maxZoom) {
        mMaxZoomLevel = maxZoom;
    }

    public void setMinZoomLevel(float minZoom) {
        mMinZoomLevel = minZoom;
    }
}
