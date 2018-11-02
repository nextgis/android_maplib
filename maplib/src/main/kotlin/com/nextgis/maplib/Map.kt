/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 25.09.18 15:41.
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
 * Color representation
 *
 * RGBA color
 *
 * @property R a red color
 * @property G a green color
 * @property B a blue color
 * @property A an alpha color
 */
data class RGBA(val R: Int, val G: Int, val B: Int, val A: Int)

/**
 * MapDocument class.
 *
 * The ordered array of layers.
 *
 * @property id map identifier
 * @property path map file path
 */
class MapDocument(val id: Int, val path: String, private var bkColor: RGBA = API.mapGetBackgroundColorInt(id)) {

    companion object {
        const val ext = ".ngmd"
    }

    /**
     * Map scale read/write property.
     */
    var scale: Double
        get() = API.mapGetScaleInt(id)
        set(value) {
            API.mapSetScaleInt(id, value)
        }

    /**
     * Map center read/write property. The point coordinates should be geographic, not screen.
     */
    var center: Point
        get() = API.mapGetCenterInt(id)
        set(newPoint) {
            API.mapSetCenterInt(id, newPoint.x, newPoint.y)
        }

    /**
     * Map layer count readonly property.
     */
    val layerCount: Int get() = API.mapLayerCountInt(id)

    /**
     * Map extent read/write property.
     */
    var extent: Envelope
        get() = API.mapGetExtentInt(id, Constants.Map.epsg)
        set(value) {
            API.mapSetExtentInt(id, value)
        }

    /**
     * @enum Map selection style types. In map can be configured styles for point, line and polygon (and multi...) layers.
     * The styles are common for all layers.
     */
    enum class SelectionStyleType(val code: Int) {
        UNKNOWN(0),
        POINT(1),   /**< point */
        LINE(2),    /**< linestring */
        FILL(3);    /**< polygon */

