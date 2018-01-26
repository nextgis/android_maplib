/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.util.Pair;

import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.INGWResource;
import com.nextgis.maplib.datasource.ngw.Resource;
import com.nextgis.maplib.datasource.ngw.ResourceGroup;
import com.nextgis.maplib.map.NGWLookupTable;
import com.nextgis.maplib.map.VectorLayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import static com.nextgis.maplib.util.Constants.JSON_DISPLAY_NAME;
import static com.nextgis.maplib.util.Constants.JSON_ID_KEY;
import static com.nextgis.maplib.util.Constants.JSON_RESOURCE_KEY;
import static com.nextgis.maplib.util.Constants.TAG;


public class NGWUtil
{
    public static String NGWKEY_ID              = "id";
    public static String NGWKEY_GEOM            = "geom";
    public static String NGWKEY_FIELDS          = "fields";
    public static String NGWKEY_SRS             = "srs";
    public static String NGWKEY_EXTENSIONS      = "extensions";
    public static String NGWKEY_NAME            = "name";
    public static String NGWKEY_MIME            = "mime_type";
    public static String NGWKEY_DESCRIPTION     = "description";
    public static String NGWKEY_DISPLAY_NAME    = JSON_DISPLAY_NAME;
    public static String NGWKEY_YEAR            = "year";
    public static String NGWKEY_MONTH           = "month";
    public static String NGWKEY_DAY             = "day";
    public static String NGWKEY_HOUR            = "hour";
    public static String NGWKEY_MINUTE          = "minute";
    public static String NGWKEY_SECOND          = "second";
    public static String NGWKEY_FEATURE_COUNT   = "total_count";
    public static String NGWKEY_KEYNAME         = "keyname";
    public static String NGWKEY_PASSWORD        = "password";
    public static String NGWKEY_CLS             = "cls";
    public static String NGWKEY_VECTOR_LAYER    = "vector_layer";
    public static String NGWKEY_RESOURCE_GROUP  = "resource_group";
    public static String NGWKEY_PARENT          = "parent";
    public static String NGWKEY_GEOMETRY_TYPE   = "geometry_type";
    public static String NGWKEY_DATATYPE        = "datatype";
    public static String NGWKEY_LOOKUP_TABLE    = "lookup_table";
    public static String NGWKEY_RESMETA         = "resmeta";
    public static String NGWKEY_ITEMS           = "items";


    /**
     * NGW API Functions
     */

    public static String getConnectionCookie(
            AtomicReference<String> reference,
            String login,
            String password)
            throws IOException
    {
        String sUrl = reference.get();
        if (!sUrl.startsWith("http")) {
            sUrl = "http://" + sUrl;
            reference.set(sUrl);
        }

        sUrl += "/login";
        String sPayload = "login=" + login + "&password=" + password;
        final HttpURLConnection conn = NetworkUtil.getHttpConnection("POST", sUrl, null, null);
        if (null == conn) {
            Log.d(TAG, "Error get connection object: " + sUrl);
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
        if (!(responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM)) {
            Log.d(TAG, "Problem execute post: " + sUrl + " HTTP response: " + responseCode);
            return null;
        }

        String headerName;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equals("Set-Cookie")) {
                return conn.getHeaderField(i);
            }
        }

        if (!sUrl.startsWith("https")) {
            sUrl = sUrl.replace("http", "https").replace("/login", "");
            reference.set(sUrl);
        }

