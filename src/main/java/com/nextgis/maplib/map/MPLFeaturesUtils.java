package com.nextgis.maplib.map;



import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_BOTTOM;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_BOTTOM_LEFT;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_BOTTOM_RIGHT;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_LEFT;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_RIGHT;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_TOP;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_TOP_LEFT;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_TOP_RIGHT;
import static com.nextgis.maplib.util.GeoConstants.GTLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPoint;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPolygon;
import static com.nextgis.maplib.util.GeoConstants.GTPoint;
import static com.nextgis.maplib.util.GeoConstants.GTPolygon;
import static com.nextgis.maplib.util.GeoConstants.GT_RASTER_WA;
import static com.nextgis.maplib.util.GeoConstants.GT_TRACK_WA;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_NORMAL;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;

import static org.maplibre.android.style.layers.PropertyFactory.rasterBrightnessMax;
import static org.maplibre.android.style.layers.PropertyFactory.rasterContrast;
import static org.maplibre.android.style.layers.PropertyFactory.rasterOpacity;
import static org.maplibre.android.style.layers.PropertyFactory.rasterResampling;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.ITextStyle;
import com.nextgis.maplib.datasource.GeoGeometryCollection;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoLinearRing;
import com.nextgis.maplib.datasource.GeoMultiLineString;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoMultiPolygon;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;
import com.nextgis.maplib.display.FieldStyleRule;
import com.nextgis.maplib.display.RuleFeatureRenderer;
import com.nextgis.maplib.display.SimpleLineStyle;
import com.nextgis.maplib.display.SimpleMarkerStyle;
import com.nextgis.maplib.display.SimplePolygonStyle;
import com.nextgis.maplib.display.TMSRenderer;
import com.nextgis.maplib.map.MLP.LineEditClass;
import com.nextgis.maplib.map.MLP.MLGeometryEditClass;
import com.nextgis.maplib.map.MLP.MeasurmentLine;
import com.nextgis.maplib.map.MLP.MultiLineEditClass;
import com.nextgis.maplib.map.MLP.MultiPointEditClass;
import com.nextgis.maplib.map.MLP.MultiPolygonEditClass;
import com.nextgis.maplib.map.MLP.PointEditClass;
import com.nextgis.maplib.map.MLP.PolygonEditClass;
import com.nextgis.maplib.util.GeoConstants;


import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.expressions.Expression;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.FillLayer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.Property;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.PropertyValue;
import org.maplibre.android.style.layers.RasterLayer;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.android.style.sources.RasterSource;
import org.maplibre.android.style.sources.TileSet;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Geometry;
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.MultiLineString;
import org.maplibre.geojson.MultiPoint;
import org.maplibre.geojson.MultiPolygon;
import org.maplibre.geojson.Point;
import org.maplibre.geojson.Polygon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//loader for features from NG to maplibre geojson features
public class MPLFeaturesUtils {

    static public Number pointRaduis = 8;
    static public Number middleRaduis = 4;
    static public String colorLightBlue = "#03a9f4";
    static public String colorVeryLightBlue = "#A2BCF8";
    static public String colorBlue = "#0000FF";
    static public String colorRED = "#FF0000";

    static public String prop_color_fill_rule = "fillcolor";
    static public String prop_text_color = "textcolor";
    static public String prop_text_textsize = "textsize";
    static public String prop_text_textanchor = "textanchor";
    static public String prop_text_textoffsets = "textoffset";

    // common properties
    static public String prop_color_fill = "colorfill";  // fill color
    static public String prop_color_stroke = "colorstroke"; // color outline for dot and polygone
    static public String prop_size = "size"; // size for dot
    static public String prop_thinkness = "thinkness";   // thinkness for dot / line
    static public String prop_opacity = "opacity";   // thinkness for dot / line

    static public String prop_type = "filltype"; // dot (circle square ...) .  // line - dashed
    static public String prop_type2 = "filltype2"; // dot (circle square ...) .  // line - dashed

    static public String prop_featureid = "featureid";
    static public String prop_layerid = "layerid";
    static public String prop_order = "order";
    static public String namePrefix = "nglayer-";
    static public String prop_color = "color";
    static public String prop_signature_text = "signature";
    static public String prop_start_flag = "type";

    static final public String layer_namepart = "layer-";
    static final public String source_namepart = "source-";
    static final public String outline_namepart = "_outline";
    static final public String track_namepart = "track-";
    static final public String track_flags_namepart = "track-flags-";

    static final public String source_polygon_text = "-text"; // source for text part of polygon[s]
    static final public String id_name = "_id";


