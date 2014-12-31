/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplib.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class NetworkUtil {

    protected final ConnectivityManager mConnectionManager;
    protected final TelephonyManager mTelephonyManager;

    public NetworkUtil(Context context) {
        mConnectionManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public boolean isNetworkAvailable(){
        if(mConnectionManager == null)
            return false;


        NetworkInfo info = mConnectionManager.getActiveNetworkInfo();
        if (info == null ) //|| !cm.getBackgroundDataSetting()
            return false;

        int netType = info.getType();
        if (netType == ConnectivityManager.TYPE_WIFI) {
            return info.isConnected();
        }
        else if (netType == ConnectivityManager.TYPE_MOBILE){ // netSubtype == TelephonyManager.NETWORK_TYPE_UMTS
            if(mTelephonyManager == null)
                return false;
            if(mTelephonyManager.isNetworkRoaming())
                return info.isConnected();
        }

        return false;
    }
}
