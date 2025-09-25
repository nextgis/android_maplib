package com.nextgis.maplib.map.MLP;

import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Point;

import java.util.List;

public abstract class MLGeometryEditClass {
    final org.maplibre.geojson.Feature originalEditingFeature;
    public org.maplibre.geojson.Feature editingFeature;
    final GeoJsonSource selectedPolySource;
    final GeoJsonSource vertexSource;      // edit points  //
    public int selectedVertexIndex = -1;

    final GeoJsonSource markerSource;

    public MLGeometryEditClass(int geoType,
                               GeoJsonSource selectedEditedSource,
                               org.maplibre.geojson.Feature editingFeature,
                               List<org.maplibre.geojson.Feature> polygonFeatures,
                               GeoJsonSource selectedPolySource,
                               GeoJsonSource vertexSource,
                               GeoJsonSource markerSource) {
        this.originalEditingFeature = editingFeature;
        this.selectedPolySource = selectedPolySource;
        this.vertexSource = vertexSource;
        this.markerSource = markerSource;
    }

    // extract vertices from feature -
    abstract public void extractVertices(org.maplibre.geojson.Feature feature, boolean selectRandomVertex);       // edit points  //);

    // select another vertices by id (first display for example)
    abstract public void updateSelectionVerticeIndex(int id);       // update selection

    // select another vertices by point
    abstract public void updateSelectionVertice(Point newPoint);       // update selection


    // re-assemble points - move point for example
    abstract public void updateEditingPolygonAndVertex();

    abstract public LatLng getSelectedPoint();





    abstract public void deleteCurrentPoint();



    abstract public void movePointTo(LatLng point); // true = map center // false= location

    public void updateSelectionMiddlePoint(org.maplibre.geojson.Feature point) {
    }       // update selection

    public int getSelectedVertexIndex() {
        return selectedVertexIndex;
    }

    public void setSelectedVertexIndex(int i) {
        selectedVertexIndex = i;
    }

    public void displayMiddlePoints(boolean isInit, boolean displayMiddlePoints) {
    }

    public void regenerateVertexFeatures() {
    }

    public void setMarker(LatLng latLng) {
        org.maplibre.geojson.Feature feature = org.maplibre.geojson.Feature.fromGeometry(Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()));
        FeatureCollection markerFeatureCollection = FeatureCollection.fromFeature(feature);
        markerSource.setGeoJson(markerFeatureCollection);
    }
}
