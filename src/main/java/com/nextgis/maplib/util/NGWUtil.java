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

import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

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
    public static String NGWKEY_FEATURE_COUNT = "total_count";

     /**
      * NGW API Functions
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
        return server + "/resource/" + styleId + "/tms?z={z}&x={x}&y={y}";
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

    public static List<Field> getFieldsFromJson(JSONArray fieldsJSONArray)
            throws JSONException
    {
        List<Field> fields = new LinkedList<>();
        for (int i = 0; i < fieldsJSONArray.length(); i++) {
            JSONObject fieldJSONObject = fieldsJSONArray.getJSONObject(i);
            String type = fieldJSONObject.getString("datatype");
            String alias = fieldJSONObject.getString("display_name");
            String name = fieldJSONObject.getString("keyname");

            int nType = stringToType(type);
            if (Constants.NOT_FOUND != nType) {
                fields.add(new Field(nType, name, alias));
            }
        }
        return fields;
    }

    protected static int stringToType(String type)
    {
        switch (type) {
            case "STRING":
                return GeoConstants.FTString;
            case "INTEGER":
                return GeoConstants.FTInteger;
            case "REAL":
                return GeoConstants.FTReal;
            case "DATETIME":
                return GeoConstants.FTDateTime;
            case "DATE":
                return GeoConstants.FTDate;
            case "TIME":
                return GeoConstants.FTTime;
            default:
                return Constants.NOT_FOUND;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static Feature readNGWFeature(JsonReader reader, List<Field> fields, int nSRS) throws IOException {
        final Feature feature = new Feature(Constants.NOT_FOUND, fields);

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(NGWUtil.NGWKEY_ID)) {
                feature.setId(reader.nextLong());
            } else if (name.equals(NGWUtil.NGWKEY_GEOM)) {
                String wkt = reader.nextString();
                GeoGeometry geom = GeoGeometryFactory.fromWKT(wkt);
                geom.setCRS(nSRS);
                if (nSRS != GeoConstants.CRS_WEB_MERCATOR) {
                    geom.project(GeoConstants.CRS_WEB_MERCATOR);
                }
                feature.setGeometry(geom);
            }
            else if(name.equals(NGWUtil.NGWKEY_FIELDS)){
                readNGWFeatureFields(feature, reader, fields);
            }
            else if(name.equals(NGWUtil.NGWKEY_EXTENSIONS)){
                if (reader.peek() != JsonToken.NULL) {
                    readNGWFeatureAttachments(feature, reader);
                }
            }
            else
                reader.skipValue();
        }
        reader.endObject();
        return feature;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected static void readNGWFeatureFields(Feature feature, JsonReader reader, List<Field> fields) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if(reader.peek() == JsonToken.NULL)
                reader.skipValue();
            else {
                boolean bAdded = false;
                for (Field field : fields) {
                    if (field.getName().equals(name)) {
                        switch (field.getType()) {
                            case GeoConstants.FTReal:
                                feature.setFieldValue(field.getName(), reader.nextDouble());
                                bAdded = true;
                                break;
                            case GeoConstants.FTInteger:
                                feature.setFieldValue(field.getName(), reader.nextInt());
                                bAdded = true;
                                break;
                            case GeoConstants.FTString:
                                feature.setFieldValue(field.getName(), reader.nextString());
                                bAdded = true;
                                break;
                            case GeoConstants.FTDate:
                            case GeoConstants.FTTime:
                            case GeoConstants.FTDateTime:
                                readNGWDate(feature, reader, field.getName());
                                bAdded = true;
                                break;
                            default:
                                break;
                        }
                        break;
                    }
                }
                if(!bAdded)
                    reader.skipValue();
            }
        }
        reader.endObject();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected static void readNGWDate(Feature feature, JsonReader reader, String fieldName) throws IOException {
        reader.beginObject();
        int nYear = 1900;
        int nMonth = 1;
        int nDay = 1;
        int nHour = 0;
        int nMinute = 0;
        int nSecond = 0;
        while (reader.hasNext()){
            String name = reader.nextName();
            if(name.equals(NGWUtil.NGWKEY_YEAR)){
                nYear = reader.nextInt();
            }
            else if(name.equals(NGWUtil.NGWKEY_MONTH)){
                nMonth = reader.nextInt();
            }
            else if(name.equals(NGWUtil.NGWKEY_DAY)){
                nDay = reader.nextInt();
            }
            else if(name.equals(NGWUtil.NGWKEY_HOUR)){
                nHour = reader.nextInt();
            }
            else if(name.equals(NGWUtil.NGWKEY_MINUTE)){
                nMinute = reader.nextInt();
            }
            else if(name.equals(NGWUtil.NGWKEY_SECOND)){
                nSecond = reader.nextInt();
            }
            else {
                reader.skipValue();
            }
        }

        Calendar calendar = new GregorianCalendar(nYear, nMonth - 1, nDay, nHour, nMinute, nSecond);
        feature.setFieldValue(fieldName, calendar.getTimeInMillis());

        reader.endObject();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected static void readNGWFeatureAttachments(Feature feature, JsonReader reader) throws IOException {
        //add extensions
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if(name.equals("attachment") && reader.peek() != JsonToken.NULL){
                reader.beginArray();
                while (reader.hasNext()) {
                    readNGWFeatureAttachment(feature, reader);
                }
                reader.endArray();
            }
            else {
                reader.skipValue();
            }
        }

        reader.endObject();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected static void readNGWFeatureAttachment(Feature feature, JsonReader reader) throws IOException {
        reader.beginObject();
        String attachId = "";
        String name = "";
        String mime = "";
        String descriptionText = "";
        while (reader.hasNext()) {
            String keyName = reader.nextName();
            if(reader.peek() == JsonToken.NULL){
                reader.skipValue();
                continue;
            }

            if(keyName.equals(NGWUtil.NGWKEY_ID)){
                attachId += reader.nextLong();
            }
            else if(keyName.equals(NGWUtil.NGWKEY_NAME)){
                name += reader.nextString();
            }
            else if(keyName.equals(NGWUtil.NGWKEY_MIME)){
                mime += reader.nextString();
            }
            else if(keyName.equals(NGWUtil.NGWKEY_DESCRIPTION)){
                descriptionText += reader.nextString();
            }
            else{
                reader.skipValue();
            }
        }
        AttachItem item = new AttachItem(attachId, name, mime, descriptionText);
        feature.addAttachment(item);

        reader.endObject();
    }


    public static List<Feature> jsonToFeatures(
            JSONArray featuresJSONArray,
            List<Field> fields,
            int nSRS,
            IProgressor progressor)
            throws JSONException
    {
        List<Feature> features = new LinkedList<>();
        if(null != progressor){
            progressor.setMax(featuresJSONArray.length());
            progressor.setIndeterminate(false);
        }

        for (int i = 0; i < featuresJSONArray.length(); i++) {
            JSONObject featureJSONObject = featuresJSONArray.getJSONObject(i);

            if(null != progressor){
                progressor.setValue(i);
            }

            long id = featureJSONObject.getLong(NGWUtil.NGWKEY_ID);
            String wkt = featureJSONObject.getString(NGWUtil.NGWKEY_GEOM);
            JSONObject fieldsJSONObject = featureJSONObject.getJSONObject(NGWUtil.NGWKEY_FIELDS);
            Feature feature = new Feature(id, fields);
            GeoGeometry geom = GeoGeometryFactory.fromWKT(wkt);
            if (null == geom) {
                continue;
            }
            geom.setCRS(nSRS);
            if (nSRS != GeoConstants.CRS_WEB_MERCATOR) {
                geom.project(GeoConstants.CRS_WEB_MERCATOR);
            }
            feature.setGeometry(geom);

            for (Field field : fields) {
                if (field.getType() == GeoConstants.FTDateTime ||
                        field.getType() == GeoConstants.FTDate ||
                        field.getType() == GeoConstants.FTTime ) {
                    if (!fieldsJSONObject.isNull(field.getName())) {
                        JSONObject dateJson = fieldsJSONObject.getJSONObject(field.getName());
                        int nYear = 1900;
                        int nMonth = 1;
                        int nDay = 1;
                        int nHour = 0;
                        int nMinute = 0;
                        int nSec = 0;
                        if(dateJson.has(NGWKEY_YEAR))
                            nYear = dateJson.getInt(NGWKEY_YEAR);
                        if(dateJson.has(NGWKEY_MONTH))
                            nMonth = dateJson.getInt(NGWKEY_MONTH);
                        if(dateJson.has(NGWKEY_DAY))
                            nDay = dateJson.getInt(NGWKEY_DAY);
                        if(dateJson.has(NGWKEY_HOUR))
                            nHour = dateJson.getInt(NGWKEY_HOUR);
                        if(dateJson.has(NGWKEY_MINUTE))
                            nMinute = dateJson.getInt(NGWKEY_MINUTE);
                        if(dateJson.has(NGWKEY_SECOND))
                            nSec = dateJson.getInt(NGWKEY_SECOND);

                        Calendar calendar = new GregorianCalendar(nYear, nMonth - 1, nDay, nHour, nMinute, nSec);
                        feature.setFieldValue(field.getName(), calendar.getTime());
                    }
                } else {
                    if (!fieldsJSONObject.isNull(field.getName())) {
                        feature.setFieldValue(
                                field.getName(), fieldsJSONObject.get(field.getName()));
                    }
                }
            }

            //add extensions
            if (featureJSONObject.has("extensions")) {
                JSONObject ext = featureJSONObject.getJSONObject("extensions");
                //get attachment & description
                if (!ext.isNull("attachment")) {
                    JSONArray attachment = ext.getJSONArray("attachment");
                    for (int j = 0; j < attachment.length(); j++) {
                        JSONObject jsonAttachmentDetails = attachment.getJSONObject(j);
                        String attachId = "" + jsonAttachmentDetails.getLong(Constants.JSON_ID_KEY);
                        String name = jsonAttachmentDetails.getString(Constants.JSON_NAME_KEY);
                        String mime = jsonAttachmentDetails.getString("mime_type");
                        String descriptionText = jsonAttachmentDetails.getString("description");
                        AttachItem item = new AttachItem(attachId, name, mime, descriptionText);
                        feature.addAttachment(item);
                    }
                }
            }

            features.add(feature);
        }
        return features;
    }

}
