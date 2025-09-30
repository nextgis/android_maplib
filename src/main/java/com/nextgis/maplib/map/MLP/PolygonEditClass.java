package com.nextgis.maplib.map.MLP;

import android.graphics.PointF;

import com.nextgis.maplib.map.MPLFeaturesUtils;

import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.Projection;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Point;
import org.maplibre.geojson.Polygon;
import org.maplibre.geojson.Geometry;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class PolygonEditClass extends MLGeometryEditClass {


    private List<org.maplibre.geojson.Point> editingVertices = new ArrayList<>();    // editing vertices
    List<org.maplibre.geojson.Feature> vertexFeatures = new ArrayList<>();

    // Holds the main vertices for all rings in a flat list.
    // Example: [ext_pt1, ext_pt2, ext_pt3, hole1_pt1, hole1_pt2, hole1_pt3, hole2_pt1, ...]
    // Inherited: protected List<Point> editingVertices = new ArrayList<>();

    // Stores the ending index (exclusive) in editingVertices for each ring.
    // Example: If exterior has 3 points, first hole has 3 points, this list will be [3, 6]
    private List<Integer> polygonRingEndIndices = new ArrayList<>();

    // Index of the currently selected ring (0 for exterior, 1 for first hole, etc.)
    private int selectedRingIndex = -1;
    // Index of the selected vertex within its specific ring.
    private int selectedVertexIndexInRing = -1;
    // Inherited: protected int selectedVertexIndex = -1; // Global index in editingVertices

    // Inherited: List<org.maplibre.geojson.Feature> vertexFeatures = new ArrayList<>();


    public PolygonEditClass(int geoType, GeoJsonSource selectedEditedSource, Feature editingFeature, List<Feature> polygonFeatures,
                            GeoJsonSource selectedPolySource, GeoJsonSource vertexSource, GeoJsonSource markerSource) {
        super(geoType, selectedEditedSource, editingFeature, polygonFeatures,
                selectedPolySource, vertexSource, markerSource);

        // Remove the currently editing feature from the background list if it exists
        if (editingFeature != null && editingFeature.hasProperty(MPLFeaturesUtils.prop_order)) {
            polygonFeatures.removeIf(f -> Objects.equals(f.getStringProperty(MPLFeaturesUtils.prop_order), editingFeature.getStringProperty(MPLFeaturesUtils.prop_order)));
        }
        selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(polygonFeatures));

        if (this.originalEditingFeature != null) {
            extractVertices(this.originalEditingFeature, true);
        } else {
            // Handle new polygon creation if needed, perhaps with default points
            this.editingFeature = Feature.fromGeometry(Polygon.fromLngLats(new ArrayList<>()));
            if (originalEditingFeature != null && originalEditingFeature.properties() != null) {
                originalEditingFeature.properties().keySet().forEach(key -> {
                    this.editingFeature.addProperty(key, originalEditingFeature.properties().get(key));
                });
            }
            this.editingFeature.addStringProperty("color", MPLFeaturesUtils.colorRED);
            selectedPolySource.setGeoJson(this.editingFeature);
            displayMiddlePoints(true, true);
        }
    }

    @Override
    public void extractVertices(org.maplibre.geojson.Feature feature, boolean selectInitialVertex) {
        editingVertices.clear();
        polygonRingEndIndices.clear();
        selectedVertexIndex = -1;
        selectedRingIndex = -1;
        selectedVertexIndexInRing = -1;

        Geometry geometry = feature != null ? feature.geometry() : null;
        if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;
            List<List<Point>> coordinates = polygon.coordinates();
            for (List<Point> ring : coordinates) {
                if (ring.size() > 1) { // A valid ring needs at least 3 points, but we store n-1
                    List<Point> ringWithoutClosing = ring.subList(0, ring.size() - 1);
                    editingVertices.addAll(ringWithoutClosing);
                    polygonRingEndIndices.add(editingVertices.size());
                } else if (!ring.isEmpty()){ // single point passed as a ring?
                     editingVertices.addAll(ring);
                     polygonRingEndIndices.add(editingVertices.size());
                }
            }
        }

        if (selectInitialVertex && !editingVertices.isEmpty()) {
            updateSelectedRingAndVertexInRingIndices(0); // Select first vertex of the first ring
        }
        updateEditingPolygonAndVertex(); // This will also call displayMiddlePoints
    }


    /**
     * Updates the selectedRingIndex and selectedVertexIndexInRing based on the global selectedVertexIndex.
     *
     * @param globalIndex The global index in the flat editingVertices list.
     */
    private void updateSelectedRingAndVertexInRingIndices(int globalIndex) {
        if (globalIndex == -1 || editingVertices.isEmpty() || polygonRingEndIndices.isEmpty()) {
            selectedRingIndex = -1;
            selectedVertexIndexInRing = -1;
            selectedVertexIndex = -1;
            return;
        }

        selectedVertexIndex = globalIndex; // Ensure global selectedVertexIndex is set

        int currentRingStartIndex = 0;
        for (int i = 0; i < polygonRingEndIndices.size(); i++) {
            int ringEndIndex = polygonRingEndIndices.get(i);
            if (globalIndex >= currentRingStartIndex && globalIndex < ringEndIndex) {
                selectedRingIndex = i;
                selectedVertexIndexInRing = globalIndex - currentRingStartIndex;
                return;
            }
            currentRingStartIndex = ringEndIndex;
        }
        // Fallback if something is inconsistent
        selectedRingIndex = -1;
        selectedVertexIndexInRing = -1;
    }


    @Override
    public void displayMiddlePoints(boolean isInit, boolean changeGeoJsonSource) {
        vertexFeatures.clear();

        if (editingVertices.isEmpty() && polygonRingEndIndices.isEmpty() && changeGeoJsonSource) {
            vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
            return;
        }

        int currentRingStartIndex = 0;
        for (int i = 0; i < polygonRingEndIndices.size(); i++) { // Loop through each ring
            int ringEndIndex = polygonRingEndIndices.get(i);
            List<Point> currentRingPoints = editingVertices.subList(currentRingStartIndex, ringEndIndex);

            if (currentRingPoints.isEmpty()) {
                currentRingStartIndex = ringEndIndex;
                continue;
            }

            // Generate main vertex features for the current ring
            for (int j = 0; j < currentRingPoints.size(); j++) {
                Point pt = currentRingPoints.get(j);
                Feature vertexFeature = Feature.fromGeometry(pt);
                vertexFeature.addNumberProperty("radius", MPLFeaturesUtils.pointRaduis);
                vertexFeature.addNumberProperty("ringIndex", i);
                vertexFeature.addNumberProperty("vertexIndexInRing", j);
                vertexFeature.addNumberProperty("index", currentRingStartIndex + j); // Global index

                boolean isSelectedVertex = (selectedVertexIndex == currentRingStartIndex + j);
                String color = (i == selectedRingIndex) ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue;
                if (isSelectedVertex) color = MPLFeaturesUtils.colorRED; // Ensure selected is always red
                vertexFeature.addStringProperty("color", color);
                vertexFeatures.add(vertexFeature);
            }

            // Generate middle point features for the current ring (if it has enough points to form segments)
            if (currentRingPoints.size() >= 1) { // Need at least 1 point to show middle point if it's a line, 2 for polygon segment
                 for (int j = 0; j < currentRingPoints.size(); j++) {
                    Point pt1 = currentRingPoints.get(j);
                    // For a closed ring, the last point connects to the first
                    Point pt2 = currentRingPoints.get((j + 1) % currentRingPoints.size());

                    Point middlePoint = Point.fromLngLat(
                            (pt1.longitude() + pt2.longitude()) / 2.0,
                            (pt1.latitude() + pt2.latitude()) / 2.0
                    );

                    Feature middleFeature = Feature.fromGeometry(middlePoint);
                    middleFeature.addBooleanProperty("middle", true);
                    middleFeature.addNumberProperty("ringIndex", i);
                    // This middle point is after vertex 'j' in its ring
                    middleFeature.addNumberProperty("prevVertexIndexInRing", j);
                    middleFeature.addNumberProperty("radius", MPLFeaturesUtils.middleRaduis);
                    String color = (i == selectedRingIndex) ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue;
                    middleFeature.addStringProperty("color", color);
                    vertexFeatures.add(middleFeature);
                }
            }
            currentRingStartIndex = ringEndIndex;
        }
        
        if (changeGeoJsonSource && vertexSource != null) {
            vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
        }
    }

    @Override
    public void updateSelectionMiddlePoint(Feature middlePointFeature) {
        if (middlePointFeature == null || !middlePointFeature.hasProperty("middle") ||
            !middlePointFeature.getBooleanProperty("middle") || !(middlePointFeature.geometry() instanceof Point)) {
            return;
        }

        int ringIdx = middlePointFeature.getNumberProperty("ringIndex").intValue();
        int prevVertexIdxInRing = middlePointFeature.getNumberProperty("prevVertexIndexInRing").intValue();
        Point newVertexPoint = (Point) middlePointFeature.geometry();

        if (ringIdx < 0 || ringIdx >= polygonRingEndIndices.size()) return; // Invalid ring index

        int ringStartIndex = (ringIdx == 0) ? 0 : polygonRingEndIndices.get(ringIdx - 1);
        int insertionPointInFlatList = ringStartIndex + prevVertexIdxInRing + 1;

        editingVertices.add(insertionPointInFlatList, newVertexPoint);

        // Update ring end indices from the affected ring onwards
        for (int i = ringIdx; i < polygonRingEndIndices.size(); i++) {
            polygonRingEndIndices.set(i, polygonRingEndIndices.get(i) + 1);
        }

        // Set the new vertex as selected
        selectedVertexIndex = insertionPointInFlatList;
        updateSelectedRingAndVertexInRingIndices(selectedVertexIndex);

        updateEditingPolygonAndVertex();
        setMarker(getSelectedPoint());
    }


    @Override
    public void updateSelectionVerticeIndex(int index) {
        if (index >= 0 && index < editingVertices.size()) {
            selectedVertexIndex = index;
            updateSelectedRingAndVertexInRingIndices(index);
        } else {
            selectedVertexIndex = -1;
            selectedRingIndex = -1;
            selectedVertexIndexInRing = -1;
        }
        updateEditingPolygonAndVertex(); // To refresh colors
        setMarker(getSelectedPoint());
    }

    @Override
    public void updateSelectionVertice(Point newPoint) {
        if (selectedVertexIndex >= 0 && selectedVertexIndex < editingVertices.size()) {
            editingVertices.set(selectedVertexIndex, newPoint);
            updateEditingPolygonAndVertex();
        }
    }

    @Override
    public void updateEditingPolygonAndVertex() {
        List<List<Point>> rings = new ArrayList<>();
        int currentRingStartIndex = 0;

        for (int ringEndIndex : polygonRingEndIndices) {
            List<Point> currentRingGeoPoints = new ArrayList<>();
            if (ringEndIndex > currentRingStartIndex) {
                 List<Point> ringVertices = editingVertices.subList(currentRingStartIndex, ringEndIndex);
                 currentRingGeoPoints.addAll(ringVertices);
                 // Close the ring: add the first point to the end if it's not already there and size > 0
                 if (!ringVertices.isEmpty()) {
                     currentRingGeoPoints.add(ringVertices.get(0));
                 }
            }
             // A valid GeoJSON ring needs at least 4 points (3 unique + closing).
             // However, for display, we allow fewer. Polygon.fromLngLats might handle this.
            if (currentRingGeoPoints.size() >= 4 || (rings.isEmpty() && currentRingGeoPoints.size() >=1) ) { // Exterior can be less for a moment
                 rings.add(currentRingGeoPoints);
            } else if (!rings.isEmpty() && currentRingGeoPoints.size() >= 1 && currentRingGeoPoints.size() < 4) {
                // Invalid inner ring, do not add. Or decide on a strategy.
                // For now, we are strict for inner rings for valid GeoJSON.
            }


            currentRingStartIndex = ringEndIndex;
        }
        
        Polygon polygonGeom;
        if (rings.isEmpty() && !editingVertices.isEmpty() && polygonRingEndIndices.isEmpty()) { 
            // Special case: if we have points but no ring structure (e.g. during initial drag of a new poly)
            // This might not be the best place, extractVertices should define at least one ring.
            // For now, create a single open ring for display.
             List<Point> tempRing = new ArrayList<>(editingVertices);
             if(!tempRing.isEmpty()) tempRing.add(tempRing.get(0)); // Close it
             polygonGeom = Polygon.fromLngLats(Collections.singletonList(tempRing));
        } else if (rings.isEmpty()) {
            polygonGeom = Polygon.fromLngLats(new ArrayList<>()); // Empty polygon
        }
        else {
             polygonGeom = Polygon.fromLngLats(rings);
        }


        Feature newFeature = Feature.fromGeometry(polygonGeom);

        if (originalEditingFeature != null && originalEditingFeature.properties() != null) {
            originalEditingFeature.properties().keySet().forEach(key -> {
                newFeature.addProperty(key, originalEditingFeature.properties().get(key));
            });
        }
        newFeature.addStringProperty("color", MPLFeaturesUtils.colorRED); // Highlight editing polygon
        editingFeature = newFeature;

        if (selectedPolySource != null) {
            selectedPolySource.setGeoJson(editingFeature);
        }
        displayMiddlePoints(false, true); // Refresh vertices and middle points
    }

    @Override
    public LatLng getSelectedPoint() {
        if (selectedVertexIndex >= 0 && selectedVertexIndex < editingVertices.size()) {
            Point point = editingVertices.get(selectedVertexIndex);
            if (point != null) {
                return new LatLng(point.latitude(), point.longitude());
            }
        }
        return null;
    }

    @Override
    public void deleteCurrentPoint() {
        if (selectedVertexIndex == -1 || editingVertices.isEmpty() || selectedRingIndex == -1) return;

        int ringStartIndex = (selectedRingIndex == 0) ? 0 : polygonRingEndIndices.get(selectedRingIndex - 1);
        int numPointsInSelectedRing = polygonRingEndIndices.get(selectedRingIndex) - ringStartIndex;

        // Prevent deleting if it makes the exterior ring too small (less than 3 points)
        if (numPointsInSelectedRing <= 3) {
            return; // Cannot delete from exterior if it has 3 or fewer points
        }

        editingVertices.remove(selectedVertexIndex);

        // Update ring end indices
        for (int i = selectedRingIndex; i < polygonRingEndIndices.size(); i++) {
            polygonRingEndIndices.set(i, polygonRingEndIndices.get(i) - 1);
        }
        
        numPointsInSelectedRing--; // Point removed

        // If a hole (inner ring) becomes too small (less than 3 points), remove the entire hole
        if (selectedRingIndex > 0 && numPointsInSelectedRing < 3) {
            // Remove all points of this hole
            int holeStartIndex = (selectedRingIndex == 0) ? 0 : polygonRingEndIndices.get(selectedRingIndex -1 ); // This will be the new end index of previous
            int holeEndIndex = polygonRingEndIndices.get(selectedRingIndex); // This is the old end index of the current (empty) ring

            // If numPointsInSelectedRing is 0, all points of this hole were already removed by the loop above.
            // if > 0 points remain, they were not part of this hole.
            // The list `polygonRingEndIndices` needs to remove the entry for this hole.
            
            // Re-calculate start/end after prior modification
            int actualHolePointsToRemove = numPointsInSelectedRing; // these are the points left that form the invalid hole
            if (actualHolePointsToRemove > 0) {
                 // The point was already removed, the remaining points form the invalid ring.
                 // Need to remove these remaining points of the now invalid hole
                 // `editingVertices` was already shifted.
                 // The 'holeStartIndex' for deletion should be where the invalid hole now begins.
                 int currentHoleStart = (selectedRingIndex == 0) ? 0 : polygonRingEndIndices.get(selectedRingIndex - 1);
                 for(int k=0; k < actualHolePointsToRemove; k++) {
                     editingVertices.remove(currentHoleStart); // remove from the start of the invalid ring
                 }
                 // Adjust subsequent ring end indices again
                 for (int i = selectedRingIndex; i < polygonRingEndIndices.size(); i++) {
                    polygonRingEndIndices.set(i, polygonRingEndIndices.get(i) - actualHolePointsToRemove);
                 }
            }
            polygonRingEndIndices.remove(selectedRingIndex);

        }


        // Adjust selection
        if (editingVertices.isEmpty()) {
            selectedVertexIndex = -1;
        } else {
            // Try to select the previous vertex, or the first one if the deleted one was the first
            selectedVertexIndex = Math.max(0, selectedVertexIndex - 1);
            if (selectedVertexIndex >= editingVertices.size()) { // If last element was deleted
                selectedVertexIndex = editingVertices.size() -1;
            }
        }
        updateSelectedRingAndVertexInRingIndices(selectedVertexIndex); // Update context

        updateEditingPolygonAndVertex();
        setMarker(getSelectedPoint());
    }


    private void addHole(List<Point> holePoints) {
        if (holePoints == null || holePoints.size() < 3) return; // A hole needs at least 3 points

        // Add the new hole's points to the end of editingVertices
        int newHoleStartIndex = editingVertices.size();
        editingVertices.addAll(holePoints);
        polygonRingEndIndices.add(editingVertices.size()); // Add new end index for this hole

        // Select the first point of the new hole
        selectedVertexIndex = newHoleStartIndex;
        updateSelectedRingAndVertexInRingIndices(selectedVertexIndex);

        updateEditingPolygonAndVertex();
        setMarker(getSelectedPoint());
    }

    public void deleteCurrentHole() { // holeIndexToRemove is 1-based for user, so 0 for first hole, 1 for second etc.
        int holeIndexToRemove = selectedRingIndex;
                                                // but here we expect 0-based for internal (1 for first hole)
        if (holeIndexToRemove <= 0 || holeIndexToRemove >= polygonRingEndIndices.size()) return; // Invalid hole index

        int holeStartIndex = polygonRingEndIndices.get(holeIndexToRemove - 1);
        int holeEndIndex = polygonRingEndIndices.get(holeIndexToRemove);
        int pointsInHole = holeEndIndex - holeStartIndex;

        editingVertices.subList(holeStartIndex, holeEndIndex).clear(); // Remove points

        // Update subsequent ring end indices
        for (int i = holeIndexToRemove; i < polygonRingEndIndices.size(); i++) {
            polygonRingEndIndices.set(i, polygonRingEndIndices.get(i) - pointsInHole);
        }
        polygonRingEndIndices.remove(holeIndexToRemove); // Remove the hole's end index entry

        // Adjust selection if it was in the removed hole or a subsequent one
        if (selectedRingIndex == holeIndexToRemove) {
            selectedVertexIndex = (holeStartIndex > 0) ? holeStartIndex -1 : 0; // Select last point of previous ring or first overall
             if (editingVertices.isEmpty()) selectedVertexIndex = -1;

        } else if (selectedRingIndex > holeIndexToRemove) {
            selectedVertexIndex -= pointsInHole; // Adjust global index
        }
         if (selectedVertexIndex >= editingVertices.size() && !editingVertices.isEmpty()) {
            selectedVertexIndex = editingVertices.size() -1;
        } else if (editingVertices.isEmpty()){
            selectedVertexIndex = -1;
        }


        updateSelectedRingAndVertexInRingIndices(selectedVertexIndex);
        updateEditingPolygonAndVertex();
        setMarker(getSelectedPoint());
    }


    @Override
    public void movePointTo(LatLng latLng) {
        if (selectedVertexIndex != -1 && latLng != null) {
            Point newPoint = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
            editingVertices.set(selectedVertexIndex, newPoint);
            updateEditingPolygonAndVertex(); // This will refresh the polygon and middle points
            // Marker is usually handled by the caller of movePointTo, but we can update it here too.
            // if (markerSource != null && getSelectedPoint() != null) {
            //    markerSource.setGeoJson(Feature.fromGeometry(Point.fromLngLat(getSelectedPoint().getLongitude(), getSelectedPoint().getLatitude())));
            // }
        }
    }

    public int getSelectedRingIndex() {
        return selectedRingIndex;
    }



    public void addHole(LatLng center, Projection projection) {

        List<org.maplibre.geojson.Point> result = new ArrayList<>();
        if (projection == null || center == null) {
            // Fallback to a small fixed degree offset if projection or center is not available
            double offsetDegrees = 0.001;
            Point point1Geo = Point.fromLngLat(center != null ? center.getLongitude() - offsetDegrees : -offsetDegrees,
                    center != null ? center.getLatitude() + offsetDegrees : offsetDegrees);
            Point point2Geo = Point.fromLngLat(center != null ? center.getLongitude() + offsetDegrees : offsetDegrees,
                    center != null ? center.getLatitude() - offsetDegrees : -offsetDegrees);

            Point point3Geo = Point.fromLngLat(center != null ? center.getLongitude()  : 0,
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
            PointF screenPoint3 = new PointF(centerScreenCoords.x - pixelOffset, centerScreenCoords.y +  pixelOffset);

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

        addHole(result);

    }
}
