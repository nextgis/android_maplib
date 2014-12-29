/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.maplib.util;

import java.util.concurrent.TimeUnit;

public interface Constants {
    /**
     * HTTP parameters
     */
    public static final String APP_USER_AGENT = "NextGIS Mobile";
    public static final int TIMEOUT_CONNECTION = 1500;
    public static final int TIMEOUT_SOKET = 3000;
    public static final int IO_BUFFER_SIZE = 1024;
    /**
     * Preference key - not UI
     */
    public static final String PREFS_TILE_SOURCE = "map_tile_source";
    public static final String PREFS_SCROLL_X = "map_scroll_x";
    public static final String PREFS_SCROLL_Y = "map_scroll_y";
    public static final String PREFS_ZOOM_LEVEL = "map_zoom_level";
    public static final String PREFS_SHOW_LOCATION = "map_show_loc";
    public static final String PREFS_SHOW_COMPASS = "map_show_compass";
    public static final String PREFS_SHOW_INFO = "map_show_info";
    public static final String PREFS_WAR_SHOW_INFO = "map_war_show_info";
    public static final String PREFS_IS_FRESH_INSTALL = "map_is_fresh_install";
    public static final String PREFS_MAP = "map";

    /**
     * Preference keys - in UI
     */
    public static final String KEY_PREF_STORAGE_SITE = "storage_site";
    public static final String KEY_PREF_USER_ID = "user_id";
    public static final String KEY_PREF_MIN_DIST_CHNG_UPD = "min_dist_change_for_update";
    public static final String KEY_PREF_MIN_TIME_UPD = "min_time_beetwen_updates";
    public static final String KEY_PREF_SW_TRACK_SRV = "sw_track_service";
    public static final String KEY_PREF_SW_TRACKGPX_SRV = "sw_trackgpx_service";
    public static final String KEY_PREF_SHOW_LAYES_LIST = "show_layers_list";
    public static final String KEY_PREF_SW_SENDPOS_SRV = "sw_sendpos_service";
    public static final String KEY_PREF_SW_ENERGY_ECO = "sw_energy_economy";
    public static final String KEY_PREF_TIME_DATASEND = "time_between_datasend";
    public static final String KEY_PREF_COORD_FORMAT = "coordinates_format";
    public static final String KEY_PREF_START_SVC_ON_STARTUP = "start_services_on_startup";
    public static final String KEY_PREF_ACCURATE_LOC = "accurate_coordinates_pick";
    public static final String KEY_PREF_ACCURATE_GPSCOUNT = "accurate_coordinates_pick_count";
    public static final String KEY_PREF_ACCURATE_CE = "accurate_type";
    public static final String KEY_PREF_TILE_SIZE = "map_tile_size";
    public static final String KEY_PREF_COMPASS_VIBRO = "compass_vibration";
    public static final String KEY_PREF_COMPASS_TRUE_NORTH = "compass_true_north";
    public static final String KEY_PREF_COMPASS_SHOW_MAGNET = "compass_show_magnetic";
    public static final String KEY_PREF_COMPASS_WAKE_LOCK = "compass_wake_lock";
    public static final String KEY_PREF_MAP_PATH = "map_path";
    public static final String KEY_PREF_MAP_NAME = "map_name";
    public static final String KEY_PREF_KEEPSCREENON = "keep_screen_on";

    public static final String SERVICE_PREF = "preferences";

    public final static String LOCATION_HINT = "ngm.location";
    public final static String TAG = "nextgismobile";

    public static final int MAX_DRAWLER_WIDTH = 640;
    public static final float MIN_SCROLL_STEP = 5.5f;

    public static final int MAP_LIMITS_NO = 1; // no limits to scroll map
    public static final int MAP_LIMITS_X = 2;  // limit to scroll map by x axis
    public static final int MAP_LIMITS_Y = 3;  // limit to scroll map by y axis
    public static final int MAP_LIMITS_XY = 4; // limit to scroll map by x & y axis

    //the additional size to off screen drawing
    //from 1 and higher
    //As more than more memory needed
    public static final float OFFSCREEN_EXTRASIZE_RATIO = 1.5f;
    //display redraw timeout ms
    public static final int DISPLAY_REDRAW_TIMEOUT = 550;


