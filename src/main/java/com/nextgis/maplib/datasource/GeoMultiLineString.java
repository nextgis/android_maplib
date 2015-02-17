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

import com.nextgis.maplib.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;

import static com.nextgis.maplib.util.GeoConstants.GTMultiLineString;


public class GeoMultiLineString
        extends GeoGeometryCollection
{
    protected static final long serialVersionUID =-1241179697270831766L;
    @Override
    public void add(GeoGeometry geometry)
            throws ClassCastException
    {
        if (!(geometry instanceof GeoLineString)) {
            throw new ClassCastException("GeoMultiLineString: geometry is not GeoLineString type.");
        }

        super.add(geometry);
    }


    @Override
    public GeoLineString get(int index)
    {
        return (GeoLineString) mGeometries.get(index);
    }


    @Override
    public int getType()
    {
        return GTMultiLineString;
    }


    @Override
    public void setCoordinatesFromJSON(JSONArray coordinates)
            throws JSONException
    {
        for (int i = 0; i < coordinates.length(); ++i) {
            GeoLineString lineString = new GeoLineString();
            lineString.setCoordinatesFromJSON(coordinates.getJSONArray(i));
            add(lineString);
        }
    }


    @Override
    public void setCoordinatesFromWKT(String wkt)
    {
        if(wkt.contains("EMPTY"))
            return;

        if(wkt.startsWith("("))
            wkt = wkt.substring(1, wkt.length() - 1);

        int pos = wkt.indexOf("(");
        while(pos != Constants.NOT_FOUND) {
            wkt = wkt.substring(pos + 1, wkt.length());
            pos = wkt.indexOf(")") - 1;
            if(pos < 1)
                return;

            GeoLineString lineString = new GeoLineString();
            lineString.setCoordinatesFromWKT(wkt.substring(0, pos).trim());
            add(lineString);

            pos = wkt.indexOf("(");
        }
    }


    public void add(GeoLineString lineString)
    {
        super.add(lineString);
    }


    @Override
    public String toWKT(boolean full)
    {
        StringBuilder buf = new StringBuilder();
        if(full)
            buf.append("MULTILINESTRING ");
        if (mGeometries.size() == 0)
            buf.append(" EMPTY");
        else {
            buf.append("(");
            for (int i = 0; i < mGeometries.size(); i++) {
                if(i > 0)
                    buf.append(", ");
                GeoGeometry geom = mGeometries.get(i);
                buf.append(geom.toWKT(false));
            }
            buf.append(")");
        }
        return buf.toString();
    }
}
