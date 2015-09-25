/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.DatabaseHelper;
import com.nextgis.maplib.util.Constants;

import java.io.File;
import java.util.List;

import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.FIELD_OLD_ID;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP_PATH;


public class MapContentProviderHelper
        extends MapBase
{
    protected DatabaseHelper mDatabaseHelper;

    protected static final String DBNAME           = "layers";
    protected static final int    DATABASE_VERSION = 2;


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

        mDatabaseHelper = new DatabaseHelper(
                context,          // the application context
                dbFullName,       // the name of the database
                null,             // uses the default SQLite cursor
                DATABASE_VERSION  // the version number
        );

        // register events from layers modify in services or other applications
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.NOTIFY_DELETE);
        intentFilter.addAction(Constants.NOTIFY_DELETE_ALL);
        intentFilter.addAction(Constants.NOTIFY_INSERT);
        intentFilter.addAction(Constants.NOTIFY_UPDATE);
        intentFilter.addAction(Constants.NOTIFY_UPDATE_ALL);
        intentFilter.addAction(Constants.NOTIFY_UPDATE_FIELDS);
        intentFilter.addAction(Constants.NOTIFY_FEATURE_ID_CHANGE);

        context.registerReceiver(new VectorLayerNotifyReceiver(), intentFilter);
    }


    public SQLiteDatabase getDatabase(boolean readOnly)
    {
        if (readOnly) {
            return mDatabaseHelper.getReadableDatabase();
        } else {
            return mDatabaseHelper.getWritableDatabase();
        }
    }


    public static Layer getVectorLayerByPath(
            LayerGroup layerGroup,
            String path)
    {
        for (int i = 0; i < layerGroup.getLayerCount(); i++) {
            ILayer layer = layerGroup.getLayer(i);
            if (layer instanceof LayerGroup) {
                LayerGroup inLayerGroup = (LayerGroup) layer;
                Layer out = getVectorLayerByPath(inLayerGroup, path);
                if (null != out) {
                    return out;
                }
            } else if (layer instanceof VectorLayer) {
                VectorLayer vectorLayer = (VectorLayer) layer;
                if (path.contains(vectorLayer.getPath().getName())) {
                    return vectorLayer;
                }
            } else if (layer instanceof TrackLayer) {
                TrackLayer trackLayer = (TrackLayer) layer;
                if (path.contains(TrackLayer.TABLE_TRACKS) ||
                    path.contains(TrackLayer.TABLE_TRACKPOINTS)) {
                    return trackLayer;
                }
            }
        }
        return null;
    }


    public static void getLayersByAccount(
            LayerGroup layerGroup,
            String account,
            List<INGWLayer> layerList)
    {
        for (int i = 0; i < layerGroup.getLayerCount(); i++) {
            ILayer layer = layerGroup.getLayer(i);

            if (layer instanceof INGWLayer) {
                INGWLayer ngwLayer = (INGWLayer) layer;
                if (ngwLayer.getAccountName().equals(account)) {
                    layerList.add(ngwLayer);
                }
            }

            if (layer instanceof LayerGroup) {
                getLayersByAccount((LayerGroup) layer, account, layerList);
            }
        }
    }


    public class VectorLayerNotifyReceiver
            extends BroadcastReceiver
    {

        @Override
        public void onReceive(
                Context context,
                Intent intent)
        {
            // extreme logging commented
            //Log.d(TAG, "Receive notify: " + intent.getAction());

            if(!intent.hasExtra(Constants.NOTIFY_LAYER_NAME))
                return;

            ILayer layer = getVectorLayerByPath(MapContentProviderHelper.this,
                    intent.getStringExtra(Constants.NOTIFY_LAYER_NAME));
            if(null == layer)
                return;

            switch (intent.getAction()) {

                case Constants.NOTIFY_DELETE:
                    layer.notifyDelete(intent.getLongExtra(FIELD_ID, NOT_FOUND));
                    break;

                case Constants.NOTIFY_DELETE_ALL:
                    layer.notifyDeleteAll();
                    break;

                case Constants.NOTIFY_UPDATE:
                    layer.notifyUpdate(
                                intent.getLongExtra(FIELD_ID, NOT_FOUND),
                                intent.getLongExtra(FIELD_OLD_ID, NOT_FOUND),
                                intent.getBooleanExtra(Constants.ATTRIBUTES_ONLY, true));
                    break;

                case Constants.NOTIFY_UPDATE_ALL:
                    layer.notifyUpdateAll();
                    break;

                case Constants.NOTIFY_INSERT:
                    layer.notifyInsert(intent.getLongExtra(FIELD_ID, NOT_FOUND));
                    break;
            }
        }
    }
}