    public static final String MAP_CONFIG = "map.json";
    public static final String LAYER_CONFIG = "layer.json";
    public static final String LAYER_PREFIX = "layer_";
    public static final String TILE_EXT = ".tile";
    public static final String DATA_GEOJSON = "data.geojson";
    public static final String NGW_CONNECTIONS_JSON = "ngw_connections.json";
    public static final String FORM_DATA_GEOJSON = "form_data.geojson";
    public static final String FORM_XML = "form.xml";
    public static final String CONNECTION_JSON = "connection.json";

    /**
     * message type
     */
    public final static int MSGTYPE_LAYER_ADDED = 300;
    public final static int MSGTYPE_EDIT_LAYER_ADDED = 301;
    public final static int MSGTYPE_DRAWING_DONE = 500;
    public final static int MSGTYPE_PANNING_DONE = 501;
    public final static int MSGTYPE_ZOOMING_DONE = 502;
    public final static int MSGTYPE_EDIT_DRAWING_DONE = 503;

    /**
     * Bundle keys
     */
    public static final String BUNDLE_MSG_KEY = "msg";
    public static final String BUNDLE_HASERROR_KEY = "has_error";
    public static final String BUNDLE_TYPE_KEY = "type";
    public static final String BUNDLE_PATH_KEY = "path";
    public static final String BUNDLE_DONE_KEY = "done";
    public static final String BUNDLE_DRAWSTATE_KEY = "draw_state";

    /**
     * JSON keys
     */
    public static final String JSON_NAME_KEY = "name";
    public static final String JSON_VISIBILITY_KEY = "visible";
    public static final String JSON_TMSTYPE_KEY = "tms_type";
    public static final String JSON_LEVELS_KEY = "levels";
    public static final String JSON_LEVEL_KEY = "level";
    public static final String JSON_TYPE_KEY = "type";
    public static final String JSON_MAXX_KEY = "maxx";
    public static final String JSON_MINX_KEY = "minx";
    public static final String JSON_MAXY_KEY = "maxy";
    public static final String JSON_MINY_KEY = "miny";
    public static final String JSON_MAXLEVEL_KEY = "max_level";
    public static final String JSON_MINLEVEL_KEY = "min_level";
    public static final String JSON_LAYERS_KEY = "layers";
    public static final String JSON_LAYER_KEY = "layer";
    public static final String JSON_PATH_KEY = "path";
    public static final String JSON_MAP_KEY = "map";
    public static final String JSON_URL_KEY = "url";
    public static final String JSON_CONNECTION_ID_KEY = "connection_id";
    public static final String JSON_LOGIN_KEY = "login";
    public static final String JSON_PASSWORD_KEY = "password";
    public static final String JSON_BBOX_KEY = "bbox";
    public static final String JSON_BBOX_MINX_KEY = "bbox_minx";
    public static final String JSON_BBOX_MINY_KEY = "bbox_miny";
    public static final String JSON_BBOX_MAXX_KEY = "bbox_maxx";
    public static final String JSON_BBOX_MAXY_KEY = "bbox_maxy";
    public static final String JSON_FIELDS_KEY = "fields";
    public static final String JSON_FIELD_NAME_KEY = "field_name";
    public static final String JSON_FIELD_ALIAS_KEY = "field_alias";
    public static final String JSON_FIELD_TYPE_KEY = "field_type";
    public static final String JSON_GEOMETRY_TYPE_KEY = "geometry_type";
    public static final String JSON_NGW_CONNECTIONS_KEY = "ngw_connections";

    public static final String JSON_RESOURCE_KEY = "resource";
    public static final String JSON_ID_KEY = "id";
    public static final String JSON_PARENT_KEY = "parent";
    public static final String JSON_CLS_KEY = "cls";
    public static final String JSON_DISPLAY_NAME_KEY = "display_name";

