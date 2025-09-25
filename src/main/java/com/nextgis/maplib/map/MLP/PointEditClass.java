package com.nextgis.maplib.map.MLP;

import com.nextgis.maplib.map.MPLFeaturesUtils; // For constants

import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Geometry;
import org.maplibre.geojson.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PointEditClass extends MLGeometryEditClass {
    private List<org.maplibre.geojson.Point> editingVertices = new ArrayList<>();    // editing vertices
    List<org.maplibre.geojson.Feature> vertexFeatures = new ArrayList<>();

    public PointEditClass(int geoType, GeoJsonSource selectedEditedSource, Feature editingFeature, List<Feature> polygonFeatures,
                          GeoJsonSource selectedPolySource, GeoJsonSource vertexSource, GeoJsonSource markerSource) {
        super(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                selectedPolySource, vertexSource, markerSource);

        polygonFeatures.removeIf(f -> Objects.equals(f.getStringProperty(MPLFeaturesUtils.prop_order), editingFeature.getStringProperty(MPLFeaturesUtils.prop_order)));
        selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(polygonFeatures));

        editingFeature.addStringProperty("color", MPLFeaturesUtils.colorLightBlue);
    }

    @Override
    public void extractVertices(org.maplibre.geojson.Feature feature, boolean selectRandomVertex) {
        List<Point> points = new ArrayList<>();
        Geometry geometry = feature != null ? feature.geometry() : null;

        if (geometry instanceof Point) {
            points.add(((Point) geometry));
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

        // update selection to 1st
        if (!vertexFeatures.isEmpty()){
            vertexFeatures.get(0).addStringProperty("color", MPLFeaturesUtils.colorRED);
            int firstIndex = vertexFeatures.get(0).getNumberProperty("index").intValue();
            this.selectedVertexIndex = firstIndex;
        }
        vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
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
        selectedVertexIndex = 0;
        Point point = editingVertices.get(0);
        Point newPoint = Point.fromLngLat(point.longitude(), point.latitude());
        org.maplibre.geojson.Feature feature = org.maplibre.geojson.Feature.fromGeometry(newPoint);

        if (originalEditingFeature != null) {
            String order = originalEditingFeature.getStringProperty(MPLFeaturesUtils.prop_order);
            if (order != null) {
                feature.addStringProperty(MPLFeaturesUtils.prop_order, order);
            }
        }
        editingFeature = feature;
        feature.addStringProperty("color", MPLFeaturesUtils.colorRED);

        // outline
        selectedPolySource.setGeoJson(feature);

        Point pt = editingVertices.get(0);
        org.maplibre.geojson.Feature f = org.maplibre.geojson.Feature.fromGeometry(pt);
        f.addNumberProperty("index", 0);
        f.addNumberProperty("radius", MPLFeaturesUtils.pointRaduis);
        f.addStringProperty("color", 0 == selectedVertexIndex ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue);
        List<org.maplibre.geojson.Feature> currentVertexFeatures = new ArrayList<>();
        currentVertexFeatures.add(f);
        vertexSource.setGeoJson(FeatureCollection.fromFeatures(currentVertexFeatures));
    }

    @Override
    public LatLng getSelectedPoint() {
        if (editingVertices != null && selectedVertexIndex >= 0 && selectedVertexIndex < editingVertices.size() && editingVertices.get(selectedVertexIndex) != null) {
            Point point = editingVertices.get(selectedVertexIndex);
            if (point != null) {
                return new LatLng(point.latitude(), point.longitude());
            }
        }
        return null;
    }

    @Override
    public void deleteCurrentPoint() {
        // Point cannot be deleted, or it would cease to exist.
        // Or, handle as appropriate for your application (e.g., delete the whole feature)
    }

    @Override
    public void movePointTo(LatLng point) {
         if (editingVertices != null && !editingVertices.isEmpty() && selectedVertexIndex == 0) {
            Point newGeoPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
            editingVertices.set(selectedVertexIndex, newGeoPoint);
            updateEditingPolygonAndVertex();
            setMarker(point);
        }
    }
}
