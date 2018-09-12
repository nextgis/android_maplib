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

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
    const val refreshTime = 0.35
    const val bigValue = 10000000.0
    const val bufferSize = 1024

    object Map {
        const val tolerance = 11.0
        const val epsg: Int = 3857
    }

    object Sizes {
        const val minPanPix: Double = 4.0
    }

//    const val tmpDirCatalogPath = "ngc://Local connections/Home/tmp"
//    const val docDirCatalogPath = "ngc://Local connections/Home/Documents"
}