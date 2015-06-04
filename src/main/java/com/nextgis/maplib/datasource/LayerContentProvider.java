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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.map.VectorLayer;

import java.io.FileNotFoundException;


public class LayerContentProvider
        extends ContentProvider
{
    protected MapContentProviderHelper mMap;


    @Override
    public boolean onCreate()
    {

        if (getContext() instanceof IGISApplication) {
            IGISApplication app = (IGISApplication) getContext();
            mMap = (MapContentProviderHelper) app.getMap();
            return null != mMap;
        }
        return false;
    }


    protected Layer getLayerByUri(Uri uri)
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
        Layer layer = getLayerByUri(uri);
        if (null == layer) {
            return null;
        }

        if (layer instanceof VectorLayer) {
            return ((VectorLayer) layer).query(
                    uri, projection, selection, selectionArgs, sortOrder);
        }

        if (layer instanceof TrackLayer) {
            return ((TrackLayer) layer).query(uri, projection, selection, selectionArgs, sortOrder);
        }

        return null;
    }


    @Override
    public String getType(Uri uri)
    {
        Layer layer = getLayerByUri(uri);
        if (null == layer) {
            return null;
        }

        if (layer instanceof VectorLayer) {
            return ((VectorLayer) layer).getType(uri);
        }

        if (layer instanceof TrackLayer) {
            return ((TrackLayer) layer).getType(uri);
        }

        return null;
    }


    @Override
    public String[] getStreamTypes(
            Uri uri,
            String mimeTypeFilter)
    {
        Layer layer = getLayerByUri(uri);
        if (null == layer) {
            return null;
        }
        return ((VectorLayer) layer).getStreamTypes(uri, mimeTypeFilter);
    }


    @Override
    public ParcelFileDescriptor openFile(
            Uri uri,
            String mode)
            throws FileNotFoundException
    {
        Layer layer = getLayerByUri(uri);
        if (null == layer) {
            return null;
        }
        return ((VectorLayer) layer).openFile(uri, mode);
    }


    @Override
    public Uri insert(
            Uri uri,
            ContentValues contentValues)
    {
        Layer layer = getLayerByUri(uri);
        if (null == layer) {
            return null;
        }

        if (layer instanceof VectorLayer) {
            return ((VectorLayer) layer).insert(uri, contentValues);
        }

        if (layer instanceof TrackLayer) {
            return ((TrackLayer) layer).insert(uri, contentValues);
        }

        return null;
    }


    @Override
    public int delete(
            Uri uri,
            String s,
            String[] strings)
    {
        Layer layer = getLayerByUri(uri);
        if (null == layer) {
            return 0;
        }

        if (layer instanceof VectorLayer) {
            return ((VectorLayer) layer).delete(uri, s, strings);
        }

        if (layer instanceof TrackLayer) {
            return ((TrackLayer) layer).delete(uri, s, strings);
        }

        return 0;
    }


    @Override
    public int update(
            Uri uri,
            ContentValues contentValues,
            String s,
            String[] strings)
    {
        Layer layer = getLayerByUri(uri);
        if (null == layer) {
            return 0;
        }

        if (layer instanceof VectorLayer) {
            return ((VectorLayer) layer).update(uri, contentValues, s, strings);
        }

        if (layer instanceof TrackLayer) {
            return ((TrackLayer) layer).update(uri, contentValues, s, strings);
        }

        return 0;
    }
}
