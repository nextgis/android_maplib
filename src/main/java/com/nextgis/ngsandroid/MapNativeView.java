package com.nextgis.ngsandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.store.Api;
import com.nextgis.store.ErrorCodes;
import com.nextgis.store.ProgressCallback;

import java.io.File;
import java.nio.ByteBuffer;

import static com.nextgis.maplib.util.Constants.TAG;


public class MapNativeView
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

    protected ProgressCallback mDrawCallback;
    protected ProgressCallback mLoadCallback;

    protected long mMapId = -1;

    protected double mDrawComplete = 0;

    protected long mTime;


    public MapNativeView(Context context)
    {
        super(context);

        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        mBuffer = ByteBuffer.allocateDirect(metrics.widthPixels * metrics.heightPixels * 4);

        mDrawCallback = new ProgressCallback()
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

        mLoadCallback = new ProgressCallback()
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

        Api.ngsInit(null, null);
        Log.d(TAG, "NGS formats: " + Api.ngsGetVersionString("formats"));
        newMap();
//        loadMap();
//        openMap();
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
        int mapId = Api.ngsCreateMap(DEFAULT_MAP_NAME, "test gl map", DEFAULT_EPSG, DEFAULT_MIN_X,
                                     DEFAULT_MIN_Y, DEFAULT_MAX_X, DEFAULT_MAX_Y);

        if (mapId != -1) {
            mMapId = mapId;
            Api.ngsSetMapBackgroundColor(mMapId, (short) 0, (short) 255, (short) 0, (short) 255);
        }
    }


    protected void setMapSize(
            final int width,
            final int height)
    {
        mTime = System.currentTimeMillis();

        mWidth = width;
        mHeight = height;

        Api.ngsInitMap(mMapId, mBuffer, width, height, 1);

        mDrawComplete = 0;
        Api.ngsDrawMap(mMapId, mDrawCallback);
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
        File mapNativePath = new File(getMapPath(), "test-db-mini.ngmd");

        int mapId = Api.ngsOpenMap(mapNativePath.getPath());
        if (-1 == mapId) {
            Log.d(TAG, "Error: Map load failed");
        } else {
            mMapId = mapId;
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
}
