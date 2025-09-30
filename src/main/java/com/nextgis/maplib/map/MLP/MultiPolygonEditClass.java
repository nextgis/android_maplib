package com.nextgis.maplib.map.MLP;

import com.nextgis.maplib.map.MPLFeaturesUtils;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.Projection;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.MultiPolygon;
import org.maplibre.geojson.Point;
import org.maplibre.geojson.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MultiPolygonEditClass extends MLGeometryEditClass {

    // Flat list of all vertices for all rings of all polygons
    protected List<Point> editingVertices = new ArrayList<>(); // Inherited

    List<org.maplibre.geojson.Feature> vertexFeatures = new ArrayList<>();

    // For each ring of each polygon, its end index in editingVertices
    private List<Integer> polygonRingEndIndices = new ArrayList<>();

    // For each polygon, marks the index in polygonRingEndIndices that corresponds to its last ring.
    // e.g., P1 (2 rings), P2 (1 ring) -> multiPolygonRingEndIndicesMarker = [2, 3]
    // (meaning P1's rings are described by polygonRingEndIndices[0] and polygonRingEndIndices[1],
    // P2's ring by polygonRingEndIndices[2])
    private List<Integer> multiPolygonRingEndIndicesMarker = new ArrayList<>();

    // Selection context (derived from selectedVertexIndex)
    private int selectedPolygonIndex = -1;
    private int selectedRingIndexInPolygon = -1; // Ring index within the selectedPolygonIndex
    // 0 -main ring (polygon)  >1 - inner rings (hole
    // )
    private int selectedVertexIndexInRing = -1;  // Vertex index within that ring

    // Middle points: List per polygon -> List per ring -> List of middle Points
    private List<List<List<Point>>> middleVerticesPerPolygonPerRing = new ArrayList<>();

    public MultiPolygonEditClass(int geoType, GeoJsonSource selectedEditedSource, Feature editingFeature, List<Feature> polygonFeatures,
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
    public void extractVertices(Feature feature, boolean selectRandomVertex) {
        editingVertices.clear();
        polygonRingEndIndices.clear();
        multiPolygonRingEndIndicesMarker.clear();
        middleVerticesPerPolygonPerRing.clear();
        selectedVertexIndex = -1;
        selectedPolygonIndex = -1;
        selectedRingIndexInPolygon = -1;
        selectedVertexIndexInRing = -1;

        if (feature == null || !(feature.geometry() instanceof MultiPolygon)) {
            updateEditingPolygonAndVertex(); // Show empty or handle error
            return;
        }

        MultiPolygon multiPolygon = (MultiPolygon) feature.geometry();
        int currentVertexCount = 0;
        int currentRingCountTotal = 0;

        for (Polygon polygon : multiPolygon.polygons()) {
            List<List<Point>> rings = polygon.coordinates(); // Rings for current polygon
            if (rings.isEmpty()) continue;

            for (List<Point> ring : rings) {
                if (ring.size() < 3 && rings.indexOf(ring) == 0) continue; // Invalid outer ring
                if (ring.size() < 3 && rings.indexOf(ring) > 0) continue; // Invalid inner ring

                // GeoJSON spec: first point must be same as last to close ring. We store n-1 points.
                List<Point> uniquePoints = ring.subList(0, ring.size() -1);
                editingVertices.addAll(uniquePoints);
                currentVertexCount += uniquePoints.size();
                polygonRingEndIndices.add(currentVertexCount);
                currentRingCountTotal++;
            }
            if (currentRingCountTotal > ((multiPolygonRingEndIndicesMarker.isEmpty()) ? 0 : multiPolygonRingEndIndicesMarker.get(multiPolygonRingEndIndicesMarker.size()-1))) {
                 multiPolygonRingEndIndicesMarker.add(currentRingCountTotal);
            }
        }

        if (selectRandomVertex && !editingVertices.isEmpty()) {
            updateDerivedSelectionIndices(0);
        } else {
            updateDerivedSelectionIndices(-1);
        }
        updateEditingPolygonAndVertex();
    }

    private void updateDerivedSelectionIndices(int globalIndex) {
        if (globalIndex == -1 || editingVertices.isEmpty() || polygonRingEndIndices.isEmpty() || multiPolygonRingEndIndicesMarker.isEmpty()) {
            selectedPolygonIndex = -1;
            selectedRingIndexInPolygon = -1;
            selectedVertexIndexInRing = -1;
            selectedVertexIndex = -1;
            return;
        }

        selectedVertexIndex = globalIndex;
        int ringStartGlobalIndex = 0; // The start index in editingVertices for the current ring

        int processedRingsOverall = 0;
        for (int pIdx = 0; pIdx < multiPolygonRingEndIndicesMarker.size(); pIdx++) {
            int endRingMarkerForThisPolygon = multiPolygonRingEndIndicesMarker.get(pIdx);
            // Index for polygonRingEndIndices where rings for *this* polygon start
            int startRingIndexInPREI = (pIdx == 0) ? 0 : multiPolygonRingEndIndicesMarker.get(pIdx - 1);

            for (int rIdxInMarker = startRingIndexInPREI; rIdxInMarker < endRingMarkerForThisPolygon; rIdxInMarker++) {
                int ringEndGlobalIndex = polygonRingEndIndices.get(rIdxInMarker); // End index in editingVertices for this ring
                if (globalIndex < ringEndGlobalIndex) {
                    selectedPolygonIndex = pIdx;
                    selectedRingIndexInPolygon = rIdxInMarker - startRingIndexInPREI;
                    selectedVertexIndexInRing = globalIndex - ringStartGlobalIndex;
                    return;
                }
                ringStartGlobalIndex = ringEndGlobalIndex; // Next ring starts where this one ended
            }
        }
        // Fallback if something is inconsistent
        selectedPolygonIndex = -1;
        selectedRingIndexInPolygon = -1;
        selectedVertexIndexInRing = -1;
    }

    @Override
    public void updateEditingPolygonAndVertex() {
        List<Polygon> polygons = new ArrayList<>();
        int currentVertexOffset = 0;
        int currentRingOffsetInPRI = 0; // PRI = polygonRingEndIndices

        for (int pIdx = 0; pIdx < multiPolygonRingEndIndicesMarker.size(); pIdx++) {
            List<List<Point>> currentPolygonRings = new ArrayList<>();
            int endRingMarkerForThisPolygon = multiPolygonRingEndIndicesMarker.get(pIdx);
            
            for (int rIdx = currentRingOffsetInPRI; rIdx < endRingMarkerForThisPolygon; rIdx++) {
                List<Point> ringPoints = new ArrayList<>(editingVertices.subList(currentVertexOffset, polygonRingEndIndices.get(rIdx)));
                if (!ringPoints.isEmpty()) {
                    ringPoints.add(ringPoints.get(0)); // Close the ring for GeoJSON
                    currentPolygonRings.add(ringPoints);
                }
                currentVertexOffset = polygonRingEndIndices.get(rIdx);
            }

            if (!currentPolygonRings.isEmpty()) {
                polygons.add(Polygon.fromLngLats(currentPolygonRings));
            }
            currentRingOffsetInPRI = endRingMarkerForThisPolygon;
        }

        MultiPolygon multiPolygon = MultiPolygon.fromPolygons(polygons);

        Feature newFeature = Feature.fromGeometry(multiPolygon);

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

        setMarker(getSelectedPoint());
    }

    @Override
    public void displayMiddlePoints(boolean isInit, boolean changeGeoJsonSource) {
        vertexFeatures.clear();
        middleVerticesPerPolygonPerRing.clear();

        if (editingVertices.isEmpty()) {
            if (changeGeoJsonSource && vertexSource != null) {
                vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
            }
            return;
        }

        int currentVertexGlobalOffset = 0;
        for (int pIdx = 0; pIdx < multiPolygonRingEndIndicesMarker.size(); pIdx++) {
            middleVerticesPerPolygonPerRing.add(new ArrayList<>()); // List of rings for this polygon
            int startRingIndexForCurrentPolygon = (pIdx == 0) ? 0 : multiPolygonRingEndIndicesMarker.get(pIdx-1);
            int endRingIndexForCurrentPolygon = multiPolygonRingEndIndicesMarker.get(pIdx);

            for (int rRingIdx = startRingIndexForCurrentPolygon; rRingIdx < endRingIndexForCurrentPolygon; rRingIdx++) {
                middleVerticesPerPolygonPerRing.get(pIdx).add(new ArrayList<>()); // List of middle points for this ring

                int ringStartIndex = currentVertexGlobalOffset;
                int ringEndIndex = polygonRingEndIndices.get(rRingIdx);
                List<Point> currentRingMainPoints = editingVertices.subList(ringStartIndex, ringEndIndex);

                if (currentRingMainPoints.isEmpty()) {
                     currentVertexGlobalOffset = ringEndIndex;
                     continue;
                }
                
                int currentRingNumberInPolygon = rRingIdx - startRingIndexForCurrentPolygon;

                // Main vertices for the current ring
                for (int vIdxInRing = 0; vIdxInRing < currentRingMainPoints.size(); vIdxInRing++) {
                    Point pt = currentRingMainPoints.get(vIdxInRing);
                    Feature vertexFeature = Feature.fromGeometry(pt);
                    vertexFeature.addNumberProperty("radius", MPLFeaturesUtils.pointRaduis);
                    vertexFeature.addNumberProperty("polygonIndex", pIdx);
                    vertexFeature.addNumberProperty("ringIndexInPolygon", currentRingNumberInPolygon);
                    vertexFeature.addNumberProperty("vertexIndexInRing", vIdxInRing);
                    vertexFeature.addNumberProperty("index", ringStartIndex + vIdxInRing); // Global index

                    boolean isSelected = (pIdx == selectedPolygonIndex && 
                                          currentRingNumberInPolygon == selectedRingIndexInPolygon &&
                                          vIdxInRing == selectedVertexIndexInRing);
                    // Highlight selected polygon's selected ring
                     String color = (pIdx == selectedPolygonIndex && currentRingNumberInPolygon == selectedRingIndexInPolygon) ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue;
                    if (isSelected) color = MPLFeaturesUtils.colorRED; // ensure selected vertex is red
                    vertexFeature.addStringProperty("color", color);
                    vertexFeatures.add(vertexFeature);
                }

                // Middle points for the current ring
                List<Point> currentRingMiddlePointsList = middleVerticesPerPolygonPerRing.get(pIdx).get(currentRingNumberInPolygon);
                for (int vIdxInRing = 0; vIdxInRing < currentRingMainPoints.size(); vIdxInRing++) {
                    Point pt1 = currentRingMainPoints.get(vIdxInRing);
                    Point pt2 = currentRingMainPoints.get((vIdxInRing + 1) % currentRingMainPoints.size());

                    Point middleP = Point.fromLngLat((pt1.longitude() + pt2.longitude()) / 2.0, (pt1.latitude() + pt2.latitude()) / 2.0);
                    currentRingMiddlePointsList.add(middleP);

                    Feature middleFeature = Feature.fromGeometry(middleP);
                    middleFeature.addBooleanProperty("middle", true);
                    middleFeature.addNumberProperty("polygonIndex", pIdx);
                    middleFeature.addNumberProperty("ringIndexInPolygon", currentRingNumberInPolygon);
                    // prevVertexIndexInRing is the index of the vertex this middle point *follows*
                    middleFeature.addNumberProperty("prevVertexIndexInRing", vIdxInRing); 
                    middleFeature.addNumberProperty("radius", MPLFeaturesUtils.middleRaduis);
                    String color = (pIdx == selectedPolygonIndex && currentRingNumberInPolygon == selectedRingIndexInPolygon) ? MPLFeaturesUtils.colorRED : MPLFeaturesUtils.colorLightBlue;
                    middleFeature.addStringProperty("color", color);
                    vertexFeatures.add(middleFeature);
                }
                currentVertexGlobalOffset = ringEndIndex;
            }
        }
        
        if (isInit && selectedVertexIndex == -1 && !editingVertices.isEmpty()) {
             updateDerivedSelectionIndices(0); // Select first vertex
        }


        if (changeGeoJsonSource && vertexSource != null) {
            vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
        }
    }

    @Override
    public void updateSelectionMiddlePoint(Feature middlePointFeature) {
        if (middlePointFeature == null || !middlePointFeature.hasProperty("middle") ||
            !middlePointFeature.getBooleanProperty("middle")) {
            return;
        }

        int pIdx = middlePointFeature.getNumberProperty("polygonIndex").intValue();
        int rIdxInP = middlePointFeature.getNumberProperty("ringIndexInPolygon").intValue();
        int prevVIdxInR = middlePointFeature.getNumberProperty("prevVertexIndexInRing").intValue();
        Point newVertexPoint = (Point) middlePointFeature.geometry();

        // Calculate global start index of the ring this middle point belongs to
        int ringGlobalStartIndex = 0;
        int targetRingIndexInPRI = -1; // Actual index in polygonRingEndIndices

        int startRingMarkerForPolygon = (pIdx == 0) ? 0 : multiPolygonRingEndIndicesMarker.get(pIdx - 1);
        targetRingIndexInPRI = startRingMarkerForPolygon + rIdxInP;
        
        if (targetRingIndexInPRI == 0) {
            ringGlobalStartIndex = 0;
        } else {
            ringGlobalStartIndex = polygonRingEndIndices.get(targetRingIndexInPRI -1);
        }
        
        int insertionPointInFlatList = ringGlobalStartIndex + prevVIdxInR + 1;

        editingVertices.add(insertionPointInFlatList, newVertexPoint);

        // Update polygonRingEndIndices for all subsequent rings
        for (int i = targetRingIndexInPRI; i < polygonRingEndIndices.size(); i++) {
            polygonRingEndIndices.set(i, polygonRingEndIndices.get(i) + 1);
        }
        // multiPolygonRingEndIndicesMarker does not need to change as number of rings per polygon is same.
        // (This changes if a point deletion removes a ring)

        updateDerivedSelectionIndices(insertionPointInFlatList);
        updateEditingPolygonAndVertex();
        // setMarker is called in updateEditingPolygonAndVertex
    }
    
    @Override
    public void deleteCurrentPoint() {
        if (selectedVertexIndex == -1 || editingVertices.isEmpty()) return;

        int pIdx = selectedPolygonIndex;
        int rIdxInP = selectedRingIndexInPolygon;

        // Determine the actual index in polygonRingEndIndices for the current ring
        int startRingMarkerForSelectedPolygon = (pIdx == 0) ? 0 : multiPolygonRingEndIndicesMarker.get(pIdx - 1);
        int currentRingAbsoluteIndexInPRI = startRingMarkerForSelectedPolygon + rIdxInP;

        int ringStartGlobal = (currentRingAbsoluteIndexInPRI == 0) ? 0 : polygonRingEndIndices.get(currentRingAbsoluteIndexInPRI - 1);
        int ringEndGlobal = polygonRingEndIndices.get(currentRingAbsoluteIndexInPRI);
        int pointsInCurrentRing = ringEndGlobal - ringStartGlobal;

        if (pointsInCurrentRing < 4 ) // no delete ring by point - min 3 points
            return;

        boolean removingOuterRing = (rIdxInP == 0);
        boolean ringBecomesInvalid = (pointsInCurrentRing - 1 < 3);

        if (removingOuterRing && ringBecomesInvalid) {
            // Removing a point from outer ring makes it invalid. Delete the whole polygon.
            deletePolygon(pIdx); // This will handle selection update.
            return;
        }
        editingVertices.remove(selectedVertexIndex);

        // Update polygonRingEndIndices for this ring and all subsequent rings
        for (int i = currentRingAbsoluteIndexInPRI; i < polygonRingEndIndices.size(); i++) {
            polygonRingEndIndices.set(i, polygonRingEndIndices.get(i) - 1);
        }
        
        // Check if the current ring (not outer) became invalid and needs removal
        if (!removingOuterRing && ringBecomesInvalid) {
            // Remove the now-empty or invalid hole ring's points (already done by global list modification)
            // And remove its entry from polygonRingEndIndices & adjust multiPolygonRingEndIndicesMarker
            polygonRingEndIndices.remove(currentRingAbsoluteIndexInPRI);
            for (int i = pIdx; i < multiPolygonRingEndIndicesMarker.size(); i++) {
                 multiPolygonRingEndIndicesMarker.set(i, multiPolygonRingEndIndicesMarker.get(i) - 1);
            }
             // If a polygon now has zero rings after hole removal (should not happen if outer still exists)
            if (multiPolygonRingEndIndicesMarker.get(pIdx) <= ((pIdx == 0) ? 0 : multiPolygonRingEndIndicesMarker.get(pIdx-1))) {
                 deletePolygon(pIdx); // This polygon is now empty/invalid
                 return;
            }
            selectedVertexIndex = (ringStartGlobal > 0) ? ringStartGlobal -1 : 0; // Select last point of previous ring or first overall
            if (editingVertices.isEmpty()) selectedVertexIndex = -1;

        } else {
            // Point removed, ring still valid (or outer ring that became smaller but still valid)
            if (selectedVertexIndex >= editingVertices.size() && !editingVertices.isEmpty()) {
                selectedVertexIndex = editingVertices.size() - 1;
            } else if (editingVertices.isEmpty()) {
                selectedVertexIndex = -1;
            }
            // If selectedVertexIndex was the one removed, it effectively points to the next one
            // or end of list. No change needed to selectedVertexIndex unless it's out of bounds.
        }
        
        if (editingVertices.isEmpty()) { // All polygons might have been deleted
             clear();
        } else {
            updateDerivedSelectionIndices(selectedVertexIndex);
        }
        updateEditingPolygonAndVertex();
    }

    public void addNewPolygonAt(LatLng mapCenter, Projection projection) {
        // Create a default square polygon
        List<List<Point>> newPolyRings = new ArrayList<>();
        List<Point> outerRing = prepareNewPolyPoints(mapCenter, projection);

        newPolyRings.add(outerRing);

        int newPolygonStartIndex = editingVertices.size();
        int newPolygonRingCount = 0;

        for(List<Point> ring : newPolyRings) {
            editingVertices.addAll(ring);
            polygonRingEndIndices.add(editingVertices.size());
            newPolygonRingCount++;
        }
        
        if (newPolygonRingCount > 0) {
            if (multiPolygonRingEndIndicesMarker.isEmpty()) {
                multiPolygonRingEndIndicesMarker.add(newPolygonRingCount);
            } else {
                multiPolygonRingEndIndicesMarker.add(multiPolygonRingEndIndicesMarker.get(multiPolygonRingEndIndicesMarker.size()-1) + newPolygonRingCount);
            }
            selectedPolygonIndex = multiPolygonRingEndIndicesMarker.size() - 1; // last polygon
            selectedRingIndexInPolygon = 0; // first ring
            selectedVertexIndexInRing = 0; // first vertex
            selectedVertexIndex = newPolygonStartIndex;
            updateDerivedSelectionIndices(selectedVertexIndex);
        }
        updateEditingPolygonAndVertex();
    }

    private void deleteRing(int pIdxToDeleteRingFrom, int rIdxInPToDelete) {
        // Basic checks: must be a valid polygon index, and rIdxInPToDelete must be > 0 (an inner ring).
        if (pIdxToDeleteRingFrom < 0 || pIdxToDeleteRingFrom >= multiPolygonRingEndIndicesMarker.size() ||
                rIdxInPToDelete <= 0) {
            return;
        }

        int startRingMarkerForPolygon = (pIdxToDeleteRingFrom == 0) ? 0 : multiPolygonRingEndIndicesMarker.get(pIdxToDeleteRingFrom - 1);
        int numRingsInThisPolygon = multiPolygonRingEndIndicesMarker.get(pIdxToDeleteRingFrom) - startRingMarkerForPolygon;

        // Check if rIdxInPToDelete is a valid ring index within this polygon
        if (rIdxInPToDelete >= numRingsInThisPolygon) {
            return;
        }

        int absoluteRingIndexInPRI = startRingMarkerForPolygon + rIdxInPToDelete;

        int ringStartGlobal = (absoluteRingIndexInPRI == 0) ? 0 : polygonRingEndIndices.get(absoluteRingIndexInPRI - 1);
        int ringEndGlobal = polygonRingEndIndices.get(absoluteRingIndexInPRI);
        int numVerticesInDeletedRing = ringEndGlobal - ringStartGlobal;

        if (numVerticesInDeletedRing <= 0) { // Should not happen for a valid ring
            return;
        }

        // 1. Remove vertices of the specified ring
        editingVertices.subList(ringStartGlobal, ringEndGlobal).clear();

        // 2. Update polygonRingEndIndices
        // First, remove the entry for the deleted ring
        polygonRingEndIndices.remove(absoluteRingIndexInPRI);
        // Then, adjust all subsequent end indices by the number of vertices removed
        for (int i = absoluteRingIndexInPRI; i < polygonRingEndIndices.size(); i++) {
            polygonRingEndIndices.set(i, polygonRingEndIndices.get(i) - numVerticesInDeletedRing);
        }

        // 3. Update multiPolygonRingEndIndicesMarker
        // The polygon pIdxToDeleteRingFrom now has one less ring.
        // Adjust its marker and all subsequent polygon markers.
        for (int i = pIdxToDeleteRingFrom; i < multiPolygonRingEndIndicesMarker.size(); i++) {
            multiPolygonRingEndIndicesMarker.set(i, multiPolygonRingEndIndicesMarker.get(i) - 1);
        }

        // If all vertices were somehow removed (e.g. this was the only content), clear everything.
        if (editingVertices.isEmpty()) {
            clear();
            updateEditingPolygonAndVertex(); // To refresh UI
            return;
        }

        // 4. Update selection: Default to the first vertex of the outer ring of the modified polygon.
        // The current polygon (pIdxToDeleteRingFrom) is still the selected one.
        // Select its outer ring (index 0) and first vertex (index 0).
        this.selectedPolygonIndex = pIdxToDeleteRingFrom;
        this.selectedRingIndexInPolygon = 0;
        this.selectedVertexIndexInRing = 0;

        int globalStartIndexOfOuterRing;
        // Calculate the global start index for the (newly selected) first vertex of the outer ring.
        int firstRingStartMarkerForPolygonInPRI = (this.selectedPolygonIndex == 0) ? 0 : multiPolygonRingEndIndicesMarker.get(this.selectedPolygonIndex - 1);

        if (firstRingStartMarkerForPolygonInPRI == 0) {
            // This means the outer ring of the selected polygon is the very first ring overall.
            // Its points start at index 0 in editingVertices.
            globalStartIndexOfOuterRing = 0;
        } else {
            // The points for this outer ring start after the points of the ring that comes just before it
            // in the polygonRingEndIndices list.
            globalStartIndexOfOuterRing = polygonRingEndIndices.get(firstRingStartMarkerForPolygonInPRI - 1);
        }

        // Safety check in case the polygon became empty or calculation is off
        if (globalStartIndexOfOuterRing >= editingVertices.size() && !editingVertices.isEmpty()) {
            globalStartIndexOfOuterRing = 0; // Fallback to the very first point
        }

        if (editingVertices.isEmpty()) { // Re-check after potential fallback
            updateDerivedSelectionIndices(-1);
        } else {
            updateDerivedSelectionIndices(globalStartIndexOfOuterRing);
        }

        updateEditingPolygonAndVertex();
    }


    public void addHole(LatLng center, Projection projection) {
        addHole(prepareNewPolyPoints(center, projection));
    }


    public void addHole(List<org.maplibre.geojson.Point> holePoints) {
        if (holePoints == null || holePoints.size() < 3) {
            return;
        }
        if (selectedPolygonIndex == -1) {
            return;
        }

        int pIdx = selectedPolygonIndex;


        // Determine where the new ring's information (its end vertex count) will be inserted in polygonRingEndIndices.
        // This is effectively after all existing rings of the selected polygon pIdx.
        int absoluteRingInsertionIndexInPRI = multiPolygonRingEndIndicesMarker.get(pIdx);

        // Determine where the new hole's vertices will be inserted in editingVertices.
        // This is after all vertices of all existing rings of the selected polygon.
        // It's the cumulative vertex count of the ring just before where this new ring is being inserted.
        int vertexDataInsertionGlobalIndex;
        if (absoluteRingInsertionIndexInPRI == 0) {
            // This implies inserting the very first ring overall (pIdx = 0, no rings yet for it).
            // But the ringsInSelectedPolygon check above should prevent adding a hole in this scenario.
            // If it were an outer ring, this would be 0.
            vertexDataInsertionGlobalIndex = 0; // Should be guarded by ringsInSelectedPolygon check
        } else {
            // The new hole's vertices are inserted after the vertices of the
            // ring whose end index is at polygonRingEndIndices.get(absoluteRingInsertionIndexInPRI - 1).
            vertexDataInsertionGlobalIndex = polygonRingEndIndices.get(absoluteRingInsertionIndexInPRI - 1);
        }

        // 1. Add holePoints to editingVertices
        editingVertices.addAll(vertexDataInsertionGlobalIndex, holePoints);
        int numAddedPoints = holePoints.size();

        // 2. Update polygonRingEndIndices
        //    a. Insert the new hole's cumulative vertex count.
        int newHoleEndVertexCount = vertexDataInsertionGlobalIndex + numAddedPoints;
        polygonRingEndIndices.add(absoluteRingInsertionIndexInPRI, newHoleEndVertexCount);
        //    b. Adjust subsequent cumulative vertex counts.
        for (int i = absoluteRingInsertionIndexInPRI + 1; i < polygonRingEndIndices.size(); i++) {
            polygonRingEndIndices.set(i, polygonRingEndIndices.get(i) + numAddedPoints);
        }

        // 3. Update multiPolygonRingEndIndicesMarker
        //    The selected polygon (pIdx) and all subsequent polygons now effectively have one more ring
        //    associated with them in the polygonRingEndIndices list.
        for (int i = pIdx; i < multiPolygonRingEndIndicesMarker.size(); i++) {
            multiPolygonRingEndIndicesMarker.set(i, multiPolygonRingEndIndicesMarker.get(i) + 1);
        }

        // 4. Update selection to the first point of the newly added hole.
        // The global index of the first vertex of the new hole is vertexDataInsertionGlobalIndex.
        // updateDerivedSelectionIndices will correctly set selectedPolygonIndex,
        // selectedRingIndexInPolygon, and selectedVertexIndexInRing.
        updateDerivedSelectionIndices(vertexDataInsertionGlobalIndex);


        // 5. Refresh map display
        updateEditingPolygonAndVertex();
        if (getSelectedPoint() != null) {
            setMarker(getSelectedPoint());
        } else if (!editingVertices.isEmpty()) {
            // Fallback if somehow selection is lost but vertices exist
            updateDerivedSelectionIndices(0); // Select first overall point
            if (getSelectedPoint() != null) setMarker(getSelectedPoint());
        }
    }

    public void deleteSelectedPolygon() {
        if (selectedPolygonIndex == -1 || multiPolygonRingEndIndicesMarker.isEmpty() || selectedRingIndexInPolygon == -1) {
            // No valid selection to delete
            return;
        }

        if (selectedRingIndexInPolygon > 0) {
            // An inner ring (hole) is selected, so delete only the hole.
            // selectedPolygonIndex is the polygon, selectedRingIndexInPolygon is the hole's index within that polygon.
            deleteRing(selectedPolygonIndex, selectedRingIndexInPolygon);
        } else {
            // The outer ring (selectedRingIndexInPolygon == 0) of the polygon is selected.
            // This means the user wants to delete the entire polygon.

            // Check if this is the last polygon in the MultiPolygon.
            if (multiPolygonRingEndIndicesMarker.size() == 1) {
                // It's the last polygon. As per requirement, do not delete it.
                // You might want to show a message to the user here (e.g., Toast).

                return;
            }
            // It's not the last polygon, or deleting the last one is allowed (not the case here).
            // Proceed to delete the entire polygon.
            deletePolygon(selectedPolygonIndex); // Calls the existing method to delete the whole polygon
        }
    }


    private void deletePolygon(int pIdxToDelete) {
        if (pIdxToDelete < 0 || pIdxToDelete >= multiPolygonRingEndIndicesMarker.size()) return;

        int startRingMarkerForPoly = (pIdxToDelete == 0) ? 0 : multiPolygonRingEndIndicesMarker.get(pIdxToDelete - 1);
        int endRingMarkerForPoly = multiPolygonRingEndIndicesMarker.get(pIdxToDelete);
        int ringsInThisPolygon = endRingMarkerForPoly - startRingMarkerForPoly;

        int verticesStartGlobal = (startRingMarkerForPoly == 0) ? 0 : polygonRingEndIndices.get(startRingMarkerForPoly - 1);
        int verticesEndGlobal = polygonRingEndIndices.get(endRingMarkerForPoly - 1); // Last point of last ring of this polygon

        // Remove vertices
        if (verticesStartGlobal < verticesEndGlobal) { // Check to avoid issues if list is manipulated
             editingVertices.subList(verticesStartGlobal, verticesEndGlobal).clear();
        }

        // Remove ring end indices for this polygon
        if (startRingMarkerForPoly < endRingMarkerForPoly) {
            polygonRingEndIndices.subList(startRingMarkerForPoly, endRingMarkerForPoly).clear();
        }
        
        // Update subsequent polygonRingEndIndices by the number of vertices removed
        int numVerticesRemoved = verticesEndGlobal - verticesStartGlobal;
        for (int i = startRingMarkerForPoly; i < polygonRingEndIndices.size(); i++) {
             polygonRingEndIndices.set(i, polygonRingEndIndices.get(i) - numVerticesRemoved);
        }

        // Remove marker for this polygon and adjust subsequent markers
        multiPolygonRingEndIndicesMarker.remove(pIdxToDelete);
        for (int i = pIdxToDelete; i < multiPolygonRingEndIndicesMarker.size(); i++) {
            multiPolygonRingEndIndicesMarker.set(i, multiPolygonRingEndIndicesMarker.get(i) - ringsInThisPolygon);
        }

        if (editingVertices.isEmpty()) {
            clear();
        } else {
            // Try to select previous polygon's first point, or next, or first overall
            if (pIdxToDelete > 0) { // Try previous
                 selectedPolygonIndex = pIdxToDelete -1;
            } else if (!multiPolygonRingEndIndicesMarker.isEmpty()) { // Try next (now at pIdxToDelete)
                 selectedPolygonIndex = pIdxToDelete; // (which is now the one after the deleted one)
            } else {
                 selectedPolygonIndex = -1; // No polygons left
            }

            if(selectedPolygonIndex != -1) {
                int newSelectedGlobalIndex = ( ( (selectedPolygonIndex == 0) ? 0 : multiPolygonRingEndIndicesMarker.get(selectedPolygonIndex -1) ) == 0 ) ? 0 : polygonRingEndIndices.get( ((selectedPolygonIndex == 0) ? 0 : multiPolygonRingEndIndicesMarker.get(selectedPolygonIndex -1)) -1 );
                updateDerivedSelectionIndices(newSelectedGlobalIndex);
            } else {
                 updateDerivedSelectionIndices(-1);
            }
        }
        updateEditingPolygonAndVertex();
    }


    // TODO: Implement addHoleToSelectedPolygon, removeHoleFromSelectedPolygon
    // These would be similar to PolygonEditClass but operate on selectedPolygonIndex's data.

    @Override
    public void movePointTo(LatLng point) {
        if (selectedVertexIndex != -1 && selectedVertexIndex < editingVertices.size()) {
            Point newPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
            editingVertices.set(selectedVertexIndex, newPoint);
            updateEditingPolygonAndVertex();
        }
    }

    @Override
    public LatLng getSelectedPoint() {
        if (selectedVertexIndex != -1 && selectedVertexIndex < editingVertices.size()) {
            Point point = editingVertices.get(selectedVertexIndex);
            if (point != null) {
                return new LatLng(point.latitude(), point.longitude());
            }
        }
        return null;
    }


    public void clear() {
        polygonRingEndIndices.clear();
        multiPolygonRingEndIndicesMarker.clear();
        middleVerticesPerPolygonPerRing.clear();
        selectedPolygonIndex = -1;
        selectedRingIndexInPolygon = -1;
        selectedVertexIndexInRing = -1;
        if (selectedPolySource != null) selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        if (vertexSource != null) vertexSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        if (markerSource != null) markerSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));

    }

    @Override
    public void updateSelectionVerticeIndex(int index) {
            updateDerivedSelectionIndices(index);
            updateEditingPolygonAndVertex(); // To update highlighting and marker
    }

    @Override
    public void updateSelectionVertice(Point newPoint) {
            if (selectedVertexIndex >= 0 && selectedVertexIndex < editingVertices.size()) {
                editingVertices.set(selectedVertexIndex, newPoint);
                updateEditingPolygonAndVertex();
            }
    }

    public int getSelectedRingIndexInPolygon(){
        return selectedRingIndexInPolygon;
    }

    public int getSelectedPolygonIndex(){
        return selectedPolygonIndex;
    }
}
