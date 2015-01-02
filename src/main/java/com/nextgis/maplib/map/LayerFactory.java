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

import android.content.Context;
import android.util.Log;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.util.FileUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.nextgis.maplib.util.Constants.*;


public abstract class LayerFactory
{

    protected File mMapPath;


    public LayerFactory(File mapPath)
    {
        mMapPath = mapPath;
    }


    public ILayer createLayer(
            Context context,
            File path)
    {
        File config_file = new File(path, LAYER_CONFIG);
        ILayer layer = null;

        try {
            String sData = FileUtil.readFromFile(config_file);
            JSONObject rootObject = new JSONObject(sData);
            int nType = rootObject.getInt(JSON_TYPE_KEY);

            switch (nType) {
                case LAYERTYPE_LOCAL_TMS:
                    //layer = new LocalTMSLayer(this, path, rootObject);
                    break;
                case LAYERTYPE_LOCAL_GEOJSON:
                    //layer = new LocalGeoJsonLayer(this, path, rootObject);
                    break;
                case LAYERTYPE_LOCAL_RASTER:
                    break;
                case LAYERTYPE_REMOTE_TMS:
                    layer = new RemoteTMSLayer(context, path);
                    break;
                case LAYERTYPE_NDW_VECTOR:
                    //layer = new NgwVectorLayer(this, path, rootObject);
                    break;
                case LAYERTYPE_NDW_RASTER:
                    //layer = new NgwRasterLayer(this, path, rootObject);
                    break;
                case LAYERTYPE_LOCAL_NGFP:
                    //layer = new LocalNgfpLayer(this, path, rootObject);
                    break;
                case LAYERTYPE_NGW:
                    break;
            }
        } catch (IOException | JSONException e) {
            Log.d(TAG, e.getLocalizedMessage());
        }

        return layer;
    }


    protected File cretateLayerStorage()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String layerDir = LAYER_PREFIX + sdf.format(new Date());
        return new File(mMapPath, layerDir);
    }


    public abstract void createNewRemoteTMSLayer(
            final LayerGroup groupLayer);
}
