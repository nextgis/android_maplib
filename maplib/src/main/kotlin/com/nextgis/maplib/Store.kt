/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 20.08.18 11:04.
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

/**
 * In memory spatial data storage. After class instance destruction all data will loose.
 *
 * @param copyFrom Origin object to copy properties.
 */
class MemoryStore(copyFrom: Object): Object(copyFrom)  {

    /**
     * Memory storage description file extension.
     */
    companion object {
        const val ext = ".ngmem"
    }

    /**
     * Create feature class in storage.
     *
     * @param name Feature class name.
     * @param geometryType Geometry type.
     * @param epsg Spatial reference EPSG code.
     * @param fields Feature class fields.
     * @param options Any other create option if form of key-value dictionary.
     * @return FeatureClass class instance or null.
     */
    fun createFeatureClass(name: String, geometryType: Geometry.Type, epsg: Int,
                           fields: List<Field>, options: Map<String, String>) : FeatureClass? {
        val fullOptions : MutableMap<String, String> = options.toMutableMap()
        fullOptions["GEOMETRY_TYPE"] = Geometry.typeToName(geometryType)
        fullOptions["TYPE"] = Type.FC_MEM.toString()
        fullOptions["EPSG"] = epsg.toString()
        fullOptions["FIELD_COUNT"] = fields.size.toString()
        for((index, field) in fields.withIndex()) {
            fullOptions["FIELD_${index}_TYPE"] = Field.fieldTypeToName(field.type)
            fullOptions["FIELD_${index}_NAME"] = field.name
            fullOptions["FIELD_${index}_ALIAS"] = field.alias
            if(field.defaultValue != null) {
                fullOptions["FIELD_${index}_DEFAULT_VAL"] = field.defaultValue
            }
        }

        if(API.catalogObjectCreateInt(handle, name, toArrayOfCStrings(fullOptions))) {
            val featureClassObject = child(name)
            if(featureClassObject != null) {
                return FeatureClass(featureClassObject)
            }
        }
        return null

    }
}

/**
 * Spatial data storage. This is geopackage with specific additions.
 *
 * @param copyFrom Origin object to copy properties.
 */
class Store(copyFrom: Object): Object(copyFrom) {

    companion object {
        /**
         * Spatial data storage file extension.
         */
        const val  ext = ".ngst"
    }

    /**
     * Create feature class in storage.
     *
     * @param name Feature class name.
     * @param geometryType Geometry type.
     * @param fields Feature class fields.
     * @param options Any other create option if form of key-value dictionary.
     * @return FeatureClass class instance or null.
     */
    fun createFeatureClass(name: String, geometryType: Geometry.Type, fields: List<Field>,
                           options: Map<String, String>) : FeatureClass? {
        val fullOptions = options.toMutableMap()
        fullOptions["GEOMETRY_TYPE"] = Geometry.typeToName(geometryType)
        fullOptions["TYPE"] = Type.FC_GPKG.toString()
        fullOptions["FIELD_COUNT"] = fields.size.toString()
        for((index, field) in fields.withIndex()) {
            fullOptions["FIELD_${index}_TYPE"] = Field.fieldTypeToName(field.type)
            fullOptions["FIELD_${index}_NAME"] = field.name
            fullOptions["FIELD_${index}_ALIAS"] = field.alias
            if( field.defaultValue != null) {
                fullOptions["FIELD_${index}_DEFAULT_VAL"] = field.defaultValue
            }
        }

        if( API.catalogObjectCreateInt(handle, name, toArrayOfCStrings(fullOptions)) ) {
            val featureClassObject = child(name)
            if( featureClassObject != null ){
                return FeatureClass(featureClassObject)
            }
        }

        return null
    }

    /**
     * Create table in storage.
     *
     * @param name Table name.
     * @param fields Table fields.
     * @param options Any other create option if form of key-value dictionary.
     * @return Table class instance or null.
     */
    fun createTable(name: String, fields: List<Field>, options: Map<String, String>) : Table? {
        val fullOptions = options.toMutableMap()
        fullOptions["TYPE"] = Type.TABLE_GPKG.toString()
        fullOptions["FIELD_COUNT"] = fields.size.toString()
        for((index, field) in fields.withIndex()) {
            fullOptions["FIELD_${index}_TYPE"] = Field.fieldTypeToName(field.type)
            fullOptions["FIELD_${index}_NAME"] = field.name
            fullOptions["FIELD_${index}_ALIAS"] = field.alias
        }

        if( API.catalogObjectCreateInt(handle, name, toArrayOfCStrings(fullOptions)) ) {
            val tableObject = child(name)
            if( tableObject != null ){
                return Table(tableObject)
            }
        }
        return null
    }

