/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 06.09.18 17:19.
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

data class RequestResult(val status: Int, val value: String)
data class RequestResultJson(val status: Int, val value: JsonObject)
data class RequestResultRaw(val status: Int, val value: ByteArray)

internal data class RequestResultJsonInt(val status: Int, val value: Long)

/**
 * @class Request. HTTP request class.
 */
object Request {

    /**
     * @enum RequestType. Request type.
     */
    enum class RequestType(val code: Int) {
        GET(1),     /**< GET request. */
        POST(2),    /**< POST request. */
        PUT(3),     /**< PUT request. */
        DELETE(4)   /**< DELETE request. */
    }

    /**
     * Execute get request.
     *
     * <h1>Request option values.</h1>
     * <p>
     * Request options are key-value array. The keys may be:
     * <ul>
     *     <li>"**CONNECTTIMEOUT**": "val", where val is in seconds (possibly with decimals)
     *     <li>"**TIMEOUT**": "val", where val is in seconds. This is the maximum delay for the whole request to complete before being aborted
     *     <li>"**LOW_SPEED_TIME**": "val", where val is in seconds. This is the maximum time where the transfer speed should be below the LOW_SPEED_LIMIT (if not specified 1b/s), before the transfer to be considered too slow and aborted
     *     <li>"**LOW_SPEED_LIMIT**": "val", where val is in bytes/second. See LOW_SPEED_TIME. Has only effect if LOW_SPEED_TIME is specified too
     *     <li>"**HEADERS**": "val", where val is an extra header to use when getting a web page For example "Accept: application/x-ogcwkt"
     *     <li>"**COOKIE**": "val", where val is formatted as COOKIE1=VALUE1; COOKIE2=VALUE2;
     *     <li>"**MAX_RETRY**": "val", where val is the maximum number of retry attempts if a 502, 503 or 504 HTTP error occurs. Default is 0
     *     <li>"**RETRY_DELAY**": "val", where val is the number of seconds between retry attempts. Default is 30
     * </ul>
     * <p>
     *
     * @param url: URL to execute.
     * @param options: the array of key-value pairs - String:String.
     *
     * @return structure with return status code and String data.
     */
    fun get(url: String, options: Map<String, String> = mapOf()) : RequestResult {
        return API.URLRequest(RequestType.GET.code, url, options)
    }

    /**
     * Executes delete request.
     *
     * @see `Request option values`, for a description of the available options.
     *
     * @param url: URL to execute.
     * @param options: the array of key-value pairs - String:String.
     *
     * @return structure with return status code and String data.
     */
    fun delete(url: String, options: Map<String, String> = mapOf()) : RequestResult {
        return API.URLRequest(RequestType.DELETE.code, url, options)
    }

    /**
     * Executes post request.
     *
     * @see `Request option values`, for a description of the available options.
     *
     * @param url: URL to execute.
     * @param payload: Post payload string.
     * @param options: the array of key-value pairs - String:String.
     * @return structure with return status code and String data.
     */
    fun post(url: String, payload: String, options: Map<String, String> = mapOf()) : RequestResult {
        val fullOptions: MutableMap<String, String> = options.toMutableMap()
        fullOptions["POSTFIELDS"] = payload
        return API.URLRequest(RequestType.POST.code, url, options)
    }

    /**
     * Executes put request.
     *
     * @see `Request option values`, for a description of the available options.
     *
     * @param url: URL to execute.
     * @param payload: Post payload string.
     * @param options: the array of key-value pairs - String:String.
     * @return structure with return status code and String data.
     */
    fun put(url: String, payload: String, options: Map<String, String> = mapOf()) : RequestResult {
        val fullOptions: MutableMap<String, String> = options.toMutableMap()
        fullOptions["POSTFIELDS"] = payload
        return API.URLRequest(RequestType.POST.code, url, options)
    }

    /**
     * Executes get request.
     *
     * @see `Request option values`, for a description of the available options.
     *
     * @param url: URL to execute.
     * @param options: the array of key-value pairs - String:String.
     * @return structure with return status code and json data.
     */
    fun getJson(url: String, options: Map<String, String> = mapOf()) : RequestResultJson {
        return API.URLRequestJson(RequestType.GET.code, url, options)
    }

    /**
     * Executes post request.
     *
     * @see `Request option values`, for a description of the available options.
     *
     * @param url: URL to execute.
     * @param payload: Post payload.
     * @param options: the array of key-value pairs - String:String.
     * @return structure with return status code and json data.
     */
    fun postJson(url: String, payload: String, options: Map<String, String> = mapOf()) : RequestResultJson {
        val fullOptions: MutableMap<String, String> = options.toMutableMap()
        fullOptions["POSTFIELDS"] = payload
        return API.URLRequestJson(RequestType.POST.code, url, options)
    }

    /**
     * Executes get request. Useful for get images.
     *
     * @param url: URL to execute.
     * @param options: the array of key-value pairs - String:String.
     * @return structure with return status code and raw data.
     */
    fun getRaw(url: String, options: Map<String, String> = mapOf()) : RequestResultRaw {
        return API.URLRequestRaw(RequestType.GET.code, url, options)
    }

    /**
     * Executes upload request.
     *
     * @param path: Path to file in file system to upload.
     * @param url: URL to execute.
     * @param options: the array of key-value pairs - String:String.
     * @param callback: callback function to show progress or cancel upload.
     *
     * @return structure with return status code and json data.
     */
    fun upload(filePath: String, url: String, options: Map<String, String> = mapOf(),
               callback: ((status: StatusCode, complete: Double, message: String) -> Boolean)? = null) : RequestResultJson {
        val result = API.URLUploadFile(filePath, url, toArrayOfCStrings(options), callback)
        return RequestResultJson(result.status, JsonObject(result.value))
    }
}