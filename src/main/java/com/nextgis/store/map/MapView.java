package com.nextgis.store.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;
import com.nextgis.maplib.util.SettingsConstants;

import java.io.File;

import static com.nextgis.maplib.util.Constants.TAG;


public class MapView
        extends View
        implements MapDrawing.OnMapDrawListener,
                   GestureDetector.OnGestureListener,
                   GestureDetector.OnDoubleTapListener,
                   ScaleGestureDetector.OnScaleGestureListener
{
    protected static final int DRAW_STATE_none              = 0;
    protected static final int DRAW_STATE_drawing           = 1;
    protected static final int DRAW_STATE_drawing_noclearbk = 2;
    protected static final int DRAW_STATE_panning           = 3;
    protected static final int DRAW_STATE_panning_fling     = 4;
    protected static final int DRAW_STATE_zooming           = 5;
    protected static final int DRAW_STATE_resizing          = 6;

    protected GestureDetector      mGestureDetector;
    protected ScaleGestureDetector mScaleGestureDetector;
    protected Scroller             mScroller;

    protected MapDrawing mMapDrawing;

    protected PointF mStartDragLocation;
    protected PointF mCurrentDragOffset;
    protected PointF mCurrentFocusLocation;
    protected PointF mCurrentFocusOffset;
    protected PointF mMapDisplayCenter;

    protected int    mDrawingState;
    protected double mScaleFactor;
    protected double mCurrentSpan;

    protected long mDrawTime;


    public MapView(Context context)
    {
        super(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        mGestureDetector = new GestureDetector(context, this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mScroller = new Scroller(context);

        mStartDragLocation = new PointF();
        mCurrentDragOffset = new PointF();
        mCurrentFocusLocation = new PointF();
        mCurrentFocusOffset = new PointF();
        mMapDisplayCenter = new PointF();

        mMapDrawing = new MapDrawing(getMapPath());
        mMapDrawing.setOnMapDrawListener(this);

//        mMapDrawing.createMap();
//        mMapDrawing.loadMap();
        mMapDrawing.openMap();
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


    @Override
    protected void onDraw(Canvas canvas)
    {
        //Log.d(
        //        TAG, "state: " + mDrawingState + ", current loc: " + mCurrentDragOffset.toString()
        //                + " current focus: " + mCurrentFocusLocation.toString() + " scale: "
        //                + mScaleFactor);

        if (mMapDrawing.isDrawingEmpty()) {
            super.onDraw(canvas);
            return;
        }

        Bitmap drawing = mMapDrawing.getDrawing();

        switch (mDrawingState) {

            case DRAW_STATE_drawing:
                canvas.drawBitmap(drawing, 0, 0, null);
                break;

//TODO: add invalidate rect to prevent flicker
            case DRAW_STATE_drawing_noclearbk:
                canvas.drawBitmap(drawing, 0, 0, null);
                break;

            case DRAW_STATE_resizing:
                canvas.drawBitmap(drawing, 0, 0, null);
                break;

            case DRAW_STATE_panning:
            case DRAW_STATE_panning_fling:
                canvas.drawBitmap(drawing, -mCurrentDragOffset.x, -mCurrentDragOffset.y, null);
                break;

            case DRAW_STATE_zooming:
                canvas.save();
                canvas.scale((float) mScaleFactor, (float) mScaleFactor);
                canvas.drawBitmap(drawing, -mCurrentFocusOffset.x, -mCurrentFocusOffset.y, null);
                canvas.restore();
                break;

            //case DRAW_STATE_none:
            //    break;

            default:
                break;
        }
    }


    protected void drawMap()
    {
        mDrawTime = System.currentTimeMillis();

        switch (mDrawingState) {

            case DRAW_STATE_drawing:
                break;

            case DRAW_STATE_drawing_noclearbk:
                break;

            case DRAW_STATE_resizing:
                mMapDrawing.draw();
                break;

            case DRAW_STATE_panning:
            case DRAW_STATE_panning_fling: {
                mMapDrawing.centeredDraw(mMapDisplayCenter.x, mMapDisplayCenter.y);
                break;
            }

            case DRAW_STATE_zooming: {
                mMapDrawing.scaledDraw(
                        mScaleFactor, mCurrentFocusLocation.x, mCurrentFocusLocation.y);
                break;
            }

            //case DRAW_STATE_none:
            //    break;

            default:
                break;
        }
    }


    @Override
    public void onMapDraw()
    {
        mDrawingState = DRAW_STATE_drawing;
        postInvalidate();

        mDrawTime = System.currentTimeMillis() - mDrawTime;
        Log.d(TAG, "Native map draw time: " + mDrawTime);
    }


    public void resize(
            int width,
            int height)
    {
        mDrawingState = DRAW_STATE_resizing;
        mMapDrawing.setSize(width, height);

        drawMap();
    }


    public void panStart(final MotionEvent e)
    {

        if (mDrawingState == DRAW_STATE_zooming || mDrawingState == DRAW_STATE_panning ||
                mDrawingState == DRAW_STATE_panning_fling) {
            return;
        }

        //Log.d(TAG, "panStart");

        mDrawingState = DRAW_STATE_panning;
        mStartDragLocation.set(e.getX(), e.getY());
        mCurrentDragOffset.set(0, 0);

        mMapDisplayCenter.set(mMapDrawing.getCenter());
    }


    public void panMoveTo(final MotionEvent e)
    {

        if (mDrawingState == DRAW_STATE_zooming || mDrawingState == DRAW_STATE_drawing_noclearbk ||
                mDrawingState == DRAW_STATE_drawing) {
            return;
        }

        if (mDrawingState == DRAW_STATE_panning) {
            float x = mStartDragLocation.x - e.getX();
            float y = mStartDragLocation.y - e.getY();

            //Log.d(TAG, "panMoveTo x - " + x + " y - " + y);

            mCurrentDragOffset.set(x, y);
            invalidate();
        }
    }


    public void panStop()
    {
        //Log.d(Constants.TAG, "panStop state: " + mDrawingState);

        if (mDrawingState == DRAW_STATE_panning) {

            float x = mCurrentDragOffset.x;
            float y = mCurrentDragOffset.y;

            //Log.d(TAG, "panStop x - " + x + " y - " + y);
            //Log.d(TAG, "panStop: draw");

            mMapDisplayCenter.offset(x, y);
            drawMap();
        }
    }


    public void zoomStart(ScaleGestureDetector scaleGestureDetector)
    {
        if (mDrawingState == DRAW_STATE_zooming) {
            return;
        }

        mDrawingState = DRAW_STATE_zooming;
        mCurrentSpan = scaleGestureDetector.getCurrentSpan();
        mCurrentFocusLocation.set(
                scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY());
        mCurrentFocusOffset.set(0, 0);
        mScaleFactor = 1.0;
    }


    public void zoom(ScaleGestureDetector scaleGestureDetector)
    {
        if (mDrawingState != DRAW_STATE_zooming) {
            zoomStart(scaleGestureDetector);
        }

        if (mDrawingState == DRAW_STATE_zooming) {
            mScaleFactor =
                    scaleGestureDetector.getScaleFactor() * scaleGestureDetector.getCurrentSpan()
                            / mCurrentSpan;

            double invertScale = 1 / mScaleFactor;
            double offX = (1 - invertScale) * (mCurrentFocusLocation.x);
            double offY = (1 - invertScale) * (mCurrentFocusLocation.y);
            mCurrentFocusOffset.set((float) offX, (float) offY);

            invalidate();
        }
    }


    public void zoomStop()
    {
        if (mDrawingState == DRAW_STATE_zooming) {
            drawMap();
        }
    }


    @Override
    public boolean onDoubleTap(MotionEvent e)
    {
        if (mMapDrawing.isDrawingEmpty()) {
            return false;
        }

        mDrawingState = DRAW_STATE_zooming;
        mCurrentFocusLocation.set(e.getX(), e.getY());
        mScaleFactor = 2.0;

        double invertScale = 1 / mScaleFactor;
        double offX = (1 - invertScale) * (mCurrentFocusLocation.x);
        double offY = (1 - invertScale) * (mCurrentFocusLocation.y);
        mCurrentFocusOffset.set((float) offX, (float) offY);

        invalidate();
        drawMap();

        return true;
    }


    @Override
    protected void onSizeChanged(
            int w,
            int h,
            int oldw,
            int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        resize(w, h);
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
        resize((right - left), (bottom - top));
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

        //Log.d(
        //        TAG, "onScroll: " + event1.toString() + ", " + event2.toString() + ", " + distanceX
        //                + ", " + distanceY);

        panStart(event1);
        panMoveTo(event2);
        return true;
    }


    @Override
    public void computeScroll()
    {
        super.computeScroll();
        if (mDrawingState == DRAW_STATE_panning_fling) {
            if (mScroller.computeScrollOffset()) {
                if (mScroller.isFinished()) {
                    mDrawingState = DRAW_STATE_panning;
                    panStop();
                } else {
                    float x = mScroller.getCurrX();
                    float y = mScroller.getCurrY();
                    mCurrentDragOffset.set(x, y);
                    postInvalidate();
                }
            } else if (mScroller.isFinished()) {
                mDrawingState = DRAW_STATE_panning;
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
        if (mMapDrawing.isDrawingEmpty()) { //fling not always exec panStop
            return false;
        }

        if (mDrawingState == DRAW_STATE_zooming || mDrawingState == DRAW_STATE_drawing_noclearbk ||
                mDrawingState == DRAW_STATE_drawing) {
            return false;
        }

        mDrawingState = DRAW_STATE_panning_fling;
        float x = mCurrentDragOffset.x;
        float y = mCurrentDragOffset.y;

        mScroller.forceFinished(true);
        mScroller.fling((int) x, (int) y, (int) -velocityX, (int) -velocityY, 0,
                        mMapDrawing.getWidth(), 0, mMapDrawing.getHeight());

        postInvalidate();

        //Log.d(Constants.TAG, "Fling");
        return true;
    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector)
    {
        zoomStart(detector);
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
    public void onScaleEnd(ScaleGestureDetector detector)
    {
        zoomStop();
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
