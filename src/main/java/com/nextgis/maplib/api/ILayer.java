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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.nextgis.maplib.datasource.GeoEnvelope;

import java.io.File;

/**
 * Interface for layer. Each layer should implement it.
 * @author Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 */
public interface ILayer
{
    /**
     * @return Application context
     */
    Context getContext();

    /**
     * @return User readable layer name
     */
    String getName();

    /**
     * Set layer name
     * @param newName New name
     */
    void setName(String newName);

    /**
     * @return Layer identofoctor - set by map on current session
     */
    int getId();

    /**
     * Get Layer type (@see com.nextgis.maplib.util.Constants)
     * @return Layer type
     */
    int getType();

    /**
     * Delete layer
     * @return true on success or false
     */
    boolean delete();

    /**
     * Get layer path in storage
     * @return Layer path
     */
    File getPath();

    /**
     * Save layer changes
     * @return true on success or false
     */
    boolean save();

    /**
     * Load layer
     * @return true on  success or false
     */
    boolean load();

    /**
     * Get layer extents
     * @return Layer extents in map coordinates
     */
    GeoEnvelope getExtents();

    /**
     * set layer parent
     * @param layer Layer parent object
     */
    void setParent(ILayer layer);

    /**
     * @return Layer parent object
     */
    ILayer getParent();

    /**
     * Set layer internal identifictor - set by map on current session
     * @param id New layer identificator
     */
    void setId(int id);

    /**
     * @return Is layer valid (all data are present, .etc.)
     */
    boolean isValid();

    /**
     * Triggered on layer contents or properties changes
     */
    void notifyUpdateAll();

    /**
     * Triggered on layer contents changed
     * @param rowId New record id
     * @param oldRowId Old record id
     * @param attributesOnly
     */
    void notifyUpdate(long rowId, long oldRowId, boolean attributesOnly);

    /**
     * Triggered on layer added new record
     * @param rowId New record id
     */
    void notifyInsert(long rowId);

    /**
     * Triggered on layer delete all records
     */
    void notifyDeleteAll();

    /**
     * Triggered on layer delete record
     * @param rowId Deleted record id
     */
    void notifyDelete(long rowId);

    /**
     * Executed then database version is changes. Triggered on application upgrade
     * @param sqLiteDatabase The database
     * @param oldVersion Old database version
     * @param newVersion New database version
     */
    void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion);
}
