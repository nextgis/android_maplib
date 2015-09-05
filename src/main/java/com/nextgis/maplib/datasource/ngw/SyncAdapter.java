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

package com.nextgis.maplib.datasource.ngw;

import android.accounts.Account;
import android.accounts.AccountManager;
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
import android.util.Log;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.SettingsConstants;

import static com.nextgis.maplib.util.Constants.NGW_ACCOUNT_TYPE;
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


    public SyncAdapter(
            Context context,
            boolean autoInitialize)
    {
        super(context, autoInitialize);
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
        Log.d(TAG, "onPerformSync");

        getContext().sendBroadcast(new Intent(SYNC_START));

        IGISApplication application = (IGISApplication) getContext();
        MapContentProviderHelper mapContentProviderHelper =
                (MapContentProviderHelper) application.getMap();

        if (null != mapContentProviderHelper) {
            mapContentProviderHelper.load(); // reload map for deleted/added layers
            sync(mapContentProviderHelper, authority, syncResult);
        }

        if (isCanceled()) {
            Log.d(Constants.TAG, "onPerformSync - SYNC_CANCELED is sent");
            getContext().sendBroadcast(new Intent(SYNC_CANCELED));
            return;
        }

        SharedPreferences settings = getContext().getSharedPreferences(
                Constants.PREFERENCES, Context.MODE_PRIVATE | Constants.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(
                SettingsConstants.KEY_PREF_LAST_SYNC_TIMESTAMP, System.currentTimeMillis());
        editor.commit();

        getContext().sendBroadcast(new Intent(SYNC_FINISH));
    }


    protected void sync(
            LayerGroup layerGroup,
            String authority,
            SyncResult syncResult)
    {
        for (int i = 0; i < layerGroup.getLayerCount(); i++) {
            if (isCanceled()) {
                return;
            }
            ILayer layer = layerGroup.getLayer(i);
            if (layer instanceof LayerGroup) {
                sync((LayerGroup) layer, authority, syncResult);
            } else if (layer instanceof INGWLayer) {
                INGWLayer ngwLayer = (INGWLayer) layer;
                ngwLayer.sync(authority, syncResult);
            }
        }
    }

    public static void setSyncPeriod(
            IGISApplication application,
            Bundle extras,
            long pollFrequency)
    {
        final AccountManager accountManager = AccountManager.get((Context) application);
        for (Account account : accountManager.getAccountsByType(NGW_ACCOUNT_TYPE)) {
            ContentResolver.addPeriodicSync(
                    account, application.getAuthority(), extras, pollFrequency);
        }
    }


    public boolean isCanceled()
    {
        return Thread.currentThread().isInterrupted();
    }
}
