/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2018, 2021 NextGIS, info@nextgis.com
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

package com.nextgis.maplib.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplib.util.ZipResourceFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.nextgis.maplib.util.Constants.DRAWING_SEPARATE_THREADS;
import static com.nextgis.maplib.util.Constants.JSON_BBOX_MAXX_KEY;
import static com.nextgis.maplib.util.Constants.JSON_BBOX_MAXY_KEY;
import static com.nextgis.maplib.util.Constants.JSON_BBOX_MINX_KEY;
import static com.nextgis.maplib.util.Constants.JSON_BBOX_MINY_KEY;
import static com.nextgis.maplib.util.Constants.JSON_EXTENSION_KEY;
import static com.nextgis.maplib.util.Constants.JSON_LEVELS_KEY;
import static com.nextgis.maplib.util.Constants.JSON_LEVEL_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MAXLEVEL_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MINLEVEL_KEY;
import static com.nextgis.maplib.util.Constants.JSON_TOP_DIR_KEY;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOCAL_TMS;


/**
 * The tiles in device folder as image
 */
public class LocalTMSLayer
        extends TMSLayer
{
    protected Map<Integer, TileCacheLevelDescItem> mLimits;
    protected String  mTopDirName;
    protected String  mExtension;
    protected ZipResourceFile  mExpansionFile;


    public LocalTMSLayer(
            Context context,
            File path)
    {
        super(context, path);

        mLayerType = LAYERTYPE_LOCAL_TMS;
    }


    @Override
    public Bitmap getBitmap(TileItem tile)
    {
        Log.d(Constants.TAG, "Raster layer " + getName() + " getBitmap ");
        Bitmap ret = getBitmapFromCache(tile.getHash());
        if (null != ret) {
            if(Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "Raster layer " + getName() + " getBitmap from cache for: " + tile.toString());
            }
            return ret;
        }

        TileCacheLevelDescItem item = mLimits.get(tile.getZoomLevel());
        boolean isInside = item != null && item.isInside(tile.getX(), tile.getY());
        if (isInside) {
            try {
                String tilePath = tile.toString() + "." + mExtension;
                if (!TextUtils.isEmpty(mTopDirName))
                    tilePath = mTopDirName + "/" + tilePath;

                InputStream is = mExpansionFile.getInputStream(tilePath);
                boolean status = is != null && is.available() > 0;
                if (status) {
                    BitmapFactory.Options bfo = new BitmapFactory.Options();
                    bfo.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    return BitmapFactory.decodeStream(is, null, bfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return getBitmapLocal(tile);
        }

        if(Constants.DEBUG_MODE && isInside) {
            Log.d(Constants.TAG, "Raster layer " + getName() + " getBitmap failed for: " + tile.toString());
        }
        return null;
    }

    protected Bitmap getBitmapLocal(TileItem tile) {
        File tilePath = new File(mPath, tile.toString() + TILE_EXT);
        boolean isExist = tilePath.exists();
        if (isExist) {
            Bitmap ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
            putBitmapToCache(tile.getHash(), ret);
            if(Constants.DEBUG_MODE) {
                Log.d(Constants.TAG, "Raster layer " + getName() + " getBitmap for: " + tile.toString() + ", path " + tilePath.getAbsolutePath() + " is valid - " + (ret != null));
            }
            return ret;
        }
        return null;
    }

    @Override
    protected void copyZip(Uri uri) throws IOException, NGException {
        try {
            byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
            InputStream inputStream = getZipStream(uri);
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry ze;
            boolean config = false;
            while ((ze = zis.getNextEntry()) != null) {
                String topDirName = FileUtil.getTopDirNameIfHas(ze.getName());
                if (topDirName != null)
                    mTopDirName = topDirName;
                if (ze.getName().endsWith(".jpg"))
                    mExtension = "jpg";
                if (ze.getName().endsWith(".jpeg"))
                    mExtension = "jpeg";
                if (ze.getName().endsWith(".png"))
                    mExtension = "png";
                if (mExtension != null && config) {
                    zis.closeEntry();
                    break;
                }
                if (ze.isDirectory() || ze.getName().contains("/")) {
                    zis.closeEntry();
                    continue;
                }
                config = true;
                FileUtil.unzipEntry(zis, ze, buffer, mPath);
                zis.closeEntry();
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.copyZip(uri);

        loadZip();
    }

    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();

        if(null != mLimits) {
            JSONArray jsonArray = new JSONArray();
            rootConfig.put(JSON_LEVELS_KEY, jsonArray);
            int nMaxLevel = 0;
            int nMinLevel = 512;
            for (Map.Entry<Integer, TileCacheLevelDescItem> entry : mLimits.entrySet()) {
                int nLevelZ = entry.getKey();
                TileCacheLevelDescItem item = entry.getValue();
                JSONObject oJSONLevel = new JSONObject();
                oJSONLevel.put(JSON_LEVEL_KEY, nLevelZ);
                oJSONLevel.put(JSON_BBOX_MAXX_KEY, item.getMaxX());
                oJSONLevel.put(JSON_BBOX_MAXY_KEY, item.getMaxY());
                oJSONLevel.put(JSON_BBOX_MINX_KEY, item.getMinX());
                oJSONLevel.put(JSON_BBOX_MINY_KEY, item.getMinY());

                jsonArray.put(oJSONLevel);

                if (nMaxLevel < nLevelZ) {
                    nMaxLevel = nLevelZ;
                }
                if (nMinLevel > nLevelZ) {
                    nMinLevel = nLevelZ;
                }
            }

            rootConfig.put(JSON_MAXLEVEL_KEY, nMaxLevel);
            rootConfig.put(JSON_MINLEVEL_KEY, nMinLevel);
        }
        rootConfig.put(JSON_TOP_DIR_KEY, mTopDirName);
        rootConfig.put(JSON_EXTENSION_KEY, mExtension);
        return rootConfig;
    }

    @Override
    public void fillFromNgrc(Uri uri, IProgressor progressor) throws IOException, NumberFormatException, SecurityException, NGException {
        fillFromZipInt(uri, progressor);
        String ext = mExtension;
        String topDir = mTopDirName;
        load();
        mExtension = ext;
        mTopDirName = topDir;
        save();
    }

    protected void loadZip() {
        try {
            File path = new File(mPath, SOURCE_ARCH);
            // Define the zip file as ZipResourceFile
            mExpansionFile = new ZipResourceFile(path.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mTopDirName = jsonObject.optString(JSON_TOP_DIR_KEY);
        mExtension = jsonObject.optString(JSON_EXTENSION_KEY);
        mLimits = new HashMap<>();

        loadZip();

        if(jsonObject.has(JSON_LEVELS_KEY)) {
            final JSONArray jsonArray = jsonObject.getJSONArray(JSON_LEVELS_KEY);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonLevel = jsonArray.getJSONObject(i);
                int nLevel = jsonLevel.getInt(JSON_LEVEL_KEY);
                int nMaxX = jsonLevel.getInt(JSON_BBOX_MAXX_KEY);
                int nMaxY = jsonLevel.getInt(JSON_BBOX_MAXY_KEY);
                int nMinX = jsonLevel.getInt(JSON_BBOX_MINX_KEY);
                int nMinY = jsonLevel.getInt(JSON_BBOX_MINY_KEY);

                mLimits.put(nLevel, new TileCacheLevelDescItem(nMaxX, nMinX, nMaxY, nMinY));

                if(Constants.DEBUG_MODE) {
                    Log.d(Constants.TAG, "Raster layer " + getName() + " limits: zoom " + nLevel + " X[" + nMinX + "," + nMaxX + "] Y[" + nMinY + "," + nMaxY + "]");
                }
            }
        }
    }


    @Override
    public int getMaxThreadCount()
    {
        return DRAWING_SEPARATE_THREADS;
    }


    @Override
    public void fillFromZip(Uri uri, IProgressor progressor) throws IOException, NGException, RuntimeException {
        fillFromZipInt(uri, progressor);

        addLimitsZip(progressor);
//        addLimitsLocal(progressor);

        save();
    }

    private void addLimitsLocal(IProgressor progressor) throws NGException {
        int nMaxLevel = 0;
        int nMinLevel = 512;
        final File[] zoomLevels = mPath.listFiles();

        if (zoomLevels == null)
            throw new NGException("Invalid content or zip structure");

        if(null != progressor){
            progressor.setMax(zoomLevels.length);
            progressor.setValue(0);
            progressor.setMessage(mContext.getString(R.string.message_opening));
        }

        int counter = 0;

        for (File zoomLevel : zoomLevels) {
            if(null != progressor) {
                if(progressor.isCanceled())
                    return;
                progressor.setValue(counter++);
                progressor.setMessage(getContext().getString(R.string.processed) + " " + counter + " " + getContext().getString(R.string.of) + " " + zoomLevels.length);

            }

            if(zoomLevel.getName().equals(Constants.CONFIG) || !zoomLevel.isDirectory()){
                continue;
            }

            int nMaxX = 0;
            int nMinX = 10000000;
            int nMaxY = 0;
            int nMinY = 10000000;

            int nLevelZ = Integer.parseInt(zoomLevel.getName());
            if (nLevelZ > nMaxLevel) {
                nMaxLevel = nLevelZ;
            }
            if (nLevelZ < nMinLevel) {
                nMinLevel = nLevelZ;
            }
            final File[] levelsX = zoomLevel.listFiles();

            boolean bFirstTurn = true;
            for (File inLevelX : levelsX) {

                int nX = Integer.parseInt(inLevelX.getName());
                if (nX > nMaxX) {
                    nMaxX = nX;
                }
                if (nX < nMinX) {
                    nMinX = nX;
                }

                final File[] levelsY = inLevelX.listFiles();

                if (bFirstTurn) {
                    for (File inLevelY : levelsY) {
                        String sLevelY = inLevelY.getName();

                        //Log.d(TAG, sLevelY);
                        int nY = Integer.parseInt(
                                sLevelY.replace(TILE_EXT, ""));
                        if (nY > nMaxY) {
                            nMaxY = nY;
                        }
                        if (nY < nMinY) {
                            nMinY = nY;
                        }
                    }
                    bFirstTurn = false;
                }
            }
            addLimits(nLevelZ, nMaxX, nMaxY, nMinX, nMinY);
        }
    }

    private void putXY(HashMap<String, ArrayList<String>> xy, String x, String y) {
        ArrayList<String> ys = new ArrayList<>();
        ys.add(y);
        xy.put(x, ys);
    }

    private void addLimitsZip(IProgressor progressor) {
        int nMaxLevel = 0;
        int nMinLevel = 512;

        ArrayList<ZipResourceFile.ZipEntryRO> files = mExpansionFile.getAllEntries();
        if(null != progressor){
            progressor.setMax(files.size());
            progressor.setValue(0);
            progressor.setMessage(mContext.getString(R.string.message_opening));
        }

        HashMap<String, HashMap<String, ArrayList<String>>> zoomLevels = new HashMap<>();
        for (ZipResourceFile.ZipEntryRO file : files) {
            String name = file.mFileName;
            if (name.endsWith("/"))
                continue;
            if (!TextUtils.isEmpty(mTopDirName)) {
                name = name.replace(mTopDirName + "/", "");
            }
            String[] parts = name.split("/");
            if (parts.length == 3) {
                String y = parts[2].replace("." + mExtension, "");
                HashMap<String, ArrayList<String>> xy;
                if (zoomLevels.containsKey(parts[0])) {
                    xy = zoomLevels.get(parts[0]);
                    if (xy.containsKey(parts[1])) {
                        xy.get(parts[1]).add(y);
                    } else {
                        putXY(xy, parts[1], y);
                    }
                } else {
                    xy = new HashMap<>();
                    putXY(xy, parts[1], y);
                    zoomLevels.put(parts[0], xy);
                }
            }
        }

        int counter = 0;

        for (Map.Entry<String, HashMap<String, ArrayList<String>>> zoomLevel : zoomLevels.entrySet()) {
            if(null != progressor) {
                if(progressor.isCanceled())
                    return;
                progressor.setValue(counter++);
                progressor.setMessage(getContext().getString(R.string.processed) + " " + counter + " " + getContext().getString(R.string.of) + " " + zoomLevels.size());
            }

            int nMaxX = 0;
            int nMinX = 10000000;
            int nMaxY = 0;
            int nMinY = 10000000;

            int nLevelZ = Integer.parseInt(zoomLevel.getKey());
            if (nLevelZ > nMaxLevel) {
                nMaxLevel = nLevelZ;
            }
            if (nLevelZ < nMinLevel) {
                nMinLevel = nLevelZ;
            }

            boolean bFirstTurn = true;
            for (Map.Entry<String, ArrayList<String>> xy : zoomLevel.getValue().entrySet()) {
                int nX = Integer.parseInt(xy.getKey());
                if (nX > nMaxX) {
                    nMaxX = nX;
                }
                if (nX < nMinX) {
                    nMinX = nX;
                }

                if (bFirstTurn) {
                    for (String y : xy.getValue()) {
                        //Log.d(TAG, y);
                        int nY = Integer.parseInt(y);
                        if (nY > nMaxY) {
                            nMaxY = nY;
                        }
                        if (nY < nMinY) {
                            nMinY = nY;
                        }
                    }
                    bFirstTurn = false;
                }
            }
            addLimits(nLevelZ, nMaxX, nMaxY, nMinX, nMinY);
        }
    }

    public void addLimits(
            int nLevelZ,
            int nMaxX,
            int nMaxY,
            int nMinX,
            int nMinY)
    {
        if (null == mLimits) {
            mLimits = new HashMap<>();
        }
        mLimits.put(nLevelZ, new TileCacheLevelDescItem(nMaxX, nMinX, nMaxY, nMinY));
    }


    protected class TileCacheLevelDescItem
    {
        int minY, maxY;
        int minX, maxX;


        public TileCacheLevelDescItem(
                int nMaxX,
                int nMinX,
                int nMaxY,
                int nMinY)
        {
            this.minX = nMinX;
            this.minY = nMinY;
            this.maxX = nMaxX;
            this.maxY = nMaxY;
        }


        public int getMinX()
        {
            return minX;
        }


        public int getMinY()
        {
            return minY;
        }


        public int getMaxX()
        {
            return maxX;
        }


        public int getMaxY()
        {
            return maxY;
        }


        public boolean isInside(
                int nX,
                int nY)
        {
            return !(nX < minX || nX > maxX) && !(nY < minY || nY > maxY);
        }
    }

    @Override
    public GeoEnvelope getExtents() {
        if(null == mLimits)
            return super.getExtents();
        Integer firstZoom = GeoConstants.DEFAULT_MAX_ZOOM;
        for(Integer key: mLimits.keySet()){
            if(key < firstZoom)
                firstZoom = key;
        }

        TileCacheLevelDescItem item = mLimits.get(firstZoom);

        if(null == item)
            return super.getExtents();

        double mapTileCount = 1 << firstZoom;
        double mapTileSize = GeoConstants.MERCATOR_MAX * 2 / mapTileCount;

        GeoEnvelope env = new GeoEnvelope();
        env.setMinX(item.getMinX() * mapTileSize - GeoConstants.MERCATOR_MAX);
        if(item.getMinX() == item.getMaxX())
            env.setMaxX((item.getMaxX() +1) * mapTileSize - GeoConstants.MERCATOR_MAX);
        else
            env.setMaxX(item.getMaxX() * mapTileSize - GeoConstants.MERCATOR_MAX);

        if(mTMSType == GeoConstants.TMSTYPE_OSM) {
            if(item.getMinY() == item.getMaxY())
                env.setMinY(GeoConstants.MERCATOR_MAX - (item.getMaxY() + 1) * mapTileSize);
            else
                env.setMinY(GeoConstants.MERCATOR_MAX - item.getMaxY() * mapTileSize);
            env.setMaxY(GeoConstants.MERCATOR_MAX - item.getMinY() * mapTileSize);
        } else {
            env.setMinY(item.getMinY() * mapTileSize - GeoConstants.MERCATOR_MAX);
            if(item.getMinY() == item.getMaxY())
                env.setMaxY((item.getMaxY() + 1) * mapTileSize - GeoConstants.MERCATOR_MAX);
            else
                env.setMaxY(item.getMaxY() * mapTileSize - GeoConstants.MERCATOR_MAX);
        }
        return env;
    }

    @Override
    public boolean isValid() {
        return null != mLimits;
    }

    @Override
    public void notifyUpdateAll() {
        load();
    }
}
