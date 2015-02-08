/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Authors:  Stanislav Petriakov
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.display;

import android.database.Cursor;
import android.graphics.Paint;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.util.GeoConstants;
import org.json.JSONException;
import org.json.JSONObject;


public class TrackRenderer
        extends Renderer
{
    private Paint mPaint;


    public TrackRenderer(ILayer layer)
    {
        super(layer);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        return null;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {

    }


    @Override
    public void runDraw(GISDisplay display)
    {
        TrackLayer layer = (TrackLayer) mLayer;
        layer.loadTracks();

        mPaint.setColor(layer.getColor());
        mPaint.setStrokeWidth((float) (4 / display.getScale()));

        int size = layer.getTracksCount();

        for (int i = 0; i < size; i++) {
            Cursor track = layer.getTrack(i);

            if (track == null || !track.moveToFirst()) {
                continue;
            }

            float x0 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LON)),
                    y0 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LAT)), x1, y1;

            GeoPoint point0 = new GeoPoint(x0, y0);
            point0.setCRS(GeoConstants.CRS_WGS84);
            point0.project(GeoConstants.CRS_WEB_MERCATOR);
            x0 = (float) point0.getX();
            y0 = (float) point0.getY();

            while (track.moveToNext()) {
                x1 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LON));
                y1 = track.getFloat(track.getColumnIndex(TrackLayer.FIELD_LAT));
                point0.setCoordinates(x1, y1);
                point0.setCRS(GeoConstants.CRS_WGS84);
                point0.project(GeoConstants.CRS_WEB_MERCATOR);

                display.drawLine(x0, y0, (float) point0.getX(), (float) point0.getY(), mPaint);
                x0 = (float) point0.getX();
                y0 = (float) point0.getY();
            }
        }

        layer.onDrawFinished(layer.getId(), 1);
    }


    @Override
    public void cancelDraw()
    {

    }
}