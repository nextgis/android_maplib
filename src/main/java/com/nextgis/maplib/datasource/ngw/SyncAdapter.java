/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2017, 2019-2021 NextGIS, info@nextgis.com
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

package com.nextgis.maplib.datasource.ngw;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.hypertrack.hyperlog.HyperLog;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.SettingsConstants;

import java.util.HashMap;

import static android.content.Context.MODE_MULTI_PROCESS;
import static com.nextgis.maplib.util.Constants.MESSAGE_ALERT_INTENT;
import static com.nextgis.maplib.util.Constants.MESSAGE_EXTRA;
import static com.nextgis.maplib.util.Constants.MESSAGE_NOTIFY_INTENT;
import static com.nextgis.maplib.util.Constants.MESSAGE_TITLE_EXTRA;
import static com.nextgis.maplib.util.Constants.TAG;

/* useful links
https://udinic.wordpress.com/2013/07/24/write-your-own-android-sync-adapter/#more-507
http://www.fussylogic.co.uk/blog/?p=1031
http://www.fussylogic.co.uk/blog/?p=1035
http://www.fussylogic.co.uk/blog/?p=1037
http://developer.android.com/training/sync-adapters/creating-sync-adapter.html
https://github.com/elegion/ghsync
http://habrahabr.ru/company/e-Legion/blog/206210/
http://habrahabr.ru/company/e-Legion/blog/216857/
http://stackoverflow.com/questions/5486228/how-do-we-control-an-android-sync-adapter-preference
https://books.google.ru/books?id=SXlMAQAAQBAJ&pg=PA158&lpg=PA158&dq=android:syncAdapterSettingsAction&source=bl&ots=T832S7VvKb&sig=vgNNDHfwyMzvINeHfdfDhu9tREs&hl=ru&sa=X&ei=YviqVIPMF9DgaPOUgOgP&ved=0CFUQ6AEwBw#v=onepage&q=android%3AsyncAdapterSettingsAction&f=false
*/


