/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 25.09.18 16:18.
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

import java.lang.ref.WeakReference

data class TouchResult(val pointId: Int, val isHole: Boolean)

/**
 * @class Overlay. Map overlay to draw different features (for example, current position, edit features, etc.)
 */
open class Overlay(mapPtr: MapDocument, val type: Overlay.Type, protected val map: WeakReference<MapDocument> = WeakReference(mapPtr)) {

    /**
     * @enum Type. Map overlay types.
     */
    enum class Type(val code: Int) {
        UNKNOWN(0),     /**< UNKNOWN: unknown overlay type. */
        LOCATION(1),    /**< LOCATION: overlay with current location mark. */
        TRACK(2),       /**< TRACK: overlay for current tracks. */
        EDIT(3),        /**< EDIT: overlay for vector geometry editing. */
        FIGURES(4),     /**< FIGURES: overlay to show geometry primitives. */
        ALL(5);         /**< ALL: All overlays. */

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
     * Overlay visible read/write property.
     */
    var visible: Boolean
        get() = API.overlayGetVisibleInt(map.get()!!.id, type.code)
        set(newValue) {
            API.overlaySetVisibleInt(map.get()!!.id, type.code, newValue)
        }

    /**
     * Overlay options key-value dictionary. The keys depend on overlay type.
     */
    var options: Map<String, String>
        get() = API.overlayGetOptionsInt(map.get()!!.id, type.code)
        set(newValue) {
            API.overlaySetOptionsInt(map.get()!!.id, type.code, toArrayOfCStrings(newValue))
        }
}


/**
 * @class LocationOverlay. Overlay showing current position
 */
class LocationOverlay(mapPtr: MapDocument) : Overlay(mapPtr, Type.LOCATION) {

    /**
     *  Overlay style json object
     */
    var style: JsonObject
        get() = JsonObject(API.locationOverlayGetStyleInt(map.get()!!.id))
        set(newValue) {
            API.locationOverlaySetStyleInt(map.get()!!.id, newValue.handle)
        }

    /**
     * Set overlay style name.
     *
     * @param name: Overlay style name. The supported names depend on overlay type.
     * @return True on success.
     */
    fun setStyle(name: String) : Boolean {
        return API.locationOverlaySetStyleNameInt(map.get()!!.id, name)
    }

    internal fun updateLocation(location: Point, direction: Float, accuracy: Float) {
        API.locationOverlayUpdateInt(map.get()!!.id, location.x, location.y, 0.0, direction, accuracy)
    }
}

/**
 * Overlay showing edit features
 */
class EditOverlay(mapPtr: MapDocument) : Overlay(mapPtr, Type.EDIT) {

    /**
     * Edit layer read/write property.
     */
    var editLayer: Layer? = null

    /**
     * @enum DeleteResultType. Edit operation result type.
     */
    enum class DeleteResultType(val code: Int) {
        FAILED(1),              /**< Delete operation failed */
        SELTYPE_NO_CHANGE(2),   /**< Same piece type is selected after delete operation */
        HOLE(3),                /**< Hole is deleted. Outer ring selected */
        PART(4),                /**< Part is deleted. Other part selected */
        GEOMETRY(5);            /**< Whole geometry is deleted */

        companion object {
            fun from(value: Int): DeleteResultType {
                for (code in values()) {
                    if (code.code == value) {
                        return code
                    }
                }
                return FAILED
            }
        }
    }

    /**
     * @enum EditStyleType. Edit style type.
     */
    enum class EditStyleType(val code: Int) {
        POINT(0), LINE(1), FILL(2), CROSS(3)
    }

    /**
     * Point json object style.
     */
    var pointStyle: JsonObject
        get() = JsonObject(API.editOverlayGetStyleInt(map.get()!!.id, EditStyleType.POINT.code))
        set(newValue) {
            API.editOverlaySetStyleInt(map.get()!!.id, EditStyleType.POINT.code, newValue.handle)
        }

    /**
     * Line json object style.
     */
    var lineStyle: JsonObject
        get() = JsonObject(API.editOverlayGetStyleInt(map.get()!!.id, EditStyleType.LINE.code))
        set(newValue) {
            API.editOverlaySetStyleInt(map.get()!!.id, EditStyleType.LINE.code, newValue.handle)
        }

    /**
     * Polygon json object style.
     */
    var fillStyle: JsonObject
        get() = JsonObject(API.editOverlayGetStyleInt(map.get()!!.id, EditStyleType.FILL.code))
        set(newValue) {
            API.editOverlaySetStyleInt(map.get()!!.id, EditStyleType.FILL.code, newValue.handle)
        }

