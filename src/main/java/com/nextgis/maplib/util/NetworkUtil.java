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

package com.nextgis.maplib.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.nextgis.maplib.util.Constants.TAG;


public class NetworkUtil
{
    protected final ConnectivityManager mConnectionManager;
    protected final TelephonyManager    mTelephonyManager;
    protected       long                mLastCheckTime;
    protected       boolean             mLastState;
    protected       Context             mContext;

    public final static int TIMEOUT_CONNECTION = 10000;
    public final static int TIMEOUT_SOCKET = 240000; // 180 sec


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

    public static HttpURLConnection getHttpConnection(String method, String targetURL, String username,
                                               String password) throws IOException {
        URL url = new URL(targetURL);
        // Open a HTTP connection to the URL
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String basicAuth = getHTTPBaseAuth(username, password);
        if (null != basicAuth) {
            conn.setRequestProperty("Authorization", basicAuth);
        }
        conn.setRequestProperty("User-Agent", Constants.APP_USER_AGENT);

        // Allow Inputs
        conn.setDoInput(true);
        // Don't use a cached copy.
        conn.setUseCaches(false);
        // Use a post method.
        if(method.length() > 0)
            conn.setRequestMethod(method);

        conn.setConnectTimeout(TIMEOUT_CONNECTION);
        conn.setReadTimeout(TIMEOUT_SOCKET);
        conn.setRequestProperty("Accept", "*/*");

        return conn;
    }

    public static String getHTTPBaseAuth(String username, String password){
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            return "Basic " + Base64.encodeToString(
                    (username + ":" + password).getBytes(), Base64.NO_WRAP);
        }
        return null;
    }

    protected static String responseToString(final HttpURLConnection conn) throws IOException {
        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        InputStream is = conn.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyStream(is, baos, buffer, Constants.IO_BUFFER_SIZE);
        byte[] bytesReceived = baos.toByteArray();
        baos.close();
        is.close();

        return new String(bytesReceived);
    }

    public static void getStream(
            String targetURL,
            String username,
            String password,
            OutputStream outputStream)
            throws IOException
    {
        final HttpURLConnection conn = getHttpConnection("GET", targetURL, username, password);
        if(null == conn){
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Error get connection object");
            return;
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Problem execute getStream: " + targetURL + " HTTP response: " +
                    responseCode + " username: " + username);
            return;
        }

        byte data[] = new byte[Constants.IO_BUFFER_SIZE];
        InputStream is = conn.getInputStream();
        FileUtil.copyStream(is, outputStream, data, Constants.IO_BUFFER_SIZE);
        outputStream.close();
    }


    public static String get(
            String targetURL,
            String username,
            String password)
            throws IOException, NGException {
        final HttpURLConnection conn = getHttpConnection("GET", targetURL, username, password);
        if(null == conn){
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Error get connection object");
            return null;
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Problem execute get: " + targetURL + " HTTP response: " +
                    responseCode);
            throw new NGException(responseCode + "");
        }

        return responseToString(conn);
    }


    public static String post(
            String targetURL,
            String payload,
            String username,
            String password)
            throws IOException
    {
        final HttpURLConnection conn = getHttpConnection("POST", targetURL, username, password);
        if(null == conn){
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Error get connection object");
            return null;
        }
        conn.setRequestProperty("Content-type", "application/json");
        // Allow Outputs
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(payload);

        writer.flush();
        writer.close();
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Problem execute post: " + targetURL + " HTTP response: " +
                    responseCode);
            return null;
        }

        return responseToString(conn);
    }


    public static boolean delete(
            String targetURL,
            String username,
            String password)
            throws IOException
    {
        final HttpURLConnection conn = getHttpConnection("DELETE", targetURL, username, password);
        if(null == conn){
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Error get connection object");
            return false;
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Problem execute delete: " + targetURL + " HTTP response: " +
                    responseCode);
            return false;
        }

        return true;
    }


    public static String put(
            String targetURL,
            String payload,
            String username,
            String password)
            throws IOException
    {
        final HttpURLConnection conn = getHttpConnection("PUT", targetURL, username, password);
        if(null == conn){
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Error get connection object");
            return null;
        }
        conn.setRequestProperty("Content-type", "application/json");
        // Allow Outputs
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(payload);

        writer.flush();
        writer.close();
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Problem execute put: " + targetURL + " HTTP response: " +
                    responseCode);
            return null;
        }

        return responseToString(conn);
    }


    public static String postFile(
            String targetURL,
            String fileName,
            File file,
            String fileMime,
            String username,
            String password)
            throws IOException
    {
        final String lineEnd = "\r\n";
        final String twoHyphens = "--";
        final String boundary = "**nextgis**";

        //------------------ CLIENT REQUEST
        FileInputStream fileInputStream = new FileInputStream(file);
        // open a URL connection to the Servlet

        HttpURLConnection conn = getHttpConnection("POST", targetURL, username, password);
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        // Allow Outputs
        conn.setDoOutput(true);

        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
        dos.writeBytes(twoHyphens + boundary + lineEnd);
        dos.writeBytes(
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" +
                lineEnd);

        if (!TextUtils.isEmpty(fileMime)) {
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
        if (responseCode != HttpURLConnection.HTTP_OK) {
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Problem postFile(), targetURL: " + targetURL + " HTTP response: " +
                    responseCode);
            return null;
        }

        return responseToString(conn);
    }
}
