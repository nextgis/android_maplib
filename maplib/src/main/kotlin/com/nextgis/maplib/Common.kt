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
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Convert Color int to hex string.
 *
 * @param color: Color in integer representation.
 * @return Hex string.
 */
fun colorToHexString(color: Int) : String {
    return "#" + Integer.toHexString(color)
}

private const val DELTA_MINUTES: Long = 1000 * 60 * 2 // Two minutes

/** Determines whether one Location reading is better than the current Location fix
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
    val isSignificantlyNewer: Boolean = timeDelta > DELTA_MINUTES
    val isSignificantlyOlder:Boolean = timeDelta < -DELTA_MINUTES

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
    val isSignificantlyLessAccurate: Boolean = accuracyDelta > 200f

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

internal fun checkPermission(context: Context, permission: String) : Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
    return true // In lower version permissions granted during install
}

internal fun toArrayOfCStrings(values: Map<String,String>?) : Array<String> {

    values?.let {
        val out = arrayListOf<String>()
        for ((key, value) in values) out.add("$key=$value")
        return out.toTypedArray()
    }
    return emptyArray()
}

internal fun printError(message: String) {
    Log.e(Constants.tag, "ngmobile: $message")
}

internal fun printMessage(message: String) {
    if (Constants.debugMode) {
        Log.i(Constants.tag, "ngmobile: $message")
    }
}

internal fun printWarning(message: String) {
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

object Constants {
    const val tag = "com.nextgis.maplib"
    const val debugMode = true
    const val refreshTime = 350L
    const val bigValue = 10000000.0
    const val bufferSize = 1024

    object Map {
        const val tolerance = 11.0
        const val epsg: Int = 3857
    }

    object Sizes {
        const val minPanPix: Double = 4.0
    }

    const val tmpDirCatalogPath = "ngc://Local connections/Home/tmp"
//    const val docDirCatalogPath = "ngc://Local connections/Home/Documents"
}