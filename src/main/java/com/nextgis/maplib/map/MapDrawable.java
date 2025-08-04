/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.IMapView;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;
import com.nextgis.maplib.display.GISDisplay;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.MapUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.nextgis.maplib.map.MPLFeaturesUtils.colorLightBlue;
import static com.nextgis.maplib.map.MPLFeaturesUtils.colorRED;
import static com.nextgis.maplib.map.MPLFeaturesUtils.convert4326To3857;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createFeatureListFromLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createFillLayerForLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createSourceForLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeaturePolygon;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getMultiPolygonFeatures;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getPolygonFeatures;
import static com.nextgis.maplib.map.MPLFeaturesUtils.namePrefix;
import static com.nextgis.maplib.map.MPLFeaturesUtils.prop_featureid;
import static com.nextgis.maplib.map.MPLFeaturesUtils.prop_layerid;
import static com.nextgis.maplib.map.MPLFeaturesUtils.prop_order;
import static com.nextgis.maplib.util.Constants.DRAW_FINISH_ID;
import static com.nextgis.maplib.util.Constants.MAP_LIMITS_Y;

import static java.util.Collections.emptyList;

import androidx.annotation.NonNull;

import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.IconFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.expressions.Expression;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.FillLayer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Geometry;
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.Point;
import org.maplibre.geojson.Polygon;

