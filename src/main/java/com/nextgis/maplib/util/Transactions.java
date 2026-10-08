/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Alexey Kovalenko, alexey.kovalenko.v@gmail.com
  * *****************************************************************************
 * Copyright (c) 2012-2026 NextGIS, info@nextgis.com
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
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class Transactions {

    static  final String keyEpochPref = "epoch_";
    static  final String keyTIdPref = "trans_";
    static  final String keyTIdPrefList = "trans_list_";

    static public Integer loadEpoch(Context context, String mAccountName, int layerId){
        SharedPreferences sharedPreferences =PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = keyEpochPref + mAccountName + "_" + layerId;
        Integer tId = sharedPreferences.contains(prefKey) ?  sharedPreferences.getInt(prefKey , -1) : null;
        if (tId != null && tId == -1)
            return null;
        return tId;
    }

    static public void saveEpoch(Context context, String mAccountName, int layerId, int epoch){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = keyEpochPref + mAccountName + "_" + layerId;
        sharedPreferences.edit().putInt(prefKey, epoch).commit();
    }



    static public Integer loadCurrentTransactionId(Context context, String mAccountName, int layerId){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = keyTIdPref + mAccountName + "_" + layerId;
        Integer tId = sharedPreferences.contains(prefKey) ?  sharedPreferences.getInt(prefKey , -1) : null;
        if (tId != null && tId == -1)
            return null;
        return tId;
    }

    static public void saveCurrentTransactionId(Context context, String mAccountName, int layerId, int tId){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = keyTIdPref + mAccountName + "_" + layerId;
        sharedPreferences.edit().putInt(prefKey, tId).commit();
    }

    static public void clearCurrentTransactionId(Context context, String mAccountName, int layerId){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = keyTIdPref + mAccountName + "_" + layerId;
        sharedPreferences.edit().remove(prefKey).commit();
    }

    static public void clearCurrentTransactionList(Context context, String mAccountName, int layerId){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKeyList = keyTIdPrefList + mAccountName + "_" + layerId;
        sharedPreferences.edit().remove(prefKeyList).commit();
    }

    static public List<int[]> loadCurrentTransactionsList(Context context,
                                                            String mAccountName,
                                                          int layerId){
        return loadPairs(context, mAccountName, layerId);
    }

    public static List<int[]> loadPairs(Context context,
                                        String mAccountName,
                                        int layerId) {
        String prefKeyList = keyTIdPrefList + mAccountName + "_" + layerId;
        return loadPairsByKey(context, prefKeyList);
    }

    private static List<int[]> loadPairsByKey(Context context, String key) {
        String value = PreferenceManager.getDefaultSharedPreferences(context).getString(key, "");
        List<int[]> result = new ArrayList<>();
        if (value.isEmpty())
            return result;

        for (String pair : value.split(";")) {
            String[] values = pair.split(":");

            if (values.length == 4) {
                result.add(new int[]{
                        Integer.parseInt(values[0]),
                        Integer.parseInt(values[1]),
                        Integer.parseInt(values[2]),
                        Integer.parseInt(values[3])
                });
            }
        }
        return result;
    }

    // save 4 digits // pairID , featureID, chgangeID, lastChangeId (on send moment)
    public static void savePairs(Context context,
                                        String mAccountName,
                                        int layerId,
                                        List<int[]> pairs) {
        String prefKeyList = keyTIdPrefList + mAccountName + "_" + layerId;
        savePairsByKey(context, prefKeyList, pairs);
    }

    private static void savePairsByKey(Context context, String key, List<int[]> pairs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.size(); i++) {
            if (i > 0)
                sb.append(";");
            int[] pair = pairs.get(i);
                  sb.append(pair[0]).append(":")
                    .append(pair[1]).append(":")
                    .append(pair[2]).append(":")
                    .append(pair[3]);
        }
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, sb.toString())
                .apply();
    }
}
