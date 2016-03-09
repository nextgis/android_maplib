/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public final class PermissionUtil {

    public static boolean hasPermission(Context context, String permission) {

        PackageManager pm = context.getPackageManager();
        if(pm == null) {
            if(Constants.DEBUG_MODE)
                Log.d(Constants.TAG, "Permission " + permission + " is not granted");
            return false;
        }
        int hasPerm = pm.checkPermission(permission, context.getPackageName());
        if(Constants.DEBUG_MODE)
            Log.d(Constants.TAG, "Permission " + permission + " is " +
                    (hasPerm == PackageManager.PERMISSION_GRANTED ? "granted" : "not granted"));
        return hasPerm == PackageManager.PERMISSION_GRANTED;
    }

}
