/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 26.09.18 23:17.
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
 * @class Layer
 */
class Layer(internal val handle: Long) {

    /**
     * Layer name read/write property
     */
    var name: String
        get() = API.layerGetNameInt(handle)
        set(newName) {
            API.layerSetNameInt(handle, newName)
        }

    /**
     * Layer visible read/write property
     */
    var visible: Boolean
        get() = API.layerGetVisibleInt(handle)
        set(newVisibility) {
            API.layerSetVisibleInt(handle, newVisibility)
        }

    /**
     * Layer data source readonly property
     */
    val dataSource: Object
        get() {
            val out = Object(API.layerGetDataSourceInt(handle))
            if(Object.isFeatureClass(out.type)) {
                return FeatureClass(out)
            }
            if(Object.isRaster(out.type)) {
                return Raster(out)
            }
            return out // TODO: Add support for other types of objects (Table)
        }

    /**
     * Layer style in Json format read/write property
     */
    var style: JsonObject
        get() = JsonObject(API.layerGetStyleInt(handle))
        set(newValue) {
            API.layerSetStyleInt(handle, newValue.handle)
        }

    /**
     * Layer style name (type) read/write property. The supported styles are:
     *  - simplePoint
     *  - primitivePoint
     *  - simpleLine
     *  - simpleFill
     *  - simpleFillBordered
     *  - marker
     *  The style connected with geometry type of vector layer. If you set style of incompatible type the layer will not see on map.
     */
    var styleName: String
        get() = API.layerGetStyleNameInt(handle)
        set(newValue) {
            API.layerSetStyleNameInt(handle, newValue)
        }

    /**
     * Find features in vector layer that intersect envelope
     *
     * @param envelope: Envelope to test on intersection
     * @param limit: The return feature limit
     * @return The array of features from datasource in layer
     */
    fun identify(envelope: Envelope, limit: Int = 0) : Array<Feature> {
        val out = mutableListOf<Feature>()
        val source = dataSource
        var count = 0
        if(Object.isFeatureClass(source.type)) {
            val fc = source as? FeatureClass
            if(fc != null) {
                fc.setSpatialFilter(envelope)
                var f = fc.nextFeature()
                while(f != null) {
                    out.add(f)
                    if(limit != 0 && count >= limit) {
                        break
                    }
                    count++
                    f = fc.nextFeature()
                }
                fc.clearFilters()
            }
        }
        return out.toTypedArray()
    }

    /**
     * Highlight feature in layer. Change the feature style to selection style. The selection style mast be set in map.
     *
     * @param features: Features array. If array is empty the current highlighted features will get layer style and drawn not hightailed.
     */
    fun select(features: List<Feature> = listOf()) {
        val ids = mutableListOf<Long>()
        for(feature in features) {
            ids.add(feature.id)
        }
        API.layerSetSelectionIdsInt(handle, ids.toLongArray())
    }
}