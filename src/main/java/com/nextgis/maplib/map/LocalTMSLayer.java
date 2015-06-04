/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

package com.nextgis.maplib.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.nextgis.maplib.datasource.TileItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.*;


/**
 * The tiles in device folder as image
 */
public class LocalTMSLayer
        extends TMSLayer
{
    protected Map<Integer, TileCacheLevelDescItem> mLimits;


    protected LocalTMSLayer(
            Context contex,
            File path)
    {
        super(contex, path);

        mLayerType = LAYERTYPE_LOCAL_TMS;
    }


    @Override
    public Bitmap getBitmap(TileItem tile)
    {
        Bitmap ret = getBitmapFromCache(tile.getHash());
        if (null != ret) {
            return ret;
        }

        TileCacheLevelDescItem item = mLimits.get(tile.getZoomLevel());
        if (item != null && item.isInside(tile.getX(), tile.getY())) {
            File tilePath = new File(mPath, tile.toString("{z}/{x}/{y}" + TILE_EXT));
            if (tilePath.exists()) {
                ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
                putBitmapToCache(tile.getHash(), ret);
                return ret;
            }
        }
        return null;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
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

        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mLimits = new HashMap<>();
        final JSONArray jsonArray = jsonObject.getJSONArray(JSON_LEVELS_KEY);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonLevel = jsonArray.getJSONObject(i);
            int nLevel = jsonLevel.getInt(JSON_LEVEL_KEY);
            int nMaxX = jsonLevel.getInt(JSON_BBOX_MAXX_KEY);
            int nMaxY = jsonLevel.getInt(JSON_BBOX_MAXY_KEY);
            int nMinX = jsonLevel.getInt(JSON_BBOX_MINX_KEY);
            int nMinY = jsonLevel.getInt(JSON_BBOX_MINY_KEY);

            mLimits.put(nLevel, new TileCacheLevelDescItem(nMaxX, nMinX, nMaxY, nMinY));
        }
    }


    @Override
    public int getMaxThreadCount()
    {
        return DRAWING_SEPARATE_THREADS;
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
}
