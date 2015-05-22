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

import android.accounts.Account;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapBase;


/**
 * Interface that all applications using the library should implements. This is use in content provider.
 * If your application will not implement this interface - the syncronize vector layers with server
 * will not work.
 */
public interface IGISApplication
{
    /**
     *
     * @return A MapBase or any inherited classes or null if not created in application
     */
    public MapBase getMap();

    /**
     *
     * @return A authority for sync purposes or empty string in not sync anything
     */
    public String getAuthority();

    /**
     * @param accountName Account name
     * @return Account by its name
     */
    public Account getAccount(String accountName);

    /**
     * @param account Account
     * @return Account URL
     */
    public String getAccountUrl(Account account);

    /**
     * @param account Account
     * @return Account login
     */
    public String getAccountLogin(Account account);

    /**
     * @param account Account
     * @return Account password
     */
    public String getAccountPassword(Account account);

    /**
     *
     * @return A GpsEventSource or null if not needed or created in application
     */
    public GpsEventSource getGpsEventSource();

    /**
     * Show settings Activity
     */
    public void showSettings();
}
