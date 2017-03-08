/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

import com.nextgis.maplib.BuildConfig;

import java.util.concurrent.TimeUnit;


public interface Constants
{
    String  TAG                = "nextgismobile";
    String  PREFERENCES        = "nextgismobile";
    int     MODE_MULTI_PROCESS = 0x4;
    int     NOT_FOUND          = -1;
    boolean DEBUG_MODE         = true;

    int NGW_v3 = 3;

    /**
     * HTTP parameters
     */
    String APP_USER_AGENT     = "NextGIS Mobile maplib v" + BuildConfig.VERSION_NAME;
    int    IO_BUFFER_SIZE     = 32 * 1024; // 32k
    int    MAX_CONTENT_LENGTH = 5 * 1024 * 1024; //5Mb

    long MIN_LOCAL_FEATURE_ID = 10000000;
    int  MAX_TILES_COUNT      = 6001;

    /**
     * NGW account type
     */
    String NGW_ACCOUNT_TYPE = "com.nextgis.account";
    String NGW_ACCOUNT_GUEST = "anonymous";

    /**
     * Map parameters
     */
    float MIN_SCROLL_STEP = 5.5f;
    int   MAP_LIMITS_NO   = 1; // no limits to scroll map
    int   MAP_LIMITS_X    = 2; // limit to scroll map by x axis
    int   MAP_LIMITS_Y    = 3; // limit to scroll map by y axis
    int   MAP_LIMITS_XY   = 4; // limit to scroll map by x & y axis

    /**
     * The additional size to off screen drawing from 1 and higher As more than more memory needed
     */

    float OFFSCREEN_EXTRASIZE_RATIO = 1.5f;
    int   DEFAULT_TILE_SIZE         = 256;

    /**
     * thread priorities and delays
     */
    int DEFAULT_DRAW_THREAD_PRIORITY       = android.os.Process.THREAD_PRIORITY_DEFAULT + 11;
    int DEFAULT_DOWNLOAD_THREAD_PRIORITY   = android.os.Process.THREAD_PRIORITY_BACKGROUND + 3;
    int DEFAULT_LOAD_LAYER_THREAD_PRIORITY = Thread.MIN_PRIORITY;
    int DEFAULT_EXECUTION_DELAY            = 450;

    /**
     * tune line string and linear ring simplifier
     */
    double SIMPLIFY_TOENV_AREA_MULTIPLY = 1.5;
    // area multiplier to skip if greater than quad tolerance
    double SIMPLIFY_SKIP_AREA_MULTIPLY  = 5;
    int    SAMPLE_DISTANCE_PX           = 5;


    String CONFIG       = "config.json";
    String LAYER_PREFIX = "layer_";
    String MAP_EXT      = ".ngm";

    /**
     * notifications
     */
    String NOTIFY_DELETE            = "com.nextgis.maplib.notify_delete";
    String NOTIFY_DELETE_ALL        = "com.nextgis.maplib.notify_delete_all";
    String NOTIFY_INSERT            = "com.nextgis.maplib.notify_insert";
    String NOTIFY_UPDATE            = "com.nextgis.maplib.notify_update";
    String NOTIFY_UPDATE_ALL        = "com.nextgis.maplib.notify_update_all";
    String NOTIFY_UPDATE_FIELDS     = "com.nextgis.maplib.notify_update_fields";
    String NOTIFY_FEATURE_ID_CHANGE = "com.nextgis.maplib.notify_change_id";

    String NOTIFY_LAYER_NAME = "layer_name";

