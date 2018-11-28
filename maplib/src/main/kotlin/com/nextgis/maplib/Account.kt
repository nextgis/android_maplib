/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 06.09.18 21:26.
 * Copyright (c) 2018 NextGIS, info@nextgis.com.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib

import android.graphics.Bitmap

/**
 * Class for NextGIS Account operations.
 *
 * @param clientId Application identifier
 * @param accessToken oAuth2 access token
 * @param updateToken oAuth2 refresh token
 * @param authorizeFailedCallback Callback function. Executes if update token failed. In such case account change state to unauthorized. May be null.
 */
class Account(clientId: String, accessToken: String, updateToken: String,
              authorizeFailedCallback: (() -> Unit)? = null) {

    /**
     * User first name.
     */
    val firstName: String get() = API.accountFirstNameGetInt()

    /**
     * User last name.
     */
    val lastName: String get() = API.accountLastNameGetInt()

    /**
     * User e-mail.
     */
    val email: String get() = API.accountEmailGetInt()

    /**
     * User avatar.
     */
    val avatar: Bitmap get() = API.accountBitmapGetInt()

    /**
     * Return if account is authorised or not.
     */
    val authorized: Boolean get() = API.accountAuthorizedGetInt()

    /**
     * Return if user supported or not.
     */
    val supported: Boolean get() = API.accountSupportedGetInt()

    private val auth = Auth("https://my.nextgis.com/api/v1", "https://my.nextgis.com/oauth2/token/",
            accessToken, updateToken, "120", clientId, this::onRefreshTokenFailed)
    private val callback = authorizeFailedCallback

    init {
        API.addAuth(auth)
    }

    private fun finalize() {
        API.removeAuth(auth)
    }

    internal fun onRefreshTokenFailed() {
        callback?.invoke()
    }

    /**
     * Exit from account. Authorize property became false.
     */
    fun exit() {
        API.accountExitInt()
        API.removeAuth(auth)
    }

    /**
     * Get account options. Now return only authorize status (last access token, update token etc.).
     *
     * @return key-value dictionary of options.
     */
    fun options() : Map<String, String> {
        return auth.options()
    }

    /**
     * Check if function indicated by application name and function name is available to this account.
     *
     * @param application Application name.
     * @param function Function name.
     * @return True if function available or false.
     */
    fun isFunctionAvailable(application: String, function: String) : Boolean {
        return API.accountIsFuncAvailableInt(application, function)
    }
}