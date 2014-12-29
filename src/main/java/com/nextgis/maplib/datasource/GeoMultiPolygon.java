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

import static com.nextgis.maplib.util.GeoConstants.*;

public class GeoMultiPolygon extends GeoGeometryCollection {

    @Override
    public void add(GeoGeometry geometry) throws ClassCastException {
        if (!(geometry instanceof GeoPolygon)) {
            throw new ClassCastException("GeoMultiPolygon: geometry is not GeoPolygon type.");
        }

        super.add(geometry);
    }

    public void add(GeoPolygon lineString) {
        super.add(lineString);
    }

    @Override
    public GeoPolygon get(int index) {
        return (GeoPolygon) mGeometries.get(index);
    }

    @Override
    public int getType() {
        return GTMultiPolygon;
    }

    @Override
    public void setCoordinatesFromJSON(JSONArray coordinates) throws JSONException {
        for (int i = 0; i < coordinates.length(); ++i) {
            GeoPolygon polygon = new GeoPolygon();
            polygon.setCoordinatesFromJSON(coordinates.getJSONArray(i));
            add(polygon);
        }
    }
}
