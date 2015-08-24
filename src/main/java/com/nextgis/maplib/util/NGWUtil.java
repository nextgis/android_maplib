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

package com.nextgis.maplib.util;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

import static com.nextgis.maplib.util.Constants.TAG;

public class NGWUtil
{
    public static String NGWKEY_ID = "id";
    public static String NGWKEY_GEOM = "geom";
    public static String NGWKEY_FIELDS = "fields";
    public static String NGWKEY_SRS = "srs";
    public static String NGWKEY_EXTENSIONS = "extensions";
    public static String NGWKEY_NAME = "name";
    public static String NGWKEY_MIME = "mime_type";
    public static String NGWKEY_DESCRIPTION = "description";
    public static String NGWKEY_YEAR = "year";
    public static String NGWKEY_MONTH = "month";
    public static String NGWKEY_DAY = "day";
    public static String NGWKEY_HOUR = "hour";
    public static String NGWKEY_MINUTE = "minute";
    public static String NGWKEY_SECOND = "second";

     /*
    NGW API Functions
     */

    public static String getConnectionCookie(String sUrl, String login, String password) throws IOException {
        sUrl += "/login";
        String sPayload = "login=" + login + "&password=" + password;
        final HttpURLConnection conn = NetworkUtil.getHttpConnection("POST", sUrl, null, null);
        if(null == conn){
            Log.d(TAG, "Error get connection object");
            return null;
        }
        conn.setInstanceFollowRedirects(false);
        conn.setDefaultUseCaches(false);
        conn.setDoOutput(true);
        conn.connect();

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(sPayload);

        writer.flush();
        writer.close();
        os.close();

        int responseCode = conn.getResponseCode();
        if (!(responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM)) {
            Log.d(TAG, "Problem execute post: " + sUrl + " HTTP response: " +
                    responseCode);
            return null;
        }

        String headerName;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equals("Set-Cookie")) {
                return conn.getHeaderField(i);
            }
        }

        return null;
    }


    public static String getFileUploadUrl(String server)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        return server + "/api/component/file_upload/upload";
    }


    /**
     * GeoJSON URL. Get data as GeoJSON
     *
     * @param server URL to NextGIS Web server
     * @param remoteId Vector layer resource id
     * @return URL
     */
    public static String getGeoJSONUrl(
            String server,
            long remoteId)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        return server + "/resource/" + remoteId + "/geojson/";
    }

    /**
     * TMS URL for raster layer
     *
     * @param server
     *         URL
     * @param styleId
     *         Raster style id
     *
     * @return URL to TMS for TMSLayer
     */
    public static String getTMSUrl(
            String server,
            long styleId)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        return server + "/resource/" + styleId + "//tms?z={z}&x={x}&y={y}";
    }


    /**
     * Resource URL
     *
     * @param server
     *         URL
     * @param remoteId
     *         resource id
     *
     * @return URL to resource
     */
    public static String getResourceUrl(
            String server,
            long remoteId)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        return server + "/resource/" + remoteId;
    }


    /**
     * The resource metadata (fields, geometry type, SRS, etc.)
     *
     * @param server
     *         URL
     * @param remoteId
     *         resource id
     *
     * @return URL to resource meta
     */
    public static String getResourceMetaUrl(
            String server,
            long remoteId)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        return server + "/api/resource/" + remoteId;
    }


    /**
     * Get one row from vector layer
     *
     * @param server
     *         URL
     * @param remoteId
     *         resource id
     * @param featureId
     *         row id
     *
     * @return URL to row
     */
    public static String getFeatureUrl(
            String server,
            long remoteId,
            long featureId)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        return server + "/api/resource/" + remoteId + "/feature/" + featureId;
    }


    /**
     * Get the url to JSONArray of features  (NOT GeoJSON!)
     *
     * @param server
     *         URL
     * @param remoteId
     *         vector layer id
     *
     * @return URL
     */
    public static String getFeaturesUrl(
            String server,
            long remoteId)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        return server + "/api/resource/" + remoteId + "/feature/";
    }

    public static String getFeaturesUrl(
            String server,
            long remoteId,
            String where){
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        if(TextUtils.isEmpty(where))
            return server + "/api/resource/" + remoteId + "/feature/";

        return server + "/api/resource/" + remoteId + "/feature/?" + where;
    }


    public static boolean containsCaseInsensitive(
            String strToCompare,
            String[] list)
    {
        for (String str : list) {
            if (str.equalsIgnoreCase(strToCompare)) {
                return (true);
            }
        }
        return (false);
    }


    public static String getFeatureAttachmentUrl(
            String server,
            long remoteId,
            long featureId)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        return server + "/api/resource/" + remoteId + "/feature/" + featureId + "/attachment/";
    }

}
