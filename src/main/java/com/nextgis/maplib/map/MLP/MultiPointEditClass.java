package com.nextgis.maplib.map.MLP;

import com.nextgis.maplib.map.MPLFeaturesUtils; // For constants

import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Geometry;
import org.maplibre.geojson.MultiPoint;
import org.maplibre.geojson.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiPointEditClass extends MLGeometryEditClass {
    private List<org.maplibre.geojson.Point> editingVertices = new ArrayList<>();    // editing vertices
    List<org.maplibre.geojson.Feature> vertexFeatures = new ArrayList<>();

    public MultiPointEditClass(int geoType, GeoJsonSource selectedEditedSource, Feature editingFeature, List<Feature> polygonFeatures,
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

        if (geometry instanceof MultiPoint) {
            MultiPoint multiPoint = ((MultiPoint) geometry);
            points.addAll(multiPoint.coordinates());
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

        MultiPoint multiPoint = MultiPoint.fromLngLats(editingVertices);
        org.maplibre.geojson.Feature feature = org.maplibre.geojson.Feature.fromGeometry(multiPoint);

        if (originalEditingFeature != null) {
            String order = originalEditingFeature.getStringProperty(MPLFeaturesUtils.prop_order);
            if (order != null) {
                 feature.addStringProperty(MPLFeaturesUtils.prop_order, order);
            }
        }

        editingFeature = feature;
        // feature.addStringProperty("color", MPLFeaturesUtils.colorRED); // Usually not for the whole MultiPoint

        // outline
        selectedPolySource.setGeoJson(feature);

        List<org.maplibre.geojson.Feature> currentVertexFeatures =
                IntStream.range(0, editingVertices.size())
                        .mapToObj(index -> {
                            Point pt = editingVertices.get(index);
                            org.maplibre.geojson.Feature f = org.maplibre.geojson.Feature.fromGeometry(pt);
                            f.addNumberProperty("index", index);
                            f.addNumberProperty("radius", MPLFeaturesUtils.pointRaduis);
                            f.addStringProperty("color", index == selectedVertexIndex ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue);
                            return f;
                        })
                        .collect(Collectors.toList());

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
        if (editingVertices.size() <= 1) return; // Cannot delete if only one point remains (or becomes empty)
        if (selectedVertexIndex >= 0 && selectedVertexIndex < editingVertices.size()) {
            editingVertices.remove(selectedVertexIndex);
            if (selectedVertexIndex >= editingVertices.size()) { // Adjust if last element was deleted
                selectedVertexIndex = editingVertices.size() - 1;
            }
            if (editingVertices.isEmpty()) selectedVertexIndex = -1;
            updateEditingPolygonAndVertex();
             // Highlight the new selected vertex (or none if empty)
            regenerateVertexFeatures(); // This will re-apply colors
            displayMiddlePoints(false, true); // Update middle points if any concept exists here

            LatLng point = getSelectedPoint();
            if (point != null) setMarker(point);
        }
    }

    @Override
    public void movePointTo(LatLng newLatLng) {
        if (editingVertices != null && selectedVertexIndex >= 0 && selectedVertexIndex < editingVertices.size()) {
            Point newGeoPoint = Point.fromLngLat(newLatLng.getLongitude(), newLatLng.getLatitude());
            editingVertices.set(selectedVertexIndex, newGeoPoint);
            updateEditingPolygonAndVertex();
            regenerateVertexFeatures();
            displayMiddlePoints(false, true);
            setMarker(newLatLng);
        }
    }

    public void addNewPoint(LatLng center){
        if (center == null)
            return; // Or throw an IllegalArgumentException

        Point newGeoPoint = Point.fromLngLat(center.getLongitude(), center.getLatitude());
        editingVertices.add(newGeoPoint);
        selectedVertexIndex = editingVertices.size() - 1; // Select the newly added point

        updateEditingPolygonAndVertex();
        // regenerateVertexFeatures(); // Potentially redundant
        setMarker(center); // Place marker on the new point


    }


}
