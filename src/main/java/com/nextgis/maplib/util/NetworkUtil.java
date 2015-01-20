
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

package com.nextgis.maplib.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;

import static com.nextgis.maplib.util.Constants.*;


public class NetworkUtil
{
    protected final ConnectivityManager mConnectionManager;
    protected final TelephonyManager    mTelephonyManager;
    protected long mLastCheckTime;
    protected boolean mLastState;


    public NetworkUtil(Context context)
    {
        mConnectionManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mLastCheckTime = Constants.NOT_FOUND;
    }


    public boolean isNetworkAvailable()
    {
        if(System.currentTimeMillis() - mLastCheckTime < ONE_SECOND * 5)     //check every 5 sec.
            return mLastState;

        mLastCheckTime = System.currentTimeMillis();
        mLastState = false;

        if (mConnectionManager == null) {
            return false;
        }


        NetworkInfo info = mConnectionManager.getActiveNetworkInfo();
        if (info == null) //|| !cm.getBackgroundDataSetting()
        {
            return false;
        }

        int netType = info.getType();
        if (netType == ConnectivityManager.TYPE_WIFI) {
            mLastState = info.isConnected();
        } else if (netType ==
                   ConnectivityManager.TYPE_MOBILE) { // netSubtype == TelephonyManager.NETWORK_TYPE_UMTS
            if (mTelephonyManager != null && !mTelephonyManager.isNetworkRoaming()) {
                mLastState = info.isConnected();
            }
        }

        return mLastState;
    }

    public DefaultHttpClient getHttpClient()
    {
        /*HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_CONNECTION);
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOKET);
        */
        DefaultHttpClient HTTPClient = new DefaultHttpClient();//httpParameters);
        HTTPClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, APP_USER_AGENT);
        HTTPClient.getParams()
                  .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, TIMEOUT_CONNECTION);
        HTTPClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, TIMEOUT_SOKET);

        return HTTPClient;
    }
}
