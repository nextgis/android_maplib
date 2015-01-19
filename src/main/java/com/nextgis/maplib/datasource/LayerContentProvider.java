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
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;


import java.io.FileNotFoundException;

import static com.nextgis.maplib.util.Constants.*;

public class LayerContentProvider
        extends ContentProvider
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

    protected VectorLayer getLayerByUri(Uri uri)
    {
        String path = uri.getPath();
        return MapContentProviderHelper.getVectorLayerByPath(mMap, path);
    }


    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder)
    {
        VectorLayer layer = getLayerByUri(uri);
        if(null == layer)
            return null;
        return layer.query(uri, projection, selection, selectionArgs, sortOrder);
    }


    @Override
    public String getType(Uri uri)
    {
        VectorLayer layer = getLayerByUri(uri);
        if(null == layer)
            return null;
        return layer.getType(uri);
    }


    @Override
    public String[] getStreamTypes(
            Uri uri,
            String mimeTypeFilter)
    {
        VectorLayer layer = getLayerByUri(uri);
        if(null == layer)
            return null;
        return layer.getStreamTypes(uri, mimeTypeFilter);
    }


    @Override
    public ParcelFileDescriptor openFile(
            Uri uri,
            String mode)
            throws FileNotFoundException
    {
        VectorLayer layer = getLayerByUri(uri);
        if(null == layer)
            return null;
        return layer.openFile(uri, mode);
    }


    @Override
    public Uri insert(
            Uri uri,
            ContentValues contentValues)
    {
        VectorLayer layer = getLayerByUri(uri);
        if(null == layer)
            return null;
        return layer.insert(uri, contentValues);
    }


    @Override
    public int delete(
            Uri uri,
            String s,
            String[] strings)
    {
        VectorLayer layer = getLayerByUri(uri);
        if(null == layer)
            return 0;
        return layer.delete(uri, s, strings);
    }


    @Override
    public int update(
            Uri uri,
            ContentValues contentValues,
            String s,
            String[] strings)
    {
        VectorLayer layer = getLayerByUri(uri);
        if(null == layer)
            return 0;
        return layer.update(uri, contentValues, s, strings);
    }
}
