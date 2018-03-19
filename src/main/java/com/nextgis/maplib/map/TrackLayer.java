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

package com.nextgis.maplib.map;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import com.nextgis.maplib.R;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.display.TrackRenderer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;


public class TrackLayer
        extends Layer
{
    public static final String TABLE_TRACKS      = "tracks";
    public static final String TABLE_TRACKPOINTS = "trackpoints";

    public static final String FIELD_ID      = "_id";
    public static final String FIELD_NAME    = "name";
    public static final String FIELD_START   = "start";
    public static final String FIELD_END     = "end";
    public static final String FIELD_COLOR   = "color";
    public static final String FIELD_VISIBLE = "visible";

    public static final String FIELD_LON       = "lon";
    public static final String FIELD_LAT       = "lat";
    public static final String FIELD_ELE       = "ele";
    public static final String FIELD_FIX       = "fix";
    public static final String FIELD_SAT       = "sat";
    public static final String FIELD_TIMESTAMP = "time";
    public static final String FIELD_SESSION   = "session";
    public static final String FIELD_SENT      = "sent";
    public static final String FIELD_SPEED     = "speed";
    public static final String FIELD_ACCURACY  = "accuracy";

    static final String DB_CREATE_TRACKS      =
            "CREATE TABLE IF NOT EXISTS " + TABLE_TRACKS + " (" +
            FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            FIELD_NAME + " TEXT NOT NULL, " +
            FIELD_START + " INTEGER NOT NULL, " +
            FIELD_END + " INTEGER, " +
            FIELD_COLOR + " TEXT, " +
            FIELD_VISIBLE + " INTEGER NOT NULL);";
    static final String DB_CREATE_TRACKPOINTS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_TRACKPOINTS + " (" +
            FIELD_LON + " REAL NOT NULL, " +
            FIELD_LAT + " REAL NOT NULL, " +
            FIELD_ELE + " REAL, " +
            FIELD_FIX + " TEXT, " +
            FIELD_SAT + " INTEGER, " +
            FIELD_SPEED + " REAL, " +
            FIELD_ACCURACY + " REAL, " +
            FIELD_TIMESTAMP + " INTEGER NOT NULL, " +
            FIELD_SENT + " INTEGER NOT NULL, " +
            FIELD_SESSION + " INTEGER NOT NULL, FOREIGN KEY(" + FIELD_SESSION + ") REFERENCES " +
            TABLE_TRACKS + "(" + FIELD_ID + "));";

    private static final int TYPE_TRACKS       = 1;
    private static final int TYPE_TRACKPOINTS  = 2;
    private static final int TYPE_SINGLE_TRACK = 3;

    private static final int UPDATE = 1;
    private static final int INSERT = 2;
    private static final int DELETE = 3;

    private static String CONTENT_TYPE, CONTENT_TYPE_TRACKPOINTS, CONTENT_ITEM_TYPE;

    protected static int    mColor = Color.LTGRAY;
    protected Cursor mCursor;
    String         mAuthority;
    SQLiteDatabase mSQLiteDatabase;
    private UriMatcher mUriMatcher;
    private Uri        mContentUriTracks, mContentUriTrackpoints;
    private MapContentProviderHelper    mMap;
    private Map<Integer, GeoLineString> mTracks;


    public TrackLayer(
            Context context,
            File path)
    {
        super(context, path);

        if (!(getContext() instanceof IGISApplication)) {
            throw new IllegalArgumentException(
                    "The context should be the instance of IGISApplication");
        }

        IGISApplication app = (IGISApplication) getContext();
        mMap = (MapContentProviderHelper) MapBase.getInstance();
        mAuthority = app.getAuthority();

        if (mMap == null) {
            throw new IllegalArgumentException(
                    "Cannot get access to DB (context's MapBase is null)");
        }

        mContentUriTracks = Uri.parse("content://" + mAuthority + "/" + TABLE_TRACKS);
        mContentUriTrackpoints = Uri.parse("content://" + mAuthority + "/" + TABLE_TRACKPOINTS);

        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(mAuthority, TABLE_TRACKS, TYPE_TRACKS);
        mUriMatcher.addURI(mAuthority, TABLE_TRACKS + "/#", TYPE_SINGLE_TRACK);
        mUriMatcher.addURI(mAuthority, TABLE_TRACKPOINTS, TYPE_TRACKPOINTS);

        if (null == CONTENT_TYPE) {
            CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + mAuthority + "." + TABLE_TRACKS;
        }
        if (null == CONTENT_TYPE_TRACKPOINTS) {
            CONTENT_TYPE_TRACKPOINTS =
                    "vnd.android.cursor.dir/vnd." + mAuthority + "." + TABLE_TRACKPOINTS;
        }
        if (null == CONTENT_ITEM_TYPE) {
            CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + mAuthority + "." + TABLE_TRACKS;
        }

        initDB();

        mLayerType = Constants.LAYERTYPE_TRACKS;
        mRenderer = new TrackRenderer(this);
        mTracks = new HashMap<>();
    }


    @Override
    public String getName()
    {
        return mContext.getString(R.string.tracks);
    }


    public Map<Integer, GeoLineString> getTracks()
    {
        if (mTracks.size() == 0) {
            reloadTracks(INSERT);
        }

        return mTracks;
    }


    public void reloadTracks(int mode)
    {
        String[] proj = new String[] {FIELD_ID};
        String selection =
                FIELD_VISIBLE + " = 1 AND " + FIELD_END + " IS NOT NULL AND " + FIELD_END +
                " != ''";

        mCursor =
                mContext.getContentResolver().query(mContentUriTracks, proj, selection, null, null);

        if (null == mCursor) {
            return;
        }

        List<Integer> trackIds = new ArrayList<>();

        if (mCursor.moveToFirst()) {
            do {
                int trackId = mCursor.getInt(mCursor.getColumnIndex(TrackLayer.FIELD_ID));
                trackIds.add(trackId);
            } while (mCursor.moveToNext());

        }

        mCursor.close();

        switch (mode) {
            case UPDATE:
                Iterator itUpdate = mTracks.keySet().iterator();
                while (itUpdate.hasNext()) {
                    Integer entry = (Integer) itUpdate.next();
                    if (trackIds.contains(entry)) {
                        loadTrack(entry);
                    } else {
                        itUpdate.remove();
                    }
                }

                for (int key : trackIds) {
                    if (!mTracks.keySet().contains(key)) {
                        loadTrack(key);
                    }
                }
                break;
            case INSERT:
                for (int key : trackIds) {
                    if (!mTracks.keySet().contains(key)) {
                        loadTrack(key);
                    }
                }
                break;
            case DELETE:
                Iterator itDelete = mTracks.keySet().iterator();
                while (itDelete.hasNext()) {
                    Integer entry = (Integer) itDelete.next();
                    if (!trackIds.contains(entry)) {
                        itDelete.remove();
                    }
                }
                break;
        }
    }


    private void loadTrack(int trackId)
    {
        Cursor track = getTrack(trackId);

        if (track == null || !track.moveToFirst()) {
            return;
        }

        float x0 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LON)),
                y0 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LAT));

        GeoLineString trackLine = new GeoLineString();
        trackLine.setCRS(GeoConstants.CRS_WEB_MERCATOR);
        trackLine.add(new GeoPoint(x0, y0));

        while (track.moveToNext()) {
            x0 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LON));
            y0 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LAT));
            trackLine.add(new GeoPoint(x0, y0));
        }

        mTracks.put(trackId, trackLine);
    }


    private Cursor getTrack(int id)
    {
        if (mCursor == null) {
            throw new RuntimeException("Tracks' cursor is null");
        }

        String[] proj = new String[] {FIELD_LON, FIELD_LAT};

        return mContext.getContentResolver().query(
                Uri.withAppendedPath(mContentUriTracks, id + ""), proj, null, null, null);
    }


    public int getColor(long id) {
        return getColor(mContext, mContentUriTracks, id);
    }

    public static int getColor(Context context, Uri tracksUri, long id)
    {
        String selection = FIELD_ID + " = ?";
        Cursor cursor = context.getContentResolver().query(tracksUri, new String[] {FIELD_COLOR}, selection, new String[]{id + ""}, null);

        if (null == cursor) {
            return mColor;
        }

        int color = mColor;
        if (cursor.moveToFirst()) {
            if (!cursor.isNull(0))
                color = cursor.getInt(0);

            cursor.close();
        }

        return color;
    }


