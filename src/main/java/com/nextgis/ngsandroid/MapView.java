package com.nextgis.ngsandroid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.DisplayMetrics;
import android.view.View;
import com.nextgis.store.Api;
import com.nextgis.store.ProgressCallback;

import java.nio.ByteBuffer;


public class MapView
        extends View
{
    protected static final int    DEFAULT_EPSG     = 3857;
    protected static final double DEFAULT_MAX_X    = 20037508.34; // 180.0
    protected static final double DEFAULT_MAX_Y    = 20037508.34; // 90.0
    protected static final double DEFAULT_MIN_X    = -DEFAULT_MAX_X;
    protected static final double DEFAULT_MIN_Y    = -DEFAULT_MAX_Y;
    protected static final String DEFAULT_MAP_NAME = "default";

    protected ByteBuffer mBuffer;
    protected Bitmap     mBitmap;
    protected int        mWidth;
    protected int        mHeight;

    protected long             mMapId;
    protected ProgressCallback mProgressCallback;

    protected double mDrawComplete = 0;


    public MapView(Context context)
    {
        super(context);

        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        mBuffer = ByteBuffer.allocateDirect(metrics.widthPixels * metrics.heightPixels * 4);

        mProgressCallback = new ProgressCallback()
        {
            @Override
            public int run(
                    double complete,
                    String message)
            {
                if (complete - mDrawComplete > 0.045) { // each 5% redraw
                    mDrawComplete = complete;
                    if (null != mBitmap) {
                        mBitmap.recycle();
                    }
                    mBitmap = NgsAndroidJni.fillBitmapFromBuffer(mBuffer, mWidth, mHeight);
                    MapView.this.postInvalidate();
                }

                return 1;
            }
        };

        newMap();
    }


    @Override
    protected void onDraw(Canvas canvas)
    {
        if (null != mBitmap) {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        } else {
            super.onDraw(canvas);
        }
    }


    @Override
    protected void onSizeChanged(
            int w,
            int h,
            int oldw,
            int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        setMapSize(w, h);
    }


    @Override
    protected void onLayout(
            boolean changed,
            int left,
            int top,
            int right,
            int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);
        setMapSize((right - left), (bottom - top));
    }


    protected void newMap()
    {
        mMapId = Api.ngsCreateMap(DEFAULT_MAP_NAME, "test gl map", DEFAULT_EPSG, DEFAULT_MIN_X,
                                  DEFAULT_MIN_Y, DEFAULT_MAX_X, DEFAULT_MAX_Y);

        if (mMapId != -1) {
            Api.ngsSetMapBackgroundColor(mMapId, (short) 0, (short) 255, (short) 0, (short) 255);
        }
    }


    protected void setMapSize(
            final int width,
            final int height)
    {
        mWidth = width;
        mHeight = height;

        Api.ngsInitMap(mMapId, mBuffer, width, height, 1);

        mDrawComplete = 0;
        Api.ngsDrawMap(mMapId, mProgressCallback);
    }
}
