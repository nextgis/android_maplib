/*
 * Project:  NextGIS Tracker
 * Purpose:  Software tracker for nextgis.com cloud
 * Author:   Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
 * Copyright (c) 2018-2019 NextGIS <info@nextgis.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.GnssStatus
import android.location.GpsStatus
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.preference.PreferenceManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.nextgis.maplib.*
import java.io.Serializable
import java.lang.ref.WeakReference
import java.text.DateFormat.getDateInstance
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

private const val DEFAULT_UPDATE_LOCATION_POINTS = 17 // Any value < 30.


/**
 * Tracker delegate protocol. Correspondent functions will be executed on tracker service events.
 */
interface TrackerDelegate : Serializable {
    fun onLocationChanged(location: Location) {}
    fun onStatusChanged(status: TrackerService.Status, trackName: String, trackStartTime: Date) {}
}

/**
 * Service to get location information and store it into trackers table of current store. Also service notifies
 * listeners on location events via local broadcast messages.
 */
class TrackerService : Service() {

    private var mNotificationManager: NotificationManager? = null
    private var mStatus = Status.UNKNOWN
    private var mTrackName = ""
    private var mTrackStartTime = Date()
    private var mStartNewTrack = true
    private var mSecondPointInTrack = false
    private var mOpenIntent: Intent? = null
    private var mPointsAdded = 0
    private var mTracksTable: Track? = null
    private var mUpdateOnLocationPoints = DEFAULT_UPDATE_LOCATION_POINTS
    private var mNotifiers = mutableListOf<WeakReference<TrackerDelegate>>()

    enum class Command(val code: String) {
        UNKNOWN("com.nextgis.tracker.UNKNOWN"),
        START("com.nextgis.tracker.START"),
        STOP("com.nextgis.tracker.STOP")
    }

    enum class Status(val code: Int) {
        UNKNOWN(0),
        RUNNING(1),
        STOPPED(2)
    }

    enum class MessageType(val code: String) {
        PROCESS_LOCATION_UPDATES("com.nextgis.tracker.PROCESS_LOCATION_UPDATES")
    }


    private var mPowerManager: PowerManager? = null
    private var mWakeLock: PowerManager.WakeLock? = null

    private var mDivTrackByDay = true
    private var mLostFixTime: Long = 30L * Constants.millisecondsInSecond

    private var mLocationManager: LocationManager? = null
    private var mSatelliteCount = 0
    /** Last known location or null. */
    private var mCurrentLocation: Location? = null
    private val mGpsLocationListener = object : LocationListener {

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
//            printMessage("onStatusChanged: provider: $provider, status: $status")
        }

        override fun onProviderEnabled(provider: String?) {
//            printMessage("onProviderEnabled: provider: $provider")
        }

        override fun onProviderDisabled(provider: String?) {
//            printMessage("onProviderDisabled: provider: $provider")
        }

