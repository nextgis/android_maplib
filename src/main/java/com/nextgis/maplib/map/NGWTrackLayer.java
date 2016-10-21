/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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
import android.content.SyncResult;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.util.Constants;

import java.io.File;

public class NGWTrackLayer extends NGWVectorLayer {
    public NGWTrackLayer(Context context, File path) {
        super(context, path);
        mSyncType = Constants.SYNC_ALL;
        mLayerType = Constants.LAYERTYPE_NGW_TRACKS;
    }

    @Override
    public boolean getChangesFromServer(String authority, SyncResult syncResult) {
        return true;
    }

    @Override
    public long createFeature(Feature feature) throws SQLiteException {
        return Constants.NOT_FOUND;
    }

    @Override
    public void createFeatureBatch(Feature feature, SQLiteDatabase db) throws SQLiteException {

    }
}
