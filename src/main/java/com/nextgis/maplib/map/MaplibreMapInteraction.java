package com.nextgis.maplib.map;

import android.graphics.PointF;

import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.map.MLP.MLGeometryEditClass;

public interface MaplibreMapInteraction {

    public boolean processMapLongClick(GeoEnvelope exactEnv,  PointF clickPoint); // x y  - mercator

    public boolean processMapClick(float x, float y);

    public void setHasEdit();

    public void updateGeometryFromMaplibre(org.maplibre.geojson.Feature feature, Feature originalSelectedFeaturem, MLGeometryEditClass editObject );

    public VectorLayer getSelectedLayer();

    public void updateActions(MLGeometryEditClass editObject);

    public Integer getMode();


    public  void loadLayersLite();

    public  boolean getLongLongClickProcesses();

    public  void setLongLongClickProcesses(boolean longLongCLickPrecesses);

    public GeoGeometry getGeometryFromMaplibreGeometry(org.maplibre.geojson.Feature feature);

    public void onLengthChanged(Double length);

    public void onAreaChanged(Double length);

    public void changeProgress(boolean show);


}
