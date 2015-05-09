/*******************************************************************************
 * Project:  NextGIS mobile apps for Compulink
 * Purpose:  Mobile GIS for Android
 * Authors:  Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 *           NikitaFeodonit, nfeodonit@yandex.com
 * *****************************************************************************
 * Copyright (C) 2014-2015 NextGIS
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
 ******************************************************************************/

package com.nextgis.maplib.datasource;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


// http://stackoverflow.com/questions/2493331/what-are-the-best-practices-for-sqlite-on-android
public class DatabaseManager
{
    protected int mOpenCounter = 0;

    protected static DatabaseManager  mInstance;
    protected static SQLiteOpenHelper mDatabaseHelper;
    protected        SQLiteDatabase   mDatabase;


    public static synchronized void initializeInstance(SQLiteOpenHelper helper)
    {
        if (mInstance == null) {
            mInstance = new DatabaseManager();
            mDatabaseHelper = helper;
        }
    }


    public static synchronized DatabaseManager getInstance()
    {
        if (mInstance == null) {
            throw new IllegalStateException(
                    DatabaseManager.class.getSimpleName() +
                            " is not initialized, call initializeInstance(..) method first.");
        }

        return mInstance;
    }


    public synchronized SQLiteDatabase openDatabase()
    {
        mOpenCounter++;
        if (mOpenCounter == 1) {
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
        return mDatabase;
    }


    // database.close(); Don't close it directly!
    // DatabaseManager.getInstance().closeDatabase(); // correct way
    public synchronized void closeDatabase()
    {
        mOpenCounter--;
        if (mOpenCounter == 0) {
            // Closing database
            mDatabase.close();

        }
    }
}