//    @Override
//    protected void notifyLayerChanged()
//    {
//        super.notifyLayerChanged();
//        mMap.onLayerChanged(this);
//    }


    private void initDB()
    {
        mSQLiteDatabase = mMap.getDatabase(false);

//        mSQLiteDatabase.execSQL("DROP TABLE IF EXISTS TRACKPOINTS;");
//        mSQLiteDatabase.execSQL("DROP TABLE IF EXISTS TRACKS;");

        mSQLiteDatabase.execSQL(DB_CREATE_TRACKS);
        mSQLiteDatabase.execSQL(DB_CREATE_TRACKPOINTS);
    }


    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder,
            String limit)
    {
        mSQLiteDatabase = mMap.getDatabase(true);
        Cursor cursor;

        switch (mUriMatcher.match(uri)) {
            case TYPE_TRACKS:
                cursor = mSQLiteDatabase.query(
                        TABLE_TRACKS, projection, selection, selectionArgs, null, null, sortOrder, limit);
                cursor.setNotificationUri(getContext().getContentResolver(), mContentUriTracks);
                return cursor;
            case TYPE_TRACKPOINTS:
                cursor = mSQLiteDatabase.query(
                        TABLE_TRACKPOINTS, projection, selection, selectionArgs, null, null,
                        sortOrder, limit);

                cursor.setNotificationUri(
                        getContext().getContentResolver(), mContentUriTrackpoints);
                return cursor;
            case TYPE_SINGLE_TRACK:
                String id = uri.getLastPathSegment();

                if (TextUtils.isEmpty(selection)) {
                    selection = FIELD_SESSION + " = " + id;
                } else {
                    selection = selection + " AND " + FIELD_SESSION + " = " + id;
                }

                cursor = mSQLiteDatabase.query(
                        TABLE_TRACKPOINTS, projection, selection, selectionArgs, null, null,
                        sortOrder, limit);
                return cursor;
            default:
                throw new IllegalArgumentException("Wrong tracks URI: " + uri);
        }
    }


    public Uri insert(
            Uri uri,
            ContentValues values)
    {
        mSQLiteDatabase = mMap.getDatabase(false);
        long id;
        Uri inserted;

        switch (mUriMatcher.match(uri)) {
            case TYPE_SINGLE_TRACK:
                values.remove(FIELD_ID);
            case TYPE_TRACKS:
                id = mSQLiteDatabase.insert(TABLE_TRACKS, null, values);
                inserted = ContentUris.withAppendedId(mContentUriTracks, id);
                break;
            case TYPE_TRACKPOINTS:
                id = mSQLiteDatabase.insert(TABLE_TRACKPOINTS, null, values);
                inserted = ContentUris.withAppendedId(mContentUriTrackpoints, id);
                break;
            default:
                throw new IllegalArgumentException("Wrong tracks URI: " + uri);
        }

        if (id != NOT_FOUND) {
//            notifyLayerChanged();
            reloadTracks(INSERT);
            getContext().getContentResolver().notifyChange(inserted, null);
        }

        return inserted;
    }


    public int delete(
            Uri uri,
            String selection,
            String[] selectionArgs)
    {
        mSQLiteDatabase = mMap.getDatabase(false);

        switch (mUriMatcher.match(uri)) {
            case TYPE_TRACKS:
                String trackpointsSel = selection.replace(FIELD_ID, FIELD_SESSION);
                mSQLiteDatabase.delete(TABLE_TRACKPOINTS, trackpointsSel, selectionArgs);
                break;
            case TYPE_SINGLE_TRACK:
            case TYPE_TRACKPOINTS:
                throw new IllegalArgumentException(
                        "Only multiple tracks deletion implemented (WHERE _id IN (?,...,?))");
            default:
                throw new IllegalArgumentException("Wrong tracks URI: " + uri);
        }

        int deleted = mSQLiteDatabase.delete(TABLE_TRACKS, selection, selectionArgs);

        if (deleted > 0) {
//            notifyLayerChanged();
            reloadTracks(DELETE);
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return deleted;
    }


    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs)
    {
        mSQLiteDatabase = mMap.getDatabase(false);
        int updated;
        String table = TABLE_TRACKS;

        switch (mUriMatcher.match(uri)) {
            case TYPE_SINGLE_TRACK:
                String id = uri.getLastPathSegment();

                if (TextUtils.isEmpty(selection)) {
                    selection = FIELD_ID + " = " + id;
                } else {
                    selection = selection + " AND " + FIELD_ID + " = " + id;
                }
            case TYPE_TRACKS:
                mMap.onLayerChanged(this);
//                notifyLayerChanged();
                break;
            case TYPE_TRACKPOINTS:
                table = TABLE_TRACKPOINTS;
                break;
//                throw new IllegalArgumentException("Trackpoints can't be updated");
            default:
                throw new IllegalArgumentException("Wrong tracks URI: " + uri);
        }

        updated = mSQLiteDatabase.update(table, values, selection, selectionArgs);

        if (updated > 0) {
            reloadTracks(UPDATE);
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return updated;
    }


    public String getType(Uri uri)
    {
        switch (mUriMatcher.match(uri)) {
            case TYPE_TRACKS:
                return CONTENT_TYPE;
            case TYPE_SINGLE_TRACK:
                return CONTENT_ITEM_TYPE;
            case TYPE_TRACKPOINTS:
                return CONTENT_TYPE_TRACKPOINTS;
        }

        return null;
    }

}
