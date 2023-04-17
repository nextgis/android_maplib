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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nextgis.maplib.Location
import com.nextgis.maplib.R
import com.nextgis.maplib.databinding.FragmentFilePickerBinding
import com.nextgis.maplib.databinding.LocationInfoBinding
import com.nextgis.maplib.formatCoordinate
import com.nextgis.maplib.service.TrackerDelegate
import com.nextgis.maplib.service.TrackerService
import com.nextgis.maplib.startTrackerService
import java.util.*

/**
 * Location information panel.
 */
class LocationInfoFragment : Fragment() {

    private var _binding: LocationInfoBinding? = null
    private val binding get() = _binding!!

    private var mTrackerService: TrackerService? = null
    private var mIsBound = false
    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TrackerService.LocalBinder
            mTrackerService = binder.getService()
            mTrackerService?.addDelegate(mTrackerDelegate)
            mIsBound = true
            mTrackerService?.status()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mIsBound = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = LocationInfoBinding.inflate(LayoutInflater.from(context), null, false)
        return binding.root
    }

    private val mTrackerDelegate = object : TrackerDelegate {
        override fun onLocationChanged(location: Location) {
            binding.locationIcon.setImageResource(R.drawable.ic_gps_fixed)

            if(location.hasAccuracy()) {
                binding.accuracyText.text = getString(R.string.location_m_format).format(location.accuracy)
                binding.accuracyIcon.setImageResource(R.drawable.ic_accuracy_on)
            }
            else {
                binding.accuracyIcon.setImageResource(R.drawable.ic_accuracy)
            }

            // TODO: Add digits and format into settings
            binding.locationText.text = formatLocation(location.longitude, location.latitude, 2, android.location.Location.FORMAT_SECONDS)

            binding.signalSourceText.text = location.provider
            binding.speedText.text = formatSpeed(location.speed)
            binding.satCountText.text = location.satelliteCount.toString()

            binding.altText.text = getString(R.string.location_m_format).format(location.altitude)
        }

        override fun onStatusChanged(status: TrackerService.Status, trackName: String, trackStartTime: Date) {

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

    override fun onStart() {
        super.onStart()

        // Get current status
        val intent = Intent(context, TrackerService::class.java)
        context?.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        if(mIsBound) {
            mTrackerService?.removeDelegate(mTrackerDelegate)
            context?.unbindService(mServiceConnection)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}