        companion object {
            fun from(value: Int): SelectionStyleType {
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
     *  Map drawing state enum
     */
    enum class DrawState(val code: Int) {
        NORMAL(1),      /**< NORMAL: normal draw. Only new tiles will be filled with data to draw. */
        REDRAW(2),      /**< REDRAW: all cached data will be drop. Tiles deleted from memory. All tiles in screen will be filled with data to draw. */
        REFILL(3),      /**< REFILL: all tiles will be mark need to fill with data to draw. */
        PRESERVED(4),   /**< PRESERVED: just update scree with cached data. */
        NOTHING(5);     /**< NOTHING: no draw operation. */

        companion object {
            fun from(value: Int): DrawState {
                for (code in values()) {
                    if (code.code == value) {
                        return code
                    }
                }
                return NOTHING
            }
        }
    }

    /**
     * Close map. The map resources (layers, styles, etc.) will be freed.
     */
    fun close() {
        if(!API.mapCloseInt(id)) {
            printError("Close map failed. Error message: " + API.lastError())
        }
    }

    /**
     * Reopen map.
     *
     * @return true if map reopened successfully.
     */
    fun reopen() : Boolean {
        return API.mapReopenInt(id, path)
    }

    /**
     * Set map background.
     *
     * @param color background color
     */
    fun setBackgroundColor(color: RGBA) {
        bkColor = color
        val result = API.mapSetBackgroundColorInt(id, bkColor)
        if(!result) {
            printError("Failed set map background [${color.R}, ${color.G}, ${color.B}, ${color.A}]. Error message: " + API.lastError())
        }
    }

    /**
     * Set map viewport size. MapView executes this function on resize.
     *
     * @param width map width in pixels.
     * @param height map height in pixels.
     */
    fun setSize(width: Int, height: Int) {
        if(!API.mapSetSizeInt(id, width, height, true)) {
            printError("Failed set map size $width x $height. Error message: " + API.lastError())
        }
    }

    /**
     * Save map.
     *
     * @return true if map saved successfully.
     */
    fun save() : Boolean {
        return API.mapSaveInt(id, path)
    }

    /**
     * Add layer to map.
     *
     * @param name layer name.
     * @param source layer datasource.
     * @return Layer class instance or null on error.
     */
    fun addLayer(name: String, source: Object) : Layer? {
        val position = API.mapCreateLayerInt(id, name, source.path)
        if(position == -1) {
            return null
        }
        return getLayer(position)
    }

    /**
     * Remove layer from map.
     *
     * @param layer Layer class instance.
     * @return True if delete succeeded.
     */
    fun deleteLayer(layer: Layer) : Boolean {
        return API.mapLayerDeleteInt(id, layer.handle)
    }

    /**
     * Remove layer from map.
     *
     * @param position Layer index.
     * @return True if delete succeeded.
     */
    fun deleteLayer(position: Int) : Boolean {
        val deleteLayer = getLayer(position)
        if(deleteLayer != null) {
            return API.mapLayerDeleteInt(id, deleteLayer.handle)
        }
        return false
    }

    /**
     * Get map layer.
     *
     * @param position Layer index.
     * @return Layer class instance.
     */
    fun getLayer(position: Int) : Layer? {
        val layerHandler = API.mapLayerGetInt(id, position)
        if(layerHandler != 0L) {
            return Layer(layerHandler)
        }
        return null
    }

    /**
     *  Set map options.
     *
     *  @param options key-value dictionary. The supported keys are:
     *      - ZOOM_INCREMENT - Add integer value to zoom level correspondent to scale. May be negative.
     *      - VIEWPORT_REDUCE_FACTOR - Reduce view size on provided value. Make sense to decrease memory usage.
     *  @return True on success
     */
    fun setOptions(options: Map<String, String>) : Boolean = API.mapSetOptionsInt(id, options)

    /**
     * Set map extent limits. This limits prevent scroll out of this bounding box.
     *
     * @param minX minimum x coordinate.
     * @param minY minimum y coordinate.
     * @param maxX maximum x coordinate.
     * @param maxY maximum y coordinate.
     * @return True on success
     */
    fun setExtentLimits(minX: Double, minY: Double, maxX: Double, maxY: Double) : Boolean = API.mapSetExtentLimitsInt(id, minX, minY, maxX, maxY)

    /**
     * Reorder map layers.
     *
     * @param before Before layer class instance will moved layer insert.
     * @param moved Layer class instance to move.
     */
    fun reorder(before: Layer?, moved: Layer) {
        val beforeHandle = before?.handle ?: 0
        API.mapLayerReorderInt(id, beforeHandle, moved.handle)
    }

    /**
     * Search features in buffer around click/tap position.
     *
     * @param x position.
     * @param y position.
     * @param limit max count return features.
     * @return Array of Features.
     */
    fun identify(x: Float, y: Float, limit: Int = 0) : Array<Feature> {
        val out = mutableListOf<Feature>()

        val coordinate = API.mapGetCoordinateInt(id, x.toDouble(), y.toDouble())
        val distance = API.mapGetDistanceInt(id, Constants.Map.tolerance, Constants.Map.tolerance)
        val envelope = Envelope(
                coordinate.x - distance.x,coordinate.y - distance.y,
                coordinate.x + distance.x,coordinate.y + distance.y)

        for(index in 0 until layerCount) {
            val layer = getLayer(index)
            if(layer != null) {
                if(layer.visible) {
                    val layerFeatures = layer.identify(envelope, limit)
                    out.addAll(layerFeatures)
                }
            }
        }
        return out.toTypedArray()
    }

    /**
     * Highlight feature in map layers. Change the feature style to selection style. The selection style mast be set in map.
     *
     * @param features Features array. If array is empty the current highlighted features will get layer style and drawn not highlighted.
     */
    fun select(features: List<Feature> = listOf()) {
        var env = Envelope()

        for( index in 0 until layerCount) {
            val layer = getLayer(index)
            if(layer != null) {
                if(layer.visible) {
                    val ds = layer.dataSource as? FeatureClass
                    if(ds != null) {
                        val lf = mutableListOf<Feature>()
                        for(feature in features) {
                            if(feature.table.isSame(ds)) {
                                lf.add(feature)
                                val geomEnvelope = feature.geometry?.envelope
                                if(geomEnvelope != null) {
                                    env.merge(geomEnvelope)
                                }
                            }
                        }
                        layer.select(lf)
                    }
                }
            }
        }
        if(!env.isInit()) {
            env = Envelope(-1.0,-1.0,1.0,1.0)
        }
        API.mapInvalidateInt(id, env)
    }

    /**
     * Get layer by feature belongs the datasource of correspondent layer.
     *
     * @param feature Feature belongs the datasource of correspondent layer.
     * @return Layer class instance or null.
     */
    fun getLayerForFeature(feature: Feature) : Layer? {
        for(index in 0 until layerCount) {
            val layer = getLayer(index)
            if(layer != null) {
                val ds = layer.dataSource as? FeatureClass
                if(ds != null) {
                    if(ds.isSame(feature.table)) {
                        return layer
                    }
                }
            }
        }
        return null
    }

    /**
     * Invalidate part of the map.
     *
     * @param extent Extent to invalidate
     */
    fun invalidate(extent: Envelope) {
        API.mapInvalidateInt(id, extent)
    }

     /**
     * Get selection style
     *
     * @param type Style type
     * @return Json object with style
     */
    fun selectionStyle(type: SelectionStyleType) : JsonObject {
        return JsonObject(API.mapGetSelectionStyleInt(id, type.code))
    }

    /**
     * Get selection style name
     *
     * @param type Style type
     * @return Style name string
     */
    fun selectionStyleName(type: SelectionStyleType) : String {
        return API.mapGetSelectionStyleNameInt(id, type.code)
    }

    /**
     * Set selection style
     *
     * @param style Json object with style. See Layer.style
     * @param type Selection style type
     * @return True on success.
     */
    fun setSelectionStyle(style: JsonObject, type: SelectionStyleType) : Boolean {
        return API.mapSetSelectionsStyleInt(id, type.code, style.handle)
    }

    /**
     * Set selection style name
     *
     * @param name Style name. See Layer.styleName
     * @param type Selection style type
     * @return True on success.
     */
    fun setSelectionStyleByName(name: String, type: SelectionStyleType) : Boolean {
        return API.mapSetSelectionStyleNameInt(id, type.code, name)
    }

    /**
     * Get map overlay
     *
     * @param type Overlay type.
     * @return Overlay class instance or null.
     */
    fun getOverlay(type: Overlay.Type) : Overlay? {
        return when (type) {
            Overlay.Type.EDIT -> EditOverlay(this)
            Overlay.Type.LOCATION -> LocationOverlay(this)
            else -> null
        }
    }

    /**
     * Add iconset to map. The iconset is square image 256 x 256 or 512 x 512 pixel with icons in it.
     *
     * @param name Iconset name.
     * @param path Path to image if file system.
     * @param move If true the image will be deleted after successfully added to map document.
     * @return True on success.
     */
    fun addIconSet(name: String, path: String, move: Boolean) : Boolean {
        return API.mapIconSetAddInt(id, name, path, move)
    }

    /**
     * Remove iconset from map.
     *
     * @param name Iconset name.
     * @return True on success.
     */
    fun removeIconSet(name: String) : Boolean {
        return API.mapIconSetRemoveInt(id, name)
    }

    /**
     * Validate iconset exists in map.
     *
     * @param name Iconset name.
     * @return True if exists.
     */
    fun isIconSetExists(name: String) : Boolean {
        return API.mapIconSetExistsInt(id, name)
    }

    // Private
    internal fun draw(state: DrawState, callback: ((status: StatusCode, complete: Double, message: String) -> Boolean)) {
        val result = API.mapDrawInt(id, state, callback)
        if(!result) {
            printError("Failed draw map. Error code: " + API.lastError())
        }
    }

    internal fun zoomIn(multiply: Double = 2.0) {
        val scale = API.mapGetScaleInt(id) * multiply
        API.mapSetScaleInt(id, scale)
    }

    internal fun zoomOut(multiply: Double = 2.0) {
        val scale = API.mapGetScaleInt(id) / multiply
        API.mapSetScaleInt(id, scale)
    }

    internal fun pan(w: Double, h: Double) {
        val offset = API.mapGetDistanceInt(id, w, h)
        val currentCenter = API.mapGetCenterInt(id)
        currentCenter.x -= offset.x
        currentCenter.y -= offset.y

        API.mapSetCenterInt(id, currentCenter.x, currentCenter.y)
    }

    internal fun setCenterAndZoom(w: Double, h: Double, multiply: Double = 2.0) {
        val scale = API.mapGetScaleInt(id) * multiply
        val pos = API.mapGetCoordinateInt(id, w, h)

        API.mapSetScaleInt(id, scale)
        API.mapSetCenterInt(id, pos.x, pos.y)
    }

    internal fun getExtent(srs: Int) : Envelope {
        return API.mapGetExtentInt(id, srs)
    }
}
