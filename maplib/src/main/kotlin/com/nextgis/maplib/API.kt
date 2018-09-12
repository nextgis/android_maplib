/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 14.08.18 20:28.
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

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class StatusCode(val code: Int) {
    UNKNOWN(0),
    CONTINUE(100),           /**< Continue */
    PENDING(101),            /**< Pending */
    IN_PROCESS(102),         /**< In process */
    SUCCESS(200),            /**< Success */
    CANCELED(201),           /**< Canceled */
    FINISHED(202),           /**< Finished */
    WARNING(300),            /**< Warning, not error */
    UNEXPECTED_ERROR(400),   /**< Unexpected error */
    NOT_SPECIFIED(401),      /**< Path, value, etc. is not specified */
    INVALID(402),            /**< Path, map, structure, etc. is invalid */
    UNSUPPORTED(403),        /**< The feature is unsupported */
    CREATE_FAILED(404),      /**< Create failed */
    DELETE_FAILED(405),      /**< Failed to delete file, folder or something else */
    SAVE_FAILED(406),        /**< Failed to save file, folder or something else */
    SET_FAILED(407),         /**< Failed to set value */
    GET_FAILED(408),         /**< Failed to get value */
    OPEN_FAILED(409),        /**< Failed to open file, folder or something else */
    INSERT_FAILED(410),      /**< Insert new feature failed */
    UPDATE_FAILED(411),      /**< Update feature failed */
    INIT_FAILED(412),        /**< Initialise failed */
    COPY_FAILED(413),        /**< Copy failed */
    MOVE_FAILED(414),        /**< Move failed */
    CLOSE_FAILED(415),       /**< Close failed */
    LOAD_FAILED(416),        /**< Load failed */
    RENAME_FAILED(417),      /**< Rename failed */
    DRAW_FAILED(418),        /**< Draw failed */
    REQUEST_FAILED(419);     /**< URL Request failed */

    companion object {
        fun from(value: Int): StatusCode {
            for (code in values()) {
                if (code.code == value) {
                    return code
                }
            }
            return UNKNOWN
        }
    }
}

private data class NotifyFunction(val callback : (uri: String, code: ChangeCode) -> Unit, val code: Int)
/**
 * If return false, execution stops
 */
private data class ProgressFunction(val callback: (status: StatusCode, complete: Double, message: String) -> Boolean)

object API {

    private var isInit = false
    private var catalog: Catalog? = null
    private var cacheDir: String = ""
    private var notifyFunctions: MutableList<NotifyFunction> = mutableListOf()
    private var progressFunctions: MutableMap<Int, ProgressFunction> = mutableMapOf()

    private var mapsDir: Object? = null
    private var geodataDir: Object? = null
    private var authArray: MutableList<Auth> = mutableListOf()
//    private var mapViewArray: Set<MapView>

    // Use to load the 'ngstore' library on application startup.
    init {
        System.loadLibrary("ngstore")
    }

    @Suppress("NON_EXHAUSTIVE_WHEN")
    private fun notifyFunction(uri: String, code: ChangeCode) {
        when (code) {
            ChangeCode.TOKEN_EXPIRED -> API.onAuthNotify(uri)
            ChangeCode.CREATE_FEATURE, ChangeCode.CHANGE_FEATURE, ChangeCode.DELETE_FEATURE, ChangeCode.DELETEALL_FEATURES -> API.onMapViewNotify(uri, code)
        }
    }

    @Suppress("unused")
    @JvmStatic
    private fun notifyBridgeFunction(uri: String, code: Int) {
        for ((ncallback, ncode) in notifyFunctions) {
            if (ncode and code == 0) {
                ncallback.invoke(uri, ChangeCode.from(code))
            }
        }
    }

    @Suppress("unused")
    @JvmStatic
    private fun progressBridgeFunction(status: Int, progress: Double, message: String, key: Int) : Int {
        val function = progressFunctions[key]
        if(function != null) {
            return if(function.callback.invoke(StatusCode.from(status), progress, message)) 1 else 0
        }
        return 1
    }