    /**
     * JSON keys
     */
    String JSON_ID_KEY            = "id";
    String JSON_NAME_KEY          = "name";
    String JSON_VISIBILITY_KEY    = "visible";
    String JSON_LEVELS_KEY        = "levels";
    String JSON_LEVEL_KEY         = "level";
    String JSON_TYPE_KEY          = "type";
    String JSON_MAXLEVEL_KEY      = "max_level";
    String JSON_MINLEVEL_KEY      = "min_level";
    String JSON_LAYERS_KEY        = "layers";
    String JSON_LAYER_KEY         = "layer";
    String JSON_PATH_KEY          = "path";
    String JSON_BBOX_MINX_KEY     = "bbox_minx";
    String JSON_BBOX_MINY_KEY     = "bbox_miny";
    String JSON_BBOX_MAXX_KEY     = "bbox_maxx";
    String JSON_BBOX_MAXY_KEY     = "bbox_maxy";
    String JSON_RENDERERPROPS_KEY = "renderer_properties";
    String JSON_STYLE_RULE_KEY    = "style_rule";
    String JSON_WIDTH_KEY         = "width";
    String JSON_COLOR_KEY         = "color";
    String JSON_OUTCOLOR_KEY      = "out_color";
    String JSON_CHANGES_KEY       = "changes";
    String JSON_VALUE_KEY         = "value";
    String JSON_SIZE_KEY          = "size";
    String JSON_TEXT_SIZE_KEY     = "text_size";
    String JSON_DISPLAY_NAME      = "display_name";
    String JSON_RESOURCE_KEY      = "resource";
    String JSON_MESSAGE_KEY       = "message";
    String JSON_EXTENT_KEY        = "extent";
    String JSON_MIN_LAT_KEY        = "minLat";
    String JSON_MAX_LAT_KEY        = "maxLat";
    String JSON_MIN_LON_KEY        = "minLon";
    String JSON_MAX_LON_KEY        = "maxLon";

    /**
     * database fields
     */
    String FIELD_ID               = "_id";
    String FIELD_OLD_ID           = "old_id";
    String FIELD_GEOM             = "_geom";
    String FIELD_GEOM_            = "_geom_";
    String FIELD_FEATURE_ID       = "feature_id";
    String FIELD_OPERATION        = "operation";
    String FIELD_ATTACH_ID        = "attach_id";
    String FIELD_ATTACH_OPERATION = "attach_operation";

    String ATTRIBUTES_ONLY = "attributes_only";

    /**
     * Layer type
     */
    String CE50 = "CE50";
    String CE90 = "CE90";
    String CE95 = "CE95";
    String CE98 = "CE98";

    /**
     * Layer type
     */
    int LAYERTYPE_REMOTE_TMS   = 1 << 0;
    int LAYERTYPE_NGW_RASTER   = 1 << 1;
    int LAYERTYPE_NGW_VECTOR   = 1 << 2;
    int LAYERTYPE_GROUP        = 1 << 3;
    int LAYERTYPE_LOCAL_VECTOR = 1 << 4;
    int LAYERTYPE_LOCAL_TMS    = 1 << 5;
    int LAYERTYPE_TRACKS       = 1 << 6;
    int LAYERTYPE_LOOKUPTABLE  = 1 << 7;
    int LAYERTYPE_NGW_WEBMAP   = 1 << 8;
    int LAYERTYPE_NGW_TRACKS   = 1 << 9;

    int LAYERTYPE_SYSMAX = 10; // should be the max + 1 of system layer type

    /**
     * File type
     */
    int FILETYPE_PARENT  = 1 << 0;
    int FILETYPE_FOLDER  = 1 << 1;
    int FILETYPE_ZIP     = 1 << 2;
    int FILETYPE_GEOJSON = 1 << 3;
    int FILETYPE_FB      = 1 << 4;
    int FILETYPE_UNKNOWN = 1 << 31;

    /**
     * time constants
     */
    long     ONE_SECOND           = 1000;
    long     ONE_MINUTE           = ONE_SECOND * 60;
    long     ONE_HOUR             = ONE_MINUTE * 60;
    long     ONE_DAY              = ONE_HOUR * 24;
    long     ONE_WEEK             = ONE_DAY * 7;
    long     DEFAULT_TILE_MAX_AGE = ONE_WEEK;
    long     ONE_YEAR             = ONE_DAY * 365;
    int      KEEP_ALIVE_TIME      = 35;
    int      TERMINATE_TIME       = 350;
    TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.MILLISECONDS;

