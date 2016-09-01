package com.nextgis.store.map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import com.nextgis.ngsandroid.NgsAndroidJni;
import com.nextgis.store.bindings.Api;
import com.nextgis.store.bindings.ProgressCallback;
import com.nextgis.store.bindings.RawPoint;

import java.nio.ByteBuffer;


public class MapDrawing
        extends MapStore
{
    protected ByteBuffer mBuffer;
    protected Bitmap     mDrawing;
    protected int        mWidth;
    protected int        mHeight;

    protected ProgressCallback  mDrawCallback;
    protected double            mDrawComplete;
    protected OnMapDrawListener mOnMapDrawListener;


    public MapDrawing(String mapPath)
    {
        super(mapPath);

        mDrawComplete = 0;

        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        mBuffer = ByteBuffer.allocateDirect(metrics.widthPixels * metrics.heightPixels * 4);

        mDrawCallback = createDrawCallback();
    }


    @Override
    public boolean createMap()
    {
        if (super.createMap()) {
            Api.ngsSetMapBackgroundColor(mMapId, (short) 0, (short) 255, (short) 0, (short) 255);
            return true;
        }
        return false;
    }


    @Override
    public boolean openMap()
    {
        if (super.openMap()) {
            Api.ngsSetMapBackgroundColor(mMapId, (short) 0, (short) 255, (short) 0, (short) 255);
            return true;
        }
        return false;
    }


    public Bitmap getDrawing()
    {
        return mDrawing;
    }


    public int getHeight()
    {
        return mHeight;
    }


    public int getWidth()
    {
        return mWidth;
    }


    public boolean isDrawingEmpty()
    {
        return (null == mDrawing);
    }


    public void draw()
    {
        mDrawComplete = 0;
        Api.ngsDrawMap(mMapId, mDrawCallback);
    }


    public void sizedDraw(
            int width,
            int height)
    {
        if (!setSize(width, height)) {
            return;
        }

//        Point center = new Point();
//        center.setX(308854.653167);
//        center.setY(4808439.3765);
//        Api.ngsSetMapCenter(mMapId, center);
//        Api.ngsSetMapScale(mMapId, 0.0003);

        draw();
    }


    public void centeredDraw(
            double centerX,
            double centerY)
    {
        setCenter(centerX, centerY);
        draw();
    }


    public void scaledDraw(
            double scaleFactor,
            double focusLocationX,
            double focusLocationY)
    {
        setScaleByFactor(scaleFactor);
        setScaledFocusLocation(scaleFactor, focusLocationX, focusLocationY);
        draw();
    }


    public boolean isSizeChanged(
            int width,
            int height)
    {
        return !(width == mWidth && height == mHeight);
    }


    public boolean setSize(
            int width,
            int height)
    {
        if (!isSizeChanged(width, height)) {
            return false;
        }

        mWidth = width;
        mHeight = height;
        Api.ngsInitMap(mMapId, mBuffer, width, height, 1);
        return true;
    }


    public void setCenter(
            double x,
            double y)
    {
        RawPoint center = new RawPoint();
        center.setX(x);
        center.setY(y);
        Api.ngsSetMapDisplayCenter(mMapId, center);
    }


    public PointF getCenter()
    {
        RawPoint center = new RawPoint();
        Api.ngsGetMapDisplayCenter(mMapId, center);
        PointF point = new PointF();
        point.x = (float) center.getX();
        point.y = (float) center.getY();
        return point;
    }


    public void setScaleByFactor(double scaleFactor)
    {
        double[] scale = new double[1];
        Api.ngsGetMapScale(mMapId, scale);
        scale[0] *= scaleFactor;
        Api.ngsSetMapScale(mMapId, scale[0]);
    }


    public void setScaledFocusLocation(
            double scaleFactor,
            double x,
            double y)
    {
        RawPoint center = new RawPoint();
        Api.ngsGetMapDisplayCenter(mMapId, center);

        double distX = center.getX() - x;
        double distY = center.getY() - y;
        double scaledDistX = distX * scaleFactor;
        double scaledDistY = distY * scaleFactor;
        double offX = scaledDistX - distX;
        double offY = scaledDistY - distY;

        center.setX(center.getX() - offX);
        center.setY(center.getY() - offY);
        Api.ngsSetMapDisplayCenter(mMapId, center);
    }


    protected ProgressCallback createDrawCallback()
    {
        return new ProgressCallback()
        {
            @Override
            public int run(
                    double complete,
                    String message)
            {
                if (complete - mDrawComplete > 0.045) { // each 5% redraw
                    mDrawComplete = complete;
                    if (null != mDrawing) {
                        mDrawing.recycle();
                    }
                    mDrawing = NgsAndroidJni.createBitmapFromBuffer(mBuffer, mWidth, mHeight);

                    if (null != mOnMapDrawListener) {
                        mOnMapDrawListener.onMapDraw();
                    }

//                    MapNativeView.this.postInvalidate();

//                    mDrawTime = System.currentTimeMillis() - mDrawTime;
//                    Log.d(TAG, "Native map draw_old time: " + mDrawTime);
                }

                return 1;
            }
        };
    }


    public void setOnMapDrawListener(OnMapDrawListener onMapDrawListener)
    {
        mOnMapDrawListener = onMapDrawListener;
    }


    interface OnMapDrawListener
    {
        void onMapDraw();
    }
}
