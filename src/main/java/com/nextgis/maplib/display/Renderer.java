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

package com.nextgis.maplib.display;

import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.IRenderer;

import java.lang.ref.WeakReference;


public abstract class Renderer
        implements IJSONStore,
                   IRenderer
{
    protected static int mCPUTotalCount;

    // for avoid circular references and memory leak
    protected final WeakReference<ILayer> mLayerRef;


    public Renderer(ILayer layer)
    {
        mLayerRef = new WeakReference<>(layer);

        mCPUTotalCount = Runtime.getRuntime().availableProcessors() * 8;
        if (mCPUTotalCount < 1) {
            mCPUTotalCount = 1;
        }
    }


    ILayer getLayer()
    {
        ILayer layer = mLayerRef.get();
        if (null == layer) {
            throw new IllegalStateException("getLayer() == null, illegal state of struct");
        }
        return layer;
    }
}
