package com.nextgis.maplib.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.nextgis.maplib.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MbTilesInfo {

    public static final String MBTILES_FILENAME = "map-mbtiles.mbtiles";


    public boolean valid;
    public boolean raster;
    public boolean vector;
    public String format;
    public int minZoom = -1;
    public int maxZoom = -1;
    public String name;
    public String bounds;

    // true if bounds parsed
    public boolean boundsValid;

    // coordinates from bounds
    public double west;
    public double south;
    public double east;
    public double north;


    public String error;

    public static MbTilesInfo checkMbTiles(File file, Context context) {
        MbTilesInfo info = new MbTilesInfo();

        if (file == null || !file.exists()) {
            info.error = context.getString(R.string.mbtiles_problem_not_found);
            return info;
        }

        SQLiteDatabase db = null;

        try {
            db = SQLiteDatabase.openDatabase(
                    file.getAbsolutePath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY);

            // ---------- integrity ----------
            Cursor c = db.rawQuery("PRAGMA integrity_check;", null);

            if (!c.moveToFirst() || !"ok".equalsIgnoreCase(c.getString(0))) {
                info.error = context.getString(R.string.mbtiles_problem_integry);
                c.close();
                return info;
            }
            c.close();

            // ---------- metadata ----------
            c = db.rawQuery(
                    "SELECT value FROM metadata WHERE name=?",
                    new String[]{"format"});

            if (c.moveToFirst())
                info.format = c.getString(0);

            c.close();

            c = db.rawQuery(
                    "SELECT value FROM metadata WHERE name=?",
                    new String[]{"name"});

            if (c.moveToFirst())
                info.name = c.getString(0);

            c.close();

            c = db.rawQuery(
                    "SELECT value FROM metadata WHERE name=?",
                    new String[]{"bounds"});

            if (c.moveToFirst())
                info.bounds = c.getString(0);

            if (info.bounds != null) {
                try {
                    String[] p = info.bounds.split(",");

                    if (p.length == 4) {
                        info.west = Double.parseDouble(p[0].trim());
                        info.south = Double.parseDouble(p[1].trim());
                        info.east = Double.parseDouble(p[2].trim());
                        info.north = Double.parseDouble(p[3].trim());

                        info.boundsValid = true;
                    }
                } catch (NumberFormatException ignored) {
                    info.boundsValid = false;
                }
            }


            c.close();

            c = db.rawQuery(
                    "SELECT value FROM metadata WHERE name=?",
                    new String[]{"minzoom"});

            if (c.moveToFirst())
                info.minZoom = Integer.parseInt(c.getString(0));

            c.close();

            c = db.rawQuery(
                    "SELECT value FROM metadata WHERE name=?",
                    new String[]{"maxzoom"});

            if (c.moveToFirst())
                info.maxZoom = Integer.parseInt(c.getString(0));

            c.close();

            if (info.format != null) {
                String f = info.format.toLowerCase();

                info.raster =
                        f.equals("png") ||
                                f.equals("jpg") ||
                                f.equals("jpeg") ||
                                f.equals("webp");

                info.vector =
                        f.equals("pbf") ||
                                f.equals("mvt");
            }

            info.valid = true;

        } catch (Exception e) {
            info.error = e.getMessage();
        } finally {
            if (db != null)
                db.close();
        }

        return info;
    }


    public static boolean isSQLiteFile(File file) {
        if (file == null || !file.isFile() || file.length() < 100)
            return false;

        byte[] magic = new byte[16];

        try (FileInputStream in = new FileInputStream(file)) {
            if (in.read(magic) != 16)
                return false;

            String header = new String(magic, StandardCharsets.US_ASCII);
            return "SQLite format 3\u0000".equals(header);
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isValidSQLite(File file) {
        SQLiteDatabase db = null;

        try {
            db = SQLiteDatabase.openDatabase(
                    file.getAbsolutePath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY);

            db.rawQuery("SELECT name FROM sqlite_master LIMIT 1", null).close();

            return true;

        } catch (Exception e) {
            return false;

        } finally {
            if (db != null)
                db.close();
        }
    }

    // check if file ready to load from maplibre lib
    public static boolean isMbtileFileReadyForLoad(String filepath){
        File file = new File(filepath);
        if (file.exists() && file.length() > 0)
            if (isSQLiteFile(file) && isValidSQLite(file))
                return true;
        return false;
    }

}
