/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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
import android.util.Log;

import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.NGException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.DRAWING_SEPARATE_THREADS;
import static com.nextgis.maplib.util.Constants.JSON_BBOX_MAXX_KEY;
import static com.nextgis.maplib.util.Constants.JSON_BBOX_MAXY_KEY;
import static com.nextgis.maplib.util.Constants.JSON_BBOX_MINX_KEY;
import static com.nextgis.maplib.util.Constants.JSON_BBOX_MINY_KEY;
import static com.nextgis.maplib.util.Constants.JSON_LEVELS_KEY;
import static com.nextgis.maplib.util.Constants.JSON_LEVEL_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MAXLEVEL_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MINLEVEL_KEY;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOCAL_TMS;


/**
 * The tiles in device folder as image
 */
public class LocalTMSLayer
        extends TMSLayer
{
    protected Map<Integer, TileCacheLevelDescItem> mLimits;


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
            File tilePath = new File(mPath, tile.toString() + TILE_EXT);
            boolean isExist = tilePath.exists();
            if (isExist) {
                ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
                putBitmapToCache(tile.getHash(), ret);
                if(Constants.DEBUG_MODE) {
                    Log.d(Constants.TAG, "Raster layer " + getName() + " getBitmap for: " + tile.toString() + ", path " + tilePath.getAbsolutePath() + " is valid - " + (ret != null));
                }
                return ret;
            }
        }

        if(Constants.DEBUG_MODE && isInside) {
            Log.d(Constants.TAG, "Raster layer " + getName() + " getBitmap failed for: " + tile.toString());
        }
        return null;
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
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mLimits = new HashMap<>();

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

        save();
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
