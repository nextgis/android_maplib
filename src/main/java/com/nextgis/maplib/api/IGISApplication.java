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

import android.accounts.Account;
import android.accounts.AccountManagerFuture;

import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapBase;


/**
 * If you plan to fix maplib or maplibui libraries, you nee to clone the sources such way:
 * <ul>
 *     <li>clone maplib and/or maplibui as submodules</li>
 *     <pre>
 *         <code>
 *             git submodule add https://github.com/nextgis/android_maplib.git maplib
 *             git submodule add https://github.com/nextgis/android_maplibui.git maplibui
 *         </code>
 *     </pre>
 *     <li>Modify settings.gradle:</li>
 *     <pre>
 *         <code>
 *             from: include ':app'
 *             to: include ':app', ':maplib', ':maplibui'
 *         </code>
 *     </pre>
 * </ul>
 * <p>
 * Note: Expected that project was created via Android studio new project wizard.
 * </p>
 * <p>
 * Interface that all applications using the library should implements. This is use in content
 * provider. If your application will not implement this interface - the synchronize vector layers
 * with server will not work.
 * </p>
 * @author Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 */
public interface IGISApplication
{
    /**
     * @return A MapBase or any inherited classes or null if not created in application
     */
    MapBase getMap();

    /**
     * @return A authority for sync purposes or empty string in not sync anything
     */
    String getAuthority();

    boolean addAccount(String name, String url, String login, String password, String token);

    void setUserData(String name, String key, String value);

    void setPassword(String name,String value);

    /**
     * @param accountName
     *         Account name
     *
     * @return Account by its name
     */
    Account getAccount(String accountName);

    /**
     * Remove an account
     * @param account Account to remove
     * @return An @see AccountManagerFuture which resolves to a Boolean, true if the account has been successfully removed
     */
    AccountManagerFuture<Boolean> removeAccount(Account account);

    /**
     * @param account
     *         Account
     *
     * @return Account URL
     */
    String getAccountUrl(Account account);

    /**
     * @param account
     *         Account
     *
     * @return Account login
     */
    String getAccountLogin(Account account);

    /**
     * @param account
     *         Account
     *
     * @return Account password
     */
    String getAccountPassword(Account account);

    /**
     * @return A GpsEventSource or null if not needed or created in application
     */
    GpsEventSource getGpsEventSource();

    /**
     * Show settings Activity
     */
    void showSettings();
}