    fun init(context: Context) {

        val url = "http://www.google.com/"
        val obj = URL(url)

        with(obj.openConnection() as HttpURLConnection) {
            // optional default is GET
            requestMethod = "GET"


            println("\nSending 'GET' request to URL : $url")
            println("Response Code : $responseCode")
        }

        // Prevent multiple launches
        if (isInit) {
            return
        }
        isInit = true

        val homeDir = context.filesDir.parentFile.absolutePath
        val settingsDir = File(context.filesDir, "settings/ngstore").absolutePath
        val sharedPref = context.getSharedPreferences("ngstore", Context.MODE_PRIVATE)
        cacheDir = sharedPref.getString("cache_dir", context.cacheDir.absolutePath)

        // Get library mandatory directories.
        val assetManager = context.assets;
        val gdalDataDir = File(context.filesDir, "data/gdal")
        if (!gdalDataDir.exists()) {
            gdalDataDir.mkdirs()
            try {
                // Extract files from raw.
                val gdalFiles = assetManager.list("gdal")
                for(gdalFile in gdalFiles) {
                    val inStream = assetManager.open("gdal/$gdalFile")
                    copyFrom(inStream, File(gdalDataDir, gdalFile))
                    inStream.close()
                }
            }
            catch (e: IOException) {
                printError(e.localizedMessage)
            }
        }
        val certDataDir = File(context.filesDir, "data/certs")
        val certFile = File(certDataDir, "cert.pem")
        if (!certDataDir.exists()) {
            certDataDir.mkdirs()
            // Extract files from raw.
            try {
                val inStream = assetManager.open("certs/cert.pem")
                copyFrom(inStream, certFile)
                inStream.close()
            }
            catch (e: IOException) {
                printError(e.localizedMessage)
            }
        }

        val options = mapOf(
                "HOME" to homeDir,
                "GDAL_DATA" to gdalDataDir.absolutePath,
                "CACHE_DIR" to cacheDir,
                "SETTINGS_DIR" to settingsDir,
                "SSL_CERT_FILE" to certFile.absolutePath,
                "NUM_THREADS" to "ALL_CPUS", // NOTE: "4"
                "DEBUG_MODE" to if (Constants.debugMode) "ON" else "OFF"
        )

        if (!init(toArrayOfCStrings(options))) {
            printError("Init ngstore failed: " + getLastErrorMessage())
            return
        }

        printMessage("\n home dir: $homeDir\n settings: $settingsDir\n cache dir: $cacheDir\n GDAL data dir: $gdalDataDir")

        catalog = Catalog(catalogObjectGet("ngc://"))

        val libDirCatalogPath = sharedPref.getString("files_dir", "ngc://Local connections/Home")
        val libDir = catalog?.childByPath(libDirCatalogPath)
        if(libDir == null) {
            printError("Application directory not found")
            return
        }

        val appSupportDir = Catalog.getOrCreateFolder(libDir, "files")
        if (appSupportDir == null) {
            printError("Application files directory not found")
            return
        }

        val ngstoreDir = Catalog.getOrCreateFolder(appSupportDir, "ngstore")
        if (ngstoreDir == null) {
            printError("ngstore directory not found")
            return
        }

        mapsDir = Catalog.getOrCreateFolder(ngstoreDir, "maps")
        geodataDir = Catalog.getOrCreateFolder(ngstoreDir, "geodata")

        addNotifyFunction(ChangeCode.ALL, API::notifyFunction)
    }

    private fun finalize() {
        unInit()
    }

    /**
     * Returns library version as number
     *
     * @param component May be self, gdal, sqlite, tiff, jpeg, png, jsonc, proj, geotiff, expat, iconv, zlib, openssl
     *
     * @return version number
     */
    fun version(component: String = ""): Int {
        return getVersion(component)
    }

    /**
     * Returns library version as string
     *
     * @param component May be self, gdal, sqlite, tiff, jpeg, png, jsonc, proj, geotiff, expat, iconv, zlib, openssl
     *
     * @return version string
     */
    fun versionString(component: String = ""): String {
        return getVersionString(component)
    }

    /**
     * Returns last error message
     *
     * @return message string
     */
    fun lastError() : String {
        return getLastErrorMessage()
    }

    /**
     * Get library property
     *
     * @param key: key value
     * @param value: default value if not exists
     *
     * @return property value corespondent to key
     */
    fun getProperty(key: String, defaultValue: String) : String {
        return settingsGetString(key, defaultValue)
    }

    /**
     * Set library property
     *
     * @param key: key value
     * @param value: value to set
     */
    fun setProperty(key: String, value: String) {
        settingsSetString(key, value)
    }

    /**
     * Add authorization to use in HTTP requests and automatically update access tokens if needed
     */
    fun addAuth(auth: Auth) : Boolean {
        if(URLAuthAdd(auth.getURL(), toArrayOfCStrings(auth.options()))) {
            authArray.add(auth)
            return true
        }
        return false
    }

    /**
     * Remove authorization
     */
    fun removeAuth(auth: Auth) {
        if(URLAuthDelete(auth.getURL())) {
            authArray.remove(auth)
        }
    }

    private fun onAuthNotify(url: String) {
        authArray.forEach { auth ->
            auth.onRefreshTokenFailed(url)
        }
    }

    /**
     * Get catalog class instance. The catalog object is singleton.
     *
     * @return Catalog class instance
     */
    fun getCatalog() : Catalog? {
        return catalog
    }

