/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 18.08.18 22:38.
 * Copyright (c) 2018-2019 NextGIS, info@nextgis.com.
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
 * Catalog object class. This is base class for all catalog objects.
 *
 * @property name Object name
 * @property type Object type
 * @property path Object path
 * @property handle Object handle for C API
 */
open class Object(val name: String, val type: Int, val path: String, internal val handle: Long) {

    constructor(copyFrom: Object) : this(copyFrom.name, copyFrom.type, copyFrom.path, copyFrom.handle)

    /**
     * Third constructor
     *
     * @param handle Object handle for C API
     */
    constructor(handle: Long) : this(API.catalogObjectNameInt(handle), API.catalogObjectTypeInt(handle),
            API.catalogObjectPathInt(handle), handle)

    /**
     * Catalog object types
     */
    enum class Type(val code: Int) {
        UNKNOWN(0),
        ROOT(51) ,                      // CAT_CONTAINER_ROOT
        FOLDER(53),                     // CAT_CONTAINER_DIR
        CONTAINER_NGW(63),              // CAT_CONTAINER_NGW
        CONTAINER_NGS(64),              // CAT_CONTAINER_NGS
        CONTAINER_MEM(71),              // CAT_CONTAINER_MEM
        CONTAINER_NGWGROUP(3001),       // CAT_NGW_GROUP
        CONTAINER_NGWTRACKERGROUP(3002),// CAT_NGW_TRACKERGROUP
        FC_GEOJSON(507),                // CAT_FC_GEOJSON
        FC_MEM(509),                    // CAT_FC_MEM
        FC_GPKG(515),                   // CAT_FC_GPKG
        FC_GPX(517),                    // CAT_FC_GPX
        RASTER_TMS(1011),               // CAT_RASTER_TMS
        TABLE_GPKG(1507),               // CAT_TABLE_GPKG
        NGW_TRACKER(3016);              // CAT_NGW_TRACKER

        override fun toString(): String {
            return code.toString()
        }

        companion object {
            fun from(value: Int): Type {
                for (code in values()) {
                    if (code.code == value) {
                        return code
                    }
                }
                return UNKNOWN
            }
        }
    }

    /**
     * Get catalog object properties.
     *
     * @param forDomain Parameters domain
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
     * Get catalog object property.
     *
     * @param name Key name.
     * @param defaultValue Default value.
     * @param domain Domain name.
     * @return Property value.
     */
    fun getProperty(name: String, defaultValue: String, domain: String = "") : String {
        return API.catalogObjectGetPropertyInt(handle, name, defaultValue, domain)
    }

    /**
     * Set catalog object property.
     *
     * @param name Key name.
     * @param value Key value.
     * @param domain Domain name.
     * @return True on success.
     */
    fun setProperty(name: String, value: String, domain: String) : Boolean {
        return API.catalogObjectSetPropertyInt(handle, name, value, domain)
    }

    /**
     * Compare current catalog object with other.
     *
     * @param otherObject Catalog object to compare.
     * @return True if equal.
     */
    fun isSame(otherObject: Object) : Boolean {
        return handle == otherObject.handle
    }

    /**
     * Get catalog object children.
     *
     * @param filter Catalog object types to filter.
     * @return Array of catalog object class instances.
     */
    fun children(filter: Array<Type> = emptyArray()) : Array<Object> {
        val out: MutableList<Object> = mutableListOf()
        val queryResult: Array<CatalogObjectInfo>
        queryResult = if (filter.isEmpty()) {
            API.catalogObjectQueryInt(handle, 0)
        } else {
            val filterArray = arrayListOf<Int>()
            for (value in filter) filterArray.add(value.code)
            API.catalogObjectQueryMultiFilterInt(handle, filterArray.toTypedArray())
        }
        for(catalogItem in queryResult) {
            printMessage("Name: ${catalogItem.name}, type: ${catalogItem.type}, handle: ${catalogItem.handle}")

            out.add(Object(catalogItem.name, catalogItem.type,
                    API.catalogObjectPathInt(catalogItem.handle), catalogItem.handle))
        }
        return out.toTypedArray()
    }

