package com.nextgis.maplib.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.util.JsonReader;
import android.util.JsonToken;

import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.VectorLayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Class to store features to Vector layer
 */
public class GeoJSONUtil {

    public static void createLayerFromGeoJSON(VectorLayer layer, JSONObject geoJSONObject, IProgressor progressor) throws NGException, JSONException, SQLiteException {
        if(null != progressor){
            progressor.setIndeterminate(true);
            progressor.setMessage(layer.getContext().getString(R.string.start_fill_layer) + " " + layer.getName());
        }

        //check crs
        boolean isWGS84 = true; //if no crs tag - WGS84 CRS
        if (geoJSONObject.has(GeoConstants.GEOJSON_CRS)) {
            JSONObject crsJSONObject = geoJSONObject.getJSONObject(GeoConstants.GEOJSON_CRS);
            //the link is unsupported yet.
            if (!crsJSONObject.getString(GeoConstants.GEOJSON_TYPE).equals(GeoConstants.GEOJSON_NAME)) {
                throw new NGException(layer.getContext().getString(R.string.error_crs_unsupported));
            }
            JSONObject crsPropertiesJSONObject =
                    crsJSONObject.getJSONObject(GeoConstants.GEOJSON_PROPERTIES);
            String crsName = crsPropertiesJSONObject.getString(GeoConstants.GEOJSON_NAME);
            isWGS84 = checkCRSSupportAndWGS(crsName, layer.getContext());
        }

        //load contents to memory and reproject if needed
        JSONArray geoJSONFeatures = geoJSONObject.getJSONArray(GeoConstants.GEOJSON_TYPE_FEATURES);
        if (0 == geoJSONFeatures.length()) {
            throw new NGException(layer.getContext().getString(R.string.error_empty_dataset));
        }

        List<Feature> features = new LinkedList<>();
        List<Field> fields = new LinkedList<>();

        if(null != progressor){
            progressor.setIndeterminate(false);
            progressor.setMax(geoJSONFeatures.length());
        }

        int geometryType = GeoConstants.GTNone;
        for (int i = 0; i < geoJSONFeatures.length(); i++) {

            if(null != progressor){
                if(progressor.isCanceled())
                    break;
                progressor.setValue(i);
            }
            JSONObject jsonFeature = geoJSONFeatures.getJSONObject(i);
            //get geometry
            JSONObject jsonGeometry = jsonFeature.getJSONObject(GeoConstants.GEOJSON_GEOMETRY);
            GeoGeometry geometry = GeoGeometryFactory.fromJson(jsonGeometry);
            if (geometryType == GeoConstants.GTNone) {
                geometryType = geometry.getType();
            } else if (geometryType != geometry.getType()) {
                //skip different geometry type
                continue;
            }

            //reproject if needed
            if (isWGS84) {
                geometry.setCRS(GeoConstants.CRS_WGS84);
                geometry.project(GeoConstants.CRS_WEB_MERCATOR);
            } else {
                geometry.setCRS(GeoConstants.CRS_WEB_MERCATOR);
            }

            int nId = i;
            if (jsonFeature.has(GeoConstants.GEOJSON_ID)) {
                nId = jsonFeature.optInt(GeoConstants.GEOJSON_ID, nId);
            }
            Feature feature = new Feature(nId, fields); // ID == i
            feature.setGeometry(geometry);

            //normalize attributes
            JSONObject jsonAttributes = jsonFeature.getJSONObject(GeoConstants.GEOJSON_PROPERTIES);
            Iterator<String> iter = jsonAttributes.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                Object value = jsonAttributes.get(key);
                int nType = Constants.NOT_FOUND;
                //check type
                if (value instanceof Integer || value instanceof Long) {
                    nType = GeoConstants.FTInteger;
                } else if (value instanceof Double || value instanceof Float) {
                    nType = GeoConstants.FTReal;
                } else if (value instanceof String) {
                    nType = GeoConstants.FTString;
                } else if (value instanceof JSONObject) {
                    nType = Constants.NOT_FOUND;
                    //the some list - need to check it type FTIntegerList, FTRealList, FTStringList
                }

                String fieldName = LayerUtil.normalizeFieldName(key);

                if (nType != Constants.NOT_FOUND) {
                    int fieldIndex = Constants.NOT_FOUND;
                    for (int j = 0; j < fields.size(); j++) {
                        if (fields.get(j).getName().equals(fieldName)) {
                            fieldIndex = j;
                            break;
                        }
                    }
                    if (fieldIndex == Constants.NOT_FOUND) { //add new field
                        Field field = new Field(nType, fieldName, fieldName);
                        fieldIndex = fields.size();
                        fields.add(field);
                    }
                    feature.setFieldValue(fieldIndex, value);
                }
            }
            features.add(feature);
        }

