package com.nextgis.maplib.map;



import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_BOTTOM;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_BOTTOM_LEFT;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_BOTTOM_RIGHT;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_LEFT;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_RIGHT;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_TOP;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_TOP_LEFT;
import static com.nextgis.maplib.display.SimpleMarkerStyle.ALIGN_TOP_RIGHT;
import static com.nextgis.maplib.util.GeoConstants.GT_RASTER_WA;
import static com.nextgis.maplib.util.GeoConstants.GT_TRACK_WA;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_NORMAL;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;

import static org.maplibre.android.style.layers.PropertyFactory.rasterBrightnessMax;
import static org.maplibre.android.style.layers.PropertyFactory.rasterContrast;
import static org.maplibre.android.style.layers.PropertyFactory.rasterOpacity;

import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

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
import com.nextgis.maplib.display.SimpleLineStyle;
import com.nextgis.maplib.display.SimpleMarkerStyle;
import com.nextgis.maplib.display.SimplePolygonStyle;
import com.nextgis.maplib.display.TMSRenderer;
import com.nextgis.maplib.map.MLP.LineEditClass;
import com.nextgis.maplib.map.MLP.MLGeometryEditClass;
import com.nextgis.maplib.map.MLP.MultiLineEditClass;
import com.nextgis.maplib.map.MLP.MultiPointEditClass;
import com.nextgis.maplib.map.MLP.MultiPolygonEditClass;
import com.nextgis.maplib.map.MLP.PointEditClass;
import com.nextgis.maplib.map.MLP.PolygonEditClass;
import com.nextgis.maplib.util.GeoConstants;


import org.maplibre.android.maps.Style;
import org.maplibre.android.style.expressions.Expression;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.FillLayer;
import org.maplibre.android.style.layers.Layer;
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
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.MultiLineString;
import org.maplibre.geojson.MultiPoint;
import org.maplibre.geojson.MultiPolygon;
import org.maplibre.geojson.Point;
import org.maplibre.geojson.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//loader for features from NG to maplibre geojson features
public class MPLFeaturesUtils {

    static public Number pointRaduis = 8;
    static public Number middleRaduis = 4;
    static public String colorLightBlue = "#03a9f4";
    static public String colorBlue = "#0000FF";
    static public String colorRED = "#FF0000";

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

    static final public String id_name = "_id";


