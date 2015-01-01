/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplib.display;

import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.IRenderer;


public abstract class Renderer
        implements IJSONStore, IRenderer
{

    protected static int    mCPUTotalCount;
    protected final  ILayer mLayer;


    public Renderer(ILayer layer)
    {
        mLayer = layer;

        mCPUTotalCount = Runtime.getRuntime().availableProcessors() * 8;
        if (mCPUTotalCount < 1) {
            mCPUTotalCount = 1;
        }
    }
}