    int  SYNC_NONE           = 1 << 0;
    int  SYNC_GEOMETRY       = 1 << 1;
    int  SYNC_ATTRIBUTES     = 1 << 2;
    int  SYNC_DATA           = SYNC_GEOMETRY | SYNC_ATTRIBUTES;
    int  SYNC_ATTACH         = 1 << 3;
    int  SYNC_ALL            = SYNC_DATA | SYNC_ATTACH;
    long DEFAULT_SYNC_PERIOD = 3600; //1 hour

    String URI_ATTACH  = "attach";
    String URI_CHANGES = "changes";

    // http://stackoverflow.com/a/24055457
    String URI_PARAMETER_LIMIT    = "limit";
    String URI_PARAMETER_TEMP     = "temp";
    String URI_PARAMETER_NOT_SYNC = "not_sync";

    String CHANGES_NAME_POSTFIX      = "_changes";
    int    CHANGE_OPERATION_TEMP     = 1;
    int    CHANGE_OPERATION_NEW      = 1 << 1; // 2
    int    CHANGE_OPERATION_CHANGED  = 1 << 2; // 4
    int    CHANGE_OPERATION_DELETE   = 1 << 3; // 8
    int    CHANGE_OPERATION_ATTACH   = 1 << 4; // 16
    int    CHANGE_OPERATION_NOT_SYNC = 1 << 5; // 32

    int DRAWING_SEPARATE_THREADS = 9;
    int DRAW_NOTIFY_STEP_PERCENT = 20; // 5%

    String[] VECTOR_FORBIDDEN_FIELDS = {
            "ABORT",
            "ACTION",
            "ADD",
            "AFTER",
            "ALL",
            "ALTER",
            "ANALYZE",
            "AND",
            "AS",
            "ASC",
            "ATTACH",
            "AUTOINCREMENT",
            "BEFORE",
            "BEGIN",
            "BETWEEN",
            "BY",
            "CASCADE",
            "CASE",
            "CAST",
            "CHECK",
            "COLLATE",
            "COLUMN",
            "COMMIT",
            "CONFLICT",
            "CONSTRAINT",
            "CREATE",
            "CROSS",
            "CURRENT_DATE",
            "CURRENT_TIME",
            "CURRENT_TIMESTAMP",
            "DATABASE",
            "DEFAULT",
            "DEFERRABLE",
            "DEFERRED",
            "DELETE",
            "DESC",
            "DETACH",
            "DISTINCT",
            "DROP",
            "EACH",
            "ELSE",
            "END",
            "ESCAPE",
            "EXCEPT",
            "EXCLUSIVE",
            "EXISTS",
            "EXPLAIN",
            "FAIL",
            "FOR",
            "FOREIGN",
            "FROM",
            "FULL",
            "GLOB",
            "GROUP",
            "HAVING",
            "IF",
            "IGNORE",
            "IMMEDIATE",
            "IN",
            "INDEX",
            "INDEXED",
            "INITIALLY",
            "INNER",
            "INSERT",
            "INSTEAD",
            "INTERSECT",
            "INTO",
            "IS",
            "ISNULL",
            "JOIN",
            "KEY",
            "LEFT",
            "LIKE",
            "LIMIT",
            "MATCH",
            "NATURAL",
            "NO",
            "NOT",
            "NOTNULL",
            "NULL",
            "OF",
            "OFFSET",
            "ON",
            "OR",
            "ORDER",
            "OUTER",
            "PLAN",
            "PRAGMA",
            "PRIMARY",
            "QUERY",
            "RAISE",
            "RECURSIVE",
            "REFERENCES",
            "REGEXP",
            "REINDEX",
            "RELEASE",
            "RENAME",
            "REPLACE",
            "RESTRICT",
            "RIGHT",
            "ROLLBACK",
            "ROW",
            "SAVEPOINT",
            "SELECT",
            "SET",
            "TABLE",
            "TEMP",
            "TEMPORARY",
            "THEN",
            "TO",
            "TRANSACTION",
            "TRIGGER",
            "UNION",
            "UNIQUE",
            "UPDATE",
            "USING",
            "VACUUM",
            "VALUES",
            "VIEW",
            "VIRTUAL",
            "WHEN",
            "WHERE",
            "WITH",
            "WITHOUT"};

}