    /**
     * The cross in screen center style read/write property.
     */
    var crossStyle: JsonObject
        get() = JsonObject(API.editOverlayGetStyleInt(map.get()!!.id, EditStyleType.CROSS.code))
        set(newValue) {
            API.editOverlaySetStyleInt(map.get()!!.id, EditStyleType.CROSS.code, newValue.handle)
        }

    /**
     * Edit geometry property.
     */
    val geometry: Geometry?
        get() {
            val geomHandle = API.editOverlayGetGeometryInt(map.get()!!.id)
            if(geomHandle != 0L) {
                return Geometry(geomHandle)
            }
            return null
        }
    /**
     * Edit geometry type property.
     */
    val geometryType: Geometry.Type
        get() {
            if(editLayer != null) {
                val ds = editLayer?.dataSource as? FeatureClass
                if(ds != null) {
                    return ds.geometryType
                }
            }
            return Geometry.Type.NONE
        }

    /**
     * Enable/disable edit by walk mode.
     */
    var walkingMode: Boolean
        get() = API.editOverlayGetWalkingModeInt(map.get()!!.id)
        set(newValue) {
            API.editOverlaySetWalkingModeInt(map.get()!!.id, newValue)
        }

    /**
     * Set edit overlay point feature style.
     *
     * @param name: Style name.
     * @return True on success.
     */
    fun setPointStyle(name: String) : Boolean {
        return API.editOverlaySetStyleNameInt(map.get()!!.id, EditStyleType.POINT.code, name)
    }

    /**
     * Set edit overlay line feature style.
     *
     * @param name: Style name.
     * @return True on success.
     */
    fun setLineStyle(name: String) : Boolean {
        return API.editOverlaySetStyleNameInt(map.get()!!.id, EditStyleType.LINE.code, name)
    }

    /**
     * Set edit overlay polygon feature style.
     *
     * @param name: Style name.
     * @return True on success.
     */
    fun setFillStyle(name: String) : Boolean {
        return API.editOverlaySetStyleNameInt(map.get()!!.id, EditStyleType.FILL.code, name)
    }

    /**
     * Set edit overlay cross style.
     *
     * @param name: Style name.
     * @return True on success.
     */
    fun setCrossStyle(name: String) : Boolean {
        return API.editOverlaySetStyleNameInt(map.get()!!.id, EditStyleType.CROSS.code, name)
    }

    /**
     * Can undo edit operation.
     *
     * @return True or false.
     */
    fun canUndo() : Boolean {
        return API.editOverlayCanUndoInt(map.get()!!.id)
    }

    /**
     * Can redo edit operation.
     *
     * @return True or false.
     */
    fun canRedo() : Boolean {
        return API.editOverlayCanRedoInt(map.get()!!.id)
    }

    /**
     * Undo edit operation.
     *
     * @return True on success.
     */
    fun undo() : Boolean {
        return API.editOverlayUndoInt(map.get()!!.id)
    }

    /**
     * Redo edit operation.
     *
     * @return True on success.
     */
    fun redo() : Boolean {
        return API.editOverlayRedoInt(map.get()!!.id)
    }

    /**
     * Save edits and return result feature instance.
     *
     * @return Feature class instance or null.
     */
    fun save() : Feature? {
        val feature = API.editOverlaySaveInt(map.get()!!.id)
        if(feature != 0L) {
            return Feature(feature, editLayer?.dataSource as FeatureClass)
        }
        return null
    }

    /**
     * Cancel any edits.
     *
     * @return True on success.
     */
    fun cancel() : Boolean {
        return API.editOverlayCancelInt(map.get()!!.id)
    }

    /**
     * Create new geometry and start editing. If the layer datasource is point - te point at the center
     * of screen will be created, if line - line with two points, if polygon - polygon with three points.
     *
     * @param layer: Layer in which to create new geometry. The feature will be created in layer datasource.
     * @return True on success.
     */
    fun createNewGeometry(layer: Layer) : Boolean {
        editLayer = layer
        return API.editOverlayCreateGeometryInLayerInt(map.get()!!.id, layer.handle, false)
    }

    /**
     * Create new geometry and start editing. The geometry will be empty. This is for edit by walk editing.
     *
     * @param layer: Layer in which to create new geometry. The feature will be created in layer datasource.
     * @return True on success.
     */
    fun createNewEmptyGeometry(layer: Layer) : Boolean {
        editLayer = layer
        return API.editOverlayCreateGeometryInLayerInt(map.get()!!.id, layer.handle, true)
    }

