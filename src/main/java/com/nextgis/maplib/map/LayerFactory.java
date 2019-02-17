/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2014-2016, 2019 NextGIS, info@nextgis.com
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
import android.net.Uri;
import android.util.Log;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.util.FileUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import static com.nextgis.maplib.util.Constants.CONFIG;
import static com.nextgis.maplib.util.Constants.JSON_TYPE_KEY;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_GROUP;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOCAL_TMS;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOCAL_VECTOR;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOOKUPTABLE;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_RASTER;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_VECTOR;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_WEBMAP;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_REMOTE_TMS;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_TRACKS;
import static com.nextgis.maplib.util.Constants.TAG;


public abstract class LayerFactory
{
    public ILayer createLayer(
            Context context,
            File path)
    {
        File config_file = new File(path, CONFIG);
        ILayer layer = null;

        try {
            String sData = FileUtil.readFromFile(config_file);
            JSONObject rootObject = new JSONObject(sData);
            int nType = rootObject.getInt(JSON_TYPE_KEY);

            switch (nType) {
                case LAYERTYPE_REMOTE_TMS:
                    layer = new RemoteTMSLayer(context, path);
                    break;
                case LAYERTYPE_NGW_RASTER:
                    layer = new NGWRasterLayer(context, path);
                    break;
                case LAYERTYPE_NGW_VECTOR:
                    layer = new NGWVectorLayer(context, path);
                    break;
                case LAYERTYPE_NGW_WEBMAP:
                    layer = new NGWWebMapLayer(context, path);
                    break;
                case LAYERTYPE_LOCAL_VECTOR:
                    layer = new VectorLayer(context, path);
                    break;
                case LAYERTYPE_LOCAL_TMS:
                    layer = new LocalTMSLayer(context, path);
                    break;
                case LAYERTYPE_GROUP:
                    layer = new LayerGroup(context, path, this);
                    break;
                case LAYERTYPE_TRACKS:
                    layer = new TrackLayer(context, path);
                    break;
                case LAYERTYPE_LOOKUPTABLE:
                    layer = new NGWLookupTable(context, path);
                    break;
            }
        } catch (IOException | JSONException e) {
            Log.d(TAG, e.getLocalizedMessage());
        }

        return layer;
    }

    public String getLayerTypeString(
            Context context,
            int type)
    {
        switch (type) {
            case LAYERTYPE_GROUP:
                return "layer group";
            case LAYERTYPE_NGW_RASTER:
                return "NGW raster layer";
            case LAYERTYPE_NGW_VECTOR:
                return "NGW vector layer";
            case LAYERTYPE_NGW_WEBMAP:
                return "NGW web map";
            case LAYERTYPE_REMOTE_TMS:
                return "remote tms layer";
            case LAYERTYPE_LOCAL_VECTOR:
                return "vector layer";
            case LAYERTYPE_LOCAL_TMS:
                return "local tms layer";
            case LAYERTYPE_LOOKUPTABLE:
                return "lookup table";
            default:
                return "n/a";
        }
    }

    public abstract void createNewRemoteTMSLayer(
            final Context context,
            final LayerGroup groupLayer);

    public abstract void createNewNGWLayer(
            final Context context,
            final LayerGroup groupLayer);


    public abstract void createNewLocalTMSLayer(
            final Context context,
            final LayerGroup groupLayer,
            final Uri uri);

    public abstract void createNewVectorLayer(
            final Context context,
            final LayerGroup groupLayer,
            final Uri uri);

    public abstract void createNewVectorLayerWithForm(
            final Context context,
            final LayerGroup groupLayer,
            final Uri uri);
}