        override fun onLocationChanged(location: android.location.Location) {
            processLocationChanges(location)
        }
    }

    // See: https://codelabs.developers.google.com/codelabs/background-location-updates-android-o/index.html
    private val mBroadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MessageType.PROCESS_LOCATION_UPDATES.code -> {
                    if(intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
                        val location = intent.extras?.get(LocationManager.KEY_LOCATION_CHANGED) as? android.location.Location
                        if(location != null) {
                            processLocationChanges(location)
                        }
                    }
                }
            }
        }
    }

    private fun processLocationChanges(location: android.location.Location) {
//        printMessage("onLocationChanged: location: $location")
        val satelliteInLoc = location.extras.getInt("satellites")
        if(satelliteInLoc > 0) {
            mSatelliteCount = satelliteInLoc
//            printMessage("onLocationChanged: Satellite count: $mSatelliteCount")
        }

        val thisLocation = Location(location, mSatelliteCount)

        if(isBetterLocation(thisLocation, mCurrentLocation)) {
            setNewLocation(thisLocation)
        }
    }

    private fun getPendingIntent() : PendingIntent {
        val intent = Intent()
        intent.action = MessageType.PROCESS_LOCATION_UPDATES.code
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private val mGnssStatusListener = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var satelliteCount = 0
            for(index in 0 until status.satelliteCount) {
                if (status.usedInFix(index)) {
                    satelliteCount++
                }
            }

            if(satelliteCount > 0) {
                mSatelliteCount = satelliteCount
            }

//            printMessage("onSatelliteStatusChanged: Satellite count: $mSatelliteCount")
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private val mGpsStatusListener = GpsStatus.Listener { event ->
        if(event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
            var satelliteCount = 0

            if (checkPermission(this@TrackerService, Manifest.permission.ACCESS_FINE_LOCATION)) {
                val satellites = mLocationManager?.getGpsStatus(null)?.satellites
                if(satellites != null) {
                    for (sat in satellites) {
                        if (sat.usedInFix()) {
                            satelliteCount++
                        }
                    }
                }
            }

            if(satelliteCount > 0) {
                mSatelliteCount = satelliteCount
            }

//                printMessage("onGpsStatusChanged: Satellite count: $mSatelliteCount")
        }
    }

    private fun setNewLocation(location: Location) {
        if(mTracksTable == null) {
            printWarning("Tracks table is not opened. Try to get it.")
            val store = API.getStore(API.getLastStoreName())
            mTracksTable = store?.trackTable()
            if(mTracksTable == null) {
                printError("Tracks table is null")
                return
            }
        }
        var newSegment = false
        if(mCurrentLocation != null) {
            // For a long time delay create new track segment.
            if(location.time - mCurrentLocation!!.time > mLostFixTime) {
                newSegment = true
            }
        }

        mCurrentLocation = location
        if(!mSecondPointInTrack && !mStartNewTrack && mDivTrackByDay) {
            val newTrackName = getTrackName()
            if(newTrackName != mTrackName) {
                mTrackName = newTrackName
                mTrackStartTime = Date()
                mStartNewTrack = true
            }
        }

        // Add location to DB
        if(mTracksTable?.addPoint(mTrackName, location, mStartNewTrack, newSegment) == false) {
            printError("Add track point failed. " + API.lastError())
            return
        }

//            printMessage("Track point added [$location]")
        mPointsAdded++

        // Send location to listeners
        for(notify in mNotifiers) {
            notify.get()?.onLocationChanged(location)
        }

        // If get second point after start new track - send status broadcast to update tracks list
        if(mSecondPointInTrack) {
            mSecondPointInTrack = false
            mPointsAdded = 0
            status()
        }

        if(mStartNewTrack) {
            updateNotify()
            mSecondPointInTrack = true
            mStartNewTrack = false
        }

        if(mPointsAdded > mUpdateOnLocationPoints) {
            updateNotify() // Update track time length
            mPointsAdded = 0
            status()
        }
    }

    override fun onCreate() {
        super.onCreate()

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mPowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        mDivTrackByDay = sharedPref.getBoolean("divTracksByDay", true)

        // Start syncing with NGW
        API.init(this@TrackerService)
        val store = API.getStore(API.getLastStoreName())
        mTracksTable = store?.trackTable()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Get or create tracks table.
        if(intent?.hasExtra("STORE_NAME") == true && mTracksTable == null) {
            val storeName = intent.getStringExtra("STORE_NAME")
            val store = API.getStore(storeName)
            mTracksTable = store?.trackTable()
        }

        when(intent?.action) {
            Command.START.code -> {
                mOpenIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT)
                mOpenIntent?.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

                val delegate = intent.getSerializableExtra("com.nextgis.tracker.DELEGATE") as? TrackerDelegate
                addDelegate(delegate)

                start()
            }
            Command.STOP.code -> stop()
        }


        // Start and stop immediately, as Android needs to call startForeground after service started.
        if(mStatus != Status.RUNNING) {
            prepareStart()
            stopForeground(true)
        }

        return START_STICKY
    }

    fun addDelegate(delegate: TrackerDelegate?) {
        if(delegate == null) {
            return
        }
        for(notify in mNotifiers) {
            if(notify.get() == delegate) {
                return
            }
        }
        mNotifiers.add(WeakReference(delegate))
    }

    fun removeDelegate(delegate: TrackerDelegate?) {
        if (delegate == null) {
            return
        }
        for(notify in mNotifiers) {
            if(notify.get() == delegate) {
                mNotifiers.remove(notify)
                return
            }
        }
    }

    override fun onBind(intent: Intent) = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService() = this@TrackerService
    }

    override fun onDestroy() {
        // The service is no longer used and is being destroyed
        stop()
        printMessage("Destroy tracking service")
    }

    @Suppress("DEPRECATION")
    private fun stop() {
        if(mStatus != Status.RUNNING) {
            status()
            return
        }

        mStatus = Status.STOPPED
        status()

        stopForeground(true)
        // Service will stopped by system
        // stopSelf()

        // Stop tracking
        if(checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationManager?.removeUpdates(getPendingIntent())
                unregisterReceiver(mBroadCastReceiver)
                mLocationManager?.unregisterGnssStatusCallback(mGnssStatusListener)
            }
            else {
                mLocationManager?.removeUpdates(mGpsLocationListener)
                mLocationManager?.removeGpsStatusListener(mGpsStatusListener)
            }
        }

        if(checkPermission(this, Manifest.permission.WAKE_LOCK)) {
            mWakeLock?.let { if (it.isHeld) it.release() }
        }

        printMessage("Stop tracking service")
    }

    private fun getTrackName() : String {

        val formatter = getDateInstance()

        // Get existing names
//        val tracksInfo = mTracksTable?.getTracks()
//        return getUniqueName(date, 0, tracksInfo)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            formatter.format(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
        }
        else {
            formatter.format(Date())
        }
    }