    /**
     * Get tracks table
     *
     * @return Track class instance or null
     */
    fun trackTable() : Track? {
        val tracksHandle = API.storeGetTracksTableInt(handle)
        if(tracksHandle != 0L) {
            return Track(tracksHandle)
        }

        printError(API.lastError())
        return null
    }
}

/**
 * The table, datasource, map and etc. change codes enumerator.
 */
enum class ChangeCode(val code: Int) {
    NOP(1 shl 0),
    CREATE_OBJECT(1 shl 1),
    DELETE_OBJECT(1 shl 2),
    CHANGE_OBJECT(1 shl 3),
    CREATE_FEATURE(1 shl 4),    /**< Create feature/row. */
    CHANGE_FEATURE(1 shl 5),    /**< Change feature/row. */
    DELETE_FEATURE(1 shl 6),    /**< Delete feature/row. */
    DELETEALL_FEATURES(1 shl 7),/**< Delete all features. */
    CREATE_ATTACHMENT(1 shl 8), /**< Create new attachment */
    CHANGE_ATTACHMENT(1 shl 9), /**< Change attachment name and/or description */
    DELETE_ATTACHMENT(1 shl 10),/**< Delete attachment */
    DELETEALL_ATTACHMENTS(1 shl 11),    /**< Delete all attachments */
    CREATE_MAP(1 shl 12),
    CHANGE_MAP(1 shl 13),
    CREATE_LAYER(1 shl 14),
    DELETE_LAYER(1 shl 15),
    CHANGE_LAYER(1 shl 16),
    TOKEN_EXPIRED(1 shl 17),
    TOKEN_CHANGED(1 shl 18),
    ALL(524286); //(CREATE_OBJECT or DELETE_OBJECT or CHANGE_OBJECT or CREATE_FEATURE or CHANGE_FEATURE or DELETE_FEATURE or DELETEALL_FEATURES or CREATE_ATTACHMENT or CHANGE_ATTACHMENT or DELETE_ATTACHMENT or DELETEALL_ATTACHMENTS or CREATE_MAP or CHANGE_MAP or CREATE_LAYER or DELETE_LAYER or CHANGE_LAYER or TOKEN_EXPIRED or TOKEN_CHANGED)

    companion object {
        fun from(value: Int): ChangeCode {
            for (code in values()) {
                if (code.code == value) {
                    return code
                }
            }
            return NOP
        }
    }
}

/**
 * Edit operation for logging properties.
 *
 * @property fid Edited feature identifier.
 * @property aid Edited feature attachment identifier.
 * @property rid Edited feature remote identifier.
 * @property arid Edited feature attachment remote identifier.
 * @property operationType Edit operation type.
 */
data class EditOperation(val fid: Long, val aid: Long, val rid: Long, val arid: Long, val operationType: ChangeCode) {
    constructor(fid: Long, aid: Long, rid: Long, arid: Long, operationTypeCode: Int) :
            this(fid, aid, rid, arid, ChangeCode.from(operationTypeCode))
}

/**
 * Spatial referenced raster or image.
 *
 * @param copyFrom Origin object to copy properties.
 */
class Raster(copyFrom: Object): Object(copyFrom) {

    var isOpened: Boolean
        get() = API.datasetIsOpenedInt(handle)
        set(value) {
            if(value) {
                API.datasetOpenInt(handle, 96)
            }
            else {
                API.datasetCloseInt(handle)
            }
        }

    /**
     * Cache tiles for some area for TMS datasource.
     *
     * @param bbox Area to cache.
     * @param zoomLevels Zoom levels to cache.
     * @param callback Callback function which executes periodically indicating progress.
     * @return True on success.
     */
    fun cacheArea(bbox: Envelope, zoomLevels: List<Int>,
                  callback: ((status: StatusCode, complete: Double, message: String) -> Boolean)? = null) : Boolean {
        var zoomLevelsValue = ""
        for(zoomLevel in zoomLevels) {
            if(!zoomLevelsValue.isEmpty()) {
                zoomLevelsValue += ","
            }
            zoomLevelsValue += zoomLevel.toString()
        }
        val options = mutableMapOf(
                "MINX" to bbox.minX.toString(),
                "MINY" to bbox.minY.toString(),
                "MAXX" to bbox.maxX.toString(),
                "MAXY" to bbox.maxY.toString(),
                "ZOOM_LEVELS" to zoomLevelsValue
        )

        return API.rasterCacheArea(handle, toArrayOfCStrings(options), callback)
    }
}

