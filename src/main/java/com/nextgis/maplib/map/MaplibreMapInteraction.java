package com.nextgis.maplib.map;

import android.graphics.PointF;
import android.view.MotionEvent;

import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.map.MLP.MLGeometryEditClass;

public interface MaplibreMapInteraction {

    public boolean processMapLongClick(GeoEnvelope exactEnv,  PointF clickPoint); // x y  - mercator

    public boolean processMapClick(float x, float y);

    public void setHasEdit();

    public void updateGeometryFromMaplibre(org.maplibre.geojson.Feature feature, Feature originalSelectedFeaturem, MLGeometryEditClass editObject );

    public int getSelectedLayerId();

    public void updateActions(MLGeometryEditClass editObject);

}
