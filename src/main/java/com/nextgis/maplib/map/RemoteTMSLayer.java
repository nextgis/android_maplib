/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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
import android.util.Base64;
import android.util.Log;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.NetworkUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.MERCATOR_MAX;


public class RemoteTMSLayer
        extends TMSLayer
{
    protected static final String JSON_URL_KEY      = "url";
    protected static final String JSON_LOGIN_KEY    = "login";
    protected static final String JSON_PASSWORD_KEY = "password";

    protected       String       mURL;
    protected       NetworkUtil  mNet;
    protected final List<String> mSubdomains;
    protected       String       mSubDomainsMask;
    protected       int          mCurrentSubdomain;
    protected       String       mLogin;
    protected       String       mPassword;
    protected       Semaphore    mAvailable;

    protected       Map<String, Bitmap> mBitmapCache;
    protected       int          mCacheSize;

    public final static long DELAY = 2150;
    protected final Object lock = new Object();


    public RemoteTMSLayer(
            Context context,
            File path)
    {
        super(context, path);

        mNet = new NetworkUtil(context);
        mSubdomains = new ArrayList<>();
        mCurrentSubdomain = 0;
        mLayerType = LAYERTYPE_REMOTE_TMS;

        setViewSize(100, 100);
    }


    public synchronized void onPrepare()
    {
        int diff = getMaxThreadCount() - mAvailable.availablePermits();
        if (diff > 0)
            mAvailable.release(diff);
        Log.d(TAG, "Semaphore left: " + mAvailable.availablePermits() + " max thread: " +
                   getMaxThreadCount());
    }

    protected void putBitmapToCache(
            String tileHash,
            Bitmap bitmap){

        synchronized (lock) {
            if(mBitmapCache != null) {
                mBitmapCache.put(tileHash, bitmap);
            }
        }
    }

    protected Bitmap getBitmapFromCache(String tileHash){
        synchronized (lock) {
            if(mBitmapCache != null) {
                return mBitmapCache.get(tileHash);
            }
        }
        return null;
    }

    @Override
    public Bitmap getBitmap(TileItem tile)
    {
        if(null == tile)
            return null;

        Bitmap ret = getBitmapFromCache(tile.getHash());
        if(null != ret)
            return ret;

        // try to get tile from local cache
        File tilePath = new File(mPath, tile.toString("{z}/{x}/{y}" + TILE_EXT));
        boolean exist = tilePath.exists();
        //Log.d(TAG, "time diff: " + (System.currentTimeMillis() - tilePath.lastModified()) + " age: " + DEFAULT_MAXIMUM_CACHED_FILE_AGE);
        if (exist && System.currentTimeMillis() - tilePath.lastModified() <
                     DEFAULT_MAXIMUM_CACHED_FILE_AGE) {
            ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
            if (ret != null) {
                putBitmapToCache(tile.getHash(), ret);
                return ret;
            }
        }

        if (!mNet.isNetworkAvailable()) { //return tile from cache
            if (exist) {
                ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
                putBitmapToCache(tile.getHash(), ret);
                return ret;
            } else {
                return null;
            }
        }

        // try to get tile from remote
        String url = tile.toString(getURLSubdomain());
        Log.d(TAG, "url: " + url);
        try {

            final HttpGet get = new HttpGet(url);

            //basic auth
            if (null != mLogin && mLogin.length() > 0 && null != mPassword &&
                mPassword.length() > 0) {
                get.setHeader("Accept", "*/*");
                final String basicAuth = "Basic " + Base64.encodeToString(
                        (mLogin + ":" + mPassword).getBytes(), Base64.NO_WRAP);
                get.setHeader("Authorization", basicAuth);
            }

            final DefaultHttpClient HTTPClient = mNet.getHttpClient();
            mNet.setProxy(HTTPClient, url);
            if(!mAvailable.tryAcquire(DELAY, TimeUnit.MILLISECONDS)) { //.acquire();
                if(exist) //if exist but not reload from internet
                    ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());

                putBitmapToCache(tile.getHash(), ret);
                return ret;
            }
            Log.d(TAG, "Semaphore left: " + mAvailable.availablePermits());
            final HttpResponse response = HTTPClient.execute(get);
            mAvailable.release();

            // Check to see if we got success
            final org.apache.http.StatusLine line = response.getStatusLine();
            if (line.getStatusCode() != 200) {
                Log.d(TAG,
                      "Problem downloading MapTile: " + url + " HTTP response: " +
                      line);
                ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
                putBitmapToCache(tile.getHash(), ret);
                return ret;
            }

            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                Log.d(TAG, "No content downloading MapTile: " + url);
                ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
                putBitmapToCache(tile.getHash(), ret);
                return ret;
            }

            FileUtil.createDir(tilePath.getParentFile());

            InputStream input = entity.getContent();
            OutputStream output = new FileOutputStream(tilePath.getAbsolutePath());
            byte data[] = new byte[IO_BUFFER_SIZE];

            FileUtil.copyStream(input, output, data, IO_BUFFER_SIZE);

            output.close();
            input.close();
            ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
            putBitmapToCache(tile.getHash(), ret);
            return ret;

        } catch (InterruptedException | IOException | IllegalArgumentException e) {
            e.printStackTrace();
            Log.d(TAG, "Problem downloading MapTile: " + url + " Error: " +
                       e.getLocalizedMessage());
        }

        if(exist) //if exist but not reload from internet
            ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
        putBitmapToCache(tile.getHash(), ret);
        return ret;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_URL_KEY, mURL);
        rootConfig.put(JSON_LOGIN_KEY, mLogin);
        rootConfig.put(JSON_PASSWORD_KEY, mPassword);
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mURL = jsonObject.getString(JSON_URL_KEY);
        if(jsonObject.has(JSON_LOGIN_KEY))
            mLogin = jsonObject.getString(JSON_LOGIN_KEY);
        if(jsonObject.has(JSON_PASSWORD_KEY))
            mPassword = jsonObject.getString(JSON_PASSWORD_KEY);

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
        if(mSubdomains.size() == 0 || mSubDomainsMask.length() == 0)
            return mURL;

        if(mCurrentSubdomain >= mSubdomains.size())
            mCurrentSubdomain = 0;

        String subdomain = mSubdomains.get(mCurrentSubdomain++);
        return mURL.replace(mSubDomainsMask, subdomain);
    }

    protected void analizeURL(String url){
        //analize url for subdomains
        boolean begin_block = false;
        String subdomain = "";
        int beginSubDomains = NOT_FOUND;
        int endSubDomains = NOT_FOUND;
        for(int i = 0; i < url.length(); ++i)
        {
            if(begin_block)
            {
                if(url.charAt(i) == 'x' || url.charAt(i) == 'y' || url.charAt(i) == 'z')
                {
                    begin_block = false;
                }
                else if(url.charAt(i) == ',')
                {
                    subdomain = subdomain.trim();
                    if(subdomain.length() > 0) {
                        mSubdomains.add(subdomain);
                        subdomain = "";
                    }
                }
                else if(url.charAt(i) == '}')
                {
                    subdomain = subdomain.trim();
                    if(subdomain.length() > 0) {
                        mSubdomains.add(subdomain);
                        subdomain = "";
                    }
                    endSubDomains = i;
                    begin_block = false;
                }
                else
                {
                    subdomain += url.charAt(i);
                }
            }

            if(url.charAt(i) == '{')
            {
                if(endSubDomains == NOT_FOUND)
                    beginSubDomains = i;
                begin_block = true;
            }
        }

        if(endSubDomains > beginSubDomains)
        {
            mSubDomainsMask = url.substring(beginSubDomains, endSubDomains + 1);
        }

        mAvailable = new Semaphore(getMaxThreadCount(), true);
    }

    @Override
    public GeoEnvelope getExtents()
    {
        if(mExtents == null)
            mExtents = new GeoEnvelope(-MERCATOR_MAX, MERCATOR_MAX, -MERCATOR_MAX, MERCATOR_MAX);
        return mExtents;
    }


    @Override
    public int getMaxThreadCount()
    {
        if(mSubdomains.isEmpty())
            return HTTP_SEPARATE_THREADS;
        return mSubdomains.size() * HTTP_SEPARATE_THREADS;
    }


    public void setLogin(String login)
    {
        mLogin = login;
    }


    public void setPassword(String password)
    {
        mPassword = password;
    }

    protected static <K,V> Map<K,V> lruCache(final int maxSize)
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


    @Override
    public void setViewSize(
            int w,
            int h)
    {
        super.setViewSize(w, h);

        // calc new hash size
        int nTileCount = (int) (w * Constants.OFFSCREEN_EXTRASIZE_RATIO / Constants.DEFAULT_TILE_SIZE) *
                         (int) (h * Constants.OFFSCREEN_EXTRASIZE_RATIO / Constants.DEFAULT_TILE_SIZE) * 2;

        if(null != mBitmapCache && mCacheSize >= nTileCount)
            return;
        if(nTileCount < 30)
            nTileCount = 30;

        synchronized (lock) {
            mBitmapCache = lruCache(nTileCount);
        }

        mCacheSize = nTileCount;
    }
}
