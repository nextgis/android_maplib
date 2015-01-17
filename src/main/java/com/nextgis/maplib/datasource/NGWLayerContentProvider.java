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
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.NGWVectorLayer;


import static com.nextgis.maplib.util.Constants.*;

public class NGWLayerContentProvider extends ContentProvider
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

    protected NGWVectorLayer getLayerByUri(Uri uri)
    {
        String path = uri.getPath();
        int nPos = path.indexOf('/');
        String layerPath;
        if(nPos != NOT_FOUND){
            layerPath = path.substring(0, nPos);
        }
        else {
            layerPath = path;
        }
        return MapContentProviderHelper.getLayerByPath(mMap, layerPath);
    }


    @Override
    public Cursor query(
            Uri uri,
            String[] strings,
            String s,
            String[] strings2,
            String s2)
    {
        NGWVectorLayer layer = getLayerByUri(uri);
        if(null == layer)
            return null;
        return layer.query(uri, strings, s, strings2, s2);
    }


    @Override
    public String getType(Uri uri)
    {
        NGWVectorLayer layer = getLayerByUri(uri);
        if(null == layer)
            return null;
        return layer.getType(uri);
    }


    @Override
    public String[] getStreamTypes(
            Uri uri,
            String mimeTypeFilter)
    {
        NGWVectorLayer layer = getLayerByUri(uri);
        if(null == layer)
            return null;
        return layer.getStreamTypes(uri, mimeTypeFilter);
    }


    @Override
    public Uri insert(
            Uri uri,
            ContentValues contentValues)
    {
        NGWVectorLayer layer = getLayerByUri(uri);
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
        NGWVectorLayer layer = getLayerByUri(uri);
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
        NGWVectorLayer layer = getLayerByUri(uri);
        if(null == layer)
            return 0;
        return layer.update(uri, contentValues, s, strings);
    }
}
