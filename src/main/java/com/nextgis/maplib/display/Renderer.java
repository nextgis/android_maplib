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
package com.nextgis.maplib.display;

import com.nextgis.maplib.datasource.JSONStore;
import com.nextgis.maplib.map.Layer;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class Renderer implements JSONStore{

    protected final Layer mLayer;
    protected static int mCPUTotalCount;

    public Renderer(final Layer layer){
        mLayer = layer;

        mCPUTotalCount = Runtime.getRuntime().availableProcessors() * 8;
        if(mCPUTotalCount < 1)
            mCPUTotalCount = 1;
    }

    public abstract void runDraw();
    public abstract void cancelDraw();
}
