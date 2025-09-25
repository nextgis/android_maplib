package com.nextgis.maplib.map.MLP;

import com.nextgis.maplib.map.MPLFeaturesUtils; // For constants

import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Geometry;
import org.maplibre.geojson.Point;
import org.maplibre.geojson.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PolygonEditClass extends MLGeometryEditClass {
    private List<org.maplibre.geojson.Point> editingVertices = new ArrayList<>();    // editing vertices
    private List<Integer> ringSizes = new ArrayList<>();
    List<org.maplibre.geojson.Feature> vertexFeatures = new ArrayList<>();

    public PolygonEditClass(int geoType, GeoJsonSource selectedEditedSource, Feature editingFeature, List<Feature> polygonFeatures,
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
        int start = 0;
        List<List<Point>> rings = new ArrayList<>();

        for (Integer size : ringSizes) {
            if (start + size > editingVertices.size()) break; // Avoid IndexOutOfBounds
            List<Point> ring = new ArrayList<>(editingVertices.subList(start, start + size));
            // close ring
            if (!ring.isEmpty()) {
                ring.add(ring.get(0));
            }
            rings.add(ring);
            start += size;
        }

        Polygon polygon = Polygon.fromLngLats(rings);

        org.maplibre.geojson.Feature feature = org.maplibre.geojson.Feature.fromGeometry(polygon);

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

        // vertex without close point via Stream
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
        // Implement deletion logic for polygon vertex
    }

    @Override
    public void movePointTo(LatLng point) {
        // Implement move logic for polygon vertex
    }
}
