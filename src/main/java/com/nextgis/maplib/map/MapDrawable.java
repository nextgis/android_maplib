/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplib.map;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.ILayerView;
import com.nextgis.maplib.api.IMapView;
import com.nextgis.maplib.api.IRenderer;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.display.GISDisplay;

import java.io.File;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.*;

public class MapDrawable extends MapEventSource implements IMapView{
    protected GISDisplay mDisplay;

    public MapDrawable(Bitmap backgroundTile, File mapPath, LayerFactory layerFactory) {
        super(mapPath, layerFactory);

        //initialise display
        mDisplay = new GISDisplay(backgroundTile);
    }

    @Override
    public Bitmap getView(){
        if(mDisplay != null){
            return mDisplay.getDisplay(true);
        }
        return null;
    }

    @Override
    public void setViewSize(int w, int h) {
        if(mDisplay != null){
            mDisplay.setSize(w, h);
            onExtentChanged((int) mDisplay.getZoomLevel(), mDisplay.getCenter());
        }
    }

    @Override
    public void runDraw(){
        cancelDraw();
        mDisplay.clearBackground();
        mDisplay.clearLayer();

        drawNext();
    }

    @Override
    public float getZoomLevel() {
        if(mDisplay != null)
            return mDisplay.getZoomLevel();
        return 0;
    }

    @Override
    public float getMaxZoom() {
        if(mDisplay != null)
            return mDisplay.getMaxZoomLevel();
        return DEFAULT_MAX_ZOOM;
    }

    @Override
    public float getMinZoom() {
        if(mDisplay != null)
            return mDisplay.getMinZoomLevel();
        return 0;
    }

    /**
     * Set new map extent according zoom level and center
     * @param zoom A zoom level
     * @param center A map center coordinates
     */
    @Override
    public void setZoomAndCenter(float zoom, GeoPoint center){
        if(mDisplay != null){
            float newZoom = zoom;
            if( zoom < mDisplay.getMinZoomLevel())
                newZoom = mDisplay.getMinZoomLevel();
            else if( zoom > mDisplay.getMaxZoomLevel())
                newZoom = mDisplay.getMaxZoomLevel();
            mDisplay.setZoomAndCenter(newZoom, center);
            onExtentChanged((int) newZoom, center);
        }
    }

    @Override
    public GeoPoint getMapCenter(){
        if(mDisplay != null){
            return mDisplay.getCenter();
        }
        return new GeoPoint();
    }
}
