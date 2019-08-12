/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 19.08.18 17:29.
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

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Point class. Holds X and Y coordinates.
 *
 * @property x X coordinate.
 * @property y Y coordinate.
 */
data class Point(var x: Double = 0.0, var y: Double = 0.0) {
    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Point
        val delta = 0.00000001

        if (abs(x - other.x) < delta && abs(y - other.y) < delta) return true

        return false
    }

    override fun hashCode(): Int {
        return x.hashCode() + y.hashCode()
    }
}

/**
 * Coordinate transformation class. Helps to transform from one spatial reference to another.
 */
class CoordinateTransformation(val handle: Long) {

    private fun finalize() {
        API.coordinateTransformationFreeInt(handle)
    }

    companion object {
        /**
         * Create new coordinate transformation.
         *
         * @param fromEPSG Source EPSG spatial reference code.
         * @param toEPSG Destination EPSG spatial reference code.
         * @return CoordinateTransformation class instance.
         */
        fun new(fromEPSG: Int, toEPSG: Int) : CoordinateTransformation {
            return CoordinateTransformation(API.coordinateTransformationCreateInt(fromEPSG, toEPSG))
        }
    }

    /**
     * Perform transformation of point from one spatial reference to another.
     *
     * @param point Point to transform.
     * @return Point with new coordinates.
     */
    fun transform(point: Point) : Point {
        return API.coordinateTransformationDoInt(handle, point.x, point.y)
    }
}

/**
 * Spatial envelope.
 *
 * @property minX Minimum X value.
 * @property maxX Maximum X value.
 * @property minY Minimum Y value.
 * @property maxY Maximum X value.
 */
data class Envelope(var minX: Double = 0.0, var maxX: Double = 0.0, var minY: Double = 0.0,
                    var maxY: Double = 0.0) {

    /**
     * Envelope width.
     */
    val width: Double get() = this.maxX - this.minX

    /**
     * Envelope height.
     */
    val height: Double get() = this.maxY - this.minY

    /**
     * Envelope center.
     */
    val center: Point get() = Point(minX + width / 2, minY + height / 2)



    /**
     * Check if envelope is init.
     *
     * @return True if envelope is init.
     */
    fun isInit() : Boolean {
        return minX != 0.0 && minY != 0.0 && maxX != 0.0 && maxY != 0.0
    }

    /**
     * Merge envelope with other envelope. The result of extent of this and other envelop will be set to this envelope.
     *
     * @param other Other envelope.
     */
    fun merge(other: Envelope) {
        if( isInit() ) {
            minX = min(minX, other.minX)
            minY = min(minY, other.minY)
            maxX = max(maxX, other.maxX)
            maxY = max(maxY, other.maxY)
        }
        else {
            minX = other.minX
            minY = other.minY
            maxX = other.maxX
            maxY = other.maxY
        }
    }

    /**
     * Increase envelope by value.
     *
     * @param byValue Value to increase width and height of envelope. May be negative for decrease sizes.
     */
    fun increase(byValue: Double) {
        val deltaWidth = (width * byValue - width) / 2.0
        val deltaHeight = (height * byValue - height) / 2.0
        minX -= deltaWidth
        minY -= deltaHeight
        maxX += deltaWidth
        maxY += deltaHeight
    }

    /**
     * Transform envelope from one spatial reference to another.
     *
     * @param fromEPSG Source spatial reference EPSG code.
     * @param toEPSG Destination spatial reference EPSD code.
     */
    fun transform(fromEPSG: Int, toEPSG: Int) {
        val newTransform = CoordinateTransformation.new(fromEPSG, toEPSG)
        val points: ArrayList<Point> = arrayListOf()
        points.add(Point(minX, minY))
        points.add(Point(minX, maxY))
        points.add(Point(maxX, maxY))
        points.add(Point(maxX, minY))

        for((index, point) in points.withIndex()) {
            points[index] = newTransform.transform(point)
        }

        minX = Constants.bigValue
        minY = Constants.bigValue
        maxX = -Constants.bigValue
        maxY = -Constants.bigValue

        for(point in points) {
            if( minX > point.x ) {
                minX = point.x
            }
            if( minY > point.y ) {
                minY = point.y
            }
            if( maxX < point.x ) {
                maxX = point.x
            }
            if( maxY < point.y ) {
                maxY = point.y
            }
        }
    }
}

