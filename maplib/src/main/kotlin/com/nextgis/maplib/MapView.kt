/*
 * Project: NextGIS Mobile SDK
 * Author:  Dmitry Baryshnikov, dmitry.baryshnikov@nextgis.com
 *
 * Created by Dmitry Baryshnikov on 02.10.18 14:01.
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

import android.Manifest
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import java.lang.ref.WeakReference
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs


/**
 * Gesture delegate protocol. Correspondent functions will be executed on gesture events.
 */
interface GestureDelegate {
    fun onSingleTap(event: MotionEvent?) {}
    fun onDoubleTap(event: MotionEvent?) {}
    fun onPanGesture(distanceX: Float, distanceY: Float) {}
    fun onPinchGesture(event: ScaleGestureDetector?) {}
}

/**
 * Location delegate protocol. Correspondent functions will be executed on location events.
 */
interface LocationDelegate {
    fun onLocationChanged(location: Location) {}
    fun onLocationStop() {}
}

/**
 * Map drawing delegate protocol. Correspondent functions will be executed on map draw events.
 */
interface MapViewDelegate {
    fun onMapDrawFinished() {}
    fun onMapDraw(percent: Double) {}
}

private class MapTimerRunnable(private val drawState: MapDocument.DrawState, private val view: MapView, private val viewRef: WeakReference<MapView> = WeakReference(view)) : TimerTask() {
    override fun run() {
        viewRef.get()?.draw(drawState)
    }
}

private class MapRenderer(private val mapView: MapView?, private val mapViewRef: WeakReference<MapView> = WeakReference<MapView>(mapView)) : GLSurfaceView.Renderer {

    override fun onDrawFrame(p0: GL10?) {
        val mapView = mapViewRef.get()
        if(mapView != null) {
            printMessage("MapRenderer:onDrawFrame")
            mapView.map?.draw(mapView.drawState, mapView::drawingProgressFunc)
            mapView.drawState = MapDocument.DrawState.PRESERVED
        }
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        val mapView = mapViewRef.get()
        if(mapView != null) {
            mapView.map?.setSize(width, height)
            mapView.refresh()
        }
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
//        val mapView = mapViewRef.get()
//        if(mapView != null) {
//            mapView.map?.draw(MapDocument.DrawState.NORMAL, mapView::drawingProgressFunc)
//            mapView.drawState = MapDocument.DrawState.PRESERVED
//        }
    }
}

/**
 * Map view with GL rendering.
 *
 * MapView holds MapDocument and renders it.
 */
open class MapView : GLSurfaceView {

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    internal var map: MapDocument? = null
    internal var drawState: MapDocument.DrawState = MapDocument.DrawState.PRESERVED
    private val timer = Timer()
    private var timerRunner: MapTimerRunnable? = null
    private var timerDrawState = MapDocument.DrawState.PRESERVED
    private var gestureDelegate = WeakReference<GestureDelegate>(null)
    private var locationDelegate = WeakReference<LocationDelegate>(null)
    private var mapViewDelegate = WeakReference<MapViewDelegate>(null)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var gestureDetector: GestureDetector? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null

    /** Last known location or null. */
    var currentLocation: Location? = null

    // Define a listener that responds to location updates
    private val networkLocationListener = object : LocationListener {
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        override fun onProviderEnabled(provider: String) {
        }

        override fun onProviderDisabled(provider: String) {
        }

        override fun onLocationChanged(location: android.location.Location) {
            val thisLoc = Location(location, 0)
            if(isBetterLocation(thisLoc, currentLocation)) {
                setNewLocation(thisLoc)
            }
            else if(currentLocation?.provider == LocationManager.GPS_PROVIDER) {
                // Disable WiFi if GPS is available.
                locationManager.removeUpdates(this)
            }
        }
    }

    private val gpsLocationListener = object : LocationListener {
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        override fun onProviderEnabled(provider: String) {
        }

        override fun onProviderDisabled(provider: String) {
        }

        override fun onLocationChanged(location: android.location.Location) {
            val thisLoc = Location(location, 0)
            if(isBetterLocation(thisLoc, currentLocation)) {
                setNewLocation(thisLoc)
            }
        }
    }

    private fun setNewLocation(location: Location) {
        currentLocation = location

        if(showLocation) {
            val position = transformFromGPS(location.longitude, location.latitude)
            val locationOverlay = map?.getOverlay(Overlay.Type.LOCATION) as? LocationOverlay
            if(locationOverlay != null) {
                locationOverlay.updateLocation(position, location.bearing, location.accuracy)
                draw(MapDocument.DrawState.PRESERVED)
            }

            printMessage("Location. Lat: ${location.latitude} Long: ${location.longitude} " +
                    "Alt: ${location.altitude} Dir: ${location.bearing}, Accuracy: ${location.accuracy}")

        }

        locationDelegate.get()?.onLocationChanged(location)
    }

