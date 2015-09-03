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

/**
 * A geometry cache item
 */
public interface IGeometryCacheItem {
    /**
     *
     * @return Return an envelope
     */
    GeoEnvelope getEnvelope();

    /**
     *
     * @return A feature identificator connected with this cache item
     */
    long getFeatureId();


    /**
     * Set a eature identificator connected with this cache item
     * @param id Feature identificator
     */
    void setFeatureId(long id);
}
