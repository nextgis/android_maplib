package com.nextgis.maplib.map.MLP;

import com.nextgis.maplib.map.MPLFeaturesUtils; // For constants
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.Projection;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Geometry;
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.MultiLineString;
import org.maplibre.geojson.Point;
import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiLineEditClass extends MLGeometryEditClass {
    private List<org.maplibre.geojson.Point> editingVertices = new ArrayList<>();    // all vertices from all lines
    private List<Integer> lineSizes = new ArrayList<>(); // number of vertices in each line
    private List<org.maplibre.geojson.Point> middleVertices = new ArrayList<>();    // middle points
    List<org.maplibre.geojson.Feature> vertexFeatures = new ArrayList<>(); // all features for display (vertices + middle points)

    public MultiLineEditClass(int geoType, GeoJsonSource selectedEditedSource, Feature editingFeature, List<Feature> lineFeatures,
                              GeoJsonSource selectedPolySource, GeoJsonSource vertexSource, GeoJsonSource markerSource) {
        super(geoType, selectedEditedSource, editingFeature, lineFeatures,
                selectedPolySource, vertexSource, markerSource);

        lineFeatures.removeIf(f -> Objects.equals(f.getStringProperty(MPLFeaturesUtils.prop_order), editingFeature.getStringProperty(MPLFeaturesUtils.prop_order)));
        selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(lineFeatures));

        // The main editingFeature for a MultiLineString doesn't usually get a single color here,
        // as its individual lines will be colored based on selection during updateEditingPolygonAndVertex.
        // However, if you need an initial color for the whole geometry before selection, set it.
        // editingFeature.addStringProperty("color", MPLFeaturesUtils.colorLightBlue);
    }

    @Override
    public void extractVertices(org.maplibre.geojson.Feature feature, boolean selectRandomVertex) {
        List<Point> allPoints = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();

        Geometry geometry = feature != null ? feature.geometry() : null;
        if (geometry instanceof MultiLineString) {
            MultiLineString multiLineString = (MultiLineString) geometry;
            List<LineString> lineStrings = multiLineString.lineStrings();
            for (LineString lineString : lineStrings) {
                List<Point> coordinates = lineString.coordinates();
                allPoints.addAll(coordinates);
                sizes.add(coordinates.size());
            }
        }

        editingVertices = new ArrayList<>(allPoints);
        vertexFeatures.clear();
        lineSizes = sizes;

        for (int index = 0; index < editingVertices.size(); index++) {
            Point pt = editingVertices.get(index);
            org.maplibre.geojson.Feature vertexFeat = org.maplibre.geojson.Feature.fromGeometry(pt);
            vertexFeat.addNumberProperty("index", index); // Global index for editingVertices
            vertexFeat.addNumberProperty("radius", MPLFeaturesUtils.pointRaduis);
            vertexFeat.addStringProperty("color", MPLFeaturesUtils.colorLightBlue);
            vertexFeatures.add(vertexFeat);
        }
        displayMiddlePoints(true, true);
    }

    @Override
    public void regenerateVertexFeatures() {
        List<org.maplibre.geojson.Feature> vertexFeaturesTmp =
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

        // Preserve middle points if they exist, only update main vertices
        List<Feature> middlePointFeatures = vertexFeatures.stream()
            .filter(f -> f.hasNonNullValueForProperty("middle"))
            .collect(Collectors.toList());

        vertexFeatures.clear();
        vertexFeatures.addAll(vertexFeaturesTmp);
        vertexFeatures.addAll(middlePointFeatures); // Add back middle points

        vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
    }

    public void generateMiddlePointsAddAndDisplay() {
        // Remove old middle points before generating new ones
        List<org.maplibre.geojson.Feature> mainVertexFeatures = vertexFeatures.stream()
                .filter(f -> !f.hasNonNullValueForProperty("middle"))
                .collect(Collectors.toList());
        vertexFeatures.clear();
        vertexFeatures.addAll(mainVertexFeatures);

        middleVertices.clear();
        int cumulativeIndex = 0;
        int middlePointFeatureIndex = 0; // Separate index for middle points in vertexFeatures list

        for (Integer currentLineSize : lineSizes) {
            for (int i = 0; i < currentLineSize - 1; i++) {
                int globalIndexPt1 = cumulativeIndex + i;
                int globalIndexPt2 = cumulativeIndex + i + 1;

                Point pt1 = editingVertices.get(globalIndexPt1);
                Point pt2 = editingVertices.get(globalIndexPt2);

                double midLat = (pt1.latitude() + pt2.latitude()) / 2.0;
                double midLon = (pt1.longitude() + pt2.longitude()) / 2.0;
                Point middlePoint = Point.fromLngLat(midLon, midLat);
                middleVertices.add(middlePoint);

                org.maplibre.geojson.Feature middleVertexFeature = org.maplibre.geojson.Feature.fromGeometry(middlePoint);
                middleVertexFeature.addNumberProperty("previndex", globalIndexPt1); // Global index of the vertex before this middle point
                middleVertexFeature.addNumberProperty("middleIndex", middlePointFeatureIndex++); // Index within middleVertices list
                middleVertexFeature.addNumberProperty("radius", MPLFeaturesUtils.middleRaduis);
                middleVertexFeature.addStringProperty("color", MPLFeaturesUtils.colorLightBlue);
                middleVertexFeature.addBooleanProperty("middle", true);
                vertexFeatures.add(middleVertexFeature); // Add to the main display list
            }
            cumulativeIndex += currentLineSize;
        }
    }

    @Override
    public void updateSelectionMiddlePoint(Feature point) {
        int prevIndex = point.getNumberProperty("previndex").intValue(); // Global index of vertex before middle point
        int middleListIndex = point.getNumberProperty("middleIndex").intValue(); // Index in middleVertices list

        Point pointToInsert = middleVertices.get(middleListIndex);

        // Insert into editingVertices
        editingVertices.add(prevIndex + 1, pointToInsert);
        selectedVertexIndex = prevIndex + 1;

        // Update lineSizes: find which line this middle point belongs to and increment its size
        int cumulativeSize = 0;
        for (int i = 0; i < lineSizes.size(); i++) {
            int currentLineSize = lineSizes.get(i);
            if (prevIndex >= cumulativeSize && prevIndex < cumulativeSize + currentLineSize -1) { // -1 because prevIndex is before a segment in that line
                lineSizes.set(i, currentLineSize + 1);
                break;
            }
            cumulativeSize += currentLineSize;
        }

        // Rebuild main vertex features list (excluding middle points for now)
        vertexFeatures.clear();
        for (int index = 0; index < editingVertices.size(); index++) {
            Point pt = editingVertices.get(index);
            org.maplibre.geojson.Feature vertexFeat = org.maplibre.geojson.Feature.fromGeometry(pt);
            vertexFeat.addNumberProperty("index", index);
            vertexFeat.addNumberProperty("radius", MPLFeaturesUtils.pointRaduis);
            // Color will be set in displayMiddlePoints or regenerateVertexFeatures
            vertexFeat.addStringProperty("color", index == selectedVertexIndex ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue);
            vertexFeatures.add(vertexFeat);
        }

        displayMiddlePoints(false, true); // This will regenerate middle points and update source
    }

    @Override
    public void displayMiddlePoints(boolean isInit, boolean changeGeoJsonSource) {
        generateMiddlePointsAddAndDisplay(); // This populates middleVertices and adds them to vertexFeatures

        if (isInit) {
            if (!editingVertices.isEmpty()) {
                selectedVertexIndex = 0;
                // Apply color to the initially selected main vertex (if any)
                 vertexFeatures.stream()
                    .filter(f -> !f.hasNonNullValueForProperty("middle") && f.getNumberProperty("index").intValue() == selectedVertexIndex)
                    .findFirst()
                    .ifPresent(f -> f.addStringProperty("color", MPLFeaturesUtils.colorRED));
            } else {
                selectedVertexIndex = -1;
            }
        } else {
            // Ensure correct coloring for the currently selected main vertex
            vertexFeatures.forEach(f -> {
                if (!f.hasNonNullValueForProperty("middle")) {
                    f.addStringProperty("color", f.getNumberProperty("index").intValue() == selectedVertexIndex ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue);
                }
            });
        }

        if (changeGeoJsonSource) {
            vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
        }
    }

    @Override
    public void updateSelectionVerticeIndex(int index) {
        selectedVertexIndex = index; // This is the global index in editingVertices
        // Color update will happen in updateEditingPolygonAndVertex or regenerateVertexFeatures
    }

    @Override
    public void updateSelectionVertice(Point newPoint) {
        if (selectedVertexIndex >= 0 && selectedVertexIndex < editingVertices.size()) {
            editingVertices.set(selectedVertexIndex, newPoint);
        }
    }

    @Override
    public void updateEditingPolygonAndVertex() {
        if (editingVertices.isEmpty()) {
            editingFeature = Feature.fromGeometry(MultiLineString.fromLineStrings(new ArrayList<>()));
            selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
            vertexSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
            return;
        }

        List<LineString> lines = new ArrayList<>();
        int cumulativeIndex = 0;
        int currentSelectedLineIndex = -1; // Index of the line containing the selected vertex

        for (int i = 0; i < lineSizes.size(); i++) {
            int currentLineSize = lineSizes.get(i);
            if (currentLineSize == 0) continue;

            List<Point> linePoints = new ArrayList<>(editingVertices.subList(cumulativeIndex, cumulativeIndex + currentLineSize));
            lines.add(LineString.fromLngLats(linePoints));

            if (selectedVertexIndex >= cumulativeIndex && selectedVertexIndex < cumulativeIndex + currentLineSize) {
                currentSelectedLineIndex = i;
            }
            cumulativeIndex += currentLineSize;
        }

        MultiLineString multiLineString = MultiLineString.fromLineStrings(lines);
        editingFeature = org.maplibre.geojson.Feature.fromGeometry(multiLineString);

        if (originalEditingFeature != null) {
            String order = originalEditingFeature.getStringProperty(MPLFeaturesUtils.prop_order);
            if (order != null) {
                editingFeature.addStringProperty(MPLFeaturesUtils.prop_order, order);
            }
        }

        // For display: color the selected line red, others light blue
        List<Feature> displayLineFeatures = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Feature lineFeature = Feature.fromGeometry(lines.get(i));
            lineFeature.addStringProperty("color", i == currentSelectedLineIndex ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue);
            // Copy other properties if needed, e.g., from originalEditingFeature or a template
            if (originalEditingFeature != null) {
                 String order = originalEditingFeature.getStringProperty(MPLFeaturesUtils.prop_order);
                 if (order != null) lineFeature.addStringProperty(MPLFeaturesUtils.prop_order, order + "_line_" + i); // Ensure unique order if needed
            }
            displayLineFeatures.add(lineFeature);
        }
        selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(displayLineFeatures));

        // Regenerate main vertex features (middle points are handled by displayMiddlePoints)
        List<org.maplibre.geojson.Feature> mainVertexFeatures = IntStream.range(0, editingVertices.size())
            .mapToObj(index -> {
                Point pt = editingVertices.get(index);
                org.maplibre.geojson.Feature f = org.maplibre.geojson.Feature.fromGeometry(pt);
                f.addNumberProperty("index", index);
                f.addNumberProperty("radius", MPLFeaturesUtils.pointRaduis);
                f.addStringProperty("color", index == selectedVertexIndex ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue);
                return f;
            })
            .collect(Collectors.toList());
        
        vertexFeatures.clear();
        vertexFeatures.addAll(mainVertexFeatures);

        displayMiddlePoints(false, false); // Regenerate middle points and add them to vertexFeatures, but don't update source yet

        vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures)); // Now update source with all points
    }

    @Override
    public LatLng getSelectedPoint() {
        if (editingVertices != null && selectedVertexIndex >= 0 && selectedVertexIndex < editingVertices.size()) {
            Point point = editingVertices.get(selectedVertexIndex);
            return new LatLng(point.latitude(), point.longitude());
        }
        return null;
    }

    @Override
    public void deleteCurrentPoint() {
        if (selectedVertexIndex < 0 || selectedVertexIndex >= editingVertices.size()) return;

        int cumulativeSize = 0;
        int lineToRemoveFrom = -1; // This is the index in lineSizes
        int originalLineSizeForSelected = 0;
        int lineStartIndex = 0;


        for (int i = 0; i < lineSizes.size(); i++) {
            int currentLineSize = lineSizes.get(i);
            if (selectedVertexIndex >= cumulativeSize && selectedVertexIndex < cumulativeSize + currentLineSize) {
                lineToRemoveFrom = i;
                originalLineSizeForSelected = currentLineSize;
                lineStartIndex = cumulativeSize;
                break;
            }
            cumulativeSize += currentLineSize;
        }

        if (lineToRemoveFrom != -1 && originalLineSizeForSelected > 2) { // Line must have at least 2 points to remain a line
            editingVertices.remove(selectedVertexIndex);
            lineSizes.set(lineToRemoveFrom, originalLineSizeForSelected - 1);

            // Adjust selectedVertexIndex
            // If the deleted point was the last one in the (now shorter) line, select the new last one.
            if (selectedVertexIndex >= lineStartIndex + lineSizes.get(lineToRemoveFrom)) {
                 selectedVertexIndex = lineStartIndex + lineSizes.get(lineToRemoveFrom) -1;
            }
            // If editingVertices becomes empty (shouldn't happen if originalLineSizeForSelected > 2 and lineSizes wasn't empty)
            else if (editingVertices.isEmpty()) {
                selectedVertexIndex = -1;
            }
            // If the selected index is now out of bounds (e.g. was the last overall point)
            else if (selectedVertexIndex >= editingVertices.size()){
                 selectedVertexIndex = editingVertices.size() -1;
             }
             // Otherwise, the index of the point that took the deleted point's place is fine.

        } else if (lineToRemoveFrom != -1 && originalLineSizeForSelected <= 2) {
            // Prevent deletion if it makes the line invalid (less than 2 points)
            // Or, consider deleting the entire line here as well (see deleteCurrentLine)
            return;
        }
        
        updateEditingPolygonAndVertex();
        LatLng point = getSelectedPoint();
        if (point != null) setMarker(point);
    }

    @Override
    public void movePointTo(LatLng newLatLng) {
        if (editingVertices != null && selectedVertexIndex >= 0 && selectedVertexIndex < editingVertices.size()) {
            Point newGeoPoint = Point.fromLngLat(newLatLng.getLongitude(), newLatLng.getLatitude());
            editingVertices.set(selectedVertexIndex, newGeoPoint);
            
            updateEditingPolygonAndVertex();
            setMarker(newLatLng);
        }
    }

    public void deleteCurrentLine() {
        if (selectedVertexIndex < 0 || selectedVertexIndex >= editingVertices.size() || lineSizes.size() < 2) {
            // Cannot delete if no point is selected, or if there's only one line (or no lines).
            return;
        }

        int lineToDeleteIndex = -1;
        int verticesToRemoveCount = 0;
        int verticesToRemoveStartIndex = 0;
        int cumulativeSize = 0;

        // Find which line the selectedVertexIndex belongs to
        for (int i = 0; i < lineSizes.size(); i++) {
            int currentLineSize = lineSizes.get(i);
            if (selectedVertexIndex >= cumulativeSize && selectedVertexIndex < cumulativeSize + currentLineSize) {
                lineToDeleteIndex = i;
                verticesToRemoveCount = currentLineSize;
                verticesToRemoveStartIndex = cumulativeSize;
                break;
            }
            cumulativeSize += currentLineSize;
        }

        if (lineToDeleteIndex == -1) {
            return; // Should not happen if selectedVertexIndex is valid and lineSizes is not empty
        }

        // Remove the vertices of the identified line
        if (verticesToRemoveCount > 0) {
            editingVertices.subList(verticesToRemoveStartIndex, verticesToRemoveStartIndex + verticesToRemoveCount).clear();
        }
        
        // Remove the size entry for the deleted line
        lineSizes.remove(lineToDeleteIndex);

        // Update selectedVertexIndex to the first point of the first remaining line
        if (editingVertices.isEmpty()) {
            selectedVertexIndex = -1;
        } else {
            selectedVertexIndex = 0; // Select the first point of the (new) first line
        }

        updateEditingPolygonAndVertex();

        LatLng point = getSelectedPoint();
        if (point != null) {
            setMarker(point);
        } else if (markerSource != null) { // Clear marker if no points are left
            markerSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        }
    }

    public void addNewLine(LatLng center, Projection projection) {
        if (projection == null || center == null) {
            // Fallback to a small fixed degree offset if projection or center is not available
            double offsetDegrees = 0.001;
            Point point1Geo = Point.fromLngLat(center != null ? center.getLongitude() - offsetDegrees : -offsetDegrees,
                                             center != null ? center.getLatitude() + offsetDegrees : offsetDegrees);
            Point point2Geo = Point.fromLngLat(center != null ? center.getLongitude() + offsetDegrees : offsetDegrees,
                                             center != null ? center.getLatitude() - offsetDegrees : -offsetDegrees);
            
            int newPointStartIndex = editingVertices.size();
            editingVertices.add(point1Geo);
            editingVertices.add(point2Geo);
            lineSizes.add(2);
            selectedVertexIndex = newPointStartIndex;

            updateEditingPolygonAndVertex();
            LatLng selectedLatLng = getSelectedPoint();
            if (selectedLatLng != null) {
                setMarker(selectedLatLng);
            }
            return;
        }

        org.maplibre.android.geometry.VisibleRegion visibleRegion = projection.getVisibleRegion();
        PointF screenNearLeft = projection.toScreenLocation(visibleRegion.nearLeft);
        PointF screenNearRight = projection.toScreenLocation(visibleRegion.nearRight);

        float screenWidthInPixels = Math.abs(screenNearRight.x - screenNearLeft.x);
        float pixelOffset = screenWidthInPixels * 0.20f;

        PointF centerScreenCoords = projection.toScreenLocation(center);

        PointF screenPoint1 = new PointF(centerScreenCoords.x - pixelOffset, centerScreenCoords.y - pixelOffset);
        PointF screenPoint2 = new PointF(centerScreenCoords.x + pixelOffset, centerScreenCoords.y + pixelOffset);

        LatLng latLng1 = projection.fromScreenLocation(screenPoint1);
        LatLng latLng2 = projection.fromScreenLocation(screenPoint2);

        Point point1 = Point.fromLngLat(latLng1.getLongitude(), latLng1.getLatitude());
        Point point2 = Point.fromLngLat(latLng2.getLongitude(), latLng2.getLatitude());

        int newPointStartIndex = editingVertices.size();

        editingVertices.add(point1);
        editingVertices.add(point2);
        lineSizes.add(2); // New line has 2 points

        selectedVertexIndex = newPointStartIndex; // Select the first point of the new line

        updateEditingPolygonAndVertex();

        LatLng selectedLatLng = getSelectedPoint();
        if (selectedLatLng != null) {
            setMarker(selectedLatLng);
        }
    }
}
