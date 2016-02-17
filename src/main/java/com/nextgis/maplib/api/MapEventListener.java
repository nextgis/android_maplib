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

package com.nextgis.maplib.api;


import com.nextgis.maplib.datasource.GeoPoint;

/**
 * The object which need map events, should implement this interface.
 * @author Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 */
public interface MapEventListener
{
    /**
     * Executed then new layer added
     * @param id The new layer identificator set in this session. May be another in next execution.
     */
    void onLayerAdded(int id);

    /**
     * Executed then layer deleted
     * @param id The deleted layer identificator.
     */
    void onLayerDeleted(int id);

    /**
     * Executed then layer changed
     * @param id The changed layer identificator.
     */
    void onLayerChanged(int id);

    /**
     * Executed then map extent changed (i.e. user zoom in or out)
     * @param zoom New map zoom
     * @param center New map center
     */
    void onExtentChanged(
            float zoom,
            GeoPoint center);

    /**
     * Executed then layer are reordered
     */
    void onLayersReordered();

    /**
     * Executed then some drawing routine finished or indicate drawing process
     * @param id The layer identificator
     * @param percent The percent of layer drawn
     */
    void onLayerDrawFinished(
            int id,
            float percent);

    /**
     * Executed then layer draw started
     */
    void onLayerDrawStarted();
}
