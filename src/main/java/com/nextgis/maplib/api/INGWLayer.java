/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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

import android.content.SyncResult;
import android.util.Pair;

/**
 * Each NGW layer must implement this interface.
 * @author Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 */
public interface INGWLayer
{
    /**
     * Return the account connected with this layer
     * @return account name
     */
    String getAccountName();

    /**
     * Set the account connected with this layer
     * @param account Name
     */
    void setAccountName(String account);

    /**
     * cache account data for fast access
     */
    void setAccountCacheData();

    /**
     * Sync layer data with NextGIS Web
     * @param authority the account authority
     * @param syncResult object for storing results of operations
     */
    void sync(String authority, Pair<Integer, Integer> ver, SyncResult syncResult);

    /**
     * Return the sync type (i.e. sync only attributes or the whole feature)
     * @return
     */
    int getSyncType();

    /**
     * Set sync type.
     * @see com.nextgis.maplib.util.Constants#SYNC_ALL
     * @param syncType The value result of OR different sync types
     */
    void setSyncType(int syncType);

    /**
     * Return the NextGIS Web server identificator - uniq identificator set for layer on server.
     * @return The NextGIS Web server identificator
     */
    long getRemoteId();

    /**
     * Set NextGIS Web server identificator.
     * @param remoteId The NextGIS Web server identificator
     */
    void setRemoteId(long remoteId);
}
