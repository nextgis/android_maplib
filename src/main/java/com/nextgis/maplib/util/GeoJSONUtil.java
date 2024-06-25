/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2017, 2019 NextGIS, info@nextgis.com
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
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.JsonReader;
import android.util.JsonToken;

import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.map.VectorLayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.nextgis.maplib.util.GeoConstants.FTDate;
import static com.nextgis.maplib.util.GeoConstants.FTDateTime;
import static com.nextgis.maplib.util.GeoConstants.FTTime;

/**
 * Class to store features to Vector layer
 */
public class GeoJSONUtil {

    /**
     * Check if provided name support and throw NGWException if not. Now support EPSG 3857 and 4326 only.
     * @param crsName Spatial reference name
     * @param context Context to get error string
     * @return true if provided string is WGS84 (4326) or false if Web Mercator (3857)/
     * @throws NGException
     */
    public static boolean checkCRSSupportAndWGS(String crsName, Context context) throws NGException {
        boolean isWGS84;
        switch (crsName) {
            case GeoConstants.GEOJSON_CRS_WGS84:  // WGS84
            case GeoConstants.GEOJSON_CRS_EPSG_4326:  // WGS84
                isWGS84 = true;
                break;
            case GeoConstants.GEOJSON_CRS_EPSG_3857:
            case GeoConstants.GEOJSON_CRS_WEB_MERCATOR:  //Web Mercator
                isWGS84 = false;
                break;
            default:
                throw new NGException(context.getString(R.string.error_crs_unsupported));
        }
        return isWGS84;
    }

