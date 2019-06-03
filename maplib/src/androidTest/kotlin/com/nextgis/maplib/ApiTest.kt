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
import java.util.*

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
    fun store() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        API.init(appContext)

        // Get or create store
        val store = API.getStore("default")
        if(store == null) {
            printError(API.lastError())
        }
        assertTrue(store != null && store.handle != 0L && store.path != "")

        // Try to destroy previous layer
        val treesObj = store?.child("trees")
        if(treesObj != null) {
            assertTrue(treesObj.path != "")
            printMessage("Try to destroy trees layer from store")
            assertTrue(treesObj.delete())
        }

        // Load shape file
        val testGeojson = File(appContext.cacheDir, "test.geojson")
        val treesUrl = "https://raw.githubusercontent.com/nextgis/testdata/master/vector/geojson/trees.geojson"
        val options = mapOf(
                "CONNECTTIMEOUT"    to "30",
                "TIMEOUT"           to "30",
                "MAX_RETRY"         to "10",
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

        val createResult = testGeojsonObj?.copy(Object.Type.FC_GPKG, store!!, true, copyOptions) ?: false
        if(!createResult) {
            printError(API.lastError())
        }
        assertTrue(createResult)

        val treesFC = Object.forceChildToFeatureClass(store?.child("trees")!!)
        assertTrue(treesFC != null && treesFC.path != "")
        assertEquals(treesFC?.geometryType, Geometry.Type.POINT)

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
    fun properties() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        API.init(appContext)

        val tmpDir = API.getTmpDirectory()
        assertTrue(tmpDir != null)

        val bbox = Envelope(-20037508.34, 20037508.34, -20037508.34, 20037508.34)
        val baseMap = tmpDir?.createTMS("osm.wconn", "http://tile.openstreetmap.org/{z}/{x}/{y}.png",
                3857, 0, 18, bbox, bbox, 14)
        baseMap!!.isOpened = true
        assertTrue(baseMap.isOpened)
        var properties = baseMap.getProperties()

        assertTrue(properties.isNotEmpty())
        assertTrue(properties["TMS_CACHE_EXPIRES"] == "14")

        baseMap.setProperty("TMS_CACHE_EXPIRES", "15", "")
        baseMap.setProperty("TMS_CACHE_MAX_SIZE", "256", "")

        properties = baseMap.getProperties()

        assertTrue(properties["TMS_CACHE_EXPIRES"] == "15")
        assertTrue(properties["TMS_CACHE_MAX_SIZE"] == "256")

        assertTrue(baseMap.getProperty("TMS_CACHE_EXPIRES", "-1") == "15")
    }

    private fun gpsTime() : Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
    }

    @Test
    fun tracks() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        API.init(appContext)
        val dataStore = API.getStore("store")

        assertTrue(dataStore != null)

        val tracks = dataStore?.trackTable()
        assertTrue(tracks != null)

        var loc = Location(0.0, 0.0, 0.0, 0.0f, 0.0f, gpsTime(), "", 0.0f, 0)
        tracks?.addPoint("test1", loc, startTrack = true, startSegment = false)
        Thread.sleep(1500)
        loc = Location(1.0, 1.0, 1.0, 1.0f, 1.0f, gpsTime(),"", 0.0f, 1)
        tracks?.addPoint("test1", loc, startTrack = false, startSegment = false)
        Thread.sleep(1500)
        loc = Location(2.0, 2.0, 2.0, 2.0f, 2.0f, gpsTime(), "", 0.0f, 2)
        tracks?.addPoint("test1", loc, startTrack = false, startSegment = false)
        Thread.sleep(1500)
        loc = Location(3.0, 3.0, 3.0, 3.0f, 3.0f, gpsTime(), "", 0.0f, 3)
        tracks?.addPoint("test1", loc, startTrack = false, startSegment = false)
        Thread.sleep(1500)
        loc = Location(4.0, 4.0, 4.0, 4.0f, 4.0f, gpsTime(), "", 0.0f, 4)
        tracks?.addPoint("test1", loc, startTrack = false, startSegment = false)
        Thread.sleep(2000)
        // New segment
        loc = Location(5.0, 5.0, 5.0, 5.0f, 5.0f, gpsTime(), "", 0.0f, 5)
        tracks?.addPoint("test1", loc, startTrack = false, startSegment = true)
        Thread.sleep(1500)
        loc = Location(6.0, 6.0, 6.0, 6.0f, 6.0f, gpsTime(), "", 0.0f, 6)
        tracks?.addPoint("test1", loc, startTrack = false, startSegment = false)
        Thread.sleep(1500)
        loc = Location(7.0, 7.0, 7.0, 7.0f, 7.0f, gpsTime(), "", 0.0f, 7)
        tracks?.addPoint("test1", loc, startTrack = false, startSegment = false)
        Thread.sleep(2000)
        // New track
        loc = Location(8.0, 8.0, 8.0, 8.0f, 8.0f, gpsTime(), "", 0.0f, 8)
        tracks?.addPoint("test2", loc, startTrack = true, startSegment = false)
        Thread.sleep(1500)
        loc = Location(9.0, 9.0, 9.0, 9.0f, 9.0f, gpsTime(), "", 0.0f, 9)
        tracks?.addPoint("test2", loc, startTrack = false, startSegment = false)
        Thread.sleep(1500)
        loc = Location(10.0, 10.0, 10.0, 10.0f, 10.0f, gpsTime(), "", 0.0f, 10)
        tracks?.addPoint("test2", loc, startTrack = false, startSegment = false)

        val info = tracks?.getTracks()
        val count1: Int = info?.size ?: 0 // Potentially get empty geometry in some features.
        val count2: Int = tracks?.count?.toInt() ?: 0 // All records has valid geometry (more than 1 point).

        assertTrue(count1 > 0 && count2 > 0)
    }

    @Test
    fun layerVisibility() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        API.init(appContext)
        val map = API.getMap("main")

        assertTrue(map != null)
        addPointsTo(map!!)
        addOSMTo(map)

        assertTrue(map.layerCount == 2)

        for(index in 0 until map.layerCount) {
            val layer = map.getLayer(index)
            assertTrue(layer != null)
            layer?.visible = false
        }

        for(index in 0 until map.layerCount) {
            assertFalse(map.getLayer(index)?.visible ?: true)
        }
    }

    private fun addOSMTo(map: MapDocument) {
        val dataDir = API.getDataDirectory()
        if(dataDir != null) {
            val bbox = Envelope(-20037508.34, 20037508.34, -20037508.34, 20037508.34)
            val baseMap = dataDir.createTMS("osm.wconn", "http://tile.openstreetmap.org/{z}/{x}/{y}.png",
                    3857, 0, 18, bbox, bbox, 14)
            if(baseMap != null) {
                map.addLayer("OSM", baseMap)
            }
        }
    }

    private fun addPointsTo(map: MapDocument) {
        // Get or create data store
        val dataStore = API.getStore("store")
        if(dataStore != null) {
            // Create points feature class

            var pointsFC = dataStore.child("points")

            if(pointsFC == null) {
                val options = mapOf(
                        "CREATE_OVERVIEWS" to "ON",
                        "ZOOM_LEVELS" to "2,3,4,5,6,7,8,9,10,11,12,13,14"
                )

                val fields = listOf(
                        Field("long", "long", Field.Type.REAL),
                        Field("lat", "lat", Field.Type.REAL),
                        Field("datetime", "datetime", Field.Type.DATE, "CURRENT_TIMESTAMP"),
                        Field("name", "name", Field.Type.STRING)
                )

                pointsFC = dataStore.createFeatureClass("points", Geometry.Type.POINT, fields, options)
                if(pointsFC != null) {

                    data class PtCoord(val name: String, val x: Double, val y: Double)

                    // Add geodata to points feature class from https://en.wikipedia.org
                    val coordinates = listOf(
                            PtCoord("Moscow", 37.616667, 55.75),
                            PtCoord("London", -0.1275, 51.507222),
                            PtCoord("Washington", -77.016389, 38.904722),
                            PtCoord("Beijing", 116.383333, 39.916667)
                    )

                    val coordTransform = CoordinateTransformation.new(4326, 3857)

                    for (coordinate in coordinates) {
                        val feature = pointsFC.createFeature()
                        if (feature != null) {
                            val geom = feature.createGeometry() as? GeoPoint
                            if (geom != null) {
                                val point = Point(coordinate.x, coordinate.y)
                                val transformPoint = coordTransform.transform(point)
                                geom.setCoordinates(transformPoint)
                                feature.geometry = geom
                                feature.setField(0, coordinate.x)
                                feature.setField(1, coordinate.y)
                                feature.setField(3, coordinate.name)
                                pointsFC.insertFeature(feature)
                            }
                        }
                    }
                }
            }

            if(pointsFC != null) {
                map.addLayer("Points", pointsFC)
            }
        }
    }
}