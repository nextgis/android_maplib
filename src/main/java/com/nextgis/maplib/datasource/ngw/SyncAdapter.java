/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplib.datasource.ngw;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;

import static com.nextgis.maplib.util.Constants.TAG;
/*
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


    @Override
    public void onPerformSync(
            Account account,
            Bundle bundle,
            String s,
            ContentProviderClient contentProviderClient,
            SyncResult syncResult)
    {
        Log.d(TAG, "onPerformSync");
        //1. get remote changes

        //2. send current changes
        //MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
    }
}