    /**
     * Get NextGIS store catalog object. The NextGIS store is geopackage file with some additions needed for library.
     *
     * @param name: File name. If file name extension is not set it will append.
     *
     * @return Catalog object instance or null
     */
    fun getStore(name: String) : Store? {
        if (geodataDir == null) {
            printError("GeoData dir undefined. Cannot find store.")
            return null
        }

        var newName = name
        if(!newName.endsWith(Store.ext)) {
            newName += Store.ext
        }

        val storePath = geodataDir!!.path + Catalog.separator + newName
        var store = geodataDir?.child(newName)
        if(store == null) {
            printWarning("Store $storePath is not exists. Create it.")
            val options = mutableMapOf(
                "TYPE" to Object.ObjectType.CONTAINER_NGS.toString(),
                "CREATE_UNIQUE" to "OFF"
            )
            store = geodataDir?.create(newName, options)
        }

        if(store == null) {
            printError("Store is null")
            return null
        }

        return Store(store)
    }

    /**
     * Get library data directory. Directory to store various data include maps, files, etc.
     *
     * @return Catalog object instance or null
     */
    fun getDataDirectory() : Object? {
        return geodataDir
    }

    /**
     * Get library temp directory.
     *
     * @return Catalog object instance or null
     */
    fun getTmpDirectory() : Object? {
        return catalog?.childByPath(catalogPathFromSystem(cacheDir)) //(Constants.tmpDirCatalogPath)
    }

//    /**
//     * Get library documents directory
//     *
//     * @return Catalog object instance or null
//     */
//    fun getDocDirectory() : Object? {
//        return catalog?.childByPath(Constants.docDirCatalogPath)
//    }

//    func addMapView(_ view: MapView) {
//        mapViewArray.insert(view)
//    }
//
//    func removeMapView(_ view: MapView) {
//        mapViewArray.remove(view)
//    }

    private fun onMapViewNotify(url: String, code: ChangeCode) {
//        if url.hasPrefix(Constants.tmpDirCatalogPath) {
//            return
//        }
//
//        let path = url.components(separatedBy: "#")
//
//        printMessage("onMapViewNotify: \(path)")
//
//        if path.count == 2 && code == CC_CREATE_FEATURE { // NOTE: We dont know the last feature envelope so for change/delete - update all view
//            let fid = Int64(path[1])
//            if let object = getCatalog().childByPath(path: path[0]) {
//                if let fc = Object.forceChildTo(featureClass: object) {
//                if let feautre = fc.getFeature(index: fid!) {
//                let env = feautre.geometry?.envelope ??
//                Envelope(minX: -0.5, minY: -0.5, maxX: 0.5, maxY: 0.5)
//                for view in mapViewArray {
//                    view.invalidate(envelope: env)
//                    view.scheduleDraw(drawState: .PRESERVED)
//                }
//            }
//            }
//            }
//        }
//        else {
//            for view in mapViewArray {
//                view.scheduleDraw(drawState: .REFILL)
//            }
//        }
    }

    fun addNotifyFunction(code: ChangeCode, callback: (uri: String, code: ChangeCode) -> Unit) {
        val function = NotifyFunction(callback, code.code)
        if (notifyFunctions.indexOf(function) == -1) {
            notifyFunctions.add(NotifyFunction(callback, code.code))
        }
    }

    fun removeNotifyFunction(callback: (uri: String, code: ChangeCode) -> Unit) {
        for ((index, function) in notifyFunctions.withIndex()) {
            if (function.callback == callback) {
                notifyFunctions.removeAt(index)
                return
            }
        }
    }

    internal fun catalogObjectCopy(source: Long, destination: Long, options: Array<String>,
                                   callback: ((status: StatusCode, complete: Double,
                                               message: String) -> Boolean)? = null) : Boolean {
        if(null != callback) {
            progressFunctions[callback.hashCode()] = ProgressFunction(callback)
        }

        val res = catalogObjectCopy(source, destination, options, callback?.hashCode() ?: 0)

        if(null != callback) {
            progressFunctions.remove(callback.hashCode())
        }
        return res
    }

    internal fun rasterCacheArea(handle: Long, options: Array<String>,
                                 callback: ((status: StatusCode, complete: Double, message: String) -> Boolean)? = null) : Boolean
    {
        if(null != callback) {
            progressFunctions[callback.hashCode()] = ProgressFunction(callback)
        }

        val res = rasterCacheArea(handle, options, callback?.hashCode() ?: 0)

        if(null != callback) {
            progressFunctions.remove(callback.hashCode())
        }
        return res
    }

    internal fun featureClassCreateOverviews(handle: Long, options: Array<String>,
                                 callback: ((status: StatusCode, complete: Double, message: String) -> Boolean)? = null) : Boolean
    {
        if(null != callback) {
            progressFunctions[callback.hashCode()] = ProgressFunction(callback)
        }

        val res = featureClassCreateOverviews(handle, options, callback?.hashCode() ?: 0)

        if(null != callback) {
            progressFunctions.remove(callback.hashCode())
        }
        return res
    }

    internal fun featureClassDeleteEditOperation(handle: Long, operation: EditOperation) : Boolean
    {
        return featureClassDeleteEditOperation(handle, operation.fid, operation.aid, operation.operationType.code, operation.rid, operation.arid)
    }

