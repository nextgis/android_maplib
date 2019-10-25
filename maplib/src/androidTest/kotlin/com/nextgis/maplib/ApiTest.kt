/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 17.08.18 13:44.
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

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ApiTest {
    @Test
    fun initApi() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        API.init(appContext)
        assertNotEquals(API.version(), 0)
        assertNotEquals(API.versionString(), "")
        assertNotEquals(API.versionString("gdal"), "")
        assertNotEquals(API.versionString("geos"), "")
        assertNotEquals(API.versionString("proj"), "")
        assertNotEquals(API.versionString("sqlite"), "")
        assertNotEquals(API.versionString("png"), "")
        assertNotEquals(API.versionString("jpeg"), "")
        assertNotEquals(API.versionString("tiff"), "")
        assertNotEquals(API.versionString("geotiff"), "")

        // Check asset files copying
        val certFile = File(appContext.filesDir, "data/${BuildConfig.VERSION_CODE}/certs/cert.pem")
        assertTrue(certFile.exists())
        val gdalDataDir = File(appContext.filesDir, "data/${BuildConfig.VERSION_CODE}/gdal")
        assertTrue(gdalDataDir.listFiles().isNotEmpty())
        val projDBFile = File(appContext.filesDir, "data/${BuildConfig.VERSION_CODE}/proj/proj.db")
        assertTrue(projDBFile.exists())

        // Check catalog, store, data, temp, doc directories
        assertTrue(API.getCatalog() != null)
        assertTrue(API.getDataDirectory() != null)
        assertTrue(API.getTmpDirectory() != null)

        // Check set/get settings
        API.setProperty("TEST", "ON")
        assertEquals(API.getProperty("TEST", "OFF"), "ON")
    }

    @Test
    fun json() {
        val treesUrl = "https://raw.githubusercontent.com/nextgis/testdata/master/vector/geojson/trees.geojson"
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        API.init(appContext)

        val doc = JsonDocument()
        val options = mapOf(
            "CONNECTTIMEOUT"    to "15",
            "TIMEOUT"           to "20",
            "MAX_RETRY"         to  "20",
            "RETRY_DELAY"       to "5"
        )
        assertTrue(doc.load(treesUrl, options))

        val value = doc.getRoot()
        assertTrue(value.getString("name", "") == "trees")
        assertTrue(value.getString("type", "") == "FeatureCollection")
    }

    @Test
    fun url() {
        val treesUrl = "https://raw.githubusercontent.com/nextgis/testdata/master/vector/geojson/trees.geojson"
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        API.init(appContext)

        val options = mapOf(
                "CONNECTTIMEOUT"    to "20",
                "TIMEOUT"           to "20",
                "MAX_RETRY"         to "20",
                "RETRY_DELAY"       to "5"
        )

        val result = Request.get(treesUrl, options)
        if(result.status != 0) {
            printMessage("url code: ${result.status}, value: ${result.value}")
            printMessage(API.lastError())
        }

        assertTrue(result.status == 0)
        assertTrue(result.value.isNotEmpty())

        // Get json
        val resultJson = Request.getJson(treesUrl, options)
        assertTrue(resultJson.status == 0)
        assertTrue(resultJson.value.getString("name", "") == "trees")
        assertTrue(resultJson.value.getString("type", "") == "FeatureCollection")

        // Get raw
        val resultRaw = Request.getRaw("https://github.com/nextgis/testdata/raw/master/raster/png/tex.png",
                options)
        assertTrue(resultRaw.status == 0)
        val bmp = BitmapFactory.decodeByteArray(resultRaw.value, 0, resultRaw.value.size)
        assertTrue(bmp.height == 256)
        assertTrue(bmp.width == 256)
    }

    @Test
    fun coordinateTransform() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        API.init(appContext)

        val coordTransform = CoordinateTransformation.new(4326, 3857)
        var point = Point(37.616667, 55.75)
        var transformPoint = coordTransform.transform(point)
        val transformPointControl1 = Point(4187468.2157801385, 7508807.851301952)

        printMessage("X1: ${transformPoint.x}, Y1: ${transformPoint.y}")
        assertTrue(transformPoint == transformPointControl1)

        point = Point(-0.1275, 51.507222)
        transformPoint = coordTransform.transform(point)

        val transformPointControl2 = Point(-14193.235076142382, 6711510.640113423)
        printMessage("X2: ${transformPoint.x}, Y2: ${transformPoint.y}")
        assertTrue(transformPoint == transformPointControl2)
    }

    @Test
    fun backup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        API.init(appContext)

        val dstObj = API.getTmpDirectory()
        assertTrue(API.backup("test.zip", dstObj!!))

        val sysPath = dstObj.getProperty("system_path", "")
        assertFalse(sysPath.isEmpty())

        val buFile = File(sysPath, "test.zip")
        assertTrue(buFile.exists())
    }

    @Test
    fun auth() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        API.init(appContext)

        val account = Account("", "", "")
        val accountOpt = account.options()
        assertFalse(accountOpt.isEmpty())
        assertFalse(account.authorized)

        val test = Auth("https://demo.nextgis.com", "https://my.nextgis.com", "", "", "120", "")
        val options = test.options()
        assertFalse(options.isEmpty())
    }
}