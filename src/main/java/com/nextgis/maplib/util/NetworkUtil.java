/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2014-2021 NextGIS, info@nextgis.com
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
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.hypertrack.hyperlog.BuildConfig;
import com.hypertrack.hyperlog.HyperLog;
import com.nextgis.maplib.R;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import static com.nextgis.maplib.util.Constants.TAG;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;

import org.json.JSONObject;


public class NetworkUtil
{
    private static final String IP_ADDRESS = "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
            + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
            + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
            + "|[1-9][0-9]|[0-9]))(:[0-9]{1,5})?";
    public static final String URL_PATTERN = "^(?i)((ftp|https?)://)?(([\\da-z.-]+)\\.([a-z.]{2,6})|" + IP_ADDRESS + ")(:[0-9]{1,5})?(/\\S*)?$";

    public final ConnectivityManager mConnectionManager;
    public final TelephonyManager    mTelephonyManager;
    protected       long                mLastCheckTime;
    protected       Context             mContext;

    static String NGUid = null;
    static String isProString = "";
    static String userAgentPrefix1 = "";
    static String userAgentPrefix2 = "";
    static String userAgentPostfix = "none";

    public static String getUserAgent(String middlePart){
        return getUserAgentPrefix() + " "
                + middlePart + " " + getUserAgentPostfix();
    }

    private static String getUserAgentPrefix(){
        return userAgentPrefix1 +
                (TextUtils.isEmpty(NGUid) ? "" : "NGID " + NGUid  + "; ") +
                isProString +
                userAgentPrefix2;
    }

    private static String getUserAgentPostfix(){
        return userAgentPostfix;
    }

    public static void setUserAgentPrefix(final Context context, final String pref,
                                          final String deviceID, int versionCode){

        NGUid = AccountUtil.getNGUID(context);
        isProString = AccountUtil.isProUser(context)? "Supported; " : "";
        userAgentPrefix1 = pref
                + " (";
        userAgentPrefix2 =
                  "DID " + deviceID + "; "
                + "Build " + versionCode + "; "
                + "Vendor " + Build.MANUFACTURER
                +")";
    }

    public static void setUserNGUID(final String NGUID){
        if (TextUtils.isEmpty(NGUID))
            NGUid = "";
        else
            NGUid = NGUID ;
    }

    public static void setIsPro(boolean isPro){
        isProString = isPro ? "Supported; " : "";
    }

    public static void setUserAgentPostfix(final String pref){
        userAgentPostfix = pref;
    }

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
        if (mConnectionManager == null) {
            return false;
        }

        NetworkInfo info = mConnectionManager.getActiveNetworkInfo();
        if (info == null) {
            return false;
        }

