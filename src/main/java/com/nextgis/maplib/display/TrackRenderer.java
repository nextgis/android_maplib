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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.TrackLayer;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;


public class TrackRenderer
        extends Renderer
{
    private Paint mPaint;
    private Bitmap mEndingMarker;

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


    public void setEndingMarker(int drawableResId)
    {
        mEndingMarker =
                BitmapFactory.decodeResource(getLayer().getContext().getResources(), drawableResId);
    }


    @Override
    public void runDraw(GISDisplay display)
    {
        final TrackLayer layer = (TrackLayer) getLayer();

        mPaint.setStrokeWidth((float) Math.ceil(4 / display.getScale()));

        Map<Integer, GeoLineString> trackLines = layer.getTracks();
        int trackLinesSize = trackLines.size();
        if (trackLinesSize < 1) {
            return;
        }

        int i = 0;
        int nStep = trackLinesSize / 10;
        if(nStep == 0)
            nStep = 1;

        for (Map.Entry<Integer, GeoLineString> entry : trackLines.entrySet()) {
            i++;
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            mPaint.setColor(layer.getColor(entry.getKey()));
            List<GeoPoint> points = entry.getValue().getPoints();
            for (int k = 1; k < points.size(); k++) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                display.drawLine(
                        (float) points.get(k - 1).getX(), (float) points.get(k - 1).getY(),
                        (float) points.get(k).getX(), (float) points.get(k).getY(), mPaint);
            }

            // draw start and finish flag
            if (mEndingMarker != null) {
                GeoPoint endings = points.get(0);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                ColorFilter filter = new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP);
                paint.setColorFilter(filter);
                Bitmap ending = mEndingMarker.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(ending);
                canvas.drawBitmap(ending, 0, 0, paint);
                display.drawBitmap(ending, endings, 0, ending.getHeight());

                endings = points.get(points.size() - 1);
                filter = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
                paint.setColorFilter(filter);
                canvas = new Canvas(ending);
                canvas.drawBitmap(ending, 0, 0, paint);
                display.drawBitmap(ending, endings, 0, ending.getHeight());
            }

            float percent = (float) i / trackLinesSize;
            if(i % nStep == 0) //0..10..20..30..40..50..60..70..80..90..100
                layer.onDrawFinished(layer.getId(), percent);
        }
    }


    @Override
    public void cancelDraw()
    {

    }
}