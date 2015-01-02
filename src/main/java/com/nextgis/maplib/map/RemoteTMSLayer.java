/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplib.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.Pair;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.NetworkUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;


public class RemoteTMSLayer
        extends TMSLayer
{
    protected static final String JSON_URL_KEY = "url";
    protected String                      mURL;
    protected DefaultHttpClient           mHTTPClient;
    protected NetworkUtil                 mNet;
    protected List<Pair<String, Integer>> mSubdomains;
    protected String subDomainsMask;


    public RemoteTMSLayer(
            Context context,
            File path)
    {
        super(context, path);

        setupHttpClient();

        mNet = new NetworkUtil(context);
    }


    protected void setupHttpClient()
    {
        /*HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION);
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOKET);
        */
        mHTTPClient = new DefaultHttpClient();//httpParameters);
        mHTTPClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, APP_USER_AGENT);
        mHTTPClient.getParams()
                   .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, TIMEOUT_CONNECTION);
        mHTTPClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, TIMEOUT_SOKET);
    }


    @Override
    public Bitmap getBitmap(TileItem tile)
    {
        Bitmap ret;
        // try to get tile from local cache
        File tilePath = new File(mPath, tile.toString("{z}/{x}/{y}" + TILE_EXT));
        if (tilePath.exists() && System.currentTimeMillis() - tilePath.lastModified() <
                                 DEFAULT_MAXIMUM_CACHED_FILE_AGE) {
            ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
            if (ret != null) {
                return ret;
            }
        }

        if (!mNet.isNetworkAvailable()) { //return tile from cache
            ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
            return ret;
        }
        // try to get tile from remote
        Pair<String, String> pair = getURLSubdomain();
        try {

            final HttpUriRequest head = new HttpGet(tile.toString(pair.first));
            final HttpResponse response;

            response = mHTTPClient.execute(head);

            // Check to see if we got success
            final org.apache.http.StatusLine line = response.getStatusLine();
            if (line.getStatusCode() != 200) {
                Log.d(TAG,
                      "Problem downloading MapTile: " + tile.toString(mURL) + " HTTP response: " +
                      line);
                ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
                returnSubdomain(pair.second);
                return ret;
            }

            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                Log.d(TAG, "No content downloading MapTile: " + tile.toString(mURL));
                ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
                returnSubdomain(pair.second);
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
            returnSubdomain(pair.second);
            return ret;

        } catch (IOException e) {
            Log.d(TAG, "Problem downloading MapTile: " + tile.toString(mURL) + " Error: " +
                       e.getLocalizedMessage());
        }

        ret = BitmapFactory.decodeFile(tilePath.getAbsolutePath());
        returnSubdomain(pair.second);
        return ret;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_URL_KEY, mURL);
        return rootConfig;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        mURL = jsonObject.getString(JSON_URL_KEY);

        mSubdomains = new ArrayList<>();
        //analize url for subdomains
        boolean begin_block = false;
        String subdomain = "";
        int beginSubDomains = NOT_FOUND;
        int endSubDomains = NOT_FOUND;
        for(int i = 0; i < mURL.length(); ++i)
        {
            if(begin_block)
            {
                if(mURL.charAt(i) == 'x' || mURL.charAt(i) == 'y' || mURL.charAt(i) == 'z')
                {
                    begin_block = false;
                }
                else if(mURL.charAt(i) == ',')
                {
                    subdomain = subdomain.trim();
                    if(subdomain.length() > 0) {
                        mSubdomains.add(new Pair<String, Integer>(subdomain, HTTP_SEPARATE_THREADS));
                        subdomain = "";
                    }
                }
                else if(mURL.charAt(i) == '}')
                {
                    subdomain = subdomain.trim();
                    if(subdomain.length() > 0) {
                        mSubdomains.add(new Pair<String, Integer>(subdomain, HTTP_SEPARATE_THREADS));
                        subdomain = "";
                    }
                    endSubDomains = i;
                    begin_block = false;
                }
                else
                {
                    subdomain += mURL.charAt(i);
                }
            }

            if(mURL.charAt(i) == '{')
            {
                if(endSubDomains == NOT_FOUND)
                    beginSubDomains = i;
                begin_block = true;
            }
        }

        if(endSubDomains > beginSubDomains)
        {
            subDomainsMask = mURL.substring(beginSubDomains, endSubDomains);
        }
    }


    @Override
    public int getType()
    {
        return LAYERTYPE_REMOTE_TMS;
    }


    public String getURL()
    {
        return mURL;
    }


    public void setURL(String URL)
    {
        mURL = URL;
    }

    public int getMaxThreadCount()
    {
        return mSubdomains.size() * HTTP_SEPARATE_THREADS;
    }

    protected synchronized void returnSubdomain(String subdomain)
    {
        for (int i = 0; i < mSubdomains.size(); ++i)
        {
            Pair<String, Integer> pair = mSubdomains.get(i);
            if(pair.first.equals(subdomain))
            {
                mSubdomains.set(i, new Pair<>(pair.first, pair.second + 1));
                return;
            }
        }
    }

    protected synchronized Pair<String, String> getURLSubdomain()
    {
        if(mSubdomains.size() == 0 || subDomainsMask.length() == 0)
            return new Pair<>(mURL, "");

        for (int i = 0; i < mSubdomains.size(); ++i) {
            Pair<String, Integer> pair = mSubdomains.get(i);
            if (pair.second > 0) {
                String newURL = mURL.replace(subDomainsMask, pair.first);
                mSubdomains.set(i, new Pair<>(pair.first, pair.second - 1));

                return new Pair<>(newURL, pair.first);
            }
        }
        return new Pair<>(mURL, "");
    }
}