        return info.isConnected();
    }

    public static HttpURLConnection getProperConnection(String targetURL) throws IOException {
        URL url = new URL(targetURL);
        // Open a HTTP connection to the URL
        HttpURLConnection result = null;
        if (targetURL.startsWith("https://")) {
            //configureSSLdefault();
            result = (HttpsURLConnection) url.openConnection();
        }
        else
            result= (HttpURLConnection) url.openConnection();
        result.setRequestProperty("User-Agent",getUserAgent(Constants.MAPLIB_USER_AGENT_PART));
        result.setRequestProperty("connection", "keep-alive");
        return result;

    }


    public static HttpURLConnection getHttpConnection(
            String method,
            String targetURL,
            String username,
            String password)
            throws IOException {
        HttpURLConnection conn = getProperConnection(targetURL);

        String basicAuth = getHTTPBaseAuth(username, password);
        if (null != basicAuth) {
            conn.setRequestProperty("Authorization", basicAuth);
        }

        return getHttpConnection(method, targetURL, conn);
    }

    public static HttpURLConnection getHttpConnection(
            String method,
            String targetURL,
            String auth)
            throws IOException {
        HttpURLConnection conn = getProperConnection(targetURL);

        if (null != auth) {
            conn.setRequestProperty("Authorization", auth);
        }

        return getHttpConnection(method, targetURL, conn);
    }


    public static HttpURLConnection getHttpConnection(
            String method,
            String targetURL,
            HttpURLConnection conn)
            throws IOException {
        conn.setRequestProperty("User-Agent", getUserAgentPrefix() + " "
                + Constants.MAPLIB_USER_AGENT_PART + " " + getUserAgentPostfix());

        // Allow Inputs
        conn.setDoInput(true);
        // Don't use a cached copy.
        conn.setUseCaches(false);
        // Use a post method.
        if (method.length() > 0)
            conn.setRequestMethod(method);

        conn.setConnectTimeout(TIMEOUT_CONNECTION);
        conn.setReadTimeout(TIMEOUT_SOCKET);
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("connection", "keep-alive");

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
            throw new IOException("Connection is null");
        }
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM && conn.getURL().getProtocol().equals("http")) {
            targetURL = targetURL.replace("http", "https");
            getStream(targetURL, username, password, outputStream);
            return;
        }
        if (responseCode != HttpURLConnection.HTTP_OK) {
            if(Constants.DEBUG_MODE)
                Log.d(TAG, "Problem execute getStream: " + targetURL + " HTTP response: " +
                    responseCode + " username: " + username);
            throw new IOException("Response code is " + responseCode);
        }

        byte data[] = new byte[Constants.IO_BUFFER_SIZE];
        InputStream is = conn.getInputStream();
        FileUtil.copyStream(is, outputStream, data, Constants.IO_BUFFER_SIZE);
        outputStream.close();
    }

    public static HttpResponse getHttpResponse(
            HttpURLConnection conn,
            boolean readErrorResponseBody)
            throws IOException
    {
        String method = conn.getRequestMethod();
        int code = conn.getResponseCode();
        String message = conn.getResponseMessage();
        HttpResponse response = new HttpResponse(code, message);

        if (code == HttpURLConnection.HTTP_MOVED_PERM && conn.getURL().getProtocol().equals("http")) {
            if (method.equals("PUT") || method.equals("POST")) {
                return response;
            }
            String target = conn.getURL().toString().replace("http", "https");
            String auth = conn.getRequestProperty("Authorization");
            HttpURLConnection connection = getHttpConnection(conn.getRequestMethod(), target, auth);
            return getHttpResponse(connection, readErrorResponseBody);
        }

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

            HyperLog.v(Constants.TAG, "HTTP post error null == conn with url " + targetURL);

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

        HttpResponse response = getHttpResponse(conn, readErrorResponseBody);
        // no perm 403 - need add
        if (response.mResponseCode == HTTP_FORBIDDEN){ // 403

        }

        if (response.mResponseCode == HttpURLConnection.HTTP_MOVED_PERM && conn.getURL().getProtocol().equals("http")) {
            targetURL = targetURL.replace("http", "https");
            return post(targetURL, payload, username, password, readErrorResponseBody);
        }
        return response;
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

        HttpResponse response = getHttpResponse(conn, readErrorResponseBody);
        if (response.mResponseCode == HttpURLConnection.HTTP_MOVED_PERM && conn.getURL().getProtocol().equals("http")) {
            targetURL = targetURL.replace("http", "https");
            return put(targetURL, payload, username, password, readErrorResponseBody);
        }
        return response;
    }


    public static HttpResponse postFile(
            String targetURL,
            String fileName,
            File file,
            long fileLength,
            String fileMime,
            String username,
            String password,
            boolean readErrorResponseBody)
            throws IOException
    {
        HyperLog.v(Constants.TAG, "postFile start url = " + targetURL + " filename " + fileName);


        //------------------ CLIENT REQUEST
        // open a URL connection to the Servlet
        HttpURLConnection conn = getHttpConnection(HTTP_POST, targetURL, username, password);
        if (null == conn) {
            HyperLog.v(Constants.TAG, "postFile getHttpConnection = null  start url = " + targetURL + " filename " + fileName);

            if (Constants.DEBUG_MODE)
                Log.d(TAG, "Error get connection object: " + targetURL);
            return  new HttpResponse(ERROR_CONNECT_FAILED);
        }

        final TusClient client = new TusClient();
        client.prepareConnection(conn);
        client.setUploadCreationURL(new URL(targetURL));
        String basicAuth = getHTTPBaseAuth(username, password);
        if (null != basicAuth) {
            conn.setRequestProperty("Authorization", basicAuth);
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", basicAuth);
        headers.put("Connection", "Keep-Alive");
        headers.put("User-Agent",getUserAgent(Constants.MAPLIB_USER_AGENT_PART));
        client.setHeaders(headers);

        String returnUrl = "";
        final TusUpload upload = new TusUpload(file);

        try {
            TusUploader uploader = client.resumeOrCreateUpload(upload);
            uploader.setChunkSize(10240);

            do {
                //long totalBytes = upload.getSize();
                //long bytesUploaded = uploader.getOffset();
                //double progress = (double) bytesUploaded / totalBytes * 100;
                //System.out.printf("Upload at %06.2f%%.\n", progress);
            } while (uploader.uploadChunk() > -1);
            uploader.finish();
            returnUrl = uploader.getUploadURL().toString();

        } catch (ProtocolException exception) {
            HyperLog.v(Constants.TAG, "postFile upload fail ProtocolException " + exception.getMessage());

            return new HttpResponse(0);
        }

        if (! TextUtils.isEmpty(returnUrl) ){
            HttpResponse response = NetworkUtil.get(returnUrl, username, password,false);
            if (response.mIsOk){
                try {
                    JSONObject result = new JSONObject(response.getResponseBody());
                    result.put("name", fileName);
                    HttpResponse responseS = new HttpResponse(response.getResponseCode(), response.getResponseMessage(),
                            result.toString());
                    responseS.setOk(true);
                    return  responseS;
                } catch (Exception ex){
                    HyperLog.v(Constants.TAG, "postFile Exception " + ex.getMessage() );

                    Log.e("ff", ex.getMessage());
                }
                return response;
            }
        }else {
            HyperLog.v(Constants.TAG, "postFile returnUrl =0  EXIT " );

        }
        return new HttpResponse(200);
    }

    public static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
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

    public static void configureAllTrustSSLCert(){

        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        } catch (Exception ex){
            Log.e("ssl_trust_store_error", ex.getMessage());
        }
    }

    public static void configureSSLdefault(){
        //configureAllTrustSSLCert();

//        try {
//            SSLContext sslContext = null;
//
//            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            // Load the CA certificates into the KeyStore
//            // This is a simplified example; you'll need to handle exceptions and possibly convert certificates to the correct format
//            keyStore.load(null, null);
//            // Create a TrustManager that trusts the certificates in the KeyStore
//            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            tmf.init(keyStore);
//            TrustManager[] trustManagers = tmf.getTrustManagers();
//
//            // Step 3: Configure the SSL Context
//            sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, trustManagers, new SecureRandom());
//            if (sslContext != null)
//                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
//            else
//                Log.e("ssl_trust_store_error", "sslContext is NULL!!!!");
//
//        } catch (Exception ex){
//            Log.e("ssl_trust_store_error", ex.getMessage());
//        }
    }
}
