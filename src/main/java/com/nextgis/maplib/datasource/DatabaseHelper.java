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

package com.nextgis.maplib.datasource;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.util.DatabaseContext;

import java.io.File;


public class DatabaseHelper
        extends SQLiteOpenHelper
{

    public DatabaseHelper(
            Context context,
            File dbFullName,
            SQLiteDatabase.CursorFactory factory,
            int version)
    {
        super(
                new DatabaseContext(context, dbFullName.getParentFile()), dbFullName.getName(),
                factory, version);
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public DatabaseHelper(
            Context context,
            File dbFullName,
            SQLiteDatabase.CursorFactory factory,
            int version,
            DatabaseErrorHandler errorHandler)
    {
        super(
                new DatabaseContext(context, dbFullName.getParentFile()), dbFullName.getName(),
                factory, version, errorHandler);
    }


    /**
     * is called whenever the app is freshly installed
     * @param sqLiteDatabase Database
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase)
    {

    }


    /**
     * is called whenever the app is upgraded and launched and the database version is not the same
     * @param sqLiteDatabase Database
     * @param oldVersion The previous database version
     * @param newVersion The current database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion)
    {
        MapBase map = MapBase.getInstance();
        map.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
    }
}
