package com.nextgis.maplib.map;

import android.graphics.Color;
import android.util.Log;

import com.nextgis.maplib.datasource.GeoGeometryCollection;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoLinearRing;
import com.nextgis.maplib.datasource.GeoMultiLineString;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;
import com.nextgis.maplib.util.GeoConstants;

import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.FillLayer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.MultiLineString;
import org.maplibre.geojson.MultiPoint;
import org.maplibre.geojson.MultiPolygon;
import org.maplibre.geojson.Point;
import org.maplibre.geojson.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//loader for features from NG to maplibre geojson features
public class MPLFeaturesUtils {

    static  String prop_featureid = "featureid";
    static  String prop_layerid = "layerid";
    static  String prop_order = "order";

    static String namePrefix = "nglayer-";


    static public List<org.maplibre.geojson.Feature> createFeatureListFromLayer(final VectorLayer layer){
        // mplibre  features from any type
        List<org.maplibre.geojson.Feature> vectorFeatures = new ArrayList<>();


        if  (layer.getGeometryType() == GeoConstants.GTPoint) {
            return getPointFeatures(layer);
        }

        if  (layer.getGeometryType() == GeoConstants.GTMultiPoint) {
            return getMultiPointFeatures(layer);
        }

        if  (layer.getGeometryType() == GeoConstants.GTLineString) {
            return getLineFeatures(layer);
        }

        if  (layer.getGeometryType() == GeoConstants.GTMultiLineString) {
            return getMultiLineFeatures(layer);
        }

        if  (layer.getGeometryType() == GeoConstants.GTPolygon) {
            return getPolygonFeatures(layer);
        }

        if  (layer.getGeometryType() == GeoConstants.GTMultiPolygon) {
            return getMultiPolygonFeatures(layer);
        }

        return vectorFeatures;

    }

    private static List<Feature> getLineFeatures(VectorLayer layer) {

        List<org.maplibre.geojson.Feature> lineFeatures = new ArrayList<>();

        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        for (Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry : features.entrySet()) {
            i++;
            Long id = entry.getKey();
            com.nextgis.maplib.datasource.Feature feature = entry.getValue();
            GeoLineString geoLineGeometry = (GeoLineString) feature.getGeometry();

            LineString multiPoint = getLineString(geoLineGeometry);

            Feature lineFeature = org.maplibre.geojson.Feature.fromGeometry(multiPoint);
            lineFeature.addStringProperty(prop_layerid, String.valueOf(layer.getId()));
            lineFeature.addStringProperty(prop_order, String.valueOf(i));
            lineFeature.addStringProperty(prop_featureid, String.valueOf(id));

            lineFeatures.add(lineFeature);
        }
        return  lineFeatures;

    }

    private static List<Feature> getMultiLineFeatures(VectorLayer layer) {

        List<org.maplibre.geojson.Feature> lineFeatures = new ArrayList<>();

        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        for (Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry : features.entrySet()) {

            List<LineString> linesArray = new ArrayList<>();

            i++;
            Long id = entry.getKey();
            com.nextgis.maplib.datasource.Feature feature = entry.getValue();
            GeoMultiLineString geoLineGeometry = (GeoMultiLineString) feature.getGeometry();

            for (int j = 0; j< geoLineGeometry.size(); j++) {
                GeoLineString geoLineString = geoLineGeometry.get(j);
                LineString lineString = getLineString(geoLineString);
                linesArray.add(lineString);

            }
            MultiLineString multiLineString = MultiLineString.fromLineStrings(linesArray);
            Feature lineFeature = Feature.fromGeometry(multiLineString);


            lineFeature.addStringProperty(prop_layerid, String.valueOf(layer.getId()));
            lineFeature.addStringProperty(prop_order, String.valueOf(i));
            lineFeature.addStringProperty(prop_featureid, String.valueOf(id));

            lineFeatures.add(lineFeature);

        }
        return  lineFeatures;
    }


    static public LineString getLineString(GeoLineString geoLineGeometry){
        List<Point> pointList = new ArrayList<>();

        for (int j = 0; j < geoLineGeometry.getPointCount(); j++){
            GeoPoint geoPointGeometry = (GeoPoint) geoLineGeometry.getPoint(j);
            double[] lonLat = convert3857To4326(geoPointGeometry.getX(), geoPointGeometry.getY());
            Point point = Point.fromLngLat(lonLat[0], lonLat[1]);
            pointList.add(point);
        }
        LineString multiPoint = LineString.fromLngLats(pointList);
        return multiPoint;
    }


