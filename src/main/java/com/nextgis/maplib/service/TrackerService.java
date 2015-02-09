/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Authors:  Stanislav Petriakov
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

package com.nextgis.maplib.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.TrackLayer;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class TrackerService
        extends Service
        implements LocationListener
{
    //    private final       String TAG          = "com.nextgis.mobile";
    public static final  String TARGET_CLASS          = "target_class";
    private static final String TRACK_URI             = "track_uri";
    private static final String ACTION_STOP           = "TRACK_STOP";
    private static final String ACTION_SPLIT          = "TRACK_SPLIT";
    private static final int    TRACK_NOTIFICATION_ID = 1;

    private boolean         mIsRunning;
    private LocationManager mLocationManager;

    private SharedPreferences mSharedPreferences;
    private String            mTrackName, mTrackId;
    private Uri mContentUriTracks, mContentUriTrackpoints, mNewTrack;
    private Cursor        mLastTrack;
    private ContentValues mValues;

    private NotificationManager mNotificationManager;
    private AlarmManager        mAlarmManager;
    private PendingIntent       mSplitService, mOpenActivity;
    private String mTicker;
    private int    mSmallIcon;


    public void onCreate()
    {
        super.onCreate();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        IGISApplication application = (IGISApplication) getApplication();
        String authority = application.getAuthority();
        mContentUriTracks = Uri.parse("content://" + authority + "/" + TrackLayer.TABLE_TRACKS);
        mContentUriTrackpoints =
                Uri.parse("content://" + authority + "/" + TrackLayer.TABLE_TRACKPOINTS);

        mValues = new ContentValues();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // TODO preference
        long minTime = 1000;
        float minDistance = 2;

        // TODO provider selection
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (mLocationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime,
                                                    minDistance, this);
        }

        if (mLocationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime,
                                                    minDistance, this);
        }

        mLastTrack = getLastTrack();

        mTicker = getString(R.string.tracks_running);
        mSmallIcon = android.R.drawable.ic_menu_myplaces;

        Intent intentSplit = new Intent(this, TrackerService.class);
        intentSplit.setAction(ACTION_SPLIT);
        mSplitService =
                PendingIntent.getService(this, 0, intentSplit, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public int onStartCommand(
            Intent intent,
            int flags,
            int startId)
    {

        String targetActivity = "";

        if (intent != null) {
            targetActivity = intent.getStringExtra(TARGET_CLASS);
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action)) {
                switch (action) {
                    case ACTION_STOP:
                        stopSelf();
                        return START_NOT_STICKY;
                    case ACTION_SPLIT:
                        stopTrack();
                        mLastTrack = getLastTrack();
                        startTrack();
                        addNotification();
                        return START_STICKY;
                }
            }
        }

        if (!mIsRunning) {
            // there are no tracks or last track correctly ended
            if (!hasLastTrack() || isLastTrackClosed()) {
                startTrack();
                mSharedPreferences.edit().putString(TARGET_CLASS, targetActivity).commit();
            } else {
                // looks like service was killed, restore data
                restoreData();
                targetActivity = mSharedPreferences.getString(TARGET_CLASS, "");
            }

            initTargetIntent(targetActivity);
            addNotification();
        }

        return START_STICKY;
    }


    private Cursor getLastTrack()
    {
        String[] proj = new String[] {TrackLayer.FIELD_END, TrackLayer.FIELD_NAME};
        String selection = TrackLayer.FIELD_ID + " = (SELECT MAX(" + TrackLayer.FIELD_ID +
                           ") FROM " + TrackLayer.TABLE_TRACKS + ")";
        String[] args = new String[] {};

        return getContentResolver().query(mContentUriTracks, proj, selection, args, null);
    }


    private boolean hasLastTrack()
    {
        return mLastTrack != null && mLastTrack.moveToFirst();
    }


    private boolean isLastTrackClosed()
    {
        return mLastTrack.moveToFirst() && !TextUtils.isEmpty(
                mLastTrack.getString(mLastTrack.getColumnIndex(TrackLayer.FIELD_END)));
    }


    private void restoreData()
    {
        mTrackName = mLastTrack.getString(mLastTrack.getColumnIndex(TrackLayer.FIELD_NAME));
        mNewTrack = Uri.parse(mSharedPreferences.getString(TRACK_URI, ""));
        mTrackId = mNewTrack.getLastPathSegment();
        mIsRunning = true;
    }


    private void startTrack()
    {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.MILLISECOND, 0);

        // get track name date unique appendix
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-");
        String append = "1";

        // there is at least one track
        if (hasLastTrack()) {
            long lastTrackEnd = mLastTrack.getLong(mLastTrack.getColumnIndex(TrackLayer.FIELD_END));

            // last track recorded today, increase appendix
            if (lastTrackEnd > today.getTimeInMillis()) {
                String[] segments =
                        mLastTrack.getString(mLastTrack.getColumnIndex(TrackLayer.FIELD_NAME))
                                  .split("-");
                int newTrackDayId = Integer.parseInt(segments[segments.length - 1]) + 1;
                append = Integer.toString(newTrackDayId);
            }
        }

        // insert DB row
        final long started = System.currentTimeMillis();
        mTrackName = simpleDateFormat.format(started) + append;
        mValues.clear();
        mValues.put(TrackLayer.FIELD_NAME, mTrackName);
        mValues.put(TrackLayer.FIELD_START, started);
        mValues.put(TrackLayer.FIELD_VISIBLE, true);
        mNewTrack = getContentResolver().insert(mContentUriTracks, mValues);

        // save vars
        mTrackId = mNewTrack.getLastPathSegment();
        mSharedPreferences.edit().putString(TRACK_URI, mNewTrack.toString()).commit();
        mIsRunning = true;

        // set midnight track splitter
        today.add(Calendar.DATE, 1);
        mAlarmManager.set(AlarmManager.RTC, today.getTimeInMillis(), mSplitService);
    }


    private void stopTrack()
    {
        // update DB row
        mValues.clear();
        mValues.put(TrackLayer.FIELD_END, System.currentTimeMillis());
        getContentResolver().update(mNewTrack, mValues, null, null);
        mIsRunning = false;

        // cancel midnight splitter
        mAlarmManager.cancel(mSplitService);
    }


    private void addNotification()
    {
        Notification notif;
        String title = String.format(getString(R.string.tracks_title), mTrackName);
        Bitmap largeIcon =
                BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_myplaces);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Intent intentStop = new Intent(this, TrackerService.class);
            intentStop.setAction(ACTION_STOP);
            PendingIntent stopService = PendingIntent.getService(this, 0, intentStop,
                                                                 PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder builder = new Notification.Builder(this);

            builder.setContentIntent(mOpenActivity)
                   .setSmallIcon(mSmallIcon)
                   .setLargeIcon(largeIcon)
                   .setTicker(mTicker)
                   .setWhen(System.currentTimeMillis())
                   .setAutoCancel(false)
                   .setContentTitle(title)
                   .setContentText(mTicker)
                   .setOngoing(true);

            builder.addAction(android.R.drawable.ic_menu_mapmode, getString(R.string.tracks_open),
                              mOpenActivity);
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                              getString(R.string.tracks_stop), stopService);

            notif = builder.build();
        } else {
            notif = buildDeprecated(mSmallIcon, mTicker, title, mOpenActivity);
        }

        mNotificationManager.notify(TRACK_NOTIFICATION_ID, notif);
    }


    @SuppressWarnings("deprecation")
    private Notification buildDeprecated(
            int smallIcon,
            String ticker,
            String title,
            PendingIntent intent)
    {
        Notification result = new Notification(smallIcon, ticker, System.currentTimeMillis());
        result.setLatestEventInfo(this, title, ticker, intent);
        result.flags |= Notification.FLAG_ONGOING_EVENT;

        return result;
    }


    private void removeNotification()
    {
        mNotificationManager.cancel(TRACK_NOTIFICATION_ID);
    }


    // intent to open on notification click
    private void initTargetIntent(String targetActivity)
    {
        Intent intentActivity = new Intent();

        if (!TextUtils.isEmpty(targetActivity)) {
            Class<?> targetClass = null;

            try {
                targetClass = Class.forName(targetActivity);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (targetClass != null) {
                intentActivity = new Intent(this, targetClass);
            }
        }

        intentActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mOpenActivity = PendingIntent.getActivity(this, 0, intentActivity,
                                                  PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public void onDestroy()
    {
        stopTrack();
        mLocationManager.removeUpdates(this);
        removeNotification();
        stopSelf();
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }


    @Override
    public void onLocationChanged(Location location)
    {
        mValues.clear();
        mValues.put(TrackLayer.FIELD_SESSION, mTrackId);
        mValues.put(TrackLayer.FIELD_LON, location.getLongitude());
        mValues.put(TrackLayer.FIELD_LAT, location.getLatitude());
        mValues.put(TrackLayer.FIELD_TIMESTAMP, System.currentTimeMillis());
        getContentResolver().insert(mContentUriTrackpoints, mValues);
    }


    @Override
    public void onStatusChanged(
            String provider,
            int status,
            Bundle extras)
    {

    }


    @Override
    public void onProviderEnabled(String provider)
    {

    }


    @Override
    public void onProviderDisabled(String provider)
    {

    }
}
