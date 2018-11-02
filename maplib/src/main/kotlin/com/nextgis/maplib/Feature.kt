/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 20.08.18 22:04.
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

import java.util.*

/**
 * Date components class to transform from separate values of year, month, day, hour, minute and second to date.
 *
 * @property year Year value
 * @property month Month value
 * @property day Day value
 * @property hour Hour value
 * @property minute Minute value
 * @property second Second value
 * @property gmtOffset Time zone offset value
 */
data class DateComponents(val year: Int, val month: Int, val day: Int, val hour: Int,
                          val minute: Int, val second: Int, val gmtOffset: Int) {
    /**
     * Transform to date class.
     *
     * @return Date class instance.
     */
    fun toDate() : Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute, second)
        calendar.set(Calendar.ZONE_OFFSET, gmtOffset)
        return calendar.time
    }
}

/**
 * Feature or row.
 *
 * @property handle Feature handle for C API.
 * @property table Table the feature belongs to.
 */
class Feature(val handle: Long, val table: Table) {

    /**
     * Feature/row identifier.
     */
    val id: Long get() = API.featureGetIdInt(handle)

    /**
     * Feature/row geometry.
     */
    var geometry: Geometry?
        get() {
            val geomHandle = API.featureGetGeometryInt(handle)
            return if(geomHandle != 0L) Geometry(geomHandle) else null
        }
        set(value) {
            if( table is FeatureClass && value != null ) {
                API.featureSetGeometryInt(handle, value.handle)
            }
            else {
                printError("This is not Feature class. Don't add geometry")
            }
        }

    /**
     * Feature/row remote identifier or -1.
     */
    var remoteId: Long
        get() = API.storeFeatureGetRemoteIdInt(handle)
        set(id) {
            API.storeFeatureSetRemoteIdInt(handle, id)
        }

    private fun finalize() {
        API.featureFreeInt(handle)
    }

    /**
     * Check if field set.
     *
     * @param index Field index.
     * @return True if field set.
     */
    fun isFieldSet(index: Int) : Boolean {
        return API.featureIsFieldSetInt(handle, index)
    }

    /**
     * Get field integer value.
     *
     * @param index Field index.
     * @return Field value.
     */
    fun getFieldAsInteger(index: Int) : Int {
        return API.featureGetFieldAsIntegerInt(handle, index)
    }

    /**
     * Get field double value.
     *
     * @param index Field index.
     * @return Field value.
     */
    fun getFieldAsDouble(index: Int) : Double {
        return API.featureGetFieldAsDoubleInt(handle, index)
    }

    /**
     * Get field string value.
     *
     * @param index Field index.
     * @return Field value.
     */
    fun getFieldAsString(index: Int) : String {
        return API.featureGetFieldAsStringInt(handle, index)
    }

    /**
     * Get field value.
     *
     * @param index Field index.
     * @return Field value.
     */
    fun getFieldAsDateTime(index: Int) : Date {
        return API.featureGetFieldAsDateTimeInt(handle, index).toDate()
    }

    /**
     * Set field value.
     *
     * @param index Field index.
     * @param value Value to set.
     */
    fun setField(index: Int, value: String) {
        API.featureSetFieldStringInt(handle, index, value)
    }

    /**
     * Set field value.
     *
     * @param index Field index.
     * @param value Value to set.
     */
    fun setField(index: Int, value: Double) {
        API.featureSetFieldDoubleInt(handle, index, value)
    }

    /**
     * Set field value.
     *
     * @param index Field index.
     * @param value Value to set.
     */
    fun setField(index: Int, value: Int) {
        API.featureSetFieldIntegerInt(handle, index, value)
    }

