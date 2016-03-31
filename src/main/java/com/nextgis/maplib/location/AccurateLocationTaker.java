/*
 * Project:  NextGIS mobile apps for Compulink
 * Purpose:  Mobile GIS for Android
 * Authors:  Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 *           NikitaFeodonit, nfeodonit@yandex.com
 * *****************************************************************************
 * Copyright (C) 2014-2015 NextGIS
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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.PermissionUtil;

import java.util.ArrayList;
import java.util.Collections;


public class AccurateLocationTaker implements LocationListener
{
    protected Float   mMaxTakenAccuracy;
    protected Integer mMaxTakeCount;
    protected Long    mMaxTakeTimeMillis;
    protected long    mPublishProgressDelayMillis;
    protected float   mCircularError;

    protected long mStartTakeTimeMillis;
    protected long mTakeTimeMillis;

    protected Double mLatMin, mLatMax, mLonMin, mLonMax, mAltMin, mAltMax;
    protected double mLatSum, mLonSum, mAltSum;
    protected double mLatAverage, mLonAverage, mAltAverage;

    protected Long mLastLocationTime;

    protected ArrayList<Location> mGpsTakings;
    protected LocationManager mLocationManager;
    protected Context mContext;

    // Create a Handler that uses the Main Looper to run in
    protected Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mStopTakingRunner;
    private Runnable mProgressUpdateRunner;

    protected boolean mIsStopped   = false;
    protected boolean mIsCancelled = false;
    protected OnProgressUpdateListener             mOnProgressUpdateListener;
    protected OnGetCurrentAccurateLocationListener mOnGetCurrentAccurateLocationListener;
    protected OnGetAccurateLocationListener        mOnGetAccurateLocationListener;


    /**
     * @param context
     *         The context.
     * @param maxTakeCount
     *         The max count of the GPS taking. If null then count is ignored.
     * @param maxTakeTimeMillis
     *         The max time for GPS taking (milliseconds). If null then time is ignored.
     * @param publishProgressDelayMillis
     *         The delay (milliseconds) for publish progress.
     * @param circularErrorStr
     *         The circular error, may be "CE50", "CE90", "CE95" or "CE98". If null or something
     *         other then takes the default value, "CE50".
     */
    public AccurateLocationTaker(
            Context context,
            Float maxTakenAccuracy,
            Integer maxTakeCount,
            Long maxTakeTimeMillis,
            long publishProgressDelayMillis,
            String circularErrorStr)
    {
        mContext = context;
        IGISApplication app = (IGISApplication) context.getApplicationContext();
        mMaxTakenAccuracy = maxTakenAccuracy;
        mLocationManager = app.getGpsEventSource().mLocationManager;
        mMaxTakeCount = maxTakeCount;
        mMaxTakeTimeMillis = maxTakeTimeMillis;
        mPublishProgressDelayMillis = publishProgressDelayMillis;
        mCircularError = getCircularErrorFromString(circularErrorStr);
    }


    protected float getCircularErrorFromString(String circularErrorString)
    {
        if (null == circularErrorString) {
            circularErrorString = Constants.CE50;
        }

        switch (circularErrorString) {
            case Constants.CE50:
            default:
                return 0.5f;
            case Constants.CE90:
                return 0.9f;
            case Constants.CE95:
                return 0.95f;
            case Constants.CE98:
                return 0.98f;
        }
    }


    protected Location getAccurateLocation(float circularError)
    {
        if (0 > circularError || 1 < circularError) {
            throw new IllegalArgumentException(
                    "GPS taking, circularError must be in the [0, 1] bounds, now is " +
                            circularError);
        }

        if (0 == mGpsTakings.size()) {
            return null;
        }

        int takeCount = mGpsTakings.size();

        mLatAverage = mLatSum / takeCount;
        mLonAverage = mLonSum / takeCount;
        mAltAverage = mAltSum / takeCount;

        long time;
        if (null != mLastLocationTime) {
            time = mLastLocationTime;
        } else {
            time = System.currentTimeMillis();
        }

        Location accurateLoc = new Location("GPS Accurate");

        accurateLoc.setSpeed(0);
        accurateLoc.setLatitude(mLatAverage);
        accurateLoc.setLongitude(mLonAverage);
        accurateLoc.setAltitude(mAltAverage);
        accurateLoc.setTime(time);

        // Here accurateLoc.getAccuracy() == 0.0f, form getAccuracy() docs:
        // "If this location does not have an accuracy, then 0.0 is returned."
        // We must check for 0.

        if (2 <= takeCount) {

            ArrayList<PairDistLoc> GPSDist = new ArrayList<>();
            for (Location location : mGpsTakings) {
                float dist = accurateLoc.distanceTo(location);
                GPSDist.add(new PairDistLoc(dist, location));
            }
            Collections.sort(GPSDist);


            int ceSize = ((int) (GPSDist.size() * circularError));
            double ceLatSum = 0, ceLonSum = 0, ceAltSum = 0;
            for (int i = 0; i < ceSize; ++i) {
                Location location = GPSDist.get(i).mLocation;
                ceLatSum += location.getLatitude();
                ceLonSum += location.getLongitude();
                ceAltSum += location.getAltitude();
            }
            double ceLatAverage = ceLatSum / ceSize;
            double ceLonAverage = ceLonSum / ceSize;
            double ceAltAverage = ceAltSum / ceSize;


            int accIndex = ceSize - 1;
            float accuracy = GPSDist.get(accIndex).mDistanceToCenter;


            accurateLoc.setLatitude(ceLatAverage);
            accurateLoc.setLongitude(ceLonAverage);
            accurateLoc.setAltitude(ceAltAverage);
            accurateLoc.setAccuracy(accuracy);
        }

        return accurateLoc;
    }


    protected void takeLocation(Location location)
    {
        if (null == mGpsTakings) {
            return;
        }

        if (null != mMaxTakenAccuracy && mMaxTakenAccuracy.compareTo(location.getAccuracy()) < 0) {
            return;
        }

        mGpsTakings.add(location);

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        double alt = location.getAltitude();

        mLatMin = null == mLatMin ? lat : Math.min(mLatMin, lat);
        mLatMax = null == mLatMax ? lat : Math.max(mLatMax, lat);

        mLonMin = null == mLonMin ? lon : Math.min(mLonMin, lon);
        mLonMax = null == mLonMax ? lon : Math.max(mLonMax, lon);

        mAltMin = null == mAltMin ? alt : Math.min(mAltMin, alt);
        mAltMax = null == mAltMax ? alt : Math.max(mAltMax, alt);

        mLatSum += lat;
        mLonSum += lon;
        mAltSum += alt;

        mLastLocationTime = location.getTime();

        if (!isTaking()) {
            Log.d(Constants.TAG, "Stop the GPS taking after the maxTakeCount");
            stopTaking();
        }
    }


    @Override
    public void onLocationChanged(Location location)
    {
        takeLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    public void startTaking()
    {
        mIsStopped = false;
        mGpsTakings = new ArrayList<>();
        mStartTakeTimeMillis = System.currentTimeMillis();
        mTakeTimeMillis = 0;

        mLatMin = mLatMax = mLonMin = mLonMax = mAltMin = mAltMax = null;
        mLatSum = mLonSum = mAltSum = 0;
        mLatAverage = mLonAverage = mAltAverage = 0;
        mLastLocationTime = null;

        Log.d(Constants.TAG, "Start the GPS taking");

        mStopTakingRunner = new Runnable()
        {
            @Override
            public void run()
            {
                Log.d(Constants.TAG, "Stop the GPS taking after the maxTakeTimeMillis");
                stopTaking();
            }
        };

        if (isTaking()) {
            mHandler.postDelayed(mStopTakingRunner, mMaxTakeTimeMillis);
        } else {
            Log.d(
                    Constants.TAG,
                    "GPS taking, WARNING: all params are ignored, take only one taking");
        }

        mProgressUpdateRunner = new Runnable()
        {
            @Override
            public void run()
            {
                if (isCancelled()) {
                    stopTaking();
                    return;
                }

                if (isTaking()) {
                    // Re-run it after the mPublishProgressDelayMillis
                    mHandler.postDelayed(mProgressUpdateRunner, mPublishProgressDelayMillis);

                    if (null != mOnProgressUpdateListener) {
                        mOnProgressUpdateListener.onProgressUpdate(
                                (long) mGpsTakings.size(), mTakeTimeMillis);
                    }

                    if (null != mOnGetCurrentAccurateLocationListener) {
                        mOnGetCurrentAccurateLocationListener.onGetCurrentAccurateLocation(
                                getAccurateLocation(mCircularError));
                    }
                }
            }
        };

        mProgressUpdateRunner.run();

        if(!PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                || !PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION))
            return;

        if (null != mLocationManager) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }


    public boolean isTaking()
    {
        mTakeTimeMillis = System.currentTimeMillis() - mStartTakeTimeMillis;
        boolean isAllIgnored = null == mMaxTakeTimeMillis && null == mMaxTakeCount;
        boolean hasTakeCount =
                null == mMaxTakeCount || null != mGpsTakings && mGpsTakings.size() < mMaxTakeCount;
        boolean hasTakeTime = null == mMaxTakeTimeMillis || mTakeTimeMillis < mMaxTakeTimeMillis;

        return !mIsStopped && !mIsCancelled && !isAllIgnored && hasTakeCount && hasTakeTime;
    }


    public void stopTaking()
    {
        mIsStopped = true;

        mHandler.removeCallbacks(mStopTakingRunner);
        mHandler.removeCallbacks(mProgressUpdateRunner);

        if (null != mGpsTakings && !isCancelled() && null != mOnGetAccurateLocationListener) {
            Log.d(Constants.TAG, "Get the GPS accurate location");
            mOnGetAccurateLocationListener.onGetAccurateLocation(
                    getAccurateLocation(mCircularError), (long) mGpsTakings.size(),
                    mTakeTimeMillis);
        }

        if(!PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                || !PermissionUtil.hasPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION))
            return;

        if (null != mLocationManager) {
            mLocationManager.removeUpdates(this);
        }
    }


    public boolean isCancelled()
    {
        return mIsCancelled;
    }


    public void cancelTaking()
    {
        Log.d(Constants.TAG, "GPS taking is cancelled");
        mIsCancelled = true;
        mIsStopped = true;
        stopTaking();
    }


    public void setOnProgressUpdateListener(OnProgressUpdateListener onProgressUpdateListener)
    {
        mOnProgressUpdateListener = onProgressUpdateListener;
    }


    /**
     * Implement the OnProgressUpdateListener interface to obtain the progress update.
     */
    public interface OnProgressUpdateListener
    {
        /**
         * @param values
         *         The values[0] presents the take counts. The values[1] presents the take time
         *         (milliseconds).
         */
        void onProgressUpdate(Long... values);
    }


    public void setOnGetCurrentAccurateLocationListener(
            OnGetCurrentAccurateLocationListener onGetCurrentAccurateLocationListener)
    {
        mOnGetCurrentAccurateLocationListener = onGetCurrentAccurateLocationListener;
    }


    /**
     * Implement the OnGetCurrentAccurateLocationListener interface to obtain the current accurate
     * location during the measurement.
     */
    public interface OnGetCurrentAccurateLocationListener
    {
        /**
         * @param currentAccurateLocation
         *         The current accurate location. May be null.
         */
        void onGetCurrentAccurateLocation(Location currentAccurateLocation);
    }


    public void setOnGetAccurateLocationListener(
            OnGetAccurateLocationListener onGetAccurateLocationListener)
    {
        mOnGetAccurateLocationListener = onGetAccurateLocationListener;
    }


    /**
     * Implement the OnGetAccurateLocationListener interface to obtain the accurate location.
     */
    public interface OnGetAccurateLocationListener
    {
        /**
         * @param accurateLocation
         *         The accurate location. May be null.
         * @param values
         *         The values[0] presents the take counts. The values[1] presents the take time
         *         (milliseconds).
         */
        void onGetAccurateLocation(
                Location accurateLocation,
                Long... values);
    }


    protected class PairDistLoc
            implements Comparable<PairDistLoc>
    {
        Float    mDistanceToCenter;
        Location mLocation;


        PairDistLoc(
                float dist,
                Location location)
        {
            mDistanceToCenter = dist;
            mLocation = location;
        }


        @Override
        public int compareTo(PairDistLoc another)
        {
            return mDistanceToCenter.compareTo(another.mDistanceToCenter);
        }
    }
}
