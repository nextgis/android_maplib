/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

import android.annotation.TargetApi;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
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


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase)
    {

    }


    @Override
    public void onUpgrade(
            SQLiteDatabase sqLiteDatabase,
            int i,
            int i2)
    {

    }
}
