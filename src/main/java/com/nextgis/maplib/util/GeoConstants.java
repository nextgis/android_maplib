/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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

public interface GeoConstants
{

    /**
     * Mercator projection constants
     */
    double MERCATOR_MAX = 20037508.34;
    double WGS_LONG_MAX = 180;
    double WGS_LAT_MAX  = 90;

    /**
     * TMS type
     */
    int TMSTYPE_NORMAL = 1;
    int TMSTYPE_OSM    = 2;

    int DEFAULT_MAX_ZOOM = 25;
    int DEFAULT_CACHE_MAX_ZOOM = 18;
    int DEFAULT_MIN_ZOOM = 0;

    /**
     * geometry type
     */
    int GTPoint              = 1;
    int GTLineString         = 2;
    int GTPolygon            = 3;
    int GTMultiPoint         = 4;
    int GTMultiLineString    = 5;
    int GTMultiPolygon       = 6;
    int GTGeometryCollection = 7;
    int GTNone               = 100;
    int GTLinearRing         = 200;

    int GTPointCheck              = 1 << GTPoint;
    int GTLineStringCheck         = 1 << GTLineString;
    int GTPolygonCheck            = 1 << GTPolygon;
    int GTMultiPointCheck         = 1 << GTMultiPoint;
    int GTMultiLineStringCheck    = 1 << GTMultiLineString;
    int GTMultiPolygonCheck       = 1 << GTMultiPolygon;
    int GTGeometryCollectionCheck = 1 << GTGeometryCollection;
    int GTNoneCheck               = 1 << 10;
    int GTAnyCheck                =
            GTMultiPointCheck | GTPointCheck | GTLineStringCheck | GTMultiLineStringCheck |
                    GTPolygonCheck | GTMultiPolygonCheck | GTGeometryCollectionCheck;

    /**
     * geojson see http://geojson.org/geojson-spec.html
     */
    String GEOJSON_CRS_EPSG_4326           = "EPSG:4326";
    String GEOJSON_CRS_WGS84               = "urn:ogc:def:crs:OGC:1.3:CRS84";
    String GEOJSON_CRS_WEB_MERCATOR        = "EPSG:3857";
    String GEOJSON_CRS_EPSG_3857           = "urn:ogc:def:crs:EPSG::3857";
    String GEOJSON_TYPE                    = "type";
    String GEOJSON_ID                      = "id";
    String GEOJSON_FEATURE_ID              = "FEATURE_ID";
    String GEOJSON_CRS                     = "crs";
    String GEOJSON_NAME                    = "name";
    String GEOJSON_PROPERTIES              = "properties";
    String GEOJSON_ATTACHES                = "attaches";
    String GEOJSON_BBOX                    = "bbox";
    String GEOJSON_TYPE_FEATURES           = "features";
    String GEOJSON_GEOMETRY                = "geometry";
    String GEOJSON_GEOMETRIES              = "geometries";
    String GEOJSON_COORDINATES             = "coordinates";
    String GEOJSON_TYPE_Point              = "Point";
    String GEOJSON_TYPE_MultiPoint         = "MultiPoint";
    String GEOJSON_TYPE_LineString         = "LineString";
    String GEOJSON_TYPE_MultiLineString    = "MultiLineString";
    String GEOJSON_TYPE_Polygon            = "Polygon";
    String GEOJSON_TYPE_MultiPolygon       = "MultiPolygon";
    String GEOJSON_TYPE_GeometryCollection = "GeometryCollection";
    String GEOJSON_TYPE_Feature            = "Feature";
    String GEOJSON_TYPE_FeatureCollection  = "FeatureCollection";

    /**
     * field type
     */
    int FTInteger     = 0;
    int FTIntegerList = 1;
    int FTReal        = 2;
    int FTRealList    = 3;
    int FTString      = 4;
    int FTStringList  = 5;
    int FTBinary      = 8;
    int FTDateTime    = 10;
    int FTDate        = 11;
    int FTTime        = 12;

    /**
     * CRS
     */
    int CRS_WGS84        = 4326;
    int CRS_WEB_MERCATOR = 3857;

}