    internal fun jsonDocumentLoadUrl(handle: Long, url: String, options: Array<String>,
                                   callback: ((status: StatusCode, complete: Double,
                                               message: String) -> Boolean)? = null) : Boolean {
        if(null != callback) {
            progressFunctions[callback.hashCode()] = ProgressFunction(callback)
        }

        val res = jsonDocumentLoadUrl(handle, url, options, callback?.hashCode() ?: 0)

        if(null != callback) {
            progressFunctions.remove(callback.hashCode())
        }
        return res
    }

    internal fun URLUploadFile(path: String, url: String, options: Array<String>,
                               callback: ((status: StatusCode, complete: Double,
                                                 message: String) -> Boolean)? = null) : RequestResultJsonInt {
        if(null != callback) {
            progressFunctions[callback.hashCode()] = ProgressFunction(callback)
        }

        val res = URLUploadFile(path, url, options, callback?.hashCode() ?: 0)

        if(null != callback) {
            progressFunctions.remove(callback.hashCode())
        }
        return res
    }

    internal fun URLAuthGetMap(uri: String) : Map<String, String> {
        val properties = API.URLAuthGet(uri)
        val out: MutableMap<String, String> = mutableMapOf()
        for(property in properties) {
            val parts = property.split("=")
            if(parts.size > 1) {
                out[parts[0]] = parts[1]
            }
        }
        return out
    }

    internal fun URLRequest(type: Int, url: String, options: Map<String, String> = mapOf()) : RequestResult = URLRequest(type, url, toArrayOfCStrings(options))

    internal fun URLRequestJson(type: Int, url: String, options: Map<String, String> = mapOf()) : RequestResultJson {
        val result = API.URLRequestJson(type, url, toArrayOfCStrings(options))
        return RequestResultJson(result.status, JsonObject(result.value))
    }

    internal fun URLRequestRaw(type: Int, url: String, options: Map<String, String> = mapOf()) : RequestResultRaw = URLRequestRaw(type, url, toArrayOfCStrings(options))


    internal fun jsonDocumentCreateInt() : Long = jsonDocumentCreate()
    internal fun jsonDocumentFreeInt(handle: Long) = jsonDocumentFree(handle)
    internal fun jsonDocumentRootInt(handle: Long) : Long = jsonDocumentRoot(handle)
    internal fun jsonObjectFreeInt(handle: Long) = jsonObjectFree(handle)
    internal fun jsonObjectTypeInt(handle: Long) : Int = jsonObjectType(handle)
    internal fun jsonObjectValidInt(handle: Long) : Boolean = jsonObjectValid(handle)
    internal fun jsonObjectNameInt(handle: Long) : String = jsonObjectName(handle)
    internal fun jsonObjectChildrenInt(handle: Long) : LongArray = jsonObjectChildren(handle)
    internal fun jsonObjectGetStringInt(handle: Long, default: String) : String = jsonObjectGetString(handle, default)
    internal fun jsonObjectGetDoubleInt(handle: Long, default: Double) : Double = jsonObjectGetDouble(handle, default)
    internal fun jsonObjectGetIntegerInt(handle: Long, default: Int) : Int = jsonObjectGetInteger(handle, default)
    internal fun jsonObjectGetLongInt(handle: Long, default: Long) : Long = jsonObjectGetLong(handle, default)
    internal fun jsonObjectGetBoolInt(handle: Long, default: Boolean) : Boolean = jsonObjectGetBool(handle, default)
    internal fun jsonObjectGetArrayInt(handle: Long, name: String) : Long = jsonObjectGetArray(handle, name)
    internal fun jsonObjectGetObjectInt(handle: Long, name: String) : Long = jsonObjectGetObject(handle, name)
    internal fun jsonArraySizeInt(handle: Long) : Int = jsonArraySize(handle)
    internal fun jsonArrayItemInt(handle: Long, index: Int) : Long = jsonArrayItem(handle, index)
    internal fun jsonObjectGetStringForKeyInt(handle: Long, name: String, default: String) : String = jsonObjectGetStringForKey(handle, name, default)
    internal fun jsonObjectGetDoubleForKeyInt(handle: Long, name: String, default: Double) : Double = jsonObjectGetDoubleForKey(handle, name, default)
    internal fun jsonObjectGetIntegerForKeyInt(handle: Long, name: String, default: Int) : Int = jsonObjectGetIntegerForKey(handle, name, default)
    internal fun jsonObjectGetLongForKeyInt(handle: Long, name: String, default: Long) : Long = jsonObjectGetLongForKey(handle, name, default)
    internal fun jsonObjectGetBoolForKeyInt(handle: Long, name: String, default: Boolean) : Boolean = jsonObjectGetBoolForKey(handle, name, default)
    internal fun jsonObjectSetStringForKeyInt(handle: Long, name: String, value: String) : Boolean = jsonObjectSetStringForKey(handle, name, value)
    internal fun jsonObjectSetDoubleForKeyInt(handle: Long, name: String, value: Double) : Boolean = jsonObjectSetDoubleForKey(handle, name, value)
    internal fun jsonObjectSetIntegerForKeyInt(handle: Long, name: String, value: Int) : Boolean = jsonObjectSetIntegerForKey(handle, name, value)
    internal fun jsonObjectSetLongForKeyInt(handle: Long, name: String, value: Long) : Boolean = jsonObjectSetLongForKey(handle, name, value)
    internal fun jsonObjectSetBoolForKeyInt(handle: Long, name: String, value: Boolean) : Boolean = jsonObjectSetBoolForKey(handle, name, value)

