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

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.os.Build
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Operation status codes enumeration.
 */
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
    INIT_FAILED(412),        /**< Initialize failed */
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

/**
 * Singleton object. Entry point for library interaction.
 */
object API {

    private var isInit = false
    private var catalog: Catalog? = null
    private var cacheDir: String = ""
    private var notifyFunctions = mutableListOf<NotifyFunction>()
    private var progressFunctions = mutableMapOf<Int, ProgressFunction>()
    private var drawingProgressFuncId = 0

    private var mapsDir: Object? = null
    private var geodataDir: Object? = null
    private var authArray = mutableListOf<Auth>()
    private val mapViewArray = mutableSetOf<MapView>()
    private val mapDefaultOptions = mutableMapOf(
            "ZOOM_INCREMENT" to "0", // Add extra to zoom level corresponding to scale
            "VIEWPORT_REDUCE_FACTOR" to "1.0" // Reduce viewport width and height to decrease memory usage
    )

    /**
     * Use to load the 'ngstore' library on application startup.
     */
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

    /**
     * Initialize library. Should be executed as soon as possible.
     *
     * @param context Context of executed object.
     */
    fun init(context: Context) {
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

        // Form default map options
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        // For low memory devices we create tiles bigger, so we get less tiles and less memory load
        var reduceFactor = 1.0
        val totalRam = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            memoryInfo.totalMem / (1024 * 1024)
        } else {
            512
        }

        if(totalRam < 1024) {
            reduceFactor = 2.0
        }

