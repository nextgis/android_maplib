/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.location;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.PermissionUtil;
import com.nextgis.maplib.util.SettingsConstants;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class GpsEventSource
{
    protected Queue<GpsEventListener> mListeners;

    protected LocationManager     mLocationManager;
    protected GpsLocationListener mGpsLocationListener;
    protected GpsStatusListener   mGpsStatusListener;
    protected boolean             mHasGPSFix;
    protected int                 mListenProviders;
    protected Location            mLastLocation;
    protected Location            mCurrentBestLocation;
    protected Context             mContext;
    protected long                mUpdateMinTime;
    protected float               mUpdateMinDistance;

    public static final    int GPS_PROVIDER     = 1 << 0;
    public static final    int NETWORK_PROVIDER = 1 << 1;
    protected static final int TWO_MINUTES      = 1000 * 60 * 2;


    public GpsEventSource(Context context)
    {
        mContext = context;
        mListeners = new ConcurrentLinkedQueue<>();

        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mGpsLocationListener = new GpsLocationListener();
        mGpsStatusListener = new GpsStatusListener();

        mHasGPSFix = false;

        updateActiveListeners();
    }


    /**
     * Add new listener for GPS events. You will likely want to call addListener() from your
     * Activity's or Fragment's onResume() method, to enable the features. Remember to call the
     * corresponding removeListener() in your Activity's or Fragment's onPause() method, to prevent
     * unnecessary use of the battery.
     *
     * @param listener
     *         A listener class implements GpsEventListener adding to listeners array
     */
    public void addListener(GpsEventListener listener)
    {
        if (mListeners != null && !mListeners.contains(listener)) {
            mListeners.add(listener);

            if (mListeners.size() == 1) {
                if(!PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                    || !PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION))
                    return;

                requestUpdates();
                mLocationManager.addGpsStatusListener(mGpsStatusListener);
            }
        }
    }


    /**
     * Remove listener from listeners of GPS events. You will likely want to call removeListener()
     * from your Activity's or Fragment's onPause() method, to prevent unnecessary use of the
     * battery. Remember to call the corresponding addListener() in your Activity's or Fragment's
     * onResume() method.
     *
     * @param listener
     *         A listener class implements GpsEventListener removing from listeners array
     */
    public void removeListener(GpsEventListener listener)
    {
        if (mListeners != null) {
            mListeners.remove(listener);

            if(!PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                    || !PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION))
                return;

            if (mListeners.size() == 0) {
                mLocationManager.removeUpdates(mGpsLocationListener);
                mLocationManager.removeGpsStatusListener(mGpsStatusListener);
            }
        }
    }


    public Location getLastKnownLocation()
    {
        if(!PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                || !PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION))
            return null;

        if (null != mLastLocation) {
            return mLastLocation;
        }

        if (null != mLocationManager) {
            if (0 != (mListenProviders & GPS_PROVIDER)) {
                mLastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (null != mLastLocation) {
                    return mLastLocation;
                }
            }

            if (0 != (mListenProviders & NETWORK_PROVIDER)) {
                mLastLocation =
                        mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (null != mLastLocation) {
                    return mLastLocation;
                }
            }
        }
        return null;
    }


    public Location getLastKnownBestLocation()
    {
        if(!PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                || !PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION))
            return null;

        if (null != mCurrentBestLocation) {
            return mCurrentBestLocation;
        }

        if (null != mLocationManager) {
            Location gpsLocation = null;
            Location networkLocation = null;

            if (0 != (mListenProviders & GPS_PROVIDER)) {
                gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (0 != (mListenProviders & NETWORK_PROVIDER)) {
                networkLocation =
                        mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (null == gpsLocation) {
                mCurrentBestLocation = networkLocation;
                return mCurrentBestLocation;
            }

            if (null == networkLocation) {
                mCurrentBestLocation = gpsLocation;
                return mCurrentBestLocation;
            }

            if (isBetterLocation(gpsLocation, networkLocation)) {
                mCurrentBestLocation = gpsLocation;
            } else {
                mCurrentBestLocation = networkLocation;
            }
            return mCurrentBestLocation;
        }

        return null;
    }


    public void updateActiveListeners()
    {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(mContext);
        mListenProviders = Integer.parseInt(sharedPreferences.getString(
                SettingsConstants.KEY_PREF_LOCATION_SOURCE, "3"));

        String minTimeStr =
                sharedPreferences.getString(SettingsConstants.KEY_PREF_LOCATION_MIN_TIME, "2");
        String minDistanceStr =
                sharedPreferences.getString(SettingsConstants.KEY_PREF_LOCATION_MIN_DISTANCE, "10");
        mUpdateMinTime = Long.parseLong(minTimeStr) * 1000;
        mUpdateMinDistance = Float.parseFloat(minDistanceStr);

        if(!PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                || !PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION))
            return;

        mLocationManager.removeUpdates(mGpsLocationListener);
        if (mListeners.size() >= 1)
            requestUpdates();
    }


    private void requestUpdates()
    {
        if (0 != (mListenProviders & GPS_PROVIDER) &&
                mLocationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {

            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, mUpdateMinTime, mUpdateMinDistance,
                    mGpsLocationListener);

            if(Constants.DEBUG_MODE)
                Log.d(Constants.TAG, "GpsEventSource request location updates for " + LocationManager.GPS_PROVIDER);
        }

        if (0 != (mListenProviders & NETWORK_PROVIDER) &&
                mLocationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {

            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, mUpdateMinTime, mUpdateMinDistance,
                    mGpsLocationListener);

            if(Constants.DEBUG_MODE)
                Log.d(Constants.TAG, "GpsEventSource request location updates for " + LocationManager.NETWORK_PROVIDER);
        }
    }


    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location
     *         The new Location that you want to evaluate
     * @param currentBestLocation
     *         The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(
            Location location,
            Location currentBestLocation)
    {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider =
                isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }


    /**
     * Checks whether two providers are the same
     */
    protected boolean isSameProvider(
            String provider1,
            String provider2)
    {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


    protected final class GpsLocationListener
            implements LocationListener
    {

        public void onLocationChanged(Location location)
        {
            if(mHasGPSFix && !location.getProvider().equals(LocationManager.GPS_PROVIDER))
                return;

            mLastLocation = location;

            if (isBetterLocation(mLastLocation, mCurrentBestLocation)) {
                mCurrentBestLocation = mLastLocation;
                for (GpsEventListener listener : mListeners) {
                    listener.onBestLocationChanged(mCurrentBestLocation);
                }
            }

            for (GpsEventListener listener : mListeners) {
                listener.onLocationChanged(mLastLocation);
            }
        }


        public void onProviderDisabled(String arg0)
        {

        }


        public void onProviderEnabled(String provider)
        {
        }

        // http://stackoverflow.com/a/20812298
        public void onStatusChanged(
                String provider,
                int status,
                Bundle extras)
        {

        }
    }


    protected final class GpsStatusListener
            implements GpsStatus.Listener
    {
        @Override
        public void onGpsStatusChanged(int event)
        {
            switch(event)
            {
                case GpsStatus.GPS_EVENT_STARTED:
                case GpsStatus.GPS_EVENT_STOPPED:
                    mHasGPSFix = false;
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    mHasGPSFix = true;
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    break;
            }

            for (GpsEventListener listener : mListeners) {
                listener.onGpsStatusChanged(event);
            }
        }
    }
}