/**
 * Non spatial table.
 *
 * @param copyFrom Origin object to copy properties.
 */
open class Table(copyFrom: Object): Object(copyFrom) {

    /**
     * Fields array
     */
    val fields: Array<Field> = API.featureClassFieldsInt(handle)

    private var batchModeValue = false

    /**
     * Enable/disable batch mode property. The sqlite journal will be switch on/off.
     */
    var batchMode: Boolean
        get() = batchModeValue
        set(enable) {
            API.featureClassBatchModeInt(handle, enable)
            batchModeValue = enable
        }

    /**
     * Feature/row count readonly property.
     */
    val count: Long get() = API.featureClassCountInt(handle)

    /**
     * Create new feature/row in memory.
     *
     * @return New feature class instance or null.
     */
    fun createFeature() : Feature? {
        val featureHandle = API.featureClassCreateFeatureInt(handle)
        if( featureHandle != 0L ) {
            return Feature(featureHandle, this)
        }
        return null
    }

    /**
     * Insert feature into table.
     *
     * @param feature Feature/row to insert.
     * @param logEdits Log edits in history table. This log can be received using editOperations function.
     * @return True on success.
     */
    fun insertFeature(feature: Feature, logEdits: Boolean = true) : Boolean {
        return API.featureClassInsertFeatureInt(handle, feature.handle, logEdits)
    }

    /**
     * Update feature/row.
     *
     * @param feature Feature/row to update.
     * @param logEdits Log edits in history table. This log can be received using editOperations function.
     * @return True on success.
     */
    fun updateFeature(feature: Feature, logEdits: Boolean = true) : Boolean {
        return API.featureClassUpdateFeatureInt(handle, feature.handle, logEdits)
    }

    /**
     * Delete feature/row.
     *
     * @param id Feature/row identifier.
     * @param logEdits Log edits in history table. This log can be received using editOperations function.
     * @return True on success.
     */
    fun deleteFeature(id: Long, logEdits: Boolean = true) : Boolean {
        return API.featureClassDeleteFeatureInt(handle, id, logEdits)
    }

    /**
     * Delete feature/row.
     *
     * @param feature Feature/row to delete.
     * @param logEdits Log edits in history table. This log can be received using editOperations function.
     * @return True on success.
     */
    fun deleteFeature(feature: Feature, logEdits: Boolean = true) : Boolean {
        return API.featureClassDeleteFeatureInt(handle, feature.id, logEdits)
    }

    /**
     * Delete all features/rows in table.
     *
     * @param logEdits Log edits in history table. This log can be received using editOperations function.
     * @return True on success.
     */
    fun deleteFeatures(logEdits: Boolean = true) : Boolean {
        return API.featureClassDeleteFeaturesInt(handle, logEdits)
    }

    /**
     * Reset reading features/rows.
     */
    fun reset() {
        API.featureClassResetReadingInt(handle)
    }

    /**
     * Get next feature/row.
     *
     * @return Feature class instance or null.
     */
    fun nextFeature() : Feature? {
        val handle = API.featureClassNextFeatureInt(handle)
        if(handle != 0L) {
            return Feature(handle, this)
        }
        return null
    }

    /**
     * Get feature/row by identifier.
     *
     * @param index Feature/row
     * @return Feature class instance or null.
     */
    fun getFeature(index: Long) : Feature? {
        val handle = API.featureClassGetFeatureInt(handle, index)
        if(handle != 0L) {
            return Feature(handle, this)
        }
        return null
    }

    /**
     * Get feature/row by remote identifier.
     *
     * @param id remote identifier.
     * @return Feature class instance or null.
     */
    fun getFeatureByRemote(id: Long) : Feature? {
        val handle = API.storeFeatureClassGetFeatureByRemoteIdInt(handle, id)
        if(handle != 0L) {
            return Feature(handle, this)
        }
        return null
    }

    /**
     * Search field index and type by field name.
     *
     * @param name Field name.
     * @return Pair with index and type. If field is not exists the index will be negative and field type will be UNKNOWN
     */
    fun fieldIndexAndType(name: String) : Pair<Int, Field.Type> {
        for((count, field) in fields.withIndex()) {
            if( field.name == name) {
                return Pair(count, field.type)
            }
        }
        return Pair(-1, Field.Type.UNKNOWN)
    }

