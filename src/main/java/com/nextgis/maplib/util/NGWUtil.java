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

public class NGWUtil
{
     /*
    NGW API Functions
     */


    public static String getGeoJSONUrl(
            String server,
            long remoteId)
    {
        if (!server.startsWith("http"))
            server = "http://" + server;
        return server + "/resource/" + remoteId + "/geojson/";
    }


    public static String getVectorDataUrl(
            String server,
            long remoteId)
    {
        if (!server.startsWith("http"))
            server = "http://" + server;
        return server + "/api/resource/" + remoteId + "/feature/";
    }


    public static String getTMSUrl(
        String server,
        long styleId)
    {
        if (!server.startsWith("http"))
            server = "http://" + server;
        return server + "/resource/" + styleId + "//tms?z={z}&x={x}&y={y}";
    }

    public static String getResourceUrl(
            String server,
            long remoteId)
    {
        if (!server.startsWith("http"))
            server = "http://" + server;
        return server + "/resource/" + remoteId;
    }

    public static String getFeatureUrl(
            String server,
            long remoteId,
            long featureId)
    {
        if (!server.startsWith("http"))
            server = "http://" + server;
        return server + "/api/resource/" + remoteId + "/feature/" + featureId;
    }
}
