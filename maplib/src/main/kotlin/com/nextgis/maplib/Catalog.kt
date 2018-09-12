/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 18.08.18 22:38.
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

internal data class CatalogObjectInfo(val name: String, val type: Int, val handle: Long)

/**
 * @class Catalog object class. This is base class for all catalog objects.
 */
open class Object(val name: String, val type: Int, val path: String, internal val handle: Long) {

    constructor(copyFrom: Object) : this(copyFrom.name, copyFrom.type, copyFrom.path, copyFrom.handle)
    constructor(handle: Long) : this(API.catalogObjectNameInt(handle), API.catalogObjectTypeInt(handle),
            "", handle)

    enum class ObjectType(val code: Int) {
        UNKNOWN(0),
        ROOT(51) ,          // CAT_CONTAINER_ROOT
        FOLDER(53),         // CAT_CONTAINER_DIR
        CONTAINER_NGS(64),  // CAT_CONTAINER_NGS
        CONTAINER_MEM(71),  // CAT_CONTAINER_MEM
        FC_GEOJSON(507),    // CAT_FC_GEOJSON
        FC_MEM(509),        // CAT_FC_MEM
        FC_GPKG(515),       // CAT_FC_GPKG
        RASTER_TMS(1011),   // CAT_RASTER_TMS
        TABLE_GPKG(1507);   // CAT_TABLE_GPKG

        override fun toString(): String {
            return code.toString()
        }
    }

    /**
     * Get catalog object properties.
     *
     * @param forDomain: Parameters domain
     * @return Dictionary of key-value.
     */
    fun getProperties(forDomain: String = "") : Map<String, String> {
        val properties = API.catalogObjectPropertiesInt(handle, forDomain)
        val out: MutableMap<String, String> = mutableMapOf()
        for(property in properties) {
            val parts = property.split("=")
            if(parts.size > 1) {
                out[parts[0]] = parts[1]
            }
        }
        return out
    }

    /**
     * Set catalog object property.
     *
     * @param name: Key name.
     * @param value: Key value.
     * @param domain: Domain name.
     * @return True on success.
     */
    fun setProperty(name: String, value: String, domain: String) : Boolean {
        return API.catalogObjectSetPropertyInt(handle, name, value, domain)
    }

    /**
     * Compare current catalog object with other.
     *
     * @param otherObject: Catalog object to compare.
     * @return True if equal.
     */
    fun isSame(otherObject: Object) : Boolean {
        return handle == otherObject.handle
    }

    /**
     * Get catalog object children.
     *
     * @return Array of catalog object class instances.
     */
    fun children() : List<Object> {
        val out: MutableList<Object> = mutableListOf()
        val queryResult = API.catalogObjectQueryInt(handle, 0)

        for(catalogItem in queryResult) {
            val prefix = if(path.endsWith(Catalog.separator)) {
                path
            } else {
                path + Catalog.separator
            }

            printMessage("Name: ${catalogItem.name}, type: ${catalogItem.type}, handle: ${catalogItem.handle}")

            out.add(Object(catalogItem.name, catalogItem.type,
                    prefix + catalogItem.name, catalogItem.handle))
        }
        return out
    }

    /**
     * Get child by name.
     *
     * @param name: Catalog object child name.
     * @return Catalog object child instance or null.
     */
    fun child(name: String) : Object? {
        for( childItem in children() ) {
            if( childItem.name == name ) {
                return childItem
            }
        }
        return null
    }

    /**
     * Refresh catalog object. Reread children.
     */
    fun refresh() {
        API.catalogObjectRefreshInt(handle)
    }

    /**
     * Create new catalog object.
     *
     * @param name: New object name.
     * @param options: Dictionary describing new catalog objec. The keys are created object dependent. The mandatory key is:
     *        - TYPE - this is string value of type ObjectType
     * @return Created catalog object instance or null.
     */
    fun create(name: String, options: Map<String, String> = mapOf()) : Object? {
        if(API.catalogObjectCreateInt(handle, name, toArrayOfCStrings(options))) {
            return child(name)
        }
        return null
    }

    /**
     * Create TMS datasource
     *
     * @param name: TMS connection name
     * @param url: TMS url. {x}, {y} and {z} must be present in url string
     * @param epsg: EPSG code of TMS
     * @param z_min: Minimum zoom. Default is 0
     * @param z_max: Maximum zoom. Default is 18
     * @param fullExtent: Full extent of TMS datasource. Depends on tile schema and projection
     * @param limitExtent: Data extent. Maybe equal or less of fullExtent
     * @param cacheExpires: Time in seconds to remove cahced tiles
     * @param options: Addtional options as key: value array
     * @return Catalog object or null
     */
    fun createTMS(name: String, url: String, epsg: Int, z_min: Int, z_max: Int, fullExtent: Envelope,
                  limitExtent: Envelope, cacheExpires: Int, options: Map<String, String> = mapOf()) : Object? {
        val createOptions = mutableMapOf(
            "TYPE" to ObjectType.RASTER_TMS.toString(),
            "CREATE_UNIQUE" to "OFF",
            "url" to url,
            "epsg" to epsg.toString(),
            "z_min" to z_min.toString(),
            "z_max" to z_max.toString(),
            "x_min" to fullExtent.minX.toString(),
            "y_min" to fullExtent.minY.toString(),
            "x_max" to fullExtent.maxX.toString(),
            "y_max" to fullExtent.maxY.toString(),
            "cache_expires" to cacheExpires.toString(),
            "limit_x_min" to limitExtent.minX.toString(),
            "limit_y_min" to limitExtent.minY.toString(),
            "limit_x_max" to limitExtent.maxX.toString(),
            "limit_y_max" to limitExtent.maxY.toString()
        )

        createOptions.putAll(options)
        return create(name, createOptions)
    }

