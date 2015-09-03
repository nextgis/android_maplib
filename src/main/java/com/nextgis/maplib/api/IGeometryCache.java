/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2015. NextGIS, info@nextgis.com
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

import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;

import java.io.File;
import java.util.List;

/**
 * A geometry cache for fast drawing and searching
 */
public interface IGeometryCache {

    /**
     * Check if item with specified id is exist in cache
     * @param featureId Feature id
     * @return true if item is exist or false
     */
    boolean isItemExist(long featureId);

    /**
     * Add item to cache
     * @param id Feature identificator
     * @param envelope Envelope
     */
    IGeometryCacheItem addItem(long id, GeoEnvelope envelope);

    /**
     * Return cache item by feature identificator
     * @param featureId Feature identificator
     * @return Cache item
     */
    IGeometryCacheItem getItem(long featureId);

    /**
     * Remove item from cache
     * @param featureId Feature id of cache item
     * @return removed item or null
     */
    IGeometryCacheItem removeItem(long featureId);

    /**
     * Return count of items
     * @return count of items
     */
    int size();

    /**
     * Remove all items from cache
     */
    void clear();

    /**
     * Search items intersected provided envelope
     * @param extent Envelope to search
     * @return List of items intersected provided envelope
     */
    List<IGeometryCacheItem> search(GeoEnvelope extent);

    /**
     * Get all items
     * @return List of all items
     */
    List<IGeometryCacheItem> getAll();

    void changeId(long oldFeatureId, long newFeatureId);

    void save(File path);

    void load(File path);
}