    public static MLGeometryEditClass createEditObject(
            int geoType,
            GeoJsonSource selectedEditedSource,
            Feature editingFeature,
            List<Feature> polygonFeatures,
            GeoJsonSource choosedSource, // source of point/ line / polygon
            GeoJsonSource vertexSource,
            GeoJsonSource markerSource) {

        if (geoType == GeoConstants.GTPolygon) {
            return new PolygonEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource);
        } else if (geoType == GeoConstants.GTPoint) {
            return new PointEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource);
        } else if (geoType == GeoConstants.GTMultiPoint) {
            return new MultiPointEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource);
        } else if (geoType == GeoConstants.GTLineString) {
            return new LineEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource);
        } else if (geoType == GeoConstants.GTMultiLineString) {
            return new MultiLineEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource);
        } else if (geoType == GeoConstants.GTMultiPolygon) {
            return new MultiPolygonEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    choosedSource, vertexSource, markerSource);
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

        if (layer.getGeometryType() == GeoConstants.GTPoint) {
            return getPointFeatures(layer,signatureField);
        }

        if (layer.getGeometryType() == GeoConstants.GTMultiPoint) {
            return getMultiPointFeatures(layer,signatureField);
        }

        if (layer.getGeometryType() == GeoConstants.GTLineString) {
            return getLineFeatures(layer,signatureField);
        }

        if (layer.getGeometryType() == GeoConstants.GTMultiLineString) {
            return getMultiLineFeatures(layer,signatureField);
        }

        if (layer.getGeometryType() == GeoConstants.GTPolygon) {
            return getPolygonFeatures(layer,signatureField);
        }

        if (layer.getGeometryType() == GeoConstants.GTMultiPolygon) {
            return getMultiPolygonFeatures(layer,signatureField);
        }
        return vectorFeatures;
    }

    private static List<Feature> getLineFeatures(VectorLayer layer, String signatureField) {
        List<org.maplibre.geojson.Feature> lineFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        for (Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry : features.entrySet()) {
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
            if (signatureField != null) {
                lineFeature.addStringProperty(prop_signature_text, entry.getValue().getFieldValueAsString(signatureField));
            }
            lineFeatures.add(lineFeature);
        }
        return lineFeatures;
    }

    private static List<Feature> getMultiLineFeatures(VectorLayer layer, String signatureField) {
        List<org.maplibre.geojson.Feature> lineFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        for (Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry : features.entrySet()) {
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
            if (signatureField != null) {
                lineFeature.addStringProperty(prop_signature_text, entry.getValue().getFieldValueAsString(signatureField));
            }
            lineFeatures.add(lineFeature);
        }
        return lineFeatures;
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

    private static List<Feature> getPointFeatures(VectorLayer layer, String signatureField) {
        List<org.maplibre.geojson.Feature> pointFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        for (Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry : features.entrySet()) {
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
            if (signatureField != null) {
                pointFeature.addStringProperty(prop_signature_text, entry.getValue().getFieldValueAsString(signatureField));
            }
            pointFeatures.add(pointFeature);
        }
        return pointFeatures;
    }

    private static List<Feature> getMultiPointFeatures(VectorLayer layer, String signatureField) {
        List<org.maplibre.geojson.Feature> mpointFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        for (Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry : features.entrySet()) {
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
            if (signatureField != null) {
                mpointFeature.addStringProperty(prop_signature_text, entry.getValue().getFieldValueAsString(signatureField));
            }
            mpointFeatures.add(mpointFeature);
        }
        return mpointFeatures;
    }

    static public List<Feature> getMultiPolygonFeatures(final VectorLayer layer, String signatureField) {
        List<org.maplibre.geojson.Feature> vectorFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        for (Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry : features.entrySet()) {
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
                mpolyFeature.addStringProperty(prop_signature_text, entry.getValue().getFieldValueAsString(signatureField));
            }
            vectorFeatures.add(mpolyFeature);
        }
        return vectorFeatures;
    }

    static public List<Feature> getPolygonFeatures(final VectorLayer layer, String signatureField) {
        List<org.maplibre.geojson.Feature> vectorFeatures = new ArrayList<>();
        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        for (Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry : features.entrySet()) {
            i++;
            Long id = entry.getKey();
            com.nextgis.maplib.datasource.Feature feature = entry.getValue();
            GeoPolygon geoPolygonGeometry = (GeoPolygon) feature.getGeometry();
            org.maplibre.geojson.Feature polyFeature = getFeatureFromNGFeaturePolygon(geoPolygonGeometry);
            polyFeature.addStringProperty(prop_layerid, String.valueOf(layer.getId()));
            polyFeature.addStringProperty(prop_order, String.valueOf(i));
            polyFeature.addStringProperty(prop_featureid, String.valueOf(id));
            if (signatureField != null) {
                polyFeature.addStringProperty(prop_signature_text, entry.getValue().getFieldValueAsString(signatureField));
            }
            vectorFeatures.add(polyFeature);
        }
        return vectorFeatures;
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

    static public void createSourceForLayer(int layerId, int layerType, final List<org.maplibre.geojson.Feature> layerFeatures,
                                            final Style style, Map<Integer, GeoJsonSource> sourceHashMap,
                                            Map<Integer, String> rasterLayersURL,
                                            Map<Integer, Integer> rasterLayersTmsTypeMap) {
        String currentNamePrefix = namePrefix;

        if (layerType == GT_TRACK_WA){
            return;
        }

        if (layerType == GT_RASTER_WA){
                RasterSource rasterSource = (RasterSource) style.getSource(currentNamePrefix + source_namepart + layerId);
                if (rasterSource != null && rasterSource.getUrl()!= null &&  !rasterSource.getUrl().equals(rasterLayersURL.get(layerId))){
                    style.removeSource(currentNamePrefix + source_namepart + layerId);
                    rasterSource = null;
                }
                if (rasterSource == null) {
                    TileSet tileSet = new TileSet("tileset",
                            rasterLayersURL.get(layerId));
                    Integer tileTmsType =rasterLayersTmsTypeMap.get(layerId);
                    if ( tileTmsType != null && tileTmsType != -1){
                        if (tileTmsType == TMSTYPE_NORMAL) {
                            tileSet.setScheme( "tms");
                            Log.e("TTMS", "tileset to tms");
                        }
                        if (tileTmsType == TMSTYPE_OSM) {
                            tileSet.setScheme("xyz");
                            Log.e("TTMS", "tileset to XYZ");
                        }
                    }
                    rasterSource = new RasterSource(currentNamePrefix + source_namepart + layerId,
                            tileSet, 256 );
                    style.addSource(rasterSource);
                }
            return;
        }

        GeoJsonSource vectorSource = (GeoJsonSource) style.getSource(currentNamePrefix + source_namepart + layerId);
        if (vectorSource == null) {
            vectorSource = new GeoJsonSource(currentNamePrefix + source_namepart + layerId, FeatureCollection.fromFeatures(layerFeatures));
            style.addSource(vectorSource);
        }
        else
            vectorSource.setGeoJson(FeatureCollection.fromFeatures(layerFeatures));

        sourceHashMap.put(layerId, vectorSource);
    }


    static public org.maplibre.android.style.layers.Layer getRasterLayer(int layerId, final Style style){
        String currentNamePrefix = namePrefix;
        org.maplibre.android.style.layers.Layer rasterLayer = style.getLayer(currentNamePrefix + layer_namepart + layerId);
        return rasterLayer;
    }


    static public List<String> getLayerMLibreNames(int layerId, int layerType){

        List<String> result = new ArrayList<>();
        result.add(namePrefix + layer_namepart + layerId);

        if (layerType == GeoConstants.GTPolygon || layerType == GeoConstants.GTMultiPolygon)
            result.add(namePrefix + layer_namepart + layerId + outline_namepart);

        String symbolLayer = "symbol-" +  namePrefix + layer_namepart + layerId;
        result.add(symbolLayer);

        return result;
    }

    static public void createFillLayerForLayer(int layerId, int layerType,
                                               final Style style,
                                               Map<Integer,org.maplibre.android.style.layers.Layer> layersHashMap,
                                               Map<Integer,org.maplibre.android.style.layers.Layer> layersHashMap2,
                                               Map<Integer,org.maplibre.android.style.layers.Layer> symbolsLayerHashMap,
                                               com.nextgis.maplib.display.Style layerStyle,
                                               boolean changeLayer,
                                               ILayer iLayer){
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
                rasterLayer = new RasterLayer(currentNamePrefix + layer_namepart + layerId,
                        currentNamePrefix + source_namepart + layerId);
                //Log.e("MPLREM",  "add layer: " + rasterLayer.getId());
                style.addLayer(rasterLayer);

            }
                if (minZoom!= -1)
                    rasterLayer.setMinZoom(minZoom);
                if (maxZoom!= -1)
                    rasterLayer.setMaxZoom(maxZoom + 1);

                // TMSRenderer tmsRenderer = (TMSRenderer) mRasterLayer.getRenderer();
                if (iLayer != null && iLayer instanceof  TMSLayer) {
                    TMSRenderer tmsRenderer = (TMSRenderer) ((TMSLayer) iLayer).getRenderer();
                    float alpha = tmsRenderer.getAlpha() / 255.0f; // stored value 0 - 255 // need for maplibre 0 - 1
                    float contrast = (tmsRenderer.getContrast() - 1) ; //stored value 0 - 100 ,  needed -1  +1
                    float brightness = ((tmsRenderer.getBrightness()) / 255.0f) +1 ; // stored value 0  510 , need value 0  >1   1 norm
                    boolean isGray = tmsRenderer.isForceToGrayScale();

                    rasterLayer.setProperties(
                            rasterOpacity(alpha),
                            rasterContrast(contrast),
                            rasterBrightnessMax(brightness)
                    );

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
        int type; // circle quadro triangle

        // line
        int lineType; // line - fi

        // poly
        boolean isFilled; // polygon - is filled

        int textAlignment = ALIGN_TOP;
        float textSize = 6;

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
            }
            if (layerStyle instanceof SimpleLineStyle){ //line
                lineType = ((SimpleLineStyle)layerStyle ).getType();
            }
            if (layerStyle instanceof SimplePolygonStyle){ //line
                isFilled = ((SimplePolygonStyle)layerStyle ).isFill();
            }
        }

        if (layerType == GeoConstants.GTPoint || layerType == GeoConstants.GTMultiPoint) {
            if (changeLayer) {
//                Log.e("PPOOIINNTT", "color set to" + getColorName(fistFillColor));
                newLayer = style.getLayer(currentNamePrefix + layer_namepart + layerId);
                newLayer.setProperties(PropertyFactory.circleRadius(getMPLThinkness(rasuis)),
                        PropertyFactory.circleColor(getColorName(fistFillColor)),
                        PropertyFactory.circleStrokeColor(getColorName(outlineColor)),  //
                        PropertyFactory.circleStrokeWidth(getMPLThinkness(thinkness)),         //
                        PropertyFactory.circleStrokeOpacity(1f));
            }
            else
                newLayer = new CircleLayer(currentNamePrefix + layer_namepart + layerId,
                        currentNamePrefix + source_namepart + layerId)
                    .withProperties(
                            PropertyFactory.circleRadius(getMPLThinkness(rasuis)),
                            PropertyFactory.circleColor(getColorName(fistFillColor)),
                            //PropertyFactory.fillOpacity(alpha));

                    PropertyFactory.circleStrokeColor(getColorName(outlineColor)),  //
                    PropertyFactory.circleStrokeWidth(getMPLThinkness(thinkness)),         //
                    PropertyFactory.circleStrokeOpacity(1f));


        } else if (layerType == GeoConstants.GTPolygon || layerType == GeoConstants.GTMultiPolygon) {
            if (changeLayer) {
                newLayer = style.getLayer(currentNamePrefix + layer_namepart + layerId);
                newLayer.setProperties(
                                PropertyFactory.fillColor(getColorName(fistFillColor)),
                                PropertyFactory.fillOpacity(alpha));

                newLayer2 = style.getLayer(currentNamePrefix + layer_namepart + layerId + outline_namepart);
                newLayer2.setProperties(
                                PropertyFactory.lineColor(getColorName(outlineColor)),
                                PropertyFactory.lineWidth(getMPLThinkness(thinkness)));
            } else {
                newLayer = new FillLayer(currentNamePrefix + layer_namepart + layerId, currentNamePrefix + source_namepart + layerId)
                        .withProperties(
                                PropertyFactory.fillColor(getColorName(fistFillColor)),
                                PropertyFactory.fillOpacity(alpha));

                newLayer2 = new LineLayer(currentNamePrefix + layer_namepart + layerId + outline_namepart, currentNamePrefix + source_namepart + layerId)
                        .withProperties(
                                PropertyFactory.lineColor(getColorName(outlineColor)),
                                PropertyFactory.lineWidth(getMPLThinkness(thinkness)));
            }

        } else if (layerType == GeoConstants.GTLineString || layerType == GeoConstants.GTMultiLineString) {
            if (changeLayer) {
                newLayer = style.getLayer(currentNamePrefix + layer_namepart + layerId);
                newLayer.setProperties( PropertyFactory.lineColor(getColorName(fistFillColor)),
                        PropertyFactory.lineWidth(getMPLThinkness(thinkness)));
            } else

                newLayer = new LineLayer(currentNamePrefix + layer_namepart + layerId, currentNamePrefix + source_namepart + layerId)
                    .withProperties(
                            //PropertyFactory.lineColor(Expression.get("color")),
                            PropertyFactory.lineColor(getColorName(fistFillColor)),
                            PropertyFactory.lineWidth(getMPLThinkness(thinkness)));
        }

        // signatures turn on
        String styleField = ((ITextStyle)layerStyle).getField();
        String styleText = ((ITextStyle)layerStyle).getText();
        boolean needSignatures = !TextUtils.isEmpty(styleField) || !TextUtils.isEmpty(styleText) ;


        simbolLayer = style.getLayer(currentNamePrefixSymbol + layer_namepart + layerId);

        if (!needSignatures && simbolLayer != null ){
            // need remove
            style.removeLayer(simbolLayer);
            symbolsLayerHashMap.remove(layerId);

        } else {
            if (needSignatures){
                if (simbolLayer == null){
                    simbolLayer = new SymbolLayer(currentNamePrefixSymbol + layer_namepart + layerId, currentNamePrefix + source_namepart + layerId);
                    style.addLayer(simbolLayer);
                    symbolsLayerHashMap.put(layerId, simbolLayer);
                }


                PropertyValue<String> signatureProperty = null;
                if (!TextUtils.isEmpty(styleText))
                    signatureProperty = PropertyFactory.textField(styleText);
                else {
                    if (styleField.equals(id_name))
                        signatureProperty = PropertyFactory.textField("{" + prop_featureid + "}");
                    else
                        signatureProperty = PropertyFactory.textField("{" + prop_signature_text + "}");
                }

                String [] font = {"Open Sans Regular"};

                PropertyValue<String> placementProperty = null;
                if (layerType == GeoConstants.GTPoint || layerType == GeoConstants.GTMultiPoint)
                    placementProperty = PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_POINT);
                else
                    placementProperty = PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_LINE_CENTER);

                String anchor = getTextAnchor(textAlignment); // def - Property.TEXT_ANCHOR_TOP
                Float[] offsets = getTextAnchorOffsets(textAlignment, textSize); // {0f, 0f};

                simbolLayer.setProperties(
                        signatureProperty,
                        PropertyFactory.textSize((textSize  + 3 )*3),
                        PropertyFactory.textColor(getColorName(outlineColor)),
                        PropertyFactory.textAnchor(anchor),
                        placementProperty,

                        PropertyFactory.textOffset(offsets),
                        PropertyFactory.textAllowOverlap(true),
                        PropertyFactory.textIgnorePlacement(true),
                        PropertyFactory.textFont(font),
                        PropertyFactory.textMaxWidth(0f))
                ;
            }
        }

        if (newLayer != null) {
            if (!changeLayer) {
                //Log.e("MPLREM",  "add layer: " + newLayer.getId());

                style.addLayer(newLayer);
                layersHashMap.put(layerId, newLayer);
            }
        }

        if (newLayer2 != null) {
            if (!changeLayer) {
//                Log.e("MPLREM",  "add layer2 : " + newLayer2.getId());
                style.addLayer(newLayer2);
                layersHashMap2.put(layerId, newLayer2);
            }
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
}
