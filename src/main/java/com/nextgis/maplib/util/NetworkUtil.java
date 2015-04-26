
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

package com.nextgis.maplib.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import static com.nextgis.maplib.util.Constants.*;


public class NetworkUtil
{
    protected final ConnectivityManager mConnectionManager;
    protected final TelephonyManager    mTelephonyManager;
    protected long mLastCheckTime;
    protected boolean mLastState;
    protected Context mContext;

    public NetworkUtil(Context context)
    {
        mConnectionManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mLastCheckTime = Constants.NOT_FOUND;
        mContext = context;
    }


    public synchronized boolean isNetworkAvailable()
    {
        //if(System.currentTimeMillis() - mLastCheckTime < ONE_SECOND * 5)     //check every 5 sec.
        //    return mLastState;

        //mLastCheckTime = System.currentTimeMillis();
        mLastState = false;

        if (mConnectionManager == null) {
            return false;
        }


        NetworkInfo info = mConnectionManager.getActiveNetworkInfo();
        if (info == null) //|| !cm.getBackgroundDataSetting()
        {
            return false;
        }

        int netType = info.getType();
        if (netType == ConnectivityManager.TYPE_WIFI) {
            mLastState = info.isConnected();
        } else if (netType ==
                   ConnectivityManager.TYPE_MOBILE) { // netSubtype == TelephonyManager.NETWORK_TYPE_UMTS
            if (mTelephonyManager != null && !mTelephonyManager.isNetworkRoaming()) {
                mLastState = info.isConnected();
            }
        }

        return mLastState;
    }

