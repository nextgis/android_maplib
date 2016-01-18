/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplib.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.util.Constants;


public class NGWSyncService
        extends Service
{
    private static SyncAdapter mSyncAdapter = null;

    // Object to use as a thread-safe lock
    private static final Object mSyncAdapterLock = new Object();

    protected SyncReceiver mSyncReceiver;
    protected boolean      mIsSyncStarted;


    /*
     * Instantiate the sync adapter object.
     */
    @Override
    public void onCreate()
    {
        // For service debug
//        android.os.Debug.waitForDebugger();

        /*
         * Create the sync adapter as a singleton.
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        synchronized (mSyncAdapterLock) {
            if (mSyncAdapter == null) {
                mSyncAdapter = createSyncAdapter(getApplicationContext(), true);
            }
        }

        mIsSyncStarted = false;

        mSyncReceiver = new SyncReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SyncAdapter.SYNC_START);
        intentFilter.addAction(SyncAdapter.SYNC_FINISH);
        intentFilter.addAction(SyncAdapter.SYNC_CANCELED);
        intentFilter.addAction(SyncAdapter.SYNC_CHANGES);
        registerReceiver(mSyncReceiver, intentFilter);
    }


    protected SyncAdapter createSyncAdapter(
            Context context,
            boolean autoInitialize)
    {
        return new SyncAdapter(context, autoInitialize);
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        return mSyncAdapter.getSyncAdapterBinder();
    }


    @Override
    public void onDestroy()
    {
        unregisterReceiver(mSyncReceiver);

        if (isSyncStarted()) {
            Log.d(Constants.TAG, "SyncAdapter - sync is canceled, sleep");

            try {
                // We have not guarantee to receive SyncAdapter.SYNC_CANCELED
                // because of a possible sync service shutdown.
                // For it we sleep.
                Thread.sleep(10000);
                Log.d(Constants.TAG, "SyncAdapter - sleep for SYNC_CANCELED is ended");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        super.onDestroy();
    }


    public boolean isSyncStarted()
    {
        return mIsSyncStarted;
    }


    public class SyncReceiver
            extends BroadcastReceiver
    {
        @Override
        public void onReceive(
                Context context,
                Intent intent)
        {
            String action = intent.getAction();

            switch (action) {
                case SyncAdapter.SYNC_START:
                    mIsSyncStarted = true;
                    break;

                case SyncAdapter.SYNC_FINISH:
                    mIsSyncStarted = false;
                    break;

                case SyncAdapter.SYNC_CANCELED:
                    Log.d(Constants.TAG, "SyncAdapter - SYNC_CANCELED is received");
                    mIsSyncStarted = false;
                    break;

                case SyncAdapter.SYNC_CHANGES:
                    // TODO:  ???  mIsSyncStarted = true;  ???
                    break;
            }
        }
    }
}
