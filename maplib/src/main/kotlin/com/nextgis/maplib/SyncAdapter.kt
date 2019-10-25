/*
 * Project:  NextGIS Tracker
 * Purpose:  Software tracker for nextgis.com cloud
 * Author:   Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
 * Copyright (c) 2018-2019 NextGIS <info@nextgis.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

package com.nextgis.maplib

import android.accounts.Account
import android.content.*
import android.os.Bundle
import android.content.Intent
import android.preference.PreferenceManager


internal class SyncAdapter @JvmOverloads constructor(context: Context, autoInitialize: Boolean,
                                            allowParallelSyncs: Boolean = false) :
    AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {

    enum class SyncEvent(val code: String) {
        START("com.nextgis.maplib.sync.START"),
        FINISH("com.nextgis.maplib.sync.FINISH"),
        CANCEL("com.nextgis.maplib.sync.CANCEL"),
        CHANGES("com.nextgis.maplib.sync.CHANGES")
    }

    private fun appendErrorMessage(errorMessage: String, appendMessage: String) : String {
        return if(errorMessage.isEmpty()) {
            appendMessage
        } else {
            "$errorMessage\r\n$appendMessage"
        }
    }

    override fun onPerformSync(account: Account?, extras: Bundle?, authority: String?, provider: ContentProviderClient?,
                               syncResult: SyncResult?) {
        printMessage("onPerformSync")

        API.init(context)
        context.sendBroadcast(Intent(SyncEvent.START.code))

        var errorMessage = ""
        val settings = PreferenceManager.getDefaultSharedPreferences(context) // context.getSharedPreferences(Constants.PREFERENCES, Constants.MODE_MULTI_PROCESS)
        val sendToNgw = settings.getBoolean(Constants.Settings.sendTracksToNGWKey, false)
        // Execute sync operations here

        // TODO: Sync vector layers

        // Sync tracks
        if(sendToNgw) {
            val sendPointMax = settings.getInt(Constants.Settings.sendTracksPointsMaxKey, 100)
            val store = API.getStore(API.getLastStoreName())
            val tracksTable = store?.trackTable()
            tracksTable?.sync(sendPointMax)

            errorMessage = appendErrorMessage(errorMessage, API.lastError())
            if(!errorMessage.isEmpty()) {
                if(syncResult?.stats != null) {
                    syncResult.stats.numIoExceptions++
                }
            }
        }

        with(settings.edit()) {
            putLong(Constants.Settings.lastSyncTimestampKey + "_" + account?.name.hashCode(), System.currentTimeMillis())
            putLong(Constants.Settings.lastSyncTimestampKey, System.currentTimeMillis())
            apply()
        }

        val finish = Intent(SyncEvent.FINISH.code)
        if(!errorMessage.isEmpty()) {
            finish.putExtra(Constants.Settings.exceptionKey, errorMessage)
        }
        context.sendBroadcast(finish)
    }

}