    private val gestureListener = object : GestureDetector.OnGestureListener {

        override fun onShowPress(event: MotionEvent) { }

        override fun onSingleTapUp(p0: MotionEvent): Boolean {
            return true
        }

        override fun onDown(p0: MotionEvent): Boolean {
            return true
        }

        override fun onFling(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
            return true
        }

        override fun onScroll(event1: MotionEvent, event2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val x = distanceX.toDouble()
            val y = distanceY.toDouble()

            if(abs(x) > Constants.Sizes.minPanPix || abs(y) > Constants.Sizes.minPanPix) {
                pan(-x, -y)
            }

            gestureDelegate.get()?.onPanGesture(distanceX, distanceY)
            return true
        }

        override fun onLongPress(p0: MotionEvent) {

        }
    }

    private val doubleTapListener = object : GestureDetector.OnDoubleTapListener {
        override fun onDoubleTap(event: MotionEvent): Boolean {
            val x = event?.x?.toDouble() ?: 0.0
            val y = event?.y?.toDouble() ?: 0.0

            map?.setCenterAndZoom(x, y)
            draw(MapDocument.DrawState.PRESERVED)
            scheduleDraw(MapDocument.DrawState.NORMAL)

            gestureDelegate.get()?.onDoubleTap(event)

            return true
        }

        override fun onDoubleTapEvent(p0: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            gestureDelegate.get()?.onSingleTap(event)
            return true
        }

    }

    private val scaleGestureListener = object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScaleBegin(event: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScaleEnd(event: ScaleGestureDetector) {

        }

        override fun onScale(event: ScaleGestureDetector): Boolean {
            val scale = event?.scaleFactor?.toDouble() ?: 1.0

            map?.zoomIn(scale)
            draw(MapDocument.DrawState.PRESERVED)
            scheduleDraw(MapDocument.DrawState.NORMAL)

            gestureDelegate.get()?.onPinchGesture(event)

            return true
        }

    }

    /**
     * Show/hide current position on map. The location overlay must be set.
     */
    var showLocation: Boolean = false
        set(newVal) {
            if(newVal) {
                // start updating location
                if(checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, networkLocationListener)
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, gpsLocationListener)
                }
            }
            else {
                // stop updating location
                locationManager.removeUpdates(gpsLocationListener)
                locationManager.removeUpdates(networkLocationListener)
                locationDelegate.get()?.onLocationStop()
            }
            field = newVal
        }

    /**
     * Freeze map drawing read/write property.
     */
    var freeze: Boolean = true

    /**
     * Map scale read/write property.
     */
    var mapScale: Double
        get() = map?.scale ?: 0.0000015
        set(newScale) {
            map?.scale = newScale
        }

    /**
     * Map center in spatial reference coordinates read/write property.
     */
    var mapCenter: Point
        get() = map?.center ?: Point()
        set(newPoint) {
            map?.center = newPoint
        }

    /**
     * Map current extent read/write property.
     */
    var mapExtent: Envelope
        get() = map?.extent ?: Envelope()
        set(newExtent) {
            map?.extent = newExtent
        }

    override fun onDetachedFromWindow() {
        API.mapDrawRemoveCallbackInt()
        API.removeMapView(this)
        super.onDetachedFromWindow()
    }

    /**
     * Set map class to view.
     *
     * @param map Map class instance.
     */
    fun setMap(map: MapDocument) {
        this.map = map
//        map.setSize(width, height)
//        printMessage("Map set size w: $width x h: $height")

        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 0, 16, 8)