        return getConnectionCookie(reference, login, password);
    }


    public static String getFileUploadUrl(String server)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        return server + "/api/component/file_upload/upload";
    }


    public static String getNgwVersionUrl(String server)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        return server + "/api/component/pyramid/pkg_version";
    }


    public static Pair<Integer, Integer> getNgwVersion(Context context, String account) {
        Pair<Integer, Integer> ver = new Pair<>(-1, -1);
        try {
            AccountUtil.AccountData accountData = AccountUtil.getAccountData(context, account);
            ver = NGWUtil.getNgwVersion(accountData.url, accountData.login, accountData.password);
        } catch (IOException | JSONException | NumberFormatException | IllegalStateException ignored) { }

        return ver;
    }


    public static Pair<Integer, Integer> getNgwVersion(
            String url,
            String login,
            String password)
            throws IOException, JSONException, NumberFormatException
    {
        HttpResponse response =
                NetworkUtil.get(NGWUtil.getNgwVersionUrl(url), login, password, false);
        if (response.isOk()) {
            JSONObject verJSONObject = new JSONObject(response.getResponseBody());
            String fullVer = verJSONObject.getString("nextgisweb");
            String[] verParts = fullVer.split("\\.");

            if (2 == verParts.length) {
                Integer major = Integer.parseInt(verParts[0]);
                Integer minor = Integer.parseInt(verParts[1]);
                return new Pair<>(major, minor);
            } else {
                Log.d(TAG, "BAD format of the NGW version, must be 'major.minor', obtained: " + fullVer);
            }
        }

        return null;
    }


    /**
     * GeoJSON URL. Get data as GeoJSON
     *
     * @param server
     *         URL to NextGIS Web server
     * @param remoteId
     *         Vector layer resource id
     *
     * @return URL
     */
    public static String getGeoJSONUrl(
            String server,
            long remoteId)
    {
        return getResourceUrl(server, remoteId) + "/geojson/";
    }


    /**
     * TMS URL for raster layer
     *
     * @param server
     *         URL
     * @param styleIds
     *         Raster style ids
     *
     * @return URL to TMS for TMSLayer
     */
    public static String getTMSUrl(
            String server,
            Long[] styleIds)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }
        // old url return server + "/resource/" + styleId + "/tms?z={z}&x={x}&y={y}";

        String ids = "";
        if (styleIds != null && styleIds.length > 0) {
            ids += styleIds[0];
            for (int i = 1; i < styleIds.length; i++) {
                ids += "," + styleIds[i];
            }
        }

        return server + "/api/component/render/tile?x={x}&y={y}&z={z}&resource=" + ids;
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
        return getBaseUrl(server) + remoteId;
    }

    /**
     * The resource base url
     *
     * @param server
     *         URL
     *
     * @return URL to base resource
     */
    private static String getBaseUrl(String server) {
        if (!server.startsWith("http"))
            server = "http://" + server;

        return server + "/api/resource/";
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
        return getFeaturesUrl(server, remoteId) + featureId;
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
        return getResourceMetaUrl(server, remoteId) + "/feature/";
    }


    public static String getFeaturesUrl(
            String server,
            long remoteId,
            String where)
    {
        if (TextUtils.isEmpty(where))
            return getFeaturesUrl(server, remoteId);

        return getFeaturesUrl(server, remoteId) + "?" + where;
    }


    public static String getExtent(String server, long remoteId) {
        return getResourceMetaUrl(server, remoteId) + "/extent";
    }


    public static String getTrackedFeaturesUrl(
            String server,
            long remoteId,
            long startDate)
    {
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        String start = sdf.format(new Date(startDate));
        return server + "/api/vector_layer/" + remoteId + "/diff/?ts_start=" + start;
    }


    public static String getFeatureAttachmentUrl(
            String server,
            long remoteId,
            long featureId)
    {
        return getFeaturesUrl(server, remoteId) + featureId + "/attachment/";
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

            int nType = LayerUtil.stringToType(type);
            if (Constants.NOT_FOUND != nType) {
                fields.add(new Field(nType, name, alias));
            }
        }
        return fields;
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static Feature readNGWFeature(
            JsonReader reader,
            List<Field> fields,
            int nSRS)
            throws IOException, IllegalStateException, NumberFormatException, OutOfMemoryError
    {
        final Feature feature = new Feature(Constants.NOT_FOUND, fields);

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(NGWUtil.NGWKEY_ID)) {
                feature.setId(reader.nextLong());
            } else if (name.equals(NGWUtil.NGWKEY_GEOM)) {
                String wkt = reader.nextString();
                GeoGeometry geom = GeoGeometryFactory.fromWKT(wkt, nSRS);
                geom.setCRS(nSRS);
                if (nSRS != GeoConstants.CRS_WEB_MERCATOR) {
                    geom.project(GeoConstants.CRS_WEB_MERCATOR);
                }
                feature.setGeometry(geom);
            } else if (name.equals(NGWUtil.NGWKEY_FIELDS)) {
                readNGWFeatureFields(feature, reader, fields);
            } else if (name.equals(NGWUtil.NGWKEY_EXTENSIONS)) {
                if (reader.peek() != JsonToken.NULL) {
                    readNGWFeatureAttachments(feature, reader);
                }
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return feature;
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void readNGWFeatureFields(Feature feature, JsonReader reader, List<Field> fields)
            throws IOException, IllegalStateException, NumberFormatException
    {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (reader.peek() == JsonToken.NULL) {
                reader.skipValue();
            } else {
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
                if (!bAdded) {
                    reader.skipValue();
                }
            }
        }
        reader.endObject();
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void readNGWDate(Feature feature, JsonReader reader, String fieldName)
            throws IOException
    {
        reader.beginObject();
        int nYear = 1900;
        int nMonth = 1;
        int nDay = 1;
        int nHour = 0;
        int nMinute = 0;
        int nSecond = 0;
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(NGWUtil.NGWKEY_YEAR)) {
                nYear = reader.nextInt();
            } else if (name.equals(NGWUtil.NGWKEY_MONTH)) {
                nMonth = reader.nextInt();
            } else if (name.equals(NGWUtil.NGWKEY_DAY)) {
                nDay = reader.nextInt();
            } else if (name.equals(NGWUtil.NGWKEY_HOUR)) {
                nHour = reader.nextInt();
            } else if (name.equals(NGWUtil.NGWKEY_MINUTE)) {
                nMinute = reader.nextInt();
            } else if (name.equals(NGWUtil.NGWKEY_SECOND)) {
                nSecond = reader.nextInt();
            } else {
                reader.skipValue();
            }
        }

        TimeZone timeZone = TimeZone.getDefault();
        timeZone.setRawOffset(0); // set to UTC
        Calendar calendar = new GregorianCalendar(timeZone);
        calendar.set(nYear, nMonth - 1, nDay, nHour, nMinute, nSecond);
        calendar.set(Calendar.MILLISECOND, 0); // we must to reset millis
        feature.setFieldValue(fieldName, calendar.getTimeInMillis());

        reader.endObject();
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void readNGWFeatureAttachments(Feature feature, JsonReader reader)
            throws IOException
    {
        //add extensions
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("attachment") && reader.peek() != JsonToken.NULL) {
                reader.beginArray();
                while (reader.hasNext()) {
                    readNGWFeatureAttachment(feature, reader);
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void readNGWFeatureAttachment(Feature feature, JsonReader reader)
            throws IOException
    {
        reader.beginObject();
        String attachId = "";
        String name = "";
        String mime = "";
        String descriptionText = "";
        while (reader.hasNext()) {
            String keyName = reader.nextName();
            if (reader.peek() == JsonToken.NULL) {
                reader.skipValue();
                continue;
            }

            if (keyName.equals(NGWUtil.NGWKEY_ID)) {
                attachId += reader.nextLong();
            } else if (keyName.equals(NGWUtil.NGWKEY_NAME)) {
                name += reader.nextString();
            } else if (keyName.equals(NGWUtil.NGWKEY_MIME)) {
                mime += reader.nextString();
            } else if (keyName.equals(NGWUtil.NGWKEY_DESCRIPTION)) {
                descriptionText += reader.nextString();
            } else {
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
        if (null != progressor) {
            progressor.setMax(featuresJSONArray.length());
            progressor.setIndeterminate(false);
        }

        for (int i = 0; i < featuresJSONArray.length(); i++) {
            JSONObject featureJSONObject = featuresJSONArray.getJSONObject(i);

            if (null != progressor) {
                progressor.setValue(i);
            }

            long id = featureJSONObject.getLong(NGWUtil.NGWKEY_ID);
            String wkt = featureJSONObject.getString(NGWUtil.NGWKEY_GEOM);
            JSONObject fieldsJSONObject = featureJSONObject.getJSONObject(NGWUtil.NGWKEY_FIELDS);
            Feature feature = new Feature(id, fields);
            GeoGeometry geom = GeoGeometryFactory.fromWKT(wkt, nSRS);
            if (null == geom)
                continue;

            geom.setCRS(nSRS);
            if (nSRS != GeoConstants.CRS_WEB_MERCATOR)
                geom.project(GeoConstants.CRS_WEB_MERCATOR);
            if (!geom.isValid())
                continue;

            feature.setGeometry(geom);

            for (Field field : fields) {
                if (field.getType() == GeoConstants.FTDateTime ||
                        field.getType() == GeoConstants.FTDate ||
                        field.getType() == GeoConstants.FTTime) {
                    if (!fieldsJSONObject.isNull(field.getName())) {
                        JSONObject dateJson = fieldsJSONObject.getJSONObject(field.getName());
                        int nYear = 1900;
                        int nMonth = 1;
                        int nDay = 1;
                        int nHour = 0;
                        int nMinute = 0;
                        int nSec = 0;
                        if (dateJson.has(NGWKEY_YEAR)) {
                            nYear = dateJson.getInt(NGWKEY_YEAR);
                        }
                        if (dateJson.has(NGWKEY_MONTH)) {
                            nMonth = dateJson.getInt(NGWKEY_MONTH);
                        }
                        if (dateJson.has(NGWKEY_DAY)) {
                            nDay = dateJson.getInt(NGWKEY_DAY);
                        }
                        if (dateJson.has(NGWKEY_HOUR)) {
                            nHour = dateJson.getInt(NGWKEY_HOUR);
                        }
                        if (dateJson.has(NGWKEY_MINUTE)) {
                            nMinute = dateJson.getInt(NGWKEY_MINUTE);
                        }
                        if (dateJson.has(NGWKEY_SECOND)) {
                            nSec = dateJson.getInt(NGWKEY_SECOND);
                        }

                        TimeZone timeZone = TimeZone.getDefault();
                        timeZone.setRawOffset(0); // set to UTC
                        Calendar calendar = new GregorianCalendar(timeZone);
                        calendar.set(nYear, nMonth - 1, nDay, nHour, nMinute, nSec);
                        calendar.set(Calendar.MILLISECOND, 0); // we must to reset millis
                        feature.setFieldValue(field.getName(), calendar.getTimeInMillis());
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


    public static boolean signUp(
            String server,
            String login,
            String password,
            String displayName,
            String description)
    {
        server += server.endsWith("/") ? "" : "/";
        server += "api/component/auth/register";
        if (!server.startsWith("http")) {
            server = "http://" + server;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put(NGWKEY_KEYNAME, login);
            payload.put(NGWKEY_PASSWORD, password);
            payload.put(NGWKEY_DISPLAY_NAME, displayName == null ? login : displayName);
            payload.put(NGWKEY_DESCRIPTION, description);

            HttpResponse response =
                    NetworkUtil.post(server, payload.toString(), null, null, false);
            return response.isOk() && new JSONObject(response.getResponseBody()).has(JSON_ID_KEY);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean getResourceByKey(Context context, INGWResource resource, Map<String, Resource> keys) {
        if (resource instanceof Connection) {
            Connection connection = (Connection) resource;
            connection.loadChildren();
        } else if (resource instanceof ResourceGroup) {
            ResourceGroup resourceGroup = (ResourceGroup) resource;
            resourceGroup.loadChildren();
        }

        for (int i = 0; i < resource.getChildrenCount(); ++i) {
            INGWResource childResource = resource.getChild(i);

            if (keys.containsKey(childResource.getKey()) && childResource instanceof Resource) {
                Resource ngwResource = (Resource) childResource;
                keys.put(ngwResource.getKey(), ngwResource);
            }

            boolean bIsFill = true;
            for (Map.Entry<String, Resource> entry : keys.entrySet()) {
                if (entry.getValue() == null) {
                    bIsFill = false;
                    break;
                }
            }

            if (bIsFill) {
                return true;
            }

            if (getResourceByKey(context, childResource, keys)) {
                return true;
            }
        }

        boolean bIsFill = true;

        for (Map.Entry<String, Resource> entry : keys.entrySet()) {
            if (entry.getValue() == null) {
                bIsFill = false;
                break;
            }
        }

        return bIsFill;
    }

    public static HttpResponse createNewResource(
            Context context,
            Connection connection,
            JSONObject json)
    {
        HttpResponse result;
        try {
            AccountUtil.AccountData accountData =
                    AccountUtil.getAccountData(context, connection.getName());
            result = NetworkUtil.post(getBaseUrl(accountData.url), json.toString(),
                    accountData.login, accountData.password, false);
        } catch (IOException e) {
            e.printStackTrace();
            result = new HttpResponse(500);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            result = new HttpResponse(401);
        }

        return result;
    }

    public static HttpResponse createNewGroup(
            Context context,
            Connection connection,
            long parentId,
            String name,
            String keyName)
    {
        JSONObject payload = new JSONObject();
        try {
            JSONObject resource = new JSONObject();
            resource.put(NGWKEY_CLS, NGWKEY_RESOURCE_GROUP);
            JSONObject id = new JSONObject();
            id.put(JSON_ID_KEY, parentId);
            resource.put(NGWKEY_PARENT, id);
            resource.put(NGWKEY_DISPLAY_NAME, name);
            if (keyName != null) {
                resource.put(NGWKEY_KEYNAME, keyName);
            }
            payload.put(JSON_RESOURCE_KEY, resource);
        } catch (JSONException e) {
            e.printStackTrace();
            return new HttpResponse(500);
        }

        return createNewResource(context, connection, payload);
    }

    public static HttpResponse createNewLayer(
            Connection connection,
            VectorLayer layer,
            long parentId,
            String keyName)
    {
        JSONObject payload = new JSONObject();
        try {
            JSONObject resource = new JSONObject();
            resource.put(NGWKEY_CLS, NGWKEY_VECTOR_LAYER);
            JSONObject id = new JSONObject();
            id.put(JSON_ID_KEY, parentId);
            resource.put(NGWKEY_PARENT, id);
            resource.put(JSON_DISPLAY_NAME, layer.getName());
            if (keyName != null) {
                resource.put(NGWKEY_KEYNAME, keyName);
            }
            payload.put(JSON_RESOURCE_KEY, resource);

            JSONObject vectorLayer = new JSONObject();
            JSONObject srs = new JSONObject();
            srs.put(JSON_ID_KEY, GeoConstants.CRS_WEB_MERCATOR);
            vectorLayer.put(NGWKEY_SRS, srs);
            vectorLayer.put(
                    NGWKEY_GEOMETRY_TYPE, GeoGeometryFactory.typeToString(layer.getGeometryType()));
            JSONArray fields = new JSONArray();
            for (Field field : layer.getFields()) {
                JSONObject current = new JSONObject();
                current.put(NGWKEY_KEYNAME, field.getName());
                current.put(NGWKEY_DISPLAY_NAME, field.getAlias());
                current.put(NGWKEY_DATATYPE, LayerUtil.typeToString(field.getType()));
                fields.put(current);
            }
            vectorLayer.put(NGWKEY_FIELDS, fields);
            payload.put(NGWKEY_VECTOR_LAYER, vectorLayer);
        } catch (JSONException e) {
            e.printStackTrace();
            return new HttpResponse(500);
        }

        return createNewResource(layer.getContext(), connection, payload);
    }

    public static HttpResponse createNewLookupTable(
            Connection connection,
            NGWLookupTable table,
            long parentId,
            String keyName)
    {
        JSONObject payload = new JSONObject();
        try {
            JSONObject resource = new JSONObject();
            resource.put(NGWKEY_CLS, NGWKEY_LOOKUP_TABLE);
            JSONObject id = new JSONObject();
            id.put(JSON_ID_KEY, parentId);
            resource.put(NGWKEY_PARENT, id);
            resource.put(JSON_DISPLAY_NAME, table.getName());
            if (keyName != null) {
                resource.put(NGWKEY_KEYNAME, keyName);
            }
            payload.put(JSON_RESOURCE_KEY, resource);

            JSONObject itemsResmeta = new JSONObject();
            itemsResmeta.put(NGWKEY_ITEMS, new JSONObject());
            payload.put(NGWKEY_RESMETA, itemsResmeta);

            JSONObject jsonData = new JSONObject();
            Map<String, String> data = table.getData();
            if (data != null) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    jsonData.put(entry.getKey(), entry.getValue());
                }
            }
            JSONObject items = new JSONObject();
            items.put(NGWKEY_ITEMS, jsonData);
            payload.put(NGWKEY_LOOKUP_TABLE, items);
        } catch (JSONException e) {
            e.printStackTrace();
            return new HttpResponse(500);
        }

        return createNewResource(table.getContext(), connection, payload);
    }
}
