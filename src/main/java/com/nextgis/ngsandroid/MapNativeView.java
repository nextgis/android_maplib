package com.nextgis.ngsandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.store.Api;
import com.nextgis.store.ErrorCodes;
import com.nextgis.store.RawPoint;
import com.nextgis.store.ProgressCallback;

import java.io.File;
import java.nio.ByteBuffer;

import static com.nextgis.maplib.util.Constants.TAG;


public class MapNativeView
        extends View
        implements GestureDetector.OnGestureListener,
                   GestureDetector.OnDoubleTapListener,
                   ScaleGestureDetector.OnScaleGestureListener
{
    protected static final int    DEFAULT_EPSG     = 3857;
    protected static final double DEFAULT_MAX_X    = 20037508.34; // 180.0
    protected static final double DEFAULT_MAX_Y    = 20037508.34; // 90.0
    protected static final double DEFAULT_MIN_X    = -DEFAULT_MAX_X;
    protected static final double DEFAULT_MIN_Y    = -DEFAULT_MAX_Y;
    protected static final String DEFAULT_MAP_NAME = "default";

    protected static final int DRAW_SATE_none              = 0;
    protected static final int DRAW_SATE_drawing           = 1;
    protected static final int DRAW_SATE_drawing_noclearbk = 2;
    protected static final int DRAW_SATE_panning           = 3;
    protected static final int DRAW_SATE_panning_fling     = 4;
    protected static final int DRAW_SATE_zooming           = 5;
    protected static final int DRAW_SATE_resizing          = 6;

    protected ByteBuffer mBuffer;
    protected Bitmap     mBitmap;
    protected int        mWidth;
    protected int        mHeight;

    protected ProgressCallback mDrawCallback;
    protected ProgressCallback mLoadCallback;

    protected long mMapId = -1;

    protected double mDrawComplete = 0;

    protected long mTime;

    protected GestureDetector      mGestureDetector;
    protected ScaleGestureDetector mScaleGestureDetector;
    protected Scroller             mScroller;
    protected PointF               mStartMouseLocation;
    protected PointF               mCurrentMouseOffset;
    protected PointF               mCurrentFocusLocation;
    protected PointF               mMapDisplayCenter;
    protected int                  mDrawingState;
    protected double               mScaleFactor;
    protected double               mCurrentSpan;
    protected long                 mStartDrawTime;


    public MapNativeView(Context context)
    {
        super(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        mGestureDetector = new GestureDetector(context, this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mScroller = new Scroller(context);

        mStartMouseLocation = new PointF();
        mCurrentMouseOffset = new PointF();
        mCurrentFocusLocation = new PointF();
        mMapDisplayCenter = new PointF();

        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        mBuffer = ByteBuffer.allocateDirect(metrics.widthPixels * metrics.heightPixels * 4);

        mDrawCallback = getDrawCallback();
        mLoadCallback = getLoadCallback();

        String mapPath = getMapPath();
        File gdalDir = new File(mapPath, "gdal_data");
        Api.ngsInit(gdalDir.getPath(), null);
        Log.d(TAG, "NGS formats: " + Api.ngsGetVersionString("formats"));

//        newMap();
//        loadMap();
        openMap();
    }


    protected void newMap()
    {
        int mapId = Api.ngsCreateMap(DEFAULT_MAP_NAME, "test gl map", DEFAULT_EPSG, DEFAULT_MIN_X,
                                     DEFAULT_MIN_Y, DEFAULT_MAX_X, DEFAULT_MAX_Y);

        if (mapId != -1) {
            mMapId = mapId;
            Api.ngsSetMapBackgroundColor(mMapId, (short) 0, (short) 255, (short) 0, (short) 255);

            RawPoint center = new RawPoint();
            Api.ngsGetMapDisplayCenter(mMapId, center);
            mMapDisplayCenter.x = (float) center.getX();
            mMapDisplayCenter.y = (float) center.getY();
        }
    }


    protected String getMapPath()
    {
        Context context = getContext();
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        File defaultPath = getContext().getExternalFilesDir(SettingsConstants.KEY_PREF_MAP);
        if (defaultPath == null) {
            defaultPath = new File(context.getFilesDir(), SettingsConstants.KEY_PREF_MAP);
        }
        return sharedPreferences.getString(SettingsConstants.KEY_PREF_MAP_PATH,
                                           defaultPath.getPath());
    }


    protected void openMap()
    {
        File mapNativePath = new File(getMapPath(), "test-desktop.ngmd");
//        File mapNativePath = new File(getMapPath(), "test-desktop-big.ngmd");
        int mapId = Api.ngsOpenMap(mapNativePath.getPath());
        if (-1 == mapId) {
            Log.d(TAG, "Error: Map load failed");
        } else {
            mMapId = mapId;
            Api.ngsSetMapBackgroundColor(mMapId, (short) 0, (short) 255, (short) 0, (short) 255);
        }
    }


    protected void loadMap()
    {
        String mapPath = getMapPath();
        File sceneDir = new File(mapPath, "scenes");
        File sceneFile = new File(sceneDir, "scenes.shp");
        String sceneFilePath = sceneFile.getPath();

        File dbPath = new File(mapPath, "test-db");
        File dbFile = new File(dbPath, "ngs.gpkg");

        if (Api.ngsInitDataStore(dbFile.getPath()) != ErrorCodes.SUCCESS) {
            Log.d(TAG, "Error: Storage initialize failed");
            return;
        }

        if (Api.ngsLoad("orbv3", sceneFilePath, "", false, 1, mLoadCallback)
                != ErrorCodes.SUCCESS) {
            Log.d(TAG, "Error: Load scene failed");
        }
    }


    protected ProgressCallback getLoadCallback()
    {
        return new ProgressCallback()
        {
            @Override
            public int run(
                    double complete,
                    String message)
            {
                if (complete >= 1) {
                    onLoadFinished();
                }

                return 1;
            }
        };
    }


    protected void onLoadFinished()
    {
        String mapPath = getMapPath();
        File dbPath = new File(mapPath, "test-db");
        File dbFile = new File(dbPath, "ngs.gpkg");
        File dbLayer = new File(dbFile, "orbv3");
        String layerPath = dbLayer.getPath();

        if (Api.ngsCreateLayer(mMapId, "orbv3", layerPath) == ErrorCodes.SUCCESS) {
            saveMap();
//            mDrawComplete = 0;
//            Api.ngsDrawMap (mMapId, mDrawCallback);
        }
    }


    protected void saveMap()
    {
        String mapPath = getMapPath();
        File ngmdFile = new File(mapPath, "test.ngmd");

        if (Api.ngsSaveMap(mMapId, ngmdFile.getPath()) != ErrorCodes.SUCCESS) {
            Log.d(TAG, "Error: Map save failed");
        }
    }


    @Override
    protected void onDraw(Canvas canvas)
    {
        //Log.d(TAG, "state: " + mDrawingState + ", current loc: " +  mCurrentMouseOffset.toString() + " current focus: " + mCurrentFocusLocation.toString() + " scale: "  + mScaleFactor);

        if (null == mBitmap) {
            super.onDraw(canvas);
            return;
        }

        switch (mDrawingState) {

            case DRAW_SATE_drawing:
                canvas.drawBitmap(mBitmap, 0, 0, null);
                break;

//TODO: add invalidate rect to prevent flicker
            case DRAW_SATE_drawing_noclearbk:
                canvas.drawBitmap(mBitmap, 0, 0, null);
                break;

            case DRAW_SATE_resizing:
                canvas.drawBitmap(mBitmap, 0, 0, null);
                break;

            case DRAW_SATE_panning:
            case DRAW_SATE_panning_fling:
                canvas.drawBitmap(mBitmap, -mCurrentMouseOffset.x, -mCurrentMouseOffset.y, null);
                break;

            case DRAW_SATE_zooming:
//                mMap.draw(
//                        canvas, -mCurrentFocusLocation.x, -mCurrentFocusLocation.y,
//                        (float) mScaleFactor);
                break;

            //case DRAW_SATE_none:
            //    break;

            default:
                break;
        }
    }


    protected void drawMap()
    {
        switch (mDrawingState) {

            case DRAW_SATE_drawing:
                break;

            case DRAW_SATE_drawing_noclearbk:
                break;

            case DRAW_SATE_resizing:
                mDrawComplete = 0;
                Api.ngsDrawMap(mMapId, mDrawCallback);
                break;

            case DRAW_SATE_panning:
            case DRAW_SATE_panning_fling:
                mDrawingState = DRAW_SATE_drawing;

                RawPoint center = new RawPoint();
                center.setX(mMapDisplayCenter.x);
                center.setY(mMapDisplayCenter.y);
                Api.ngsSetMapDisplayCenter(mMapId, center);

                mDrawComplete = 0;
                Api.ngsDrawMap(mMapId, mDrawCallback);
                break;

            case DRAW_SATE_zooming:
                break;

            //case DRAW_SATE_none:
            //    break;

            default:
                break;
        }
    }


    protected ProgressCallback getDrawCallback()
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
                    if (null != mBitmap) {
                        mBitmap.recycle();
                    }
                    mBitmap = NgsAndroidJni.fillBitmapFromBuffer(mBuffer, mWidth, mHeight);
                    MapNativeView.this.postInvalidate();

                    mTime = System.currentTimeMillis() - mTime;
                    Log.d(TAG, "Native map draw time: " + mTime);
                }

                return 1;
            }
        };
    }


    protected void setMapSize(
            final int width,
            final int height)
    {
        if (width == mWidth && height == mHeight) {
            return;
        }

        mTime = System.currentTimeMillis();

        mDrawingState = DRAW_SATE_resizing;
        mWidth = width;
        mHeight = height;

        Api.ngsInitMap(mMapId, mBuffer, width, height, 1);

//        Point center = new Point();
//        center.setX(308854.653167);
//        center.setY(4808439.3765);
//        Api.ngsSetMapCenter(mMapId, center);
//        Api.ngsSetMapScale(mMapId, 0.0003);

        drawMap();
    }


    protected void panStart(final MotionEvent e)
    {

        if (mDrawingState == DRAW_SATE_zooming || mDrawingState == DRAW_SATE_panning ||
                mDrawingState == DRAW_SATE_panning_fling) {
            return;
        }

        //Log.d(TAG, "panStart");

        mDrawingState = DRAW_SATE_panning;
        mStartMouseLocation.set(e.getX(), e.getY());
        mCurrentMouseOffset.set(0, 0);

        RawPoint center = new RawPoint();
        Api.ngsGetMapDisplayCenter(mMapId, center);
        mMapDisplayCenter.x = (float) center.getX();
        mMapDisplayCenter.y = (float) center.getY();
    }


    protected void panMoveTo(final MotionEvent e)
    {

        if (mDrawingState == DRAW_SATE_zooming || mDrawingState == DRAW_SATE_drawing_noclearbk ||
                mDrawingState == DRAW_SATE_drawing) {
            return;
        }

        if (mDrawingState == DRAW_SATE_panning /*&& mMap != null*/) {
            float x = mStartMouseLocation.x - e.getX();
            float y = mStartMouseLocation.y - e.getY();

            //Log.d(TAG, "panMoveTo x - " + x + " y - " + y);

            mCurrentMouseOffset.set(x, y);
            invalidate();
        }
    }


    protected void panStop()
    {
        //Log.d(Constants.TAG, "panStop state: " + mDrawingState);

        if (mDrawingState == DRAW_SATE_panning /*&& mMap != null*/) {

            float x = mCurrentMouseOffset.x;
            float y = mCurrentMouseOffset.y;

            //Log.d(TAG, "panStop x - " + x + " y - " + y);
            //Log.d(TAG, "panStop: drawMap");

            mMapDisplayCenter.offset(x, y);
            drawMap();
        }
    }


    protected void zoomStart(ScaleGestureDetector scaleGestureDetector)
    {

//        if (mDrawingState == DRAW_SATE_zooming) {
//            return;
//        }
//
//        mDrawingState = DRAW_SATE_zooming;
//        mCurrentSpan = scaleGestureDetector.getCurrentSpan();
//        mCurrentFocusLocation.set(
//                -scaleGestureDetector.getFocusX(), -scaleGestureDetector.getFocusY());
//        mScaleFactor = 1.f;
//
//        mMap.buffer(0, 0, 1);
    }


    protected void zoom(ScaleGestureDetector scaleGestureDetector)
    {
//        if (mDrawingState != DRAW_SATE_zooming) {
//            zoomStart(scaleGestureDetector);
//        }
//
//        if (mDrawingState == DRAW_SATE_zooming && mMap != null) {
//            double scaleFactor =
//                    scaleGestureDetector.getScaleFactor() * scaleGestureDetector.getCurrentSpan() /
//                            mCurrentSpan;
//            double zoom = MapUtil.getZoomForScaleFactor(scaleFactor, mMap.getZoomLevel());
//            if (zoom < mMap.getMinZoom() || zoom > mMap.getMaxZoom()) {
//                return;
//            }
//
//            mScaleFactor = scaleFactor;
//            mMap.buffer(0, 0, 1);
//            invalidate();
//        }
    }


    protected void zoomStop()
    {
//        if (mDrawingState == DRAW_SATE_zooming && mMap != null) {
//
//            float zoom = MapUtil.getZoomForScaleFactor(mScaleFactor, mMap.getZoomLevel());
//
//            GeoEnvelope env = mMap.getFullScreenBounds();
//            GeoPoint focusPt = new GeoPoint(-mCurrentFocusLocation.x, -mCurrentFocusLocation.y);
//
//            double invertScale = 1 / mScaleFactor;
//
//            double offX = (1 - invertScale) * focusPt.getX();
//            double offY = (1 - invertScale) * focusPt.getY();
//            env.scale(invertScale);
//            env.offset(offX, offY);
//
//            GeoPoint newCenterPt = env.getCenter();
//            GeoPoint newCenterPtMap = mMap.screenToMap(newCenterPt);
//
//            //Log.d(TAG, "zoomStop: setZoomAndCenter");
//
//            setZoomAndCenter(zoom, newCenterPtMap);
//        }
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


    @Override
    public boolean onScroll(
            MotionEvent event1,
            MotionEvent event2,
            float distanceX,
            float distanceY)
    {
        if (event2.getPointerCount() > 1) {
            return false;
        }
        //Log.d(TAG, "onScroll: " + event1.toString() + ", " + event2.toString() + ", "
        //           + distanceX + ", " + distanceY);

        panStart(event1);
        panMoveTo(event2);
        return true;
    }


    @Override
    public void computeScroll()
    {
        super.computeScroll();
        if (mDrawingState == DRAW_SATE_panning_fling /*&& mMap != null*/) {
            if (mScroller.computeScrollOffset()) {
                if (mScroller.isFinished()) {
                    mDrawingState = DRAW_SATE_panning;
                    panStop();
                } else {
                    float x = mScroller.getCurrX();
                    float y = mScroller.getCurrY();
                    mCurrentMouseOffset.set(x, y);
                    postInvalidate();
                }
            } else if (mScroller.isFinished()) {
                mDrawingState = DRAW_SATE_panning;
                panStop();
            }
        }
    }


    // delegate the event to the gesture detector
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mScaleGestureDetector.onTouchEvent(event);

        if (!mGestureDetector.onTouchEvent(event)) {
            // TODO: get action can be more complicated:
            // TODO: if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN)
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!mScroller.isFinished()) {
                        mScroller.forceFinished(true);
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    break;

                case MotionEvent.ACTION_UP:
                    panStop();
                    break;

                default:
                    break;
            }
        }

        return true;
    }


    @Override
    public boolean onFling(
            MotionEvent e1,
            MotionEvent e2,
            float velocityX,
            float velocityY)
    {
        if (null == mBitmap) //fling not always exec panStop
        {
            return false;
        }

        if (mDrawingState == DRAW_SATE_zooming || mDrawingState == DRAW_SATE_drawing_noclearbk ||
                mDrawingState == DRAW_SATE_drawing) {
            return false;
        }

        mDrawingState = DRAW_SATE_panning_fling;
        float x = mCurrentMouseOffset.x;
        float y = mCurrentMouseOffset.y;

        mScroller.forceFinished(true);
        mScroller.fling(
                (int) x, (int) y, -(int) velocityX, -(int) velocityY, 0, mWidth, 0, mHeight);

        postInvalidate();

        //Log.d(Constants.TAG, "Fling");
        return true;
    }


    @Override
    public boolean onScale(ScaleGestureDetector detector)
    {
        //Log.d(TAG, "onScale");
        zoom(detector);
        return true;
    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector)
    {
        zoomStart(detector);
        return true;
    }


    @Override
    public void onScaleEnd(ScaleGestureDetector detector)
    {
        zoomStop();
    }


    @Override
    public boolean onDoubleTap(MotionEvent e)
    {
//        if (mMap == null) {
//            return false;
//        }
//
//        mDrawingState = DRAW_SATE_zooming;
//        mScaleFactor = 2;
//        mCurrentFocusLocation.set(-e.getX(), -e.getY());
//        invalidate();
//
//        GeoEnvelope env = mMap.getFullScreenBounds();
//        GeoPoint focusPt = new GeoPoint(-mCurrentFocusLocation.x, -mCurrentFocusLocation.y);
//
//        double invertScale = 1 / mScaleFactor;
//
//        double offX = (1 - invertScale) * focusPt.getX();
//        double offY = (1 - invertScale) * focusPt.getY();
//        env.scale(invertScale);
//        env.offset(offX, offY);
//
//        GeoPoint newCenterPt = env.getCenter();
//        GeoPoint newCenterPtMap = mMap.screenToMap(newCenterPt);
//
//        //Log.d(TAG, "onDoubleTap: setZoomAndCenter");
//
//        mMap.buffer(0, 0, 1);
//        setZoomAndCenter((float) Math.ceil(getZoomLevel() + 0.5), newCenterPtMap);
//
//        return true;
        return false;
    }


    @Override
    public boolean onDown(MotionEvent e)
    {
        return false;
    }


    @Override
    public void onShowPress(MotionEvent e)
    {
    }


    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        return false;
    }


    @Override
    public void onLongPress(MotionEvent e)
    {
    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent e)
    {
        return false;
    }


    @Override
    public boolean onDoubleTapEvent(MotionEvent e)
    {
        return false;
    }
}
