package com.nextgis.maplib.map;

import android.view.MotionEvent;

import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.GeoGeometry;

public interface MaplibreMapInteraction {
    public boolean processMapLongClick(double x, double y); // x y  - mercator
    public boolean processMapClick(double x, double y);
    public void setHasEdit();

    public void updateGeometryFromMaplibre(org.maplibre.geojson.Feature feature, Feature originalSelectedFeature );
}
