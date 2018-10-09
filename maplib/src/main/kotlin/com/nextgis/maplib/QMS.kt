/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 02.10.18 0:13.
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

internal data class QMSItemInt(val id: Int, val name: String, val description: String, val type: Int,
                   val iconUrl: String, val status: Int, val extent: Envelope, val total: Int)

data class QMSItem(val id: Int, val name: String, val description: String, val type: Object.Type,
                      val iconUrl: String, val status: StatusCode, val extent: Envelope, val total: Int) {
    internal constructor(internalItem: QMSItemInt) : this(internalItem.id, internalItem.name,
            internalItem.description, Object.Type.from(internalItem.type), internalItem.iconUrl,
            StatusCode.from(internalItem.status), internalItem.extent, internalItem.total)
}

internal data class QMSItemPropertiesInt(val id: Int, val status: Int, val url: String, val name: String,
                                      val description: String, val type: Int, val EPSG: Int,
                                      val z_min: Int, val z_max: Int, val iconUrl: String,
                                      val extent: Envelope, val yOriginTop: Boolean)

data class QMSItemProperties(val id: Int, val status: StatusCode, val url: String, val name: String,
                             val description: String, val type: Object.Type, val EPSG: Int,
                             val z_min: Int, val z_max: Int, val iconUrl: String, val extent: Envelope,
                             val yOriginTop: Boolean) {
    internal constructor(internalItem: QMSItemPropertiesInt) : this(internalItem.id,
            StatusCode.from(internalItem.status), internalItem.url, internalItem.name,
            internalItem.description, Object.Type.from(internalItem.type), internalItem.EPSG,
            internalItem.z_min, internalItem.z_max, internalItem.iconUrl, internalItem.extent,
            internalItem.yOriginTop)
}

/**
 * @object QMS - QuickMapServices singleton
 */
object QMS {

    /**
     * Query QuickMapServices service for items
     *
     * @param options: Key - value map of options/filters. All keys are optional. Available keys are:
     *  type - services type. May be tms, wms, wfs, geojson
     *  epsg - services spatial reference EPSG code
     *  cumulative_status - services status. May be works, problematic, failed
     *  search - search string for a specific geoserver
     *  intersects_extent - only services bounding boxes intersecting provided
     *                      extents will return. Extent mast be in WKT or EWKT format.
     *  ordering - an order in which services will return. May be name, -name, id,
     *             -id, created_at, -created_at, updated_at, -updated_at
     *  limit - return services maximum count. Works together with offset.
     *  offset - offset from the beginning of the return list. Works together with limit.
     *
     * @return Array of QMSItem
     */
    fun query(options: Map<String, String> = mapOf()) : Array<QMSItem> {
        val items = API.QMSQueryInt(options)
        val out = mutableListOf<QMSItem>()
        for(item in items) {
            out.add(QMSItem(item))
        }
        return out.toTypedArray()
    }

    /**
     * Query item properties by identifier
     *
     * @param id: identifier to request properties
     * @return QMSItemProperties class instance
     */
    fun queryItemProperties(id: Int) : QMSItemProperties {
        val properties = API.QMSQueryPropertiesInt(id)
        return QMSItemProperties(properties)
    }
}