/**
 * Geometry class.
 *
 * @property handle C API handle
 */
open class Geometry(val handle : Long) {

    /**
     * Geometry type.
     */
    enum class Type(val code: Int) {
        NONE(0),            /**< No geometry. */
        POINT(1),           /**< Point. */
        LINESTRING(2),      /**< Linestring. */
        POLYGON(3),         /**< Polygon. */
        MULTIPOINT(4),      /**< Multipoint. */
        MULTILINESTRING(5), /**< Multilinestring. */
        MULTIPOLYGON(6);    /**< Multipolygon. */

        companion object {
            fun from(value: Int): Type {
                for (code in values()) {
                    if (code.code == value) {
                        return code
                    }
                }
                return NONE
            }
        }
    }

    companion object {

        /**
         * Get name from geometry type.
         *
         * @param geometryType Geometry type.
         * @return Geometry type name string.
         */
        fun typeToName(geometryType: Type) : String {
            return when(geometryType) {
                Type.NONE -> "NONE"
                Type.POINT -> "POINT"
                Type.LINESTRING -> "LINESTRING"
                Type.POLYGON -> "POLYGON"
                Type.MULTIPOINT -> "MULTIPOINT"
                Type.MULTILINESTRING -> "MULTILINESTRING"
                Type.MULTIPOLYGON -> "MULTIPOLYGON"
            }
        }

        /**
         * Create geometry from json object. The GeoJson geometry part.
         *
         * @param json JsonObject class instance.
         * @return Geometry or null.
         */
        fun createFromJson(json: JsonObject) : Geometry? {
            val geomHandle = API.featureCreateGeometryFromJsonInt(json.handle)
            if(geomHandle != 0L) {
                return Geometry(geomHandle)
            }
            return null
        }
    }

    /**
     * Envelope of geometry.
     */
    val envelope: Envelope get() = API.geometryGetEnvelopeInt(handle)

    /**
     * Is empty geometry.
     */
    val isEmpty: Boolean get() = API.geometryIsEmptyInt(handle)

    /**
     * Geometry type.
     */
    val type: Type get() = Type.from(API.geometryGetTypeInt(handle))

    private fun finalize() {
        API.geometryFreeInt(handle)
    }

    /**
     * Transform geometry from one spatial reference to another.
     *
     * @param epsg Destination spatial reference.
     * @return True on success.
     */
    fun transform(toEPSG: Int) : Boolean {
        return API.geometryTransformToInt(handle, toEPSG)
    }

    /**
     * Transform geometry from one spatial reference to another.
     *
     * @param transformation CoordinateTransformation class instance.
     * @return True on success.
     */
    fun transform(transformation: CoordinateTransformation) : Boolean {
        return API.geometryTransformInt(handle, transformation.handle)
    }

    /**
     * Transform geometry to GeoJson string.
     *
     * @return GeoJson string.
     */
    fun asJson() : String {
        return API.geometryToJsonInt(handle)
    }
}


/**
 * Geometry point class.
 *
 * @property handle C API handle
 */
class GeoPoint(handle: Long) : Geometry(handle) {

    /**
     * Set the point location
     *
     * @param x input X coordinate
     * @param y input Y coordinate
     * @param z input Z coordinate
     * @param m input M coordinate
     */
    fun setCoordinates(x: Double, y: Double, z: Double = 0.0, m: Double = 0.0) {
        API.geometrySetPointInt(handle, 0, x, y, z, m)
    }

    /**
     * Set the point location.
     *
     * @param point input raw point struct
     * @param z input Z coordinate
     * @param m input M coordinate
     */
    fun setCoordinates(point: Point, z: Double = 0.0, m: Double = 0.0) {
        API.geometrySetPointInt(handle, 0, point.x, point.y, z, m)
    }
}