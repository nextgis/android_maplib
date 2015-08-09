/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplib.display;

import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.TrackLayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.nextgis.maplib.util.Constants.DEFAULT_EXECUTION_DELAY;


public class TrackRenderer
        extends Renderer
{
    private Paint mPaint;


    public TrackRenderer(ILayer layer)
    {
        super(layer);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
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
        final TrackLayer layer = (TrackLayer) mLayer;

        mPaint.setColor(layer.getColor());
        mPaint.setStrokeWidth((float) Math.ceil(4 / display.getScale()));

        List<GeoLineString> trackLines = layer.getTracks();
        if (trackLines.size() < 1) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    layer.onDrawFinished(layer.getId(), 1.0f);
                }
            }, DEFAULT_EXECUTION_DELAY);
            return;
        }

        int linesCompleteCount = 0;
        for (GeoLineString trackLine : trackLines) {
            List<GeoPoint> points = trackLine.getPoints();

            for (int i = 1; i < points.size(); i++) {
                display.drawLine(
                        (float) points.get(i - 1).getX(), (float) points.get(i - 1).getY(),
                        (float) points.get(i).getX(), (float) points.get(i).getY(), mPaint);
            }

            linesCompleteCount++;
            synchronized (mLayer) {
                layer.onDrawFinished(
                        layer.getId(), (float) (linesCompleteCount) / trackLines.size());
            }
        }
    }


    @Override
    public void cancelDraw()
    {

    }
}