    protected static Object parseNumber(String value) {
        try {
            if (value.contains(".") || value.contains(",")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static Object parseDateTime(Object value, int type) {
        SimpleDateFormat sdf = null;

        switch (type) {
            case GeoConstants.FTDate:
                sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                break;
            case GeoConstants.FTTime:
                sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                break;
            case GeoConstants.FTDateTime:
                sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
                break;
        }

        if (sdf != null && value instanceof String)
            try {
                value = sdf.parse((String) value).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

        return value;
    }

    public static String formatDateTime(long millis, int type) {
        String result = millis + "";
        SimpleDateFormat sdf = null;

        switch (type) {
            case FTDate:
                sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                break;
            case FTTime:
                sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                break;
            case FTDateTime:
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                break;
        }

        if (sdf != null)
            try {
                result = sdf.format(new Date(millis));
            } catch (Exception e) {
                e.printStackTrace();
            }

        return result;
    }

    public static void fillLayerFromGeoJSONStream(VectorLayer layer, InputStream in, int srs, IProgressor progressor) throws IOException, NGException {
        int streamSize = in.available();
        if(null != progressor){
            progressor.setIndeterminate(false);
            progressor.setMax(streamSize);
            progressor.setMessage(layer.getContext().getString(R.string.start_fill_layer) + " " + layer.getName());
        }

        SQLiteDatabase db = null;
        if(layer.getFields() != null && layer.getFields().isEmpty()){
            db = DatabaseContext.getDbForLayer(layer);
        }

        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        boolean isWGS84 = srs == GeoConstants.CRS_WGS84;
        long counter = 0;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if(name.equals(GeoConstants.GEOJSON_TYPE_FEATURES)){
                reader.beginArray();
                while (reader.hasNext()) {
                    Feature feature = readGeoJSONFeature(reader, layer, isWGS84);
                    if (null != feature) {
                        if(layer.getFields() != null && !layer.getFields().isEmpty()){
                            if (feature.getGeometry() != null)
                                layer.create(feature.getGeometry().getType(), feature.getFields());

                            db = DatabaseContext.getDbForLayer(layer);
                        }

                        if(feature.getGeometry() != null) {
                            layer.createFeatureBatch(feature, db);
                            if(null != progressor){
                                if (progressor.isCanceled()) {
                                    layer.save();
                                    return;
                                }
                                progressor.setValue(streamSize - in.available());
                                progressor.setMessage(layer.getContext().getString(R.string.process_features) + ": " + counter++);
                            }
                        }
                    }
                }
                reader.endArray();
            }
            else {
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.close();

        //if(null != db)
        //    db.close(); // return pragma to init

        layer.save();
    }

    // TODO refactor it and fillLayerFromGeoJsonStream
    public static void createLayerFromGeoJSONStream(VectorLayer layer, InputStream in, IProgressor progressor, boolean isWGS84) throws IOException, NGException {
        int streamSize = in.available();
        if(null != progressor){
            progressor.setIndeterminate(false);
            progressor.setMax(streamSize);
            progressor.setMessage(layer.getContext().getString(R.string.start_fill_layer) + " " + layer.getName());
        }

        SQLiteDatabase db = null;
        if(layer.getFields() != null && layer.getFields().isEmpty()){
            db = DatabaseContext.getDbForLayer(layer);
        }

        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        long counter = 0;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(GeoConstants.GEOJSON_TYPE_FEATURES)) {
                reader.beginArray();
                while (reader.hasNext()) {
                    Feature feature = readGeoJSONFeature(reader, layer, isWGS84);
                    if (null != feature) {
                        if (layer.getFields() == null || layer.getFields().isEmpty()) {
                            if (feature.getGeometry() != null)
                                layer.create(feature.getGeometry().getType(), feature.getFields());

                            db = DatabaseContext.getDbForLayer(layer);
                        }

                        if (feature.getGeometry() != null) {
                            layer.createFeatureBatch(feature, db);
                            if(null != progressor){
                                if (progressor.isCanceled()) {
                                    layer.save();
                                    return;
                                }
                                progressor.setValue(streamSize - in.available());
                                progressor.setMessage(layer.getContext().getString(R.string.process_features) + ": " + counter++);
                            }
                        }
                    }
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.close();

        //if(null != db)
        //   db.close(); // return pragma to init
        layer.save();
    }

    private static Feature readGeoJSONFeature(JsonReader reader, VectorLayer layer, boolean isWGS84) throws IOException {
        Feature feature;
        if(layer.getFields() != null && !layer.getFields().isEmpty())
            feature = new Feature(Constants.NOT_FOUND, layer.getFields());
        else
            feature = new Feature();

        int crs = isWGS84 ? GeoConstants.CRS_WGS84 : GeoConstants.CRS_WEB_MERCATOR;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(GeoConstants.GEOJSON_PROPERTIES)) {
                readGeoJSONFeatureProperties(reader, layer, feature);
            } else if (name.equals(GeoConstants.GEOJSON_GEOMETRY)) {
                GeoGeometry geometry = GeoGeometryFactory.fromJsonStream(reader, crs);
                if (null != geometry) {
                    if (isWGS84) {
                        geometry.setCRS(GeoConstants.CRS_WGS84);
                        geometry.project(GeoConstants.CRS_WEB_MERCATOR);
                    }

                    feature.setGeometry(geometry);
                }
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return feature;
    }


    private static int getOrCreateField(String name, int type, VectorLayer layer, Feature feature) {
        boolean isFieldsFilled = layer.getFields() != null && !layer.getFields().isEmpty();
        if (isFieldsFilled) {
            int fieldIndex = feature.getFieldValueIndex(name);
            if(Constants.NOT_FOUND == fieldIndex){
                Field field = new Field(type, name, name);
                layer.createField(field);
                feature.getFields().add(field);
            }
        }
        else {
            Field field = new Field(GeoConstants.FTString, name, name);
            feature.getFields().add(field);
        }

        return feature.getFieldValueIndex(name);
    }

    private static void readGeoJSONFeatureProperties(JsonReader reader, VectorLayer layer, Feature feature) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = LayerUtil.normalizeFieldName(reader.nextName());
            if (!LayerUtil.isFieldNameValid(name)) {
                reader.skipValue();
            } else {
                JsonToken jsonToken = reader.peek();
                if (jsonToken == JsonToken.STRING) {
                    String value = reader.nextString();
                    int fieldIndex = getOrCreateField(name, GeoConstants.FTString, layer, feature);
                    if (fieldIndex < 0) {
                        continue;
                    }

                    int fieldType = feature.getFields().get(fieldIndex).getType();
                    if (fieldType == GeoConstants.FTDate || fieldType == GeoConstants.FTDateTime || fieldType == GeoConstants.FTTime) {
                        feature.setFieldValue(fieldIndex, parseDateTime(value, fieldType));
                    } else {
                        feature.setFieldValue(fieldIndex, value);
                    }
                }
                else if (jsonToken == JsonToken.BOOLEAN) {
                    boolean value = reader.nextBoolean();
                    int fieldIndex = getOrCreateField(name, GeoConstants.FTInteger, layer, feature);
                    if (fieldIndex < 0) {
                        continue;
                    }

                    feature.setFieldValue(name, value ? 1 : 0);
                } else if (jsonToken == JsonToken.NUMBER) {
                    String value = reader.nextString();
                    int fieldIndex;
                    Object number = parseNumber(value);
                    if (null != number) {
                        if (number instanceof Double)
                            fieldIndex = getOrCreateField(name, GeoConstants.FTReal, layer, feature);
                        else if (number instanceof Long)
                            fieldIndex = getOrCreateField(name, GeoConstants.FTLong, layer, feature);
                        else
                            fieldIndex = getOrCreateField(name, GeoConstants.FTInteger, layer, feature);

                        if (fieldIndex < 0) {
                            continue;
                        }

                        feature.setFieldValue(name, number);
                    }
                } else {
                    reader.skipValue();
                }
            }
        }
        reader.endObject();
    }

    public static boolean readGeoJSONCRS(InputStream is, Context context) throws IOException, NGException {
        JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
        boolean isWGS = true;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if(name.equals(GeoConstants.GEOJSON_CRS)) {
                reader.beginObject();
                while (reader.hasNext()) {
                    name = reader.nextName();
                    if(name.equals(GeoConstants.GEOJSON_PROPERTIES)) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String subname = reader.nextName();
                            if(subname.equals(GeoConstants.GEOJSON_NAME)){
                                String val = reader.nextString();
                                isWGS = checkCRSSupportAndWGS(val, context);
                            }
                            else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.close();
        return isWGS;
    }


    public static boolean isGeoJsonHasFeatures(File path) throws IOException {
        FileInputStream inputStream = new FileInputStream(path);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        char[] buffer = new char[Constants.IO_BUFFER_SIZE];
        inputStreamReader.read(buffer, 0, Constants.IO_BUFFER_SIZE);
        inputStream.close();

        String testString = new String(buffer);

        return testString.contains("\"" + GeoConstants.GEOJSON_TYPE_FEATURES + "\"");
    }

}