    /**
     * Create new geometry and start editing.
     *
     * @param type: Geometry type.
     * @return True on success.
     */
    fun createNewGeometryOfType(type: Geometry.Type) : Boolean {
        editLayer = null
        return API.editOverlayCreateGeometryInt(map.get()!!.id, type.code)
    }

    /**
     * Start editing geometry from feature.
     *
     * @param feature: Feature to edit geometry.
     * @return True on success.
     */
    fun editGeometry(feature: Feature) : Boolean {
        val layer = map.get()?.getLayerForFeature(feature)
        if(layer != null) {
            editLayer = layer
            return API.editOverlayEditGeometryInt(map.get()!!.id, layer.handle, feature.id)
        }
        return false
    }

    /**
     * Delete geometry in editing feature.
     *
     * @return True on success.
     */
    fun deleteGeometry() : Boolean {
        return API.editOverlayDeleteGeometryInt(map.get()!!.id)
    }

    /**
     * Add geometry part.
     *
     * @return True on success.
     */
    fun addGeometryPart() : Boolean {
        return API.editOverlayAddGeometryPartInt(map.get()!!.id)
    }

    /**
     * Add point to geometry. Make sense only for line or polygon ring.
     *
     * @return True on success.
     */
    fun addGeometryPoint() : Boolean {
        return API.editOverlayAddPointInt(map.get()!!.id)
    }

    /**
     * Add point to geometry. Make sense only for line or polygon ring.
     *
     * @param coordinates: Point coordinates.
     * @return True on success.
     */
    fun addGeometryPoint(coordinates: Point) : Boolean {
        return API.editOverlayAddVertexInt(map.get()!!.id, coordinates.x, coordinates.y, 0.0)
    }

    /**
     * Delete point from geometry.
     *
     * @return True on success.
     */
    fun deleteGeometryPoint() : DeleteResultType {
        return EditOverlay.DeleteResultType.from(API.editOverlayDeletePointInt(map.get()!!.id))
    }

    /**
     * Delete geometry part
     *
     * @return The value of type enum DeleteResultType
     */
    fun deleteGeometryPart() : DeleteResultType {
        return EditOverlay.DeleteResultType.from(API.editOverlayDeleteGeometryPartInt(map.get()!!.id))
    }

    /**
     * Add hole to polygon geometry.
     *
     * @return True on success.
     */
    fun addGeometryHole() : Boolean {
        return API.editOverlayAddHoleInt(map.get()!!.id)
    }

    /**
     * Delete geometry hole.
     *
     * @return Delete result type indicating is this last hole or any already exists.
     */
    fun deleteGeometryHole() : DeleteResultType {
        return EditOverlay.DeleteResultType.from(API.editOverlayDeleteHoleInt(map.get()!!.id))
    }

    enum class MapTouchType {
        ON_DOWN,
        ON_MOVE,
        ON_UP,
        SINGLE
    }

    /**
     * Touch down event in edit overlay.
     *
     * @param x screen coordinate.
     * @param y screen coordinate.
     * @return TouchResult with selected point identifier and is this point belongs to hole.
     */
    fun touchDown(x: Float, y: Float) : TouchResult {
        return API.editOverlayTouchInt(map.get()!!.id, x.toDouble(), y.toDouble(), MapTouchType.ON_DOWN.ordinal)
    }

    /**
     * Touch up event in edit overlay.
     *
     * @param x screen coordinate.
     * @param y screen coordinate.
     * @return TouchResult with selected point identifier and is this point belongs to hole.
     */
    fun touchUp(x: Float, y: Float) : TouchResult {
        return API.editOverlayTouchInt(map.get()!!.id, x.toDouble(), y.toDouble(), MapTouchType.ON_UP.ordinal)
    }

    /**
     * Touch move event in edit overlay.
     *
     * @param x screen coordinate.
     * @param y screen coordinate.
     * @return TouchResult with selected point identifier and is this point belongs to hole.
     */
    fun touchMove(x: Float, y: Double) : TouchResult {
        return API.editOverlayTouchInt(map.get()!!.id, x.toDouble(), y.toDouble(), MapTouchType.ON_MOVE.ordinal)
    }

    /**
     * Touch single event in edit overlay. For example down and up.
     *
     * @param x screen coordinate.
     * @param y screen coordinate.
     * @return TouchResult with selected point identifier and is this point belongs to hole.
     */
    fun singleTouch(x: Float, y: Float) : TouchResult {
        return API.editOverlayTouchInt(map.get()!!.id, x.toDouble(), y.toDouble(), MapTouchType.SINGLE.ordinal)
    }
}