    /**
     * Catalog
     */
    internal fun catalogObjectGetInt(path: String): Long = catalogObjectGet(path)
    internal fun catalogObjectQueryInt(handle: Long, filter: Int) : Array<CatalogObjectInfo> = catalogObjectQuery(handle, filter)
    internal fun catalogObjectQueryMultiFilterInt(handle: Long, filters: Array<Int>) : Array<CatalogObjectInfo> = catalogObjectQueryMultiFilter(handle, filters)
    internal fun catalogObjectCreateInt(handle: Long, name: String, options: Array<String>): Boolean = catalogObjectCreate(handle, name, options)
    internal fun catalogObjectDeleteInt(handle: Long): Boolean = catalogObjectDelete(handle)
    internal fun catalogObjectRenameInt(handle: Long, newName: String): Boolean = catalogObjectRename(handle, newName)
    internal fun catalogObjectOptionsInt(handle: Long, optionType: Int): String = catalogObjectOptions(handle, optionType)
    internal fun catalogObjectTypeInt(handle: Long): Int = catalogObjectType(handle)
    internal fun catalogObjectNameInt(handle: Long): String = catalogObjectName(handle)
//    internal fun catalogPathFromSystemInt(path: String): String = catalogPathFromSystem(path)
    internal fun catalogObjectPropertiesInt(handle: Long, domain: String): Array<String> = catalogObjectProperties(handle, domain)
    internal fun catalogObjectSetPropertyInt(handle: Long, name: String, value: String, domain: String): Boolean = catalogObjectSetProperty(handle, name, value, domain)
    internal fun catalogObjectRefreshInt(handle: Long) = catalogObjectRefresh(handle)

    /**
     * Geometry
     */
    internal fun geometryFreeInt(handle: Long) = geometryFree(handle)
    internal fun geometrySetPointInt(handle: Long, point: Int, x: Double, y: Double, z: Double, m: Double) = geometrySetPoint(handle, point, x, y, z, m)
    internal fun geometryTransformToInt(handle: Long, EPSG: Int) : Boolean = geometryTransformTo(handle, EPSG)
    internal fun geometryTransformInt(handle: Long, ctHandle: Long) : Boolean = geometryTransform(handle, ctHandle)
    internal fun geometryIsEmptyInt(handle: Long) : Boolean = geometryIsEmpty(handle)
    internal fun geometryGetTypeInt(handle: Long) : Int = geometryGetType(handle)
    internal fun geometryToJsonInt(handle: Long) : String = geometryToJson(handle)
    internal fun geometryGetEnvelopeInt(handle: Long) : Envelope = geometryGetEnvelope(handle)

    /**
     * Coordinate transformation
     */
    internal fun coordinateTransformationCreateInt(fromEPSG: Int, toEPSG: Int): Long = coordinateTransformationCreate(fromEPSG, toEPSG)
    internal fun coordinateTransformationFreeInt(handle: Long) = coordinateTransformationFree(handle)
    internal fun coordinateTransformationDoInt(handle: Long, x: Double, y: Double): Point = coordinateTransformationDo(handle, x, y)

