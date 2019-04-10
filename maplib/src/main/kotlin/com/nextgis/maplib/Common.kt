/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 17.08.18 0:52.
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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.nextgis.maplib.service.TrackerService
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream



/**
 * Convert Color int to hex string.
 *
 * @param color Color in integer representation.
 * @return Hex string.
 */
fun colorToHexString(color: Int) : String {
    return "#" + Integer.toHexString(color)
}

/**
 * Determines whether one Location reading is better than the current Location fix
 *
 * @param location The new Location that you want to evaluate
 * @param currentBestLocation The current Location fix, to which you want to compare the new one
 */
fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
    if (currentBestLocation == null) {
        // A new location is always better than no location
        return true
    }

    // Check whether the new location fix is newer or older
    val timeDelta: Long = location.time - currentBestLocation.time
    val isSignificantlyNewer: Boolean = timeDelta > Constants.Location.deltaMinutes
    val isSignificantlyOlder:Boolean = timeDelta < -Constants.Location.deltaMinutes

    when {
        // If it's been more than delta minutes since the current location, use the new location
        // because the user has likely moved
        isSignificantlyNewer -> return true
        // If the new location is more than two minutes older, it must be worse
        isSignificantlyOlder -> return false
    }

    // Check whether the new location fix is more or less accurate
    val isNewer: Boolean = timeDelta > 0L
    val accuracyDelta: Float = location.accuracy - currentBestLocation.accuracy
    val isLessAccurate: Boolean = accuracyDelta > 0f
    val isMoreAccurate: Boolean = accuracyDelta < 0f
    val isSignificantlyLessAccurate: Boolean = accuracyDelta > if(location.provider == LocationManager.GPS_PROVIDER) Constants.Location.significantLessAccurateGPS else Constants.Location.significantLessAccurateCell

    // Check if the old and new location are from the same provider
    val isFromSameProvider: Boolean = location.provider == currentBestLocation.provider

    // Determine location quality using a combination of timeliness and accuracy
    return when {
        isMoreAccurate -> true
        isNewer && !isLessAccurate -> true
        isNewer && !isSignificantlyLessAccurate && isFromSameProvider -> true
        else -> false
    }
}

/**
 * Check if permission is granted or not. On Android M or higher do real work, otherwise return true.
 *
 * @return True if permission granted
 */
fun checkPermission(context: Context, permission: String) : Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
    return true // In lower version permissions granted during install
}

/**
 * Start, stop or update tracker service
 *
 * @param context Application context
 * @param command Tracker service command enum value
 * @param extraIntent Intent to put in service start intent EXTRA_INTENT field
 * @param options Key - value string map. Key set to intent key, and value - to intent value.
 */
fun startTrackerService(context: Context, command: TrackerService.Command, extraIntent: Intent? = null,
                        options: Map<String, String> = mapOf()) {
    val intent = Intent(context, TrackerService::class.java)
    intent.action = command.code
    if(command == TrackerService.Command.START && extraIntent != null) {
        intent.putExtra(Intent.EXTRA_INTENT, extraIntent)
    }

    for ((k, v) in options) {
        intent.putExtra(k, v)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    }
    else {
        context.startService(intent)
    }
}

fun formatCoordinate(coordinate: Double, outputType: Int, fraction: Int): String {
    if(outputType == Location.FORMAT_DEGREES) {
        val formatString = "%.${fraction}f${Constants.Location.degreeChar}"
        return formatString.format(coordinate)
    }

    // Get minutes
    val degrees = coordinate.toInt()
    val minutes = (coordinate - degrees) * 60

    if(outputType == Location.FORMAT_MINUTES) {
        val formatString = "%d${Constants.Location.degreeChar} %.${fraction}f\'"
        return formatString.format(degrees, minutes)
    }

    val minutesRound = minutes.toInt()
    val seconds = (minutes - minutesRound) * 60

    val formatString = "%d${Constants.Location.degreeChar} %d\' %.${fraction}f\""
    return formatString.format(degrees, minutesRound, seconds)
}

internal fun toArrayOfCStrings(values: Map<String,String>?) : Array<String> {

    values?.let {
        val out = arrayListOf<String>()
        for ((key, value) in values) out.add("$key=$value")
        return out.toTypedArray()
    }
    return emptyArray()
}

/**
 * Print error message to console
 *
 * @param message Error to print
 */
fun printError(message: String) {
    Log.e(Constants.tag, "ngmobile: $message")
}

/**
 * Print message to console
 *
 * @param message Message to print
 */
fun printMessage(message: String) {
    if (Constants.debugMode) {
        Log.i(Constants.tag, "ngmobile: $message")
    }
}

/**
 * Print warning message to console
 *
 * @param message Warning message to print
 */
fun printWarning(message: String) {
    if (Constants.debugMode) {
        Log.w(Constants.tag, "ngmobile: $message")
    }
}

internal fun copyFrom(inStream: InputStream, outFile: File) {
    printMessage("copyFrom to ${outFile.path}")

    val outStream = FileOutputStream(outFile)
    val buffer = ByteArray(Constants.bufferSize)
    var read = inStream.read(buffer)
    while(read != -1) {
        outStream.write(buffer, 0, read)
        read = inStream.read(buffer)
    }

    outStream.close()
}

internal fun isMapIdValid(mapId: Int) : Boolean {
    return mapId > Constants.notFound && mapId < 128
}

object Constants {
    const val tag = "com.nextgis.maplib"
    const val debugMode = true
    const val refreshTime = 330L
    const val bigValue = 10000000.0
    const val bufferSize = 1024
    const val notFound = -1
    const val millisecondsInSecond = 1000
    const val millisecondsPerDay = 24 * 60 * 60 * millisecondsInSecond
    const val foregroundId = 107

    object Store {
        const val name = "default"
    }

    object Map {
        const val tolerance = 11.0
        const val epsg: Int = 3857
    }

    object Sizes {
        const val minPanPix: Double = 4.0
    }

    object Location {
        const val degreeChar = "°"
        const val deltaMinutes: Long = 1000 * 60 * 2 // Two minutes in milliseconds
        const val significantLessAccurateGPS = 75f
        const val significantLessAccurateCell = 200f
    }

    object Settings {
        const val sendTracksToNGWKey = "sendTracksToNGWKey"
        const val sendTracksPointsMaxKey = "sendTracksPointsMaxKey"
        const val lastSyncTimestampKey = "lastSyncTimestampKey"
        const val exceptionKey = "exception"
    }

    const val tmpDirCatalogPath = "ngc://Local connections/Home/tmp"
//    const val docDirCatalogPath = "ngc://Local connections/Home/Documents"
}
