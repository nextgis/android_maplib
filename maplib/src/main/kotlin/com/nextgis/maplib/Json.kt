/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 06.09.18 15:19.
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
 * @class Json document class.
 */
class JsonDocument(val handle: Long = API.jsonDocumentCreateInt()) {


    private fun finalize() {
        API.jsonDocumentFreeInt(handle)
    }

    /**
     * Load document from url.
     *
     * @param url: Url to fetch Json document.
     * @param options: Options passed to the http request function. See Request.get for details.
     * @param callback: Callback function to show progress or cancel operation.
     *
     * @return True on success.
     */
    fun load(url: String, options: Map<String, String> = mapOf(),
             callback: ((status: StatusCode, complete: Double, message: String) -> Boolean)? = null ) : Boolean {
        return API.jsonDocumentLoadUrl(handle, url, toArrayOfCStrings(options), callback)
    }

    /**
     * Get json document root object.
     *
     * @return JsonObject class instance.
     */
    fun getRoot() : JsonObject {
        val newHandle = API.jsonDocumentRootInt(this.handle)
        val type = API.jsonObjectTypeInt(newHandle)
        return if(JsonObject.JsonObjectType.from(type) == JsonObject.JsonObjectType.ARRAY) {
            JsonArray(newHandle)
        } else {
            JsonObject(newHandle)
        }
    }
}

/**
 * @class JsonObject class.
 */
open class JsonObject(val handle: Long = 0) {

    /**
     * @enum Json object type.
     */
    enum class JsonObjectType(val code: Int) {
        NULL(1),    /**< Null object. */
        OBJECT(2),  /**< Another object. */
        ARRAY(3),   /**< Array of json objects. */
        BOOLEAN(4), /**< Boolean object. */
        STRING(5),  /**< String object. */
        INTEGER(6), /**< Integer object. */
        LONG(7),    /**< Long object. */
        DOUBLE(8);  /**< Double object. */

        companion object {
            fun from(value: Int): JsonObjectType {
                for (code in values()) {
                    if (code.code == value) {
                        return code
                    }
                }
                return NULL
            }
        }
    }

    /**
     * If json object valid the property will be true. The property is readonly.
     */
    val valid: Boolean get() = API.jsonObjectValidInt(handle)

    private fun finalize() {
        API.jsonObjectFreeInt(handle)
    }

    /**
     * Json object name.
     */
    val name: String  get() = API.jsonObjectNameInt(handle)

    /**
     * Json object type.
     */
    val type: JsonObjectType  get() = JsonObjectType.from(API.jsonObjectTypeInt(handle))

    /**
     * Get string from json object.
     *
     * @param defaultValue: Default string value.
     *
     * @return string
     */
    fun getString(defaultValue: String) : String {
        return API.jsonObjectGetStringInt(handle, defaultValue)
    }

    /**
     * Get double from json object.
     *
     * @param defaultValue: Default double value.
     *
     * @return double
     */
    fun getDouble(defaultValue: Double) : Double {
        return API.jsonObjectGetDoubleInt(handle, defaultValue)
    }

    /**
     * Get int from json object.
     *
     * @param defaultValue: Default int value.
     *
     * @return integer
     */
    fun getInteger(defaultValue: Int) : Int {
        return API.jsonObjectGetIntegerInt(handle, defaultValue)
    }

    /**
     * Get long from json object.
     *
     * @param defaultValue: Default long value.
     *
     * @return long
     */
    fun getLong(defaultValue: Long) : Long {
        return API.jsonObjectGetLongInt(handle, defaultValue)
    }

    /**
     * Get bool from json object.
     *
     * @param defaultValue: Default bool value.
     *
     * @return boolean
     */
    fun getBool(defaultValue: Boolean) : Boolean {
        return API.jsonObjectGetBoolInt(handle, defaultValue)
    }

    /**
     * Get json object from json object.
     *
     * @param name: Key name.
     *
     * @return json object.
     */
    fun getObject(name: String) : JsonObject {
        return JsonObject(API.jsonObjectGetObjectInt(handle, name))
    }

