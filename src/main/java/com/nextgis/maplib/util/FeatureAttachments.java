package com.nextgis.maplib.util;

import static com.nextgis.maplib.util.Constants.CHANGE_OPERATION_ATTACH;
import static com.nextgis.maplib.util.Constants.CHANGE_OPERATION_DELETE;
import static com.nextgis.maplib.util.Constants.CHANGE_OPERATION_NOT_SYNC;
import static com.nextgis.maplib.util.Constants.CHANGE_OPERATION_TEMP;
import static com.nextgis.maplib.util.Constants.FIELD_ATTACH_DESCRIPTION;
import static com.nextgis.maplib.util.Constants.FIELD_ATTACH_DISPLAYNAME;
import static com.nextgis.maplib.util.Constants.FIELD_ATTACH_ID;
import static com.nextgis.maplib.util.Constants.FIELD_ATTACH_MIMETYPE;
import static com.nextgis.maplib.util.Constants.FIELD_ATTACH_OPERATION;
import static com.nextgis.maplib.util.Constants.FIELD_FEATURE_ID;
import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.FIELD_OPERATION;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.Constants.TAG;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteReadOnlyDatabaseException;
import android.text.TextUtils;
import android.util.Log;

import com.hypertrack.hyperlog.HyperLog;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;

public class FeatureAttachments {

    public static void initialize(String tableName)
    {
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "init the change log for the layer " + tableName);

        String sqlCreateTable = "CREATE TABLE IF NOT EXISTS " + tableName + " ( ";
        sqlCreateTable += FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, ";
        sqlCreateTable += FIELD_FEATURE_ID + " INTEGER, ";
        //sqlCreateTable += FIELD_OPERATION + " INTEGER, ";
        sqlCreateTable += FIELD_ATTACH_ID + " INTEGER, ";
        sqlCreateTable += FIELD_ATTACH_DESCRIPTION + " TEXT, ";
        sqlCreateTable += FIELD_ATTACH_DISPLAYNAME + " TEXT, ";

        //sqlCreateTable += FIELD_ATTACH_OPERATION + " INTEGER, ";
        sqlCreateTable += FIELD_ATTACH_MIMETYPE + " TEXT";
        sqlCreateTable += " );";


        if (Constants.DEBUG_MODE)
            Log.d(TAG, "create the layer change table: " + sqlCreateTable);

        // create table
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        db.execSQL(sqlCreateTable);
    }

    public static void checkTable(String tableName){
        try {
            if (!checkTableExists(tableName))
                initialize(tableName);
        } catch (Exception ex){
            HyperLog.a("table not creates : " + tableName);

        }
    }

    public static boolean checkTableExists(String tableName){
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);


