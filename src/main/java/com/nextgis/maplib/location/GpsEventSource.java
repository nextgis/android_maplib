/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplib.location;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.util.SettingsConstants;

import java.util.ArrayList;
import java.util.List;


public class GpsEventSource
{
    public static final   int MIN_SATELLITES_IN_FIX = 3;
    protected List<GpsEventListener> mListeners;

    protected LocationManager     mLocationManager;
    protected GpsLocationListener mGpsLocationListener;
    protected GpsStatusListener   mGpsStatusListener;
    protected int                 mListenProviders;
    protected Location            mLastLocation;
    protected Context             mContext;
    protected long                mUpdateMinTime;
    protected float               mUpdateMinDistance;

    public static final int GPS_PROVIDER     = 1 << 0;
    public static final int NETWORK_PROVIDER = 1 << 1;


    public GpsEventSource(Context context)
    {
        mContext = context;
        mListeners = new ArrayList<>();

        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mGpsLocationListener = new GpsLocationListener();
        mGpsStatusListener = new GpsStatusListener();

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

            if (mListeners.size() == 0) {
                mLocationManager.removeUpdates(mGpsLocationListener);
                mLocationManager.removeGpsStatusListener(mGpsStatusListener);
            }
        }
    }


    public Location getLastKnownLocation()
    {
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


    public void updateActiveListeners()
    {
        mLocationManager.removeUpdates(mGpsLocationListener);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(mContext);
        mListenProviders = sharedPreferences.getInt(SettingsConstants.KEY_PREF_LOCATION_SOURCE,
                                                    GPS_PROVIDER | NETWORK_PROVIDER);

        String minTimeStr =
                sharedPreferences.getString(SettingsConstants.KEY_PREF_LOCATION_MIN_TIME, "3");
        String minDistanceStr =
                sharedPreferences.getString(SettingsConstants.KEY_PREF_LOCATION_MIN_DISTANCE, "10");
        mUpdateMinTime = Long.parseLong(minTimeStr) * 1000;
        mUpdateMinDistance = Float.parseFloat(minDistanceStr);

        if (mListeners.size() >= 1) {
            requestUpdates();
        }
    }


    private void requestUpdates()
    {
        if (0 != (mListenProviders & GPS_PROVIDER) &&
            mLocationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mUpdateMinTime,
                                                    mUpdateMinDistance, mGpsLocationListener);
        }

        if (0 != (mListenProviders & NETWORK_PROVIDER) &&
            mLocationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                                                    mUpdateMinTime, mUpdateMinDistance,
                                                    mGpsLocationListener);
        }
    }


    protected final class GpsLocationListener
            implements LocationListener
    {

        public void onLocationChanged(Location location)
        {
            mLastLocation = location;

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


        public void onStatusChanged(
                String provider,
                int status,
                Bundle extras)
        {
        }
    }


    private final class GpsStatusListener
            implements GpsStatus.Listener
    {

        @Override
        public void onGpsStatusChanged(int event)
        {
            for (GpsEventListener listener : mListeners) {
                listener.onGpsStatusChanged(event);
            }
        }
    }
}
