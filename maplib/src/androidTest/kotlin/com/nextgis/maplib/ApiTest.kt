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
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class ApiTest {
    @Test
    fun initApi() {
        val appContext = InstrumentationRegistry.getTargetContext()
        API.init(appContext)
        assertNotEquals(API.version(), 0)
        assertNotEquals(API.versionString(), "")

        // Check asset files copying
        val certFile = File(appContext.filesDir, "data/certs/cert.pem")
        assertTrue(certFile.exists())
        val gdalDataDir = File(appContext.filesDir, "data/gdal")
        assertTrue(gdalDataDir.listFiles().isNotEmpty())

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
        val appContext = InstrumentationRegistry.getTargetContext()
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
        val appContext = InstrumentationRegistry.getTargetContext()
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
    fun store() {
        val appContext = InstrumentationRegistry.getTargetContext()
        API.init(appContext)

        // Get or create store
        val store = API.getStore("default")
        if(store == null) {
            printError(API.lastError())
        }
        assertTrue(store != null && store.handle != 0L)

        // Try to destroy previous layer
        val treesObj = store?.child("trees")
        if(treesObj != null) {
            printMessage("Try to destroy trees layer from store")
            assertTrue(treesObj.delete())
        }

        // Load shape file
        val testGeojson = File(appContext.cacheDir, "test.geojson")
        val treesUrl = "https://raw.githubusercontent.com/nextgis/testdata/master/vector/geojson/trees.geojson"
        val options = mapOf(
                "CONNECTTIMEOUT"    to "20",
                "TIMEOUT"           to "20",
                "MAX_RETRY"         to "20",
                "RETRY_DELAY"       to "5"
        )
        val result = Request.get(treesUrl, options)
        assertTrue(result.status == 0)
        testGeojson.writeText(result.value)

        val tmpDir = API.getTmpDirectory()
        assertTrue(tmpDir != null)

        val testGeojsonObj = tmpDir?.child("test.geojson")
        if(testGeojsonObj == null || testGeojsonObj.handle == 0L) {
            printMessage(API.lastError())
        }
        assertTrue(testGeojsonObj != null && testGeojsonObj.handle != 0L)

        val copyOptions = mapOf(
                "CREATE_OVERVIEWS" to "ON",
                "NEW_NAME" to "trees"
        )

        val createResult = testGeojsonObj?.copy(Object.ObjectType.FC_GPKG, store!!, true, copyOptions) ?: false
        if(!createResult) {
            printError(API.lastError())
        }
        assertTrue(createResult)

        val treesFC = Object.forceChildToFeatureClass(store?.child("trees")!!)
        assertTrue(treesFC != null)
        assertEquals(treesFC?.geometryType, Geometry.GeometryType.POINT)

        // Get feature
        val feature = treesFC?.nextFeature()
        assertTrue(feature != null)

        // Get geometry
        val geometry = feature?.geometry
        assertTrue(geometry != null)

        // Create feature
        val newFeature = treesFC?.createFeature()!!
        newFeature.setField(0, "test")
        newFeature.setField(1, 77.7)

        // Create empty geometry
        val newGeometry = newFeature.createGeometry() as GeoPoint

        newGeometry.setCoordinates(37.5, 55.4)
        newFeature.geometry = newGeometry

        assertTrue(treesFC.insertFeature(newFeature))

        // Add attachment
        val resultRaw = Request.getRaw("https://github.com/nextgis/testdata/raw/master/raster/png/tex.png",
                options)
        assertTrue(resultRaw.status == 0)
        val testPng = File(appContext.cacheDir, "test.png")
        testPng.writeBytes(resultRaw.value)

        val attId = newFeature.addAttachment("test.png", "test picture",
                testPng.absolutePath,true)
        assertTrue(attId != -1L)
    }
}