//        holder.setFormat(PixelFormat.TRANSLUCENT)
        API.addMapView(this)

        setRenderer(MapRenderer(this))
        renderMode = RENDERMODE_WHEN_DIRTY
        preserveEGLContextOnPause = true

        draw(MapDocument.DrawState.NORMAL)
    }

    internal fun draw(state: MapDocument.DrawState) {
        drawState = state
        if (!freeze) {
            printMessage("MapView:requestRender")
            requestRender()
        }
    }

    /**
     * Cancel drawing process. Default return value is false. Using override function can cancel drawing on some cases.
     *
     * @return True to cancel drawing.
     */
    fun cancelDraw() : Boolean {
        return false
    }

    internal fun drawingProgressFunc(status: StatusCode, complete: Double, message: String) : Boolean {
        printMessage("MapView:drawingProgressFunc $status - $message")
        if(status == StatusCode.FINISHED) {
            mapViewDelegate.get()?.onMapDrawFinished()
            return true
        }

        scheduleDraw(MapDocument.DrawState.PRESERVED)
        mapViewDelegate.get()?.onMapDraw(complete)
        return cancelDraw()
    }

    /**
     * Refresh map.
     *
     * @param normal If true just refresh view, otherwise refill all map tiles.
     */
    fun refresh(normal: Boolean = true) {
        if(normal) {
            draw(MapDocument.DrawState.NORMAL)
        }
        else {
            draw(MapDocument.DrawState.REFILL)
        }
    }

    /**
     * Zoom in.
     *
     * @param multiply Multiply factor. Default is 2.
     */
    fun zoomIn(multiply: Double = 2.0) {
        map?.zoomIn(multiply)
        draw(MapDocument.DrawState.PRESERVED)
        scheduleDraw(MapDocument.DrawState.NORMAL)
    }

    /**
     * Zoom out.
     *
     * @param multiply Multiply factor. Default is 2.
     */
    fun zoomOut(multiply: Double = 2.0) {
        map?.zoomOut(multiply)
        draw(MapDocument.DrawState.PRESERVED)
        scheduleDraw(MapDocument.DrawState.NORMAL)
    }

    /**
     * Center map at spatial reference coordinates and redraw it.
     *
     * @param coordinate New center coordinates.
     */
    fun centerMap(coordinate: Point) {
        map?.center = coordinate
        draw(MapDocument.DrawState.PRESERVED)
        scheduleDraw(MapDocument.DrawState.NORMAL)
    }

    /**
     * Center map in current location. If current location is null, nothing happened.
     */
    fun centerInCurrentLocation() {
        if(currentLocation == null) {
            return
        }
        val newCenter = transformFromGPS(currentLocation?.longitude ?: 0.0,
                currentLocation?.latitude ?: 0.0)
        centerMap(newCenter)
    }

    /**
     * Invalidate part of the map. The refresh function invalidates all visible screen.
     *
     * @param envelope Envelope to invalidate.
     */
    fun invalidate(envelope: Envelope) {
        map?.invalidate(envelope)
        scheduleDraw(MapDocument.DrawState.PRESERVED, 700L)
    }

    /**
     * Pan map to specific screen shift.
     *
     * @param w horizontal pixel shift
     * @param h vertical pixel shift.
     */
    fun pan(w: Double, h: Double) {
        map?.pan(w, h)
        draw(MapDocument.DrawState.PRESERVED)
        scheduleDraw(MapDocument.DrawState.NORMAL, 700L)
    }

    private fun transformFromGPS(x: Double, y: Double) : Point {
        val ct = CoordinateTransformation.new(4326, 3857)
        return ct.transform(Point(x, y))
    }

    /**
     * Schedule map redraw.
     *
     * @param drawState Draw state value. See Map.DrawState values.
     * @param timeInterval Time interval in seconds.
     */
    fun scheduleDraw(drawState: MapDocument.DrawState, timeInterval: Long = Constants.refreshTime) {
        printMessage("MapView:scheduleDraw")
        timerDrawState = drawState
        timerRunner = MapTimerRunnable(drawState, this)
        timer.schedule(timerRunner, timeInterval)
    }

    /**
     * Register gesture delegate function.
     *
     * @param delegate delegate function.
     */
    fun registerGestureRecognizers(delegate: GestureDelegate) {

        gestureDetector = GestureDetector(context, gestureListener)
        gestureDetector?.setOnDoubleTapListener(doubleTapListener)
        scaleGestureDetector = ScaleGestureDetector(context, scaleGestureListener)

        gestureDelegate = WeakReference(delegate)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val res = scaleGestureDetector?.onTouchEvent(event) == true
        if(scaleGestureDetector?.isInProgress == true)
            return res
        return if (gestureDetector?.onTouchEvent(event) == true) {
            true
        }
        else {
            super.onTouchEvent(event)
        }
    }

    /**
     * Register location delegate function.
     *
     * @param delegate location delegate function.
     */
    fun registerLocation(delegate: LocationDelegate) {
        locationDelegate = WeakReference(delegate)
    }

    /**
     * Register map drawing delegate function.
     *
     * @param delegate map drawing delegate function.
     */
    fun registerView(delegate: MapViewDelegate) {
        mapViewDelegate = WeakReference(delegate)
    }

    /**
     * Get current extent in specified spatial reference system by EPSG code.
     *
     * @param srs EPSG code.
     * @return Envelope in specified spatial reference system.
     */
    fun getExtent(srs: Int) : Envelope {
        return map?.getExtent(srs) ?: Envelope()
    }


}

