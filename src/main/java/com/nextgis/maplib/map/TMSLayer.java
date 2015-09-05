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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.JSON_RENDERERPROPS_KEY;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;


public abstract class TMSLayer
        extends Layer
{
    protected static final String JSON_TMSTYPE_KEY     = "tms_type";
    protected static final String JSON_CACHE_SIZE_MULT = "cache_size_multiply";
    protected String TILE_EXT     = ".tile";

    protected int mTMSType;
    protected static final int HTTP_SEPARATE_THREADS = 2;
    protected Map<String, Bitmap> mBitmapCache;
    protected int                 mCacheSize, mCacheSizeMult;
    protected int mViewWidth, mViewHeight;
    protected final Object lock = new Object();


    protected TMSLayer(
            Context context,
            File path)
    {
        super(context, path);

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

    public abstract Bitmap getBitmap(TileItem tile);


    protected void putBitmapToCache(
            String tileHash,
            Bitmap bitmap)
    {
        if (mCacheSizeMult == 0) {
            return;
        }
        synchronized (lock) {
            if (mBitmapCache != null) {
                mBitmapCache.put(tileHash, bitmap);
            }
        }
    }


    protected Bitmap getBitmapFromCache(String tileHash)
    {
        if (mCacheSizeMult == 0) {
            return null;
        }
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