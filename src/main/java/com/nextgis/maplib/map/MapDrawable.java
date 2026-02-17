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
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

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
import com.nextgis.maplib.display.RuleFeatureRenderer;
import com.nextgis.maplib.map.MLP.MLGeometryEditClass;
import com.nextgis.maplib.map.MLP.MeasurmentLine;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import static com.nextgis.maplib.map.MLP.MultiLineEditClass.getNewLinePoints;
import static com.nextgis.maplib.map.MLP.PolygonEditClass.createPointsForRing;
import static com.nextgis.maplib.map.MPLFeaturesUtils.applyTextAndStyle;
import static com.nextgis.maplib.map.MPLFeaturesUtils.colorBlue;
import static com.nextgis.maplib.map.MPLFeaturesUtils.colorLightBlue;
import static com.nextgis.maplib.map.MPLFeaturesUtils.colorRED;
import static com.nextgis.maplib.map.MPLFeaturesUtils.convert3857To4326;
import static com.nextgis.maplib.map.MPLFeaturesUtils.convert4326To3857;
import static com.nextgis.maplib.map.MPLFeaturesUtils.convertToPointFeatures;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createFeatureListFlagsFromTrackLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createFeatureListFromLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createFeatureListFromTrackLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createFillLayerForLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.createSourceForLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.geoPointFromLatLng;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeatureLine;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeatureMultiLine;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeatureMultiPoint;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeatureMultiPolygon;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeaturePoint;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getFeatureFromNGFeaturePolygon;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getLayerSignatureField;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getMPLThinkness;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getRasterLayer;
import static com.nextgis.maplib.map.MPLFeaturesUtils.getSpaceCorrectedText;
import static com.nextgis.maplib.map.MPLFeaturesUtils.id_name;
import static com.nextgis.maplib.map.MPLFeaturesUtils.latLngPointFromGeoPoint;
import static com.nextgis.maplib.map.MPLFeaturesUtils.layer_namepart;
import static com.nextgis.maplib.map.MPLFeaturesUtils.namePrefix;
import static com.nextgis.maplib.map.MPLFeaturesUtils.outline_namepart;
import static com.nextgis.maplib.map.MPLFeaturesUtils.prop_featureid;
import static com.nextgis.maplib.map.MPLFeaturesUtils.prop_layerid;
import static com.nextgis.maplib.map.MPLFeaturesUtils.prop_order;
import static com.nextgis.maplib.map.MPLFeaturesUtils.prop_signature_text;
import static com.nextgis.maplib.map.MPLFeaturesUtils.source_namepart;
import static com.nextgis.maplib.map.MPLFeaturesUtils.source_polygon_text;
import static com.nextgis.maplib.util.Constants.DRAW_FINISH_ID;
import static com.nextgis.maplib.util.Constants.MAP_LIMITS_Y;
import static com.nextgis.maplib.util.Constants.MESSAGE_INTENT_STYLING;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.GTLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPoint;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPolygon;
import static com.nextgis.maplib.util.GeoConstants.GTNone;
import static com.nextgis.maplib.util.GeoConstants.GTPoint;
import static com.nextgis.maplib.util.GeoConstants.GTPolygon;
import static com.nextgis.maplib.util.GeoConstants.GT_MEASURMENT;
import static com.nextgis.maplib.util.GeoConstants.GT_RASTER_WA;
import static com.nextgis.maplib.util.GeoConstants.GT_TRACK_WA;
import static com.nextgis.maplib.util.NetworkUtil.extractResourceValue;
import static com.nextgis.maplib.util.NetworkUtil.fillConnections;
import static com.nextgis.maplib.util.NetworkUtil.getBaseUrlpart;
import static com.nextgis.maplib.util.NetworkUtil.getHTTPBaseAuth;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_DARK;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_LIGHT;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_NEUTRAL;
import static org.maplibre.android.style.layers.Property.NONE;
import static org.maplibre.android.style.layers.Property.VISIBLE;
import static org.maplibre.android.style.layers.PropertyFactory.visibility;
import static java.util.Collections.emptyList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.IconFactory;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Projection;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.expressions.Expression;
import org.maplibre.android.style.layers.BackgroundLayer;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.FillLayer;
import org.maplibre.android.style.layers.Layer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.Property;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.android.style.sources.Source;
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

    public final static int MODE_NONE = 0;
    public final static int MODE_HIGHLIGHT = 1;
    public final static int MODE_EDIT = 2;
    public final static int MODE_CHANGE = 3;
    public final static int MODE_EDIT_BY_WALK = 4;
    public final static int MODE_EDIT_BY_TOUCH = 5;

    static int testColor = 0;


    // map  layerID : list of added features for layer
    LinkedHashMap<Integer, List<org.maplibre.geojson.Feature>> sourceFeaturesHashMap = new LinkedHashMap<Integer, List<org.maplibre.geojson.Feature>>();

    LinkedHashMap<Integer, List<org.maplibre.geojson.Feature>> sourcesOrder = new LinkedHashMap<Integer, List<org.maplibre.geojson.Feature>>();

    // map sources added to maplibre  from layers
    LinkedHashMap<String, GeoJsonSource>  sourceHashMap = new LinkedHashMap<String, GeoJsonSource>();

    // map fill Layer of each added layer
    LinkedHashMap<Integer, org.maplibre.android.style.layers.Layer>  layersHashMap = new LinkedHashMap<Integer, org.maplibre.android.style.layers.Layer>();

    // outline for polygone
    LinkedHashMap<Integer, org.maplibre.android.style.layers.Layer>  layersHashMap2 = new LinkedHashMap<Integer, org.maplibre.android.style.layers.Layer>();

    // Symbols for geometry signature
    LinkedHashMap<Integer, org.maplibre.android.style.layers.Layer>  symbolsLayerHashMap = new LinkedHashMap<Integer, org.maplibre.android.style.layers.Layer>();

    GeoJsonSource selectedEditedSource = null; // choosed  source - from with edit (selectable)
    GeoJsonSource selectedPolySource = null; // choosed source of polygon/line  //
    GeoJsonSource selectedDotSource = null; // choosed source of polygon  //
    GeoJsonSource signaturesRootLayerSource = null; // choosed source of polygon  //

    GeoJsonSource tracksLineSource = null; // constant tracks
    GeoJsonSource tracksFlagsSource = null; // flags ( start/stop ) tracks
    GeoJsonSource trackInProgressSource = null; // flags ( start/stop ) tracks

    CircleLayer selectedDotCircleLayer = null;

    CircleLayer signaturesRootLayer = null;



    GeoJsonSource vertexSource = null;      // edit points  //

    FillLayer fillPolyEditLayer = null; // fill poly on edit layer (while on move points)

    public org.maplibre.geojson.Feature hiddedFeature = null;
    Long hiddedFeatureId = null;
    int hiddedlayerdID = -1;

    FeatureCollection markerFeatureCollection = FeatureCollection.fromFeatures(new ArrayList<>());
    GeoJsonSource markerSource = null; // marker source - select point

    GeoJsonSource locationSource = null;

    List<org.maplibre.geojson.Feature> polygonFeatures = new ArrayList<org.maplibre.geojson.Feature>();  //

    PointF clickPoint = null;

    public Feature  originalSelectedFeature = null;            // original who edit

    public MLGeometryEditClass editingObject = null;    // current edit

    public boolean isMeasurmentChangeVertex = false;
    private org.maplibre.geojson.Feature  editingFeature = null;    // current edit
    private org.maplibre.geojson.Feature  editingFeatureOriginal = null;
    private org.maplibre.geojson.Feature  viewedFeature = null;   // who looking
    private boolean hasEditeometry = false; // was edit

    private boolean isDragging = false;
    private boolean isDraggingByTouchGPS = false;
    private boolean isSwitchVertex = false;
    private MotionEvent startEvent = null;
    private PointF deltaPoint = null;

    public float zoomSaved = 1.0f; // one time used zoom after map start
    public GeoPoint centerSaved = new GeoPoint(0,0); //

    protected int  mLimitsType;
    protected RunnableFuture<Void> mDrawThreadTask;

    public WeakReference<MaplibreMapInteraction> mapFragment = new WeakReference(null);

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

    public void setMapFragment(final MaplibreMapInteraction mapFragment){
        this.mapFragment = new WeakReference<>(mapFragment);
    }

    public void setMaplibreMap(final MapLibreMap maplibreMap){
        this.maplibreMap = new WeakReference<>(maplibreMap);
    }

    public void setMaplibreMapView(final org.maplibre.android.maps.MapView maplibreMapView){
        this.maplibreMapView = new WeakReference<>(maplibreMapView);

    }


    public void loadMarkersTopPart(){

        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            loadLayersToMaplibreMapLite(new ArrayList<>(), true);
        });
    }

    public void clearMaplLibreMap(){

        final Style style = maplibreMap.get().getStyle();
        for (final Layer layer :maplibreMap.get().getStyle().getLayers()){
//            Log.e("ZXZY", "delete layer" + layer.getId());
            if (!layer.getId().equals("background"))
                style.removeLayer(layer);
        }
        // clear all layers - so - first tool layer also clear
        signaturesRootLayer = null;
        selectedDotCircleLayer = null;

//        for (final Source source : maplibreMap.get().getStyle().getSources()) {
//            Log.e("ZXZY", "delete source" + source.getId());
//            boolean result = style.removeSource(source);
//        }



//
//        this.maplibreMap.get().clear();
//
//        this.maplibreMap.clear();
//        this.maplibreMapView.get().onPause();
//        this.maplibreMapView.get().onStop();
//        this.maplibreMapView.get().onDestroy();
//
//
//        this.maplibreMapView.clear();
//
//
//        this.maplibreMap = new WeakReference<>(null);
//        this.maplibreMapView = new WeakReference<>(null);
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
                reloadVectorLayerDataToMaplibre(iLayer);
                return;
            }
        }
    }


    // change feature id at map objects - features // objects
    public void changeFeatureId(Long oldFeatureId,Long newFeatureId, int layerId){

        String oldFeatureIdString = String.valueOf(oldFeatureId);
        List<org.maplibre.geojson.Feature> layerFeatures = sourceFeaturesHashMap.get(layerId);
        for (org.maplibre.geojson.Feature feature : layerFeatures){
            if (feature.getStringProperty(prop_featureid).equals(oldFeatureIdString)) {
                feature.addStringProperty(prop_featureid, String.valueOf(newFeatureId));
                break;// only one feature with same id
            }
        }

        for (org.maplibre.geojson.Feature feature : polygonFeatures){
            if (feature.getStringProperty(prop_featureid).equals(oldFeatureIdString)) {
                feature.addStringProperty(prop_featureid, String.valueOf(newFeatureId));
                break;// only one feature with same id
            }
        }
    }

    public void deleteLayerByID(int id){
        if (maplibreMap.get()!= null) {
            String sourceId = namePrefix + source_namepart + id;

            String vectorLayerId = namePrefix + layer_namepart + id;

            String vectorLayerId2 = namePrefix + layer_namepart + id + outline_namepart;

            String currentNamePrefixSymbol = "symbol-" +  namePrefix;
            String vectorLayerIdSymbols =currentNamePrefixSymbol + layer_namepart + id;

            if (maplibreMap.get().getStyle().getLayer(vectorLayerId)!= null)
                maplibreMap.get().getStyle().removeLayer(vectorLayerId);

            if (maplibreMap.get().getStyle().getLayer(vectorLayerId2)!= null)
                maplibreMap.get().getStyle().removeLayer(vectorLayerId2);

            if (maplibreMap.get().getStyle().getLayer(vectorLayerIdSymbols)!= null)
                maplibreMap.get().getStyle().removeLayer(vectorLayerIdSymbols);


            if (maplibreMap.get().getStyle().getSource(sourceId)!= null)
                maplibreMap.get().getStyle().removeSource(sourceId);
        }
    }

    public void addLayerByID(int id){

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            ILayer crashLayer = null;
            if (maplibreMap.get() != null)
                crashLayer = LayerGroup.getVectorLayersById(this, id);
            try {
                if (maplibreMap.get() != null) {
                    ILayer iLayer = LayerGroup.getVectorLayersById(this, id);
                    if (iLayer == null)
                        return;

                    final AccountManager accountManager = AccountManager.get(getContext());
                    final Connections connections = fillConnections(getContext(), accountManager);

                    if (iLayer instanceof NGWRasterLayer) {
                        // need add auth
                        Connection found = null;
                        if (iLayer instanceof NGWRasterLayer) {
                            for (int i = 0; i < connections.getChildrenCount(); i++) {
                                if (connections.getChild(i).getName().equals((((NGWRasterLayer) iLayer).getAccountName()))) {
                                    found = (Connection) connections.getChild(i);
                                    String basicAuth = getHTTPBaseAuth(found.getLogin(), found.getPassword());
                                    if (null != basicAuth) {
                                        final String url = ((NGWRasterLayer) iLayer).getURL();
                                        final String getBaseUrl = getBaseUrlpart(url);
                                        final String resPart = "resource=" + extractResourceValue(url);

                                        final String[] authPart = new String[4];
                                        authPart[0] = getBaseUrl;
                                        authPart[1] = resPart;
                                        authPart[2] = basicAuth;
                                        authPart[3] = iLayer.getPath().toString();
                                        ((IGISApplication) getContext().getApplicationContext()).updateAuthPair(authPart);
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (iLayer instanceof RemoteTMSLayer) {
                        final String url = ((RemoteTMSLayer) iLayer).getURL();
                        final String getBaseUrl = getBaseUrlpart(url);
                        final String resPart = "resource=" + extractResourceValue(url);
                        final String[] authPart = new String[4];
                        authPart[0] = getBaseUrl;
                        authPart[1] = resPart;
                        authPart[2] = "no";//no auth RemoteTMSLayer - geoservice map
                        authPart[3] = iLayer.getPath().toString();
                        ((IGISApplication) getContext().getApplicationContext()).updateAuthPair(authPart);
                    }

                    int geoType = GTNone;
                    final List<org.maplibre.geojson.Feature> vectorPolygonFeatures = new ArrayList<>();
                    Map<Integer, String> rasterLayersURLMap = new HashMap<>();
                    Map<Integer, Integer> rasterLayersTmsTypeMap = new HashMap<>();
                    com.nextgis.maplib.display.Style ngStyle = null;

                    if (iLayer instanceof VectorLayer) {
                        VectorLayer layer = (VectorLayer) iLayer;
                        geoType = layer.getGeometryType();
                        // this layer

                        ((IGISApplication) getContext().getApplicationContext()).setGetingStyleInProgress(true);

                        mainHandler.post(()-> {
                                    mapFragment.get().changeProgress(true); });
                        try {
                            Intent msg = new Intent(MESSAGE_INTENT_STYLING);

                            String loadHint = getContext().getString(R.string.process_layer_hint);

                            msg.putExtra("msg", loadHint + ((VectorLayer) iLayer).getName());
                            msg.setPackage(getContext().getPackageName());
                            getContext().sendBroadcast(msg);
                            vectorPolygonFeatures.addAll(createFeatureListFromLayer(layer));
                            sourceFeaturesHashMap.put(layer.getId(), vectorPolygonFeatures);
                            sourcesOrder.put(layer.getId(), new ArrayList<>());
                            ngStyle = ((VectorLayer) iLayer).getDefaultStyleNoExcept();

                        } catch (Exception ex) {
                            Log.e("fail", ex.getMessage());

                        } finally {
                            Intent msg1 = new Intent(MESSAGE_INTENT_STYLING);
                            msg1.putExtra("msg", "");
                            msg1.setPackage(getContext().getPackageName());

                            getContext().sendBroadcast(msg1);

                            ((IGISApplication) getContext().getApplicationContext()).setGetingStyleInProgress(false);
                            mainHandler.post(()-> {
                                mapFragment.get().changeProgress(false);
                            });
                        }
                    } else if (iLayer instanceof NGWRasterLayer) {
                        geoType = GT_RASTER_WA;
                        NGWRasterLayer layer = (NGWRasterLayer) iLayer;
                        rasterLayersURLMap.put(layer.getId(), ((NGWRasterLayer) layer).getURL());
                        rasterLayersTmsTypeMap.put(layer.getId(), -1);
                    } else if (iLayer instanceof RemoteTMSLayer) {
                        if (((RemoteTMSLayer) iLayer).mIsOfflineLayer) {
                            geoType = GT_RASTER_WA;
                            RemoteTMSLayer layer = (RemoteTMSLayer) iLayer;
                            rasterLayersURLMap.put(layer.getId(), "file://" + (layer).getPath().toString() + "/{z}/{x}/{y}.tile");
                            rasterLayersTmsTypeMap.put(layer.getId(), layer.getTMSType());
                        } else {
                            geoType = GT_RASTER_WA;
                            RemoteTMSLayer layer = (RemoteTMSLayer) iLayer;
                            rasterLayersURLMap.put(layer.getId(), (layer).getURLSubdomain());
                            rasterLayersTmsTypeMap.put(layer.getId(), layer.getTMSType());
                        }
                    } else if (iLayer instanceof LocalTMSLayer) {
                        geoType = GT_RASTER_WA;
                        LocalTMSLayer layer = (LocalTMSLayer) iLayer;
                        rasterLayersURLMap.put(layer.getId(), "file://" + (layer).getPath().toString() + "/{z}/{x}/{y}.tile");
                        rasterLayersTmsTypeMap.put(layer.getId(), layer.getTMSType());
                    }

                    Style style = maplibreMap.get().getStyle();
                    final int finalGeoType = geoType;
                    final com.nextgis.maplib.display.Style finalStyle = ngStyle;

                    mainHandler.post(() -> {
                        createFillLayerForLayer(iLayer.getId(), finalGeoType, style, layersHashMap, layersHashMap2,
                                symbolsLayerHashMap,
                                finalStyle, false, iLayer,
                                iLayer.getPath().toString(),
                                selectedDotCircleLayer,
                                signaturesRootLayer);
                        createSourceForLayer(iLayer.getId(), finalGeoType, vectorPolygonFeatures, style, sourceHashMap,
                                rasterLayersURLMap, rasterLayersTmsTypeMap,
                                iLayer.getPath().toString());

                        checkLayerVisibility(iLayer.getId());
                    });
                }
            } catch (OutOfMemoryError outOfMemoryError) {
                if (mapFragment.get() != null) {
                    String layerName = (crashLayer == null ? "null" : crashLayer.getName());
                    AlertDialog builder = new AlertDialog.Builder(((Fragment) mapFragment.get()).getActivity())
                            .setTitle("MemoryError")
                            .setMessage(((Fragment) mapFragment.get()).getActivity().getString(R.string.outofmemory) + layerName)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            }
        });

    }

    public void reloadFillLayerStyleToMaplibre(final  int  id) {
        if (mapFragment.get() != null && (!(mapFragment.get().getMode() == 0)))
            return;

        List<ILayer> vectorss = new ArrayList<>();
        List<ILayer> tmss = new ArrayList<>();

        getTMSLayersByType(this,  GeoConstants.GTAnyCheck, tmss);

        LayerGroup.getVectorLayersByType(this, GeoConstants.GTAnyCheck, vectorss);
        for (ILayer iLayer : vectorss){
            if (iLayer.getId() == id){
                com.nextgis.maplib.display.Style newStyle = ((VectorLayer)iLayer).getDefaultStyleNoExcept();
                Style maplbrStyle = maplibreMap.get().getStyle();
                if (! ((VectorLayer) iLayer).isVisible())
                    return;
                createFillLayerForLayer(id, ((VectorLayer) iLayer).getGeometryType(),maplbrStyle ,layersHashMap,layersHashMap2,
                        symbolsLayerHashMap,
                        newStyle, true, iLayer,
                        iLayer.getPath().toString(),
                        selectedDotCircleLayer,
                        signaturesRootLayer);
                checkLayerVisibility(id);
                reloadVectorLayerDataToMaplibre(iLayer);
                return;
            }
        }

        for (ILayer iLayer : tmss){
            if (iLayer.getId() == id){
                Style maplbrStyle = maplibreMap.get().getStyle();
                createFillLayerForLayer(id,  GT_RASTER_WA, maplbrStyle ,layersHashMap, layersHashMap2,
                        symbolsLayerHashMap,
                        null, true, iLayer,
                        iLayer.getPath().toString(),
                        selectedDotCircleLayer,
                        signaturesRootLayer);
                checkLayerVisibility(id);
                reloadVectorLayerDataToMaplibre(iLayer);
                return;
            }
        }
    }

    public void reloadVectorLayerDataToMaplibre(final  ILayer ilayer) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        Runnable r = () -> {
            executor.execute(() -> {
                if (maplibreMap.get() == null || maplibreMapView.get() == null)
                    return;
                if (ilayer instanceof  TMSLayer){

                    return;
                }


                if (!(ilayer instanceof VectorLayer))
                    return;
                VectorLayer layer = (VectorLayer) ilayer;


                ((IGISApplication)getContext().getApplicationContext()).setGetingStyleInProgress(true);
                mainHandler.post(() -> {
                    mapFragment.get().changeProgress(true);
                });

                String loadHint = getContext().getString(R.string.process_layer_hint);

                Intent msg = new Intent(MESSAGE_INTENT_STYLING);
                msg.putExtra("msg", loadHint + layer.getName());
                msg.setPackage(getContext().getPackageName());
                getContext().sendBroadcast(msg);


                List<org.maplibre.geojson.Feature> vectorPolygonFeatures = createFeatureListFromLayer(layer);
                sourceFeaturesHashMap.put(layer.getId(), vectorPolygonFeatures);
                sourcesOrder.put(layer.getId(), new ArrayList<>());

                ((IGISApplication)getContext().getApplicationContext()).setGetingStyleInProgress(false);
                mainHandler.post(() -> {
                    mapFragment.get().changeProgress(false);
                });

                // Switch to main thread
                mainHandler.post(() -> {
                    GeoJsonSource layerSource = sourceHashMap.get(layer.getPath().toString());
                    List<org.maplibre.geojson.Feature> features = sourceFeaturesHashMap.get(layer.getId());
                    if (layerSource != null) {
                        layerSource.setGeoJson(FeatureCollection.fromFeatures(features));
                    }

                    if (layer.mGeometryType == GTPolygon || layer.mGeometryType ==  GTMultiPolygon){
                        GeoJsonSource layerSourceText = sourceHashMap.get(layer.getPath().toString() + source_polygon_text);
                        if (layerSourceText != null){
                            List<org.maplibre.geojson.Feature> points =  convertToPointFeatures(features);
                            layerSourceText.setGeoJson(FeatureCollection.fromFeatures(points));
                        }
                    }
                });
            });
            executor.shutdown();
        };
        mainHandler.postDelayed(r, 500);

    }

    public void loadLayersToMaplibreMap(final String styleJson,
                                        final  List<ILayer> allLayers,
                                        final boolean createSource,
                                        final boolean skipInvisibleLayers) {

        mapFragment.get().changeProgress(true);

        maplibreMapView.get().setOnTouchListener(this);
        maplibreMap.get().addOnMapClickListener(this);
        maplibreMap.get().addOnMapLongClickListener(this);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        final Map<Integer, Integer> layersType = new HashMap<>();
        final Map<Integer, String> layersPath = new HashMap<>();
        final Map<Integer, com.nextgis.maplib.display.Style> layersStyle = new HashMap<>();
        final Map<Integer, String> rasterLayersURLMap = new HashMap<>();
        final Map<Integer, Integer> rasterLayersTmsTypeMap = new HashMap<>();

        final List<org.maplibre.geojson.Feature> tracksFeatures = new ArrayList<>();
        final List<org.maplibre.geojson.Feature> tracksFlagsFeatures = new ArrayList<>();

        executor.execute(() -> {
            if (maplibreMap.get() == null || maplibreMapView.get() == null)
                return;
            // Load layers

            final AccountManager accountManager = AccountManager.get(getContext());
            final Connections connections = fillConnections(getContext(), accountManager);

            //sourceFeaturesHashMap.clear();
            //sourceHashMap.clear();
            sourcesOrder.clear();

            for (ILayer iLayer : allLayers) {
//                Log.e("MPLREM",  "iterate layer " + iLayer.getName());

                try {
                    if (iLayer instanceof VectorLayer) {

                        VectorLayer layer = (VectorLayer) iLayer;

                        if (skipInvisibleLayers && !layer.isVisible())
                            continue;

                        Intent msg = new Intent(MESSAGE_INTENT_STYLING);
                        String loadHint = getContext().getString(R.string.process_layer_hint);
                        msg.putExtra("msg", loadHint + ((VectorLayer)iLayer).getName());
                        msg.setPackage(getContext().getPackageName());
                        getContext().sendBroadcast(msg);

                        layersType.put(layer.getId(), layer.getGeometryType());
                        layersPath.put(layer.getId(), layer.getPath().toString());
                        layersStyle.put(layer.getId(), layer.getDefaultStyleNoExcept());
                        List<org.maplibre.geojson.Feature> vectorFeatures = createFeatureListFromLayer(layer);
                        sourceFeaturesHashMap.put(layer.getId(), vectorFeatures);
                        sourcesOrder.put(layer.getId(), new ArrayList<>());
                    } else if (iLayer instanceof TrackLayer) {
                        TrackLayer layer = (TrackLayer) iLayer;
                        layersType.put(layer.getId(), GT_TRACK_WA);
                        layersPath.put(layer.getId(), layer.getPath().toString());

                        tracksFeatures.clear();
                        tracksFeatures.addAll(createFeatureListFromTrackLayer(layer));

                        tracksFlagsFeatures.clear();
                        tracksFlagsFeatures.addAll(createFeatureListFlagsFromTrackLayer(layer));

                        //List<org.maplibre.geojson.Feature> tracksFeatures = createFeatureListFromTrackLayer(layer);
                        //sourceFeaturesHashMap.put(layer.getId(), tracksFeatures);
                    } else if (iLayer instanceof NGWRasterLayer) {
                        // need add auth
                        Connection found = null;
                        if (iLayer instanceof NGWRasterLayer) {
                            for (int i = 0; i < connections.getChildrenCount(); i++) {
                                if (connections.getChild(i).getName().equals((((NGWRasterLayer) iLayer).getAccountName()))) {
                                    found = (Connection) connections.getChild(i);
                                    String basicAuth = getHTTPBaseAuth(found.getLogin(), found.getPassword());
                                    if (null != basicAuth) {
                                        final String url = ((NGWRasterLayer) iLayer).getURL();
                                        final String getBaseUrl = getBaseUrlpart(url);
                                        final String resPart = "resource=" + extractResourceValue(url);
                                        final String[] authPart = new String[4];
                                        authPart[0] = getBaseUrl;
                                        authPart[1] = resPart;
                                        authPart[2] = basicAuth;
                                        authPart[3] = iLayer.getPath().toString();
                                        ((IGISApplication) getContext().getApplicationContext()).updateAuthPair(authPart);
                                        break;
                                    }
                                }
                            }

                            TMSLayer layer = (TMSLayer) iLayer;
                            layersType.put(layer.getId(), GT_RASTER_WA);
                            layersPath.put(layer.getId(), layer.getPath().toString());

                            rasterLayersURLMap.put(layer.getId(), ((NGWRasterLayer) layer).getURL());
                            sourceFeaturesHashMap.put(layer.getId(), new ArrayList<>());
                            sourcesOrder.put(layer.getId(), new ArrayList<>());
                        }
                    } else if (iLayer instanceof RemoteTMSLayer) {
                        final String url = ((RemoteTMSLayer) iLayer).getURL();
                        final String getBaseUrl = getBaseUrlpart(url);
                        final String resPart = "resource=" + extractResourceValue(url);
                        final String[] authPart = new String[4];
                        authPart[0] = getBaseUrl;
                        authPart[1] = resPart;
                        authPart[2] = "no";//no auth RemoteTMSLayer - geoservice map
                        authPart[3] = iLayer.getPath().toString();
                        ((IGISApplication) getContext().getApplicationContext()).updateAuthPair(authPart);

                        TMSLayer layer = (TMSLayer) iLayer;
                        layersType.put(layer.getId(), GT_RASTER_WA);
                        layersPath.put(layer.getId(), layer.getPath().toString());

                        if (((RemoteTMSLayer) layer).mIsOfflineLayer) {
                            rasterLayersURLMap.put(layer.getId(), "file://" + (layer).getPath().toString() + "/{z}/{x}/{y}.tile");
                            rasterLayersTmsTypeMap.put(layer.getId(), layer.getTMSType());
                        } else {
                            rasterLayersURLMap.put(layer.getId(), ((RemoteTMSLayer) layer).getURLSubdomain());
                            rasterLayersTmsTypeMap.put(layer.getId(), layer.getTMSType());
                        }
                        sourceFeaturesHashMap.put(layer.getId(), new ArrayList<>());
                        sourcesOrder.put(layer.getId(), new ArrayList<>());
                    } else if (iLayer instanceof LocalTMSLayer) {
                        TMSLayer layer = (TMSLayer) iLayer;
                        layersType.put(layer.getId(), GT_RASTER_WA);
                        layersPath.put(layer.getId(), layer.getPath().toString());

                        rasterLayersURLMap.put(layer.getId(), "file://" + (layer).getPath().toString() + "/{z}/{x}/{y}.tile");
                        rasterLayersTmsTypeMap.put(layer.getId(), layer.getTMSType());
                        sourceFeaturesHashMap.put(layer.getId(), new ArrayList<>());
                        sourcesOrder.put(layer.getId(), new ArrayList<>());
                    }
                } catch (OutOfMemoryError outOfMemoryError){
                    mainHandler.post(()-> {
                        Toast.makeText(mContext, mContext.getString(R.string.outofmemory) + iLayer.getName(), Toast.LENGTH_LONG).show();
                    });
                }
            }

            mainHandler.post(() -> {
                maplibreMapView.get().addOnDidFinishLoadingStyleListener(new MapView.OnDidFinishLoadingStyleListener() {
                    @Override
                    public void onDidFinishLoadingStyle() {

                        Style style = maplibreMap.get().getStyle();
                        updateMapBackground();

                        for (Layer layer :maplibreMap.get().getStyle().getLayers()){
                            if (!layer.getId().equals("background"))
                                style.removeLayer(layer);
                        }

                        if (createSource) {
                            for (Source source : maplibreMap.get().getStyle().getSources()) {
                                boolean result = style.removeSource(source);
                            }
                        }

                        if (createSource) {
                            selectedDotSource = new GeoJsonSource("selected-dot-source", FeatureCollection.fromFeatures(emptyList()));
                            style.addSource(selectedDotSource);

                            signaturesRootLayerSource = new GeoJsonSource("signature-root-source", FeatureCollection.fromFeatures(emptyList()));
                            style.addSource(signaturesRootLayerSource);
                        }

                        selectedDotCircleLayer = new CircleLayer("selected-dot-layer", "selected-dot-source")
                                .withProperties(PropertyFactory.circleStrokeWidth(1f),
                                        PropertyFactory.circleStrokeColor("#000000"));
                        selectedDotCircleLayer.setProperties(
                                PropertyFactory.circleRadius(Expression.get("radius")),
                                PropertyFactory.circleColor(Expression.get("color")),
                                PropertyFactory.circleOpacity(1.0f));
                        style.addLayer(selectedDotCircleLayer);

                        signaturesRootLayer = new CircleLayer("signatures-root-layer", "signature-root-source");
                        style.addLayerBelow(signaturesRootLayer, "selected-dot-layer");

                        if (createSource) {
                            selectedPolySource = new GeoJsonSource("selected-poly-source", FeatureCollection.fromFeatures(emptyList()));
                            style.addSource(selectedPolySource);
                        }

                        LineLayer lineLayer = new LineLayer("selected-polygon-line", "selected-poly-source")
                                .withProperties(
                                        PropertyFactory.lineColor(Expression.get("color")),
                                        PropertyFactory.lineWidth(2.0f) );
                        style.addLayer(lineLayer);

                        fillPolyEditLayer = new FillLayer("selected-polygon-fill" ,"selected-poly-source" )
                                .withProperties(
                                        PropertyFactory.fillColor("#FF00FF"),
                                        PropertyFactory.fillOpacity(0.2f));

                        // edit layer source
                        if (createSource) {
                            vertexSource = new GeoJsonSource("vertex-source", FeatureCollection.fromFeatures(emptyList()));
                            style.addSource(vertexSource);
                        }

                        CircleLayer vertexFillLayer = new CircleLayer("vertex-layer", "vertex-source")
                                .withProperties(PropertyFactory.circleStrokeWidth(1f),
                                        PropertyFactory.circleStrokeColor("#000000"));
                        vertexFillLayer.setProperties(
                                PropertyFactory.circleRadius(Expression.get("radius")),
                                PropertyFactory.circleColor(Expression.get("color")),
                                PropertyFactory.circleOpacity(1.0f));

                        style.addLayer(vertexFillLayer);



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
                                        PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP));
                        style.addLayer(locationLayer);


                        // TRACKING
                        // saved track line
                        if (createSource) {
                            tracksLineSource = new GeoJsonSource("track-line-source", FeatureCollection.fromFeatures(tracksFeatures));
                            style.addSource(tracksLineSource);
                        }

                        Layer trackLayer = new LineLayer( "track-line-layer", "track-line-source").
                                withProperties(
                                        PropertyFactory.lineColor("#0000FF"),
                                        PropertyFactory.lineWidth(getMPLThinkness(5)));
                        style.addLayer(trackLayer);

                        // track in progress
                        if (createSource) {
                            trackInProgressSource = new GeoJsonSource("track-inprogress-source", FeatureCollection.fromFeatures(new ArrayList<>()));
                            style.addSource(trackInProgressSource);
                        }

                        Layer trackInProgressLayer = new LineLayer( "track-inprogress-layer", "track-inprogress-source").
                                withProperties(
                                        PropertyFactory.lineColor("#0000FF"),
                                        PropertyFactory.lineWidth(getMPLThinkness(5)));
                        style.addLayer(trackInProgressLayer);


                        // tracks start stop flags
                        if (createSource) {
                            tracksFlagsSource = new GeoJsonSource("track-flag-source", FeatureCollection.fromFeatures(tracksFlagsFeatures));
                            style.addSource(tracksFlagsSource);
                        }

                        final Drawable drawableGreenFlag = getContext().getResources().getDrawable( R.drawable.ic_track_flag);
                        final Bitmap bitmapFlagStart = drawableToBitmap(drawableGreenFlag);
                        Bitmap greenMarker = recolorBitmap(bitmapFlagStart, Color.GREEN);
                        String iconFlagStart = "user-marker-flag-start";
                        style.addImage(iconFlagStart, greenMarker);

                        final Drawable drawableRedFlag = getContext().getResources().getDrawable( R.drawable.ic_track_flag);
                        final Bitmap bitmapFlagEnd = drawableToBitmap(drawableRedFlag);
                        Bitmap redBitmap = recolorBitmap(bitmapFlagEnd, Color.RED);
                        String iconFlagEnd = "user-marker-flag-end";
                        style.addImage(iconFlagEnd, redBitmap);

                        SymbolLayer trackFlagsLayer = new SymbolLayer("track-flags-layer", "track-flag-source")
                                .withProperties(
                                        PropertyFactory.iconImage(
                                                Expression.switchCase(
                                                        Expression.eq(Expression.get("type"), Expression.literal(true)), Expression.literal("user-marker-flag-start"),
                                                        Expression.eq(Expression.get("type"), Expression.literal(false)), Expression.literal("user-marker-flag-end"),
                                                        Expression.literal("user-marker-flag-start"))),
                                        PropertyFactory.iconSize(1.0f),
                                        PropertyFactory.iconAllowOverlap(true),
                                        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM_LEFT));
                        style.addLayer(trackFlagsLayer);


                        // marker
                        final Drawable drawable = getContext().getResources().getDrawable( R.drawable.ic_action_anchor_2);
                        final Bitmap bitmap = drawableToBitmap(drawable);

                        final IconFactory iconFactory = IconFactory.getInstance(getContext());
                        final Icon markerIcon = iconFactory.fromBitmap(bitmap);
                        String iconId = "marker-icon-selected";
                        style.addImage(iconId, bitmap);

                        // marker layer
                        markerFeatureCollection = FeatureCollection.fromFeatures(new ArrayList<>());

                        if (createSource) {
                            markerSource = new GeoJsonSource("marker-source", markerFeatureCollection);
                            style.addSource(markerSource);
                        }

                        SymbolLayer symbolLayer = new SymbolLayer("marker-layer", "marker-source")
                                .withProperties(
                                        org.maplibre.android.style.layers.PropertyFactory.iconImage(iconId),
                                        org.maplibre.android.style.layers.PropertyFactory.iconAnchor(org.maplibre.android.style.layers.Property.ICON_ANCHOR_TOP_LEFT));
                        style.addLayer(symbolLayer);

                        if (createSource) {
                            locationSource = new GeoJsonSource("user-location-source", Point.fromLngLat(-100.0, -100.0));
                            style.addSource(locationSource);
                        }

                        List<Map.Entry<Integer, List<org.maplibre.geojson.Feature>>> listOf = new ArrayList<>(sourcesOrder.entrySet());
                        Collections.reverse(listOf);

                        for (Map.Entry<Integer, List<org.maplibre.geojson.Feature>> entry : listOf) {
                            // create source and FillLayer put to style

                            if (createSource)
                                createSourceForLayer(entry.getKey(), layersType.get(entry.getKey()),
                                        sourceFeaturesHashMap.get(entry.getKey()), style,
                                        sourceHashMap, rasterLayersURLMap, rasterLayersTmsTypeMap,
                                        layersPath.get(entry.getKey()));

                            createFillLayerForLayer(entry.getKey(),
                                    layersType.get(entry.getKey()),
                                    style,
                                    layersHashMap,
                                    layersHashMap2,
                                    symbolsLayerHashMap,
                                    layersStyle.get(entry.getKey()), false,
                                    getLayerById(entry.getKey()),
                                    layersPath.get(entry.getKey()), selectedDotCircleLayer,
                                    signaturesRootLayer);

                            checkLayerVisibility(entry.getKey());
                        }

                        ((IGISApplication)getContext().getApplicationContext()).setGetingStyleInProgress(false);
                        mapFragment.get().changeProgress(((IGISApplication)getContext().getApplicationContext()).getGetingStyleInProgress());
                    }

                });

                // Set map style NG
                ((IGISApplication)getContext().getApplicationContext()).setGetingStyleInProgress(true);
                maplibreMap.get().setStyle(new Style.Builder().fromJson(styleJson));
            });
        });
        executor.shutdown();
    }

    public void loadLayersToMaplibreMapLite(final  List<ILayer> allLayers, boolean skipUserLayers){
        Style style = maplibreMap.get().getStyle();
        for (Layer layer : maplibreMap.get().getStyle().getLayers()) {
            if (!layer.getId().equals("background"))
                style.removeLayer(layer);
        }

        selectedDotCircleLayer = new CircleLayer("selected-dot-layer", "selected-dot-source")
                .withProperties(PropertyFactory.circleStrokeWidth(1f),
                        PropertyFactory.circleStrokeColor("#000000"));
        selectedDotCircleLayer.setProperties(
                PropertyFactory.circleRadius(Expression.get("radius")),
                PropertyFactory.circleColor(Expression.get("color")),
                PropertyFactory.circleOpacity(1.0f));
        style.addLayer(selectedDotCircleLayer);

        signaturesRootLayer = new CircleLayer("signatures-root-layer", "signature-root-source");
        style.addLayerBelow(signaturesRootLayer, "selected-dot-layer");

        LineLayer lineLayer = new LineLayer("selected-polygon-line", "selected-poly-source")
                .withProperties(
                        PropertyFactory.lineColor(Expression.get("color")),
                        PropertyFactory.lineWidth(2.0f) );
        style.addLayer(lineLayer);

        fillPolyEditLayer = new FillLayer("selected-polygon-fill" ,"selected-poly-source" )
                .withProperties(
                        PropertyFactory.fillColor("#FF00FF"),
                        PropertyFactory.fillOpacity(0.2f));


        CircleLayer vertexFillLayer = new CircleLayer("vertex-layer", "vertex-source")
                .withProperties(PropertyFactory.circleStrokeWidth(1f),
                        PropertyFactory.circleStrokeColor("#000000"));
        vertexFillLayer.setProperties(
                PropertyFactory.circleRadius(Expression.get("radius")),
                PropertyFactory.circleColor(Expression.get("color")),
                PropertyFactory.circleOpacity(1.0f));

        style.addLayer(vertexFillLayer);

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
                        PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP));
        style.addLayer(locationLayer);

        // TRACKING
        // saved track line
        Layer trackLayer = new LineLayer( "track-line-layer", "track-line-source").
                withProperties(
                        PropertyFactory.lineColor("#0000FF"),
                        PropertyFactory.lineWidth(getMPLThinkness(5)));
        style.addLayer(trackLayer);

        // track in progress
        Layer trackInProgressLayer = new LineLayer( "track-inprogress-layer", "track-inprogress-source").
                withProperties(
                        PropertyFactory.lineColor("#0000FF"),
                        PropertyFactory.lineWidth(getMPLThinkness(5)));
        style.addLayer(trackInProgressLayer);

        // tracks start stop flags
        SymbolLayer trackFlagsLayer = new SymbolLayer("track-flags-layer", "track-flag-source")
                .withProperties(
                        PropertyFactory.iconImage(
                                Expression.switchCase(
                                        Expression.eq(Expression.get("type"), Expression.literal(true)), Expression.literal("user-marker-flag-start"),
                                        Expression.eq(Expression.get("type"), Expression.literal(false)), Expression.literal("user-marker-flag-end"),
                                        Expression.literal("user-marker-flag-start"))),
                        PropertyFactory.iconSize(1.0f),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM_LEFT));
        style.addLayer(trackFlagsLayer);

        // marker
        final Drawable drawable = getContext().getResources().getDrawable( R.drawable.ic_action_anchor_2);
        final Bitmap bitmap = drawableToBitmap(drawable);

        final IconFactory iconFactory = IconFactory.getInstance(getContext());
        final Icon markerIcon = iconFactory.fromBitmap(bitmap);
        String iconId = "marker-icon-selected";
        style.addImage(iconId, bitmap);

        SymbolLayer symbolLayer = new SymbolLayer("marker-layer", "marker-source")
                .withProperties(
                        org.maplibre.android.style.layers.PropertyFactory.iconImage(iconId),
                        org.maplibre.android.style.layers.PropertyFactory.iconAnchor(org.maplibre.android.style.layers.Property.ICON_ANCHOR_TOP_LEFT));
        style.addLayer(symbolLayer);

        if (!skipUserLayers) {
            final Map<Integer, Integer> layersType = new HashMap<>();
            final Map<Integer, com.nextgis.maplib.display.Style> layersStyle = new HashMap<>();
            final Map<Integer, String> rasterLayersURLMap = new HashMap<>();
            final Map<Integer, Integer> rasterLayersTmsTypeMap = new HashMap<>();

            final List<org.maplibre.geojson.Feature> tracksFeatures = new ArrayList<>();
            final List<org.maplibre.geojson.Feature> tracksFlagsFeatures = new ArrayList<>();

            final AccountManager accountManager = AccountManager.get(getContext());
            final Connections connections = fillConnections(getContext(), accountManager);

            sourcesOrder.clear();

            for (ILayer iLayer : allLayers) {
//            Log.e("MPLREM",  "iterate layer " + iLayer.getName());

                if (iLayer instanceof VectorLayer) {
                    VectorLayer layer = (VectorLayer) iLayer;
                    layersType.put(layer.getId(), layer.getGeometryType());

                    layersStyle.put(layer.getId(), layer.getDefaultStyleNoExcept());

                    List<org.maplibre.geojson.Feature> vectorFeatures = sourceFeaturesHashMap.get(layer.getId());
                    // be more lite - get features from saved hash
                    //sourceFeaturesHashMap.put(layer.getId(), vectorFeatures);
                    //List<org.maplibre.geojson.Feature> vectorFeatures = createFeatureListFromLayer(layer);
                    //sourceFeaturesHashMap.put(layer.getId(), vectorFeatures);
                    sourcesOrder.put(layer.getId(), new ArrayList<>());
                } else if (iLayer instanceof TrackLayer) {
                    TrackLayer layer = (TrackLayer) iLayer;
                    layersType.put(layer.getId(), GT_TRACK_WA);
                    tracksFeatures.clear();
                    tracksFeatures.addAll(createFeatureListFromTrackLayer(layer));

                    tracksFlagsFeatures.clear();
                    tracksFlagsFeatures.addAll(createFeatureListFlagsFromTrackLayer(layer));
                } else if (iLayer instanceof NGWRasterLayer) {
                    // need add auth
                    Connection found = null;
                    if (iLayer instanceof NGWRasterLayer) {
                        for (int i = 0; i < connections.getChildrenCount(); i++) {
                            if (connections.getChild(i).getName().equals((((NGWRasterLayer) iLayer).getAccountName()))) {
                                found = (Connection) connections.getChild(i);
                                String basicAuth = getHTTPBaseAuth(found.getLogin(), found.getPassword());
                                if (null != basicAuth) {
                                    final String url = ((NGWRasterLayer) iLayer).getURL();
                                    final String getBaseUrl = getBaseUrlpart(url);
                                    final String resPart = "resource=" + extractResourceValue(url);
                                    final String[] authPart = new String[4];
                                    authPart[0] = getBaseUrl;
                                    authPart[1] = resPart;
                                    authPart[2] = basicAuth;
                                    authPart[3] = iLayer.getPath().toString();
                                    ((IGISApplication) getContext().getApplicationContext()).updateAuthPair(authPart);
                                    break;
                                }
                            }
                        }

                        TMSLayer layer = (TMSLayer) iLayer;
                        layersType.put(layer.getId(), GT_RASTER_WA);
                        rasterLayersURLMap.put(layer.getId(), ((NGWRasterLayer) layer).getURL());
                        sourceFeaturesHashMap.put(layer.getId(), new ArrayList<>());
                        sourcesOrder.put(layer.getId(), new ArrayList<>());
                    }
                } else if (iLayer instanceof RemoteTMSLayer) {
                    final String url = ((RemoteTMSLayer) iLayer).getURL();
                    final String getBaseUrl = getBaseUrlpart(url);
                    final String resPart = "resource=" + extractResourceValue(url);
                    final String[] authPart = new String[4];
                    authPart[0] = getBaseUrl;
                    authPart[1] = resPart;
                    authPart[2] = "no";//no auth RemoteTMSLayer - geoservice map
                    authPart[3] = iLayer.getPath().toString();
                    ((IGISApplication) getContext().getApplicationContext()).updateAuthPair(authPart);

                    TMSLayer layer = (TMSLayer) iLayer;
                    layersType.put(layer.getId(), GT_RASTER_WA);
                    if (((RemoteTMSLayer) layer).mIsOfflineLayer) {
                        rasterLayersURLMap.put(layer.getId(), "file://" + (layer).getPath().toString() + "/{z}/{x}/{y}.tile");
                        rasterLayersTmsTypeMap.put(layer.getId(), layer.getTMSType());
                    } else {
                        rasterLayersURLMap.put(layer.getId(), ((RemoteTMSLayer) layer).getURLSubdomain());
                        rasterLayersTmsTypeMap.put(layer.getId(), layer.getTMSType());
                    }
                    sourceFeaturesHashMap.put(layer.getId(), new ArrayList<>());
                    sourcesOrder.put(layer.getId(), new ArrayList<>());
                } else if (iLayer instanceof LocalTMSLayer) {
                    TMSLayer layer = (TMSLayer) iLayer;
                    layersType.put(layer.getId(), GT_RASTER_WA);
                    rasterLayersURLMap.put(layer.getId(), "file://" + (layer).getPath().toString() + "/{z}/{x}/{y}.tile");
                    rasterLayersTmsTypeMap.put(layer.getId(), layer.getTMSType());
                    sourceFeaturesHashMap.put(layer.getId(), new ArrayList<>());
                    sourcesOrder.put(layer.getId(), new ArrayList<>());
                }
            }



            List<Map.Entry<Integer, List<org.maplibre.geojson.Feature>>> listOf = new ArrayList<>(sourcesOrder.entrySet());
            Collections.reverse(listOf);

            for (Map.Entry<Integer, List<org.maplibre.geojson.Feature>> entry : listOf) {

                createFillLayerForLayer(entry.getKey(),
                        layersType.get(entry.getKey()),
                        style,
                        layersHashMap,
                        layersHashMap2,
                        symbolsLayerHashMap,
                        layersStyle.get(entry.getKey()), false,
                        getLayerById(entry.getKey()),
                        getLayerById(entry.getKey()).getPath().toString(), selectedDotCircleLayer,
                        signaturesRootLayer);

                checkLayerVisibility(entry.getKey());
            }
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        android.graphics.PointF screenPoint = new android.graphics.PointF(event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (mapFragment.get()!= null)
                    mapFragment.get().setLongLongClickProcesses(false);
                clickPoint = new PointF(event.getX(), event.getY());
                android.graphics.RectF rect = new android.graphics.RectF(event.getX() - 20,event.getY() - 20,event.getX() + 20,event.getY() + 20);
                List<org.maplibre.geojson.Feature> featuresMarker = maplibreMap.get().queryRenderedFeatures(rect, "marker-layer");

                if (mapFragment.get().getMode() == MODE_EDIT_BY_TOUCH ){
                    isDraggingByTouchGPS = true;
                    startEvent = event;

                    if (editingObject != null)
                        editingObject.hideVertext();
                    return true;
                }

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
                            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);

                        }
                        Point point = ((Point)clickedFeature.geometry());
                        setMarker(new LatLng(point.latitude(), point.longitude()));
                        isDragging = true;
                        return true;
                    }

                    if (features.get(0).hasNonNullValueForProperty("index")) {
                        clickedFeature = features.get(0);
                        index = features.get(0).getNumberProperty("index").intValue();
                        if (editingObject != null && editingObject instanceof  MeasurmentLine)
                            isMeasurmentChangeVertex = true;
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

                if (isDraggingByTouchGPS && editingObject!= null){
                    if (mapFragment.get().getMode() == MODE_EDIT_BY_TOUCH){
                        PointF  newPoint = new PointF(screenPoint.x,screenPoint.y);
                        LatLng latLng = maplibreMap.get().getProjection().fromScreenLocation(newPoint);
                        editingObject.addNewFlowPoint(latLng);
                        editingObject.updateEditingPolygonAndVertex();
                        return true;
                    }
                }

                if (isDragging && selectedVertexIndex != -1) {
                    if (!hasEditeometry) {
                        hasEditeometry = true;
                    }
                    if(deltaPoint == null && startEvent != null){
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
                if (mapFragment.get()!=null && mapFragment.get().getLongLongClickProcesses()){
                    mapFragment.get().setLongLongClickProcesses(false);
                    return false;
                }

                float deltaX = clickPoint.x - event.getX();
                float deltaY = clickPoint.y - event.getY();
                float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                if (isDraggingByTouchGPS){

                    mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);

//                    if (editingObject != null)
//                        editingObject.showVertext();

                    isDraggingByTouchGPS = false;
                    isDragging = false;
                    isDraggingByTouchGPS = false;
                    isSwitchVertex = false;
                    deltaPoint = null;
                    startEvent = null;
                    return false;
                } else {


                    if (!isDragging)
                        if (distance < 5) {
                            if (editingObject != null && editingObject instanceof MeasurmentLine){
                                if (!isMeasurmentChangeVertex) {
                                    android.graphics.PointF touchscreenPoint = new android.graphics.PointF(event.getX(), event.getY());
                                    LatLng latLng = maplibreMap.get().getProjection().fromScreenLocation(touchscreenPoint); // todo add tolerance and rect
                                    editingObject.addNewFlowPoint(latLng);
                                    setMarker(latLng);
                                    editingObject.updateEditingPolygonAndVertex();
                                    updateMeasurmentCaptions(editingObject);

                                } else
                                    isMeasurmentChangeVertex = false;
                                return false;

                            } else
                                mapFragment.get().processMapClick(screenPoint.x, screenPoint.y);
                        }
                    clickPoint = null;

                    if (isDragging || isSwitchVertex) {
                        if (editingObject != null) {
                            mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
                            editingObject.regenerateVertexFeatures();
                            editingObject.displayMiddlePoints(false, true);
                            LatLng pointReleased = editingObject.getSelectedPoint();

                            if (pointReleased != null)
                                setMarker(pointReleased);

                            if (editingObject  instanceof  MeasurmentLine)
                                updateMeasurmentCaptions(editingObject);
                        } else
                            setMarker(event);
                    }
                }
                isDragging = false;
                isDraggingByTouchGPS = false;
                isSwitchVertex = false;
                deltaPoint = null;
                startEvent = null;
                return false;
            }
        }
        return false;
    }


    public void updateHistoryByWalkEnd(){
        mapFragment.get().updateGeometryFromMaplibre(editingObject.editingFeature, originalSelectedFeature, editingObject);
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

        boolean result = mapFragment.get().processMapLongClick(clickeEnelope, clickPoint);
        if (result && mapFragment.get()!= null)
            mapFragment.get().setLongLongClickProcesses(true);
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

    public void startFeatureSelectionForView(ILayer layerd, Feature originalSelectedFeature){
        if (layerd == null)
            return;
        Long selectedFeatureId = originalSelectedFeature.getId();

        if (editingFeature != null  ){
            Integer lID = Integer.valueOf(editingFeature.getStringProperty(prop_layerid));
            Long fID = Long.valueOf(editingFeature.getStringProperty(prop_featureid));

            if (layerd.getId() == lID && selectedFeatureId.equals(fID))
                return;
            // need unselect feature
            unselectFeatureFromView();
            return;
        }

        List<org.maplibre.geojson.Feature> layerFeatures = sourceFeaturesHashMap.get(layerd.getId());

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

            int type = ((VectorLayer)layerd).getGeometryType();

            if  (type == GTPoint || type == GTMultiPoint) {
                selectedDotSource.setGeoJson(featureSelected);
            }

            if  (type == GeoConstants.GTPolygon || type == GTMultiPolygon
                    || type == GTLineString ||type == GTMultiLineString ) {
                selectedPolySource.setGeoJson(featureSelected);
            }

            this.originalSelectedFeature = originalSelectedFeature;
        }
    }


    public void startFeatureSelectionForEdit(final ILayer  ilayerd, Integer layerGeoType,
                                             Feature originalSelectedFeature, boolean createNew,
                                             com.nextgis.maplib.display.Style ngstyle){

        Long selectedFeatureId = originalSelectedFeature.getId();

        // clear prev edit state
        if (editingObject != null) {
            if (editingFeature != null) {
                Integer lID = Integer.valueOf(editingFeature.getStringProperty(prop_layerid));
                Long fID = Long.valueOf(editingFeature.getStringProperty(prop_featureid));

                if (ilayerd.getId() != lID || !selectedFeatureId.equals(fID)) {
                    // need clear previous edited obj
                    unselectFeatureFromEdit(false, false);
                } else {
                    // same obj - do nothing
                    return;
                }
            }
        }

        // clear sources
        selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        selectedDotSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        vertexSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));


        List<org.maplibre.geojson.Feature> layerFeatures = sourceFeaturesHashMap.get(ilayerd.getId());
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
            int type = ((VectorLayer)ilayerd).getGeometryType();

            LatLng center = null;
            if (originalSelectedFeature != null && originalSelectedFeature.getGeometry() != null
                    && originalSelectedFeature.getGeometry() instanceof  GeoPoint){
                center = latLngPointFromGeoPoint((GeoPoint) originalSelectedFeature.getGeometry());
            } else {
                center = maplibreMap.get().getCameraPosition().target;
            }

            Projection projection = maplibreMap.get().getProjection();
            Point point = Point.fromLngLat(center.getLongitude(), center.getLatitude());

            switch (type){
                case GTPoint :
                    feature = org.maplibre.geojson.Feature.fromGeometry(point);
                    break;

                case GTMultiPoint:
                    MultiPoint mpoint = MultiPoint.fromLngLats(Arrays.asList(point));
                    feature = org.maplibre.geojson.Feature.fromGeometry(mpoint);
                    break;

                case GeoConstants.GTPolygon:
                    List<List<org.maplibre.geojson.Point>> polyList = new ArrayList<>();
                    polyList.add(createPointsForRing(center, maplibreMap.get().getProjection(),  true));
                    Polygon polygon = Polygon.fromLngLats(polyList);
                    feature = org.maplibre.geojson.Feature.fromGeometry(polygon);
                    break;

                case GTMultiPolygon:
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

            feature.addStringProperty(prop_layerid, String.valueOf(ilayerd.getId()));

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
                        feature.addStringProperty(prop_signature_text, "dsf sdf s"); //getSpaceCorrectedText(signature));
                    }
                }
            }


            int size = sourceFeaturesHashMap.get(ilayerd.getId()).size();
            feature.addStringProperty(prop_order, String.valueOf(size+1));
            feature.addStringProperty(prop_featureid, String.valueOf(originalSelectedFeature.getId()));
            editingFeatureTmp = copyFeature(feature);
        }

        if (editingFeatureTmp != null) {
            selectedEditedSource = sourceHashMap.get(ilayerd.getPath().toString());
            editingFeature = editingFeatureTmp;

            int type = ((VectorLayer)ilayerd).getGeometryType();
            GeoJsonSource choosed = null;
            if  (type == GTPoint || type == GTMultiPoint) {
                selectedDotSource.setGeoJson(FeatureCollection.fromFeature(editingFeature));
                choosed = selectedDotSource;
                editingFeature.addStringProperty("color", colorRED);
            }

            if  (type == GeoConstants.GTPolygon || type == GTMultiPolygon) {
                selectedPolySource.setGeoJson(FeatureCollection.fromFeature(editingFeature));
                choosed = selectedPolySource;
            }

            if  (type == GTLineString || type == GTMultiLineString) {
                selectedPolySource.setGeoJson(FeatureCollection.fromFeature(editingFeature));
                choosed = selectedPolySource;
            }

            editingFeatureOriginal = copyFeature(editingFeatureTmp);

            polygonFeatures = sourceFeaturesHashMap.get(ilayerd.getId());
            this.originalSelectedFeature = originalSelectedFeature;

            // choose layer
            editingObject = MPLFeaturesUtils.createEditObject(layerGeoType,
                    selectedEditedSource,
                    editingFeature,
                    polygonFeatures,
                    choosed,
                    vertexSource,
                    markerSource,
                    ilayerd.getPath().toString());

            Layer layer = maplibreMap.get().getStyle().getLayer("selected-polygon-fill");
            if (layerGeoType ==GeoConstants.GTPolygon ||
                    layerGeoType == GTMultiPolygon){
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
            selectedEditedSource = sourceHashMap.get(getLayerById(layerdID).getPath().toString());
            editingFeature = editingFeatureTmp;
            editingFeatureOriginal = editingFeatureTmp;
            polygonFeatures = sourceFeaturesHashMap.get(layerdID);

            Iterator<org.maplibre.geojson.Feature> it = polygonFeatures.iterator();
            String targetOrder = editingFeature.getStringProperty(MPLFeaturesUtils.prop_order);

            while (it.hasNext()) {
                org.maplibre.geojson.Feature f = it.next();
                if (Objects.equals(f.getStringProperty(MPLFeaturesUtils.prop_order), targetOrder)) {
                    it.remove();
                }
            }

            //polygonFeatures.removeIf(f -> Objects.equals(f.getStringProperty(prop_order), editingFeature.getStringProperty(prop_order)));
            selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(polygonFeatures));
            // need check sign
            ILayer iLayer = getLayerById(layerdID);
            VectorLayer vectorLayer = null;
            if (iLayer instanceof  VectorLayer)
                vectorLayer = (VectorLayer)iLayer;

            if (vectorLayer != null){
                if (vectorLayer.mGeometryType == GTPolygon || vectorLayer.mGeometryType == GTMultiPolygon){
                    reAssembleSignPoly(
                            maplibreMap.get().getStyle(),
                            polygonFeatures,
                            vectorLayer.getPath().toString());
                }
            }
        }
        editingFeature = null;
        editingFeatureOriginal = null;
        selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        selectedDotSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
    }

    public void hideFeature(Long selectedFeatureId, int layerdID){
        List<org.maplibre.geojson.Feature> layerFeatures = sourceFeaturesHashMap.get(layerdID);
        org.maplibre.geojson.Feature found = null;
        for (org.maplibre.geojson.Feature feature : layerFeatures){
            if (feature.getStringProperty(prop_featureid).equals(String.valueOf(selectedFeatureId))) {
                found = feature;
                break;
            }
        }
        GeoJsonSource source = sourceHashMap.get(getLayerById(layerdID).getPath().toString());

        if (found != null && source != null){
            String fid = String.valueOf(selectedFeatureId);
            Iterator<org.maplibre.geojson.Feature> it = layerFeatures.iterator();
            while (it.hasNext()) {
                org.maplibre.geojson.Feature f = it.next();
                if (Objects.equals(f.getStringProperty(prop_featureid), fid)) {
                    it.remove();
                    hiddedFeature = f;
                    hiddedFeatureId =selectedFeatureId;
                    hiddedlayerdID = layerdID;

                }
            }

            if (getLayerById(layerdID) instanceof VectorLayer vectorLayer){
                if (vectorLayer.mGeometryType == GTPolygon || vectorLayer.mGeometryType == GTMultiPolygon){
                    reAssembleSignPoly(
                            maplibreMap.get().getStyle(),
                            layerFeatures,
                            getLayerById(layerdID).getPath().toString());
                }
            }

            source.setGeoJson(FeatureCollection.fromFeatures(layerFeatures));
            selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
            selectedDotSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));

        }
    }

    public void showFeatureFromHide(Long selectedFeatureId, int layerdID, org.maplibre.geojson.Feature hiddedFeature){
        List<org.maplibre.geojson.Feature> layerFeatures = sourceFeaturesHashMap.get(layerdID);
        GeoJsonSource source = sourceHashMap.get(getLayerById(layerdID).getPath().toString());

        if (hiddedFeature != null && source != null && layerFeatures != null){
            layerFeatures.add(hiddedFeature);

            source.setGeoJson(FeatureCollection.fromFeatures(layerFeatures));
            selectedPolySource.setGeoJson(FeatureCollection.fromFeature(hiddedFeature));
            //selectedDotSource.setGeoJson(FeatureCollection.fromFeature(hiddedFeature));

        }
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
        unselectFeatureFromEdit(backToOriginal, false);
        hideMarker();

        Layer layer = maplibreMap.get().getStyle().getLayer("selected-polygon-fill");
        if (layer != null)
            maplibreMap.get().getStyle().removeLayer(fillPolyEditLayer);


        if (originalSelectedFeature!= null && originalSelectedFeature.getId() != -1)
            startFeatureSelectionForView(mapFragment.get().getSelectedLayer(), originalSelectedFeature );
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

    public void hideVertex(){
        if (editingObject != null)
            editingObject.hideVertext();
    }

    public void showVertex(){
        if (editingObject != null)
            editingObject.showVertext();
    }

    public void showMarker(){
        if (editingObject != null)
            editingObject.showCurrentMarker();
    }

    public void unselectFeatureFromEdit(boolean backToOriginal, boolean keepEditObj) {
        if (editingObject != null && editingObject.editingFeature != null) {
            if (backToOriginal) {
                copyProperties(editingFeatureOriginal, editingObject.editingFeature);
            }

            hasEditeometry = false;
            org.maplibre.geojson.Feature target = backToOriginal ? editingFeatureOriginal : editingObject.editingFeature;

            boolean needChangeFeature = true;
            if (target != null && target.hasNonNullValueForProperty(prop_featureid) &&
                    target.getStringProperty(prop_featureid).equals("-1")) {
                needChangeFeature = false;
            }

            if (needChangeFeature) {
                // remove old - add new
                String targetOrder = target.getStringProperty(prop_order);
                Iterator<org.maplibre.geojson.Feature> it = polygonFeatures.iterator();
                while (it.hasNext()) {
                    org.maplibre.geojson.Feature f = it.next();
                    if (Objects.equals(f.getStringProperty(prop_order), targetOrder)) {
                        it.remove();
                        break;
                    }
                }
                target.addStringProperty("color", colorLightBlue);

                polygonFeatures.add(target);
            }

            if (!keepEditObj) {
                selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(polygonFeatures));

                // re-assemble signs for poly
                reAssembleSignPoly(maplibreMap.get().getStyle(),
                        polygonFeatures,
                        editingObject.layerPath);

                GeoJsonSource choosed = null;
                if (editingObject instanceof PointEditClass ||
                        editingObject instanceof MultiPointEditClass) {
                    choosed = selectedDotSource;
                } else {
                    choosed = selectedPolySource;
                }

                org.maplibre.geojson.Feature featureToRecolor = backToOriginal ? editingFeatureOriginal : editingObject.editingFeature;
                featureToRecolor.addStringProperty("color", colorLightBlue);
                // color for selection

                if  (keepEditObj)
                    choosed.setGeoJson(FeatureCollection.fromFeature(featureToRecolor));
                else
                    choosed.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));

                // clear vertex
                vertexSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));

                // fill layer remove
                Layer layer = maplibreMap.get().getStyle().getLayer("selected-polygon-fill");
                if (layer != null) {
                    maplibreMap.get().getStyle().removeLayer(fillPolyEditLayer);
                }

                // clear edited objects
                editingObject = null;
                editingFeatureOriginal = null;
                editingFeature = null;
                //originalSelectedFeature = null;
            }
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
            GeoPoint center, boolean startSecondMaplibre)
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
            zoomSaved = zoom;
            centerSaved = center;
        }

        if (!startSecondMaplibre)
            if (maplibreMap.get()!= null)
                maplibreMap.get().easeCamera(CameraUpdateFactory.newLatLngZoom(latLngPointFromGeoPoint(center), zoom), 800);
    }


    public final GeoPoint getMaplibreCenter() {
        if (maplibreMap.get() != null) {

            LatLng center =  maplibreMap.get().getCameraPosition().target;
            double[] centerPoints = convert4326To3857(center.getLongitude(), center.getLatitude());

            return new GeoPoint(centerPoints[0], centerPoints[1]);
        }
        return null;
    }

    @Override
    public void zoomToExtent(GeoEnvelope envelope) {
        zoomToExtent(envelope, getMaxZoom() ,true);
    }

    public void zoomToExtent(GeoEnvelope envelope, float maxZoom, boolean startSecondMaplibre) {
        if (envelope.isInit()) {
            double size = GeoConstants.MERCATOR_MAX * 2;
            double scale = Math.min(envelope.width() / size, envelope.height() / size);
            double zoom = MapUtil.lg(1 / scale);
            if (zoom < getMinZoom())
                zoom = getMinZoom();
            if (zoom > maxZoom)
                zoom = maxZoom;

            setZoomAndCenter((float) zoom, envelope.getCenter(), startSecondMaplibre);
            if (!startSecondMaplibre)
                return;

            // maplibre part
            double[] lonLat1 = convert3857To4326(envelope.getMaxX(), envelope.getMinY());
            double[] lonLat2 = convert3857To4326(envelope.getMinX(), envelope.getMaxY());

            LatLng sw = new LatLng(lonLat1[1], lonLat1[0]);
            LatLng ne = new LatLng(lonLat2[1], lonLat2[0]);
            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(sw)
                    .include(ne)
                    .build();
            maplibreMap.get().easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50), 800);
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

    @Override
    public GeoEnvelope getCurrentBounds() {

        if (maplibreMap.get()!= null){
            LatLngBounds bounds = maplibreMap.get().getProjection().getVisibleRegion().latLngBounds;

            LatLng swPoint = bounds.getSouthWest();
            LatLng nePoint = bounds.getNorthEast();


            double[] sw = convert4326To3857(swPoint.getLongitude(), swPoint.getLatitude());
            double[] ne = convert4326To3857(nePoint.getLongitude(), nePoint.getLatitude());

            return new GeoEnvelope(sw[0], ne[0], sw[1], ne[1]);

        }

        return null;
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

        if (maplibreMap.get() != null) {
            LatLng latLng = maplibreMap.get().getProjection().fromScreenLocation(new PointF((float) pt.getX(), (float) pt.getY()));
            GeoPoint result = geoPointFromLatLng(latLng);
            return result;
        }
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

    public void finishCreateNewFeature(
            long newFeatureID,
            VectorLayer layer){

        hideMarker();

        if (editingObject != null) {
            editingObject.finishCreateNewFeature(newFeatureID);

            boolean ruleStyle = false;
            if (layer.getRenderer() instanceof RuleFeatureRenderer) { // feature render
                ruleStyle = true;
            }

            String signatureField =  getLayerSignatureField(layer);
            com.nextgis.maplib.display.Style layerStyle = layer.getDefaultStyleNoExcept();
            String styleField = ((ITextStyle) layerStyle).getField();
            String styleText = ((ITextStyle) layerStyle).getText();

            boolean needSignatures = false;
            if (layer.getRenderer() instanceof RuleFeatureRenderer ||
                    !TextUtils.isEmpty(styleField) || !TextUtils.isEmpty(styleText)) {
                needSignatures = true;
            }
            String commonText = ((ITextStyle) layerStyle).getText();


            // get created feature with fields
            Uri uri = ContentUris.withAppendedId(layer.getContentUri(), newFeatureID);
            uri = uri.buildUpon().fragment("no_sync").build();

            // get it's cursor
            try {
                Cursor cursor = layer.query(uri, null, null, null, null, null);
                if (cursor.moveToFirst()) {
                    Feature newFeatureWithFields = layer.cursorToFeature(cursor);

                    // update new feature properties
                    applyTextAndStyle(
                            layer,
                            newFeatureWithFields,
                            editingObject.editingFeature,
                            layer.getGeometryType(),
                            ruleStyle,
                            needSignatures,
                            signatureField,
                            commonText);
                }
                cursor.close();
            } catch (Exception ex){
                Log.e("NGW", ex.getMessage());
            }

        }
        originalSelectedFeature.setId(newFeatureID);

        // WA for sign by id field for new feature
        if (editingObject.originalEditingFeature != null && editingObject.originalEditingFeature.getStringProperty(prop_signature_text) != null &&
                editingObject.originalEditingFeature.getStringProperty(prop_signature_text) .equals("-1"))
            editingObject.originalEditingFeature.addStringProperty(prop_signature_text, String.valueOf(newFeatureID));

        cancelFeatureEdit(false);
    }


    public void reloadFeatureToMaplibre(
            long newFeatureID,
            VectorLayer layer){

        if (viewedFeature != null) {


            boolean ruleStyle = false;
            if (layer.getRenderer() instanceof RuleFeatureRenderer) { // feature render
                ruleStyle = true;
            }

            String signatureField =  getLayerSignatureField(layer);
            com.nextgis.maplib.display.Style layerStyle = layer.getDefaultStyleNoExcept();
            String styleField = ((ITextStyle) layerStyle).getField();
            String styleText = ((ITextStyle) layerStyle).getText();

            boolean needSignatures = false;
            if (layer.getRenderer() instanceof RuleFeatureRenderer ||
                    !TextUtils.isEmpty(styleField) || !TextUtils.isEmpty(styleText)) {
                needSignatures = true;
            }
            String commonText = ((ITextStyle) layerStyle).getText();


            // get created feature with fields
            Uri uri = ContentUris.withAppendedId(layer.getContentUri(), newFeatureID);
            uri = uri.buildUpon().fragment("no_sync").build();

            // get it's cursor
            try {
                Cursor cursor = layer.query(uri, null, null, null, null, null);
                if (cursor.moveToFirst()) {
                    Feature newFeatureWithFields = layer.cursorToFeature(cursor);

                    // update new feature properties
                    applyTextAndStyle(
                            layer,
                            newFeatureWithFields,
                            viewedFeature,
                            layer.getGeometryType(),
                            ruleStyle,
                            needSignatures,
                            signatureField,
                            commonText);
                }
                cursor.close();
            } catch (Exception ex){
                Log.e("NGW", ex.getMessage());
            }


            // need add changes feature to list of objects


                // remove old - add new

            List<org.maplibre.geojson.Feature> targetFeatures = sourceFeaturesHashMap.get(layer.getId());
                String targetOrder = String.valueOf(newFeatureID);
                Iterator<org.maplibre.geojson.Feature> it = targetFeatures.iterator();
                while (it.hasNext()) {
                    org.maplibre.geojson.Feature f = it.next();
                    if (Objects.equals(f.getStringProperty(prop_order), targetOrder)) {
                        it.remove();
                        break;
                    }
                }

            targetFeatures.add(viewedFeature);

            GeoJsonSource targetSource = sourceHashMap.get(layer.getPath().toString());
            targetSource.setGeoJson(FeatureCollection.fromFeatures(targetFeatures));


//                // re-assemble signs for poly
            if (layer.getGeometryType() == GTPolygon || layer.getGeometryType() == GTMultiPolygon  )
                reAssembleSignPoly(maplibreMap.get().getStyle(),
                        targetFeatures,
                        layer.getPath().toString());



//                org.maplibre.geojson.Feature featureToRecolor = backToOriginal ? editingFeatureOriginal : editingObject.editingFeature;
//                featureToRecolor.addStringProperty("color", colorLightBlue);
                // color for selection

//                if  (keepEditObj)
//                    choosed.setGeoJson(FeatureCollection.fromFeature(featureToRecolor));
//                else
//                    choosed.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        }





//        // WA for sign by id field for new feature
//        if (editingObject.originalEditingFeature != null && editingObject.originalEditingFeature.getStringProperty(prop_signature_text) != null &&
//                editingObject.originalEditingFeature.getStringProperty(prop_signature_text) .equals("-1"))
//            editingObject.originalEditingFeature.addStringProperty(prop_signature_text, String.valueOf(newFeatureID));
//
//        cancelFeatureEdit(false);
    }

    public void checkLayerVisibility(int id){
        if (maplibreMap.get().getStyle() == null)
            return;

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


        if (isVisible && targetlayer instanceof VectorLayer){
            List<org.maplibre.geojson.Feature> features  = sourceFeaturesHashMap.get(targetlayer.getId());
            if (features == null || features.isEmpty() ){
                // layer was not uploaded by start - skipped
                // start load layer
                if (features == null)
                    addLayerByID(id);
                else //
                    reloadVectorLayerDataToMaplibre(targetlayer);
            }
        }


        if (targetlayer instanceof NGWRasterLayer || targetlayer instanceof  RemoteTMSLayer ||
                targetlayer instanceof  LocalTMSLayer){
            Layer layerRaster = getRasterLayer(id,  maplibreMap.get().getStyle());
            if (layerRaster != null){
                layerRaster.setProperties(visibility(isVisible ? VISIBLE:NONE));
            }
        }
        if (targetlayer instanceof TrackLayer ){

            Layer trackLayer = maplibreMap.get().getStyle().getLayer("track-line-layer");
            if (trackLayer!= null)
                trackLayer.setProperties(visibility(isVisible ? VISIBLE:NONE));


            Layer trackLayerFlags = maplibreMap.get().getStyle().getLayer("track-flags-layer");
            if (trackLayerFlags!= null)
                trackLayerFlags.setProperties(visibility(isVisible ? VISIBLE:NONE));

            Layer trackLayerInProgress = maplibreMap.get().getStyle().getLayer("track-inprogress-layer");
            if (trackLayerInProgress!= null)
                trackLayerInProgress.setProperties(visibility(isVisible ? VISIBLE:NONE));
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

                String currentNamePrefix = namePrefix;
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

    public void addPointByWalk(LatLng latLng){


        Log.e("POI", "add point " + latLng.getLatitude() + " " + latLng.getLongitude());

        if (editingObject != null) {
            editingObject.addNewFlowPoint(latLng);
            editingObject.updateEditingPolygonAndVertex();
        }
    }

    public void reloadCurrentTrackToMap(){
        if (maplibreMap.get() == null)
            return;
        Style style = maplibreMap.get().getStyle();
        if (style != null) {

            List<org.maplibre.geojson.Feature> tracksFeatures = createFeatureListFromCurrentTrack(getContext());

            if (tracksFeatures .size() > 0){
                GeoJsonSource tracksLineSource = (GeoJsonSource)style.getSource("track-inprogress-source");
                if (tracksLineSource!=null)
                    tracksLineSource.setGeoJson(FeatureCollection.fromFeatures(tracksFeatures));
            }
        }
    }

    static public List<org.maplibre.geojson.Feature> createFeatureListFromCurrentTrack(Context context) {

        List<org.maplibre.geojson.Feature> result = new ArrayList<>();

        List<Point> pointsList = new ArrayList<>();

        Cursor mCursor;
        final Uri mContentUriTracks;

        IGISApplication app = (IGISApplication) context.getApplicationContext();
        String authority = app.getAuthority();

        String[] mProjection = new String[] {TrackLayer.FIELD_ID};
        String   mSelection  = TrackLayer.FIELD_VISIBLE + " = 1 AND (" + TrackLayer.FIELD_END +
                " IS NULL OR " + TrackLayer.FIELD_END +
                " = '')";

        mContentUriTracks = Uri.parse("content://" + authority + "/" + TrackLayer.TABLE_TRACKS);
        mCursor = context.getContentResolver()
                .query(mContentUriTracks, mProjection, mSelection, null, null);


        if (mCursor == null || mCursor.getCount() == 0 || !mCursor.moveToFirst()) {
            return result;
        }

        String id = mCursor.getString(0);
        String[] proj = new String[] {TrackLayer.FIELD_LON, TrackLayer.FIELD_LAT};

        Cursor track = null;
        try {
            track = context.getContentResolver()
                    .query(Uri.withAppendedPath(mContentUriTracks, id), proj, null, null, null);
        }catch (Exception ex){
            Log.e(TAG, ex.getMessage());
            return result;
        }

        if (track == null || track.getCount() == 0 || !track.moveToFirst())
            return result;

        int lonInx = track.getColumnIndex(TrackLayer.FIELD_LON);
        int latInx = track.getColumnIndex(TrackLayer.FIELD_LAT);
        int i = 0;
        do {
            i++;
            float x1 = track.getFloat(lonInx);
            float y1 = track.getFloat(latInx);
            double[] lonLat = convert3857To4326(x1, y1);
            Point point1 = Point.fromLngLat(lonLat[0], lonLat[1]);
            pointsList.add(point1);
//                org.maplibre.geojson.Feature pointFeature1 = org.maplibre.geojson.Feature.fromGeometry(point1);
//                result.add(pointFeature1);

//                Log.e("TTRR", "point" + i + ": " + lonLat[0] + " : " +lonLat[1]);


        } while (track.moveToNext());
        track.close();


        LineString lineString = LineString.fromLngLats(pointsList);
        org.maplibre.geojson.Feature lineFeature = org.maplibre.geojson.Feature.fromGeometry(lineString);
        result.add(lineFeature);


        return result;
    }

    public void reloadTrackListToMap(){
        List<ILayer> tracks = new ArrayList<>();
        LayerGroup.getLayersByType(this, Constants.LAYERTYPE_TRACKS, tracks);
        if (tracks.size() > 0){

            Style style = maplibreMap.get().getStyle();
            if (style != null) {

                TrackLayer trackLayer = (TrackLayer) (tracks.get(0));
                List<org.maplibre.geojson.Feature> tracksFeatures = createFeatureListFromTrackLayer(trackLayer);
                List<org.maplibre.geojson.Feature> tracksFeaturesFlags = createFeatureListFlagsFromTrackLayer(trackLayer);


                GeoJsonSource tracksLineSource = (GeoJsonSource)style.getSource("track-line-source");
                if (tracksLineSource!=null)
                    tracksLineSource.setGeoJson(FeatureCollection.fromFeatures(tracksFeatures));

                GeoJsonSource tracksLineFlagsSource = (GeoJsonSource)style.getSource("track-flag-source");
                if (tracksLineFlagsSource!=null)
                    tracksLineFlagsSource.setGeoJson(FeatureCollection.fromFeatures(tracksFeaturesFlags));
            }
        }
    }


    // draw icon in color
    public Bitmap recolorBitmap(Bitmap src, int color) {
        Bitmap result = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, 0, 0, paint);

        return result;
    }


    public void addPressedPoint(LatLng point){
        Point newPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        org.maplibre.geojson.Feature feature = org.maplibre.geojson.Feature.fromGeometry(newPoint);
        feature.addStringProperty("color", colorLightBlue);

        selectedDotSource.setGeoJson(FeatureCollection.fromFeature(feature));
    }

    public void clearPressedPoint(){
        selectedDotSource.setGeoJson(FeatureCollection.fromFeatures(emptyList()));

    }

    // check if need sign for polygon
    // make point and add to source
    public void reAssembleSignPoly(@Nullable  final Style style,
                                   final List<org.maplibre.geojson.Feature> polyFeatures,
                                   String layerPath ){

        if (style == null)
            return;

        List<org.maplibre.geojson.Feature> points =  convertToPointFeatures(polyFeatures);
        if (points.size() == 0)
            return;
        GeoJsonSource vectorTextSource = (GeoJsonSource) style.getSource(layerPath + source_polygon_text);
        if (vectorTextSource == null) {
            vectorTextSource = new GeoJsonSource(layerPath + source_polygon_text, FeatureCollection.fromFeatures(points));
            style.addSource(vectorTextSource);
            sourceHashMap.put(layerPath + source_polygon_text, vectorTextSource);
        }
        else
            vectorTextSource.setGeoJson(FeatureCollection.fromFeatures(points));

    }

    public boolean checkMeasurment(int mode){
        if (editingObject != null && editingObject instanceof  MeasurmentLine ) {
            stoptMeasuring();
            return true;
        }
        return false;

    }

    public void stoptMeasuring(){
        hideMarker();
        hideVertex();
        //selectedEditedSource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        selectedPolySource.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
        editingObject = null;
        editingFeature = null;

    }

    public void startMeasuring(){
        if (editingObject != null)
            editingObject = null;

        if (originalSelectedFeature != null)
            originalSelectedFeature = null;


        org.maplibre.geojson.Feature feature = null;

        LatLng center = null;
        if (originalSelectedFeature != null && originalSelectedFeature.getGeometry() != null
                && originalSelectedFeature.getGeometry() instanceof  GeoPoint){
            center = latLngPointFromGeoPoint((GeoPoint) originalSelectedFeature.getGeometry());
        } else {
            center = maplibreMap.get().getCameraPosition().target;
        }

        Projection projection = maplibreMap.get().getProjection();
        Point point = Point.fromLngLat(center.getLongitude(), center.getLatitude());



        Point point1Geo = Point.fromLngLat(center.getLongitude(),center.getLatitude());
        List<org.maplibre.geojson.Point> lineList = new ArrayList<>(); //  getNewLinePoints(center, projection);
        //lineList.remove(1);
        lineList.add(point1Geo);

        LineString line = LineString.fromLngLats(lineList);
        feature = org.maplibre.geojson.Feature.fromGeometry(line);
        editingFeature = feature;

        GeoJsonSource choosed = selectedPolySource;

        selectedPolySource.setGeoJson(FeatureCollection.fromFeature(editingFeature));


        // choose layer
        editingObject = MPLFeaturesUtils.createEditObject(GT_MEASURMENT,
                selectedEditedSource,
                editingFeature,
                polygonFeatures,
                choosed,
                vertexSource,
                markerSource,
                "");

        Layer layer = maplibreMap.get().getStyle().getLayer("selected-polygon-fill");

        if (layer != null)
            maplibreMap.get().getStyle().removeLayer(fillPolyEditLayer);


        editingObject.setSelectedVertexIndex(0); // firsr point always selected
        editingObject.extractVertices(editingFeature,  true);

        LatLng selectedPoint = editingObject.getSelectedPoint();
        setMarker(selectedPoint);
        editingObject.updateEditingPolygonAndVertex();
    }

    public void updateMeasurmentCaptions(MLGeometryEditClass editingObject) {
        if (mapFragment.get() != null) {
            GeoGeometry geometry = mapFragment.get().getGeometryFromMaplibreGeometry(editingObject.editingFeature);

            if (geometry != null && geometry instanceof GeoLineString) {
                double length = ((GeoLineString) (geometry)).getLength();
                mapFragment.get().onLengthChanged(length);
            }

            Polygon polygon = Polygon.fromLngLats(((MeasurmentLine)editingObject).getPoints());
            org.maplibre.geojson.Feature featurePoly =  org.maplibre.geojson.Feature.fromGeometry(polygon);
            GeoGeometry geometryPoly = mapFragment.get().getGeometryFromMaplibreGeometry(featurePoly);

            if (geometryPoly instanceof GeoPolygon){
                double area = ((GeoPolygon) (geometryPoly)).getArea();
                mapFragment.get().onAreaChanged(area);
            }
        }
    }

    public void updateMapBackground(){
        if (maplibreMap.get()!= null){
            SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            int  colorRes = 0; // black
            String KEY_PREF_MAP_BG = "map_bg"; // copy of
            String namepart = "neutral_";
            switch (mSharedPreferences.getString(KEY_PREF_MAP_BG, KEY_PREF_NEUTRAL)) {
                    case KEY_PREF_LIGHT:
                        colorRes = R.drawable.bk_tile_light;
                        namepart = "light_";
                        break;
                    case KEY_PREF_DARK:
                        colorRes = R.drawable.bk_tile_dark;
                        namepart = "dark_";
                        break;
                    default:
                        colorRes = R.drawable.bk_tile;
                        namepart = "neutral_";
                        break;
                }

            Bitmap bitmap = BitmapFactory.decodeResource(getContext().getResources(), colorRes);
            maplibreMap.get().getStyle().addImage("bg-pattern" + namepart, bitmap);

            BackgroundLayer bgLayer = (BackgroundLayer) maplibreMap.get().getStyle().getLayer("background");
            if (bgLayer == null) {
                bgLayer = new BackgroundLayer("background");
                maplibreMap.get().getStyle().addLayerAt(bgLayer, 0);
            }

            bgLayer.setProperties(PropertyFactory.backgroundPattern("bg-pattern" + namepart));
        }
    }


//    public void updateMapBackground(){
//        if (maplibreMap.get()!= null){
//            SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
//            String color = "#000000"; // black
//            String KEY_PREF_MAP_BG               = "map_bg";
//            switch (mSharedPreferences.getString(KEY_PREF_MAP_BG, KEY_PREF_NEUTRAL)) {
//                case KEY_PREF_LIGHT:
//                    color = "#FFFFFF";//backgroundResId = com.nextgis.maplibui.R.drawable.bk_tile_light;
//                    break;
//                case KEY_PREF_DARK:
//                    color = "#000000";//backgroundResId = com.nextgis.maplibui.R.drawable.bk_tile_dark;
//                    break;
//                default:
//                    color = "#888888";
//                    //backgroundResId = com.nextgis.maplibui.R.drawable.bk_tile;
//                    break;
//            }
//            maplibreMap.get().getStyle().getLayer("background").setProperties(
//                    PropertyFactory.backgroundColor(color)
//            );
//        }
//    }




//    public void updatelayerOrder(ILayer from, ILayer to){
//
//
//        List<String> fromLayers =  getLayerMLibreNames(from.getId(), from.getType());
//        List<String> toLayers =  getLayerMLibreNames(to.getId(), to.getType());
//
//        Log.e("MPLREM",  "from: " + fromLayers.get(0) + " to " + toLayers.get(0));
//
//
//    }



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
}