// Not needed
//    private fun getUniqueName(name: String, add: Int, tracksInfo: Array<TrackInfo>?) : String {
//        var newName = name
//        if(add != 0) {
//            newName = "$name [$add]"
//        }
//        if(tracksInfo != null) {
//            for (trackInfo in tracksInfo) {
//                if (trackInfo.name == newName) {
//                    return getUniqueName(name, add + 1, tracksInfo)
//                }
//            }
//        }
//        return newName
//    }

    private fun getNotification() : NotificationCompat.Builder {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                // If earlier version channel ID is not used
                ""
            }

        val notificationBuilder = NotificationCompat.Builder(this, channelId )

        val intent = Intent(this, TrackerService::class.java)
        intent.action = Command.STOP.code
        val stopIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        var diff = (Date().time - mTrackStartTime.time) / (60 * Constants.millisecondsInSecond)
        val minutes = diff % 60
        diff /= 60
        val hours = diff % 24
        val days = diff / 24

        val writingStr = when {
            days > 0 -> getString(R.string.track_is_writing_long).format(days, hours, minutes)
            hours > 0 -> getString(R.string.track_is_writing_short).format(hours, minutes)
            minutes > 0 -> getString(R.string.track_is_writing_supershort).format(minutes)
            else -> ""
        }


        var notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_walking)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(getString(R.string.track_name).format(mTrackName))
            .setContentText(getString(R.string.track_is_writing).format(writingStr))
            .addAction(android.R.drawable.ic_media_pause, getText(R.string.stop), stopIntent)

        if(mOpenIntent != null) {
            val openIntent = PendingIntent.getActivity(this, 0, mOpenIntent, 0)
            notification = notification
                .setContentIntent(openIntent)
                .addAction(android.R.drawable.ic_menu_view, getText(R.string.open), openIntent)
        }

        return notification
    }

    private fun prepareStart() {
        startForeground(Constants.foregroundId, getNotification().build())
    }

    private fun updateNotify() {
        mNotificationManager?.notify(Constants.foregroundId, getNotification().build())
    }

    @Suppress("DEPRECATION")
    private fun start() {
        if(mStatus == Status.RUNNING) {
            status()
            return
        }

        if(checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            printMessage("Start tracking service")

            mStatus = Status.RUNNING
            mTrackName = getTrackName()
            mTrackStartTime = Date()
            mStartNewTrack = true
            status()

            prepareStart()

            // Start tracking
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
            val minTime = sharedPref.getInt("timeInterval", 1).toLong() * Constants.millisecondsInSecond
            val minDist = sharedPref.getInt("minDistance", 10).toFloat()

            if(minTime > 0) {
                mLostFixTime = minTime * 15
            }



            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val filter = IntentFilter()
                filter.addAction(MessageType.PROCESS_LOCATION_UPDATES.code)
                registerReceiver(mBroadCastReceiver, filter)
                mLocationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDist, getPendingIntent())
                mLocationManager?.registerGnssStatusCallback(mGnssStatusListener)
            }
            else {
                mLocationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDist, mGpsLocationListener, mainLooper)
                mLocationManager?.addGpsStatusListener(mGpsStatusListener)
            }
        }

        if(checkPermission(this, Manifest.permission.WAKE_LOCK)) {
            mWakeLock = mPowerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${Constants.tag}:TrackerService")
//            mPowerManager.locationPowerSaveMode = PowerManager.LOCATION_MODE_FOREGROUND_ONLY
            mWakeLock?.acquire(1000000)
        }
    }

    fun status() {
        for(notify in mNotifiers) {
            notify.get()?.onStatusChanged(mStatus, mTrackName, mTrackStartTime)
        }

        // printMessage("Tracking service status: " + if(mStatus == Status.RUNNING) "running" else "stopped" )

        if(mCurrentLocation != null) {
            for(notify in mNotifiers) {
                notify.get()?.onLocationChanged(mCurrentLocation!!)
            }
        }
    }

    fun update() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        mDivTrackByDay = sharedPref.getBoolean("divTracksByDay", true)

        if(mStatus == Status.RUNNING) {
            stop()
            start()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "com.nextgis.tracker"
        val channelName = "NextGIS Tracker"
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        chan.importance = NotificationManager.IMPORTANCE_LOW
        chan.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        chan.lightColor = Color.TRANSPARENT
        mNotificationManager?.createNotificationChannel(chan)
        return channelId
    }
}
