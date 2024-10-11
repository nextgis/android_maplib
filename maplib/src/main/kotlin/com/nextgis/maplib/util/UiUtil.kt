/*
 * Project: NextGIS Mobile SDK
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 *
 * Created by Stanislav Petriakov on 07.10.19
 * Copyright © 2019 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.util

import android.content.Context
import android.content.res.ColorStateList
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton


/**
 * Runs a block in new Thread
 */
fun runAsync(action: () -> Unit) = Thread(Runnable(action)).start()

/**
 * Returns a color from resources by it's ID
 *
 * @return Int color
 */
fun Context.getColorCompat(color: Int) = ContextCompat.getColor(this, color)

/**
 * Tints a FAB's background
 *
 * @param resId color from resources
 */
fun FloatingActionButton.tint(@ColorRes resId: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        this.backgroundTintList = ColorStateList.valueOf(this.context.getColorCompat(resId))
}

/**
 * Tints a view's background
 *
 * @param resId color from resources
 */
fun View.tint(@ColorRes resId: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        this.backgroundTintList = ColorStateList.valueOf(this.context.getColorCompat(resId))
}

fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = ContextCompat.getSystemService(
        context,
        ConnectivityManager::class.java
    ) as ConnectivityManager

    val activeNetwork = connectivityManager.activeNetworkInfo
    var hasInternetCapability = false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        if (capabilities != null) {
            hasInternetCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    } else {
        // Pre API 23
        hasInternetCapability = activeNetwork?.isConnectedOrConnecting == true &&
                activeNetwork?.type == ConnectivityManager.TYPE_WIFI ||
                activeNetwork?.type == ConnectivityManager.TYPE_MOBILE
    }

    return activeNetwork != null && hasInternetCapability
}

/**
 * @return status bar height
 */
inline val Context.statusBarHeight: Int
    get() {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
