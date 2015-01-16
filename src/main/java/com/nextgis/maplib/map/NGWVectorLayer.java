/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.NetworkUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.nextgis.maplib.util.Constants.IO_BUFFER_SIZE;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_VECTOR;
import static com.nextgis.maplib.util.Constants.TAG;


public class NGWVectorLayer extends VectorLayer implements INGWLayer
{
    protected       String       mAccount;
    protected       String       mURL;
    protected       NetworkUtil  mNet;
    protected       String       mLogin;
    protected       String       mPassword;

    protected static final String JSON_ACCOUNT_KEY = "account";
    protected static final String JSON_URL_KEY = "url";
    protected static final String JSON_LOGIN_KEY = "login";
    protected static final String JSON_PASSWORD_KEY = "password";

    public NGWVectorLayer(
            Context context,
            File path)
    {
        super(context, path);

        mNet = new NetworkUtil(context);
    }

    @Override
    public void setAccount(String account)
    {
        mAccount = account;
    }


    public void setURL(String URL)
    {
        mURL = URL;
    }


    public void setLogin(String login)
    {
        mLogin = login;
    }


    public void setPassword(String password)
    {
        mPassword = password;
    }

    @Override
    public int getType()
    {
        return LAYERTYPE_NGW_VECTOR;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_ACCOUNT_KEY, mAccount);
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
        mAccount = jsonObject.getString(JSON_ACCOUNT_KEY);
        mURL = jsonObject.getString(JSON_URL_KEY);
        if(jsonObject.has(JSON_LOGIN_KEY))
            mLogin = jsonObject.getString(JSON_LOGIN_KEY);
        if(jsonObject.has(JSON_PASSWORD_KEY))
            mPassword = jsonObject.getString(JSON_PASSWORD_KEY);

        if(!mIsInitialized){
            //init in separate thread
            downloadAsync();
        }
    }

    @Override
    public String getAccount()
    {
        return mAccount;
    }

    public void downloadAsync()
    {
        new DownloadImageTask().execute();
    }

    public String download()
    {
        if (!mNet.isNetworkAvailable()) { //return tile from cache
            return mContext.getString(R.string.error_network_unavailable);
        }

        try {

            final HttpGet get = new HttpGet(mURL);
            //basic auth
            if(null != mLogin && mLogin.length() > 0 && null != mPassword && mPassword.length() > 0){
                get.setHeader("Accept", "*/*");
                final String basicAuth = "Basic " + Base64.encodeToString(
                        (mLogin + ":" + mPassword).getBytes(), Base64.NO_WRAP);
                get.setHeader("Authorization", basicAuth);
            }

            final DefaultHttpClient HTTPClient = mNet.getHttpClient();
            final HttpResponse response = HTTPClient.execute(get);

            // Check to see if we got success
            final org.apache.http.StatusLine line = response.getStatusLine();
            if (line.getStatusCode() != 200) {
                Log.d(TAG, "Problem downloading GeoJSON: " + mURL + " HTTP response: " +
                           line);
                return mContext.getString(R.string.error_download_data);
            }

            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                Log.d(TAG, "No content downloading GeoJSON: " + mURL);
                return mContext.getString(R.string.error_download_data);
            }

            String data = EntityUtils.toString(entity);
            JSONObject geoJSONObject = new JSONObject(data);

            return createFromGeoJSON(geoJSONObject);

        } catch (IOException e) {
            Log.d(TAG, "Problem downloading GeoJSON: " + mURL + " Error: " +
                       e.getLocalizedMessage());
            return mContext.getString(R.string.error_download_data);
        }
        catch (JSONException e) {
            e.printStackTrace();
            return mContext.getString(R.string.error_download_data);
        }
    }

    protected class DownloadImageTask extends AsyncTask<Void, Void, String>
    {

        @Override
        protected String doInBackground(Void... voids)
        {
            return download();
        }

        @Override
        protected void onPostExecute(String error)
        {
            if(null != error && error.length() > 0){
                Toast.makeText(mContext, error, Toast.LENGTH_SHORT)
                     .show();
            }
        }
    }
}
