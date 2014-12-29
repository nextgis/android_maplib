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

public class GeoMultiLineString extends GeoGeometryCollection {

    @Override
    public void add(GeoGeometry geometry) throws ClassCastException {
        if (!(geometry instanceof GeoLineString)) {
            throw new ClassCastException("GeoMultiLineString: geometry is not GeoLineString type.");
        }

        super.add(geometry);
    }

    public void add(GeoLineString lineString) {
        super.add(lineString);
    }

    @Override
    public GeoLineString get(int index) {
        return (GeoLineString) mGeometries.get(index);
    }

    @Override
    public int getType() {
        return GTMultiLineString;
    }

    @Override
    public void setCoordinatesFromJSON(JSONArray coordinates) throws JSONException {
        for (int i = 0; i < coordinates.length(); ++i) {
            GeoLineString lineString = new GeoLineString();
            lineString.setCoordinatesFromJSON(coordinates.getJSONArray(i));
            add(lineString);
        }
    }
}