    /**
     * Feature class
     */
    internal fun datasetOpenInt(handle: Long, openFlags: Int, openOptions: Array<String> = emptyArray()) : Boolean = datasetOpen(handle, openFlags, openOptions)
    internal fun datasetIsOpenedInt(handle: Long) : Boolean = datasetIsOpened(handle)
    internal fun datasetCloseInt(handle: Long) : Boolean = datasetClose(handle)
    internal fun featureClassFieldsInt(handle: Long) : Array<Field> = featureClassFields(handle)
    internal fun featureClassGeometryTypeInt(handle: Long) : Int = featureClassGeometryType(handle)
    internal fun featureClassCreateFeatureInt(handle: Long) : Long = featureClassCreateFeature(handle)
    internal fun featureClassBatchModeInt(handle: Long, enable: Boolean) = featureClassBatchMode(handle, enable)
    internal fun featureClassInsertFeatureInt(handle: Long, feature: Long, logEdits: Boolean) : Boolean = featureClassInsertFeature(handle, feature, logEdits)
    internal fun featureClassUpdateFeatureInt(handle: Long, feature: Long, logEdits: Boolean) : Boolean = featureClassUpdateFeature(handle, feature, logEdits)
    internal fun featureClassDeleteFeatureInt(handle: Long, id: Long, logEdits: Boolean) : Boolean = featureClassDeleteFeature(handle, id, logEdits)
    internal fun featureClassDeleteFeaturesInt(handle: Long, logEdits: Boolean) : Boolean = featureClassDeleteFeatures(handle, logEdits)
    internal fun featureClassCountInt(handle: Long) : Long = featureClassCount(handle)
    internal fun featureClassResetReadingInt(handle: Long) = featureClassResetReading(handle)
    internal fun featureClassNextFeatureInt(handle: Long) : Long = featureClassNextFeature(handle)
    internal fun featureClassGetFeatureInt(handle: Long, id: Long) : Long = featureClassGetFeature(handle, id)
    internal fun featureClassSetFilterInt(handle: Long, geometryFilter: Long, attributeFilter: String) : Boolean = featureClassSetFilter(handle, geometryFilter, attributeFilter)
    internal fun featureClassSetSpatialFilterInt(handle: Long, minX : Double, minY : Double,
                                      maxX : Double, maxY : Double) : Boolean = featureClassSetSpatialFilter(handle, minX, minY, maxX, maxY)
    internal fun featureClassGetEditOperationsInt(handle: Long) : Array<EditOperation> = featureClassGetEditOperations(handle)
    internal fun featureFreeInt(handle: Long) = featureFree(handle)
    internal fun featureFieldCountInt(handle: Long) : Int = featureFieldCount(handle)
    internal fun featureIsFieldSetInt(handle: Long, field: Int) : Boolean = featureIsFieldSet(handle, field)
    internal fun featureGetIdInt(handle: Long) : Long = featureGetId(handle)
    internal fun featureGetGeometryInt(handle: Long) : Long = featureGetGeometry(handle)
    internal fun featureGetFieldAsIntegerInt(handle: Long, field: Int) : Int = featureGetFieldAsInteger(handle, field)
    internal fun featureGetFieldAsDoubleInt(handle: Long, field: Int) : Double = featureGetFieldAsDouble(handle, field)
    internal fun featureGetFieldAsStringInt(handle: Long, field: Int) : String = featureGetFieldAsString(handle, field)
    internal fun featureGetFieldAsDateTimeInt(handle: Long, field: Int) : DateComponents = featureGetFieldAsDateTime(handle, field)
    internal fun featureSetGeometryInt(handle: Long, geometry: Long) = featureSetGeometry(handle, geometry)
    internal fun featureSetFieldIntegerInt(handle: Long, field: Int, value: Int) = featureSetFieldInteger(handle, field, value)
    internal fun featureSetFieldDoubleInt(handle: Long, field: Int, value: Double) = featureSetFieldDouble(handle, field, value)
    internal fun featureSetFieldStringInt(handle: Long, field: Int, value: String) = featureSetFieldString(handle, field, value)
    internal fun featureSetFieldDateTimeInt(handle: Long, field: Int, year: Int, month: Int,
                                 day: Int, hour: Int, minute: Int, second: Int) = featureSetFieldDateTime(handle, field, year, month, day, hour, minute, second)
    internal fun storeFeatureClassGetFeatureByRemoteIdInt(handle: Long, rid: Long) : Long = storeFeatureClassGetFeatureByRemoteId(handle, rid)
    internal fun storeFeatureGetRemoteIdInt(handle: Long) : Long = storeFeatureGetRemoteId(handle)
    internal fun storeFeatureSetRemoteIdInt(handle: Long, rid: Long) = storeFeatureSetRemoteId(handle, rid)
    internal fun featureCreateGeometryInt(handle: Long) : Long = featureCreateGeometry(handle)
    internal fun featureCreateGeometryFromJsonInt(geometry: Long) : Long = featureCreateGeometryFromJson(geometry)
    internal fun featureAttachmentAddInt(feature: Long, name: String, description: String,
                              path: String, options: Array<String>, logEdits: Boolean) : Long = featureAttachmentAdd(feature, name, description, path, options, logEdits)
    internal fun featureAttachmentDeleteInt(feature: Long, aid: Long, logEdits: Boolean) : Boolean = featureAttachmentDelete(feature, aid, logEdits)
    internal fun featureAttachmentDeleteAllInt(feature: Long, logEdits: Boolean) : Boolean = featureAttachmentDeleteAll(feature, logEdits)
    internal fun featureAttachmentsGetInt(feature: Long) : Array<Attachment> = featureAttachmentsGet(feature)
    internal fun featureAttachmentUpdateInt(feature: Long, aid: Long, name: String,
                                  description: String, logEdits: Boolean) : Boolean = featureAttachmentUpdate(feature, aid, name, description, logEdits)
    internal fun storeFeatureSetAttachmentRemoteIdInt(feature: Long, aid: Long, rid: Long) = storeFeatureSetAttachmentRemoteId(feature, aid, rid)

    /**
     * A native method that is implemented by the 'ngstore' native library,
     * which is packaged with this application.
     */
    private external fun getVersion(request: String): Int
    private external fun getVersionString(request: String): String
    private external fun unInit()
    private external fun getLastErrorMessage() : String
    private external fun settingsGetString(key: String, default: String): String
    private external fun settingsSetString(key: String, value: String)

