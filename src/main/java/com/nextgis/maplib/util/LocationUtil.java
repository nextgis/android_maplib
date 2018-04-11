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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;

import com.nextgis.maplib.R;
import com.nextgis.maplib.location.GpsEventSource;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import static android.content.Context.MODE_MULTI_PROCESS;

public class LocationUtil
{
    public static final char DEGREE_CHAR = (char) 0x00B0;


    public static String formatLatitude(
            double latitude,
            int outputType,
            int fraction,
            Resources res)
    {
        String direction = (String) res.getText(R.string.N);

        if (latitude < 0) {
            direction = (String) res.getText(R.string.S);
            latitude = -latitude;
        }

        return formatCoordinate(latitude, outputType, fraction) + " " + direction;
    }


    public static String formatLongitude(
            double longitude,
            int outputType,
            int fraction,
            Resources res)
    {
        String direction = (String) res.getText(R.string.E);

        if (longitude < 0) {
            direction = (String) res.getText(R.string.W);
            longitude = -longitude;
        }

        return formatCoordinate(longitude, outputType, fraction) + " " + direction;
    }


    public static String formatCoordinate(
            double coordinate,
            int outputType,
            int fraction)
    {
        StringBuilder sb = new StringBuilder();
        char endChar = DEGREE_CHAR;
        String appendix = "";
        for (int i = 0; i < fraction; i++)
            appendix += "0";

        DecimalFormat df = new DecimalFormat("###." + appendix);
        if (outputType == Location.FORMAT_MINUTES || outputType == Location.FORMAT_SECONDS) {
            df = new DecimalFormat("##." + appendix);

            int degrees = (int) Math.floor(coordinate);
            sb.append(degrees);
            sb.append(DEGREE_CHAR); // degrees sign
            endChar = '\''; // minutes sign
            coordinate -= degrees;
            coordinate *= 60.0;

            if (outputType == Location.FORMAT_SECONDS) {
                df = new DecimalFormat("##." + appendix);

                int minutes = (int) Math.floor(coordinate);
                sb.append(minutes);
                sb.append('\''); // minutes sign
                endChar = '\"'; // seconds sign
                coordinate -= minutes;
                coordinate *= 60.0;
            }
        }

        sb.append(df.format(coordinate));
        sb.append(endChar);

        return sb.toString();
    }


    public static String formatLength(Context context, double length, int precision) {
        SharedPreferences preferences = context.getSharedPreferences(context.getPackageName() + "_preferences", MODE_MULTI_PROCESS);
        boolean metric = preferences.getString(SettingsConstants.KEY_PREF_UNITS, "metric").equals("metric");
        int unit = metric ? R.string.unit_meter : R.string.unit_foot;
        if (metric) {
            if (length >= 1000) {
                length /= 1000;
                unit = R.string.unit_kilometer;
            }
        } else {
            length /= 0.3048;
            if (length >= 5280) {
                length /= 5280;
                unit = R.string.unit_mile;
            }
        }

        String format = precision > 0 ? "%." + precision + "f" : "%.0f";
        return String.format(format + " %s", length, context.getString(unit));
    }


    public static String formatArea(Context context, double length) {
        SharedPreferences preferences = context.getSharedPreferences(context.getPackageName() + "_preferences", MODE_MULTI_PROCESS);
        boolean metric = preferences.getString(SettingsConstants.KEY_PREF_UNITS, "metric").equals("metric");
        int unit = metric ? R.string.unit_square_meter : R.string.unit_square_foot;
        if (metric) {
            if (length >= 1000000) {
                length /= 1000000;
                unit = R.string.unit_square_kilometer;
            }
        } else {
            length /= Math.pow(0.3048, 2);
            if (length >= 27878398.8) {
                length /= 27878398.8;
                unit = R.string.unit_square_mile;
            }
        }

        return String.format("%.3f %s", length, context.getString(unit));
    }


    public static void writeLocationToExif(
            File imgFile,
            Location location)
            throws IOException
    {
        if (location == null) {
            return;
        }

        ExifInterface exif = new ExifInterface(imgFile.getCanonicalPath());

        double lat = location.getLatitude();
        double absLat = Math.abs(lat);
        String dms = Location.convert(absLat, Location.FORMAT_SECONDS);
        String[] splits = dms.split(":");
        String[] secondsArr = (splits[2]).split("\\.");
        String seconds;

        if (secondsArr.length == 0) {
            seconds = splits[2];
        } else {
            seconds = secondsArr[0];
        }

        String latitudeStr = splits[0] + "/1," + splits[1] + "/1," + seconds + "/1";
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latitudeStr);
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, lat > 0 ? "N" : "S");

        double lon = location.getLongitude();
        double absLon = Math.abs(lon);
        dms = Location.convert(absLon, Location.FORMAT_SECONDS);
        splits = dms.split(":");
        secondsArr = (splits[2]).split("\\.");

        if (secondsArr.length == 0) {
            seconds = splits[2];
        } else {
            seconds = secondsArr[0];
        }

        String longitudeStr = splits[0] + "/1," + splits[1] + "/1," + seconds + "/1";
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, longitudeStr);
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lon > 0 ? "E" : "W");

        exif.saveAttributes();
    }


    public static boolean isProviderEnabled(
            Context context,
            String provider,
            boolean isTracks)
    {
        int currentProvider = 0;

        switch (provider) {
            case LocationManager.GPS_PROVIDER:
                currentProvider = GpsEventSource.GPS_PROVIDER;
                break;
            case LocationManager.NETWORK_PROVIDER:
                currentProvider = GpsEventSource.NETWORK_PROVIDER;
                break;
        }

        String preferenceKey = isTracks
                               ? SettingsConstants.KEY_PREF_TRACKS_SOURCE
                               : SettingsConstants.KEY_PREF_LOCATION_SOURCE;

        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getPackageName() + "_preferences", MODE_MULTI_PROCESS);
        int providers = Integer.parseInt(sharedPreferences.getString(preferenceKey, "3"));

        return 0 != (providers & currentProvider);
    }


}
