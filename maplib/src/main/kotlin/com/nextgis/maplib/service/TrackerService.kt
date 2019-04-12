/*
 * Project:  NextGIS Tracker
 * Purpose:  Software tracker for nextgis.com cloud
 * Author:   Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nextgis.maplib.*
import java.text.DateFormat.getDateInstance
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

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

    enum class Command(val code: String) {
        UNKNOWN("com.nextgis.tracker.UNKNOWN"),
        STATUS("com.nextgis.tracker.STATUS"),
        START("com.nextgis.tracker.START"),
        STOP("com.nextgis.tracker.STOP"),
        UPDATE("com.nextgis.tracker.UPDATE")
    }

    enum class Status(val code: Int) {
        UNKNOWN(0),
        RUNNING(1),
        STOPPED(2)
    }

    enum class MessageType(val code: String) {
        STATUS_CHANGED("com.nextgis.tracker.STATUS_CHANGED"),
        LOCATION_CHANGED("com.nextgis.tracker.LOCATION_CHANGED")
    }

    private var mDivTrackByDay = true
    private var mLostFixTime: Long = 30L * Constants.millisecondsInSecond

    private var mLocationManager: LocationManager? = null
    private var mSatelliteCount = 0
    /** Last known location or null. */
    private var mCurrentLocation: Location? = null
    private val mGpsLocationListener = object : LocationListener {

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        override fun onProviderEnabled(provider: String?) {
        }

        override fun onProviderDisabled(provider: String?) {
        }

        override fun onLocationChanged(location: Location) {
            val satelliteInLoc = location.extras.getInt("satellites")
            if(satelliteInLoc > 0) {
                mSatelliteCount = satelliteInLoc
                printMessage("onLocationChanged: Satellite count: ${mSatelliteCount}")
            }
            if(isBetterLocation(location, mCurrentLocation)) {
                setNewLocation(location)
            }
        }
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

            printMessage("onSatelliteStatusChanged: Satellite count: ${mSatelliteCount}")
        }
    }

    @Suppress("DEPRECATION")
    private val mGpsStatusListener = object : GpsStatus.Listener {
        override fun onGpsStatusChanged(event: Int) {
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

                printMessage("onGpsStatusChanged: Satellite count: ${mSatelliteCount}")
            }
        }
    }

    private fun setNewLocation(location: Location) {
        var newSegment = false
        if(mCurrentLocation != null) {
            // For a long time delay create new track segment.
            if(location.time - mCurrentLocation!!.time > mLostFixTime) {
                newSegment = true
            }
        }

        mCurrentLocation = location
        if(mDivTrackByDay) {
            val newTrackName = getTrackName()
            if(newTrackName != mTrackName) {
                mTrackName = newTrackName
                mStartNewTrack = true
            }
        }

        // Add location to DB
        if(mTracksTable?.addPoint(mTrackName, location, mSatelliteCount, mStartNewTrack, newSegment) == false) {
            printError(API.lastError())
        }
        else {
            printMessage("Track point added [$location]")
            mPointsAdded++
        }
        // Send location to broadcast listeners
        val intent = Intent()
        intent.action = MessageType.LOCATION_CHANGED.code
        intent.putExtra("gps_location", location)
        intent.putExtra("satellites", mSatelliteCount)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        // If get second point after start new track - send status broadcast to update tracks list
        if(mSecondPointInTrack) {
            mSecondPointInTrack = false
            mPointsAdded = 0
            status()
        }

        if(mStartNewTrack) {
            mSecondPointInTrack = true
            mStartNewTrack = false
        }

        if(mPointsAdded > 120) {
            mPointsAdded = 0
            status()
        }
    }

    override fun onCreate() {
        super.onCreate()

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        mDivTrackByDay = sharedPref.getBoolean("divTracksByDay", true)

        // Start syncing with NGW
        val store = API.getStore(API.getLastStoreName())
        mTracksTable = store?.trackTable()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when(intent?.action) {
            Command.START.code -> {
                mOpenIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT)
                mOpenIntent?.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                start()
            }
            Command.STOP.code -> stop()
            Command.STATUS.code -> status()
            Command.UPDATE.code -> update()
        }

        // Start syncing with NGW
        if(intent?.hasExtra("STORE_NAME") == true && mTracksTable == null) {
            val storeName = intent.getStringExtra("STORE_NAME")
            val store = API.getStore(storeName)
            mTracksTable = store?.trackTable()
        }

        // Start and stop immediately, as a android needs to call startForeground after service started.
        if(mStatus != Status.RUNNING) {
            prepareStart()
            stopForeground(true)
        }

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        // The service is no longer used and is being destroyed
        stop()
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
            mLocationManager?.removeUpdates(mGpsLocationListener)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationManager?.unregisterGnssStatusCallback(mGnssStatusListener)
            }
            else {
                mLocationManager?.removeGpsStatusListener(mGpsStatusListener)
            }
        }
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

    private fun getUniqueName(name: String, add: Int, tracksInfo: Array<TrackInfo>?) : String {
        var newName = name
        if(add != 0) {
            newName = "$name [$add]"
        }
        if(tracksInfo != null) {
            for (trackInfo in tracksInfo) {
                if (trackInfo.name == newName) {
                    return getUniqueName(name, add + 1, tracksInfo)
                }
            }
        }
        return newName
    }

    private fun prepareStart() {
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

        var notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_walking)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(getString(R.string.track_name).format(mTrackName))
            .setContentText(getText(R.string.track_is_writing))
            .addAction(android.R.drawable.ic_media_pause, getText(R.string.stop), stopIntent)

        if(mOpenIntent != null) {
            val openIntent = PendingIntent.getActivity(this, 0, mOpenIntent, 0)
            notification = notification
                .setContentIntent(openIntent)
                .addAction(android.R.drawable.ic_menu_view, getText(R.string.open), openIntent)
        }

        startForeground(Constants.foregroundId, notification.build())
    }

    @Suppress("DEPRECATION")
    private fun start() {
        if(mStatus == Status.RUNNING) {
            status()
            return
        }

        if(checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            mStatus = Status.RUNNING
            mTrackName = getTrackName()
            mTrackStartTime = Date()
            mStartNewTrack = true
            status()

            prepareStart()

            // Start tracking
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
            val minTime = sharedPref.getInt("timeInterval", 10).toLong() * Constants.millisecondsInSecond
            val minDist = sharedPref.getInt("minDistance", 50).toFloat()

            if(minTime > 0) {
                mLostFixTime = minTime * 15
            }

            mLocationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDist, mGpsLocationListener)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationManager?.registerGnssStatusCallback(mGnssStatusListener)
            }
            else {
                mLocationManager?.addGpsStatusListener(mGpsStatusListener)
            }
        }
    }

    private fun status() {
        val intent = Intent()
        intent.action = MessageType.STATUS_CHANGED.code
        // You can also include some extra data.
        val isRunning: Boolean = mStatus == Status.RUNNING
        intent.putExtra("is_running", isRunning)
        intent.putExtra("name", mTrackName)
        intent.putExtra("start", mTrackStartTime)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        printMessage("Tracking service status: " + if(mStatus == Status.RUNNING) "running" else "stopped" )
    }

    private fun update() {
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
        chan.importance = NotificationManager.IMPORTANCE_NONE
        chan.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        chan.lightColor = Color.TRANSPARENT
        mNotificationManager?.createNotificationChannel(chan)
        return channelId
    }
}