    private static List<Feature> getPointFeatures(VectorLayer layer) {

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

            pointFeatures.add(pointFeature);
        }
        return  pointFeatures;
    }

    private static List<Feature> getMultiPointFeatures(VectorLayer layer) {

        List<org.maplibre.geojson.Feature> mpointFeatures = new ArrayList<>();

        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        for (Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry : features.entrySet()) {
            i++;
            Long id = entry.getKey();
            com.nextgis.maplib.datasource.Feature feature = entry.getValue();
            GeoMultiPoint geoMultiPointtGeometry = (GeoMultiPoint) feature.getGeometry();
            List<Point> pointList = new ArrayList<>();

            for (int j = 0; j < geoMultiPointtGeometry.size(); j++){
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

            mpointFeatures.add(mpointFeature);
        }
        return  mpointFeatures;
    }


    static public List<Feature> getMultiPolygonFeatures(final VectorLayer layer){
        List<org.maplibre.geojson.Feature> vectorFeatures = new ArrayList<>();

        Map<Long, com.nextgis.maplib.datasource.Feature> features = layer.getFeatures();
        int i = 0;
        for (Map.Entry<Long, com.nextgis.maplib.datasource.Feature> entry : features.entrySet()) {
            i++;
            Long id = entry.getKey();
            com.nextgis.maplib.datasource.Feature feature = entry.getValue();

            GeoGeometryCollection geoGeometryCollection = (GeoGeometryCollection) feature.getGeometry();

            ArrayList<Polygon> polygons = new ArrayList<>();
            for (int j = 0; j < geoGeometryCollection.size(); j++){
                GeoPolygon polygonNG = (GeoPolygon)geoGeometryCollection.getGeometry(j);
                Polygon polygonML = getPolygonFromNGFeaturePolygon(polygonNG);
                polygons.add(polygonML);
            }


            MultiPolygon multiPolygon = MultiPolygon.fromPolygons(polygons);
            org.maplibre.geojson.Feature mpolyFeature = Feature.fromGeometry(multiPolygon);

            mpolyFeature.addStringProperty(prop_layerid, String.valueOf(layer.getId()));
            mpolyFeature.addStringProperty(prop_order, String.valueOf(i));
            mpolyFeature.addStringProperty(prop_featureid, String.valueOf(id));

            vectorFeatures.add(mpolyFeature);
        }
        return vectorFeatures;
    }


    static public List<Feature> getPolygonFeatures(final VectorLayer layer){
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

                vectorFeatures.add(polyFeature);
            }
            return vectorFeatures;
    }

    public static org.maplibre.geojson.Feature getFeatureFromNGFeaturePolygon(GeoPolygon geoPolygonGeometry){

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

        org.maplibre.geojson.Feature polyFeature = org.maplibre.geojson.Feature.fromGeometry(org.maplibre.geojson.Polygon.fromLngLats(points));
        return polyFeature;
    }


    public static org.maplibre.geojson.Polygon getPolygonFromNGFeaturePolygon(GeoPolygon geoPolygonGeometry){

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
        org.maplibre.geojson.Polygon polygone = org.maplibre.geojson.Polygon.fromLngLats(points);
        return polygone;
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


    static public final String getTypePrefix(int layerType){
//        String namePrefix = "nglayer-"; // todo - now no type in layer name because deletion event cames with id only
//        if ( layerType == GeoConstants.GTPolygon || layerType == GeoConstants.GTMultiPolygon)
//            namePrefix = "nglayer-";
//        else if ( layerType == GeoConstants.GTPoint || layerType == GeoConstants.GTMultiPoint)
//            namePrefix = "point-";
//        else if ( layerType == GeoConstants.GTLineString || layerType == GeoConstants.GTMultiLineString)
//            namePrefix = "line-";
//        else namePrefix = "unknown-";
        return namePrefix;
    }

    static public void createSourceForLayer(int layerId, int layerType, final List<org.maplibre.geojson.Feature> layerFeatures,
                                            final Style style, Map<Integer, GeoJsonSource> sourceHashMap){
        String namePrefix = getTypePrefix(layerType);
        GeoJsonSource vectorSource = new GeoJsonSource(namePrefix + "source-" + layerId, FeatureCollection.fromFeatures(layerFeatures));
        style.addSource(vectorSource);
        sourceHashMap.put(layerId, vectorSource);
    }

    static public void createFillLayerForLayer(int layerId, int layerType, final Style style, Map<Integer, org.maplibre.android.style.layers.Layer>  layersHashMap){
        String namePrefix = getTypePrefix(layerType);

        org.maplibre.android.style.layers.Layer newLayer = null;

        if ( layerType == GeoConstants.GTPoint || layerType == GeoConstants.GTMultiPoint) {
            newLayer = new CircleLayer(namePrefix +"layer-"+ layerId, namePrefix +"source-"+ layerId)
                    .withProperties(
                            PropertyFactory.circleRadius(3f),
                            PropertyFactory.circleColor("#FF0000"),
                            PropertyFactory.fillOpacity(0.8f));
        } else  if ( layerType == GeoConstants.GTPolygon || layerType == GeoConstants.GTMultiPolygon){
            newLayer = new FillLayer(namePrefix + "layer-" + layerId, namePrefix +"source-" + layerId)
                    .withProperties(
                            PropertyFactory.fillColor("#FF00FF"),
                            PropertyFactory.fillOpacity(0.5f) );

        }  else if ( layerType == GeoConstants.GTLineString || layerType == GeoConstants.GTMultiLineString) {
            newLayer = new LineLayer(namePrefix +"layer-"+ layerId, namePrefix +"source-"+ layerId)
                    .withProperties(
                    PropertyFactory.lineColor(Color.BLUE),
                    PropertyFactory.lineWidth(2.0f)  );
        }

        if (newLayer != null) {
            style.addLayer(newLayer);
            layersHashMap.put(layerId, newLayer);
        }
    }





}
