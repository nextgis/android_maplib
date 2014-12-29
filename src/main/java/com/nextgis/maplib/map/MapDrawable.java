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

import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.display.GISDisplay;

import java.io.File;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.*;

public class MapDrawable extends MapEventSource {
    protected GISDisplay mDisplay;

    public MapDrawable(Bitmap backgroundTile, File mapPath, LayerFactory layerFactory) {
        super(mapPath, layerFactory);

        //initialise display
        mDisplay = new GISDisplay(backgroundTile);
    }

    public Bitmap getMap(){
        if(mDisplay != null){
            return mDisplay.getDisplay(true);
        }
        return null;
    }

    public void setSize(int w, int h) {
        if(mDisplay != null){
            mDisplay.setSize(w, h);
            onExtentChanged((int) mDisplay.getZoomLevel(), mDisplay.getCenter());
        }
    }


    protected void runDraw(){
        cancelDraw();
        mDisplay.clearBackground();
        mDisplay.clearLayer();

        drawNext(NOT_FOUND);
    }

    protected void cancelDraw(){
        for(Layer layer : mLayers) {
            if(layer.isVisible()) {
                layer.cancelDraw();
            }
        }
    }

    protected void drawNext(int layerId){
        if(mLayers.isEmpty())
            return;
        if(layerId == NOT_FOUND){
            mLayers.get(0).runDraw();
        } else {
            boolean bDrawNext = false;
            for(Layer layer : mLayers) {
                if(bDrawNext){
                    layer.runDraw();
                    break;
                }

                if(layer.getId() == layerId) {
                    bDrawNext = true;
                }
            }
        }
    }

    public final double getZoomLevel() {
        if(mDisplay != null)
            return mDisplay.getZoomLevel();
        return 0;
    }

    public final double getMaxZoomLevel() {
        if(mDisplay != null)
            return mDisplay.getMaxZoomLevel();
        return ZOOMLEVEL_MAX;
    }

    public final double getMinZoomLevel() {
        if(mDisplay != null)
            return mDisplay.getMinZoomLevel();
        return 0;
    }

    /**
     * Set new map extent according zoom level and center
     * @param zoom A zoom level
     * @param center A map center coordinates
     */
    public void setZoomAndCenter(final double zoom, final GeoPoint center){
        if(mDisplay != null){
            double newZoom = zoom;
            if( zoom < mDisplay.getMinZoomLevel())
                newZoom = mDisplay.getMinZoomLevel();
            else if( zoom > mDisplay.getMaxZoomLevel())
                newZoom = mDisplay.getMaxZoomLevel();
            mDisplay.setZoomAndCenter(newZoom, center);
            onExtentChanged((int) newZoom, center);
        }
    }

    public final GeoPoint getMapCenter(){
        if(mDisplay != null){
            return mDisplay.getCenter();
        }
        return new GeoPoint();
    }
}
