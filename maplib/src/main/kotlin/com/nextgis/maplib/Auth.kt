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

/**
 * @class Class for holding library authentication
 *
 * @param url: The http request starting from this url will be authenticated.
 * @param clientId: client Id for oAuth2 protocol.
 * @param authServerUrl: Authentication server url. The update request will use this url.
 * @param accessToken: Access token for oAuth2.
 * @param updateToken: Update token for oAuth2.
 * @param expiresIn: The token expires period.
 * @param tokenUpdateFailedCallback: function executes if update failed.
 */
data class Auth(private val url: String, private val authServerUrl: String,
                private val accessToken: String, private val updateToken: String,
                private val expiresIn: String, private val clientId: String,
                private val tokenUpdateFailedCallback: (() -> Unit)? = null) {
    init {
        API.addAuth(this)
    }

    private fun finalize() {
        API.removeAuth(this)
    }

    internal fun onRefreshTokenFailed(url: String) {
        printMessage("Refresh oAuth token for url $url failed")
        if(url == this.url) {
            tokenUpdateFailedCallback?.invoke()
        }
    }

    /**
     * The http request starting from this url will be authenticated
     *
     * @return url string
     */
    fun getURL() : String {
        return url
    }

    /**
     * Compare Auth class instances.
     *
     * @return true if class instances are equal.
     */
    override fun equals(other: Any?): Boolean {
        val otherAuth: Auth? = other as? Auth
        return getURL() == otherAuth?.getURL() ?: ""
    }

    override fun hashCode(): Int {
        return (getURL() + clientId + "bearer").hashCode()
    }

    override fun toString(): String {
        return "bearer" + ":" + getURL()
    }

    /**
     * Get current authentication options. If some options changed via interaction with authentication
     * server, this function returns actual values.
     *
     * @return key-value dictionary of options
     */
    fun options() : Map<String, String> {
        return mapOf(
            "HTTPAUTH_TYPE" to "bearer",
            "HTTPAUTH_TOKEN_SERVER" to authServerUrl,
            "HTTPAUTH_ACCESS_TOKEN" to accessToken,
            "HTTPAUTH_REFRESH_TOKEN" to updateToken,
            "HTTPAUTH_EXPIRES_IN" to expiresIn,
            "HTTPAUTH_CLIENT_ID" to clientId
        )
    }
}