    /**
     * Get string from json object.
     *
     * @param key: Key value.
     * @param defaultValue: Default value.
     *
     * @return String value.
     */
    fun getString(key: String, defaultValue: String) : String {
        return API.jsonObjectGetStringForKeyInt(handle, key, defaultValue)
    }

    /**
     * Get double from json object.
     *
     * @param key: Key value.
     * @param defaultValue: Default value.
     *
     * @return Double value.
     */
    fun getDouble(key: String, defaultValue: Double) : Double {
        return API.jsonObjectGetDoubleForKeyInt(handle, key, defaultValue)
    }

    /**
     * Get integer from json object.
     *
     * @param key: Key value.
     * @param defaultValue: Default value.
     *
     * @return Integer value.
     */
    fun getInteger(key: String, defaultValue: Int) : Int {
        return API.jsonObjectGetIntegerForKeyInt(handle, key, defaultValue)
    }

    /**
     * Get long from json object.
     *
     * @param key: Key value.
     * @param defaultValue: Default value.
     *
     * @return Long value.
     */
    fun getLong(key: String, defaultValue: Long) : Long {
        return API.jsonObjectGetLongForKeyInt(handle, key, defaultValue)
    }

    /**
     * Get bool from json object.
     *
     * @param key: Key value.
     * @param defaultValue: Default value.
     *
     * @return Boolean value.
     */
    fun getBool(key: String, defaultValue: Boolean) : Boolean {
        return API.jsonObjectGetBoolForKeyInt(handle, key, defaultValue)
    }

    /**
     * Set string value.
     *
     * @param value: Value to set.
     * @param key: Key value.
     *
     * @return True on success.
     */
    fun setString(value: String, key: String) : Boolean {
        return API.jsonObjectSetStringForKeyInt(handle, key, value)
    }

    /**
     * Set double value.
     *
     * @param value: Value to set.
     * @param key: Key value.
     *
     * @return True on success.
     */
    fun setDouble(value: Double, key: String) : Boolean {
        return API.jsonObjectSetDoubleForKeyInt(handle, key, value)
    }

    /**
     * Set integer value.
     *
     * @param value: Value to set.
     * @param key: Key value.
     *
     * @return True on success.
     */
    fun setInteger(value: Int, key: String) : Boolean {
        return API.jsonObjectSetIntegerForKeyInt(handle, key, value)
    }

    /**
     * Set long value.
     *
     * @param value: Value to set.
     * @param key: Key value.
     *
     * @return True on success.
     */
    fun setLong(value: Long, key: String) : Boolean {
        return API.jsonObjectSetLongForKeyInt(handle, key, value)
    }

    /**
     * Set boolean value.
     *
     * @param value: Value to set.
     * @param key: Key value.
     *
     * @return True on success.
     */
    fun setBoolean(value: Boolean, key: String) : Boolean {
        return API.jsonObjectSetBoolForKeyInt(handle, key, value)
    }

    /**
     * Get json object children.
     *
     * @return Array of children.
     */
    fun children() : List<JsonObject> {
        val out: MutableList<JsonObject> = mutableListOf()
        val ids = API.jsonObjectChildrenInt(handle)
        for(id in ids) {
            out.add(JsonObject(id))
        }
        return out
    }

    /**
     * Get array by name.
     *
     * @param name: Array object name.
     *
     * @return Json array object.
     */
    fun getArray(name: String) : JsonArray {
        return JsonArray(API.jsonObjectGetArrayInt(handle, name))
    }
}

/**
 * Json array class.
 */
class JsonArray(handle: Long = 0) : JsonObject(handle) {

    /**
     * Item count.
     */
    val size: Int get() = API.jsonArraySizeInt(handle)

    /**
     * Get item by index. Index mast be between 0 and size.
     *
     * @param index: Item index.
     *
     * @return Json object.
     */
    fun getItem(index: Int) : JsonObject {
        return JsonObject(API.jsonArrayItemInt(handle, index))
    }
}