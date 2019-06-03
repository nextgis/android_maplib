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

package com.nextgis.maplib.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nextgis.maplib.Location
import com.nextgis.maplib.R
import com.nextgis.maplib.formatCoordinate
import com.nextgis.maplib.service.TrackerService
import kotlinx.android.synthetic.main.location_info.*

/**
 * Location information panel.
 */
class LocationInfoFragment : Fragment() {

    private val mBroadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                TrackerService.MessageType.LOCATION_CHANGED.code -> handleLocationChanged(intent)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.location_info, container, false)
        val ctx = context
        if (ctx != null) {
            val filter = IntentFilter()
            filter.addAction(TrackerService.MessageType.LOCATION_CHANGED.code)
            LocalBroadcastManager.getInstance(ctx).registerReceiver(mBroadCastReceiver, filter)
        }
        return view
    }

    override fun onDestroyView() {
        val ctx = context
        if (ctx != null) {
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(mBroadCastReceiver)
        }
        super.onDestroyView()
    }

    private fun handleLocationChanged(intent: Intent?) {
        val location = intent?.getParcelableExtra<Location>("gps_location")
        if(location != null) {
            locationIcon.setImageResource(R.drawable.ic_gps_fixed)

            if(location.hasAccuracy()) {
                accuracyText.text = getString(R.string.location_m_format).format(location.accuracy)
                accuracyIcon.setImageResource(R.drawable.ic_accuracy_on)
            }
            else {
                accuracyIcon.setImageResource(R.drawable.ic_accuracy)
            }

            // TODO: Add digits and format into settings
            locationText.text = formatLocation(location.longitude, location.latitude, 2, android.location.Location.FORMAT_SECONDS)

            signalSourceText.text = location.provider
            speedText.text = formatSpeed(location.speed)
            satCountText.text = location.satelliteCount.toString()

            altText.text = getString(R.string.location_m_format).format(location.altitude)
        }
    }

    private fun formatSpeed(speed: Float) : String {
        return getString(R.string.location_km_hr_format).format(3.6 * speed) // When converting m/sec to km/hr, divide the speed with 1000 and multiply the result with 3600.
    }

    private fun formatLocation(lon: Double, lat: Double, digits: Int, format: Int) : String {
        val outLon = formatCoordinate(lon, format, digits)
        val outLat = formatCoordinate(lat, format, digits)
        return getString(R.string.location_coordinates).format(outLat, outLon)
    }
}