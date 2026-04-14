package com.nextgis.maplib.map.MLP;

import com.nextgis.maplib.map.MPLFeaturesUtils;

import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Geometry;
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.Point;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class MeasurmentLine extends MLGeometryEditClass {
    private List<Point> editingVertices = new ArrayList<>();    // editing vertices




    public MeasurmentLine(int geoType,
                         GeoJsonSource selectedEditedSource,
                         Feature editingFeature,
                         List<Feature> lineFeatures,
                         GeoJsonSource selectedPolySource,
                         GeoJsonSource vertexSource,
                          GeoJsonSource markerSource,
                         String layerPath
    ) {
        super(geoType, selectedEditedSource, editingFeature, lineFeatures,
                selectedPolySource, vertexSource, markerSource, layerPath);



        Iterator<Feature> it = lineFeatures.iterator();
        String targetOrder = editingFeature.getStringProperty(MPLFeaturesUtils.prop_order);

        while (it.hasNext()) {
            Feature f = it.next();
            if (Objects.equals(f.getStringProperty(MPLFeaturesUtils.prop_order), targetOrder)) {
                it.remove();
            }
        }

        //selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(lineFeatures));

        editingFeature.addStringProperty("color", MPLFeaturesUtils.colorLightBlue);
    }

    @Override
    public void extractVertices(org.maplibre.geojson.Feature feature, boolean selectRandomVertex) {
        List<Point> points = new ArrayList<>();

        Geometry geometry = feature != null ? feature.geometry() : null;
        if (geometry instanceof LineString) {
            LineString lineString = (LineString) geometry;
            points.addAll(lineString.coordinates());
        }

        editingVertices = new ArrayList<>(points);
        vertexFeatures.clear();

        for (int index = 0; index < editingVertices.size(); index++) {
            Point pt = editingVertices.get(index);
            org.maplibre.geojson.Feature vertexFeature = org.maplibre.geojson.Feature.fromGeometry(pt);
            vertexFeature.addNumberProperty("index", index);
            vertexFeature.addNumberProperty("radius", MPLFeaturesUtils.pointRaduis);
            vertexFeature.addStringProperty("color", MPLFeaturesUtils.colorLightBlue);
            vertexFeatures.add(vertexFeature);
        }
        displayMiddlePoints(true, true);
    }

    @Override
    public void regenerateVertexFeatures() {
        List<org.maplibre.geojson.Feature> vertexFeaturesTmp = new ArrayList<>();
        for (int index = 0; index < editingVertices.size(); index++) {
            Point pt = editingVertices.get(index);
            org.maplibre.geojson.Feature f = org.maplibre.geojson.Feature.fromGeometry(pt);
            f.addNumberProperty("index", index);
            f.addNumberProperty("radius", MPLFeaturesUtils.pointRaduis);
            f.addStringProperty("color",
                    index == selectedVertexIndex ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue
            );
            vertexFeaturesTmp.add(f);
        }

        vertexFeatures.clear();
        vertexFeatures.addAll(vertexFeaturesTmp);
        if (!vertextHided)
            vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
    }

    public void generateMiddlePointsAddAndDisplay() {
        List<org.maplibre.geojson.Feature> newVertexFeatures = new ArrayList<>();
        for (Feature feature : vertexFeatures) {
            if (!feature.hasNonNullValueForProperty("middle"))
                newVertexFeatures.add(feature);
        }
        vertexFeatures.clear();
        vertexFeatures.addAll(newVertexFeatures);

        for (int index = 0; index < editingVertices.size() - 1; index++) {
            Point pt1 = editingVertices.get(index);
            Point pt2 = editingVertices.get(index + 1);
            Point middlePoint = getMapMidpoint(pt1, pt2);

            org.maplibre.geojson.Feature vertexFeature = org.maplibre.geojson.Feature.fromGeometry(middlePoint);
            vertexFeature.addNumberProperty("previndex", index);
            vertexFeature.addNumberProperty("radius", MPLFeaturesUtils.middleRaduis);
            vertexFeature.addStringProperty("color", MPLFeaturesUtils.colorLightBlue);
            vertexFeature.addBooleanProperty("middle", true);
            vertexFeatures.add(vertexFeature);
        }
    }

    @Override
    public void addNewFlowPoint(LatLng newPoint, boolean addAfterSelected) {

        List<org.maplibre.geojson.Point> newEditVertex = new ArrayList<>();


        editingVertices.add(org.maplibre.geojson.Point.fromLngLat(newPoint.getLongitude() ,newPoint.getLatitude()) );
        selectedVertexIndex = editingVertices.size() - 1;
        vertexFeatures.clear();

        for (int index = 0; index < editingVertices.size(); index++) {
            Point pt = editingVertices.get(index);
            org.maplibre.geojson.Feature vertexFeature = org.maplibre.geojson.Feature.fromGeometry(pt);
            vertexFeature.addNumberProperty("index", index);
            vertexFeature.addNumberProperty("radius", MPLFeaturesUtils.pointRaduis);
            vertexFeature.addStringProperty("color", MPLFeaturesUtils.colorLightBlue);
            vertexFeatures.add(vertexFeature);
        }
        regenerateVertexFeatures();

    }

    @Override
    public void displayMiddlePoints(boolean isInit, boolean changeGeoJsonSource) {
//        generateMiddlePointsAddAndDisplay();
//
//        if (isInit) {
//            if (!vertexFeatures.isEmpty() && !editingVertices.isEmpty()) {
//                for (org.maplibre.geojson.Feature f : vertexFeatures) {
//                    if (!f.hasNonNullValueForProperty("middle")) {
//                        Number indexNum = f.getNumberProperty("index");
//                        if (indexNum != null && indexNum.intValue() == 0) {
//                            f.addStringProperty("color", MPLFeaturesUtils.colorRED);
//                            break; // findFirst() return first exit after first coincided
//                        }
//                    }
//                }
//                this.selectedVertexIndex = 0;
//            }
//        }
//        if (changeGeoJsonSource && !vertextHided)
//            vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
    }

    @Override
    public void updateSelectionVerticeIndex(int index) {
        selectedVertexIndex = index;
    }

    @Override
    public void updateSelectionVertice(Point newPoint) {
        if (selectedVertexIndex >= 0 && selectedVertexIndex < editingVertices.size()) {
            editingVertices.set(selectedVertexIndex, newPoint);
        }
    }

    @Override
    public void updateEditingPolygonAndVertex() {
        if (editingVertices.isEmpty()) return;

        LineString lineString = LineString.fromLngLats(editingVertices);
        org.maplibre.geojson.Feature feature = org.maplibre.geojson.Feature.fromGeometry(lineString);



        List<Point> featurePolyEndLinePoints = new ArrayList<>();    // editing vertices
        if (editingVertices.size() > 2){
            featurePolyEndLinePoints.add(editingVertices.get(0));
            featurePolyEndLinePoints.add(editingVertices.get(editingVertices.size() - 1));
        }
        LineString lineStringEndPolyLine = LineString.fromLngLats(featurePolyEndLinePoints);
        org.maplibre.geojson.Feature featurePolyEndLine = org.maplibre.geojson.Feature.fromGeometry(lineStringEndPolyLine);
        featurePolyEndLine.addStringProperty("color", MPLFeaturesUtils.colorVeryLightBlue);



        if (originalEditingFeature != null && originalEditingFeature.properties() != null) {

            Iterator<String> it = originalEditingFeature.properties().keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                feature.addProperty(key, originalEditingFeature.properties().get(key));
            }
        }
        editingFeature = feature;
        feature.addStringProperty("color", MPLFeaturesUtils.colorRED);



        //featurePolyEndLine

        List<org.maplibre.geojson.Feature> features = new ArrayList<>();
        features.add(feature);
        features.add(featurePolyEndLine);


        // selectedPolySource.setGeoJson(feature);
        selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(features));



        vertexFeatures.clear();
        for (int index = 0; index < editingVertices.size(); index++) {
            Point pt = editingVertices.get(index);
            org.maplibre.geojson.Feature f = org.maplibre.geojson.Feature.fromGeometry(pt);
            f.addNumberProperty("index", index);
            f.addNumberProperty("radius", MPLFeaturesUtils.pointRaduis);
            f.addStringProperty("color", index == selectedVertexIndex ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue);
            vertexFeatures.add(f);
        }

        displayMiddlePoints(false, false);
        if (!vertextHided)
            vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
    }

    @Override
    public LatLng getSelectedPoint() {
        if (selectedVertexIndex >= editingVertices.size() && !editingVertices.isEmpty())
            selectedVertexIndex = editingVertices.size() - 1;
        if (editingVertices != null && selectedVertexIndex >=0 && selectedVertexIndex < editingVertices.size() && editingVertices.get(selectedVertexIndex) != null) {
            Point point = editingVertices.get(selectedVertexIndex);
            if (point != null) {
                return new LatLng(point.latitude(), point.longitude());
            }
        }
        return null;
    }

    @Override
    public void deleteCurrentPoint() {

    }

    @Override
    public void movePointTo(LatLng newpoint) {
        if (editingVertices != null && selectedVertexIndex >= 0 && selectedVertexIndex < editingVertices.size()) {
            Point pointToMove = editingVertices.get(selectedVertexIndex);
            if (pointToMove != null) {
                Point pt = Point.fromLngLat(newpoint.getLongitude(), newpoint.getLatitude());
                editingVertices.set(selectedVertexIndex, pt);

                regenerateVertexFeatures();
                updateEditingPolygonAndVertex();
                displayMiddlePoints(false, true);
                setMarker(newpoint);
            }
        }
    }

    public List<List<Point>> getPoints(){
        List<List<Point>> result = new ArrayList<>();
        result.add(editingVertices);
        return result;
    }
}
