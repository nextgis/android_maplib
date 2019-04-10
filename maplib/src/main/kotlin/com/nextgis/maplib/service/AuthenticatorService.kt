/*
 * Project:  NextGIS Tracker
 * Purpose:  Software tracker for nextgis.com cloud
 * Author:   Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
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

package com.nextgis.maplib.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.nextgis.maplib.Authenticator

class AuthenticatorService : Service() {
    private lateinit var mAuthenticator: Authenticator

    override fun onCreate() {
        // Create a new authenticator object
        mAuthenticator = Authenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder = mAuthenticator.iBinder
}