    /**
     * Set field value.
     *
     * @param index Field index.
     * @param value Value to set.
     */
    fun setField(index: Int, value: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = value
        calendar.set(Calendar.ZONE_OFFSET, 0)

        API.featureSetFieldDateTimeInt(handle, index, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND))
    }

    /**
     * Create new geometry. The type of geometry will be corespondent to feature class geometry type.
     *
     * @return Geometry class instance or null.
     */
    fun createGeometry() : Geometry? {
        val fc: FeatureClass? = table as? FeatureClass
        if (fc != null) {
            when(fc.geometryType) {
                Geometry.Type.POINT -> return GeoPoint(API.featureCreateGeometryInt(handle))
                else -> Geometry(API.featureCreateGeometryInt(handle))
            }
        }
        return null
    }

    /**
     * Get attachment.
     *
     * @param aid Attachment identifier.
     * @return Attachment class instance or null.
     */
    fun getAttachment(aid: Long) : Attachment? {
        val attachments = API.featureAttachmentsGetInt(handle)
        for(attachment in attachments) {
            if(attachment.id == aid) {
                return attachment
            }
        }
        return null
    }

    /**
     * Get all attachments.
     *
     * @return Attachment array.
     */
    fun getAttachments() : Array<Attachment> {
        return API.featureAttachmentsGetInt(handle)
    }

    /**
     * Add new attachment.
     *
     * @param name Name.
     * @param description Description text.
     * @param path File system path.
     * @param move If true the attachment file will be
     * @param remoteId Remote identifier.
     * @param logEdits Log edits in history table. This log can be received using editOperations function.
     * @return New attachment identifier.
     */
    fun addAttachment(name: String, description: String, path: String, move: Boolean,
                      remoteId: Long = -1, logEdits: Boolean = true) : Long {
        val options = mapOf(
                "MOVE" to if(move) "ON" else "OFF",
                "RID" to remoteId.toString()
        )

        return API.featureAttachmentAddInt(handle, name, description, path, toArrayOfCStrings(options),
                logEdits)
    }

    /**
     * Delete attachment.
     *
     * @param aid Attachment identifier.
     * @param logEdits Log edits in history table. This log can be received using editOperations function.
     * @return True on success.
     */
    fun deleteAttachment(aid: Long, logEdits: Boolean = true) : Boolean {
        return API.featureAttachmentDeleteInt(handle, aid, logEdits)
    }

    /**
     * Delete attachment.
     *
     * @param attachment Attachment class instance.
     * @param logEdits Log edits in history table. This log can be received using editOperations function.
     * @return True on success.
     */
    fun deleteAttachment(attachment: Attachment, logEdits: Boolean = true) : Boolean {
        return API.featureAttachmentDeleteInt(handle, attachment.id, logEdits)
    }

    /**
     * Delete feature.
     *
     * @return True on success.
     */
    fun delete() : Boolean {
        return table.deleteFeature(id)
    }
}

/**
 * Attachment class. A file added to the feature/row
 *
 * @property handle Handle of attachment or 0 for new one.
 * @property id Attachment id.
 * @property name Attachment name.
 * @property description Attachment description.
 * @property path Attachment file path in file system.
 * @property size Attachment file size.
 */
class Attachment(val handle: Long = 0, val id: Long, val name: String, val description: String,
                 val path: String, val size: Long, private var remoteIdVal: Long) {

    constructor(name: String, description: String, path: String) : this(0, -1, name, description, path, -1, -1)

    /**
     * Remote identifier read/write property.
     */
    var remoteId: Long
        get() = remoteIdVal
        set(rid) {
            if(handle != 0L) {
                API.storeFeatureSetAttachmentRemoteIdInt(handle, id, rid)
            }
            remoteIdVal = rid
        }

    /**
     * Is attachment file available on disk.
     *
     * @return True of file exists.
     */
    fun isFileAvailable() : Boolean {
        return path.isEmpty()
    }

    /**
     * Update attachment. Only name and description can be updated. To change attachment file, just delete attachment and create new one.
     *
     * @param name New attachment name.
     * @param description New attachment description.
     * @param logEdits Log edits in history table. This log can be received using editOperations function.
     * @return True on success.
     */
    fun update(name: String, description: String, logEdits: Boolean = true) : Boolean {
        return if( handle == 0L) false else API.featureAttachmentUpdateInt(handle, id, name,
                description, logEdits)
    }
}
