/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 18.08.18 22:38.
 * Copyright (c) 2019 NextGIS, info@nextgis.com.
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

import android.util.Log


/**
 * NGWConnectionDescription NextGIS Web connection description.
 *
 * @property url NextGIS Web url.
 * @property login NextGIS Web login.
 * @property password NextGIS Web password.
 * @property isGuest If true this is anonymous access.
 */
class NGWConnectionDescription(url: String, login: String, password : String = "", isGuest : Boolean = true)
    : ConnectionDescription(Object.Type.CONTAINER_NGW,
    mapOf("url" to url, "login" to login, "password" to password, "is_guest" to if (isGuest) "yes" else "no"))

/**
 * NGWConnection NextGIS Web connection.
 *
 * @param copyFrom Origin object to copy properties.
 */
class NGWConnection(copyFrom: Object): NGWResourceGroup(copyFrom) {

    var url: String
        get() = getProperty("url", "")
        set(value) {
            setProperty("url", value, "")
        }

    var login: String
        get() = getProperty("login", "")
        set(value) {
            setProperty("login", value, "")
        }

    var isGuest: Boolean
        get() = getProperty("is_guest", "") == "yes"
        set(value) {
            setProperty("is_guest", if(value) "yes" else "no", "")
        }

    fun setPassword(password: String): Boolean {
        return setProperty("password", password, "")
    }
}

/**
 * NextGIS Web resource group.
 *
 * @param copyFrom Origin object to copy properties.
 */
open class NGWResourceGroup(copyFrom: Object): Object(copyFrom) {

    /**
     * Create new NextGIS Web resource group.
     *
     * @param name Group name.
     * @param key NextGIS Web unique key.
     * @param description Group description.
     * @return New NGWResourceGroup class instance or null.
     */
    fun createResourceGroup(name: String, key: String = "", description: String = "") : NGWResourceGroup? {
        val options = mapOf(
            "TYPE" to Type.CONTAINER_NGWGROUP.toString(),
            "CREATE_UNIQUE" to "OFF",
            "KEY" to key,
            "DESCRIPTION" to description
        )

        val group = create(name, options)
        if(group != null) {
            return NGWResourceGroup(group)
        }

        return group;
    }

    /**
     * Create new NextGIS Web tracker group.
     *
     * @param name Group name.
     * @param key NextGIS Web unique key.
     * @param description Group description.
     * @return New NGWTrackerGroup class instance or null.
     */
    fun createTrackerGroup(name: String, key: String = "", description: String = "") : NGWTrackerGroup? {
        val options = mapOf(
            "TYPE" to Type.CONTAINER_NGWTRACKERGROUP.toString(),
            "CREATE_UNIQUE" to "OFF",
            "KEY" to key,
            "DESCRIPTION" to description
        )

        val group = create(name, options)
        if(group != null) {
            return NGWTrackerGroup(group)
        }
        return group
    }

}


/**
 * NextGIS Web tracker group.
 *
 * @param copyFrom Origin object to copy properties.
 */
class NGWTrackerGroup(copyFrom: Object): Object(copyFrom) {

    /**
     * Create new NextGIS Web tracker.
     *
     * @param name Group name.
     * @param key NextGIS Web unique key.
     * @param description Group description.
     * @param tracker_id Tracker identifier.
     * @param tracker_description Tracker description.
     * @return New catalog object class instance or null.
     */
    fun createTracker(name: String, key: String = "", description: String = "", tracker_id: String,
                      tracker_description: String = "") : Object? {
        val options = mapOf(
            "TYPE" to Type.NGW_TRACKER.toString(),
            "CREATE_UNIQUE" to "OFF",
            "KEY" to key,
            "DESCRIPTION" to description,
            "TRACKER_ID" to tracker_id,
            "TRACKER_DESCRIPTION" to tracker_description,
            "TRACKER_TYPE" to "ng_mobile",
            "TRACKER_FUEL" to ""
            //"is_registered" to "null"
        )

        Log.e("TTRRAACCKKEERR", "tracker create name:" + name )
        Log.e("TTRRAACCKKEERR", "tracker create options:" + options.toString() )


        return create(name, options)
    }
}