public class MapDrawable
        extends MapEventSource
        implements IMapView,
        View.OnTouchListener,
        MapLibreMap.OnMapLongClickListener,
        MapLibreMap.OnMapClickListener {


    // map  layerID : list of added features for layer
    HashMap<Integer, List<org.maplibre.geojson.Feature>> sourceFeaturesHashMap = new HashMap<Integer, List<org.maplibre.geojson.Feature>>();

    // map sources added to maplibre  from layers
    HashMap<Integer, GeoJsonSource>  sourceHashMap = new HashMap<Integer, GeoJsonSource>();

    // map fill Layer of each added layer
    HashMap<Integer, org.maplibre.android.style.layers.Layer>  layersHashMap = new HashMap<Integer, org.maplibre.android.style.layers.Layer>();

    GeoJsonSource selectedEditedSource = null; // choosed  source - from with edit

    GeoJsonSource selectedPolySource = null; // choosed source of polygon  //
    GeoJsonSource selectedLineSource = null; // choosed source of polygon  //
    GeoJsonSource selectedDotSource = null; // choosed source of polygon  //

    FeatureCollection markerFeatureCollection = null;
    GeoJsonSource markerSource = null;

    //GeoJsonSource editPolySource = null;     // edit layer - of polygon  //
    GeoJsonSource vertexSource = null;      // edit points  //

    List<org.maplibre.geojson.Feature> polygonFeatures = new ArrayList<org.maplibre.geojson.Feature>();  //


    private Feature  originalSelectedFeature = null;            // original who edit

    private MPLFeaturesUtils.MLGeometryEditClass editingObject = null;    // current edit

    private org.maplibre.geojson.Feature  editingFeature = null;    // current edit
    private org.maplibre.geojson.Feature  editingFeatureOriginal = null;
    private org.maplibre.geojson.Feature  viewedFeature = null;   // who looking
    private boolean hasEditeometry = false; // was edit



    private List<Integer> ringSizes = new ArrayList<>();

    private boolean isDragging = false;
    private MotionEvent startEvent = null;
    private PointF deltaPoint = null;


    protected int  mLimitsType;
    protected RunnableFuture<Void> mDrawThreadTask;

    WeakReference<MaplibreMapInteraction> mapFragment = new WeakReference(null);

    WeakReference<MapLibreMap> maplibreMap = new WeakReference(null);
    WeakReference<org.maplibre.android.maps.MapView> maplibreMapView = new WeakReference(null);

    public MapDrawable(
            Bitmap backgroundTile,
            Context context,
            File mapPath,
            LayerFactory layerFactory) {
        super(context, mapPath, layerFactory);

        //initialise display
        mDisplay = new GISDisplay(backgroundTile);
        mLimitsType = MAP_LIMITS_Y;
    }

    public void setMapFragment(MaplibreMapInteraction mapFragment){
        this.mapFragment = new WeakReference<>(mapFragment);
    }

    public void setMaplibreMap(MapLibreMap maplibreMap){
        this.maplibreMap = new WeakReference<>(maplibreMap);
    }

    public void setMaplibreMapView(org.maplibre.android.maps.MapView maplibreMapView){
        this.maplibreMapView = new WeakReference<>(maplibreMapView);
    }

    public MapLibreMap getMaplibreMap(){
        return this.maplibreMap.get();
    }

    public org.maplibre.android.maps.MapView getMaplibreMapView(){
        return this.maplibreMapView.get();
    }

    public void reloadLayerByID(int id){

        List<ILayer> ret = new ArrayList<>();
        LayerGroup.getVectorLayersByType(this, GeoConstants.GTAnyCheck, ret);
        for (ILayer iLayer : ret){
            if (iLayer.getId() == id){
                reloadLayerToMaplibre(iLayer);
                return;
            }
        }
    }

    public void deleteLayerByID(int id){
        if (maplibreMap.get()!= null) {
            String sourceId = namePrefix + "source-" + id;
            String vectorLayerId = namePrefix + "layer-" + id;
            if (maplibreMap.get().getStyle().getSource(sourceId)!= null)
                maplibreMap.get().getStyle().removeSource(sourceId);
            if (maplibreMap.get().getStyle().getLayer(vectorLayerId)!= null)
                maplibreMap.get().getStyle().removeLayer(vectorLayerId);
        }
    }


    public void addLayerByID(int id){
        if (maplibreMap.get()!= null) {


            //List<ILayer> ret = new ArrayList<>();
            //LayerGroup.getVectorLayersByType(this, GeoConstants.GTAnyCheck, ret);
            ILayer iLayer = LayerGroup.getVectorLayersById(this, id);

            if (iLayer == null)
                return;
                    VectorLayer layer = (VectorLayer) iLayer;

                    // this layer
                    List<org.maplibre.geojson.Feature> vectorPolygonFeatures = createFeatureListFromLayer(layer);
                    sourceFeaturesHashMap.put(layer.getId(), vectorPolygonFeatures);
                    Style style = maplibreMap.get().getStyle();

                    Handler mainHandler = new Handler(Looper.getMainLooper());

                    mainHandler.post(() -> {
                        createSourceForLayer(layer.getId(), layer.getGeometryType(), vectorPolygonFeatures, style, sourceHashMap);
                        createFillLayerForLayer(layer.getId(), layer.getGeometryType(), style, layersHashMap);
                    });
        }
    }

    public void reloadLayerToMaplibre(final  ILayer ilayer) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            if (maplibreMap.get() == null || maplibreMapView.get() == null)
                return;
            if (!(ilayer instanceof VectorLayer))
                return;
            VectorLayer layer = (VectorLayer) ilayer;
                if (!layer.isValid()) return;
                if (layer.getGeometryType() == GeoConstants.GTPolygon ||
                        layer.getGeometryType() == GeoConstants.GTMultiPolygon
                ) {
                    List<org.maplibre.geojson.Feature> vectorPolygonFeatures = createFeatureListFromLayer(layer);
                    sourceFeaturesHashMap.put(layer.getId(), vectorPolygonFeatures);
                }

            // Switch to main thread
            mainHandler.post(() -> {
                GeoJsonSource layerSource = sourceHashMap.get(layer.getId());
                List<org.maplibre.geojson.Feature> features = sourceFeaturesHashMap.get(layer.getId());
                if (layerSource != null) {
                    layerSource.setGeoJson(FeatureCollection.fromFeatures(features));
                }
            });
        });
        executor.shutdown();
    }

    public void loadLayersToMaplibreMap(final String styleJson, final  List<ILayer> layers) {

        maplibreMapView.get().setOnTouchListener(this);
        maplibreMap.get().addOnMapClickListener(this);
        maplibreMap.get().addOnMapLongClickListener(this);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        final Map<Integer, Integer> layersType = new HashMap<>();

        executor.execute(() -> {
            if (maplibreMap.get() == null || maplibreMapView.get() == null)
                return;
            // Load layers
            for (ILayer iLayer : layers) {
                if (! (iLayer instanceof VectorLayer))
                    continue;
                VectorLayer layer = (VectorLayer)iLayer;
                if (!layer.isValid())
                    continue;

                layersType.put(layer.getId(), layer.getGeometryType());


                List<org.maplibre.geojson.Feature> vectorPolygonFeatures = createFeatureListFromLayer(layer);
                sourceFeaturesHashMap.put(layer.getId(), vectorPolygonFeatures);

            }

            // Switch to main thread
            mainHandler.post(() -> {
                maplibreMapView.get().addOnDidFinishLoadingStyleListener(new MapView.OnDidFinishLoadingStyleListener() {
                    @Override
                    public void onDidFinishLoadingStyle() {
                        Style style = maplibreMap.get().getStyle();
                        for (Map.Entry<Integer, List<org.maplibre.geojson.Feature>> entry : sourceFeaturesHashMap.entrySet()) {
                             // create source and FillLayer put to style
                            createSourceForLayer(entry.getKey(), layersType.get(entry.getKey()), entry.getValue(), style,sourceHashMap);
                            createFillLayerForLayer(entry.getKey(), layersType.get(entry.getKey()), style,layersHashMap);
                        }

                        // edit layer source
//                        editPolySource = new GeoJsonSource("edit-poly-source", FeatureCollection.fromFeatures(emptyList()));
//                        style.addSource(editPolySource);


//                        FillLayer vectorFillLayer = new FillLayer("edit-polygon-layer", "edit-poly-source")
//                                .withProperties(
//                                        PropertyFactory.fillColor("#00FF00"),
//                                        PropertyFactory.fillOpacity(0.6f)
//                                );
//                        style.addLayer(vectorFillLayer);

                        selectedPolySource = new  GeoJsonSource("selected-poly-source", FeatureCollection.fromFeatures(emptyList()));
                        style.addSource(selectedPolySource);

                        LineLayer lineLayer = new LineLayer("selected-polygon-line", "selected-poly-source")
                                .withProperties(
                                        PropertyFactory.lineColor(Expression.get("color")),
                                        PropertyFactory.lineWidth(2.0f) );
                        style.addLayer(lineLayer);

                        // edit layer source
                        vertexSource = new  GeoJsonSource("vertex-source", FeatureCollection.fromFeatures(emptyList()));
                        style.addSource(vertexSource);

                        CircleLayer vertexFillLayer = new CircleLayer("vertex-layer", "vertex-source")
                                .withProperties(PropertyFactory.circleStrokeWidth(1f),
                                        PropertyFactory.circleStrokeColor("#000000"));
                                vertexFillLayer.setProperties(
                                        PropertyFactory.circleRadius(Expression.get("radius")),
                                        PropertyFactory.circleColor(Expression.get("color")),
                                        PropertyFactory.circleOpacity(1.0f));

                        style.addLayer(vertexFillLayer);
                        // marker
                        final Drawable drawable = getContext().getResources().getDrawable( R.drawable.ic_action_anchor_2);
                        // Преобразуем Drawable в Bitmap
                        final Bitmap bitmap = drawableToBitmap(drawable);

                        // Создаем иконку с помощью IconFactory
                        final IconFactory iconFactory = IconFactory.getInstance(getContext());
                        final Icon markerIcon = iconFactory.fromBitmap(bitmap);
                        String iconId = "marker-icon-selected";
                        style.addImage(iconId, bitmap);


                        // marker layer
                        markerFeatureCollection = FeatureCollection.fromFeatures(new ArrayList<>());
                        markerSource = new GeoJsonSource("marker-source", markerFeatureCollection);
                        style.addSource(markerSource);

                        // Создаем слой SymbolLayer для отображения маркера
                        SymbolLayer symbolLayer = new SymbolLayer("marker-layer", "marker-source")
                                .withProperties(
                                        // Указываем иконку
                                        org.maplibre.android.style.layers.PropertyFactory.iconImage(iconId),
                                        // Устанавливаем точку привязки: "top-left" для левого верхнего угла
                                        org.maplibre.android.style.layers.PropertyFactory.iconAnchor(
                                                org.maplibre.android.style.layers.Property.ICON_ANCHOR_TOP_LEFT
                                        )
                                        // Альтернатива: для смещения иконки ниже/правее используйте:
                                        // .iconAnchor(org.maplibre.android.style.layers.Property.ICON_ANCHOR_BOTTOM_RIGHT)
                                        // или настройте iconOffset для точного смещения, например:
                                        // .iconOffset(new Float[]{10f, 10f}) // Смещение вправо/вниз на 10 пикселей
                                );

                        // Добавляем слой на карту
                        style.addLayer(symbolLayer);


//                        selectedLineSource
//                        selectedDotSource
                    }
                });

                // Set map style
                maplibreMap.get().setStyle(new Style.Builder().fromJson(styleJson));
            });
        });
        executor.shutdown();
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        android.graphics.PointF screenPoint = new android.graphics.PointF(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {

                android.graphics.RectF rect = new android.graphics.RectF(event.getX() - 20,event.getY() - 20,event.getX() + 20,event.getY() + 20);
                List<org.maplibre.geojson.Feature> featuresMarker = maplibreMap.get().queryRenderedFeatures(rect, "marker-layer");

                if (!featuresMarker.isEmpty()){
                    // press marker - lock for future move
                    isDragging = true;
                    startEvent = event;
                    return true;
                }


                // no marker  - check vertex press
                android.graphics.RectF rectVertex = new android.graphics.RectF(event.getX() - 30,event.getY() - 30,event.getX() + 30,event.getY() + 30);
                List<org.maplibre.geojson.Feature> features = maplibreMap.get().queryRenderedFeatures(rectVertex, "vertex-layer");

                if (!features.isEmpty()) {
                    org.maplibre.geojson.Feature clickedFeature = null;
                    // vertes press - change selection
                    int index = -1;

                    if (features.get(0).hasNonNullValueForProperty("index")) {
                        clickedFeature = features.get(0);
                        index = features.get(0).getNumberProperty("index").intValue();
                    }

                    if (index != -1) {
                        if (editingObject != null) {
                            editingObject.updateSelectionVerticeIndex(index);
                            editingObject.updateEditingPolygonAndVertex();
                        }

                        Point point = ((Point)clickedFeature.geometry());
                        setMarker(new LatLng(point.latitude(), point.longitude()));

                        return true;
                    }
                } else { // no select - no touch precess return false
                    return false;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                int selectedVertexIndex = -1;
                if (editingObject != null )
                    selectedVertexIndex = editingObject.getSelectedVertexIndex();
                Log.e("MMVVEE", "ACTION_MOVE");
                Log.e("MMVVEE", "isDragging " + (isDragging?"true" : "false"));
                Log.e("MMVVEE", "selectedVertexIndex " + selectedVertexIndex);
                if (isDragging && selectedVertexIndex != -1) {
                    if (!hasEditeometry) {
                        hasEditeometry = true;
                        mapFragment.get().setHasEdit();
                    }
                    if(deltaPoint == null){
                        if (editingObject != null){
                            LatLng latLng = editingObject.getSelectedPoint();
                            if (latLng != null) {

                                PointF vertex = maplibreMap.get().getProjection().toScreenLocation(latLng);
                                float dx = startEvent.getX() - vertex.x;
                                float dy = startEvent.getY() - vertex.y;
                                deltaPoint = new PointF(dx, dy);
                            }
                        }
                    }
                    PointF  newShiftedPoint = new PointF(screenPoint.x -deltaPoint.x,screenPoint.y - deltaPoint.y );
                    LatLng latLng = maplibreMap.get().getProjection().fromScreenLocation(newShiftedPoint);
                    Point newPoint = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());

                    if (editingObject != null) {
                        Log.e("MMVVEE", "(editingObject != null ");
                        editingObject.updateSelectionVertice(newPoint);
                        editingObject.updateEditingPolygonAndVertex();
                    }
//                    editingVertices.set(selectedVertexIndex, newPoint);
                    if (markerFeatureCollection.features().size() > 0)
                        hideMarker();
                    return true;
                } else {
                    return false;
                }
            }

            case MotionEvent.ACTION_UP: {
                v.performClick();
                v.performLongClick();
                LatLng latLng = maplibreMap.get().getProjection().fromScreenLocation(screenPoint); // todo add tolerance and rect
                double[] points = convert4326To3857(latLng.getLongitude(), latLng.getLatitude());
                mapFragment.get().processMapClick(points[0], points[1]);

                if (isDragging) {
                    if (editingObject != null)
                        mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature);
                    LatLng pointReleased = editingObject.getSelectedPoint();

                    if (pointReleased == null)
                        setMarker(event);
                    else
                        setMarker(pointReleased);
                }

                isDragging = false;
                deltaPoint = null;
                startEvent = null;


                //selectedVertexIndex = -1;
                return false;
            }
        }
        return false;
    }

    public void setMarker(MotionEvent motionEvent){
        android.graphics.PointF screenPoint = new android.graphics.PointF(motionEvent.getX(), motionEvent.getY());
        LatLng latLng = maplibreMap.get().getProjection().fromScreenLocation(screenPoint); // todo add tolerance and rect
        setMarker(latLng);
    }

    public void setMarker(LatLng latLng){
        org.maplibre.geojson.Feature feature = org.maplibre.geojson.Feature.fromGeometry(Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()));
        markerFeatureCollection = FeatureCollection.fromFeature(feature);
        markerSource.setGeoJson(markerFeatureCollection);
    }

    public void hideMarker(){
        markerFeatureCollection = FeatureCollection.fromFeatures(emptyList());
        markerSource.setGeoJson(markerFeatureCollection);
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }


//    private void updateEditingPolygonAndVertex() {
//        int start = 0;
//        List<List<Point>> rings = new ArrayList<>();
//
//        for (Integer size : ringSizes) {
//            List<Point> ring = new ArrayList<>(editingVertices.subList(start, start + size));
//            // close ring
//            ring.add(ring.get(0));
//            rings.add(ring);
//            start += size;
//        }
//
//        Polygon polygon = Polygon.fromLngLats(rings);
//
//        org.maplibre.geojson.Feature feature = org.maplibre.geojson.Feature.fromGeometry(polygon);
//
//        if (editingFeature != null) {
//            String order = editingFeature.getStringProperty(prop_order);
//            feature.addStringProperty(prop_order, order);
//        }
//
//        editingFeature = feature;
//        editPolySource.setGeoJson(feature);
//
//        // outline
//        viewedFeature = feature;
//        selectedPolySource.setGeoJson(viewedFeature);
//
//        // vertex without close point via  Stream
//        List<org.maplibre.geojson.Feature> vertexFeatures =
//                IntStream.range(0, editingVertices.size())
//                        .mapToObj(index -> {
//                            Point pt = editingVertices.get(index);
//                            org.maplibre.geojson.Feature f = org.maplibre.geojson.Feature.fromGeometry(pt);
//                            f.addNumberProperty("index", index);
//                            return f;
//                        })
//                        .collect(Collectors.toList());
//
//        vertexSource.setGeoJson(FeatureCollection.fromFeatures(vertexFeatures));
//    }


    @Override
    public boolean onMapClick(@NonNull LatLng latLng) {

        return false;
    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng latLng) {
        // ask mapFragment for selection// state
        // map

        double[] points = convert4326To3857(latLng.getLongitude(), latLng.getLatitude());
        mapFragment.get().processMapLongClick(points[0], points[1]);
        return false;
    }

    public void startFeatureSelectionForView(Integer layerdID, Feature originalSelectedFeature){
        Long selectedFeatureId = originalSelectedFeature.getId();

        if (editingFeature != null  ){
            Integer lID = Integer.valueOf(editingFeature.getStringProperty(prop_layerid));
            Long fID = Long.valueOf(editingFeature.getStringProperty(prop_featureid));

            if (layerdID.equals(lID) && selectedFeatureId.equals(fID))
                return;
            // need unselect feature
            unselectFeatureFromView();
            return;
        }

        List<org.maplibre.geojson.Feature> layerFeatures = sourceFeaturesHashMap.get(layerdID);

        for (org.maplibre.geojson.Feature item:layerFeatures){
            long id = item.getNumberProperty(prop_featureid).longValue();
            if (id == selectedFeatureId) {
                viewedFeature = item;
                break;
            }
        }

        if (viewedFeature != null) {
            viewedFeature.addStringProperty("color", colorLightBlue);
            selectedPolySource.setGeoJson(viewedFeature);
            this.originalSelectedFeature = originalSelectedFeature;
        }
    }


    public void startFeatureSelectionForEdit(Integer layerdID, Integer layerGeoType,
                                             Feature originalSelectedFeature){

        Long selectedFeatureId = originalSelectedFeature.getId();

        if (editingObject != null) {
            if (editingFeature != null  ){
                Integer lID = Integer.valueOf(editingFeature.getStringProperty(prop_layerid));
                Long fID = Long.valueOf(editingFeature.getStringProperty(prop_featureid));

                if (layerdID.equals(lID) && selectedFeatureId.equals(fID))
                    return;
                // need unselect feature
                unselectFeatureFromEdit(false);
                return;
            }
        }


        List<org.maplibre.geojson.Feature> layerFeatures = sourceFeaturesHashMap.get(layerdID);
        org.maplibre.geojson.Feature  editingFeatureTmp = null;
        for (org.maplibre.geojson.Feature item:layerFeatures){
            long id = item.getNumberProperty(prop_featureid).longValue();
            if (id == selectedFeatureId) {
                editingFeatureTmp = item;
                break;
            }
        }

        if (editingFeatureTmp != null) {
            selectedEditedSource = sourceHashMap.get(layerdID);
            editingFeature = editingFeatureTmp;
            editingFeature.addStringProperty("color", colorRED);
            selectedPolySource.setGeoJson(FeatureCollection.fromFeature(editingFeature));

            editingFeatureOriginal = editingFeatureTmp;
            polygonFeatures = sourceFeaturesHashMap.get(layerdID);
            this.originalSelectedFeature = originalSelectedFeature;

            // choose layer
            editingObject = MPLFeaturesUtils.createEditObject(layerGeoType,
                    selectedEditedSource,
                    editingFeature,
                    polygonFeatures,
                    //editPolySource,
                    selectedPolySource,vertexSource);
            editingObject.extractVertices(editingFeature,  true);

            LatLng selectedPoint = editingObject.getSelectedPoint();
            setMarker(selectedPoint);

        }
    }


    public void deleteFeature(Long selectedFeatureId, int layerdID){

        List<org.maplibre.geojson.Feature> layerFeatures = sourceFeaturesHashMap.get(layerdID);
        org.maplibre.geojson.Feature  editingFeatureTmp = null;
        for (org.maplibre.geojson.Feature item: layerFeatures){
            long id = item.getNumberProperty(prop_featureid).longValue();
            if (id == selectedFeatureId) {
                editingFeatureTmp = item;
                break;
            }
        }

        if (editingFeatureTmp != null) {
            selectedEditedSource = sourceHashMap.get(layerdID);
            editingFeature = editingFeatureTmp;
            editingFeatureOriginal = editingFeatureTmp;
            polygonFeatures = sourceFeaturesHashMap.get(layerdID);

            // choose layer
            // removeIf requires Java 8+
            polygonFeatures.removeIf(f -> Objects.equals(f.getStringProperty(prop_order), editingFeature.getStringProperty(prop_order)));
            selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(polygonFeatures));
        }
        editingFeature = null;
        editingFeatureOriginal = null;
        //editPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
    }

    public void updateMarkerByEditObject(){
        if (editingObject != null && editingObject.getSelectedPoint() != null){



            setMarker(editingObject.getSelectedPoint());
        }



    }

    public void replaceGeometryFromHistoryChanges(GeoGeometry newGeometry){
        int oldIndex = -1;
        if (editingObject != null && editingObject.getSelectedPoint() != null){
            oldIndex = editingObject.getSelectedVertexIndex();
        }

        if  (newGeometry == null)
            return;
        if (newGeometry instanceof  GeoPolygon){
             org.maplibre.geojson.Feature featureML = getFeatureFromNGFeaturePolygon((GeoPolygon)newGeometry);

             if(editingObject  != null && editingObject.editingFeature != null)
                copyProperties(editingObject.editingFeature, featureML);
            //extractVertices(featureML);
            //editPolySource.setGeoJson(featureML);
            selectedPolySource.setGeoJson(featureML);
            editingObject.extractVertices(featureML,  false);
            editingObject.editingFeature = featureML;
        }
        if (oldIndex != -1) {
            editingObject.setSelectedVertexIndex(oldIndex);
            editingObject.updateEditingPolygonAndVertex();
        }
    }

    public void  cancelFeatureEdit(boolean backToOriginal){
        unselectFeatureFromEdit(backToOriginal);
        hideMarker();
    }

    public void unselectFeatureFromView(){
        if (viewedFeature != null) {
            viewedFeature = null;

            selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));

        }
    }


    public void unselectFeatureFromEdit(boolean backToOriginal){
        if (editingObject != null && editingObject.editingFeature != null) {
            copyProperties(editingFeatureOriginal, editingObject.editingFeature);
            hasEditeometry = false;
            polygonFeatures.add(backToOriginal? editingFeatureOriginal : editingObject.editingFeature);
            selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(polygonFeatures));
            selectedPolySource.setGeoJson(backToOriginal? editingFeatureOriginal : editingObject.editingFeature);
            editingObject = null;
            editingFeatureOriginal = null;
            //editPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
            vertexSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
            //editingVertices.clear();
        }
    }

    public void copyProperties(org.maplibre.geojson.Feature from, org.maplibre.geojson.Feature targetFeature){
        JsonObject properties = from.properties();
        if (properties != null) {
            for (String key : properties.keySet()) {
                JsonElement value = properties.get(key);
                if (value.isJsonPrimitive()) {
                    if (value.getAsJsonPrimitive().isString()) {
                        targetFeature.addStringProperty(key, value.getAsString());
                    } else if (value.getAsJsonPrimitive().isNumber()) {
                        targetFeature.addNumberProperty(key, value.getAsNumber());
                    } else if (value.getAsJsonPrimitive().isBoolean()) {
                        targetFeature.addBooleanProperty(key, value.getAsBoolean());
                    }
                } else {
                    targetFeature.addProperty(key, value);
                }
            }
        }
    }




    public void clearMapListeners(){
        maplibreMap.get().removeOnMapClickListener(this);
        maplibreMap.get().removeOnMapLongClickListener(this);
    }



    @Override
    public void draw(
            Canvas canvas,
            boolean clearBackground)    {
        if (mDisplay != null) {
            mDisplay.draw(canvas, clearBackground);
        }
    }


    @Override
    public void draw(
            Canvas canvas,
            float x,
            float y,
            boolean clearBackground)
    {
        if (mDisplay != null) {
            mDisplay.draw(canvas, x, y, clearBackground);
        }
    }


    @Override
    public void draw(
            Canvas canvas,
            float x,
            float y,
            float scale)
    {
        if (mDisplay != null) {
            mDisplay.draw(canvas, x, y, scale);
        }
    }


    @Override
    public void buffer(
            float x,
            float y,
            float scale)
    {
        if (mDisplay != null) {
            mDisplay.buffer(x, y, scale);
        }
    }


    @Override
    public void setViewSize(
            int w,
            int h)
    {
        super.setViewSize(w, h);

        if (mDisplay != null) {
            if(mDisplay.setSize(w, h))
                onExtentChanged((int) mDisplay.getZoomLevel(), mDisplay.getCenter());
        }
    }


    @Override
    public float getZoomLevel()
    {
        if (mDisplay != null) {
            return mDisplay.getZoomLevel();
        }
        return 0;
    }


    /**
     * Set new map extent according zoom level and center
     *
     * @param zoom
     *         A zoom level
     * @param center
     *         A map center coordinates
     */
    @Override
    public void setZoomAndCenter(
            float zoom,
            GeoPoint center)
    {
        if (mDisplay != null) {
            float newZoom = zoom;
            if (zoom < mDisplay.getMinZoomLevel()) {
                newZoom = mDisplay.getMinZoomLevel();
            } else if (zoom > mDisplay.getMaxZoomLevel()) {
                newZoom = mDisplay.getMaxZoomLevel();
            }

            newZoom = Math.round(newZoom);
            mDisplay.setZoomAndCenter(newZoom, center);
            onExtentChanged((int) newZoom, center);
        }
    }

    @Override
    public void zoomToExtent(GeoEnvelope envelope) {
        zoomToExtent(envelope, getMaxZoom());
    }

    public void zoomToExtent(GeoEnvelope envelope, float maxZoom) {
        if (envelope.isInit()) {
            double size = GeoConstants.MERCATOR_MAX * 2;
            double scale = Math.min(envelope.width() / size, envelope.height() / size);
            double zoom = MapUtil.lg(1 / scale);
            if (zoom < getMinZoom())
                zoom = getMinZoom();
            if (zoom > maxZoom)
                zoom = maxZoom;

            setZoomAndCenter((float) zoom, envelope.getCenter());
        }
    }

    @Override
    public GeoPoint getMapCenter()
    {
        if (mDisplay != null) {
            return mDisplay.getCenter();
        }
        return new GeoPoint();
    }


    public GeoEnvelope getFullScreenBounds()
    {
        if (mDisplay != null) {
            return mDisplay.getScreenBounds();
        }
        return null;
    }

    @Override
    public GeoEnvelope getLimits()
    {
        if (mDisplay != null) {
            return mDisplay.getLimits();
        }
        return null;
    }


    @Override
    public void setLimits(
            GeoEnvelope limits,
            int limitsType)
    {
        if (mDisplay != null) {
            mDisplay.setGeoLimits(limits, limitsType);
        }
    }


    @Override
    public GeoPoint screenToMap(GeoPoint pt)
    {
        if (mDisplay != null) {
            return mDisplay.screenToMap(pt);
        }
        return null;
    }


    @Override
    public GeoPoint mapToScreen(GeoPoint pt)
    {
        if (mDisplay != null) {
            return mDisplay.mapToScreen(pt);
        }
        return null;
    }


    @Override
    public float[] mapToScreen(GeoPoint[] geoPoints)
    {
        if (mDisplay != null) {
            return mDisplay.mapToScreen(geoPoints);
        }
        return null;
    }


    @Override
    public GeoEnvelope screenToMap(GeoEnvelope env)
    {
        if (mDisplay != null) {
            return mDisplay.screenToMap(env);
        }
        return null;
    }


    @Override
    public GeoPoint[] screenToMap(float[] points)
    {
        if (mDisplay != null && points != null) {
            return mDisplay.screenToMap(points);
        }
        return new GeoPoint[]{};
    }

    @Override
    public void runDraw(final GISDisplay display)
    {
        try {
            cancelDraw();
        }
        catch (Exception e) {

        }
        onLayerDrawStarted();

        if (null != display && mDisplay != display) {
            mDisplay = display;
        }

        mDisplay.clearLayer();

        mDrawThreadTask = new FutureTask<Void>(
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        android.os.Process.setThreadPriority(
                                Constants.DEFAULT_DRAW_THREAD_PRIORITY);
                        MapDrawable.super.runDraw(mDisplay);
                    }

                }, null)
        {
            @Override
            protected void done()
            {
                super.done();
                if (!isCancelled()) {
                    onDrawFinished(DRAW_FINISH_ID, 1.0f);
                }
                else {
                    onDrawFinished(MapDrawable.this.getId(), 1.0f);
                }
            }
        };

        new Thread(mDrawThreadTask).start();
    }


    @Override
    public void cancelDraw()
    {
        super.cancelDraw();

        FutureTask task = (FutureTask) mDrawThreadTask;
        if (null != task) {
            task.cancel(true);
        }
    }


    @Override
    public float getMaxZoom()
    {
        float mapMax = super.getMaxZoom();
        if (null != mDisplay) {
            float displayMax = mDisplay.getMaxZoomLevel();
            if (displayMax < mapMax) {
                return displayMax;
            }
        }
        return mapMax;
    }


    @Override
    public float getMinZoom()
    {
        float mapMin = super.getMinZoom();
        if (null != mDisplay) {
            float displayMin = mDisplay.getMinZoomLevel();
            if (displayMin > mapMin) {
                return displayMin;
            }
        }
        return mapMin;
    }


    @Override
    public void setMaxZoom(float maxZoom)
    {
        super.setMaxZoom(maxZoom);
        if (mDisplay != null) {
            mDisplay.setMaxZoomLevel(maxZoom);
        }
    }


    @Override
    public void setMinZoom(float minZoom)
    {
        super.setMinZoom(minZoom);
        if (mDisplay != null) {
            mDisplay.setMinZoomLevel(minZoom);
        }
    }


    public void clearBackground(Canvas canvas)
    {
        if (null != mDisplay) {
            mDisplay.clearBackground(canvas);
        }
    }


    public void setBackground(Bitmap background) {
        mDisplay.setBackground(background);
    }


}