    public static final String JSON_RESOURCE_GROUP_VALUE = "resource_group";
    public static final String JSON_POSTGIS_LAYER_VALUE = "postgis_layer";
    public static final String JSON_WMSSERVER_SERVICE_VALUE = "wmsserver_service";
    public static final String JSON_BASELAYERS_VALUE = "baselayers";
    public static final String JSON_POSTGIS_CONNECTION_VALUE = "postgis_connection";
    public static final String JSON_WEBMAP_VALUE = "webmap";
    public static final String JSON_WFSSERVER_SERVICE_VALUE = "wfsserver_service";
    public static final String JSON_RASTER_LAYER_VALUE = "raster_layer";
    public static final String JSON_VECTOR_LAYER_VALUE = "vector_layer";
    public static final String JSON_VECTOR_STYLE_VALUE = "vector_style";
    public static final String JSON_RASTER_STYLE_VALUE = "raster_style";
    public static final String JSON_FILE_BUCKET_VALUE = "file_bucket";

    public static final String JSON_PARENT_RESOURCE_GROUP_VALUE = "parent_resource_group";
    public static final String JSON_PARENT_DISPLAY_NAME_VALUE = "..";
    public static final String JSON_EMPTY_DISPLAY_NAME_VALUE = "-----";

    /**
     * NGW JSON types
     */
    public static final int NGWTYPE_RESOURCE_GROUP = 1;
    public static final int NGWTYPE_POSTGIS_LAYER = 2;
    public static final int NGWTYPE_WMSSERVER_SERVICE = 3;
    public static final int NGWTYPE_BASELAYERS = 4;
    public static final int NGWTYPE_POSTGIS_CONNECTION = 5;
    public static final int NGWTYPE_WEBMAP = 6;
    public static final int NGWTYPE_WFSSERVER_SERVICE = 7;
    public static final int NGWTYPE_VECTOR_LAYER = 8;
    public static final int NGWTYPE_RASTER_LAYER = 9;
    public static final int NGWTYPE_VECTOR_STYLE = 10;
    public static final int NGWTYPE_RASTER_STYLE = 11;
    public static final int NGWTYPE_FILE_BUCKET = 12;

    public static final int NGWTYPE_PARENT_RESOURCE_GROUP = 999;
    public static final int NGWTYPE_UNKNOWN = 1000;

    public static final int FORM_TYPE_TEXT_LABEL = 0;
    public static final int FORM_TYPE_TEXT_EDIT = 1;



    /**
     * Layer type
     */
    public static final int LAYERTYPE_LOCAL_TMS = 1;
    public static final int LAYERTYPE_LOCAL_RASTER = 2;
    public static final int LAYERTYPE_LOCAL_GEOJSON = 3;
    public static final int LAYERTYPE_REMOTE_TMS = 4;
    public static final int LAYERTYPE_NGW = 5;
    public static final int LAYERTYPE_LOCAL_EDIT_GEOJSON = 6;
    public static final int LAYERTYPE_NDW_VECTOR = 7;
    public static final int LAYERTYPE_NDW_RASTER = 8;
    public static final int LAYERTYPE_LOCAL_NGFP = 9;


    /**
     * time constants
     */
    public static final long ONE_SECOND = 1000;
    public static final long ONE_MINUTE = ONE_SECOND * 60;
    public static final long ONE_HOUR = ONE_MINUTE * 60;
    public static final long ONE_DAY = ONE_HOUR * 24;
    public static final long ONE_WEEK = ONE_DAY * 7;
    public static final long ONE_YEAR = ONE_DAY * 365;
    public static final long DEFAULT_MAXIMUM_CACHED_FILE_AGE = ONE_WEEK;

    public static final int KEEP_ALIVE_TIME = 1;
    public static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    public static final int DRAW_SATE_none = 0;
    public static final int DRAW_SATE_drawing = 1;
    public static final int DRAW_SATE_drawing_noclearbk = 2;
    public static final int DRAW_SATE_panning = 3;
    public static final int DRAW_SATE_zooming = 4;
    public static final int DRAW_SATE_double_tap = 5;
    public static final int DRAW_SATE_edit_drawing = 6;

    /**
     * Service constants
     */
    public static final String DATASEND_ACTION_START = "ngm.sendpos.action.START";
    public static final String DATASEND_ACTION_STOP = "ngm.sendpos.action.STOP";

    /**
     *
     */
    public static final int NOT_FOUND = -1;
}
