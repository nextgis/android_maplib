/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2018 NextGIS, info@nextgis.com
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
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.nextgis.maplib.R;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nextgis.maplib.util.Constants.TAG;


public class NetworkUtil
{
    private static final String IP_ADDRESS = "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
            + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
            + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
            + "|[1-9][0-9]|[0-9]))(:[0-9]{1,5})?";
    public static final String URL_PATTERN = "^(?i)((ftp|https?)://)?(([\\da-z.-]+)\\.([a-z.]{2,6})|" + IP_ADDRESS + ")(:[0-9]{1,5})?(/\\S*)?$";

    protected final ConnectivityManager mConnectionManager;
    protected final TelephonyManager    mTelephonyManager;
    protected       long                mLastCheckTime;
    protected       boolean             mLastState;
    protected       Context             mContext;

    public final static int TIMEOUT_CONNECTION = 10000;
    public final static int TIMEOUT_SOCKET = 240000; // 180 sec

    public final static int ERROR_AUTH                = -401;
    public final static int ERROR_NETWORK_UNAVAILABLE = -1;
    public final static int ERROR_CONNECT_FAILED      = 0;
    public final static int ERROR_DOWNLOAD_DATA       = 1;

    public final static String HTTP_GET    = "GET";
    public final static String HTTP_POST   = "POST";
    public final static String HTTP_PUT    = "PUT";
    public final static String HTTP_DELETE = "DELETE";


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

    public static HttpURLConnection getHttpConnection(
            String method,
            String targetURL,
            String username,
            String password)
            throws IOException {
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

        return isValidUri(targetURL) ? conn : null;
    }

    public static boolean isValidUri(String url) {
        Pattern pattern = Pattern.compile(URL_PATTERN);
        Matcher match = pattern.matcher(url);
        return match.matches();
    }

    public static String getHTTPBaseAuth(String username, String password){
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            return "Basic " + Base64.encodeToString(
                    (username + ":" + password).getBytes(), Base64.NO_WRAP);
        }
        return null;
    }

    public static String responseToString(final InputStream is)
            throws IOException
    {
        if (is == null) {
            return null;
        }

        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
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
        if (null == conn) {
            if (Constants.DEBUG_MODE)
                Log.d(TAG, "Error get stream: " + targetURL);
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

    protected static HttpResponse getHttpResponse(
            HttpURLConnection conn,
            boolean readErrorResponseBody)
            throws IOException
    {
        String method = conn.getRequestMethod();
        int code = conn.getResponseCode();
        String message = conn.getResponseMessage();
        HttpResponse response = new HttpResponse(code, message);

        if (!(code == HttpURLConnection.HTTP_OK
                || (code == HttpURLConnection.HTTP_CREATED && method.equals(HTTP_POST)))) {
            if (Constants.DEBUG_MODE) {
                String url = conn.getURL().toExternalForm();
                if ("Keep-Alive".equals(conn.getRequestProperty("Connection")))
                    method = "postFile(), targetURL";
                Log.d(TAG, "Problem execute " + method + ": " + url + " HTTP response: " + code);
            }
            if (readErrorResponseBody)
                response.setResponseBody(responseToString(conn.getErrorStream()));
            return response;
        }

        String body = responseToString(conn.getInputStream());
        if (null == body) {
            response.setResponseCode(ERROR_DOWNLOAD_DATA);
            response.setResponseMessage(null);
            return response;
        }

        response.setResponseBody(body);
        response.setOk(true);
        return response;
    }

    public static HttpResponse get(
            String targetURL,
            String username,
            String password,
            boolean readErrorResponseBody)
            throws IOException {
        final HttpURLConnection conn = getHttpConnection(HTTP_GET, targetURL, username, password);
        if (null == conn) {
            if (Constants.DEBUG_MODE)
                Log.d(TAG, "Error get connection object: " + targetURL);
            return new HttpResponse(ERROR_CONNECT_FAILED);
        }
        return getHttpResponse(conn, readErrorResponseBody);
    }


    public static HttpResponse post(
            String targetURL,
            String payload,
            String username,
            String password,
            boolean readErrorResponseBody)
            throws IOException
    {
        final HttpURLConnection conn = getHttpConnection(HTTP_POST, targetURL, username, password);
        if (null == conn) {
            if (Constants.DEBUG_MODE)
                Log.d(TAG, "Error get connection object: " + targetURL);
            return new HttpResponse(ERROR_CONNECT_FAILED);
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

        return getHttpResponse(conn, readErrorResponseBody);
    }

    public static HttpResponse delete(
            String targetURL,
            String username,
            String password,
            boolean readErrorResponseBody)
            throws IOException
    {
        final HttpURLConnection conn = getHttpConnection(HTTP_DELETE, targetURL, username, password);
        if (null == conn) {
            if (Constants.DEBUG_MODE)
                Log.d(TAG, "Error get connection object: " + targetURL);
            return new HttpResponse(ERROR_CONNECT_FAILED);
        }
        return getHttpResponse(conn, readErrorResponseBody);
    }


    public static HttpResponse put(
            String targetURL,
            String payload,
            String username,
            String password,
            boolean readErrorResponseBody)
            throws IOException
    {
        final HttpURLConnection conn = getHttpConnection(HTTP_PUT, targetURL, username, password);
        if (null == conn) {
            if (Constants.DEBUG_MODE)
                Log.d(TAG, "Error get connection object: " + targetURL);
            return new HttpResponse(ERROR_CONNECT_FAILED);
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

        return getHttpResponse(conn, readErrorResponseBody);
    }


    public static HttpResponse postFile(
            String targetURL,
            String fileName,
            File file,
            String fileMime,
            String username,
            String password,
            boolean readErrorResponseBody)
            throws IOException
    {
        final String lineEnd = "\r\n";
        final String twoHyphens = "--";
        final String boundary = "**nextgis**";

        //------------------ CLIENT REQUEST
        FileInputStream fileInputStream = new FileInputStream(file);
        // open a URL connection to the Servlet

        HttpURLConnection conn = getHttpConnection(HTTP_POST, targetURL, username, password);
        if (null == conn) {
            if (Constants.DEBUG_MODE)
                Log.d(TAG, "Error get connection object: " + targetURL);
            return new HttpResponse(ERROR_CONNECT_FAILED);
        }
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

        return getHttpResponse(conn, readErrorResponseBody);
    }

    public static String getError(
            Context context,
            int responseCode)
    {
        switch (responseCode) {
            case ERROR_AUTH:
                return context.getString(R.string.error_auth);
            case ERROR_NETWORK_UNAVAILABLE:
                return context.getString(R.string.error_network_unavailable);
            case ERROR_CONNECT_FAILED:
                return context.getString(R.string.error_connect_failed);
            case ERROR_DOWNLOAD_DATA:
                return context.getString(R.string.error_download_data);
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                return context.getString(R.string.error_401);
            case HttpURLConnection.HTTP_FORBIDDEN:
                return context.getString(R.string.error_403);
            case HttpURLConnection.HTTP_NOT_FOUND:
                return context.getString(R.string.error_404);
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                return context.getString(R.string.error_500);
            default:
                return context.getString(R.string.error_500);
        }
    }
}
