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

import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.FillLayer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.sources.GeoJsonSource;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//loader for features from NG to maplibre geojson features
public class MPLFeaturesUtils {

    static public Number pointRaduis = 8;
    static public Number middleRaduis = 4;
    static public String colorLightBlue = "#03a9f4";
    static public String colorRED = "#FF0000";

    static public abstract class MLGeometryEditClass{

        //final GeoJsonSource editPolySource;
        final org.maplibre.geojson.Feature  originalEditingFeature;
        org.maplibre.geojson.Feature  editingFeature;
        final GeoJsonSource selectedPolySource;
        final GeoJsonSource vertexSource ;      // edit points  //
        public int selectedVertexIndex = -1;

        public MLGeometryEditClass(int geoType,
                                   GeoJsonSource selectedEditedSource ,
                                   org.maplibre.geojson.Feature  editingFeature,
                                   List<org.maplibre.geojson.Feature> polygonFeatures,
                                   //GeoJsonSource editPolySource ,
                                   GeoJsonSource selectedPolySource,
                                   GeoJsonSource vertexSource){
            this.originalEditingFeature = editingFeature;
            //this.editPolySource = editPolySource;
            this.selectedPolySource = selectedPolySource;
            this.vertexSource = vertexSource;

        }

        abstract public void extractVertices(org.maplibre.geojson.Feature feature, boolean selectRandomVertex );       // edit points  //);
        abstract public void updateSelectionVerticeIndex(int id);       // update selection
        abstract public void updateSelectionVertice( Point newPoint);       // update selection
        abstract public void updateEditingPolygonAndVertex();
        abstract public LatLng getSelectedPoint();

        public int getSelectedVertexIndex(){
            return selectedVertexIndex;
        }

        public void setSelectedVertexIndex(int i){
            selectedVertexIndex = i;
        }
    }

    static public class PolygonEditClass extends MLGeometryEditClass{
        private List<org.maplibre.geojson.Point>  editingVertices = new ArrayList<>();    // editing vertices
        private List<Integer> ringSizes = new ArrayList<>();
        List<org.maplibre.geojson.Feature> vertexFeatures = new ArrayList<>();

        public PolygonEditClass(int geoType, GeoJsonSource selectedEditedSource, Feature editingFeature, List<Feature> polygonFeatures,
                                //GeoJsonSource editPolySource,
                                GeoJsonSource selectedPolySource,GeoJsonSource vertexSource) {
            super(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    //editPolySource,
                    selectedPolySource, vertexSource);

            polygonFeatures.removeIf(f -> Objects.equals(f.getStringProperty(prop_order), editingFeature.getStringProperty(prop_order)));
            selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(polygonFeatures));

            editingFeature.addStringProperty("color", colorLightBlue);
            //editPolySource.setGeoJson(editingFeature);
//            Feature tmpFeature = Feature.fromGeometry(editingFeature.geometry());
//            copyProperties(editingFeature, tmpFeature);
//            editPolySource.setGeoJson(tmpFeature);
        }

        @Override
        public void extractVertices(org.maplibre.geojson.Feature feature, boolean selectRandomVertex) {
            List<Point> points = new ArrayList<>();
            List<Integer> sizes = new ArrayList<>();

            Geometry geometry = feature != null ? feature.geometry() : null;
            if (geometry instanceof Polygon) {
                Polygon polygon = (Polygon) geometry;
                List<List<Point>> coordinates = polygon.coordinates();
                for (List<Point> ring : coordinates) {
                    List<Point> ringWithoutClosing = ring.subList(0, ring.size() - 1);
                    sizes.add(ringWithoutClosing.size());
                    points.addAll(ringWithoutClosing);
                }
            }

            editingVertices = new ArrayList<>(points);
            vertexFeatures.clear();
            ringSizes = sizes; // global var

            for (int index = 0; index < editingVertices.size(); index++) {
                Point pt = editingVertices.get(index);
                org.maplibre.geojson.Feature vertexFeature = org.maplibre.geojson.Feature.fromGeometry(pt);
                vertexFeature.addNumberProperty("index", index);
                vertexFeature.addNumberProperty("radius", pointRaduis);
                vertexFeature.addStringProperty("color", colorLightBlue);
                vertexFeatures.add(vertexFeature);
            }

            // update selection to 1st
            vertexFeatures.get(0).addStringProperty("color", colorRED);
            int firstIndex = vertexFeatures.get(0).getNumberProperty("index").intValue();
            this.selectedVertexIndex = firstIndex;

            vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
        }

        @Override
        public void updateSelectionVerticeIndex(int index) {
            selectedVertexIndex = index;
        }

        @Override
        public void updateSelectionVertice( Point newPoint) {
            editingVertices.set(selectedVertexIndex, newPoint);
        }

        @Override
        public void updateEditingPolygonAndVertex() {
            int start = 0;
            List<List<Point>> rings = new ArrayList<>();

            for (Integer size : ringSizes) {
                List<Point> ring = new ArrayList<>(editingVertices.subList(start, start + size));
                // close ring
                ring.add(ring.get(0));
                rings.add(ring);
                start += size;
            }

            Polygon polygon = Polygon.fromLngLats(rings);

            org.maplibre.geojson.Feature feature = org.maplibre.geojson.Feature.fromGeometry(polygon);

            if (originalEditingFeature != null) {
                String order = originalEditingFeature.getStringProperty(prop_order);
                feature.addStringProperty(prop_order, order);
            }

            editingFeature = feature;

            feature.addStringProperty("color", colorRED);

            // outline
            selectedPolySource.setGeoJson(feature);

            // vertex without close point via  Stream
            List<org.maplibre.geojson.Feature> vertexFeatures =
                    IntStream.range(0, editingVertices.size())
                            .mapToObj(index -> {
                                Point pt = editingVertices.get(index);
                                org.maplibre.geojson.Feature f = org.maplibre.geojson.Feature.fromGeometry(pt);
                                f.addNumberProperty("index", index);
                                f.addNumberProperty("radius", pointRaduis);
                                f.addStringProperty("color", index == selectedVertexIndex? colorRED: colorLightBlue );
                                return f;
                            })
                            .collect(Collectors.toList());

            vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
        }

        @Override
        public LatLng getSelectedPoint() {
            if (editingVertices!= null && editingVertices.get(selectedVertexIndex)!= null) {
                Point point = editingVertices.get(selectedVertexIndex);
                if (point != null){
                    return new LatLng(point.latitude(), point.longitude());
                }
            }
            return null;
        }
    }

    static  String prop_featureid = "featureid";
    static  String prop_layerid = "layerid";
    static  String prop_order = "order";
    static String namePrefix = "nglayer-";

    public static MLGeometryEditClass createEditObject(
            int geoType,
            GeoJsonSource selectedEditedSource,
            Feature editingFeature,
            List<Feature> polygonFeatures,
            //GeoJsonSource editPolySource,
            GeoJsonSource selectedPolySource,
            GeoJsonSource vertexSource){

        if  (geoType == GeoConstants.GTPolygon) {
            return new PolygonEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    //editPolySource,
                    selectedPolySource,vertexSource);
        } else
            return new PolygonEditClass(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                    //editPolySource,
                    selectedPolySource, vertexSource);

    }


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
