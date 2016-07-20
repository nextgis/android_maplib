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
package com.nextgis.maplib.datasource;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.JsonReader;

import com.nextgis.maplib.util.GeoConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static com.nextgis.maplib.util.GeoConstants.GEOJSON_GEOMETRIES;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_GeometryCollection;
import static com.nextgis.maplib.util.GeoConstants.GTGeometryCollection;


public class GeoGeometryCollection
        extends GeoGeometry
{
    protected static final long serialVersionUID = -1241179697270831768L;
    protected List<GeoGeometry> mGeometries;


    public GeoGeometryCollection()
    {
        mGeometries = new LinkedList<>();
    }


    public GeoGeometryCollection(GeoGeometryCollection collection)
    {
        mGeometries = new LinkedList<>();
        for (GeoGeometry geometry : collection.mGeometries) {
            mGeometries.add(geometry.copy());
        }
    }


    public void add(GeoGeometry geometry)
            throws IllegalArgumentException
    {
        if (geometry == null) {
            throw new IllegalArgumentException("GeoGeometryCollection: geometry == null.");
        }

        mGeometries.add(geometry);
    }

    public void set(int index, GeoGeometry geometry)
            throws IllegalArgumentException
    {
        if (geometry == null) {
            throw new IllegalArgumentException("GeoGeometryCollection: geometry == null.");
        }
        if(index >= mGeometries.size())
            mGeometries.add(geometry);
        else
            mGeometries.set(index, geometry);
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
        return GeoConstants.GTGeometryCollection;
    }


    @Override
    protected boolean rawProject(int toCrs)
    {
        boolean isOk = true;
        for (GeoGeometry geometry : mGeometries) {
            isOk = isOk && geometry.rawProject(toCrs);
        }
        if (isOk) {
            super.rawProject(toCrs);
        }
        return isOk;
    }


    @Override
    public GeoEnvelope getEnvelope() {
        GeoEnvelope envelope = new GeoEnvelope();

        for (GeoGeometry geometry : mGeometries) {
            envelope.merge(geometry.getEnvelope());
        }

        return envelope;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException {
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
        for (int i = 0; i < coordinates.length(); ++i) {
            JSONObject jsonGeometry = coordinates.getJSONObject(i);
            GeoGeometry geometry = GeoGeometryFactory.fromJson(jsonGeometry);
            add(geometry);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void setCoordinatesFromJSONStream(JsonReader reader, int crs) throws IOException {
        setCRS(crs);
        reader.beginArray();
        while (reader.hasNext()){
            GeoGeometry geometry = GeoGeometryFactory.fromJsonStream(reader, crs);
            if(null != geometry)
                mGeometries.add(geometry);
        }
        reader.endArray();
    }


    @Override
    public void setCoordinatesFromWKT(String wkt, int crs)
    {
        //TODO: implement this
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


    @Override
    public String toWKT(boolean full)
    {
        StringBuilder buf = new StringBuilder();
        buf.append("GEOMETRYCOLLECTION ");
        if (mGeometries.size() == 0) {
            buf.append(" EMPTY");
        } else {
            buf.append("(");
            for (int i = 0; i < mGeometries.size(); i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                GeoGeometry geometry = mGeometries.get(i);
                buf.append(geometry.toWKT(false));
            }
            buf.append(")");
        }
        return buf.toString();
    }


    @Override
    public boolean equals(Object o)
    {
        if (!super.equals(o)) {
            return false;
        }
        GeoGeometryCollection otherColl = (GeoGeometryCollection) o;
        for (int i = 0; i < mGeometries.size(); i++) {
            GeoGeometry geom = mGeometries.get(i);
            GeoGeometry otherGeom = otherColl.getGeometry(i);
            if (!geom.equals(otherGeom)) {
                return false;
            }
        }
        return true;
    }


    public GeoGeometry getGeometry(int index)
    {
        if (mGeometries.size() > index) {
            return mGeometries.get(index);
        }
        return null;
    }


    @Override
    public boolean intersects(GeoEnvelope envelope)
    {
        for (GeoGeometry geom : mGeometries) {
            if (geom.intersects(envelope)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public GeoGeometry copy()
    {
        return new GeoGeometryCollection(this);
    }


    @Override
    public void clear()
    {
        mGeometries.clear();
    }

    @Override
    public GeoGeometry simplify(double tolerance) {
        GeoGeometryCollection collection = getInstance();
        for (GeoGeometry geom : mGeometries) {
            GeoGeometry newGeometry = geom.simplify(tolerance);
            if(null != newGeometry)
                collection.add(newGeometry);
        }

        if(collection.size() < 1)
            return null;
        return collection;
    }

    @Override
    public GeoGeometry clip(GeoEnvelope envelope) {
        GeoGeometryCollection collection = getInstance();
        collection.setCRS(mCRS);
        for (GeoGeometry geom : mGeometries) {
            GeoGeometry newGeometry = geom.clip(envelope);
            if(null != newGeometry)
                collection.add(newGeometry);
        }

        if(collection.size() < 1)
            return null;
        return collection;
    }

    @Override
    public void write(DataOutputStream stream) throws IOException {
        super.write(stream);
        int collectionSize = mGeometries.size();
        stream.writeInt(collectionSize);
        for(int i = 0; i < collectionSize; i++){
            GeoGeometry geometry = mGeometries.get(i);
            geometry.write(stream);
        }
    }

    @Override
    public void read(DataInputStream stream) throws IOException {
        super.read(stream);
        int collectionSize = stream.readInt();
        for (int i = 0; i < collectionSize; i++) {
            GeoGeometry geometry = GeoGeometryFactory.fromDataStream(stream);
            if(null != geometry)
                mGeometries.add(geometry);
        }
    }

    @Override
    public boolean isValid() {
        for (GeoGeometry geometry : mGeometries)
            if (!geometry.isValid())
                return false;

        return !mGeometries.isEmpty();
    }

    @Override
    public double distance(GeoGeometry geometry) {
        if(mGeometries.isEmpty())
            return 0;
        double distance = 0;
        for(GeoGeometry collectionGeometry : mGeometries){
            double currentDist = collectionGeometry.distance(geometry);
            if(distance == 0)
                distance = currentDist;
            else if(distance > currentDist)
                distance = currentDist;
        }
        return distance;
    }

    protected GeoGeometryCollection getInstance(){
        return new GeoGeometryCollection();
    }
}
