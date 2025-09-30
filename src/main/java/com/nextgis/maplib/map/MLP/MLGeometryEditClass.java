package com.nextgis.maplib.map.MLP;

import android.graphics.PointF;

import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.Projection;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Point;

import java.util.ArrayList;
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


    public List<org.maplibre.geojson.Point> prepareNewPolyPoints(LatLng center, Projection projection) {

        List<org.maplibre.geojson.Point> result = new ArrayList<>();
        if (projection == null || center == null) {
            // Fallback to a small fixed degree offset if projection or center is not available
            double offsetDegrees = 0.001;
            Point point1Geo = Point.fromLngLat(center != null ? center.getLongitude() - offsetDegrees : -offsetDegrees,
                    center != null ? center.getLatitude() + offsetDegrees : offsetDegrees);
            Point point2Geo = Point.fromLngLat(center != null ? center.getLongitude() + offsetDegrees : offsetDegrees,
                    center != null ? center.getLatitude() - offsetDegrees : -offsetDegrees);

            Point point3Geo = Point.fromLngLat(center != null ? center.getLongitude() : 0,
                    center != null ? center.getLatitude() : 0);

            result.add(point1Geo);
            result.add(point2Geo);
            result.add(point3Geo);
        } else {
            org.maplibre.android.geometry.VisibleRegion visibleRegion = projection.getVisibleRegion();
            PointF screenNearLeft = projection.toScreenLocation(visibleRegion.nearLeft);
            PointF screenNearRight = projection.toScreenLocation(visibleRegion.nearRight);

            float screenWidthInPixels = Math.abs(screenNearRight.x - screenNearLeft.x);
            float pixelOffset = screenWidthInPixels * 0.20f;

            PointF centerScreenCoords = projection.toScreenLocation(center);

            PointF screenPoint1 = new PointF(centerScreenCoords.x - pixelOffset, centerScreenCoords.y - pixelOffset);
            PointF screenPoint2 = new PointF(centerScreenCoords.x + pixelOffset, centerScreenCoords.y + pixelOffset);
            PointF screenPoint3 = new PointF(centerScreenCoords.x - pixelOffset, centerScreenCoords.y + pixelOffset);

            LatLng latLng1 = projection.fromScreenLocation(screenPoint1);
            LatLng latLng2 = projection.fromScreenLocation(screenPoint2);
            LatLng latLng3 = projection.fromScreenLocation(screenPoint3);

            Point point1 = Point.fromLngLat(latLng1.getLongitude(), latLng1.getLatitude());
            Point point2 = Point.fromLngLat(latLng2.getLongitude(), latLng2.getLatitude());
            Point point3 = Point.fromLngLat(latLng3.getLongitude(), latLng3.getLatitude());

            result.add(point1);
            result.add(point2);
            result.add(point3);
        }

        return result;
    }
}