        layer.create(geometryType, fields);

        if(null != progressor){
            progressor.setIndeterminate(false);
            progressor.setMax(features.size());
        }

        int counter = 0;
        for(Feature feature : features){
            layer.createFeature(feature);
            if(null != progressor){
                if(progressor.isCanceled())
                    break;
                progressor.setValue(counter++);
            }
        }

        layer.notifyLayerChanged();
    }

    /**
     * Check if provided name support and throw NGWException if not. Now support EPSG 3857 and 4326 only.
     * @param crsName Spatial reference name
     * @param context Context to get error string
     * @return true if provided string is WGS84 (4326) or false if Web Mercator (3857)/
     * @throws NGException
     */
    public static boolean checkCRSSupportAndWGS(String crsName, Context context) throws NGException {
        boolean isWGS84 = true;
        switch (crsName) {
            case "urn:ogc:def:crs:OGC:1.3:CRS84":  // WGS84
                isWGS84 = true;
                break;
            case "urn:ogc:def:crs:EPSG::3857":
            case "EPSG:3857":  //Web Mercator
                isWGS84 = false;
                break;
            default:
                throw new NGException(context.getString(R.string.error_crs_unsupported));
        }
        return isWGS84;
    }

    public static void fillLayerFromGeoJSON(VectorLayer layer, JSONObject geoJSONObject, int srs, IProgressor progressor) throws NGException, JSONException, SQLiteException {
        if(null != progressor){
            progressor.setIndeterminate(false);
            progressor.setMessage(layer.getContext().getString(R.string.start_fill_layer) + " " + layer.getName());
        }
        //check crs
        boolean isWGS84 = srs == GeoConstants.CRS_WGS84;
        if (!isWGS84 && srs != GeoConstants.CRS_WEB_MERCATOR) {
            throw new NGException(layer.getContext().getString(R.string.error_crs_unsupported));
        }

        //load contents to memory and reproject if needed
        JSONArray geoJSONFeatures = geoJSONObject.getJSONArray(GeoConstants.GEOJSON_TYPE_FEATURES);
        if (0 == geoJSONFeatures.length()) {
            throw new NGException(layer.getContext().getString(R.string.error_empty_dataset));
        }

        List<Feature> features = new LinkedList<>();
        List<Field> fields = layer.getFields();

        if(null != progressor){
            progressor.setMax(geoJSONFeatures.length());
            progressor.setValue(0);
        }

        for (int i = 0; i < geoJSONFeatures.length(); i++) {
            if(null != progressor){
                if(progressor.isCanceled())
                    break;
                progressor.setValue(i);
            }

            JSONObject jsonFeature = geoJSONFeatures.getJSONObject(i);
            //get geometry
            JSONObject jsonGeometry = jsonFeature.getJSONObject(GeoConstants.GEOJSON_GEOMETRY);
            GeoGeometry geometry = GeoGeometryFactory.fromJson(jsonGeometry);
            if (layer.getGeometryType() != geometry.getType()) {
                //skip different geometry type
                continue;
            }

            //reproject if needed
            if (isWGS84) {
                geometry.setCRS(GeoConstants.CRS_WGS84);
                geometry.project(GeoConstants.CRS_WEB_MERCATOR);
            } else {
                geometry.setCRS(GeoConstants.CRS_WEB_MERCATOR);
            }

            int nId = i;
            if (jsonFeature.has(GeoConstants.GEOJSON_ID)) {
                nId = jsonFeature.optInt(GeoConstants.GEOJSON_ID, nId);
            }

            Feature feature = new Feature(nId, fields); // ID == i
            feature.setGeometry(geometry);

            //normalize attributes
            JSONObject jsonAttributes = jsonFeature.getJSONObject(GeoConstants.GEOJSON_PROPERTIES);
            Iterator<String> iter = jsonAttributes.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                Object value = jsonAttributes.get(key);
                String fieldName = LayerUtil.normalizeFieldName(key);

                int fieldIndex = Constants.NOT_FOUND;
                for (int j = 0; j < fields.size(); j++) {
                    if (fields.get(j).getName().equals(fieldName)) {
                        fieldIndex = j;
                        break;
                    }
                }

                if (fieldIndex != Constants.NOT_FOUND) {
                    value = parseDateTime(value, fields.get(fieldIndex).getType());
                    feature.setFieldValue(fieldIndex, value);
                }
            }
            features.add(feature);
        }

        if(null != progressor){
            progressor.setMax(features.size());
            progressor.setValue(0);
        }

        int counter = 0;
        for(Feature feature : features){
            layer.createFeature(feature);
            if(null != progressor){
                if (progressor.isCanceled())
                    break;
                progressor.setValue(counter++);
            }
        }

        layer.notifyLayerChanged();
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void fillLayerFromGeoJSONStream(VectorLayer layer, InputStream in, int srs, IProgressor progressor) throws IOException, NGException {
        int streamSize = in.available();
        if(null != progressor){
            progressor.setIndeterminate(false);
            progressor.setMax(streamSize);
            progressor.setMessage(layer.getContext().getString(R.string.start_fill_layer) + " " + layer.getName());
        }

        SQLiteDatabase db = null;
        if(layer.getFields() != null && layer.getFields().isEmpty()){
            db = getDbForLayer(layer);
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
                    if(null != feature) {
                        if(layer.getFields() != null && !layer.getFields().isEmpty()){
                            layer.create(feature.getGeometry().getType(), feature.getFields());
                            db = getDbForLayer(layer);
                        }
                        if(feature.getGeometry() != null) {
                            layer.createFeatureBatch(feature, db);
                            if(null != progressor){
                                if (progressor.isCanceled()) {
                                    layer.save();
                                    return;
                                }
                                progressor.setValue(streamSize - in.available());
                                progressor.setMessage(layer.getContext().getString(R.string.proceed_features) + ": " + counter++);
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
        layer.save();
    }

    protected static SQLiteDatabase getDbForLayer(final VectorLayer layer){
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(false);
        // speedup writing
        db.rawQuery("PRAGMA synchronous=OFF", null);
        db.rawQuery("PRAGMA locking_mode=EXCLUSIVE", null);
        db.rawQuery("PRAGMA journal_mode=OFF", null);
        db.rawQuery("PRAGMA count_changes=OFF", null);
        db.rawQuery("PRAGMA cache_size=15000", null);

        return db;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void createLayerFromGeoJSONStream(VectorLayer layer, InputStream in, IProgressor progressor) throws IOException, NGException {
        int streamSize = in.available();
        if(null != progressor){
            progressor.setIndeterminate(false);
            progressor.setMax(streamSize);
            progressor.setMessage(layer.getContext().getString(R.string.start_fill_layer) + " " + layer.getName());
        }

        SQLiteDatabase db = null;
        if(layer.getFields() != null && layer.getFields().isEmpty()){
            db = getDbForLayer(layer);
        }

        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        boolean isWGS84 = true;
        long counter = 0;
        reader.beginObject();


        while (reader.hasNext()) {
            String name = reader.nextName();
            if(name.equals(GeoConstants.GEOJSON_CRS)) {
                isWGS84 = readGeoJSONCRS(reader, layer.getContext());
            }
            else if(name.equals(GeoConstants.GEOJSON_TYPE_FEATURES)){
                reader.beginArray();
                while (reader.hasNext()) {
                    Feature feature = readGeoJSONFeature(reader, layer, isWGS84);
                    if(null != feature) {
                        if(layer.getFields() == null || layer.getFields().isEmpty()){
                            layer.create(feature.getGeometry().getType(), feature.getFields());
                            db = getDbForLayer(layer);
                        }
                        if(feature.getGeometry() != null) {
                            layer.createFeatureBatch(feature, db);
                            if(null != progressor){
                                if (progressor.isCanceled()) {
                                    layer.save();
                                    return;
                                }
                                progressor.setValue(streamSize - in.available());
                                progressor.setMessage(layer.getContext().getString(R.string.proceed_features) + ": " + counter++);
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
        layer.save();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static Feature readGeoJSONFeature(JsonReader reader, VectorLayer layer, boolean isWGS84) throws IOException {
        Feature feature;
        if(layer.getFields() != null && !layer.getFields().isEmpty())
            feature = new Feature(Constants.NOT_FOUND, layer.getFields());
        else
            feature = new Feature();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if(name.equals(GeoConstants.GEOJSON_PROPERTIES)) {
                readGeoJSONFeatureProperties(reader, layer, feature);
            }
            else if(name.equals(GeoConstants.GEOJSON_GEOMETRY)) {
                GeoGeometry geometry = readGeoJSONGeometry(reader);
                if(null != geometry) {
                    if(isWGS84) {
                        geometry.setCRS(GeoConstants.CRS_WGS84);
                        geometry.project(GeoConstants.CRS_WEB_MERCATOR);
                    }
                    feature.setGeometry(geometry);
                }
            }
            else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return feature;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static GeoGeometry readGeoJSONGeometry(JsonReader reader) throws IOException {
        return GeoGeometryFactory.fromJsonStream(reader);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static boolean readGeoJSONCRS(JsonReader reader, Context context) throws IOException, NGException {
        boolean isWGS = true;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
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
            }
            else {
                reader.skipValue();
            }
        }
        reader.endObject();
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
