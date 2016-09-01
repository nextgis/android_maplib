package com.nextgis.store.map;

import android.util.Log;
import com.nextgis.store.bindings.Api;
import com.nextgis.store.bindings.ErrorCodes;
import com.nextgis.store.bindings.ProgressCallback;

import java.io.File;

import static com.nextgis.maplib.util.Constants.TAG;


public class MapStore
{
    protected static final int    DEFAULT_EPSG     = 3857;
    protected static final double DEFAULT_MAX_X    = 20037508.34; // 180.0
    protected static final double DEFAULT_MAX_Y    = 20037508.34; // 90.0
    protected static final double DEFAULT_MIN_X    = -DEFAULT_MAX_X;
    protected static final double DEFAULT_MIN_Y    = -DEFAULT_MAX_Y;
    protected static final String DEFAULT_MAP_NAME = "default";

    protected long mMapId = -1;
    protected String mMapPath;

    protected ProgressCallback mLoadCallback;


    public MapStore(String mapPath)
    {
        mMapPath = mapPath;
        mLoadCallback = createLoadCallback();

        File gdalDir = new File(mMapPath, "gdal_data");
        Api.ngsInit(gdalDir.getPath(), null);
        //Log.d(TAG, "NGS formats: " + Api.ngsGetVersionString("formats"));
    }


    public boolean createMap()
    {
        int mapId = Api.ngsCreateMap(DEFAULT_MAP_NAME, "test gl map", DEFAULT_EPSG, DEFAULT_MIN_X,
                                     DEFAULT_MIN_Y, DEFAULT_MAX_X, DEFAULT_MAX_Y);

        if (mapId != -1) {
            mMapId = mapId;
            return true;
        }
        return false;
    }


    public boolean openMap()
    {
        File mapNativePath = new File(mMapPath, "test-desktop.ngmd");
//        File mapNativePath = new File(getMapPath(), "test-desktop-big.ngmd");
        int mapId = Api.ngsOpenMap(mapNativePath.getPath());
        if (-1 == mapId) {
            Log.d(TAG, "Error: Map load failed");
            return false;
        } else {
            mMapId = mapId;
            Api.ngsSetMapBackgroundColor(mMapId, (short) 0, (short) 255, (short) 0, (short) 255);
            return true;
        }
    }


    public void loadMap()
    {
        File sceneDir = new File(mMapPath, "scenes");
        File sceneFile = new File(sceneDir, "scenes.shp");
        String sceneFilePath = sceneFile.getPath();

        File dbPath = new File(mMapPath, "test-db");
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


    protected ProgressCallback createLoadCallback()
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
        File dbPath = new File(mMapPath, "test-db");
        File dbFile = new File(dbPath, "ngs.gpkg");
        File dbLayer = new File(dbFile, "orbv3");
        String layerPath = dbLayer.getPath();

        if (Api.ngsCreateLayer(mMapId, "orbv3", layerPath) == ErrorCodes.SUCCESS) {
            saveMap();
//            mDrawComplete = 0;
//            Api.ngsDrawMap (mMapId, mDrawCallback);
        }
    }


    public void saveMap()
    {
        File ngmdFile = new File(mMapPath, "test.ngmd");

        if (Api.ngsSaveMap(mMapId, ngmdFile.getPath()) != ErrorCodes.SUCCESS) {
            Log.d(TAG, "Error: Map save failed");
        }
    }
}
