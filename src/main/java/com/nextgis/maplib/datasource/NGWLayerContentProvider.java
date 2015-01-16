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

package com.nextgis.maplib.datasource;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;

import java.io.File;

import static com.nextgis.maplib.util.SettingsConstants.*;

public class NGWLayerContentProvider  extends ContentProvider
{
    protected MapContentProviderHelper mMap;

    @Override
    public boolean onCreate()
    {
        if(getContext() instanceof IGISApplication) {
            IGISApplication app = (IGISApplication)getContext();
            mMap = (MapContentProviderHelper) app.getMap();
            return null != mMap;
        }
        return false;
    }


    @Override
    public Cursor query(
            Uri uri,
            String[] strings,
            String s,
            String[] strings2,
            String s2)
    {
        return null;
    }


    @Override
    public String getType(Uri uri)
    {
        return null;
    }


    @Override
    public Uri insert(
            Uri uri,
            ContentValues contentValues)
    {
        //mDatabase = mDatabaseHelper.getWritableDatabase();

        return null;
    }


    @Override
    public int delete(
            Uri uri,
            String s,
            String[] strings)
    {
        return 0;
    }


    @Override
    public int update(
            Uri uri,
            ContentValues contentValues,
            String s,
            String[] strings)
    {
        return 0;
    }
}
