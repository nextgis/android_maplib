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
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.nextgis.maplib.util.Constants.DEFAULT_TILE_MAX_AGE;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_REMOTE_TMS;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.MERCATOR_MAX;


public class RemoteTMSLayer
        extends TMSLayer
{
    protected static final String JSON_URL_KEY      = "url";
    protected static final String JSON_LOGIN_KEY    = "login";
    protected static final String JSON_PASSWORD_KEY = "password";
    protected static final String JSON_TILE_AGE_KEY = "tile_age";

    protected       String       mURL;
    protected       NetworkUtil  mNet;
    protected final List<String> mSubdomains;
    protected       String       mSubDomainsMask;
    protected       int          mCurrentSubdomain;
    protected       String       mLogin;
    protected       String       mPassword;
    protected       Semaphore    mAvailable;
    protected long mTileMaxAge;

    public final static long DELAY = 2150;


    public RemoteTMSLayer(
            Context context,
            File path)
    {
        super(context, path);

        mNet = new NetworkUtil(context);
        mSubdomains = new ArrayList<>();
        mCurrentSubdomain = 0;
        mLayerType = LAYERTYPE_REMOTE_TMS;
        mTileMaxAge = DEFAULT_TILE_MAX_AGE;
        setViewSize(100, 100);
    }


    public synchronized void onPrepare()
    {
        int diff = getMaxThreadCount() - mAvailable.availablePermits();
        if (diff > 0) {
            mAvailable.release(diff);
        }
        Log.d(
                TAG, "Semaphore left: " + mAvailable.availablePermits() + " max thread: " +
                        getMaxThreadCount());
    }

    public void downloadTile(TileItem tile){
        if (null == tile) {
            return;
        }

        // try to get tile from local cache
        File tilePath = new File(mPath, tile.toString("{z}/{x}/{y}" + TILE_EXT));
        boolean exist = tilePath.exists();
        if (exist && System.currentTimeMillis() - tilePath.lastModified() < mTileMaxAge) {
            return;
        }

        if (!mNet.isNetworkAvailable()) {
            return;
        }

        // try to get tile from remote
        String url = tile.toString(getURLSubdomain());
        Log.d(TAG, "url: " + url);
        try {

            if (!mAvailable.tryAcquire(DELAY, TimeUnit.MILLISECONDS)) {
                return;
            }

            FileUtil.createDir(tilePath.getParentFile());
            OutputStream output = new FileOutputStream(tilePath.getAbsolutePath());
            NetworkUtil.getStream(url, getLogin(), getPassword(), output);

            mAvailable.release();

        } catch (InterruptedException | IOException | IllegalArgumentException e) {
            e.printStackTrace();
            Log.d(
                    TAG, "Problem downloading MapTile: " + url + " Error: " +
                            e.getLocalizedMessage());
        }
    }

    @Override
    public Bitmap getBitmap(final TileItem tile)
    {
        if (null == tile) {
            return null;
        }

        Bitmap ret = getBitmapFromCache(tile.getHash());
        if (null != ret) {
            return ret;
        }

        // try to get tile from local cache
        File tilePath = new File(mPath, tile.toString("{z}/{x}/{y}" + TILE_EXT));
        boolean exist = tilePath.exists();
        //Log.d(TAG, "time diff: " + (System.currentTimeMillis() - tilePath.lastModified()) + " age: " + DEFAULT_TILE_MAX_AGE);
        if (exist) {
            ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
            if (ret != null) {
                putBitmapToCache(tile.getHash(), ret);
                if(System.currentTimeMillis() - tilePath.lastModified() > mTileMaxAge) {
                    Log.d(Constants.TAG, "Update old tile " + tile.toString());
                    // update tile
                    new Thread(new Runnable() {
                        public void run() {
                            downloadTile(tile);
                        }
                    }).start();
                }
                return ret;
            }
        }

        if (!mNet.isNetworkAvailable()) { //return tile from cache
            return null;
        }

        // try to get tile from remote
        String url = tile.toString(getURLSubdomain());
        Log.d(TAG, "url: " + url);
        try {

            if (!mAvailable.tryAcquire(DELAY, TimeUnit.MILLISECONDS)) { //.acquire();
                if (exist) //if exist but not reload from internet
                {
                    ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
                }

                putBitmapToCache(tile.getHash(), ret);
                return ret;
            }
            Log.d(TAG, "Semaphore left: " + mAvailable.availablePermits());

            FileUtil.createDir(tilePath.getParentFile());
            OutputStream output = new FileOutputStream(tilePath.getAbsolutePath());
            NetworkUtil.getStream(url, getLogin(), getPassword(), output);

            mAvailable.release();

            ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
            putBitmapToCache(tile.getHash(), ret);
            return ret;

        } catch (InterruptedException | IOException | IllegalArgumentException e) {
            e.printStackTrace();
            Log.d(
                    TAG, "Problem downloading MapTile: " + url + " Error: " +
                         e.getLocalizedMessage());
        }

        if (exist) //if exist but not reload from internet
        {
            ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
            putBitmapToCache(tile.getHash(), ret);
            return ret;
        }
        return null;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_URL_KEY, mURL);
        // Here we must use mLogin instead of getLogin() for subclasses
        // which do not use mLogin
        if (!TextUtils.isEmpty(mLogin)) {
            rootConfig.put(JSON_LOGIN_KEY, mLogin);
        }
        // Here we must use mPassword instead of getPassword() for subclasses
        // which do not use mLogin
        if (!TextUtils.isEmpty(mPassword)) {
            rootConfig.put(JSON_PASSWORD_KEY, mPassword);
        }

        rootConfig.put(JSON_TILE_AGE_KEY, mTileMaxAge);

        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mURL = jsonObject.getString(JSON_URL_KEY);
        if (jsonObject.has(JSON_LOGIN_KEY)) {
            // Here we must use mLogin instead of setLogin() for subclasses
            // which do not use mLogin
            mLogin = jsonObject.getString(JSON_LOGIN_KEY);
        }
        if (jsonObject.has(JSON_PASSWORD_KEY)) {
            // Here we must use mPassword instead of setPassword() for subclasses
            // which do not use mLogin
            mPassword = jsonObject.getString(JSON_PASSWORD_KEY);
        }

        if(jsonObject.has(JSON_TILE_AGE_KEY)) {
            mTileMaxAge = jsonObject.getLong(JSON_TILE_AGE_KEY);
        }

        analizeURL(mURL);
    }


    public String getURL()
    {
        return mURL;
    }


    public void setURL(String URL)
    {
        mURL = URL;
        analizeURL(mURL);
    }


    protected synchronized String getURLSubdomain()
    {
        if (mSubdomains.size() == 0 || mSubDomainsMask.length() == 0) {
            return mURL;
        }

        if (mCurrentSubdomain >= mSubdomains.size()) {
            mCurrentSubdomain = 0;
        }

        String subdomain = mSubdomains.get(mCurrentSubdomain++);
        return mURL.replace(mSubDomainsMask, subdomain);
    }


    protected void analizeURL(String url)
    {
        //analize url for subdomains
        boolean begin_block = false;
        String subdomain = "";
        int beginSubDomains = NOT_FOUND;
        int endSubDomains = NOT_FOUND;
        for (int i = 0; i < url.length(); ++i) {
            if (begin_block) {
                if (url.charAt(i) == 'x' || url.charAt(i) == 'y' || url.charAt(i) == 'z') {
                    begin_block = false;
                } else if (url.charAt(i) == ',') {
                    subdomain = subdomain.trim();
                    if (subdomain.length() > 0) {
                        mSubdomains.add(subdomain);
                        subdomain = "";
                    }
                } else if (url.charAt(i) == '}') {
                    subdomain = subdomain.trim();
                    if (subdomain.length() > 0) {
                        mSubdomains.add(subdomain);
                        subdomain = "";
                    }
                    endSubDomains = i;
                    begin_block = false;
                } else {
                    subdomain += url.charAt(i);
                }
            }

            if (url.charAt(i) == '{') {
                if (endSubDomains == NOT_FOUND) {
                    beginSubDomains = i;
                }
                begin_block = true;
            }
        }

        if (endSubDomains > beginSubDomains) {
            mSubDomainsMask = url.substring(beginSubDomains, endSubDomains + 1);
        }

        mAvailable = new Semaphore(getMaxThreadCount(), true);
    }


    @Override
    public GeoEnvelope getExtents()
    {
        if (mExtents == null) {
            mExtents = new GeoEnvelope(-MERCATOR_MAX, MERCATOR_MAX, -MERCATOR_MAX, MERCATOR_MAX);
        }
        return mExtents;
    }


    @Override
    public int getMaxThreadCount()
    {
        if (mSubdomains.isEmpty()) {
            return HTTP_SEPARATE_THREADS;
        }
        return mSubdomains.size() * HTTP_SEPARATE_THREADS;
    }


    public String getLogin()
    {
        return mLogin;
    }


    public void setLogin(String login)
    {
        mLogin = login;
    }


    public String getPassword()
    {
        return mPassword;
    }


    public void setPassword(String password)
    {
        mPassword = password;
    }

    public long getTileMaxAge() {
        return mTileMaxAge;
    }

    public void setTileMaxAge(long tileMaxAge) {
        mTileMaxAge = tileMaxAge;
    }
}
