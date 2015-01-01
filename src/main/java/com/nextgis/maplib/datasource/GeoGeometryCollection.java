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
package com.nextgis.maplib.datasource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.GeoConstants.*;


public class GeoGeometryCollection
        extends GeoGeometry
{

    List<GeoGeometry> mGeometries;


    public GeoGeometryCollection()
    {
        mGeometries = new ArrayList<GeoGeometry>();
    }


    public void add(GeoGeometry geometry)
            throws IllegalArgumentException
    {
        if (geometry == null) {
            throw new IllegalArgumentException("GeoGeometryCollection: geometry == null.");
        }

        mGeometries.add(geometry);
    }


    public GeoGeometry remove(int index)
    {
        return mGeometries.remove(index);
    }


    public GeoGeometry get(int index)
    {
        return mGeometries.get(index);
    }


    public int size()
    {
        return mGeometries.size();
    }


    @Override
    public int getType()
    {
        return GTGeometryCollection;
    }


    @Override
    protected boolean rawProject(int toCrs)
    {
        boolean isOk = true;
        for (GeoGeometry geometry : mGeometries) {
            isOk = isOk && geometry.rawProject(toCrs);
        }
        return isOk;
    }


    @Override
    public GeoEnvelope getEnvelope()
    {
        GeoEnvelope envelope = new GeoEnvelope();

        for (GeoGeometry geometry : mGeometries) {
            envelope.merge(geometry.getEnvelope());
        }

        return envelope;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        if (getType() != GTGeometryCollection) {
            return super.toJSON();
        } else {
            JSONObject jsonOutObject = new JSONObject();
            jsonOutObject.put(GEOJSON_TYPE, GEOJSON_TYPE_GeometryCollection);
            JSONArray geometries = new JSONArray();
            jsonOutObject.put(GEOJSON_GEOMETRIES, geometries);

            for (GeoGeometry geometry : mGeometries) {
                geometries.put(geometry.toJSON());
            }

            return jsonOutObject;
        }
    }


    @Override
    public void setCoordinatesFromJSON(JSONArray coordinates)
            throws JSONException
    {
        //if (getType() == GTGeometryCollection) return;
    }


    @Override
    public JSONArray coordinatesToJSON()
            throws JSONException, ClassCastException
    {
        JSONArray coordinates = new JSONArray();

        for (GeoGeometry geometry : mGeometries) {
            coordinates.put(geometry.coordinatesToJSON());
        }

        return coordinates;
    }
}
