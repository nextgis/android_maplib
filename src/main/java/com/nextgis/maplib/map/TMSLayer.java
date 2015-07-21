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
import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.display.TMSRenderer;
import com.nextgis.maplib.util.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.JSON_RENDERERPROPS_KEY;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;


public abstract class TMSLayer
        extends Layer
{
    protected static final String JSON_TMSTYPE_KEY     = "tms_type";
    protected static final String JSON_CACHE_SIZE_MULT = "cache_size_multiply";

    protected int mTMSType;
    protected static final int HTTP_SEPARATE_THREADS = 2;
    protected Map<String, Bitmap> mBitmapCache;
    protected int                 mCacheSize, mCacheSizeMult;
    protected int mViewWidth, mViewHeight;
    protected final Object lock = new Object();


    protected TMSLayer(
            Context contex,
            File path)
    {
        super(contex, path);

        mCacheSizeMult = 0;
        mRenderer = new TMSRenderer(this);
    }


    public int getTMSType()
    {
        return mTMSType;
    }


    public void setTMSType(int type)
    {
        mTMSType = type;
    }


    public final List<TileItem> getTielsForBounds(
            GeoEnvelope fullBounds,
            GeoEnvelope bounds,
            double zoom)
    {

        int nZoom = (int) zoom;
        int tilesInMap = 1 << nZoom;
        double halfTilesInMap = tilesInMap / 2;
        GeoPoint mapTileSize =
                new GeoPoint(fullBounds.width() / tilesInMap, fullBounds.height() / tilesInMap);

        List<TileItem> list = new ArrayList<>();

        int begX = (int) (bounds.getMinX() / mapTileSize.getX() - .5 + halfTilesInMap);
        int begY = (int) (bounds.getMinY() / mapTileSize.getY() - .5 + halfTilesInMap);
        int endX = (int) (bounds.getMaxX() / mapTileSize.getX() + halfTilesInMap) + 1;
        int endY = (int) (bounds.getMaxY() / mapTileSize.getY() + halfTilesInMap) + 1;

        if (begY < 0) {
            begY = 0;
        }
        if (endY > tilesInMap) {
            endY = tilesInMap;
        }

        //fill tiles from center snake order
/*
        checkDuplicates.clear();
        int centerX = begX + (endX - begX) / 2;
        int centerY = begY + (endY - begY) / 2;
        int center = Math.max(centerX, centerY);

        //add center point
        addItemToList(fullBounds, mapTileSize, centerX, centerY, nZoom, tilesInMap, list);

        for (int k = 1; k < center + 2; k++) {
            //1. top and bottom
            int tileBottom = centerY - k;
            if(tileBottom < begY)
                tileBottom = begY;
            int tileTop = centerY + k;
            if(tileTop > endY)
                tileTop = endY;
            for (int i = centerX - k; i < centerX + k + 1; i++) {
                if(i < begX || i > endX)
                    continue;
                addItemToList(fullBounds, mapTileSize, i, tileTop, nZoom, tilesInMap, list);
                addItemToList(fullBounds, mapTileSize, i, tileBottom, nZoom, tilesInMap, list);
            }

            //2. left and right
            int tileLeft = centerX - k;
            if(tileLeft < begX)
                tileLeft = begX;
            int tileRight = centerX + k;
            if(tileRight > endX)
                tileRight = endX;
            for (int j = centerY - k; j < centerY + k + 1; j++) {
                if(j < begY || j > endY)
                    continue;
                addItemToList(fullBounds, mapTileSize, tileLeft, j, nZoom, tilesInMap, list);
                addItemToList(fullBounds, mapTileSize, tileRight, j, nZoom, tilesInMap, list);
            }
        }
*/
        /*/ fill by 9 segments center first
        int xParts = (endX - begX) / 3;
        int yParts = (endY - begY) / 3;

        /*
                +---+---+---+
                | 1 | 2 | 3 |
                +---+---+---+
                | 4 | 5 | 6 |
                +---+---+---+
                | 7 | 8 | 9 |
                +---+---+---+
         */
/*
        //5
        for(int x = begX + xParts; x < endX - xParts; x++){
            for(int y = begY + yParts; y < endY - yParts; y++){

                addItemToList(fullBounds, mapTileSize, x, y, nZoom, tilesInMap, list);
            }
        }

        //6
        for(int x = endX - xParts; x < endX; x++){
            for(int y = begY + yParts; y < endY - yParts; y++){

                addItemToList(fullBounds, mapTileSize, x, y, nZoom, tilesInMap, list);
            }
        }

        //4
        for(int x = begX; x < begX + xParts; x++){
            for(int y = begY + yParts; y < endY - yParts; y++){

                addItemToList(fullBounds, mapTileSize, x, y, nZoom, tilesInMap, list);
            }
        }

        //2
        for(int x = begX + xParts; x < endX - xParts; x++){
            for(int y = begY; y < begY + yParts; y++){

                addItemToList(fullBounds, mapTileSize, x, y, nZoom, tilesInMap, list);
            }
        }

        //8
        for(int x = begX + xParts; x < endX - xParts; x++){
            for(int y = endY - yParts; y < endY; y++){

                addItemToList(fullBounds, mapTileSize, x, y, nZoom, tilesInMap, list);
            }
        }

        //1
        for(int x = begX; x < begX + xParts; x++){
            for(int y = begY; y < begY + yParts; y++){

                addItemToList(fullBounds, mapTileSize, x, y, nZoom, tilesInMap, list);
            }
        }

        //3
        for(int x = endX - xParts; x < endX; x++){
            for(int y = begY; y < begY + yParts; y++){

                addItemToList(fullBounds, mapTileSize, x, y, nZoom, tilesInMap, list);
            }
        }

        //9
        for(int x = endX - xParts; x < endX; x++){
            for(int y = endY - yParts; y < endY; y++){

                addItemToList(fullBounds, mapTileSize, x, y, nZoom, tilesInMap, list);
            }
        }

        //7
        for(int x = begX; x < begX + xParts; x++){
            for(int y = endY - yParts; y < endY; y++){

                addItemToList(fullBounds, mapTileSize, x, y, nZoom, tilesInMap, list);
            }
        }*/


        // normal fill from left bottom corner
        for (int x = begX; x < endX; x++) {
            for (int y = begY; y < endY; y++) {

                addItemToList(fullBounds, mapTileSize, x, y, nZoom, tilesInMap, list);
            }
        }

        return list;
    }


    protected void addItemToList(
            final GeoEnvelope fullBounds,
            final GeoPoint mapTileSize,
            int x,
            int y,
            int zoom,
            int tilesInMap,
            List<TileItem> list)
    {
        int realY;
        int realX;

        realX = x;
        if (realX < 0) {
            realX += tilesInMap;
        } else if (realX >= tilesInMap) {
            realX -= tilesInMap;
        }

        realY = y;
        if (mTMSType == TMSTYPE_OSM) {
            realY = tilesInMap - y - 1;
        }

        if (realY < 0 || realY >= tilesInMap) {
            return;
        }

        final GeoPoint pt = new GeoPoint(
                fullBounds.getMinX() + x * mapTileSize.getX(),
                fullBounds.getMinY() + (y + 1) * mapTileSize.getY());
        TileItem item = new TileItem(realX, realY, zoom, pt);
        list.add(item);

        /*int realX = x;
        if (realX < 0) {
            //while(realX < 0)
                realX += tilesInMap;
        } else if (realX >= tilesInMap) {
            //while(realX >= tilesInMap)
                realX -= tilesInMap;
        }

        final GeoPoint pt = new GeoPoint(fullBounds.getMinX() + x * mapTileSize.getX(),
                                         fullBounds.getMinY() + (y + 1) * mapTileSize.getY());
        int realY = y;
        if (mTMSType == TMSTYPE_OSM) {
            realY = tilesInMap - y - 1;
        }

        if (realY < 0 || realY >= tilesInMap) {
            return;
        }

        if(checkDuplicates.contains(realX + "." + realY))// + "." + zoom
            return;

        checkDuplicates.add(realX + "." + realY);// + "." + zoom
        TileItem item = new TileItem(realX, realY, zoom, pt);
        list.add(item);*/
    }


    public abstract Bitmap getBitmap(TileItem tile);


    protected void putBitmapToCache(
            String tileHash,
            Bitmap bitmap)
    {

        synchronized (lock) {
            if (mBitmapCache != null) {
                mBitmapCache.put(tileHash, bitmap);
            }
        }
    }


    protected Bitmap getBitmapFromCache(String tileHash)
    {
        synchronized (lock) {
            if (mBitmapCache != null) {
                return mBitmapCache.get(tileHash);
            }
        }
        return null;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_TMSTYPE_KEY, mTMSType);
        if (mRenderer instanceof IJSONStore) {
            IJSONStore jsonStore = (IJSONStore) mRenderer;
            rootConfig.put(JSON_RENDERERPROPS_KEY, jsonStore.toJSON());
        }


        rootConfig.put(JSON_CACHE_SIZE_MULT, mCacheSizeMult);
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mTMSType = jsonObject.getInt(JSON_TMSTYPE_KEY);
        if (jsonObject.has(JSON_RENDERERPROPS_KEY)) {
            if (mRenderer instanceof IJSONStore) {
                IJSONStore jsonStore = (IJSONStore) mRenderer;
                jsonStore.fromJSON(jsonObject.getJSONObject(JSON_RENDERERPROPS_KEY));
            }
        }

        if (jsonObject.has(JSON_CACHE_SIZE_MULT)) {
            mCacheSizeMult = jsonObject.getInt(JSON_CACHE_SIZE_MULT);
        }
    }


    public int getMaxThreadCount()
    {
        return HTTP_SEPARATE_THREADS;
    }


    @Override
    public void setViewSize(
            int w,
            int h)
    {
        super.setViewSize(w, h);

        mViewWidth = w;
        mViewHeight = h;

        setCacheSizeMultiply(mCacheSizeMult);
    }


    public int getCacheSizeMultiply()
    {
        return mCacheSizeMult;
    }


    public void setCacheSizeMultiply(int cacheSizeMult)
    {
        mCacheSizeMult = cacheSizeMult;
        if (mCacheSizeMult == 0) {
            synchronized (lock) {
                mBitmapCache = null;
            }
            return;
        }

        // calc new hash size
        int nTileCount = (int) (mViewWidth * Constants.OFFSCREEN_EXTRASIZE_RATIO /
                                Constants.DEFAULT_TILE_SIZE) *
                         (int) (mViewHeight * Constants.OFFSCREEN_EXTRASIZE_RATIO /
                                Constants.DEFAULT_TILE_SIZE) * mCacheSizeMult;

        if (null != mBitmapCache && mCacheSize >= nTileCount) {
            return;
        }
        if (nTileCount < 30) {
            nTileCount = 30;
        }

        synchronized (lock) {
            mBitmapCache = lruCache(nTileCount);
        }

        mCacheSize = nTileCount;
    }


    protected static <K, V> Map<K, V> lruCache(final int maxSize)
    {
        return new LinkedHashMap<K, V>(maxSize * 4 / 3, 0.75f, true)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
            {
                return size() > maxSize;
            }
        };
    }
}