    public static MLGeometryEditClass createEditObject(
            int geoType,
            GeoJsonSource selectedEditedSource,
            Feature editingFeature,
            List<Feature> polygonFeatures,
            GeoJsonSource choosedSource, // source of point/ line / polygon
            GeoJsonSource vertexSource,
            GeoJsonSource markerSource,
            final String  layerPath) {

        if (geoType == GTPolygon) {
            return new PolygonEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource, layerPath);
        } else if (geoType == GeoConstants.GTPoint) {
            return new PointEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource, layerPath);
        } else if (geoType == GeoConstants.GTMultiPoint) {
            return new MultiPointEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource, layerPath);
        } else if (geoType == GeoConstants.GTLineString) {
            return new LineEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource, layerPath);
        } else if (geoType == GeoConstants.GTMultiLineString) {
            return new MultiLineEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource, layerPath);
        } else if (geoType == GeoConstants.GTMultiPolygon) {
            return new MultiPolygonEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource, layerPath);
        }
        else if (geoType == GeoConstants.GT_MEASURMENT) {
            return new MeasurmentLine(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource, layerPath);
        }
        else
            return null;
    }


    static public String getLayerSignatureField(final VectorLayer layer){
        com.nextgis.maplib.display.Style style = layer.getDefaultStyleNoExcept();
        if (style!=null){
            String styleField = ((ITextStyle)style).getField();
            return TextUtils.isEmpty(styleField)? null : styleField;
        }
        return null;
    }

    static public List<org.maplibre.geojson.Feature> createFeatureListFromTrackLayer(final TrackLayer layer) {
        Map<Integer, GeoLineString> tracks = layer.getTracks();
        List<org.maplibre.geojson.Feature> lineFeatures = new ArrayList<>();

        for (Map.Entry<Integer, GeoLineString> entry : tracks.entrySet()) {
            Integer id = entry.getKey();
            LineString lineString = getLineString(entry.getValue());
            Feature lineFeature = org.maplibre.geojson.Feature.fromGeometry(lineString);
            lineFeature.addStringProperty(prop_layerid, String.valueOf(layer.getId()));
            lineFeatures.add(lineFeature);
        }
        return lineFeatures;
    }

    static public List<org.maplibre.geojson.Feature> createFeatureListFlagsFromTrackLayer(final TrackLayer layer) {
        Map<Integer, GeoLineString> tracks = layer.getTracks();
        List<org.maplibre.geojson.Feature> pointsFeatures = new ArrayList<>();

        for (Map.Entry<Integer, GeoLineString> entry : tracks.entrySet()) {

            GeoPoint p1 = entry.getValue().getPoints().get(0);
            GeoPoint p2 = entry.getValue().getPoints().get(entry.getValue().getPoints().size() -1);

            double[] lonLat = convert3857To4326(p1.getX(), p1.getY());
            Point point1 = Point.fromLngLat(lonLat[0], lonLat[1]);
            Feature pointFeature1 = org.maplibre.geojson.Feature.fromGeometry(point1);
            pointFeature1.addBooleanProperty(prop_start_flag, true);

            double[] lonLat2 = convert3857To4326(p2.getX(), p2.getY());
            Point point2 = Point.fromLngLat(lonLat2[0], lonLat2[1]);
            Feature pointFeature2 = org.maplibre.geojson.Feature.fromGeometry(point2);
            pointFeature2.addBooleanProperty(prop_start_flag, false);

            pointsFeatures.add(pointFeature1);
            pointsFeatures.add(pointFeature2);
        }

        return pointsFeatures;
    }

    static public List<org.maplibre.geojson.Feature> createFeatureListFromLayer(final VectorLayer layer) {
        List<org.maplibre.geojson.Feature> vectorFeatures = new ArrayList<>();

        String signatureField =  getLayerSignatureField(layer);
        com.nextgis.maplib.display.Style layerStyle = layer.getDefaultStyleNoExcept();
        String styleField = ((ITextStyle) layerStyle).getField();
        String commonText = ((ITextStyle) layerStyle).getText();

        boolean needSignatures = false;
        if (layer.getRenderer() instanceof RuleFeatureRenderer ||
                !TextUtils.isEmpty(styleField) || !TextUtils.isEmpty(commonText)) {
            needSignatures = true;
        }

        if (layer.getGeometryType() == GeoConstants.GTPoint) {
            return getPointFeatures(layer,signatureField, needSignatures, commonText);
        }

        if (layer.getGeometryType() == GeoConstants.GTMultiPoint) {
            return getMultiPointFeatures(layer,signatureField, needSignatures, commonText);
        }

        if (layer.getGeometryType() == GeoConstants.GTLineString) {
            return getLineFeatures(layer,signatureField, needSignatures, commonText);
        }

        if (layer.getGeometryType() == GeoConstants.GTMultiLineString) {
            return getMultiLineFeatures(layer,signatureField, needSignatures, commonText);
        }

        if (layer.getGeometryType() == GTPolygon) {
            return getPolygonFeatures(layer,signatureField, needSignatures, commonText);
        }

        if (layer.getGeometryType() == GeoConstants.GTMultiPolygon) {
            return getMultiPolygonFeatures(layer,signatureField, needSignatures, commonText);
        }
        return vectorFeatures;
    }

    private static List<Feature> getLineFeatures(VectorLayer layer, String signatureField,
                                                 boolean needSignatures, String commonText){
        boolean ruleStyle =  layer.getRenderer() instanceof RuleFeatureRenderer;

        List<org.maplibre.geojson.Feature> lineFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        Iterator<Map.Entry<Long, com.nextgis.maplib.datasource.Feature>> iterator = features.entrySet().iterator();

        while (iterator.hasNext()){
            Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry = iterator.next();
            i++;
            Long id = entry.getKey();
            com.nextgis.maplib.datasource.Feature feature = entry.getValue();
            GeoLineString geoLineGeometry = (GeoLineString) feature.getGeometry();
            LineString lineString = getLineString(geoLineGeometry);
            Feature lineFeature = org.maplibre.geojson.Feature.fromGeometry(lineString);
            lineFeature.addStringProperty(prop_layerid, String.valueOf(layer.getId()));
            lineFeature.addStringProperty(prop_order, String.valueOf(i));
            lineFeature.addStringProperty(prop_featureid, String.valueOf(id));
            lineFeature.addStringProperty(prop_color, colorBlue);

            applyTextAndStyle(
                    layer,
                    feature,
                    lineFeature,
                    GTLineString,
                    ruleStyle,
                    needSignatures,
                    signatureField,
                    commonText  );
            lineFeatures.add(lineFeature);
            iterator.remove();
        }
        return lineFeatures;
    }

    private static List<Feature> getMultiLineFeatures(VectorLayer layer, String signatureField,
                                                      boolean needSignatures, String commonText){

        boolean ruleStyle = false;
        if (layer.getRenderer() instanceof RuleFeatureRenderer) { // feature render
            ruleStyle = true;
        }

        List<org.maplibre.geojson.Feature> lineFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        Iterator<Map.Entry<Long, com.nextgis.maplib.datasource.Feature>> iterator = features.entrySet().iterator();

        while (iterator.hasNext()){
            Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry = iterator.next();
            List<LineString> linesArray = new ArrayList<>();
            i++;
            Long id = entry.getKey();
            com.nextgis.maplib.datasource.Feature feature = entry.getValue();
            GeoMultiLineString geoMultiLineString = (GeoMultiLineString) feature.getGeometry();
            for (int j = 0; j < geoMultiLineString.size(); j++) {
                GeoLineString geoLineString = geoMultiLineString.get(j);
                LineString lineString = getLineString(geoLineString);
                linesArray.add(lineString);
            }
            MultiLineString multiLineString = MultiLineString.fromLineStrings(linesArray);
            Feature lineFeature = Feature.fromGeometry(multiLineString);
            lineFeature.addStringProperty(prop_layerid, String.valueOf(layer.getId()));
            lineFeature.addStringProperty(prop_order, String.valueOf(i));
            lineFeature.addStringProperty(prop_featureid, String.valueOf(id));
            lineFeature.addStringProperty(prop_color, colorBlue);

            applyTextAndStyle(
                    layer,
                    feature,
                    lineFeature,
                    GTMultiLineString,
                    ruleStyle,
                    needSignatures,
                    signatureField,
                    commonText );
            lineFeatures.add(lineFeature);
            iterator.remove();
        }
        return lineFeatures;
    }


    private static List<Feature> getPointFeatures(VectorLayer layer, String signatureField,
                                                  boolean needSignatures, String commonText){
        boolean ruleStyle = layer.getRenderer() instanceof RuleFeatureRenderer;

        List<org.maplibre.geojson.Feature> pointFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        Iterator<Map.Entry<Long, com.nextgis.maplib.datasource.Feature>> iterator = features.entrySet().iterator();

        while (iterator.hasNext()){
            Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry = iterator.next();

            i++;
            Long id = entry.getKey();
            com.nextgis.maplib.datasource.Feature feature = entry.getValue();
            GeoPoint geoPointGeometry = (GeoPoint) feature.getGeometry();
            double[] lonLat = convert3857To4326(geoPointGeometry.getX(), geoPointGeometry.getY());
            Point point = Point.fromLngLat(lonLat[0], lonLat[1]);
            Feature pointFeature = org.maplibre.geojson.Feature.fromGeometry(point);
            pointFeature.addStringProperty(prop_layerid, String.valueOf(layer.getId()));
            pointFeature.addStringProperty(prop_order, String.valueOf(i));
            pointFeature.addStringProperty(prop_featureid, String.valueOf(id));

            applyTextAndStyle(
                    layer,
                    feature,
                    pointFeature,
                    GTPoint,
                    ruleStyle,
                    needSignatures,
                    signatureField,
                    commonText );

            pointFeatures.add(pointFeature);
            iterator.remove();
        }
        return pointFeatures;
    }

    private static List<Feature> getMultiPointFeatures(VectorLayer layer, String signatureField,
                                                       boolean needSignatures, String commonText) {
        boolean ruleStyle = layer.getRenderer() instanceof RuleFeatureRenderer;

        List<org.maplibre.geojson.Feature> mpointFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        Iterator<Map.Entry<Long, com.nextgis.maplib.datasource.Feature>> iterator = features.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry = iterator.next();
            i++;
            Long id = entry.getKey();
            com.nextgis.maplib.datasource.Feature feature = entry.getValue();
            GeoMultiPoint geoMultiPointtGeometry = (GeoMultiPoint) feature.getGeometry();
            List<Point> pointList = new ArrayList<>();
            for (int j = 0; j < geoMultiPointtGeometry.size(); j++) {
                GeoPoint geoPointGeometry = (GeoPoint) geoMultiPointtGeometry.get(j);
                double[] lonLat = convert3857To4326(geoPointGeometry.getX(), geoPointGeometry.getY());
                Point point = Point.fromLngLat(lonLat[0], lonLat[1]);
                pointList.add(point);
            }
            MultiPoint multiPoint = MultiPoint.fromLngLats(pointList);
            Feature mpointFeature = org.maplibre.geojson.Feature.fromGeometry(multiPoint);
            mpointFeature.addStringProperty(prop_layerid, String.valueOf(layer.getId()));
            mpointFeature.addStringProperty(prop_order, String.valueOf(i));
            mpointFeature.addStringProperty(prop_featureid, String.valueOf(id));

            applyTextAndStyle(
                    layer,
                    feature,
                    mpointFeature,
                    GTMultiPoint,
                    ruleStyle,
                    needSignatures,
                    signatureField,
                    commonText
            );

            mpointFeatures.add(mpointFeature);
            iterator.remove();
        }
        return mpointFeatures;
    }


    static public List<Feature> getPolygonFeatures(final VectorLayer layer, String signatureField,
                                                   boolean needSignatures, String commonText){
        boolean ruleStyle = layer.getRenderer() instanceof RuleFeatureRenderer;

        List<org.maplibre.geojson.Feature> vectorFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        Iterator<Map.Entry<Long, com.nextgis.maplib.datasource.Feature>> iterator = features.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry = iterator.next();
            i++;
            Long id = entry.getKey();
            com.nextgis.maplib.datasource.Feature feature = entry.getValue();

            GeoPolygon geoPolygonGeometry = (GeoPolygon) feature.getGeometry();
            org.maplibre.geojson.Feature polyFeature = getFeatureFromNGFeaturePolygon(geoPolygonGeometry);
            polyFeature.addStringProperty(prop_layerid, String.valueOf(layer.getId()));
            polyFeature.addStringProperty(prop_order, String.valueOf(i));
            polyFeature.addStringProperty(prop_featureid, String.valueOf(id));

            applyTextAndStyle(
                    layer,
                    feature,
                    polyFeature,
                    GTPolygon,
                    ruleStyle,
                    needSignatures,
                    signatureField,
                    commonText );

            vectorFeatures.add(polyFeature);
            iterator.remove();  // free immediately
        }
        return vectorFeatures;
    }

    static public List<Feature> getMultiPolygonFeatures(final VectorLayer layer, String signatureField,
                                                        boolean needSignatures, String commonText){

        boolean ruleStyle = layer.getRenderer() instanceof RuleFeatureRenderer;

        List<org.maplibre.geojson.Feature> vectorFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        Iterator<Map.Entry<Long, com.nextgis.maplib.datasource.Feature>> iterator = features.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry = iterator.next();
            i++;
            Long id = entry.getKey();
            com.nextgis.maplib.datasource.Feature feature = entry.getValue();
            GeoGeometryCollection geoGeometryCollection = (GeoGeometryCollection) feature.getGeometry();
            ArrayList<Polygon> polygons = new ArrayList<>();
            for (int j = 0; j < geoGeometryCollection.size(); j++) {
                GeoPolygon polygonNG = (GeoPolygon) geoGeometryCollection.getGeometry(j);
                Polygon polygonML = getPolygonFromNGFeaturePolygon(polygonNG);
                polygons.add(polygonML);
            }
            MultiPolygon multiPolygon = MultiPolygon.fromPolygons(polygons);
            org.maplibre.geojson.Feature mpolyFeature = Feature.fromGeometry(multiPolygon);
            mpolyFeature.addStringProperty(prop_layerid, String.valueOf(layer.getId()));
            mpolyFeature.addStringProperty(prop_order, String.valueOf(i));
            mpolyFeature.addStringProperty(prop_featureid, String.valueOf(id));
            if (signatureField != null) {
                mpolyFeature.addStringProperty(prop_signature_text, getSpaceCorrectedText(entry.getValue().getFieldValueAsString(signatureField)));
            }

            applyTextAndStyle(
                    layer,
                    feature,
                    mpolyFeature,
                    GTMultiPolygon,
                    ruleStyle,
                    needSignatures,
                    signatureField,
                    commonText);

            vectorFeatures.add(mpolyFeature);
            iterator.remove();
        }
        return vectorFeatures;
    }

    public static void applyTextAndStyle(
            VectorLayer layer,
            com.nextgis.maplib.datasource.Feature ngFeature,
            Feature feature,
            int geoType,
            boolean ruleStyle,
            boolean needSignatures,
            String signatureField,
            String commonText) {
        if (ruleStyle) {
            applyRuleStyleInternal(
                            layer,
                            ngFeature,
                            feature,
                            geoType,
                            commonText );
            return;
        }

        if (needSignatures) {
            if (signatureField != null) {
                String text = "_id".equals(signatureField)
                        ? String.valueOf(ngFeature.getId())
                        : getNullableValue(ngFeature, signatureField);
                feature.addStringProperty(
                        prop_signature_text,
                        getSpaceCorrectedText(text));
            } else if (!TextUtils.isEmpty(commonText)) {
                feature.addStringProperty(
                        prop_signature_text,
                        getSpaceCorrectedText(commonText));
            }
        }
    }

    private static void applyRuleStyleInternal(
            VectorLayer layer,
            com.nextgis.maplib.datasource.Feature ngFeature,
            Feature feature,
            int geoType,
            String commonText  ) {
        RuleFeatureRenderer rfr =  (RuleFeatureRenderer) layer.getRenderer();

        FieldStyleRule fsr =  (FieldStyleRule) rfr.getStyleRule();

        String key = fsr.getKey();
        boolean isIdKey = "_id".equals(key);

        String keyValue = isIdKey   ? String.valueOf(ngFeature.getId()): getNullableValue(ngFeature, key);
        com.nextgis.maplib.display.Style style = fsr.getStyleRules().get(keyValue);

        if (style != null) {
            String ruleCommonText = style.getText();
            applyText(style, ngFeature, feature, ruleCommonText != null? ruleCommonText : commonText);
            applyGeometrySpecificStyle(style, feature, geoType);
        } else {
            applyCommonStyleText(rfr, ngFeature, feature, commonText);
            clearGeometrySpecificStyle(feature,geoType);
        }
    }

    private static void applyText(
            com.nextgis.maplib.display.Style style,
            com.nextgis.maplib.datasource.Feature ngFeature,
            Feature feature,
            String commonText){
        String field = ((ITextStyle) style).getField();
        if (!TextUtils.isEmpty(field)) {
            String text = "_id".equals(field)? String.valueOf(ngFeature.getId()): getNullableValue(ngFeature, field);
            feature.addStringProperty(prop_signature_text,getSpaceCorrectedText(text));

        } else if (!TextUtils.isEmpty(commonText)){
            feature.addStringProperty(prop_signature_text,getSpaceCorrectedText(commonText));
        }
    }


    private static void applyCommonStyleText(
            RuleFeatureRenderer rfr,
            com.nextgis.maplib.datasource.Feature ngFeature,
            Feature feature,
            String commonText  ) {
        String styleField = rfr.getStyle().getField();

        if (!TextUtils.isEmpty(styleField)) {
            String text = "_id".equals(styleField)
                    ? String.valueOf(ngFeature.getId())
                    : getNullableValue(ngFeature, styleField);

            feature.addStringProperty(
                    prop_signature_text,
                    getSpaceCorrectedText(text) );
        } else if (!TextUtils.isEmpty(commonText)) {
            feature.addStringProperty(
                    prop_signature_text,
                    getSpaceCorrectedText(commonText));
        }
    }

    private static void applyGeometrySpecificStyle(
            com.nextgis.maplib.display.Style style,
            Feature feature,
            int geoType ) {
        switch (geoType) {

            case GTPoint:
            case GTMultiPoint:
                SimpleMarkerStyle ms = (SimpleMarkerStyle) style;

                feature.addStringProperty(prop_text_color, getColorName(ms.getTextColor()) );

                float textSize = (ms.getTextSize() + 3) * 3;
                feature.addNumberProperty( prop_text_textsize,textSize );

                int align = ms.getTextAlignment();
                feature.addStringProperty(prop_text_textanchor,getTextAnchor(align));

                Float[] offsets = getTextAnchorOffsets(align, textSize);
                JsonArray arr = new JsonArray();
                arr.add(offsets[0]);
                arr.add(offsets[1]);
                feature.addProperty(prop_text_textoffsets, arr);

                feature.addStringProperty(prop_color_fill, getColorName(style.getColor()));
                feature.addStringProperty(prop_color_stroke, getColorName(style.getOutColor()));
                feature.addNumberProperty(prop_size, getMPLThinkness(ms.getSize()));
                break;
            case GTLineString:
            case GTMultiLineString:
                feature.addStringProperty(
                        prop_color_fill,
                        getColorName(style.getColor()));
                feature.addStringProperty(
                        prop_text_color,
                        getColorName(style.getOutColor()));
                feature.addNumberProperty(
                        prop_thinkness,
                        getMPLThinkness(style.getWidth()));
                if (style instanceof SimpleLineStyle &&
                        ((SimpleLineStyle) style).getType() == 2) {
                    feature.addNumberProperty(
                            prop_type,
                            ((SimpleLineStyle) style).getType());
                }
                break;

            case GTPolygon:
            case GTMultiPolygon:
                feature.addStringProperty(
                        prop_color_fill_rule,
                        getColorName(style.getColor()));
                feature.addStringProperty(
                        prop_color_stroke,
                        getColorName(style.getOutColor()));
                feature.addNumberProperty(
                        prop_thinkness,
                        getMPLThinkness(style.getWidth()));

                if (style instanceof SimplePolygonStyle) {
                    feature.addNumberProperty(
                            prop_opacity,
                            ((SimplePolygonStyle) style).isFill() ? 0.5f : 0f);
                }
                break;
        }
    }


    private static void clearGeometrySpecificStyle(Feature feature,int geoType ) {
        switch (geoType) {

            case GTPoint:
            case GTMultiPoint:
                feature.removeProperty(prop_text_color);
                feature.removeProperty( prop_text_textsize);
                feature.removeProperty(prop_text_textanchor);
                feature.removeProperty(prop_text_textoffsets);
                feature.removeProperty(prop_color_fill);
                feature.removeProperty(prop_color_stroke);
                feature.removeProperty(prop_size);
                break;
            case GTLineString:
            case GTMultiLineString:
                feature.removeProperty(prop_color_fill);
                feature.removeProperty(prop_text_color);
                feature.removeProperty(prop_thinkness);
                feature.removeProperty(prop_type);
                break;

            case GTPolygon:
            case GTMultiPolygon:
                feature.removeProperty(prop_color_fill_rule);
                feature.removeProperty(prop_color_stroke);
                feature.removeProperty(prop_thinkness);
                feature.removeProperty(prop_opacity);
                break;
        }
    }


    static public LineString getLineString(GeoLineString geoLineGeometry) {
        List<Point> pointList = new ArrayList<>();
        for (int j = 0; j < geoLineGeometry.getPointCount(); j++) {
            GeoPoint geoPointGeometry = (GeoPoint) geoLineGeometry.getPoint(j);
            double[] lonLat = convert3857To4326(geoPointGeometry.getX(), geoPointGeometry.getY());
            Point point = Point.fromLngLat(lonLat[0], lonLat[1]);
            pointList.add(point);
        }
        return LineString.fromLngLats(pointList);
    }


    public static org.maplibre.geojson.Feature getFeatureFromNGFeatureMultiLine(GeoMultiLineString geoLineGeometry) {
        List<List<Point>> mline = new ArrayList<>();
        for (int j = 0; j < geoLineGeometry.size(); j++) {
            GeoLineString lineitem = geoLineGeometry.get(j);
            List<Point> points = new ArrayList<>();
            for (GeoPoint item : lineitem.getPoints()) {
                double[] lonLat = convert3857To4326(item.getX(), item.getY());
                points.add(Point.fromLngLat(lonLat[0], lonLat[1]));
            }
            mline.add(points);
        }
        return org.maplibre.geojson.Feature.fromGeometry(org.maplibre.geojson.MultiLineString.fromLngLats(mline));
    }

    public static org.maplibre.geojson.Feature getFeatureFromNGFeatureLine(GeoLineString geoLineGeometry) {
        List<Point> points = new ArrayList<>();
        for (GeoPoint item : geoLineGeometry.getPoints()) {
            double[] lonLat = convert3857To4326(item.getX(), item.getY());
            points.add(Point.fromLngLat(lonLat[0], lonLat[1]));
        }
        return org.maplibre.geojson.Feature.fromGeometry(org.maplibre.geojson.LineString.fromLngLats(points));
    }

    public static org.maplibre.geojson.Feature getFeatureFromNGFeaturePolygon(GeoPolygon geoPolygonGeometry) {
        List<List<Point>> points = new ArrayList<>();
        List<Point> outerRing = new ArrayList<>();
        for (GeoPoint item : geoPolygonGeometry.getOuterRing().getPoints()) {
            double[] lonLat = convert3857To4326(item.getX(), item.getY());
            outerRing.add(Point.fromLngLat(lonLat[0], lonLat[1]));
        }
        points.add(outerRing);
        for (GeoLinearRing innerRing : geoPolygonGeometry.getInnerRings()) {
            List<Point> newInnerRing = new ArrayList<>();
            for (GeoPoint itemPoint : innerRing.getPoints()) {
                double[] lonLat = convert3857To4326(itemPoint.getX(), itemPoint.getY());
                newInnerRing.add(Point.fromLngLat(lonLat[0], lonLat[1]));
            }
            points.add(newInnerRing);
        }
        return org.maplibre.geojson.Feature.fromGeometry(org.maplibre.geojson.Polygon.fromLngLats(points));
    }

    public static org.maplibre.geojson.Feature getFeatureFromNGFeatureMultiPolygon(GeoMultiPolygon geoPolygonGeometry) {
        ArrayList<Polygon> polygons = new ArrayList<>();
        for (int j = 0; j < geoPolygonGeometry.size(); j++) {
            GeoPolygon polygonNG = (GeoPolygon) geoPolygonGeometry.getGeometry(j);
            Polygon polygonML = getPolygonFromNGFeaturePolygon(polygonNG);
            polygons.add(polygonML);
        }
        MultiPolygon multiPolygon = MultiPolygon.fromPolygons(polygons);
        return org.maplibre.geojson.Feature.fromGeometry(multiPolygon);
    }

    public static org.maplibre.geojson.Feature getFeatureFromNGFeaturePoint(GeoPoint geoPointGeometry) {
        double[] lonLat = convert3857To4326(geoPointGeometry.getX(), geoPointGeometry.getY());
        Point point = Point.fromLngLat(lonLat[0], lonLat[1]);
        return org.maplibre.geojson.Feature.fromGeometry(point);
    }

    public static org.maplibre.geojson.Feature getFeatureFromNGFeatureMultiPoint(GeoMultiPoint geoGeometry) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < geoGeometry.size(); i++) {
            GeoPoint geoPoint = geoGeometry.get(i);
            double[] lonLat = convert3857To4326(geoPoint.getX(), geoPoint.getY());
            Point point = Point.fromLngLat(lonLat[0], lonLat[1]);
            points.add(point);
        }
        MultiPoint multiPoint = MultiPoint.fromLngLats(points);
        return org.maplibre.geojson.Feature.fromGeometry(multiPoint);
    }

    public static org.maplibre.geojson.Polygon getPolygonFromNGFeaturePolygon(GeoPolygon geoPolygonGeometry) {
        List<List<Point>> points = new ArrayList<>();
        List<Point> outerRing = new ArrayList<>();
        for (GeoPoint item : geoPolygonGeometry.getOuterRing().getPoints()) {
            double[] lonLat = convert3857To4326(item.getX(), item.getY());
            outerRing.add(Point.fromLngLat(lonLat[0], lonLat[1]));
        }
        points.add(outerRing);
        for (GeoLinearRing innerRing : geoPolygonGeometry.getInnerRings()) {
            List<Point> newInnerRing = new ArrayList<>();
            for (GeoPoint itemPoint : innerRing.getPoints()) {
                double[] lonLat = convert3857To4326(itemPoint.getX(), itemPoint.getY());
                newInnerRing.add(Point.fromLngLat(lonLat[0], lonLat[1]));
            }
            points.add(newInnerRing);
        }
        return org.maplibre.geojson.Polygon.fromLngLats(points);
    }

    static public double[] convert3857To4326(double x, double y) {
        double lon = x * 180 / 20037508.34;
        double lat = Math.toDegrees(Math.atan(Math.sinh(y * Math.PI / 20037508.34)));
        return new double[]{lon, lat};
    }

    static public double[] convert4326To3857(double lon, double lat) {
        double x = lon * 20037508.34 / 180;
        double y = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(lat) / 2)) * 20037508.34 / Math.PI;
        return new double[]{x, y};
    }

    static GeoPoint geoPointFromMaplibrePoint(Point point){
        double[] centerPoints = convert4326To3857(point.longitude(), point.latitude());
        return new GeoPoint(centerPoints[0], centerPoints[1]);
    }

    static public GeoPoint geoPointFromLatLng(LatLng latLng){
        double[] centerPoints = convert4326To3857(latLng.getLongitude(), latLng.getLatitude());
        return new GeoPoint(centerPoints[0], centerPoints[1]);
    }

    static public  LatLng latLngPointFromGeoPoint(GeoPoint gePoint){
        double[] lonLat = convert3857To4326(gePoint.getX(), gePoint.getY());
        return new LatLng(lonLat[1], lonLat[0]);
    }


    static public void createSourceForLayer(int layerId,
                                            int layerType,
                                            final List<org.maplibre.geojson.Feature> layerFeatures,
                                            final Style style,
                                            Map<String, GeoJsonSource> sourceHashMap,
                                            Map<Integer, String> rasterLayersURL,
                                            Map<Integer, Integer> rasterLayersTmsTypeMap,
                                            String layerPath,
                                            boolean forceCreate) {
        if (layerType == GT_TRACK_WA){
            return;
        }

        if (layerType == GT_RASTER_WA){
                RasterSource rasterSource = (RasterSource) style.getSource(layerPath);
                if (rasterSource != null && rasterSource.getUrl()!= null &&  !rasterSource.getUrl().equals(rasterLayersURL.get(layerId))){
                    style.removeSource(layerPath);
                    rasterSource = null;
                }
                if (rasterSource == null || forceCreate) {
                    TileSet tileSet = new TileSet(
                            "tileset",
                            rasterLayersURL.get(layerId));
                    Integer tileTmsType =rasterLayersTmsTypeMap.get(layerId);
                    if ( tileTmsType != null && tileTmsType != -1){
                        if (tileTmsType == TMSTYPE_NORMAL) {
                            tileSet.setScheme( "tms");
                        }
                        if (tileTmsType == TMSTYPE_OSM) {
                            tileSet.setScheme("xyz");
                        }
                    }
                    rasterSource = new RasterSource(layerPath,tileSet, 256 );
                    style.addSource(rasterSource);
                }
            return;
        }

        boolean addPolyTextSource = false;
        if (layerType == GTPolygon || layerType == GTMultiPolygon){
            addPolyTextSource = true;
        }

        GeoJsonSource vectorSource = (GeoJsonSource) style.getSource(layerPath);
        if (vectorSource == null) {
            vectorSource = new GeoJsonSource(layerPath, FeatureCollection.fromFeatures(layerFeatures));
            style.addSource(vectorSource);
        }
        else
            vectorSource.setGeoJson(FeatureCollection.fromFeatures(layerFeatures));

        sourceHashMap.put(layerPath, vectorSource);

        if (addPolyTextSource){

            List<Feature> points =  convertToPointFeatures(layerFeatures);

            GeoJsonSource vectorTextSource = (GeoJsonSource) style.getSource(layerPath + source_polygon_text);
            if (vectorTextSource == null) {
                vectorTextSource = new GeoJsonSource(layerPath + source_polygon_text, FeatureCollection.fromFeatures(points));
                Log.d("Mbgl", "create source for: " + layerPath + source_polygon_text);
                style.addSource(vectorTextSource);

            }
            else
                vectorTextSource.setGeoJson(FeatureCollection.fromFeatures(points));
            sourceHashMap.put(layerPath + source_polygon_text, vectorTextSource);
        }
    }

    static  public List<Feature> convertToPointFeatures(List<Feature> layerFeatures) {
        List<Feature> centroidFeatures = new ArrayList<>();

        for (Feature feature : layerFeatures) {
            Geometry geometry = feature.geometry();
            Point centroid = null;

            if (geometry instanceof Polygon) {
                centroid = calculatePolygonCentroid((Polygon) geometry);
            } else if (geometry instanceof MultiPolygon) {
                centroid = calculateMultiPolygonCentroid((MultiPolygon) geometry);
            }

            if (centroid != null) {
                Feature centroidFeature = Feature.fromGeometry(
                        centroid,
                        feature.properties()
                );
                centroidFeatures.add(centroidFeature);
            }
        }

        return centroidFeatures;
    }

    static private Point calculatePolygonCentroid(Polygon polygon) {
        List<Point> points = polygon.coordinates().get(0); // external ring
        return getAveragePoint(points);
    }

    static private Point calculateMultiPolygonCentroid(MultiPolygon multiPolygon) {
        List<Point> allPoints = new ArrayList<>();

        for (List<List<Point>> polygonRings : multiPolygon.coordinates()) {
            allPoints.addAll(polygonRings.get(0)); //external ring of each poly
        }

        return getAveragePoint(allPoints);
    }

    static  private Point getAveragePoint(List<Point> points) {
        double sumLon = 0;
        double sumLat = 0;
        int count = points.size();

        for (Point point : points) {
            sumLon += point.longitude();
            sumLat += point.latitude();
        }

        return Point.fromLngLat(sumLon / count, sumLat / count);
    }


    static public org.maplibre.android.style.layers.Layer getRasterLayer(int layerId, final Style style){
        String currentNamePrefix = namePrefix;
        org.maplibre.android.style.layers.Layer rasterLayer = style.getLayer(currentNamePrefix + layer_namepart + layerId);
        return rasterLayer;
    }

    static public void createFillLayerForLayer(int layerId, int layerType,
                                               final Style style,
                                               Map<Integer,org.maplibre.android.style.layers.Layer> layersHashMap,
                                               Map<Integer,org.maplibre.android.style.layers.Layer> layersHashMap2,
                                               Map<Integer,org.maplibre.android.style.layers.Layer> symbolsLayerHashMap,
                                               @Nullable com.nextgis.maplib.display.Style layerStyle,
                                               boolean changeLayer,
                                               ILayer iLayer,
                                               String layerPath,
                                               org.maplibre.android.style.layers.Layer firstToolLayer,
                                               org.maplibre.android.style.layers.Layer signaturesRootLayer){ // layers below signaturesRootLayer
        // signatures between firstToolLayer and signaturesRootLayer
        float minZoom = -1;
        float maxZoom = -1;
        if (iLayer!= null){
            minZoom =((com.nextgis.maplib.map.Layer)iLayer).getMinZoom();
            maxZoom =((com.nextgis.maplib.map.Layer)iLayer).getMaxZoom();
        }

        String currentNamePrefix = namePrefix;

        if (layerType == GT_TRACK_WA){
            return;
        }

        if (layerType == GT_RASTER_WA){
            org.maplibre.android.style.layers.Layer rasterLayer = style.getLayer(currentNamePrefix + layer_namepart + layerId);

            if (rasterLayer == null){
                rasterLayer = new RasterLayer(currentNamePrefix + layer_namepart + layerId, layerPath);

                if (signaturesRootLayer != null && style.getLayer(signaturesRootLayer.getId()) != null ) {
                    style.addLayerBelow(rasterLayer, signaturesRootLayer.getId());
                }
                else {
                    style.addLayer(rasterLayer);
                }
            }
                if (minZoom!= -1)
                    rasterLayer.setMinZoom(minZoom);
                if (maxZoom!= -1)
                    rasterLayer.setMaxZoom(maxZoom + 1);

                if (iLayer != null && iLayer instanceof  TMSLayer) {
                    TMSRenderer tmsRenderer = (TMSRenderer) ((TMSLayer) iLayer).getRenderer();
                    float alpha = tmsRenderer.getAlpha() / 255.0f; // stored value 0 - 255 // need for maplibre 0 - 1
                    float contrast = (tmsRenderer.getContrast() - 1) ; //stored value 0 - 100 ,  needed -1  +1
                    float brightness = ((tmsRenderer.getBrightness()) / 255.0f) +1 ; // stored value 0  510 , need value 0  >1   1 norm
                    boolean isGray = tmsRenderer.isForceToGrayScale();

                    rasterLayer.setProperties(
                            rasterOpacity(alpha),
                            rasterContrast(contrast),
                            rasterBrightnessMax(brightness));
                }
            return;
        }

        org.maplibre.android.style.layers.Layer simbolLayer = null;
        org.maplibre.android.style.layers.Layer newLayer = null;
        org.maplibre.android.style.layers.Layer newLayer2 = null;

        String currentNamePrefixSymbol = "symbol-" +  namePrefix;

        int fistFillColor = 0;
        int outlineColor = 0;
        float alpha  = 0.5f;
        float thinkness = 3; // outline

        // dot
        float rasuis = 6; // dot
        int type = 0; // circle quadro triangle . // dash dot

        // poly
        boolean isFilled; // polygon - is filled

        int textAlignment = ALIGN_TOP;
        float textSize = 3;
        int textColor = 0;

        if (layerStyle!= null){
            fistFillColor = layerStyle.getColor();
            outlineColor = layerStyle.getOutColor();
            alpha = layerStyle.getAlpha() / 256.0f;
            thinkness = layerStyle.getWidth();

            if (layerStyle instanceof SimpleMarkerStyle){ // dots
                rasuis = ((SimpleMarkerStyle)layerStyle).getSize();
                type = ((SimpleMarkerStyle)layerStyle).getType();
                textAlignment = ((SimpleMarkerStyle) layerStyle).getTextAlignment();
                textSize = ((SimpleMarkerStyle) layerStyle).getTextSize();
                textColor = ((SimpleMarkerStyle) layerStyle).getTextColor();
            }
            if (layerStyle instanceof SimpleLineStyle){ //line
                type = ((SimpleLineStyle)layerStyle).getType();
                textColor = ( layerStyle).getOutColor();
            }
            if (layerStyle instanceof SimplePolygonStyle){ //line
                isFilled = ((SimplePolygonStyle)layerStyle ).isFill();
                if (!isFilled)
                    alpha = 0.0f;
            }
        }


        // polygon makes signature other way: - create points for polygones
        // - differ source for points () (layerPath + source_text)
        // points as center of polygons
        // SymbolLayer for (layerPath + source_text) source
        boolean isPolygon = layerType == GTPolygon || layerType == GTMultiPolygon;

        if (layerType == GeoConstants.GTPoint || layerType == GeoConstants.GTMultiPoint) {
            if (changeLayer)
                newLayer = style.getLayer(currentNamePrefix + layer_namepart + layerId);
            if (newLayer == null)
                newLayer = new CircleLayer(currentNamePrefix + layer_namepart + layerId, layerPath);

            newLayer.setProperties(
                        PropertyFactory.circleRadius(Expression.coalesce(
                                Expression.get(prop_size), // rule
                                Expression.literal(getMPLThinkness(rasuis)))),

                        PropertyFactory.circleColor(Expression.coalesce(
                        Expression.get(prop_color_fill), // rule
                        Expression.literal(getColorName(fistFillColor)))),

                        PropertyFactory.circleStrokeColor(Expression.coalesce(
                                Expression.get(prop_color_stroke), // rule
                                Expression.literal(getColorName(outlineColor)))),

                        PropertyFactory.circleStrokeWidth(Expression.coalesce(
                                Expression.get(prop_thinkness), // rule
                                Expression.literal(getMPLThinkness(thinkness)))),
                        PropertyFactory.circleStrokeOpacity(1f));

        } else if (layerType == GeoConstants.GTLineString || layerType == GeoConstants.GTMultiLineString) {
            if (changeLayer)
                newLayer = style.getLayer(currentNamePrefix + layer_namepart + layerId);
            if (newLayer == null )
                newLayer = new LineLayer(currentNamePrefix + layer_namepart + layerId,layerPath);

            newLayer.setProperties(
                    PropertyFactory.lineColor(Expression.coalesce(
                            Expression.get(prop_color_fill), // rule
                            Expression.literal(getColorName(fistFillColor)))),

                    PropertyFactory.lineWidth(Expression.coalesce(
                            Expression.get(prop_thinkness), // rule
                            Expression.literal(getMPLThinkness(thinkness)))),

                    PropertyFactory.lineDasharray(type == 2 ?  new Float[]{2f, 2f} : null)

//                        PropertyFactory.lineDasharray(
//
//                        Expression.switchCase(
//                                Expression.eq(Expression.get(prop_type), Expression.literal(1)),
//                                // TRUE -> dashed
//                                Expression.literal(new Float[]{2f, 2f}),
//                                // DEFAULT -> "almost solid" (workaround instead of  null)
//                                Expression.literal(new Float[]{1f, 0f})
//                        ))
//                          try to use coalesce - not work - commented
//                        PropertyFactory.lineDasharray(Expression.coalesce(
//                                Expression.get(prop_type), // rule
//                                Expression.literal(type == 2 ?  new Float[]{2f, 2f} : null)))
//                        PropertyFactory.circleStrokeWidth(Expression.coalesce(
//                                Expression.get(prop_type), // rule
//                                Expression.literal(
//                                        Expression.step(
//                                                Expression.get(prop_type2),  // your prop type
//                                                Expression.literal(new Float[]{0f, 0f}),
//                                                Expression.stop(1, null),  // if type == 2
//                                                Expression.stop(2,  new Float[]{2f, 2f}),  // if type == 2
//                                                Expression.stop (3, null)  // if type == 2
//                                        )
//                                )))
                    /*
                     */

                );
        } else if (layerType == GTPolygon || layerType == GeoConstants.GTMultiPolygon) {
            if (changeLayer) {
                newLayer = style.getLayer(currentNamePrefix + layer_namepart + layerId);
                newLayer2 = style.getLayer(currentNamePrefix + layer_namepart + layerId + outline_namepart);
            }
            if (newLayer == null)
                newLayer = new FillLayer(currentNamePrefix + layer_namepart + layerId,layerPath);

            if (newLayer2 == null)
                newLayer2 = new LineLayer(currentNamePrefix + layer_namepart + layerId + outline_namepart, layerPath);

            newLayer.setProperties(
                            PropertyFactory.fillColor(Expression.coalesce(
                                    Expression.get(prop_color_fill_rule), // rule
                                    Expression.literal(getColorName(fistFillColor)))), // def value

                                PropertyFactory.fillOpacity(Expression.coalesce(
                                    Expression.get(prop_opacity), // rule
                                    Expression.literal(alpha))));

            newLayer2.setProperties(
                                PropertyFactory.lineColor(
                                        Expression.coalesce(
                                                Expression.get(prop_color_stroke), // rule
                                                Expression.literal(getColorName(outlineColor)))),

                                PropertyFactory.lineWidth(Expression.coalesce(
                                        Expression.get(prop_thinkness), // rule
                                        Expression.literal(getMPLThinkness(thinkness))
                                        )));
        }

        // signatures turn on
        if (layerStyle!= null) {
            String styleField = ((ITextStyle) layerStyle).getField();
            String styleText = ((ITextStyle) layerStyle).getText();

//            boolean needSignatures = !TextUtils.isEmpty(styleField) || !TextUtils.isEmpty(styleText);
            // old - if signature always turn on for all layer (vector)

            boolean needSignatures = false;
            boolean ruleSyling = false;

            if (iLayer instanceof  VectorLayer){
                final VectorLayer vectorLayer = (VectorLayer)(iLayer);
                if (vectorLayer.getRenderer() instanceof RuleFeatureRenderer ||
                        !TextUtils.isEmpty(styleField) || !TextUtils.isEmpty(styleText)) {
                needSignatures = true;
                if (vectorLayer.getRenderer() instanceof RuleFeatureRenderer)
                    ruleSyling = true;
                }
            }

            simbolLayer = style.getLayer(currentNamePrefixSymbol + layer_namepart + layerId);

            if (!needSignatures && simbolLayer != null) {
                // need remove
                style.removeLayer(simbolLayer);
                symbolsLayerHashMap.remove(layerId);

            } else {
                if (needSignatures) {
                    if (simbolLayer == null) {
                        Log.d("Mbgl", "create layer name : " + currentNamePrefixSymbol + layer_namepart + layerId);
                        Log.d("Mbgl", "create layer source : " + (layerPath + (isPolygon ? source_polygon_text : "")));
                        simbolLayer = new SymbolLayer(currentNamePrefixSymbol + layer_namepart + layerId,
                                layerPath + (isPolygon ? source_polygon_text : ""));

                        if (signaturesRootLayer != null && style.getLayer(signaturesRootLayer.getId()) != null ){
                            style.addLayerAbove(simbolLayer, signaturesRootLayer.getId());
                        }
                        else {
                            style.addLayer(simbolLayer);
                        }
                        symbolsLayerHashMap.put(layerId, simbolLayer);
                    }

                    PropertyValue<String> signatureProperty = null;
                    signatureProperty = PropertyFactory.textField("{" + prop_signature_text + "}");

                    String[] font = {"Open Sans Regular"};
                    //String[] font = {"Roboto Regular"}; // no offline

                    PropertyValue<String> placementProperty = null;
                    if (layerType == GeoConstants.GTPoint || layerType == GeoConstants.GTMultiPoint || isPolygon)
                        placementProperty = PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_POINT);
                    else
                        placementProperty = PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_LINE);

                    String anchor = getTextAnchor(textAlignment); // def - Property.TEXT_ANCHOR_TOP
                    Float[] offsets =  isPolygon? new Float[]{0.0f, 0f} :  getTextAnchorOffsets(textAlignment, textSize); // {0f, 0f};

                    simbolLayer.setProperties(
                            signatureProperty,

                            PropertyFactory.textSize(Expression.coalesce(
                                    Expression.get(prop_text_textsize), // rule
                                    Expression.literal((textSize + 3) * 3)  // def value
                            )),

                            PropertyFactory.symbolSpacing(15f), // less  = often

                            PropertyFactory.textColor(Expression.coalesce(
                                    Expression.get(prop_text_color), // rule
                                    Expression.literal(getColorName(textColor))  // def value
                            )),

                            PropertyFactory.textAnchor(Expression.coalesce(
                                    Expression.get(prop_text_textanchor), // rule
                                    Expression.literal(anchor)  // def value
                            )),
                            placementProperty,

                            PropertyFactory.textOffset(Expression.coalesce(
                                    Expression.get(prop_text_textoffsets), // rule
                                    Expression.literal(offsets)  // def value
                            )),

                            PropertyFactory.textAllowOverlap(true),
                            PropertyFactory.textIgnorePlacement(true),
                            PropertyFactory.textFont(font),
                            PropertyFactory.textMaxWidth(0f));
                }
            }
        }

        if (newLayer != null) {
            if (!changeLayer) {
                if (signaturesRootLayer != null && style.getLayer(signaturesRootLayer.getId()) != null ) {
                    style.addLayerBelow(newLayer, signaturesRootLayer.getId());
                }
                else {
                    style.addLayer(newLayer);
                }
                layersHashMap.put(layerId, newLayer);
            }
        }

        if (newLayer2 != null) {
            if (!changeLayer) {
                if (signaturesRootLayer != null && style.getLayer(signaturesRootLayer.getId()) != null  && newLayer != null){
                    style.addLayerBelow (newLayer2, signaturesRootLayer.getId());
                }
                else {
                    style.addLayer(newLayer2);
                }
                layersHashMap2.put(layerId, newLayer2);
            }
        }

        // set zoom for vector layer

        if (minZoom!= -1){
            if (newLayer != null)
                newLayer.setMinZoom(minZoom);
            if (newLayer2 != null)
                newLayer2.setMinZoom(minZoom);
            if (simbolLayer != null)
                simbolLayer.setMinZoom(minZoom);
        }

        if (maxZoom!= -1){
            if (newLayer != null)
                newLayer.setMaxZoom(maxZoom);
            if (newLayer2 != null)
                newLayer2.setMaxZoom(maxZoom);
            if (simbolLayer != null)
                simbolLayer.setMaxZoom(maxZoom);
        }
    }

    public static String getColorName(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    public static float getMPLThinkness(float x) {
        float xMin = 1f;
        float xMax = 100f;
        float yMin = 1f;
        float yMax = 40f;
        return yMin + (x - xMin) * (yMax - yMin) / (xMax - xMin);
    }

    public static String getTextAnchor(int ngAlignment){
        switch (ngAlignment){
            case ALIGN_TOP: return Property.TEXT_ANCHOR_TOP;
            case ALIGN_TOP_RIGHT: return Property.TEXT_ANCHOR_TOP_RIGHT;
            case ALIGN_RIGHT: return Property.TEXT_ANCHOR_RIGHT;
            case ALIGN_BOTTOM_RIGHT: return Property.TEXT_ANCHOR_BOTTOM_RIGHT;
            case ALIGN_BOTTOM: return Property.TEXT_ANCHOR_BOTTOM;
            case ALIGN_BOTTOM_LEFT: return Property.TEXT_ANCHOR_BOTTOM_LEFT;
            case ALIGN_LEFT: return Property.TEXT_ANCHOR_LEFT;
            case ALIGN_TOP_LEFT: return Property.TEXT_ANCHOR_TOP_LEFT;
        }
        return Property.TEXT_ANCHOR_TOP;
    }


    public static Float[] getTextAnchorOffsets (int ngAlignment, float textSize){
//        public final static float SIZE_SMALL = 3;
//        public final static float SIZE_MEDIUM = 6;
//        public final static float SIZE_BIG = 10;
        final float coef = textSize < 6f ? 2.0f : 1.5f;
        switch (ngAlignment) {
            case ALIGN_TOP :   return new Float[]{0f, -coef};
            case ALIGN_BOTTOM: return   new Float[]{0f, coef};
            case ALIGN_LEFT: return   new Float[]{-0.8f, 0f};
            case ALIGN_RIGHT:return   new Float[]{0.8f, 0f};
            case ALIGN_TOP_LEFT:  return   new Float[]{-0.8f, -coef};
            case ALIGN_TOP_RIGHT: return   new Float[]{0.8f, -coef};
            case ALIGN_BOTTOM_LEFT: return   new Float[]{-0.8f, coef};
            case ALIGN_BOTTOM_RIGHT: return   new Float[]{0.8f, coef};
            default: return new Float[]{0f, -coef};
        }
    }

    static public String getSpaceCorrectedText(String originalText){
        return originalText
                .replace("\u00A0", " ")      //
                .replace("\u2009", " ")      // thin space
                .replaceAll("\\s+", " ")     //
                .trim();
    }


    static public String getNullableValue(com.nextgis.maplib.datasource.Feature feature, String fieldStr ){
        return feature.getFieldValueAsString(feature.getFieldValueIndex(fieldStr));
    }

}
