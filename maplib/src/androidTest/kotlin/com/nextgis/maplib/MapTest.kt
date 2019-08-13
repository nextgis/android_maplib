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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapTest {
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
            val options = mapOf(
                "OVERWRITE" to "YES"
            )
            val baseMap = dataDir.createTMS("osm.wconn", "http://tile.openstreetmap.org/{z}/{x}/{y}.png",
                    3857, 0, 18, bbox, bbox, 14, options)
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
                    "ZOOM_LEVELS" to "2,3,4,5,6,7,8,9,10,11,12,13,14",
                    "OVERWRITE" to "YES"
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