    /**
     * Get edit operations log.
     *
     * @return EditOperation class array. It may be empty.
     */
    fun editOperations() : Array<EditOperation> {
        return API.featureClassGetEditOperationsInt(handle)
    }

    /**
     * Delete edit operation from log.
     *
     * @param editOperation EditOperation to delete.
     */
    fun delete(editOperation: EditOperation) {
        API.featureClassDeleteEditOperation(handle, editOperation)
    }
}

/**
 * Spatial table.
 *
 * @param copyFrom Origin object to copy properties.
 */
class FeatureClass(copyFrom: Object): Table(copyFrom) {
    /**
     * Geometry type of feature class.
     */
    val geometryType: Geometry.Type = Geometry.Type.from(API.featureClassGeometryTypeInt(handle))

    /**
     * Create vector overviews to speedup drawing. This is a synchronous method.
     *
     * @param force If true the previous overviews will be deleted.
     * @param zoomLevels The list of zoom levels to generate.
     * @param callback Callback function to show process and cancel creation if needed.
     * @return True on success.
     */
    fun createOverviews(force: Boolean, zoomLevels: List<Int>,
                        callback: ((status: StatusCode, complete: Double, message: String) -> Boolean)? = null) : Boolean {

        printMessage("create overviews: $zoomLevels")

        var zoomLevelsValue = ""
        for(zoomLevel in zoomLevels) {
            if(!zoomLevelsValue.isEmpty()) {
                zoomLevelsValue += ","
            }
            zoomLevelsValue += zoomLevel.toString()
        }
        val options = mapOf(
                "FORCE" to if(force) "ON" else "OFF",
                "ZOOM_LEVELS" to zoomLevelsValue
        )
        return API.featureClassCreateOverviews(handle, toArrayOfCStrings(options), callback)
    }

    /**
     * Clear any filters set on feature class.
     *
     * @return True on success.
     */
    fun clearFilters() : Boolean {
        return API.featureClassSetFilterInt(handle, 0, "")
    }

    /**
     * Set spatial filter.
     *
     * @param envelope Features intesect with envelope will be returned via nextFeature.
     * @return True on success.
     */
    fun setSpatialFilter(envelope: Envelope) : Boolean {
        return API.featureClassSetSpatialFilterInt(handle, envelope.minX, envelope.minY,
                envelope.maxX, envelope.maxY)
    }

    /**
     * Set spatial filter.
     *
     * @param geometry Features intesect with geometry will be returned via nextFeature.
     * @return True on success.
     */
    fun setSpatialFilter(geometry: Geometry) : Boolean {
        return API.featureClassSetFilterInt(handle, geometry.handle, "")
    }

    /**
     * Set attribute filter.
     *
     * @param query SQL WHERE clause.
     * @return True on success.
     */
    fun setAttributeFilter(query: String) : Boolean {
        return API.featureClassSetFilterInt(handle, 0, query)
    }

    /**
     * Set spatial and attribute filtes.
     *
     * @param geometry Features intersect with geometry will return via nextFeature.
     * @param query SQL WHERE clause.
     * @return True on success.
     */
     fun setFilters(geometry: Geometry, query: String) : Boolean {
        return  API.featureClassSetFilterInt(handle, geometry.handle, query)
    }
}

/**
 * FeatureClass/Table filed class.
 *
 * @property name Field name.
 * @property alias Field alias.
 * @property type Field type.
 * @property defaultValue Field default value.
 */
class Field(val name: String, val alias: String, val type: Type, val defaultValue: String? = null) {

    constructor(name: String, alias: String, nativeType: Int, defaultValue: String? = null) :
            this(name, alias, Type.from(nativeType), defaultValue)

    /**
     * Field type enumerator.
     */
    enum class Type(val code: Int) {
        UNKNOWN(-1),    /**< Unknown type. */
        INTEGER(0),     /**< Integer type. */
        REAL(2),        /**< Real type. */
        STRING(4),      /**< String type. */
        DATE(11);       /**< Date/time type. */

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

    companion object {
        /**
         * Field type name string.
         *
         * @param fieldType Field type.
         * @return Name string.
         */
        fun fieldTypeToName(fieldType: Type) : String {
            return when(fieldType) {
                Type.INTEGER -> "INTEGER"
                Type.REAL -> "REAL"
                Type.STRING -> "STRING"
                Type.DATE -> "DATE_TIME"
                else -> { "STRING" }
            }
        }
    }
}