public class SyncAdapter
        extends AbstractThreadedSyncAdapter
{
    public static final String SYNC_START    = "com.nextgis.maplib.sync_start";
    public static final String SYNC_FINISH   = "com.nextgis.maplib.sync_finish";
    public static final String SYNC_CANCELED = "com.nextgis.maplib.sync_canceled";
    public static final String SYNC_CHANGES  = "com.nextgis.maplib.sync_changes";

    public static final String EXCEPTION = "exception";
    protected String mError;

    private HashMap<String, Pair<Integer, Integer>> mVersions;

    public SyncAdapter(
            Context context,
            boolean autoInitialize)
    {
        super(context, autoInitialize);
    }


    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs)
    {
        super(context, autoInitialize, allowParallelSyncs);
    }


    /**
     * Warning! When you stop the sync service by ContentResolver.cancelSync() then onPerformSync
     * stops after end of syncing of current NGWVectorLayer. The data structure of the current
     * NGWVectorLayer will be saved.
     * <p/>
     * <b>Description copied from class:</b> AbstractThreadedSyncAdapter Perform a sync for this
     * account. SyncAdapter-specific parameters may be specified in extras, which is guaranteed to
     * not be null. Invocations of this method are guaranteed to be serialized.
     */
    @Override
    public void onPerformSync(
            Account account,
            Bundle bundle,
            String authority,
            ContentProviderClient contentProviderClient,
            SyncResult syncResult)
    {
        ((IGISApplication)getContext().getApplicationContext()).setError(
                null,null,0);

        ((IGISApplication)getContext().getApplicationContext()).stopHandler();
        HyperLog.v(Constants.TAG, "SyncAdapter: onPerformSync for" + account.name + " ngw part start");
        Log.d(TAG, "onPerformSync");

        MapContentProviderHelper mapContentProviderHelper =(MapContentProviderHelper) MapBase.getInstance();
        getContext().sendBroadcast(new Intent(SYNC_START));

        mVersions = new HashMap<>();
        HyperLog.v(Constants.TAG, "SyncAdapter: mapContentProviderHelper is " + mapContentProviderHelper);
        if (null != mapContentProviderHelper) {
            // FIXME Temporary fix till 3.0
//            mapContentProviderHelper.load(); // reload map for deleted/added layers
            sync(mapContentProviderHelper, authority, syncResult);
        }

        if (isCanceled()) {
            Log.d(Constants.TAG, "onPerformSync - SYNC_CANCELED is sent");
            HyperLog.v(Constants.TAG, "SyncAdapter: SYNC_CANCELED is sent");
            getContext().sendBroadcast(new Intent(SYNC_CANCELED));
            return;
        }

        final String accountNameHash = "_" + account.name.hashCode();
        SharedPreferences settings = getContext().getSharedPreferences(Constants.PREFERENCES, MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(SettingsConstants.KEY_PREF_LAST_SYNC_TIMESTAMP + accountNameHash, System.currentTimeMillis());
        editor.putLong(SettingsConstants.KEY_PREF_LAST_SYNC_TIMESTAMP, System.currentTimeMillis());
        editor.apply();

        mError = "";
        if (syncResult.stats.numIoExceptions > 0)
            mError += getContext().getString(R.string.sync_error_io);
        if (syncResult.stats.numParseExceptions > 0) {
            if (mError.length() > 0)
                mError += "\r\n";
            mError += getContext().getString(R.string.sync_error_parse);
        }
        if (syncResult.stats.numAuthExceptions > 0) {
            if (mError.length() > 0)
                mError += "\r\n";
            mError += getContext().getString(R.string.error_auth_and_forbidden);
        }
        if (syncResult.stats.numConflictDetectedExceptions > 0) {
            if (mError.length() > 0)
                mError += "\r\n";
            mError += getContext().getString(R.string.sync_error_conflict);
        }
        if (syncResult.stats.numInserts > 0) {
            if (mError.length() > 0)
                mError += "\r\n";
            mError += getContext().getString(R.string.sync_error_insert);
        }
        if (syncResult.stats.numUpdates > 0) {
            if (mError.length() > 0)
                mError += "\r\n";
            mError += getContext().getString(R.string.sync_error_change);
        }
        if (syncResult.stats.numDeletes > 0) {
            if (mError.length() > 0)
                mError += "\r\n";
            mError += getContext().getString(R.string.sync_error_delete);
        }
        if (syncResult.stats.numEntries > 0) {
            if (mError.length() > 0)
                mError += "\r\n";
            mError += getContext().getString(R.string.sync_error_server);
        }
        if (syncResult.stats.numSkippedEntries > 0) {
            if (mError.length() > 0)
                mError += "\r\n";
            mError += getContext().getString(R.string.sync_error_oom);
        }

        Intent finish = new Intent(SYNC_FINISH);
        if (!TextUtils.isEmpty(mError))
            finish.putExtra(EXCEPTION, mError);
        HyperLog.v(Constants.TAG, "SyncAdapter: SYNC_FINISH is sent / mError is " + (TextUtils.isEmpty(mError) ? null:mError));
        getContext().sendBroadcast(finish);
    }


    protected void sync(
            LayerGroup layerGroup,
            String authority,
            SyncResult syncResult)
    {
        HyperLog.v(Constants.TAG, "SyncAdapter: StartSynchronization");
        HyperLog.v(Constants.TAG, "SyncAdapter: total layers for sync in " + layerGroup + " is " + layerGroup.getLayerCount());
        for (int i = 0; i < layerGroup.getLayerCount(); i++) {
            if (isCanceled()) {
                HyperLog.v(Constants.TAG, "SyncAdapter: Sync canceled");
                return;
            }
            ILayer layer = layerGroup.getLayer(i);
            if (layer instanceof LayerGroup) {
                HyperLog.v(Constants.TAG, "SyncAdapter: start sync " + layer.getName() + " is a layer group");
                sync((LayerGroup) layer, authority, syncResult);
            } else if (layer instanceof INGWLayer) {
                HyperLog.v(Constants.TAG, "SyncAdapter: start sync " + layer.getName() + " is a NGW layer");
                INGWLayer ngwLayer = (INGWLayer) layer;
                String accountName = ngwLayer.getAccountName();
                if (!mVersions.containsKey(accountName))
                    mVersions.put(accountName, NGWUtil.getNgwVersion(getContext(), accountName));

                Pair<Integer, Integer> ver = mVersions.get(accountName);
                ngwLayer.sync(authority, ver, syncResult);
            } else if (layer instanceof TrackLayer) {
                HyperLog.v(Constants.TAG, "SyncAdapter: start sync" + layer.getName() + " is a tracking layer");
                ((TrackLayer) layer).sync();
            }
            HyperLog.v(Constants.TAG, "SyncAdapter: Sync Ended for " + layer.getName() + " layer");
        }
    }

    @SuppressLint("MissingPermission")
    public static void setSyncPeriod(
            IGISApplication application,
            Bundle extras,
            long pollFrequency)
    {
        Context context = ((Context) application).getApplicationContext();
        final AccountManager accountManager = AccountManager.get(context);
        Log.d(TAG, "SyncAdapter: AccountManager.get(" + context + ")");

        for (Account account : accountManager.getAccountsByType(application.getAccountsType())) {
            ContentResolver.addPeriodicSync(account, application.getAuthority(), extras, pollFrequency);
        }
    }

    public boolean isCanceled()
    {
        return Thread.currentThread().isInterrupted();
    }


    // send broadcast for  MESSAGE_NOTIFY_INTENT
    static public void showNotify(final Context context, final String message , final String title){
        // send broadcast to show notify
        Intent msg = new Intent(MESSAGE_NOTIFY_INTENT);
        msg.putExtra(MESSAGE_EXTRA, message);
        msg.putExtra(MESSAGE_TITLE_EXTRA, title);
        context.sendBroadcast(msg);

    }

}
