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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.IMapView;
import com.nextgis.maplib.api.ITextStyle;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoMultiLineString;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoMultiPolygon;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.Connections;
import com.nextgis.maplib.display.GISDisplay;
import com.nextgis.maplib.map.MLP.LineEditClass;
import com.nextgis.maplib.map.MLP.MLGeometryEditClass;
import com.nextgis.maplib.map.MLP.MultiLineEditClass;
import com.nextgis.maplib.map.MLP.MultiPointEditClass;
import com.nextgis.maplib.map.MLP.MultiPolygonEditClass;
import com.nextgis.maplib.map.MLP.PointEditClass;
import com.nextgis.maplib.map.MLP.PolygonEditClass;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.MapUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nextgis.maplib.map.MLP.MultiLineEditClass.getNewLinePoints;
import static com.nextgis.maplib.map.MLP.PolygonEditClass.createPointsForRing;
import static com.nextgis.maplib.map.MPLFeaturesUtils.colorBlue;
import static com.nextgis.maplib.map.MPLFeaturesUtils.colorLightBlue;
import static com.nextgis.maplib.map.MPLFeaturesUtils.colorRED;
import static com.nextgis.maplib.map.MPLFeaturesUtils.convert4326To3857;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createFeatureListFromLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createFeatureListFromTrackLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createFillLayerForLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createSourceForLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getColorName;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeatureLine;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeatureMultiLine;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeatureMultiPoint;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeatureMultiPolygon;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeaturePoint;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeaturePolygon;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getRasterLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getTypePrefix;
import static com.nextgis.maplib.map.MPLFeaturesUtils.id_name;
import static com.nextgis.maplib.map.MPLFeaturesUtils.layer_namepart;
import static com.nextgis.maplib.map.MPLFeaturesUtils.namePrefix;
import static com.nextgis.maplib.map.MPLFeaturesUtils.prop_featureid;
import static com.nextgis.maplib.map.MPLFeaturesUtils.prop_layerid;
import static com.nextgis.maplib.map.MPLFeaturesUtils.prop_order;
import static com.nextgis.maplib.map.MPLFeaturesUtils.prop_signature_text;
import static com.nextgis.maplib.map.MPLFeaturesUtils.source_namepart;
import static com.nextgis.maplib.util.Constants.DRAW_FINISH_ID;
import static com.nextgis.maplib.util.Constants.MAP_LIMITS_Y;
import static com.nextgis.maplib.util.GeoConstants.GTLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiLineString;
import static com.nextgis.maplib.util.GeoConstants.GTNone;
import static com.nextgis.maplib.util.GeoConstants.GT_RASTER_WA;
import static com.nextgis.maplib.util.GeoConstants.GT_TRACK_WA;
import static com.nextgis.maplib.util.NetworkUtil.extractResourceValue;
import static com.nextgis.maplib.util.NetworkUtil.fillConnections;
import static com.nextgis.maplib.util.NetworkUtil.getBaseUrlpart;
import static com.nextgis.maplib.util.NetworkUtil.getHTTPBaseAuth;
import static org.maplibre.android.style.layers.Property.NONE;
import static org.maplibre.android.style.layers.Property.VISIBLE;
import static org.maplibre.android.style.layers.PropertyFactory.visibility;
import static java.util.Collections.emptyList;
import androidx.annotation.NonNull;
import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.IconFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Projection;
import org.maplibre.android.maps.Style;
import org.maplibre.android.module.http.HttpRequestImpl;
import org.maplibre.android.style.expressions.Expression;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.FillLayer;
import org.maplibre.android.style.layers.Layer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.Property;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.RasterLayer;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.android.style.sources.RasterSource;
import org.maplibre.android.style.sources.VectorSource;
import org.maplibre.android.util.DefaultStyle;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.MultiLineString;
import org.maplibre.geojson.MultiPoint;
import org.maplibre.geojson.MultiPolygon;
import org.maplibre.geojson.Point;
import org.maplibre.geojson.Polygon;