/*
/// Map view with vector feature editing functions.
public class MapViewEdit : MapView {

    var editMode: Bool = false
    var editOverlay: EditOverlay? = nil
    var editMoveSelectedPoint: Bool = false
    var beginTouchLocation: CGPoint? = nil

    /// Is in edit mode or not.
    public var isEditMode: Bool {
        get {
            return editMode
        }

        set {
            editMode = newValue
            if(editMode) {
                editOverlay?.visible = true
            }
            else {
                editOverlay?.visible = false
                editMoveSelectedPoint = false
            }
        }
    }

    override public func setMap(map: Map) {
        super.setMap(map: map)
        editOverlay = map.getOverlay(type: .EDIT) as? EditOverlay
    }

    override public func onPanGesture(sender: UIPanGestureRecognizer) {

        if(editMode) {
            if sender.state == .began {
                let x = Double(beginTouchLocation?.x ?? 10000.0)
                let y = Double(beginTouchLocation?.y ?? 10000.0)
                printMessage("Edit mode begin pan x: \(x), y: \(y)")
                if let touchResult = editOverlay?.touch(down: x, y: y) {
                editMoveSelectedPoint = touchResult.pointId != -1
            }
                gestureDelegate?.onPanGesture(sender: sender)
            } else if sender.state == .changed {
                if editMoveSelectedPoint {
                    let position = sender.location(in: sender.view)
                    let x = Double(position.x)
                    let y = Double(position.y)

                    _ = editOverlay?.touch(move: x, y: y)
                    draw(.PRESERVED)
                    gestureDelegate?.onPanGesture(sender: sender)
                }
            } else if sender.state == .ended {
                if editMoveSelectedPoint {
                    let position = sender.location(in: sender.view)
                    let x = Double(position.x)
                    let y = Double(position.y)

                    _ = editOverlay?.touch(up: x, y: y)
                    draw(.PRESERVED)
                    editMoveSelectedPoint = false

                    gestureDelegate?.onPanGesture(sender: sender)
                    return
                }
            }
        }

        if !editMoveSelectedPoint {
            super.onPanGesture(sender: sender)
        }
    }

    override public func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        if let touch: UITouch = touches.first {

            if (touch.view == self) {
                beginTouchLocation = touch.location(in: self)
            }
        }

        super.touchesBegan(touches, with: event)
    }

    /// Undo edit operation.
    public func undoEdit() {
        if editOverlay?.undo() ?? false {
            draw(.PRESERVED)
        }
    }

    /// Redo edit operation.
    public func redoEdit() {
        if editOverlay?.redo() ?? false {
            draw(.PRESERVED)
        }
    }

    /// Add geometry part.
    public func addGeometryPart() {
        if editOverlay?.addGeometryPart() ?? false {
            draw(.PRESERVED)
        }
    }

    /// Add geometry point.
    public func addGeometryPoint() {
        if editOverlay?.addGeometryPoint() ?? false {
            draw(.PRESERVED)
        }
    }

    /// Add geometry point in specified coordinates.
    ///
    /// - Parameter coordinates: Point coordinates.
    public func addGeometryPoint(with coordinates: Point) {
        if editOverlay?.addGeometryPoint(with: coordinates) ?? false {
            draw(.PRESERVED)
        }
    }

    /// Deletes selected geometry part
    ///
    /// - Returns: delete result type value. EditOverlay.DeleteResultType.
    public func deleteGeometryPart() -> EditOverlay.DeleteResultType {
        let result = editOverlay?.deleteGeometryPart() ?? .NON_LAST
        draw(.PRESERVED)
        return result
    }

    /// Delete point from geometry.
    ///
    /// - Returns: delete result type value. EditOverlay.DeleteResultType.
    public func deleteGeometryPoint() -> EditOverlay.DeleteResultType {
        let result = editOverlay?.deleteGeometryPoint() ?? .NON_LAST
        draw(.PRESERVED)
        return result
    }

    /// Delete the whole geometry.
    public func deleteGeometry() {
        if editOverlay?.deleteGeometry() ?? false {
            draw(.PRESERVED)
        }
    }

    /// Add hole to geometry.
    public func addGeometryHole() {
        if editOverlay?.addGeometryHole() ?? false {
            draw(.PRESERVED)
        }
    }

    /// Delete geometry hole.
    ///
    /// - Returns: delete result type value. EditOverlay.DeleteResultType.
    public func deleteGeometryHole() -> EditOverlay.DeleteResultType {
        let result = editOverlay?.deleteGeometryHole() ?? .NON_LAST
        draw(.PRESERVED)
        return result
    }
}
*/