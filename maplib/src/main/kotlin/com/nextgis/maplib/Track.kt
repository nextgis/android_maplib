/*
 * Project:  NextGIS Tracker
 * Purpose:  Software tracker for nextgis.com cloud
 * Author:   Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 * ****************************************************************************
 * Copyright (c) 2018-2019 NextGIS <info@nextgis.com>
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

package com.nextgis.maplib

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

/**
 * Track information class.
 *
 * @property name Track name
 * @property start Track start date
 * @property stop Track stop date
 * @property count Track points count
 */
data class TrackInfo(val name: String, val start: Date, val stop: Date, val count: Long) {
    internal constructor(internalItem: TrackInfoInt) : this(internalItem.name, Date(internalItem.start * Constants.millisecondsInSecond), Date(internalItem.stop * Constants.millisecondsInSecond), internalItem.count)
}

internal data class TrackInfoInt(val name: String, val start: Long, val stop: Long, val count: Long)

/**
 * GPS location class.
 *
 * @property longitude Longitude of location.
 * @property latitude Latitude of location.
 * @property altitude Altitude of location.
 * @property accuracy Accuracy.
 * @property speed Speed at point.
 * @property time Timestamp.
 * @property satelliteCount Satellite count.
 */
@Parcelize
data class Location(val longitude: Double, val latitude: Double, val altitude: Double,
                    val accuracy: Float, val speed: Float, val course: Float, val time: Long, val provider: String,
                    val bearing: Float, val satelliteCount: Int) : Parcelable {
    constructor(loc: android.location.Location, satelliteCount: Int) : this(loc.longitude,
            loc.latitude, loc.altitude, loc.accuracy, loc.speed, loc.bearing,loc.time, loc.provider!!, loc.bearing,
            satelliteCount)
    fun hasAccuracy(): Boolean = accuracy > 0.0f
}

/**
 * Track. GPS Track class.
 */
open class Track(private val handle: Long) {

    companion object {
        /**
         * Get tracker identifier.
         *
         * @param regenerate If true, the new identifier will be generated.
         * @return String with tracker identifier.
         */
        fun getId(regenerate: Boolean = false): String {
            return API.getDeviceId(regenerate)
        }

        /**
         * Is current tracker identifier registered at NextGIS Tracker Hub.
         *
         * @return true if registered at NextGIS Tracker Hub.
         */
        fun isRegistered(): Boolean {
            return API.trackIsRegisteredInt()
        }
    }

    /**
     * Track count readonly property.
     */
    val count: Long get() = API.featureClassCountInt(handle)

    /**
     * Sync coordinates with NextGIS tracker service.
     */
    fun sync() {
        API.catalogObjectSyncInt(handle)
    }

    /**
     * Get available tracks list.
     *
     * @return Array of tracks information.
     */
    fun getTracks() : Array<TrackInfo> {
        val out = mutableListOf<TrackInfo>()
        val items = API.trackGetListInt(handle)
        if(items.isEmpty()) {
            printError("Tracks list is empty. " + API.lastError())
        }
        for (item in items) {
            out.add(TrackInfo(item))
        }
        return out.toTypedArray()
    }

    /**
     * Export track to GPX
     *
     * @param start Track start date.
     * @param stop Track stop date.
     * @param name GPX file name.
     * @param destination Destination path (must be folder)
     * @param callback Export progress
     * @return True on success.
     */
    fun export(start: Date, stop: Date, name: String, destination: Object,
               callback: ((status: StatusCode, complete: Double, message: String) -> Boolean)? = null) : Boolean {

        val pointsHandle = API.trackGetPointsTableInt(handle)
        if(pointsHandle == 0L) {
            return false
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val startStr = sdf.format(start)
        val stopStr = sdf.format(stop)

        printMessage("Export track: time_stamp >= '$startStr' and time_stamp <= '$stopStr'")

        if(!API.featureClassSetFilterInt(pointsHandle, 0,
                "time_stamp >= '$startStr' and time_stamp <= '$stopStr'")) {
            return false
        }

        val createOptions = mapOf(
            "TYPE" to Object.Type.FC_GPX.toString(),
            "CREATE_UNIQUE" to "OFF",
            "OVERWRITE" to "ON",
            "DS_NAME" to name,
            "LAYER_NAME" to "track_points",
            "GPX_USE_EXTENSIONS" to "ON",
            "SKIP_EMPTY_GEOMETRY" to "ON"
        )

        val result = API.catalogObjectCopy(pointsHandle, destination.handle, toArrayOfCStrings(createOptions), callback)

        // Reset filter
        API.featureClassSetFilterInt(pointsHandle, 0, "")

        return result
    }

    /**
     * Add new point to current track
     *
     * @param name Track name
     * @param location Point coordinates and other options
     * @param startTrack Is this new point starts new track
     * @param startSegment Is this point starts new track segment
     * @return True on success.
     */
    fun addPoint(name: String, location: Location, startTrack: Boolean, startSegment: Boolean) : Boolean {
        return API.trackAddPointInt(handle, name, location.longitude, location.latitude, location.altitude, location.accuracy,
            location.speed, location.course, location.time, location.satelliteCount, startTrack, startSegment)
    }

    /**
     * Delete points from start to stop time
     *
     * @param start Start timestamp to delete points
     * @param stop Stop timestamp to delete points
     * @return True on success.
     */
    fun deletePoints(start: Date, stop: Date) : Boolean {
        return API.trackDeletePointsInt(handle, start.time, stop.time)
    }
}