    /**
     * Get child by name.
     *
     * @param name Catalog object child name.
     * @param fullMatch If true the name must be equal, else the function return last child the name begins with 'name'.
     * @return Catalog object child instance or null.
     */
    fun child(name: String, fullMatch: Boolean = true) : Object? {
        val childHandle = API.catalogObjectGetByNameInt(handle, name, fullMatch)
        if (childHandle == 0L) {
            return null
        }

        return Object(childHandle)
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
     * @param name New object name.
     * @param options Dictionary describing new catalog object. The keys are created object dependent. The mandatory key is:
     *        - TYPE - this is string value of type Type
     * @return Created catalog object instance or null.
     */
    fun create(name: String, options: Map<String, String> = mapOf()) : Object? {
        val newHandle = API.catalogObjectCreateInt(handle, name, toArrayOfCStrings(options))
        if(newHandle != 0L) {
            return Object(newHandle)
        }
        return null
    }

    /**
     * Check if object type can be created at this parent object
     *
     * @param type Object type to check.
     * @return true if object of type can be created at this parent object or false.
     */
    fun canCreate(type: Type) : Boolean {
        return API.catalogObjectCanCreateInt(handle, type.code)
    }

    /**
     * Create TMS datasource
     *
     * @param name TMS connection name
     * @param url TMS url. {x}, {y} and {z} must be present in url string
     * @param epsg EPSG code of TMS
     * @param z_min Minimum zoom. Default is 0
     * @param z_max Maximum zoom. Default is 18
     * @param fullExtent Full extent of TMS datasource. Depends on tile schema and projection
     * @param limitExtent Data extent. Maybe equal or less of fullExtent
     * @param cacheExpires Time in seconds to remove old tiles
     * @param options Additional options as key: value array
     * @return Raster object or null
     */
    fun createTMS(name: String, url: String, epsg: Int, z_min: Int, z_max: Int, fullExtent: Envelope,
                  limitExtent: Envelope, cacheExpires: Int, options: Map<String, String> = mapOf()) : Raster? {
        val createOptions = mutableMapOf(
            "TYPE" to Type.RASTER_TMS.toString(),
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
        val ret = create(name, createOptions)
        if (ret != null) {
            return forceChildToRaster(ret)
        }
        return ret
    }

    /**
     * Create new directory.
     *
     * @param name Directory name.
     * @return Created directory or null.
     */
    fun createDirectory(name: String) : Object? {
        val options = mapOf(
                "TYPE" to Type.FOLDER.toString(),
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
     * @param name Object name to delete.
     * @return True on success.
     */
    fun delete(name: String) : Boolean {
        return child(name)?.delete() ?: return false
    }

    /**
     * Copy current catalog object to destination object.
     *
     * @param asType Output catalog object type.
     * @param inDestination Destination catalog object.
     * @param move Move object. This object will be deleted.
     * @param withOptions Key-value dictionary. This will affect how the copy will be performed.
     * @param callback Callback function. May be null
     * @return True on success.
     */
    fun copy(asType: Type, inDestination: Object, move: Boolean,
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
         * @param type Type to check.
         * @return True if this type belongs to table types.
         */
        fun isTable(type: Int) : Boolean {
            return type in 1500..1999
        }

        /**
         * Check if type is raster.
         *
         * @param type Type to check.
         * @return True if this type belongs to raster types.
         */
        fun isRaster(type: Int) : Boolean {
            return type in 1000..1499
        }

        /**
         * Check if type is FeatureClass.
         *
         * @param type Type to check.
         * @return True if this type belongs to FeatureClass types.
         */
        fun isFeatureClass(type: Int) : Boolean {
            return type in 500..999
        }

        /**
         * Check if type is container (catalog object which can hold other objects).
         *
         * @param type Type to check.
         * @return True if this type belongs to container types.
         */
        fun isContainer(type: Int) : Boolean {
            return type in 50..499
        }

        /**
         * Force catalog object instance to table.
         *
         * @param table Catalog object instance.
         * @return Table class instance or null.
         */
        fun forceChildToTable(table: Object) : Table? {
            if( isTable(table.type) ) {
                return Table(table)
            }
            return null
        }

        /**
         * Force catalog object instance to FeatureClass.
         *
         * @param featureClass Catalog object instance.
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
         * @param raster Catalog object instance.
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
         * @param memoryStore Catalog object instance.
         * @return MemoryStore class instance or null.
         */
        fun forceChildToMemoryStore(memoryStore: Object) : MemoryStore? {
            if( memoryStore.type == Type.CONTAINER_MEM.code ) {
                return MemoryStore(memoryStore)
            }
            return null
        }

        /**
         * Force catalog object instance to NextGIS Web resource group.
         *
         * @param ngwResource Catalog object instance.
         * @return NGWResourceGroup class instance or null.
         */
        fun forceChildToNGWResourceGroup(ngwResource: Object) : NGWResourceGroup? {
            if( ngwResource.type == Type.CONTAINER_NGW.code ||
                ngwResource.type == Type.CONTAINER_NGWGROUP.code ) {
                return NGWResourceGroup(ngwResource)
            }
            return null
        }
    }
}

/**
 * Catalog is root object of virtual file system tree.
 *
 * @property handle Object handle for C API
 */
class Catalog(handle: Long) : Object("Catalog", Type.ROOT.code, "ngc://", handle) {

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
     * Catalog root objects names
     */
    enum class RootObjects(val code: String) {
        UNKNOWN(""),
        LOCAL_CONNECTIONS("ngc://Local connections"),
        GIS_CONNECTIONS("ngc://GIS Server connections"),
        DB_CONNECTIONS("ngc://Database connections");


        companion object {
            fun from(value: String): RootObjects {
                for (code in values()) {
                    if (code.code == value) {
                        return code
                    }
                }
                return UNKNOWN
            }
        }
    }

    /**
     * Get catalog child by file system path.
     *
     * @param path File system path.
     * @return Catalog object class instance or nil.
     */
    fun childByPath(path: String) : Object? {
        val objectHandle = API.catalogObjectGetInt(path)
        if(objectHandle != 0L) { // zero is invalid handler
            val objectType = API.catalogObjectTypeInt(objectHandle)
            val objectName = API.catalogObjectNameInt(objectHandle)
            return Object(objectName, objectType, path, objectHandle)
        }
        return null
    }

    /**
     * Create connection at default directory corespondent to connection type (i.e. for NextGIS Web
     * connection, the connection file will be created at ngc://GIS Server connections path.
     *
     * @param name Connection file name.
     * @param connection Connection object. Before create connection, execute check function for test connection.
     * @param options Create options as key = value array.
     * @return Created connection object or null.
     */
    fun createConnection(name: String, connection: ConnectionDescription, options: Map<String, String> = mapOf()) : Object? {
        var parent: Object? = null
        if (connection.type == Type.CONTAINER_NGW) {
            parent = childByPath(RootObjects.GIS_CONNECTIONS.code)
        }
        if (parent == null) {
            return null
        }

        val createOptions = mutableMapOf("TYPE" to connection.type.toString())
        createOptions.putAll(connection.options)
        createOptions.putAll(options)

        return parent.create(name, createOptions)
    }
}

/**
 * ConnectionDescription is class to store connection properties.
 *
 * @property type Connection object type.
 * @property options Connection options as key = value map.
 */
open class ConnectionDescription(val type: Object.Type, val options: Map<String, String> = mapOf()) {

    /**
     * Check if connection is valid.
     *
     * @return true if connection is valid. 
     */
    fun check() : Boolean {
        return API.catalogCheckConnectionInt(type.code, toArrayOfCStrings(options))
    }
}