    /**
     * Create new directory.
     *
     * @param name: Directory name.
     * @return Created directory or null.
     */
    fun createDirectory(name: String) : Object? {
        val options = mapOf(
                "TYPE" to ObjectType.FOLDER.toString(),
                "CREATE_UNIQUE" to "OFF"
        )
        return create(name, options)
    }

    /**
     * Delete catalog object.
     *
     * @return True on success.
     */
    fun delete() : Boolean {
        return API.catalogObjectDeleteInt(handle)
    }

    /**
     * Delete catalog object with name.
     *
     * @param name: Object name to delete.
     * @return True on success.
     */
    fun delete(name: String) : Boolean {
        return child(name)?.delete() ?: return false
    }

    /**
     * Copy current catalog object to destination object.
     *
     * @param asType: Output catalog object type.
     * @param inDestination: Destination catalog object.
     * @param move: Move object. This object will be deleted.
     * @param withOptions: Key-value dictionary. This will affect how the copy will be performed.
     * @param callback: Callback function. May be null
     * @return True on success.
     */
    fun copy(asType: ObjectType, inDestination: Object, move: Boolean,
             withOptions: Map<String, String> = mapOf(),
             callback: ((status: StatusCode, complete: Double, message: String) -> Boolean)? = null) : Boolean {

        val createOptions = mutableMapOf(
                "TYPE" to asType.toString(),
                "MOVE" to if ( move ) "ON" else "OFF"
        )

        createOptions.putAll(withOptions)

        return API.catalogObjectCopy(handle, inDestination.handle, toArrayOfCStrings(createOptions), callback)
    }

    companion object {
        /**
         * Check if type is non spatial table.
         *
         * @param type: Type to check.
         * @return True if this type belongs to table types.
         */
        private fun isTable(type: Int) : Boolean {
            return type in 1500..1999
        }

        /**
         * Check if type is raster.
         *
         * @param type: Type to check.
         * @return True if this type belongs to raster types.
         */
        private fun isRaster(type: Int) : Boolean {
            return type in 1000..1499
        }

        /**
         * Check if type is featureclass.
         *
         * @param type: Type to check.
         * @return True if this type belongs to featureclass types.
         */
        private fun isFeatureClass(type: Int) : Boolean {
            return type in 500..999
        }

        /**
         * Check if type is container (catalog object which can hold other objects).
         *
         * @param type: Type to check.
         * @return True if this type belongs to container types.
         */
        private fun isContainer(type: Int) : Boolean {
            return type in 50..499
        }

        /**
         * Force catalog object instance to table.
         *
         * @param table: Catalog object instance.
         * @return Table class instance or null.
         */
        fun forceChildToTable(table: Object) : Table? {
            if( isTable(table.type) ) {
                return Table(table)
            }
            return null
        }

        /**
         * Force catalog object instance to featureclass.
         *
         * @param featureClass: Catalog object instance.
         * @return FeatureClass class instance or null.
         */
        fun forceChildToFeatureClass(featureClass: Object) : FeatureClass? {
            if( isFeatureClass(featureClass.type) ) {
                return FeatureClass(featureClass)
            }
            return null
        }

        /**
         * Force catalog object instance to raster.
         *
         * @param raster: Catalog object instance.
         * @return Raster class instance or null.
         */
        fun forceChildToRaster(raster: Object) : Raster? {
            if( isRaster(raster.type) ) {
                return Raster(raster)
            }
            return null
        }

        /**
         * Force catalog object instance to memory store.
         *
         * @param memoryStore: Catalog object instance.
         * @return MemoryStore class instance or null.
         */
        fun forceChildToMemoryStore(memoryStore: Object) : MemoryStore? {
            if( memoryStore.type == ObjectType.CONTAINER_MEM.code ) {
                return MemoryStore(memoryStore)
            }
            return null
        }
    }
}

class Catalog(handle: Long) : Object("Catalog", ObjectType.ROOT.code, "ngc://", handle) {

    companion object {
        const val separator = "/"

        /**
         * Get current directory
         *
         * @return Get current directory. This is file system path
         */
        fun getCurrentDirectory() : String {
            return API.getCurrentDirectory()
        }

        internal fun getOrCreateFolder(parent: Object, name: String) : Object? {
            return parent.child(name) ?: return parent.createDirectory(name)
        }
    }

    /**
     * Get catalog child by file system path.
     *
     * @param path: File system path.
     * @return Catalog object class instance or nil.
     */
    fun childByPath(path: String) : Object? {
        val objectHandle = API.catalogObjectGetInt(path)
        if(objectHandle > 0) {
            val objectType = API.catalogObjectTypeInt(objectHandle)
            val objectName = API.catalogObjectNameInt(objectHandle)
            return Object(objectName, objectType, path, objectHandle)
        }
        return null
    }
}