    public void setProxy(DefaultHttpClient client, String url){
        HttpHost httpproxy;
        String proxyAddress;
        int proxyPort;
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH )
        {
            if(url.startsWith("https")){
                proxyAddress = System.getProperty("https.proxyHost");
                String portStr = System.getProperty("https.proxyPort");
                proxyPort = Integer.parseInt((portStr != null ? portStr : "-1"));
            }
            else {
                proxyAddress = System.getProperty("http.proxyHost");
                String portStr = System.getProperty("http.proxyPort");
                proxyPort = Integer.parseInt((portStr != null ? portStr : "-1"));
            }

            if(proxyPort < 0 || TextUtils.isEmpty(proxyAddress))
                return;

            httpproxy =  new HttpHost(proxyAddress, proxyPort);
        }
        else
        {
            proxyAddress = android.net.Proxy.getHost( mContext );
            proxyPort = android.net.Proxy.getPort( mContext );

            if(proxyPort < 0 || TextUtils.isEmpty(proxyAddress))
                return;
            httpproxy =  new HttpHost(proxyAddress, proxyPort);
        }

        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,  httpproxy);
    }

    public DefaultHttpClient getHttpClient()
    {
        /*HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION);
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOKET);
        */
        DefaultHttpClient HTTPClient = new DefaultHttpClient();//httpParameters);
        HTTPClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, APP_USER_AGENT);
        HTTPClient.getParams()
                  .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, TIMEOUT_CONNECTION);
        HTTPClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, TIMEOUT_SOKET);

        return HTTPClient;
    }

    public String get(String targetURL, String username, String password)
            throws IOException
    {
        final HttpGet get = new HttpGet(targetURL);
        //basic auth
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            final String basicAuth = "Basic " + Base64.encodeToString(
                    (username + ":" + password).getBytes(), Base64.NO_WRAP);
            get.setHeader("Authorization", basicAuth);
        }

        final DefaultHttpClient HTTPClient = getHttpClient();
        setProxy(HTTPClient, targetURL);
        final HttpResponse response = HTTPClient.execute(get);

        // Check to see if we got success
        final org.apache.http.StatusLine line = response.getStatusLine();
        if (line.getStatusCode() != 200) {
            Log.d(TAG, "Problem execute get: " + targetURL + " HTTP response: " + line);
            return null;
        }

        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            Log.d(TAG, "No content downloading: " + targetURL);
            return null;
        }

        return EntityUtils.toString(entity);
    }

    public String post(String targetURL, String payload, String username, String password)
            throws IOException
    {
        final HttpPost post = new HttpPost(targetURL);
        //basic auth
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            final String basicAuth = "Basic " + Base64.encodeToString(
                    (username + ":" + password).getBytes(), Base64.NO_WRAP);
            post.setHeader("Authorization", basicAuth);
        }

        post.setEntity(new StringEntity(payload, "UTF8"));
        post.setHeader("Content-type", "application/json");

        final DefaultHttpClient HTTPClient = getHttpClient();
        setProxy(HTTPClient, targetURL);
        final HttpResponse response = HTTPClient.execute(post);

        // Check to see if we got success
        final org.apache.http.StatusLine line = response.getStatusLine();
        if (line.getStatusCode() != 200) {
            Log.d(TAG, "Problem execute insert: " + targetURL + " HTTP response: " + line);
            return null;
        }

        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            Log.d(TAG, "No content downloading: " + targetURL);
            return null;
        }

        return EntityUtils.toString(entity);
    }

    public boolean delete(String targetURL, String username, String password)
            throws IOException
    {
        final HttpDelete delete = new HttpDelete(targetURL);
        //basic auth
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            final String basicAuth = "Basic " + Base64.encodeToString(
                    (username + ":" + password).getBytes(), Base64.NO_WRAP);
            delete.setHeader("Authorization", basicAuth);
        }

        final DefaultHttpClient HTTPClient = getHttpClient();
        setProxy(HTTPClient, targetURL);
        final HttpResponse response = HTTPClient.execute(delete);

        // Check to see if we got success
        final org.apache.http.StatusLine line = response.getStatusLine();
        if (line.getStatusCode() != 200) {
            Log.d(TAG, "Problem execute delete: " + targetURL + " HTTP response: " + line);
            return false;
        }

        return true;
    }

    public String put(String targetURL, String payload, String username, String password)
            throws IOException
    {
        final HttpPut put = new HttpPut(targetURL);
        //basic auth
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            final String basicAuth = "Basic " + Base64.encodeToString(
                    (username + ":" + password).getBytes(), Base64.NO_WRAP);
            put.setHeader("Authorization", basicAuth);
        }

        put.setEntity(new StringEntity(payload, "UTF8"));
        put.setHeader("Content-type", "application/json");


        final DefaultHttpClient HTTPClient = getHttpClient();
        setProxy(HTTPClient, targetURL);
        final HttpResponse response = HTTPClient.execute(put);

        // Check to see if we got success
        final org.apache.http.StatusLine line = response.getStatusLine();
        if (line.getStatusCode() != 200) {
            Log.d(TAG, "Problem execute update: " + targetURL + " HTTP response: " + line);
            return null;
        }

        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            Log.d(TAG, "No content downloading: " + targetURL);
            return null;
        }

        return EntityUtils.toString(entity);
    }

    public String postFile(String targetURL, String fileName, File file, String fileMime, String username, String password)
            throws IOException
    {
        final String lineEnd = "\r\n";
        final String twoHyphens = "--";
        final String boundary =  "**nextgis**";

        //------------------ CLIENT REQUEST
        FileInputStream fileInputStream = new FileInputStream(file);
        // open a URL connection to the Servlet
        URL url = new URL(targetURL);
        // Open a HTTP connection to the URL
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            final String basicAuth = "Basic " + Base64.encodeToString(
                    (username + ":" + password).getBytes(), Base64.NO_WRAP);
            conn.setRequestProperty ("Authorization", basicAuth);
        }

        // Allow Inputs
        conn.setDoInput(true);
        // Allow Outputs
        conn.setDoOutput(true);
        // Don't use a cached copy.
        conn.setUseCaches(false);
        // Use a post method.
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
        DataOutputStream dos = new DataOutputStream( conn.getOutputStream() );
        dos.writeBytes(twoHyphens + boundary + lineEnd);
        dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineEnd);

        if(!TextUtils.isEmpty(fileMime)){
            dos.writeBytes("Content-Type: " + fileMime + lineEnd);
        }

        dos.writeBytes(lineEnd);

        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        FileUtil.copyStream(fileInputStream, dos, buffer, Constants.IO_BUFFER_SIZE);

        dos.writeBytes(lineEnd);
        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
        fileInputStream.close();
        dos.flush();
        dos.close();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            return null;
        }

        InputStream is = conn.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyStream(is, baos, buffer, Constants.IO_BUFFER_SIZE);
        byte[] bytesReceived = baos.toByteArray();
        baos.close();
        is.close();

        return new String(bytesReceived);
    }
}
