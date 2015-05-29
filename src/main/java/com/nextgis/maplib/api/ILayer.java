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

package com.nextgis.maplib.api;

import android.content.Context;
import com.nextgis.maplib.datasource.GeoEnvelope;

import java.io.File;


public interface ILayer
{
    public Context getContext();

    public String getName();

    public void setName(String newName);

    public short getId();

    public int getType();

    public boolean delete();

    public File getPath();

    public boolean save();

    public boolean load();

    public GeoEnvelope getExtents();

    public void setParent(ILayer layer);

    public void setId(short id);

    public boolean isValid();

    public IRenderer getRenderer();
}