public class MapDrawable
        extends MapEventSource
        implements IMapView,
        View.OnTouchListener,
        MapLibreMap.OnMapLongClickListener,
        MapLibreMap.OnMapClickListener {

    static int testColor = 0;

    // map  layerID : list of added features for layer
    HashMap<Integer, List<org.maplibre.geojson.Feature>> sourceFeaturesHashMap = new HashMap<Integer, List<org.maplibre.geojson.Feature>>();

    // map sources added to maplibre  from layers
    HashMap<Integer, GeoJsonSource>  sourceHashMap = new HashMap<Integer, GeoJsonSource>();

    // map fill Layer of each added layer
    HashMap<Integer, org.maplibre.android.style.layers.Layer>  layersHashMap = new HashMap<Integer, org.maplibre.android.style.layers.Layer>();

    // outline for polygone
    HashMap<Integer, org.maplibre.android.style.layers.Layer>  layersHashMap2 = new HashMap<Integer, org.maplibre.android.style.layers.Layer>();

    // Symbols for geometry signature
    HashMap<Integer, org.maplibre.android.style.layers.Layer>  symbolsLayerHashMap = new HashMap<Integer, org.maplibre.android.style.layers.Layer>();

    GeoJsonSource selectedEditedSource = null; // choosed  source - from with edit (selectable)
    GeoJsonSource selectedPolySource = null; // choosed source of polygon/line  //
    GeoJsonSource selectedDotSource = null; // choosed source of polygon  //
    GeoJsonSource vertexSource = null;      // edit points  //

    FillLayer fillPolyEditLayer = null; // fill poly on edit layer (while on move points)

    FeatureCollection markerFeatureCollection = FeatureCollection.fromFeatures(new ArrayList<>());
    GeoJsonSource markerSource = null; // marker source - select point

    GeoJsonSource locationSource = null;

    List<org.maplibre.geojson.Feature> polygonFeatures = new ArrayList<org.maplibre.geojson.Feature>();  //

    PointF clickPoint = null;

    private Feature  originalSelectedFeature = null;            // original who edit

    private MLGeometryEditClass editingObject = null;    // current edit

    private org.maplibre.geojson.Feature  editingFeature = null;    // current edit
    private org.maplibre.geojson.Feature  editingFeatureOriginal = null;
    private org.maplibre.geojson.Feature  viewedFeature = null;   // who looking
    private boolean hasEditeometry = false; // was edit

    private boolean isDragging = false;
    private boolean isSwitchVertex = false;
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


    // change feature id at map objects - features // objects
    public void changeFeatureId(Long oldFeatureId,Long newFeatureId, int layerId){

//        Log.e("CCACHHEE", "changeFeatureId  oldFeatureId, newFeatureId,layerId: " + oldFeatureId + " " +newFeatureId + " " + layerId );
//        Log.e("CCACHHEE", "print  sourceFeaturesHashMap---------" );

        for (Map.Entry<Integer, List<org.maplibre.geojson.Feature>> entry : sourceFeaturesHashMap.entrySet()){
//            Log.e("CCACHHEE", "print  sourceFeaturesHashMap for entry:" + entry.getKey() );
            for (org.maplibre.geojson.Feature feature : entry.getValue()){
//                Log.e("CCACHHEE", "feature id " + feature.getStringProperty(prop_featureid) );
            }
        }

//        Log.e("CCACHHEE", "print  polygonFeatures" );
        for (org.maplibre.geojson.Feature feature : polygonFeatures){
//            Log.e("CCACHHEE", "feature id " + feature.getStringProperty(prop_featureid) );
        }

//        Log.e("CCACHHEE", "end print ------- " );



        String oldFeatureIdString = String.valueOf(oldFeatureId);
        List<org.maplibre.geojson.Feature> layerFeatures = sourceFeaturesHashMap.get(layerId);
        for (org.maplibre.geojson.Feature feature : layerFeatures){
            if (feature.getStringProperty(prop_featureid).equals(oldFeatureIdString)) {
//                Log.e("CCACHHEE", "sourceFeaturesHashMap change featureid at  " + feature.toJson() );
                feature.addStringProperty(prop_featureid, String.valueOf(newFeatureId));
                break;// only one feature with same id
            }
        }

        for (org.maplibre.geojson.Feature feature : polygonFeatures){
            if (feature.getStringProperty(prop_featureid).equals(oldFeatureIdString)) {
//                Log.e("CCACHHEE", "polygonFeatures change featureid at  " + feature.toJson() );
                feature.addStringProperty(prop_featureid, String.valueOf(newFeatureId));
                break;// only one feature with same id
            }
        }
    }

    public void deleteLayerByID(int id){
        if (maplibreMap.get()!= null) {
            String sourceId = namePrefix + source_namepart + id;
            String vectorLayerId = namePrefix + layer_namepart + id;
            if (maplibreMap.get().getStyle().getSource(sourceId)!= null)
                maplibreMap.get().getStyle().removeSource(sourceId);
            if (maplibreMap.get().getStyle().getLayer(vectorLayerId)!= null)
                maplibreMap.get().getStyle().removeLayer(vectorLayerId);
        }
    }


    public void addLayerByID(int id){
        if (maplibreMap.get()!= null) {

            ILayer iLayer = LayerGroup.getVectorLayersById(this, id);

            if (iLayer == null)
                return;

            final AccountManager accountManager = AccountManager.get(getContext());
            final Connections connections = fillConnections(getContext(), accountManager);

            if (iLayer instanceof  NGWRasterLayer){
                // need add auth
                Connection found = null;
                if (iLayer instanceof NGWRasterLayer) {
                    for (int i = 0; i < connections.getChildrenCount(); i++) {
                        if (connections.getChild(i).getName().equals((((NGWRasterLayer) iLayer).getAccountName()))) {
                            found = (Connection) connections.getChild(i);
                            String basicAuth = getHTTPBaseAuth(found.getLogin(), found.getPassword());
                            if (null != basicAuth) {
                                final String url = ((NGWRasterLayer) iLayer).getURL();
                                final String getBaseUrl =  getBaseUrlpart(url);
                                final String resPart = "resource=" + extractResourceValue(url);

//                                Log.d("UURR", "layer part: " + url);
//                                Log.d("UURR", "res part: " + resPart);
//                                Log.d("UURR", "getBaseUrl part: " + getBaseUrl);

                                final String [] authPart = new String[3];
                                authPart[0] = getBaseUrl;
                                authPart[1] = resPart;
                                authPart[2] = basicAuth;
                                ((IGISApplication)getContext().getApplicationContext()).updateAuthPair( authPart);
                                break;
                            }
                        }
                    }
                }
            }



            int geoType = GTNone;
            final List<org.maplibre.geojson.Feature> vectorPolygonFeatures = new ArrayList<>();
            Map<Integer, String> rasterLayersURL = new HashMap<>();
            com.nextgis.maplib.display.Style ngStyle = null;

            if (iLayer instanceof VectorLayer) {
                VectorLayer layer = (VectorLayer) iLayer;
                geoType = layer.getGeometryType();
                // this layer
                vectorPolygonFeatures.addAll(createFeatureListFromLayer(layer));
                sourceFeaturesHashMap.put(layer.getId(), vectorPolygonFeatures);
                ngStyle = ((VectorLayer)iLayer).getDefaultStyleNoExcept();
            } else if (iLayer instanceof NGWRasterLayer){
                geoType = GT_RASTER_WA;
                NGWRasterLayer layer = (NGWRasterLayer) iLayer;
                rasterLayersURL.put(layer.getId(), ((NGWRasterLayer)layer).getURL());
            } else if (iLayer instanceof RemoteTMSLayer){
                geoType = GT_RASTER_WA;
                RemoteTMSLayer layer = (RemoteTMSLayer) iLayer;
                rasterLayersURL.put(layer.getId(), (layer).getURLSubdomain());
            }


            Style style = maplibreMap.get().getStyle();
            final int finalGeoType = geoType;
            final com.nextgis.maplib.display.Style finalStyle = ngStyle;


            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                createSourceForLayer(iLayer.getId(), finalGeoType, vectorPolygonFeatures, style, sourceHashMap, rasterLayersURL);
                createFillLayerForLayer(iLayer.getId(), finalGeoType, style, layersHashMap, layersHashMap2,
                        symbolsLayerHashMap,
                        finalStyle, false);
                checkLayerVisibility(iLayer.getId());
            });
        }
    }

    public void reloadFillLayerStyleToMaplibre(final  int  id) {

        if (!(mapFragment.get().getMode() == 0))
            return;

        List<ILayer> ret = new ArrayList<>();
        LayerGroup.getVectorLayersByType(this, GeoConstants.GTAnyCheck, ret);
        for (ILayer iLayer : ret){
            if (iLayer.getId() == id){
                com.nextgis.maplib.display.Style newStyle = ((VectorLayer)iLayer).getDefaultStyleNoExcept();
                Style maplbrStyle = maplibreMap.get().getStyle();

                createFillLayerForLayer(id, ((VectorLayer) iLayer).getGeometryType(),maplbrStyle ,layersHashMap,layersHashMap2,
                        symbolsLayerHashMap,
                        newStyle, true);
                checkLayerVisibility(id);
                reloadLayerToMaplibre(iLayer);
                return;
            }
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
            //if (!layer.isValid()) return;

            List<org.maplibre.geojson.Feature> vectorPolygonFeatures = createFeatureListFromLayer(layer);
            sourceFeaturesHashMap.put(layer.getId(), vectorPolygonFeatures);

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

    public void loadLayersToMaplibreMap(final String styleJson,
                                        final  List<ILayer> vectorLayers,
                                        final  List<ILayer> tmsLayers ) {

        maplibreMapView.get().setOnTouchListener(this);
        maplibreMap.get().addOnMapClickListener(this);
        maplibreMap.get().addOnMapLongClickListener(this);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        final Map<Integer, Integer> layersType = new HashMap<>();
        final Map<Integer, com.nextgis.maplib.display.Style> layersStyle = new HashMap<>();
        final Map<Integer, String> rasterLayersURL = new HashMap<>();

        executor.execute(() -> {
            if (maplibreMap.get() == null || maplibreMapView.get() == null)
                return;
            // Load layers
            for (ILayer iLayer : vectorLayers) {
                if (! (iLayer instanceof VectorLayer || iLayer instanceof TrackLayer))
                    continue;

                if (iLayer instanceof VectorLayer) {
                    VectorLayer layer = (VectorLayer) iLayer;
                    layersType.put(layer.getId(), layer.getGeometryType());
                    layersStyle.put(layer.getId(), layer.getDefaultStyleNoExcept());
                    List<org.maplibre.geojson.Feature> vectorFeatures = createFeatureListFromLayer(layer);
                    sourceFeaturesHashMap.put(layer.getId(), vectorFeatures);
                } else if (iLayer instanceof TrackLayer) {
                    TrackLayer layer = (TrackLayer) iLayer;
                    layersType.put(layer.getId(), GT_TRACK_WA);
                    List<org.maplibre.geojson.Feature> tracksFeatures = createFeatureListFromTrackLayer(layer);
                    sourceFeaturesHashMap.put(layer.getId(), tracksFeatures);
                }
            }


            final AccountManager accountManager = AccountManager.get(getContext());
            final Connections connections = fillConnections(getContext(), accountManager);

            for (ILayer iLayer : tmsLayers) {
                if (! (iLayer instanceof TMSLayer))
                    continue;


                if (!(iLayer instanceof NGWRasterLayer)
                        && !(iLayer instanceof RemoteTMSLayer))
                    continue;

                TMSLayer layer = (TMSLayer)iLayer;

                if (iLayer instanceof  NGWRasterLayer){
                    // need add auth
                    Connection found = null;
                    if (iLayer instanceof NGWRasterLayer) {
                        for (int i = 0; i < connections.getChildrenCount(); i++) {
                            if (connections.getChild(i).getName().equals((((NGWRasterLayer) iLayer).getAccountName()))) {
                                found = (Connection) connections.getChild(i);
                                String basicAuth = getHTTPBaseAuth(found.getLogin(), found.getPassword());
                                if (null != basicAuth) {
                                    final String url = ((NGWRasterLayer) iLayer).getURL();
                                    final String getBaseUrl =  getBaseUrlpart(url);
                                    final String resPart = "resource=" + extractResourceValue(url);

//                                    Log.d("UURR", "layer part: " + url);
//                                    Log.d("UURR", "res part: " + resPart);
//                                    Log.d("UURR", "getBaseUrl part: " + getBaseUrl);

                                    final String [] authPart = new String[3];
                                    authPart[0] = getBaseUrl;
                                    authPart[1] = resPart;
                                    authPart[2] = basicAuth;
                                    ((IGISApplication)getContext().getApplicationContext()).updateAuthPair(authPart);
                                    break;
                                }
                            }
                        }
                    }
                }

                layersType.put(layer.getId(), GT_RASTER_WA);
                if (layer instanceof  NGWRasterLayer)
                    rasterLayersURL.put(layer.getId(), ((NGWRasterLayer) layer).getURL());
                else if (layer instanceof  RemoteTMSLayer)
                    rasterLayersURL.put(layer.getId(), ((RemoteTMSLayer) layer).getURLSubdomain());
                sourceFeaturesHashMap.put(layer.getId(), new ArrayList<>());
            }

            // Switch to main thread
            mainHandler.post(() -> {
                maplibreMapView.get().addOnDidFinishLoadingStyleListener(new MapView.OnDidFinishLoadingStyleListener() {
                    @Override
                    public void onDidFinishLoadingStyle() {
                        Style style = maplibreMap.get().getStyle();
                        for (Map.Entry<Integer, List<org.maplibre.geojson.Feature>> entry : sourceFeaturesHashMap.entrySet()) {
                            // create source and FillLayer put to style
                            createSourceForLayer(entry.getKey(), layersType.get(entry.getKey()),
                                    entry.getValue(), style,
                                    sourceHashMap, rasterLayersURL);

                            createFillLayerForLayer(entry.getKey(), layersType.get(entry.getKey()),
                                    style,
                                    layersHashMap,
                                    layersHashMap2,
                                    symbolsLayerHashMap,
                                    layersStyle.get(entry.getKey()), false);

                            checkLayerVisibility(entry.getKey());
                        }



                        // PMTILES example
//                        // raster PMTILES
//                        String pmTilesPath = "pmtiles://file:///storage/emulated/0/Android/data/com.nextgis.mobile.debug/files/map/flowers.pmtiles";
//
//                        RasterSource source = new RasterSource("raster-pmtiles-source", pmTilesPath);
//                        style.addSource(source);
//
//                        RasterLayer layer = new RasterLayer("raster-pmtiles-layer", "raster-pmtiles-source");
//                        style.addLayer(layer);
//                        // END   PMTILES
//
//
//                        // PMTILES
//                        String pmTilesPath2 = "pmtiles://file:///storage/emulated/0/Android/data/com.nextgis.mobile.debug/files/map/cb_2018_us_zcta510_500k.pmtiles";
//
//                        VectorSource source2 = new VectorSource("pmtiles-source", pmTilesPath2);
//                        style.addSource(source2);
//
//                        LineLayer layer2 = new LineLayer("pmtiles-layer", "pmtiles-source")
//                                .withSourceLayer("zcta")
//                                .withProperties(
//                                PropertyFactory.lineColor("#0000ff"),
//                                PropertyFactory.lineWidth(2f)
//                        );
//                        style.addLayer(layer2);
//
//                        FillLayer fillLayer = new FillLayer("poly-pmtiles-layer", "pmtiles-source")
//                                .withSourceLayer("zcta")
//                                .withProperties(
//                                        PropertyFactory.fillColor("#FF0000"),
//                                        PropertyFactory.fillOpacity(0.5f));
//                        style.addLayer(fillLayer);
//                        // END   PMTILES

                        selectedDotSource = new  GeoJsonSource("selected-dot-source", FeatureCollection.fromFeatures(emptyList()));
                        style.addSource(selectedDotSource);

                        CircleLayer selectedDotCircleLayer = new CircleLayer("selected-dot-layer", "selected-dot-source")
                                .withProperties(PropertyFactory.circleStrokeWidth(1f),
                                        PropertyFactory.circleStrokeColor("#000000"));
                        selectedDotCircleLayer.setProperties(
                                PropertyFactory.circleRadius(Expression.get("radius")),
                                PropertyFactory.circleColor(Expression.get("color")),
                                PropertyFactory.circleOpacity(1.0f));
                        style.addLayer(selectedDotCircleLayer);

                        selectedPolySource = new  GeoJsonSource("selected-poly-source", FeatureCollection.fromFeatures(emptyList()));
                        style.addSource(selectedPolySource);

                        LineLayer lineLayer = new LineLayer("selected-polygon-line", "selected-poly-source")
                                .withProperties(
                                        PropertyFactory.lineColor(Expression.get("color")),
                                        PropertyFactory.lineWidth(2.0f) );
                        style.addLayer(lineLayer);

                        fillPolyEditLayer = new FillLayer("selected-polygon-fill" ,"selected-poly-source" )
                                .withProperties(
                                        PropertyFactory.fillColor("#FF00FF"),
                                        PropertyFactory.fillOpacity(0.2f));
//                        style.addLayer(fillLayer);

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
                        final Bitmap bitmap = drawableToBitmap(drawable);

                        final IconFactory iconFactory = IconFactory.getInstance(getContext());
                        final Icon markerIcon = iconFactory.fromBitmap(bitmap);
                        String iconId = "marker-icon-selected";
                        style.addImage(iconId, bitmap);

                        // marker layer
                        markerFeatureCollection = FeatureCollection.fromFeatures(new ArrayList<>());
                        markerSource = new GeoJsonSource("marker-source", markerFeatureCollection);
                        style.addSource(markerSource);

                        SymbolLayer symbolLayer = new SymbolLayer("marker-layer", "marker-source")
                                .withProperties(
                                        org.maplibre.android.style.layers.PropertyFactory.iconImage(iconId),
                                        org.maplibre.android.style.layers.PropertyFactory.iconAnchor(
                                                org.maplibre.android.style.layers.Property.ICON_ANCHOR_TOP_LEFT));
                        style.addLayer(symbolLayer);

                        locationSource = new GeoJsonSource("user-location-source", Point.fromLngLat(0.0, 0.0));
                        style.addSource(locationSource);


                        final Drawable drawableStand = getContext().getResources().getDrawable( R.drawable.ic_location_standing);
                        final Bitmap bitmapStand = drawableToBitmap(drawableStand);
                        String iconStandId = "user-marker-location-stand";
                        style.addImage(iconStandId, bitmapStand);

                        final Drawable drawableGo = getContext().getResources().getDrawable( R.drawable.ic_location_moving);
                        final Bitmap bitmapGo = drawableToBitmap(drawableGo);
                        String iconGoId = "user-marker-location-go";
                        style.addImage(iconGoId, bitmapGo);



                        SymbolLayer locationLayer = new SymbolLayer("user-location-layer", "user-location-source")
                                .withProperties(
                                        PropertyFactory.iconImage(
                                            Expression.switchCase(
                                                    Expression.eq(Expression.get("type"), Expression.literal("stand")), Expression.literal("user-marker-location-stand"),
                                                    Expression.eq(Expression.get("type"), Expression.literal("go")), Expression.literal("user-marker-location-go"),
                                                    Expression.literal("user-marker-location-stand"))),
                                        PropertyFactory.iconRotate(Expression.get("bearing")),
                                        PropertyFactory.iconSize(1.0f),
                                        PropertyFactory.iconAllowOverlap(true),
                                        PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP)

                                );
                        style.addLayer(locationLayer);

                    }
                });

//                // Set map style MPLB
//                DefaultStyle[] styles = Style.getPredefinedStyles();
//                if (styles != null && styles.length > 0) {
//                    String styleUrl = styles[0].getUrl();
//                    maplibreMap.get().setStyle(new Style.Builder().fromUri(styleUrl));
//                }

                // Set map style NG
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
                clickPoint = new PointF(event.getX(), event.getY());
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

                    // check for middle point click
                    if (features.get(0).hasNonNullValueForProperty("middle")) {
                        isSwitchVertex = true;
                        // need add point
                        //int previndex = features.get(0).getNumberProperty("previndex").intValue();
                        clickedFeature = features.get(0);
                        //index = features.get(0).getNumberProperty("index").intValue();

                        if (editingObject != null) {
                            editingObject.updateSelectionMiddlePoint(features.get(0));
                            //editingObject.updateSelectionVerticeIndex(index);
                            editingObject.updateEditingPolygonAndVertex();
                        }
                        Point point = ((Point)clickedFeature.geometry());
                        setMarker(new LatLng(point.latitude(), point.longitude()));

                        return true;
                    }

                    if (features.get(0).hasNonNullValueForProperty("index")) {
                        clickedFeature = features.get(0);
                        index = features.get(0).getNumberProperty("index").intValue();
                    }

                    if (index != -1) {
                        if (editingObject != null) {
                            editingObject.updateSelectionVerticeIndex(index);
                            editingObject.updateEditingPolygonAndVertex();
                            editingObject.displayMiddlePoints(false, true);
                            mapFragment.get().updateActions(editingObject);
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
                if (isDragging && selectedVertexIndex != -1) {
                    if (!hasEditeometry) {
                        hasEditeometry = true;
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
                    if(deltaPoint == null){
                        return true;
                    }
                    PointF  newShiftedPoint = new PointF(screenPoint.x -deltaPoint.x,screenPoint.y - deltaPoint.y );
                    LatLng latLng = maplibreMap.get().getProjection().fromScreenLocation(newShiftedPoint);
                    Point newPoint = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());

                    if (editingObject != null) {
                        editingObject.updateSelectionVertice(newPoint);
                        editingObject.updateEditingPolygonAndVertex();
                    }
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
                float deltaX = clickPoint.x - event.getX();
                float deltaY = clickPoint.y - event.getY();
                float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                if (!isDragging)
                    if (distance < 5)
                        mapFragment.get().processMapClick(screenPoint.x, screenPoint.y);
                clickPoint = null;

                if (isDragging || isSwitchVertex ) {
                    if (editingObject != null) {
                        mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
                        editingObject.regenerateVertexFeatures();
                        editingObject.displayMiddlePoints(false, true);
                        LatLng pointReleased = editingObject.getSelectedPoint();

                        if (pointReleased != null)
                            setMarker(pointReleased);
                    } else
                        setMarker(event);
                }
                isDragging = false;
                isSwitchVertex = false;
                deltaPoint = null;
                startEvent = null;
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


    @Override
    public boolean onMapClick(@NonNull LatLng latLng) {

        return false;
    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng latLng) {
        // ask mapFragment for selection// state
        // map
        PointF clickPoint = maplibreMap.get().getProjection().toScreenLocation(latLng);
        GeoEnvelope clickeEnelope = getClickEnelope(clickPoint);

        mapFragment.get().processMapLongClick(clickeEnelope, clickPoint);
        return false;
    }

    public GeoEnvelope getClickEnelope(PointF clickPoint){
        int TOLERANCE_DP       = 20;
        float mTolerancePX = getContext().getResources().getDisplayMetrics().density * TOLERANCE_DP;

        PointF minP = new PointF(clickPoint.x - mTolerancePX,clickPoint.y - mTolerancePX);
        PointF maxP = new PointF(clickPoint.x + mTolerancePX,clickPoint.y + mTolerancePX);

        LatLng minL = maplibreMap.get().getProjection().fromScreenLocation(minP);
        LatLng maxL = maplibreMap.get().getProjection().fromScreenLocation(maxP);

        double[] minPoints = convert4326To3857(minL.getLongitude(), minL.getLatitude());
        double[] maxPoints = convert4326To3857(maxL.getLongitude(), maxL.getLatitude());

        var minx =   minPoints[0];
        var maxx =   maxPoints[0];
        var miny =   minPoints[1];
        var maxy =   maxPoints[1];

        if (minx > maxx){
            minx =   maxPoints[0];
            maxx =   minPoints[0];
        }
        if (miny > maxy){
            miny =   maxPoints[1];
            maxy =   minPoints[1];
        }

        //val exactEnv: GeoEnvelope = GeoEnvelope(pointsMin[0],  pointsMax[0], pointsMin[1], pointsMax[1])
        GeoEnvelope exactEnv  = new  GeoEnvelope(minx, maxx, miny, maxy);
        return exactEnv;
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

        for (org.maplibre.geojson.Feature item : layerFeatures){
            long id = item.getNumberProperty(prop_featureid).longValue();
            if (id == selectedFeatureId) {
                viewedFeature = item;
                break;
            }
        }

        if (viewedFeature != null) {

            var featureSelected = copyFeature(viewedFeature);
            featureSelected.addStringProperty("color", colorLightBlue);

            int type = ((VectorLayer)getLayer(layerdID)).getGeometryType();

            if  (type == GeoConstants.GTPoint || type == GeoConstants.GTMultiPoint) {
                selectedDotSource.setGeoJson(featureSelected);
            }

            if  (type == GeoConstants.GTPolygon || type == GeoConstants.GTMultiPolygon
                    || type == GTLineString ||type == GTMultiLineString ) {
                selectedPolySource.setGeoJson(featureSelected);
            }

            this.originalSelectedFeature = originalSelectedFeature;
        }
    }


    public void startFeatureSelectionForEdit(Integer layerdID, Integer layerGeoType,
                                             Feature originalSelectedFeature, boolean createNew,
                                             com.nextgis.maplib.display.Style ngstyle){

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
        if (layerFeatures != null)
            for (org.maplibre.geojson.Feature item:layerFeatures){
                long id = item.getNumberProperty(prop_featureid).longValue();
                if (id == selectedFeatureId) {
                    editingFeatureTmp = item;
                    break;
                }
            }

        if (createNew) {
            org.maplibre.geojson.Feature feature = null;
            int type = ((VectorLayer)getLayer(layerdID)).getGeometryType();

            LatLng center = maplibreMap.get().getCameraPosition().target;
            Projection projection = maplibreMap.get().getProjection();
            Point point = Point.fromLngLat(center.getLongitude(), center.getLatitude());

            switch (type){
                case GeoConstants.GTPoint :
                    feature = org.maplibre.geojson.Feature.fromGeometry(point);
                    break;

                case GeoConstants.GTMultiPoint:
                    MultiPoint mpoint = MultiPoint.fromLngLats(Arrays.asList(point));
                    feature = org.maplibre.geojson.Feature.fromGeometry(mpoint);
                    break;

                case GeoConstants.GTPolygon:
                    List<List<org.maplibre.geojson.Point>> polyList = new ArrayList<>();
                    polyList.add(createPointsForRing(center, maplibreMap.get().getProjection(),  true));
                    Polygon polygon = Polygon.fromLngLats(polyList);
                    feature = org.maplibre.geojson.Feature.fromGeometry(polygon);
                    break;

                case GeoConstants.GTMultiPolygon:
                    List<List<org.maplibre.geojson.Point>> polyListMP = new ArrayList<>();
                    polyListMP.add(createPointsForRing(center, maplibreMap.get().getProjection(),  true));
                    MultiPolygon polygonMP = MultiPolygon.fromLngLats(Arrays.asList(polyListMP));
                    feature = org.maplibre.geojson.Feature.fromGeometry(polygonMP);
                    break;

                case GTLineString:
                    List<org.maplibre.geojson.Point> lineList = getNewLinePoints(center, projection);
                    LineString line = LineString.fromLngLats(lineList);
                    feature = org.maplibre.geojson.Feature.fromGeometry(line);
                    break;

                case GTMultiLineString:
                    List<org.maplibre.geojson.Point> lineList2 = getNewLinePoints(center, projection);
                    List<List<org.maplibre.geojson.Point>> multiline = new ArrayList<>();
                    multiline.add(lineList2);
                    MultiLineString multiLineString = MultiLineString.fromLngLats(multiline);
                    feature = org.maplibre.geojson.Feature.fromGeometry(multiLineString);
                    break;
            }

            feature.addStringProperty(prop_layerid, String.valueOf(layerdID));

            if (ngstyle != null){
                String styleField = ((ITextStyle)ngstyle).getField();
                String styleText = ((ITextStyle)ngstyle).getText();

                if (styleField != null || styleText != null ) {
                    String signature = null;
                    if (styleText != null)
                        signature = styleText;
                    else {
                        if (styleField == id_name)
                            signature = String.valueOf(originalSelectedFeature.getId());
                        else
                            signature = originalSelectedFeature.getFieldValueAsString(styleField);
                    }

                    if (!TextUtils.isEmpty(signature)) {
                        feature.addStringProperty(prop_signature_text, signature);
                    }
                }
            }


            int size = sourceFeaturesHashMap.get(layerdID).size();
            feature.addStringProperty(prop_order, String.valueOf(size+1));
            feature.addStringProperty(prop_featureid, String.valueOf(originalSelectedFeature.getId()));
            editingFeatureTmp = copyFeature(feature);
        }

        if (editingFeatureTmp != null) {
            selectedEditedSource = sourceHashMap.get(layerdID);
            editingFeature = editingFeatureTmp;

            int type = ((VectorLayer)getLayer(layerdID)).getGeometryType();
            GeoJsonSource choosed = null;
            if  (type == GeoConstants.GTPoint || type == GeoConstants.GTMultiPoint) {
                selectedDotSource.setGeoJson(FeatureCollection.fromFeature(editingFeature));
                choosed = selectedDotSource;
                editingFeature.addStringProperty("color", colorRED);
            }

            if  (type == GeoConstants.GTPolygon || type == GeoConstants.GTMultiPolygon) {
                selectedPolySource.setGeoJson(FeatureCollection.fromFeature(editingFeature));
                choosed = selectedPolySource;
            }

            if  (type == GTLineString || type == GTMultiLineString) {
                selectedPolySource.setGeoJson(FeatureCollection.fromFeature(editingFeature));
                choosed = selectedPolySource;
            }

            editingFeatureOriginal = copyFeature(editingFeatureTmp);

            polygonFeatures = sourceFeaturesHashMap.get(layerdID);
            this.originalSelectedFeature = originalSelectedFeature;

            // choose layer
            editingObject = MPLFeaturesUtils.createEditObject(layerGeoType,
                    selectedEditedSource,
                    editingFeature,
                    polygonFeatures,
                    choosed,
                    vertexSource,
                    markerSource);

            Layer layer = maplibreMap.get().getStyle().getLayer("selected-polygon-fill");
            if (layerGeoType ==GeoConstants.GTPolygon ||
                    layerGeoType ==GeoConstants.GTMultiPolygon){
                if (layer == null) {
                    maplibreMap.get().getStyle().addLayer(fillPolyEditLayer);
                }
            } else {
                if (layer != null)
                    maplibreMap.get().getStyle().removeLayer(fillPolyEditLayer);
            }

            editingObject.extractVertices(editingFeature,  true);

            LatLng selectedPoint = editingObject.getSelectedPoint();
            setMarker(selectedPoint);
            editingObject.updateEditingPolygonAndVertex();
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

            polygonFeatures.removeIf(f -> Objects.equals(f.getStringProperty(prop_order), editingFeature.getStringProperty(prop_order)));
            selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(polygonFeatures));
        }
        editingFeature = null;
        editingFeatureOriginal = null;
        selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        selectedDotSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
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

        if (newGeometry instanceof GeoLineString){
            org.maplibre.geojson.Feature featureML = getFeatureFromNGFeatureLine((GeoLineString)newGeometry);

            if(editingObject  != null && editingObject.editingFeature != null)
                copyProperties(editingObject.editingFeature, featureML);

            selectedPolySource.setGeoJson(featureML);
            editingObject.extractVertices(featureML,  false);
            editingObject.editingFeature = featureML;
        }

        if (newGeometry instanceof GeoMultiLineString){
            org.maplibre.geojson.Feature featureML = getFeatureFromNGFeatureMultiLine((GeoMultiLineString)newGeometry);

            if(editingObject  != null && editingObject.editingFeature != null)
                copyProperties(editingObject.editingFeature, featureML);

            selectedPolySource.setGeoJson(featureML);

            editingObject.extractVertices(featureML,  false);
            editingObject.editingFeature = featureML;
        }

        if (newGeometry instanceof  GeoPolygon){
            org.maplibre.geojson.Feature featureML = getFeatureFromNGFeaturePolygon((GeoPolygon)newGeometry);

            if(editingObject  != null && editingObject.editingFeature != null)
                copyProperties(editingObject.editingFeature, featureML);

            selectedPolySource.setGeoJson(featureML);

            editingObject.extractVertices(featureML,  false);
            editingObject.editingFeature = featureML;
        } else  if (newGeometry instanceof GeoMultiPolygon){

            org.maplibre.geojson.Feature featureML = getFeatureFromNGFeatureMultiPolygon((GeoMultiPolygon)newGeometry);

            if(editingObject  != null && editingObject.editingFeature != null)
                copyProperties(editingObject.editingFeature, featureML);

            selectedPolySource.setGeoJson(featureML);

            editingObject.extractVertices(featureML,  false);
            editingObject.editingFeature = featureML;

        }  else if (newGeometry instanceof  GeoPoint){

            org.maplibre.geojson.Feature featureML = getFeatureFromNGFeaturePoint((GeoPoint)newGeometry);

            if(editingObject  != null && editingObject.editingFeature != null)
                copyProperties(editingObject.editingFeature, featureML);

            selectedDotSource.setGeoJson(featureML);
            editingObject.extractVertices(featureML,  false);
            editingObject.editingFeature = featureML;

        } else if (newGeometry instanceof GeoMultiPoint){

            org.maplibre.geojson.Feature featureML = getFeatureFromNGFeatureMultiPoint((GeoMultiPoint)newGeometry);

            if(editingObject  != null && editingObject.editingFeature != null)
                copyProperties(editingObject.editingFeature, featureML);

            selectedDotSource.setGeoJson(featureML);
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

        Layer layer = maplibreMap.get().getStyle().getLayer("selected-polygon-fill");
        if (layer != null)
            maplibreMap.get().getStyle().removeLayer(fillPolyEditLayer);


        if (originalSelectedFeature.getId() != -1)
            startFeatureSelectionForView(mapFragment.get().getSelectedLayerId(),originalSelectedFeature );
    }

    public void unselectFeatureFromView(){
        if (viewedFeature != null) {
            //set color back
            viewedFeature.addStringProperty("color", colorBlue);

            viewedFeature = null;
            selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
            selectedDotSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        }
    }

    public void unselectFeatureFromEdit(boolean backToOriginal){
        if (editingObject != null && editingObject.editingFeature != null) {
            if (backToOriginal)
                copyProperties(editingFeatureOriginal, editingObject.editingFeature);
            hasEditeometry = false;
            org.maplibre.geojson.Feature target =backToOriginal? editingFeatureOriginal : editingObject.editingFeature ;

            boolean needChangeFeature = true; // false for newFeature create cancel - remove from anysource
            if (target!= null && target.hasNonNullValueForProperty(prop_featureid) && target.getStringProperty(prop_featureid).equals("-1")) // new feature - no back to anything
                needChangeFeature = false;
            if (needChangeFeature)
                polygonFeatures.add(backToOriginal? editingFeatureOriginal : editingObject.editingFeature);

            selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(polygonFeatures));

            GeoJsonSource choosed = null;
            if  (editingObject instanceof PointEditClass ||
                    editingObject instanceof MultiPointEditClass) {
                choosed = selectedDotSource;
            } else{
                choosed = selectedPolySource;
            }

            if (needChangeFeature)
                choosed.setGeoJson(backToOriginal? editingFeatureOriginal : editingObject.editingFeature);
            else
                choosed.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));

            if (editingObject.editingFeature.geometry() instanceof Polygon ||
                    editingObject.editingFeature.geometry() instanceof MultiPolygon){
                Layer layer = maplibreMap.get().getStyle().getLayer("selected-polygon-fill");
                if (layer != null)
                    maplibreMap.get().getStyle().removeLayer(fillPolyEditLayer);
            }

            editingObject = null;
            editingFeatureOriginal = null;
            editingFeature = null;
            vertexSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        }
    }

    public org.maplibre.geojson.Feature copyFeature(org.maplibre.geojson.Feature from ){
        org.maplibre.geojson.Feature newFeature = org.maplibre.geojson.Feature.fromGeometry(from.geometry());
        copyProperties(from, newFeature);
        return newFeature;
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

    public boolean deleteCurrentPoint(){
        if (editingObject != null) {
            editingObject.deleteCurrentPoint();
            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
        }
        return true;
    }

    public boolean deleteCurrentLine(){
        if (editingObject != null && editingObject instanceof MultiLineEditClass) {
            ((MultiLineEditClass)editingObject).deleteCurrentLine();
            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
        }
        return true;
    }

    public boolean addNewPoint(LatLng center){
        if (editingObject != null && editingObject instanceof MultiPointEditClass) {
            ((MultiPointEditClass)editingObject).addNewPoint(center);
            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
        }
        return true;
    }

    public boolean addNewLine(LatLng center, Projection projection){
        if (editingObject != null && editingObject instanceof MultiLineEditClass) {
            ((MultiLineEditClass)editingObject).addNewLine(center, projection);
            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);

        }
        return true;
    }

    public boolean deleteCurrentHole(){
        if (editingObject != null && editingObject instanceof PolygonEditClass) {
            ((PolygonEditClass)editingObject).deleteCurrentHole();
            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
        }

        if (editingObject != null && editingObject instanceof MultiPolygonEditClass) {
            ((MultiPolygonEditClass)editingObject).deleteSelectedPolygon();
            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
        }
        return true;
    }

    public boolean addHole(LatLng center, Projection projection){
        if (editingObject != null && editingObject instanceof PolygonEditClass) {
            ((PolygonEditClass)editingObject).addHole(center, projection);
            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
        }
        if (editingObject != null && editingObject instanceof MultiPolygonEditClass) {
            ((MultiPolygonEditClass)editingObject).addHole(center, projection);
            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
        }
        return true;
    }

    public boolean deleteCurrentPolygon(){
        if (editingObject != null && editingObject instanceof MultiPolygonEditClass) {
            ((MultiPolygonEditClass)editingObject).deleteSelectedPolygon();
            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
        }
        return true;
    }

    public boolean addNewPolygon(LatLng center, Projection projection){
        if (editingObject != null && editingObject instanceof MultiPolygonEditClass) {
            ((MultiPolygonEditClass)editingObject).addNewPolygonAt(center,projection);
            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
        }
        return true;
    }

    public boolean moveToPoint(LatLng point){
        if (editingObject != null) {
            editingObject.movePointTo(point);
        }
        return true;
    }

    public void finishCreateNewFeature(long newFeatureID){
        hideMarker();
        if (editingObject != null) {
            editingObject.finishCreateNewFeature(newFeatureID);

            if (editingFeature != null  ){ // add to objects
                // no need ?   twice creation if unquote
//                Integer layerdID = Integer.valueOf(editingFeature.getStringProperty(prop_layerid));
//                List<org.maplibre.geojson.Feature> layerFeatures = sourceFeaturesHashMap.get(layerdID);
//                if (layerFeatures != null)
//                    layerFeatures.add(editingObject.editingFeature);
            }
        }
        originalSelectedFeature.setId(newFeatureID);
        cancelFeatureEdit(false);

    }

    public void checkLayerVisibility(int id){

        ILayer targetlayer = getVectorLayersById(this,  id);
        boolean isVisible = ((com.nextgis.maplib.map.Layer)targetlayer).isVisible();

        Layer layer = layersHashMap.get(id);
        if (layer != null)
            layer.setProperties(visibility(isVisible ? VISIBLE:NONE));

        Layer layer2 = layersHashMap2.get(id);
        if (layer2 != null)
            layer2.setProperties(visibility(isVisible ? VISIBLE:NONE));

        Layer layerSymbol = symbolsLayerHashMap.get(id);
        if (layerSymbol != null)
            layerSymbol.setProperties(visibility(isVisible ? VISIBLE:NONE));

        if (targetlayer instanceof NGWRasterLayer || targetlayer instanceof  RemoteTMSLayer){
            Layer layerRaster = getRasterLayer(id,  maplibreMap.get().getStyle());
            if (layerRaster != null){
                layerRaster.setProperties(visibility(isVisible ? VISIBLE:NONE));
            }
        }
        if (getLayer(id) instanceof TrackLayer ){
            dd
        }
    }

    public void changePointColor(){
        String colorS = "#FFFFFF";
        if (testColor == 1)
            colorS = "#00FFFF";
        if (testColor == 2)
            colorS = "#0000FF";
        if (testColor == 3)
            colorS = "#FF00FF";

        int id = 5; // test point layer
        List<ILayer> ret = new ArrayList<>();
        LayerGroup.getVectorLayersByType(this, GeoConstants.GTAnyCheck, ret);
        for (ILayer iLayer : ret){
            if (iLayer.getId() == id){
                com.nextgis.maplib.display.Style newStyle = ((VectorLayer)iLayer).getDefaultStyleNoExcept();
                Style maplbrStyle = maplibreMap.get().getStyle();

                String currentNamePrefix = getTypePrefix(iLayer.getType());
                org.maplibre.android.style.layers.Layer newLayer = maplbrStyle.getLayer(currentNamePrefix + "layer-" + id);
                newLayer.setProperties(PropertyFactory.circleRadius(22f),
                        PropertyFactory.circleColor(colorS),
                        PropertyFactory.circleStrokeColor("#AA0044"),  //
                        PropertyFactory.circleStrokeWidth(5f),         //
                        PropertyFactory.circleStrokeOpacity(1f));
                break;
            }
        }

        testColor++;
        if (testColor > 3 )
            testColor = 0;
    }



    public void updateLocation(Point point, boolean isStanding, float bearing){
        if(locationSource!= null) {
            org.maplibre.geojson.Feature pointFeature = org.maplibre.geojson.Feature.fromGeometry(point);
            pointFeature.addStringProperty("type", String.valueOf(isStanding?"stand" : "go"));
            if (isStanding)
                bearing = 0.0f;
            pointFeature.addNumberProperty("bearing", bearing);

            locationSource.setGeoJson(pointFeature);
        }
    }

    public void reloadTrackListToMap(){
        List<ILayer> tracks = new ArrayList<>();
        LayerGroup.getLayersByType(this, Constants.LAYERTYPE_TRACKS, tracks);
        if (tracks.size() > 0){
            TrackLayer trackLayer = (TrackLayer)(tracks.get(0));
            List<org.maplibre.geojson.Feature> tracksFeatures = createFeatureListFromTrackLayer(trackLayer);


            createSourceForLayer(trackLayer.getId(), GT_TRACK_WA,
                    tracksFeatures, maplibreMap.get().getStyle(),
                    sourceHashMap, new HashMap<>());

            createFillLayerForLayer(trackLayer.getId(), GT_TRACK_WA,
                    maplibreMap.get().getStyle(),
                    layersHashMap,
                    layersHashMap2,
                    symbolsLayerHashMap,
                    null, false);


        }

    }

}
