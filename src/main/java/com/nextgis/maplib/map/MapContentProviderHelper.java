/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.DatabaseHelper;

import java.io.File;

import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP_PATH;


public class MapContentProviderHelper extends MapBase
{
    protected DatabaseHelper mDatabaseHelper;

    protected static final String DBNAME           = "layers";
    protected static final int    DATABASE_VERSION = 1;

    public MapContentProviderHelper(
            Context context,
            File path,
            LayerFactory layerFactory)
    {
        super(context, path, layerFactory);

        File dbFullName;
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        File defaultPath = getContext().getExternalFilesDir(KEY_PREF_MAP);
        if (defaultPath != null) {
            String mapPath = sharedPreferences.getString(KEY_PREF_MAP_PATH, defaultPath.getPath());
            dbFullName = new File(mapPath, DBNAME);
        } else {
            dbFullName = context.getDatabasePath(DBNAME);
        }

        mDatabaseHelper = new DatabaseHelper(context,          // the application context
                                             dbFullName,       // the name of the database
                                             null,             // uses the default SQLite cursor
                                             DATABASE_VERSION  // the version number
        );
    }

    public SQLiteDatabase getDatabase(boolean readOnly)
    {
        if(readOnly)
            return mDatabaseHelper.getReadableDatabase();
        else
            return mDatabaseHelper.getWritableDatabase();
    }

    public static VectorLayer getLayerByPath(LayerGroup layerGroup, String path)
    {
        for(int i = 0; i < layerGroup.getLayerCount(); i++)
        {
            ILayer layer = layerGroup.getLayer(i);
            if(layer instanceof LayerGroup)
            {
                LayerGroup inLayerGroup = (LayerGroup)layer;
                VectorLayer out = getLayerByPath(inLayerGroup, path);
                if(null != out)
                    return out;
            }
            else if(layer instanceof VectorLayer)
            {
                VectorLayer vectorLayer = (VectorLayer)layer;
                if(vectorLayer.getPath().getName().equals(path))
                    return vectorLayer;
            }
        }
        return null;
    }
}
