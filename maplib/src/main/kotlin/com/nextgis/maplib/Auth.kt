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
 * Class for holding library authentication
 *
 * @property url The http request starting from this url will be authenticated.
 * @property clientId client Id for oAuth2 protocol.
 * @property authServerUrl Authentication server url. The update request will use this url.
 * @property accessToken Access token for oAuth2.
 * @property updateToken Update token for oAuth2.
 * @property expiresIn The token expires period.
 * @property tokenUpdateFailedCallback function executes if update failed.
 */
data class Auth(val url: String, private val authServerUrl: String,
                public val accessToken: String, private val updateToken: String,
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

    internal fun initOptions() : Map<String, String> {
        return mapOf(
            "type" to "bearer",
            "clientId" to clientId,
            "accessToken" to accessToken,
            "updateToken" to updateToken,
            "tokenServer" to authServerUrl,
            "expiresIn" to expiresIn
        )
    }

    /**
     * Compare Auth class instances.
     *
     * @return true if class instances are equal.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Auth

        return url == other.url && accessToken == other.accessToken && updateToken == other.updateToken
    }

    override fun hashCode(): Int {
        return (url + clientId + "bearer").hashCode()
    }

    override fun toString(): String {
        return "bearer:$url"
    }

    /**
     * Get current authentication options. If some options changed via interaction with authentication
     * server, this function returns actual values.
     *
     * @return key-value dictionary of options
     */
    fun options() : Map<String, String> {
        return API.URLAuthGetPropertiesInt(url)
    }
}