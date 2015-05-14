/*******************************************************************************
 * Project:  NextGIS mobile apps for Compulink
 * Purpose:  Mobile GIS for Android
 * Authors:  Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 *           NikitaFeodonit, nfeodonit@yandex.com
 * *****************************************************************************
 * Copyright (C) 2014-2015 NextGIS
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
 ******************************************************************************/

package com.nextgis.maplib.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;

import static com.nextgis.maplib.util.Constants.*;


public class FeatureChanges
{
    public static void initialize(String tableName)
    {
        Log.d(TAG, "init the change log for the layer " + tableName);

        String sqlCreateTable = "CREATE TABLE IF NOT EXISTS " + tableName + " ( ";
        sqlCreateTable += FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, ";
        sqlCreateTable += FIELD_FEATURE_ID + " INTEGER, ";
        sqlCreateTable += FIELD_OPERATION + " INTEGER, ";
        sqlCreateTable += FIELD_ATTACH_ID + " INTEGER, ";
        sqlCreateTable += FIELD_ATTACH_OPERATION + " INTEGER";
        sqlCreateTable += " );";

        Log.d(TAG, "create the layer change table: " + sqlCreateTable);

        // create table
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        db.execSQL(sqlCreateTable);
    }


    public static long getChangeCount(String tableName)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);

        try {
            return DatabaseUtils.queryNumEntries(db, tableName);
        }
        catch (SQLiteException e) {
            e.printStackTrace();
            return 0;
        }
    }


    public static Cursor getFirstChangeFromRecordId(
            String tableName,
            long recordId)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = FIELD_ID + " >= " + recordId;

        return query(tableName, selection, sortOrder, "1");
    }


    public static long getLastChangeRecordId(String tableName)
    {
        String sortOrder = FIELD_ID + " DESC";
        Cursor cursor = query(tableName, null, sortOrder, "1");
        long ret = NOT_FOUND;

        if (null == cursor) {
            return ret;
        }

        if (cursor.moveToFirst()) {
             ret = cursor.getLong(cursor.getColumnIndex(FIELD_ID));
        }

        cursor.close();
        return ret;
    }


    public static Cursor getChanges(String tableName)
    {
        String sortOrder = FIELD_ID + " ASC";

        return query(tableName, null, sortOrder, null);
    }


    public static boolean isChanges(String tableName)
    {
        return isRecords(tableName, null);
    }


    public static Cursor getChanges(
            String tableName,
            long featureId)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = FIELD_FEATURE_ID + " = " + featureId;

        return query(tableName, selection, sortOrder, null);
    }


    public static boolean isChanges(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId;
        return isRecords(tableName, selection);
    }


    public static Cursor getChanges(
            String tableName,
            long featureId,
            int operation)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + operation + " ) )";

        return query(tableName, selection, sortOrder, null);
    }


    public static boolean isChanges(
            String tableName,
            long featureId,
            int operation)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + operation + " ) )";

        return isRecords(tableName, selection);
    }


    public static Cursor getAttachChanges(
            String tableName,
            long featureId)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) )";

        return query(tableName, selection, sortOrder, null);
    }


    public static boolean isAttachChanges(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) )";

        return isRecords(tableName, selection);
    }


    public static Cursor getAttachChanges(
            String tableName,
            long featureId,
            long attachId)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId;

        return query(tableName, selection, sortOrder, null);
    }


    public static boolean isAttachChanges(
            String tableName,
            long featureId,
            long attachId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId;

        return isRecords(tableName, selection);
    }


    public static Cursor getAttachChanges(
            String tableName,
            long featureId,
            long attachId,
            int attachOperation)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + attachOperation + " ) )";

        return query(tableName, selection, sortOrder, null);
    }


    public static boolean isAttachChanges(
            String tableName,
            long featureId,
            long attachId,
            int attachOperation)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + attachOperation + " ) )";

        return isRecords(tableName, selection);
    }


    public static boolean isAttachesForDelete(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) ) AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + CHANGE_OPERATION_DELETE + " ) )";

        return isRecords(tableName, selection);
    }


    public static boolean isRecords(
            String tableName,
            String selection)
    {
        Cursor cursor = query(tableName, selection, null, "1");
        boolean ret = false;

        if (null != cursor) {
            if (cursor.getCount() > 0) {
                ret = true;
            }
            cursor.close();
        }

        return ret;
    }


    public static Cursor query(
            String tableName,
            String selection,
            String sortOrder,
            String limit)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);

        try {
            return db.query(tableName, null, selection, null, null, null, sortOrder, limit);
        } catch (SQLiteException e) {
            Log.d(TAG, e.getLocalizedMessage());
            return null;
        }
    }


    public static long add(
            String tableName,
            long featureId,
            int operation)
    {
        ContentValues values = new ContentValues();
        values.put(FIELD_FEATURE_ID, featureId);
        values.put(FIELD_OPERATION, operation);
        values.put(FIELD_ATTACH_ID, NOT_FOUND);
        values.put(FIELD_ATTACH_OPERATION, 0);

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        return db.insert(tableName, null, values);
    }


    public static long add(
            String tableName,
            long featureId,
            long attachId,
            int attachOperation)
    {
        ContentValues values = new ContentValues();
        values.put(FIELD_FEATURE_ID, featureId);
        values.put(FIELD_OPERATION, CHANGE_OPERATION_ATTACH);
        values.put(FIELD_ATTACH_ID, attachId);
        values.put(FIELD_ATTACH_OPERATION, attachOperation);

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        return db.insert(tableName, null, values);
    }


    public static int setOperation(
            String tableName,
            long recordId,
            int operation)
    {
        String selection = FIELD_ID + " = " + recordId;

        ContentValues values = new ContentValues();
        values.put(FIELD_OPERATION, operation);

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        return db.update(tableName, values, selection, null);
    }


    public static int setOperation(
            String tableName,
            long recordId,
            long featureId,
            long attachId,
            int attachOperation)
    {
        String selection = FIELD_ID + " = " + recordId + " AND " +
                FIELD_FEATURE_ID + " = " + featureId + " AND " +
                FIELD_ATTACH_ID + " = " + attachId;

        ContentValues values = new ContentValues();
        values.put(FIELD_ATTACH_OPERATION, attachOperation);

        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        return db.update(tableName, values, selection, null);
    }


    public static int removeAllChanges(String tableName)
    {
        return delete(tableName, null);
    }


    public static int removeAllChangesToLast(
            String tableName,
            long lastRecordId)
    {
        String selection = FIELD_ID + " <= " + lastRecordId;
        return delete(tableName, selection);
    }


    public static int removeChanges(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId;
        return delete(tableName, selection);
    }


    public static int removeChangesToLast(
            String tableName,
            long featureId,
            long lastRecordId)
    {
        String selection = FIELD_ID + " <= " + lastRecordId + " AND " +
                FIELD_FEATURE_ID + " = " + featureId;

        return delete(tableName, selection);
    }


    public static int removeChanges(
            String tableName,
            long featureId,
            int operation)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + operation + " ) )";

        return delete(tableName, selection);
    }


    public static int removeChangesToLast(
            String tableName,
            long featureId,
            int operation,
            long lastRecordId)
    {
        String selection = FIELD_ID + " <= " + lastRecordId + " AND " +
                FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + operation + " ) )";

        return delete(tableName, selection);
    }


    public static int removeAllAttachChanges(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) )";

        return delete(tableName, selection);
    }


    public static int removeAllAttachChangesToLast(
            String tableName,
            long featureId,
            long lastRecordId)
    {
        String selection = FIELD_ID + " <= " + lastRecordId + " AND " +
                FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) )";

        return delete(tableName, selection);
    }


    public static int removeAttachChanges(
            String tableName,
            long featureId,
            long attachId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId;

        return delete(tableName, selection);
    }


    public static int removeAttachChangesToLast(
            String tableName,
            long featureId,
            long attachId,
            long lastRecordId)
    {
        String selection = FIELD_ID + " <= " + lastRecordId + " AND " +
                FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId;

        return delete(tableName, selection);
    }


    public static int removeAttachChanges(
            String tableName,
            long featureId,
            long attachId,
            int attachOperation)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + attachOperation + " ) )";

        return delete(tableName, selection);
    }


    public static int removeAttachChangesToLast(
            String tableName,
            long featureId,
            long attachId,
            int attachOperation,
            long lastRecordId)
    {
        String selection = FIELD_ID + " <= " + lastRecordId + " AND " +
                FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + attachOperation + " ) )";

        return delete(tableName, selection);
    }


    public static int removeChangeRecord(
            String tableName,
            long recordId)
    {
        String selection = FIELD_ID + " = " + recordId;
        return delete(tableName, selection);
    }


    public static int delete(
            String tableName,
            String selection)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        return db.delete(tableName, selection, null);
    }

    public static void delete(String tableName){
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        String tableDrop = "DROP TABLE IF EXISTS " + tableName;
        db.execSQL(tableDrop);
    }

}
