/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016, 2018 NextGIS, info@nextgis.com
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

package com.nextgis.maplib.util;

public interface SettingsConstants
{
    String KEY_PREF_MAP                     = "map";
    String KEY_PREF_MAP_PATH                = "map_path";
    String KEY_PREF_LAST_SYNC_TIMESTAMP     = "last_sync_timestamp";
    String KEY_PREF_LOCATION_SOURCE         = "location_source";
    String KEY_PREF_LOCATION_MIN_TIME       = "location_min_time";
    String KEY_PREF_LOCATION_MIN_DISTANCE   = "location_min_distance";
    String KEY_PREF_LOCATION_ACCURATE_COUNT = "accurate_max_count";
    String KEY_PREF_LOCATION_ACCURATE_TIME  = "accurate_max_time";
    String KEY_PREF_LOCATION_ACCURATE_CE    = "accurate_ce";
    String KEY_PREF_TRACKS_MIN_TIME         = "tracks_min_time";
    String KEY_PREF_TRACKS_MIN_DISTANCE     = "tracks_min_distance";
    String KEY_PREF_TRACKS_SOURCE           = "tracks_location_source";
    String KEY_PREF_TRACK_RESTORE           = "track_restore";
    String KEY_PREF_TRACK_SEND              = "track_send";
    String KEY_PREF_UNITS                   = "preferred_units";
}