    /**
     * Free library resources. On this call catalog removes all preloaded tree items.
     * The map storage closes and removes all maps
     *
     * @param full: If true catalog and map storage will be freed, otherwise only map storage
     */
    private external fun freeResources(full: Boolean)
    private external fun init(options: Array<String>): Boolean

    /**
     * GDAL
     */
    external fun getCurrentDirectory(): String
    private external fun formFileName(path: String, name: String, extension: String): String
    private external fun free(pointer: Long)

    /**
     * Miscellaneous functions
     */
    private external fun URLRequest(type: Int, url: String, options: Array<String>) : RequestResult
    private external fun URLRequestJson(type: Int, url: String, options: Array<String>) : RequestResultJsonInt
    private external fun URLRequestRaw(type: Int, url: String, options: Array<String>) : RequestResultRaw
    private external fun URLUploadFile(path: String, url: String, options: Array<String>, callbackId: Int) : RequestResultJsonInt
    private external fun URLAuthAdd(url: String, options: Array<String>) : Boolean
    private external fun URLAuthGet(uri: String) : Array<String>
    private external fun URLAuthDelete(uri: String) : Boolean

    /**
     * Create MD5 hash from text
     *
     * @param value: text to create MD5 hash
     *
     * @return MD5 hash string created from text
     */
    external fun md5(value: String) : String
    private external fun jsonDocumentCreate() : Long
    private external fun jsonDocumentFree(handle: Long)
    private external fun jsonDocumentLoadUrl(handle: Long, url: String, options: Array<String>, callbackId: Int) : Boolean
    private external fun jsonDocumentRoot(handle: Long) : Long
    private external fun jsonObjectFree(handle: Long)
    private external fun jsonObjectType(handle: Long) : Int
    private external fun jsonObjectValid(handle: Long) : Boolean
    private external fun jsonObjectName(handle: Long) : String
    private external fun jsonObjectChildren(handle: Long) : LongArray
    private external fun jsonObjectGetString(handle: Long, default: String) : String
    private external fun jsonObjectGetDouble(handle: Long, default: Double) : Double
    private external fun jsonObjectGetInteger(handle: Long, default: Int) : Int
    private external fun jsonObjectGetLong(handle: Long, default: Long) : Long
    private external fun jsonObjectGetBool(handle: Long, default: Boolean) : Boolean
    private external fun jsonObjectGetArray(handle: Long, name: String) : Long
    private external fun jsonObjectGetObject(handle: Long, name: String) : Long
    private external fun jsonArraySize(handle: Long) : Int
    private external fun jsonArrayItem(handle: Long, index: Int) : Long
    private external fun jsonObjectGetStringForKey(handle: Long, name: String, default: String) : String
    private external fun jsonObjectGetDoubleForKey(handle: Long, name: String, default: Double) : Double
    private external fun jsonObjectGetIntegerForKey(handle: Long, name: String, default: Int) : Int
    private external fun jsonObjectGetLongForKey(handle: Long, name: String, default: Long) : Long
    private external fun jsonObjectGetBoolForKey(handle: Long, name: String, default: Boolean) : Boolean
    private external fun jsonObjectSetStringForKey(handle: Long, name: String, value: String) : Boolean
    private external fun jsonObjectSetDoubleForKey(handle: Long, name: String, value: Double) : Boolean
    private external fun jsonObjectSetIntegerForKey(handle: Long, name: String, value: Int) : Boolean
    private external fun jsonObjectSetLongForKey(handle: Long, name: String, value: Long) : Boolean
    private external fun jsonObjectSetBoolForKey(handle: Long, name: String, value: Boolean) : Boolean

    /**
     * Catalog
     */
    private external fun catalogPathFromSystem(path: String): String
    private external fun catalogObjectGet(path: String): Long
    private external fun catalogObjectQuery(handle: Long, filter: Int) : Array<CatalogObjectInfo>
    private external fun catalogObjectQueryMultiFilter(handle: Long, filters: Array<Int>) : Array<CatalogObjectInfo>
    private external fun catalogObjectCreate(handle: Long, name: String, options: Array<String>): Boolean
    private external fun catalogObjectDelete(handle: Long): Boolean
    private external fun catalogObjectCopy(srcHandle: Long, dstHandle: Long, options: Array<String>, callbackId: Int) : Boolean
    private external fun catalogObjectRename(handle: Long, newName: String): Boolean
    private external fun catalogObjectOptions(handle: Long, optionType: Int): String
    private external fun catalogObjectType(handle: Long): Int
    private external fun catalogObjectName(handle: Long): String
    private external fun catalogObjectProperties(handle: Long, domain: String): Array<String>
    private external fun catalogObjectSetProperty(handle: Long, name: String, value: String, domain: String): Boolean
    private external fun catalogObjectRefresh(handle: Long)