//                if(mDatabase == null || !mDatabase.isOpen()) {
//                    mDatabase = getReadableDatabase();
//                }
//
//                if(!mDatabase.isReadOnly()) {
//                    mDatabase.close();
//                    mDatabase = getReadableDatabase();
//                }
//            }

            String query = "select DISTINCT tbl_name from sqlite_master where tbl_name = '"+tableName+"'";
            try (Cursor cursor = db.rawQuery(query, null)) {
                if(cursor!=null) {
                    if(cursor.getCount()>0) {
                        return true;
                    }
                    cursor.close();
                }
                return false;
            }
        }


    public static Cursor query(
            String tableName,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder)

    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);

        try {
            return db.query(
                    tableName, projection, selection, selectionArgs, null, null, sortOrder, null);
        } catch (SQLiteException e) {
            Log.d(TAG, e.getLocalizedMessage());
            return null;
        }
    }


    public static Cursor query(
            String tableName,
            String selection,
            String sortOrder,
            String limit)
    {
        return query(tableName, null, selection, null, sortOrder );
    }


    public static long insert(
            String tableName,
            ContentValues values)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(false);
        return db.insert(tableName, null, values);
    }


    public static long replace(
            String tableName,
            ContentValues values)
    {
        long featureId = values.getAsLong(FIELD_FEATURE_ID);
        int featureOperation = values.getAsInteger(FIELD_OPERATION);
        long attachId = values.getAsLong(FIELD_ATTACH_ID);
        int attachOperation = values.getAsInteger(FIELD_ATTACH_OPERATION);

        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                FIELD_OPERATION + " = " + featureOperation + " AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                FIELD_ATTACH_OPERATION + " = " + attachOperation;

        Cursor cursor = query(tableName, selection, null, "1");
        long res = 0;

        if (null != cursor) {
            res = cursor.getCount();
            cursor.close();
        }

        if (res > 0) {
            return res;
        }

        return insert(tableName, values);
    }


    public static int update(
            String tableName,
            ContentValues values,
            String selection,
            String[] selectionArgs)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        return db.update(tableName, values, selection, selectionArgs);
    }


    public static int delete(
            String tableName,
            String selection,
            String[] selectionArgs)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);
        int retResult = 0;
        try {
            retResult = db.delete(tableName, selection, selectionArgs);
        } catch (SQLiteException e) {
            e.printStackTrace();
            Log.d(TAG, e.getLocalizedMessage());
        }
        return retResult;
    }


    public static int delete(
            String tableName,
            String selection)
    {
        return delete(tableName, selection, null);
    }


    public static void delete(String tableName)
    {
        try {
            MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
            SQLiteDatabase db = map.getDatabase(true);
            String tableDrop = "DROP TABLE IF EXISTS " + tableName;
            db.execSQL(tableDrop);
        } catch (SQLiteFullException | SQLiteReadOnlyDatabaseException e) {
            e.printStackTrace();
        }
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


    public static long getEntriesCount(String tableName)
    {
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);

        try {
            return DatabaseUtils.queryNumEntries(db, tableName);
        } catch (SQLiteException e) {
            e.printStackTrace();
            Log.d(TAG, e.getLocalizedMessage());
            return 0;
        }
    }


    protected static String getSelectionForSync()
    {
        return "( " +

                "0 == " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " AND " +
                "0 == " + FIELD_OPERATION + " & " + CHANGE_OPERATION_TEMP +
                " AND " +
                "0 == " + FIELD_OPERATION + " & " + CHANGE_OPERATION_NOT_SYNC +

                " OR " +

                "0 != " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " AND " +
                "0 == " + FIELD_ATTACH_OPERATION + " & " + CHANGE_OPERATION_TEMP +
                " AND " +
                "0 == " + FIELD_ATTACH_OPERATION + " & " + CHANGE_OPERATION_NOT_SYNC +

                " )";
    }


    public static long getChangeCount(String tableName)
    {
        String selection = getSelectionForSync();
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(true);

        try {
            // From sources of DatabaseUtils.queryNumEntries()
            String s = (!TextUtils.isEmpty(selection)) ? " where " + selection : "";
            return DatabaseUtils.longForQuery(db, "select count(*) from " + tableName + s, null);
        } catch (SQLiteException e) {
            e.printStackTrace();
            Log.d(TAG, e.getLocalizedMessage());
            return 0;
        }
    }


    public static Cursor getFirstChangeFromRecordId(
            String tableName,
            long recordId)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = FIELD_ID + " >= " + recordId + " AND " + getSelectionForSync();
        return query(tableName, selection, sortOrder, "1");
    }


    public static long getLastChangeRecordId(String tableName)
    {
        String sortOrder = FIELD_ID + " DESC";
        String selection = getSelectionForSync();
        Cursor cursor = query(tableName, selection, sortOrder, "1");
        long ret = NOT_FOUND;

        if (null == cursor) {
            return ret;
        }

        try {
            if (cursor.moveToFirst()) {
                ret = cursor.getLong(cursor.getColumnIndex(FIELD_ID));
            }
        } catch (Exception e) {
            //Log.d(TAG, e.getLocalizedMessage());
        } finally {
            cursor.close();
        }

        return ret;
    }


    public static Cursor getChanges(String tableName)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = getSelectionForSync();
        return query(tableName, selection, sortOrder, null);
    }


    public static boolean isChanges(String tableName)
    {
        String selection = getSelectionForSync();
        return isRecords(tableName, selection);
    }


    public static Cursor getChanges(
            String tableName,
            long featureId)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " + getSelectionForSync();
        return query(tableName, selection, sortOrder, null);
    }


    public static boolean isChanges(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " + getSelectionForSync();
        return isRecords(tableName, selection);
    }


    public static Cursor getChanges(
            String tableName,
            long featureId,
            int operation)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + operation + " ) )" + " AND "
                + getSelectionForSync();

        return query(tableName, selection, sortOrder, null);
    }


    public static boolean isChanges(
            String tableName,
            long featureId,
            int operation)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + operation + " ) )" + " AND "
                + getSelectionForSync();

        return isRecords(tableName, selection);
    }


    public static Cursor getAttachChanges(
            String tableName,
            long featureId)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) )" + " AND "
                + getSelectionForSync();

        return query(tableName, selection, sortOrder, null);
    }


    public static boolean isAttachChanges(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) )" + " AND "
                + getSelectionForSync();

        return isRecords(tableName, selection);
    }


    public static Cursor getAttachChanges(
            String tableName,
            long featureId,
            long attachId)
    {
        String sortOrder = FIELD_ID + " ASC";
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                getSelectionForSync();

        return query(tableName, selection, sortOrder, null);
    }


    public static boolean isAttachChanges(
            String tableName,
            long featureId,
            long attachId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                getSelectionForSync();

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
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + attachOperation + " ) )" + " AND "
                + getSelectionForSync();

        return query(tableName, selection, sortOrder, null);
    }


    public static boolean isAttachChanges(
            String tableName,
            long featureId,
            long attachId,
            int attachOperation)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + attachOperation + " ) )" + " AND " +
                getSelectionForSync();

        return isRecords(tableName, selection);
    }


    public static boolean isAttachesForDelete(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + CHANGE_OPERATION_DELETE +
                " ) )" + " AND " +
                getSelectionForSync();

        return isRecords(tableName, selection);
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

        return insert(tableName, values);
    }


    /*
       String sqlCreateTable = "CREATE TABLE IF NOT EXISTS " + tableName + " ( ";
        sqlCreateTable += FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, ";
        sqlCreateTable += FIELD_FEATURE_ID + " INTEGER, ";
        sqlCreateTable += FIELD_OPERATION + " INTEGER, ";
        sqlCreateTable += FIELD_ATTACH_ID + " INTEGER, ";
        sqlCreateTable += FIELD_ATTACH_DESCRIPTION + " TEXT, ";
        sqlCreateTable += FIELD_ATTACH_DISPLAYNAME + " TEXT, ";
        sqlCreateTable += FIELD_ATTACH_MIMETYPE + " TEXT, ";
        sqlCreateTable += FIELD_ATTACH_OPERATION + " INTEGER";
        sqlCreateTable += " );";
    * */

    public static long add(
            String tableName,
            long featureId,
            long attachId,
            String attachDescription,
            String displayName,
            String mimeType
            //int attachOperation
    )
    {
        ContentValues values = new ContentValues();
        values.put(FIELD_FEATURE_ID, featureId);
        //values.put(FIELD_OPERATION, CHANGE_OPERATION_ATTACH);
        values.put(FIELD_ATTACH_ID, attachId);
        //values.put(FIELD_ATTACH_OPERATION, attachOperation);
        values.put(FIELD_ATTACH_DESCRIPTION, attachDescription);
        values.put(FIELD_ATTACH_DISPLAYNAME, displayName);
        values.put(FIELD_ATTACH_MIMETYPE, mimeType);

        return insert(tableName, values);
    }


    public static int setOperation(
            String tableName,
            long recordId,
            int operation)
    {
        String selection = FIELD_ID + " = " + recordId;

        ContentValues values = new ContentValues();
        values.put(FIELD_OPERATION, operation);

        return update(tableName, values, selection, null);
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

        return update(tableName, values, selection, null);
    }


    public static int changeFeatureId(
            String tableName,
            long oldFeatureId,
            long newFeatureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + oldFeatureId;

        ContentValues values = new ContentValues();
        values.put(FIELD_FEATURE_ID, newFeatureId);

        return update(tableName, values, selection, null);
    }


    public static int changeFeatureIdForAttaches(
            String tableName,
            long oldFeatureId,
            long newFeatureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + oldFeatureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) )";

        ContentValues values = new ContentValues();
        values.put(FIELD_FEATURE_ID, newFeatureId);

        return update(tableName, values, selection, null);
    }


    public static int removeAllChanges(String tableName)
    {
        String selection = getSelectionForSync();
        return delete(tableName, selection);
    }


    public static int removeAllChangesToLast(
            String tableName,
            long lastRecordId)
    {
        String selection = FIELD_ID + " <= " + lastRecordId + " AND " + getSelectionForSync();
        return delete(tableName, selection);
    }


    public static int removeChanges(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " + getSelectionForSync();
        return delete(tableName, selection);
    }


    public static int removeChangesToLast(
            String tableName,
            long featureId,
            long lastRecordId)
    {
        String selection = FIELD_ID + " <= " + lastRecordId + " AND " +
                FIELD_FEATURE_ID + " = " + featureId + " AND " +
                getSelectionForSync();

        return delete(tableName, selection);
    }


    public static int removeChanges(
            String tableName,
            long featureId,
            int operation)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + operation + " ) )" + " AND " +
                getSelectionForSync();

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
                "( 0 != ( " + FIELD_OPERATION + " & " + operation + " ) )" + " AND " +
                getSelectionForSync();

        return delete(tableName, selection);
    }


    public static int removeAllAttachChanges(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) )" + " AND "
                + getSelectionForSync();

        return delete(tableName, selection);
    }


    public static int removeAllAttachChangesToLast(
            String tableName,
            long featureId,
            long lastRecordId)
    {
        String selection = FIELD_ID + " <= " + lastRecordId + " AND " +
                FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH + " ) )" + " AND "
                + getSelectionForSync();

        return delete(tableName, selection);
    }


    public static int removeAttachChanges(
            String tableName,
            long featureId,
            long attachId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                getSelectionForSync();

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
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                getSelectionForSync();

        return delete(tableName, selection);
    }


    public static int removeAttachChanges(
            String tableName,
            long featureId,
            long attachId,
            int attachOperation)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + attachOperation + " ) )" + " AND "
                + getSelectionForSync();

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
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + attachOperation + " ) )" + " AND " +
                getSelectionForSync();

        return delete(tableName, selection);
    }


    public static int removeChangeRecord(
            String tableName,
            long recordId)
    {
        String selection = FIELD_ID + " = " + recordId + " AND " + getSelectionForSync();
        return delete(tableName, selection);
    }


    public static boolean hasFeatureFlags(
            String tableName,
            long featureId)
    {
        return hasFeatureTempFlag(tableName, featureId)
                || hasFeatureNotSyncFlag(tableName, featureId);
    }


    public static boolean hasAttachFlags(
            String tableName,
            long featureId,
            long attachId)
    {
        return hasAttachTempFlag(tableName, featureId, attachId)
                || hasAttachNotSyncFlag(tableName, featureId, attachId);
    }


    public static boolean hasFeatureTempFlag(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_TEMP + " ) )";

        Cursor changesCursor = query(tableName, selection, null, "1");

        boolean res = false;
        if (null != changesCursor) {
            res = changesCursor.getCount() > 0;
            changesCursor.close();
        }

        return res;
    }


    public static boolean haveFeaturesNotSyncFlag(String tableName)
    {
        String selection =
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_NOT_SYNC + " ) )";

        Cursor changesCursor = query(tableName, selection, null, "1");

        boolean res = false;
        if (null != changesCursor) {
            res = changesCursor.getCount() > 0;
            changesCursor.close();
        }

        return res;
    }


    public static boolean hasFeatureNotSyncFlag(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_NOT_SYNC + " ) )";

        Cursor changesCursor = query(tableName, selection, null, "1");

        boolean res = false;
        if (null != changesCursor) {
            res = changesCursor.getCount() > 0;
            changesCursor.close();
        }

        return res;
    }


    public static boolean hasAttachTempFlag(
            String tableName,
            long featureId,
            long attachId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + CHANGE_OPERATION_TEMP + " ) )";

        Cursor changesCursor = query(tableName, selection, null, "1");

        boolean res = false;
        if (null != changesCursor) {
            res = changesCursor.getCount() > 0;
            changesCursor.close();
        }

        return res;
    }


    public static boolean hasAttachNotSyncFlag(
            String tableName,
            long featureId,
            long attachId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + CHANGE_OPERATION_NOT_SYNC + " ) )";

        Cursor changesCursor = query(tableName, selection, null, "1");

        boolean res = false;
        if (null != changesCursor) {
            res = changesCursor.getCount() > 0;
            changesCursor.close();
        }

        return res;
    }


    public static long setFeatureTempFlag(
            String tableName,
            long featureId)
    {
        ContentValues values = new ContentValues();
        values.put(FIELD_FEATURE_ID, featureId);
        values.put(FIELD_OPERATION, CHANGE_OPERATION_TEMP);
        values.put(FIELD_ATTACH_ID, NOT_FOUND);
        values.put(FIELD_ATTACH_OPERATION, 0);

        return replace(tableName, values);
    }


    public static long setFeatureNotSyncFlag(
            String tableName,
            long featureId)
    {
        ContentValues values = new ContentValues();
        values.put(FIELD_FEATURE_ID, featureId);
        values.put(FIELD_OPERATION, CHANGE_OPERATION_NOT_SYNC);
        values.put(FIELD_ATTACH_ID, NOT_FOUND);
        values.put(FIELD_ATTACH_OPERATION, 0);

        return replace(tableName, values);
    }


    public static long setAttachTempFlag(
            String tableName,
            long featureId,
            long attachId)
    {
        ContentValues values = new ContentValues();
        values.put(FIELD_FEATURE_ID, featureId);
        values.put(FIELD_OPERATION, CHANGE_OPERATION_ATTACH);
        values.put(FIELD_ATTACH_ID, attachId);
        values.put(FIELD_ATTACH_OPERATION, CHANGE_OPERATION_TEMP);

        return replace(tableName, values);
    }


    public static long setAttachNotSyncFlag(
            String tableName,
            long featureId,
            long attachId)
    {
        ContentValues values = new ContentValues();
        values.put(FIELD_FEATURE_ID, featureId);
        values.put(FIELD_OPERATION, CHANGE_OPERATION_ATTACH);
        values.put(FIELD_ATTACH_ID, attachId);
        values.put(FIELD_ATTACH_OPERATION, CHANGE_OPERATION_NOT_SYNC);

        return replace(tableName, values);
    }


    public static int deleteFeatureTempFlag(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_TEMP + " ) )";

        return delete(tableName, selection);
    }


    public static int deleteFeatureNotSyncFlag(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_NOT_SYNC + " ) )";

        return delete(tableName, selection);
    }


    public static int deleteAttachTempFlag(
            String tableName,
            long featureId,
            long attachId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId + " AND " +
                "( 0 != ( " + FIELD_OPERATION + " & " + CHANGE_OPERATION_ATTACH +
                " ) ) AND " +
                FIELD_ATTACH_ID + " = " + attachId + " AND " +
                "( 0 != ( " + FIELD_ATTACH_OPERATION + " & " + CHANGE_OPERATION_TEMP + " ) )";

        return delete(tableName, selection);
    }


    public static int deleteAllAttachments(
            String tableName,
            long featureId)
    {
        String selection = FIELD_FEATURE_ID + " = " + featureId;
        return delete(tableName, selection);
    }

}