        mapDefaultOptions["VIEWPORT_REDUCE_FACTOR"] = reduceFactor.toString()
        mapDefaultOptions["ZOOM_INCREMENT"] = "-1"
    }

    private fun finalize() {
        unInit()
    }

    /**
     * Returns library version as number.
     *
     * @param component Component value may be one of the following: self, gdal, sqlite, tiff, jpeg, png, jsonc, proj, geotiff, expat, iconv, zlib, openssl.
     * @return version number.
     */
    fun version(component: String = ""): Int {
        return getVersion(component)
    }

    /**
     * Returns library version as string.
     *
     * @param component Component value may be one of the following: self, gdal, sqlite, tiff, jpeg, png, jsonc, proj, geotiff, expat, iconv, zlib, openssl.
     * @return version string.
     */
    fun versionString(component: String = ""): String {
        return getVersionString(component)
    }

    /**
     * Returns last error message.
     *
     * @return message string.
     */
    fun lastError() : String {
        return getLastErrorMessage()
    }

    /**
     * Get library property.
     *
     * @param key Key value.
     * @param value Default value if not exists.
     *
     * @return property value corespondent to key.
     */
    fun getProperty(key: String, defaultValue: String) : String {
        return settingsGetString(key, defaultValue)
    }

    /**
     * Set library property.
     *
     * @param key Key value.
     * @param value Value to set.
     */
    fun setProperty(key: String, value: String) {
        settingsSetString(key, value)
    }

    /**
     * Add authorization to use in HTTP requests and automatically update access tokens if needed.
     *
     * @param auth Authorization class instance.
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
     *
     * @param auth Authorization class instance to remove.
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
     * @return Catalog class instance.
     */
    fun getCatalog() : Catalog? {
        return catalog
    }

    /**
     * Get map by name.
     *
     * @param name Map file name. If map file name extension is not set it will append.
     * @return MapDocument class instance or null.
     */
    fun getMap(name: String) : MapDocument? {
        if(mapsDir == null) {
            printError("Maps dir undefined. Cannot find map.")
            return null
        }

        val mapPath = mapsDir!!.path + Catalog.separator + name + MapDocument.ext
        var mapId = mapOpenInt(mapPath)
        if(!isMapIdValid(mapId)) {
            printWarning("Map $mapPath is not exists. Create it")
            mapId = mapCreateInt(name, "default map", 3857,
                    -20037508.34, -20037508.34,
                    20037508.34, 20037508.34)
            if(!isMapIdValid(mapId)) {
                printError("Map $name create failed")
                return null
            }
        }
        else {
            printMessage("Get map with ID: $mapId")
        }

        mapSetOptionsInt(mapId, mapDefaultOptions)
        return MapDocument(mapId, mapPath)
    }

    /**
     * Get NextGIS store catalog object. The NextGIS store is geopackage file with some additions needed for library.
     *
     * @param name File name. If file name extension is not set it will append.
     * @return Catalog object instance or null.
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
                "TYPE" to Object.Type.CONTAINER_NGS.toString(),
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
     * @return Catalog object instance or null.
     */
    fun getDataDirectory() : Object? {
        return geodataDir
    }

    /**
     * Get library temp directory.
     *
     * @return Catalog object instance or null.
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

    internal fun addMapView(view: MapView) {
        mapViewArray.add(view)
    }

    internal fun removeMapView(view: MapView) {
        mapViewArray.remove(view)
    }

    private fun onMapViewNotify(url: String, code: ChangeCode) {
        if(url.startsWith(Constants.tmpDirCatalogPath)) {
            return
        }

        val path = url.split("#")

        printMessage("onMapViewNotify: $path")

        if(path.size == 2 && code == ChangeCode.CREATE_FEATURE) { // NOTE: We don't know the last feature envelope so for change/delete - update all view
            val fid = path[1].toLong()
            val objPath = getCatalog()?.childByPath(path[0])
            if(objPath != null) {
                val fc = Object.forceChildToFeatureClass(objPath)
                if(fc != null) {
                    val feature = fc.getFeature(fid)
                    if(feature != null) {
                        val env = feature.geometry?.envelope ?: Envelope(-0.5, -0.5, 0.5, 0.5)
                        for(view in mapViewArray) {
                            view.invalidate(env)
                            view.scheduleDraw(MapDocument.DrawState.PRESERVED)
                        }
                    }
                }
            }
        }
        else {
            for(view in mapViewArray) {
                view.scheduleDraw(MapDocument.DrawState.REFILL)
            }
        }
    }

    /**
     * Add function executed on various events.
     *
     * @param code Event codes or combination on which callback will execute.
     * @param callback Function to execute.
     */
    fun addNotifyFunction(code: ChangeCode, callback: (uri: String, code: ChangeCode) -> Unit) {
        val function = NotifyFunction(callback, code.code)
        if (notifyFunctions.indexOf(function) == -1) {
            notifyFunctions.add(NotifyFunction(callback, code.code))
        }
    }

    /**
     * Remove function from notify functions list.
     *
     * @param callback Function to remove.
     */
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
        return featureClassDeleteEditOperation(handle, operation.fid, operation.aid,
                operation.operationType.code, operation.rid, operation.arid)
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

    internal fun URLUploadFileInt(path: String, url: String, options: Array<String>,
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

    internal fun URLAuthGetMapInt(uri: String) : Map<String, String> {
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

    internal fun URLRequestInt(type: Int, url: String, options: Map<String, String> = mapOf()) : RequestResult = URLRequest(type, url, toArrayOfCStrings(options))

    internal fun URLRequestJsonInt(type: Int, url: String, options: Map<String, String> = mapOf()) : RequestResultJson {
        val result = API.URLRequestJson(type, url, toArrayOfCStrings(options))
        return RequestResultJson(result.status, JsonObject(result.value))
    }

    internal fun URLRequestRawInt(type: Int, url: String, options: Map<String, String> = mapOf()) : RequestResultRaw = URLRequestRaw(type, url, toArrayOfCStrings(options))


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

    /*
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

    /*
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

    /*
     * Coordinate transformation
     */
    internal fun coordinateTransformationCreateInt(fromEPSG: Int, toEPSG: Int): Long = coordinateTransformationCreate(fromEPSG, toEPSG)
    internal fun coordinateTransformationFreeInt(handle: Long) = coordinateTransformationFree(handle)
    internal fun coordinateTransformationDoInt(handle: Long, x: Double, y: Double): Point = coordinateTransformationDo(handle, x, y)

    /*
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

    internal fun mapCreateInt(name: String, description: String, EPSG: Int, minX: Double,
                                   minY: Double, maxX: Double, maxY: Double) : Int = mapCreate(name, description, EPSG, minX, minY, maxX, maxY)
    internal fun mapOpenInt(path: String) : Int = mapOpen(path)
    internal fun mapSaveInt(mapId: Int, path: String) : Boolean = mapSave(mapId, path)
    internal fun mapCloseInt(mapId: Int) : Boolean = mapClose(mapId)
    internal fun mapReopenInt(mapId: Int, path: String) : Boolean = mapReopen(mapId, path)
    internal fun mapLayerCountInt(mapId: Int) : Int = mapLayerCount(mapId)
    internal fun mapCreateLayerInt(mapId: Int, name: String, path: String) : Int = mapCreateLayer(mapId, name, path)
    internal fun mapLayerGetInt(mapId: Int, layerId: Int) : Long = mapLayerGet(mapId, layerId)
    internal fun mapLayerDeleteInt(mapId: Int, layer: Long) : Boolean = mapLayerDelete(mapId, layer)
    internal fun mapLayerReorderInt(mapId: Int, beforeLayer: Long, movedLayer: Long) : Boolean = mapLayerReorder(mapId, beforeLayer, movedLayer)
    internal fun mapSetSizeInt(mapId: Int, width: Int, height: Int, YAxisInverted: Boolean) : Boolean = mapSetSize(mapId, width, height, YAxisInverted)
    internal fun mapDrawInt(mapId: Int, state: MapDocument.DrawState, callback: ((status: StatusCode, complete: Double, message: String) -> Boolean)) : Boolean {
        if(drawingProgressFuncId != callback.hashCode()) {
            progressFunctions.remove(drawingProgressFuncId)
            drawingProgressFuncId = callback.hashCode()
            progressFunctions[drawingProgressFuncId] = ProgressFunction(callback)
        }

        return mapDraw(mapId, state.code, drawingProgressFuncId)
    }
    internal fun mapDrawRemoveCallbackInt() {
        progressFunctions.remove(drawingProgressFuncId)
        drawingProgressFuncId = 0
    }
    internal fun mapInvalidateInt(mapId: Int, envelope: Envelope) : Boolean = mapInvalidate(mapId, envelope.minX, envelope.minY, envelope.maxX, envelope.maxY)
    internal fun mapSetBackgroundColorInt(mapId: Int, color: RGBA) : Boolean = mapSetBackgroundColor(mapId, color.R, color.G, color.B, color.A)
    internal fun mapGetBackgroundColorInt(mapId: Int) : RGBA = mapGetBackgroundColor(mapId)
    internal fun mapSetCenterInt(mapId: Int, x: Double, y: Double) : Boolean = mapSetCenter(mapId, x, y)
    internal fun mapGetCenterInt(mapId: Int) : Point = mapGetCenter(mapId)
    internal fun mapGetCoordinateInt(mapId: Int, x: Double, y: Double) : Point = mapGetCoordinate(mapId, x, y)
    internal fun mapGetDistanceInt(mapId: Int, w: Double, h: Double) : Point = mapGetDistance(mapId, w, h)
    internal fun mapSetRotateInt(mapId: Int, dir: Int, rotate: Double) : Boolean = mapSetRotate(mapId, dir, rotate)
    internal fun mapGetRotateInt(mapId: Int, dir: Int) : Double = mapGetRotate(mapId, dir)
    internal fun mapSetScaleInt(mapId: Int, scale: Double) : Boolean = mapSetScale(mapId, scale)
    internal fun mapGetScaleInt(mapId: Int) : Double = mapGetScale(mapId)

    internal fun mapSetOptionsInt(mapId: Int, options: Map<String, String>) : Boolean = mapSetOptions(mapId, toArrayOfCStrings(options))
    internal fun mapSetExtentLimitsInt(mapId: Int, minX: Double, minY: Double,
                                            maxX: Double, maxY: Double) : Boolean = mapSetExtentLimits(mapId, minX, minY, maxX, maxY)
    internal fun mapGetExtentInt(mapId: Int, EPSG: Int) : Envelope = mapGetExtent(mapId, EPSG)
    internal fun mapSetExtentInt(mapId: Int, extent: Envelope) : Boolean = mapSetExtent(mapId, extent.minX, extent.minY, extent.maxX, extent.maxY)

    internal fun mapGetSelectionStyleInt(mapId: Int, styleType: Int) : Long = mapGetSelectionStyle(mapId, styleType)
    internal fun mapSetSelectionsStyleInt(mapId: Int, styleType: Int, style: Long) : Boolean = mapSetSelectionsStyle(mapId, styleType, style)
    internal fun mapGetSelectionStyleNameInt(mapId: Int, styleType: Int) : String = mapGetSelectionStyleName(mapId, styleType)
    internal fun mapSetSelectionStyleNameInt(mapId: Int, styleType: Int, name: String) : Boolean = mapSetSelectionStyleName(mapId, styleType, name)
    internal fun mapIconSetAddInt(mapId: Int, name: String, path: String, ownByMap: Boolean) : Boolean = mapIconSetAdd(mapId, name, path, ownByMap)
    internal fun mapIconSetRemoveInt(mapId: Int, name: String) : Boolean = mapIconSetRemove(mapId, name)
    internal fun mapIconSetExistsInt(mapId: Int, name: String) : Boolean = mapIconSetExists(mapId, name)

    /*
     * Layer functions
     */

    internal fun layerGetNameInt(layer: Long) : String = layerGetName(layer)
    internal fun layerSetNameInt(layer: Long, name: String) : Boolean = layerSetName(layer, name)
    internal fun layerGetVisibleInt(layer: Long) : Boolean = layerGetVisible(layer)
    internal fun layerSetVisibleInt(layer: Long, visible: Boolean) : Boolean = layerSetVisible(layer, visible)
    internal fun layerGetDataSourceInt(layer: Long) : Long = layerGetDataSource(layer)
    internal fun layerGetStyleInt(layer: Long) : Long = layerGetStyle(layer)
    internal fun layerSetStyleInt(layer: Long, style: Long) : Boolean = layerSetStyle(layer, style)
    internal fun layerGetStyleNameInt(layer: Long) : String = layerGetStyleName(layer)
    internal fun layerSetStyleNameInt(layer: Long, name: String) : Boolean = layerSetStyleName(layer, name)
    internal fun layerSetSelectionIdsInt(layer: Long, ids: LongArray) : Boolean = layerSetSelectionIds(layer, ids)
    internal fun layerSetHideIdsInt(layer: Long, ids: LongArray) : Boolean = layerSetHideIds(layer, ids)

    /*
     * Overlay functions
     */
    internal fun overlaySetVisibleInt(mapId: Int, typeMask: Int, visible: Boolean) : Boolean = overlaySetVisible(mapId, typeMask, visible)
    internal fun overlayGetVisibleInt(mapId: Int, type: Int) : Boolean = overlayGetVisible(mapId, type)
    internal fun overlaySetOptionsInt(mapId: Int, type: Int, options: Array<String>) : Boolean = overlaySetOptions(mapId, type, options)
    internal fun overlayGetOptionsInt(mapId: Int, type: Int) : Map<String, String> {
        val options = overlayGetOptions(mapId, type)
        val out: MutableMap<String, String> = mutableMapOf()
        for(option in options) {
            val parts = option.split("=")
            if(parts.size > 1) {
                out[parts[0]] = parts[1]
            }
        }
        return out
    }

    /* Edit */
    internal fun editOverlayTouchInt(mapId: Int, x: Double, y: Double, type: Int) : TouchResult = editOverlayTouch(mapId, x, y, type)
    internal fun editOverlayUndoInt(mapId: Int) : Boolean = editOverlayUndo(mapId)
    internal fun editOverlayRedoInt(mapId: Int) : Boolean = editOverlayRedo(mapId)
    internal fun editOverlayCanUndoInt(mapId: Int) : Boolean = editOverlayCanUndo(mapId)
    internal fun editOverlayCanRedoInt(mapId: Int) : Boolean = editOverlayCanRedo(mapId)
    internal fun editOverlaySaveInt(mapId: Int) : Long = editOverlaySave(mapId)
    internal fun editOverlayCancelInt(mapId: Int) : Boolean = editOverlayCancel(mapId)
    internal fun editOverlayCreateGeometryInLayerInt(mapId: Int, layer: Long, empty: Boolean) : Boolean = editOverlayCreateGeometryInLayer(mapId, layer, empty)
    internal fun editOverlayCreateGeometryInt(mapId: Int, type: Int) : Boolean = editOverlayCreateGeometry(mapId, type)
    internal fun editOverlayEditGeometryInt(mapId: Int, layer: Long, feateureId: Long) : Boolean = editOverlayEditGeometry(mapId, layer, feateureId)
    internal fun editOverlayDeleteGeometryInt(mapId: Int) : Boolean = editOverlayDeleteGeometry(mapId)
    internal fun editOverlayAddPointInt(mapId: Int) : Boolean = editOverlayAddPoint(mapId)
    internal fun editOverlayAddVertexInt(mapId: Int, x: Double, y: Double, z: Double) : Boolean = editOverlayAddVertex(mapId, x, y, z)
    internal fun editOverlayDeletePointInt(mapId: Int) : Int = editOverlayDeletePoint(mapId)
    internal fun editOverlayAddHoleInt(mapId: Int) : Boolean = editOverlayAddHole(mapId)
    internal fun editOverlayDeleteHoleInt(mapId: Int) : Int = editOverlayDeleteHole(mapId)
    internal fun editOverlayAddGeometryPartInt(mapId: Int) : Boolean = editOverlayAddGeometryPart(mapId)
    internal fun editOverlayDeleteGeometryPartInt(mapId: Int) : Int = editOverlayDeleteGeometryPart(mapId)
    internal fun editOverlayGetGeometryInt(mapId: Int) : Long = editOverlayGetGeometry(mapId)
    internal fun editOverlaySetStyleInt(mapId: Int, type: Int, style: Long) : Boolean = editOverlaySetStyle(mapId, type, style)
    internal fun editOverlaySetStyleNameInt(mapId: Int, type: Int, name: String) : Boolean = editOverlaySetStyleName(mapId, type, name)
    internal fun editOverlayGetStyleInt(mapId: Int, type: Int) : Long = editOverlayGetStyle(mapId, type)
    internal fun editOverlaySetWalkingModeInt(mapId: Int, enable: Boolean) = editOverlaySetWalkingMode(mapId, enable)
    internal fun editOverlayGetWalkingModeInt(mapId: Int) : Boolean = editOverlayGetWalkingMode(mapId)

    /* Location */
    internal fun locationOverlayUpdateInt(mapId: Int, x: Double, y: Double, z: Double,
                                               direction: Float, accuracy: Float) : Boolean = locationOverlayUpdate(mapId, x, y, z, direction, accuracy)
    internal fun locationOverlaySetStyleInt(mapId: Int, style: Long) : Boolean = locationOverlaySetStyle(mapId, style)
    internal fun locationOverlaySetStyleNameInt(mapId: Int, name: String) : Boolean = locationOverlaySetStyleName(mapId, name)
    internal fun locationOverlayGetStyleInt(mapId: Int) : Long = locationOverlayGetStyle(mapId)

    /*
     * QMS
     */
    internal fun QMSQueryInt(options: Map<String, String>) : Array<QMSItemInt> = QMSQuery(toArrayOfCStrings(options))
    internal fun QMSQueryPropertiesInt(id: Int) : QMSItemPropertiesInt = QMSQueryProperties(id)

    /*
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
     * @param full If full value is true catalog and map storage will be freed, otherwise only map storage
     */
    private external fun freeResources(full: Boolean)
    private external fun init(options: Array<String>): Boolean

    /*
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
     * Create MD5 hash from text.
     *
     * @param value Text to create MD5 hash.
     * @return MD5 hash string created from text.
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

    /*
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


    /*
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

    /*
     * Coordinate transformation
     */
    private external fun coordinateTransformationCreate(fromEPSG: Int, toEPSG: Int): Long
    private external fun coordinateTransformationFree(handle: Long)
    private external fun coordinateTransformationDo(handle: Long, x: Double, y: Double): Point

    /*
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

    /*
     * Raster
     */
    private external fun rasterCacheArea(handle: Long, options: Array<String>, callbackId: Int) : Boolean

    /*
     * Map functions
     */
    private external fun mapCreate(name: String, description: String, EPSG: Int, minX: Double,
                                   minY: Double, maxX: Double, maxY: Double) : Int
    private external fun mapOpen(path: String) : Int
    private external fun mapSave(mapId: Int, path: String) : Boolean
    private external fun mapClose(mapId: Int) : Boolean
    private external fun mapReopen(mapId: Int, path: String) : Boolean
    private external fun mapLayerCount(mapId: Int) : Int
    private external fun mapCreateLayer(mapId: Int, name: String, path: String) : Int
    private external fun mapLayerGet(mapId: Int, layerId: Int) : Long
    private external fun mapLayerDelete(mapId: Int, layer: Long) : Boolean
    private external fun mapLayerReorder(mapId: Int, beforeLayer: Long, movedLayer: Long) : Boolean
    private external fun mapSetSize(mapId: Int, width: Int, height: Int, YAxisInverted: Boolean) : Boolean
    private external fun mapDraw(mapId: Int, state: Int, callbackId: Int) : Boolean
    private external fun mapInvalidate(mapId: Int, minX : Double, minY : Double,
                                       maxX : Double, maxY : Double) : Boolean
    private external fun mapSetBackgroundColor(mapId: Int, R: Int, G: Int, B: Int, A: Int) : Boolean
    private external fun mapGetBackgroundColor(mapId: Int) : RGBA
    private external fun mapSetCenter(mapId: Int, x: Double, y: Double) : Boolean
    private external fun mapGetCenter(mapId: Int) : Point
    private external fun mapGetCoordinate(mapId: Int, x: Double, y: Double) : Point
    private external fun mapGetDistance(mapId: Int, w: Double, h: Double) : Point
    private external fun mapSetRotate(mapId: Int, dir: Int, rotate: Double) : Boolean
    private external fun mapGetRotate(mapId: Int, dir: Int) : Double
    private external fun mapSetScale(mapId: Int, scale: Double) : Boolean
    private external fun mapGetScale(mapId: Int) : Double

    private external fun mapSetOptions(mapId: Int, options: Array<String>) : Boolean
    private external fun mapSetExtentLimits(mapId: Int, minX: Double, minY: Double,
                                            maxX: Double, maxY: Double) : Boolean
    private external fun mapGetExtent(mapId: Int, EPSG: Int) : Envelope
    private external fun mapSetExtent(mapId: Int, minX : Double, minY : Double,
                                      maxX : Double, maxY : Double) : Boolean

    private external fun mapGetSelectionStyle(mapId: Int, styleType: Int) : Long
    private external fun mapSetSelectionsStyle(mapId: Int, styleType: Int, style: Long) : Boolean
    private external fun mapGetSelectionStyleName(mapId: Int, styleType: Int) : String
    private external fun mapSetSelectionStyleName(mapId: Int, styleType: Int, name: String) : Boolean
    private external fun mapIconSetAdd(mapId: Int, name: String, path: String, ownByMap: Boolean) : Boolean
    private external fun mapIconSetRemove(mapId: Int, name: String) : Boolean
    private external fun mapIconSetExists(mapId: Int, name: String) : Boolean

    /*
     * Layer functions
     */
    private external fun layerGetName(layer: Long) : String
    private external fun layerSetName(layer: Long, name: String) : Boolean
    private external fun layerGetVisible(layer: Long) : Boolean
    private external fun layerSetVisible(layer: Long, visible: Boolean) : Boolean
    private external fun layerGetDataSource(layer: Long) : Long
    private external fun layerGetStyle(layer: Long) : Long
    private external fun layerSetStyle(layer: Long, style: Long) : Boolean
    private external fun layerGetStyleName(layer: Long) : String
    private external fun layerSetStyleName(layer: Long, name: String) : Boolean
    private external fun layerSetSelectionIds(layer: Long, ids: LongArray) : Boolean
    private external fun layerSetHideIds(layer: Long, ids: LongArray) : Boolean

    /*
     * Overlay functions
     */

    private external fun overlaySetVisible(mapId: Int, typeMask: Int, visible: Boolean) : Boolean
    private external fun overlayGetVisible(mapId: Int, type: Int) : Boolean
    private external fun overlaySetOptions(mapId: Int, type: Int, options: Array<String>) : Boolean
    private external fun overlayGetOptions(mapId: Int, type: Int) : Array<String>

    /* Edit */
    private external fun editOverlayTouch(mapId: Int, x: Double, y: Double, type: Int) : TouchResult
    private external fun editOverlayUndo(mapId: Int) : Boolean
    private external fun editOverlayRedo(mapId: Int) : Boolean
    private external fun editOverlayCanUndo(mapId: Int) : Boolean
    private external fun editOverlayCanRedo(mapId: Int) : Boolean
    private external fun editOverlaySave(mapId: Int) : Long
    private external fun editOverlayCancel(mapId: Int) : Boolean
    private external fun editOverlayCreateGeometryInLayer(mapId: Int, layer: Long, empty: Boolean) : Boolean
    private external fun editOverlayCreateGeometry(mapId: Int, type: Int) : Boolean
    private external fun editOverlayEditGeometry(mapId: Int, layer: Long, featureId: Long) : Boolean
    private external fun editOverlayDeleteGeometry(mapId: Int) : Boolean
    private external fun editOverlayAddPoint(mapId: Int) : Boolean
    private external fun editOverlayAddVertex(mapId: Int, x: Double, y: Double, z: Double) : Boolean
    private external fun editOverlayDeletePoint(mapId: Int) : Int
    private external fun editOverlayAddHole(mapId: Int) : Boolean
    private external fun editOverlayDeleteHole(mapId: Int) : Int
    private external fun editOverlayAddGeometryPart(mapId: Int) : Boolean
    private external fun editOverlayDeleteGeometryPart(mapId: Int) : Int
    private external fun editOverlayGetGeometry(mapId: Int) : Long
    private external fun editOverlaySetStyle(mapId: Int, type: Int, style: Long) : Boolean
    private external fun editOverlaySetStyleName(mapId: Int, type: Int, name: String) : Boolean
    private external fun editOverlayGetStyle(mapId: Int, type: Int) : Long
    private external fun editOverlaySetWalkingMode(mapId: Int, enable: Boolean)
    private external fun editOverlayGetWalkingMode(mapId: Int) : Boolean

    /* Location */
    private external fun locationOverlayUpdate(mapId: Int, x: Double, y: Double, z: Double,
                                               direction: Float, accuracy: Float) : Boolean
    private external fun locationOverlaySetStyle(mapId: Int, style: Long) : Boolean
    private external fun locationOverlaySetStyleName(mapId: Int, name: String) : Boolean
    private external fun locationOverlayGetStyle(mapId: Int) : Long

    /*
     * QMS
     */
    private external fun QMSQuery(options: Array<String>) : Array<QMSItemInt>
    private external fun QMSQueryProperties(id: Int) : QMSItemPropertiesInt
}