    /**
     * Geometry
     */
    private external fun geometryFree(handle: Long)
    private external fun geometrySetPoint(handle: Long, point: Int, x: Double, y: Double, z: Double, m: Double)
    private external fun geometryTransformTo(handle: Long, EPSG: Int) : Boolean
    private external fun geometryTransform(handle: Long, ctHandle: Long) : Boolean
    private external fun geometryIsEmpty(handle: Long) : Boolean
    private external fun geometryGetType(handle: Long) : Int
    private external fun geometryToJson(handle: Long) : String
    private external fun geometryGetEnvelope(handle: Long) : Envelope

    /**
     * Coordinate transformation
     */
    private external fun coordinateTransformationCreate(fromEPSG: Int, toEPSG: Int): Long
    private external fun coordinateTransformationFree(handle: Long)
    private external fun coordinateTransformationDo(handle: Long, x: Double, y: Double): Point

    /**
     * Feature class
     */
    private external fun datasetOpen(handle: Long, openFlags: Int, openOptions: Array<String> = emptyArray()) : Boolean
    private external fun datasetIsOpened(handle: Long) : Boolean
    private external fun datasetClose(handle: Long) : Boolean
    private external fun featureClassFields(handle: Long) : Array<Field>
    private external fun featureClassGeometryType(handle: Long) : Int
    private external fun featureClassCreateOverviews(handle: Long, options: Array<String>, callbackId: Int) : Boolean
    private external fun featureClassCreateFeature(handle: Long) : Long
    private external fun featureClassBatchMode(handle: Long, enable: Boolean)
    private external fun featureClassInsertFeature(handle: Long, feature: Long, logEdits: Boolean) : Boolean
    private external fun featureClassUpdateFeature(handle: Long, feature: Long, logEdits: Boolean) : Boolean
    private external fun featureClassDeleteFeature(handle: Long, id: Long, logEdits: Boolean) : Boolean
    private external fun featureClassDeleteFeatures(handle: Long, logEdits: Boolean) : Boolean
    private external fun featureClassCount(handle: Long) : Long
    private external fun featureClassResetReading(handle: Long)
    private external fun featureClassNextFeature(handle: Long) : Long
    private external fun featureClassGetFeature(handle: Long, id: Long) : Long
    private external fun featureClassSetFilter(handle: Long, geometryFilter: Long, attributeFilter: String) : Boolean
    private external fun featureClassSetSpatialFilter(handle: Long, minX : Double, minY : Double,
                                                       maxX : Double, maxY : Double) : Boolean
    private external fun featureClassDeleteEditOperation(handle: Long, fid: Long, aid: Long, code: Int, rid: Long, arid: Long) : Boolean
    private external fun featureClassGetEditOperations(handle: Long) : Array<EditOperation>
    private external fun featureFree(handle: Long)
    private external fun featureFieldCount(handle: Long) : Int
    private external fun featureIsFieldSet(handle: Long, field: Int) : Boolean
    private external fun featureGetId(handle: Long) : Long
    private external fun featureGetGeometry(handle: Long) : Long
    private external fun featureGetFieldAsInteger(handle: Long, field: Int) : Int
    private external fun featureGetFieldAsDouble(handle: Long, field: Int) : Double
    private external fun featureGetFieldAsString(handle: Long, field: Int) : String
    private external fun featureGetFieldAsDateTime(handle: Long, field: Int) : DateComponents
    private external fun featureSetGeometry(handle: Long, geometry: Long)
    private external fun featureSetFieldInteger(handle: Long, field: Int, value: Int)
    private external fun featureSetFieldDouble(handle: Long, field: Int, value: Double)
    private external fun featureSetFieldString(handle: Long, field: Int, value: String)
    private external fun featureSetFieldDateTime(handle: Long, field: Int, year: Int, month: Int,
                                                  day: Int, hour: Int, minute: Int, second: Int)
    private external fun storeFeatureClassGetFeatureByRemoteId(handle: Long, rid: Long) : Long
    private external fun storeFeatureGetRemoteId(handle: Long) : Long
    private external fun storeFeatureSetRemoteId(handle: Long, rid: Long)
    private external fun featureCreateGeometry(handle: Long) : Long
    private external fun featureCreateGeometryFromJson(geometry: Long) : Long

    private external fun featureAttachmentAdd(feature: Long, name: String, description: String,
                                               path: String, options: Array<String>, logEdits: Boolean) : Long
    private external fun featureAttachmentDelete(feature: Long, aid: Long, logEdits: Boolean) : Boolean
    private external fun featureAttachmentDeleteAll(feature: Long, logEdits: Boolean) : Boolean
    private external fun featureAttachmentsGet(feature: Long) : Array<Attachment>
    private external fun featureAttachmentUpdate(feature: Long, aid: Long, name: String,
                                                  description: String, logEdits: Boolean) : Boolean
    private external fun storeFeatureSetAttachmentRemoteId(feature: Long, aid: Long, rid: Long)

    /**
     * Raster
     */
    private external fun rasterCacheArea(handle: Long, options: Array<String>, callbackId: Int) : Boolean
}