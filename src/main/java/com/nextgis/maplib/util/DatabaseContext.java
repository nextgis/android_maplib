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

package com.nextgis.maplib.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import java.io.File;


public class DatabaseContext
        extends ContextWrapper
{
    protected File mDatabasePath;

    public DatabaseContext(Context base, File databasePath)
    {
        super(base);
        mDatabasePath = databasePath;
    }


    @Override
    public File getDatabasePath(String name)
    {
        String dbfile = mDatabasePath + File.separator + name;
        if (!dbfile.endsWith(".db"))
        {
            dbfile += ".db" ;
        }

        if (!mDatabasePath.exists())
        {
            mDatabasePath.mkdirs();
        }

        return new File(dbfile);
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public SQLiteDatabase openOrCreateDatabase(
            String name,
            int mode,
            SQLiteDatabase.CursorFactory factory,
            DatabaseErrorHandler errorHandler)
    {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getAbsolutePath(), factory, errorHandler);
    }


    @Override
    public SQLiteDatabase openOrCreateDatabase(
            String name,
            int mode,
            SQLiteDatabase.CursorFactory factory)
    {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
    }
}
