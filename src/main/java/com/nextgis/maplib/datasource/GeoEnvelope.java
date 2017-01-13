/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

import com.nextgis.maplib.api.IJSONStore;

import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.Constants.JSON_BBOX_MAXX_KEY;
import static com.nextgis.maplib.util.Constants.JSON_BBOX_MAXY_KEY;
import static com.nextgis.maplib.util.Constants.JSON_BBOX_MINX_KEY;
import static com.nextgis.maplib.util.Constants.JSON_BBOX_MINY_KEY;


public class GeoEnvelope
        implements IJSONStore
{
    protected Double mMinX;
    protected Double mMaxX;
    protected Double mMinY;
    protected Double mMaxY;

    public final static int enumGISPtPosLeft = 1;
    public final static int enumGISPtPosRight = 2;
    public final static int enumGISPtPosBottom = 3;
    public final static int enumGISPtPosTop = 4;

    public GeoEnvelope()
    {
        unInit();
    }


    public void unInit()
    {
        mMinX = null;
        mMaxX = null;
        mMinY = null;
        mMaxY = null;
    }


    public GeoEnvelope(
            double minX,
            double maxX,
            double minY,
            double maxY)
    {
        mMinX = minX;
        mMaxX = maxX;
        mMinY = minY;
        mMaxY = maxY;
    }


    public GeoEnvelope(final GeoEnvelope env)
    {
        mMinX = env.mMinX;
        mMaxX = env.mMaxX;
        mMinY = env.mMinY;
        mMaxY = env.mMaxY;
    }


    public void setMin(
            double x,
            double y)
    {
        mMinX = x;
        mMinY = y;
    }


    public void setMax(
            double x,
            double y)
    {
        mMaxX = x;
        mMaxY = y;
    }


    public final GeoPoint getCenter()
    {
        double x = mMinX + width() / 2.0;
        double y = mMinY + height() / 2.0;
        return new GeoPoint(x, y);
    }

    public final double getArea(){
        return width() * height();
    }


    public final double width()
    {
        return mMaxX - mMinX;
    }


    public final double height()
    {
        return mMaxY - mMinY;
    }


    public void adjust(double ratio)
    {
        double w = width() / 2.0;
        double h = height() / 2.0;
        double centerX = mMinX + w;
        double centerY = mMinY + h;

        double envRatio = w / h;

        if (envRatio == ratio) {
            return;
        }

        if (ratio > envRatio) //increase width
        {
            w = h * ratio;
            mMaxX = centerX + w;
            mMinX = centerX - w;
        } else                //increase height
        {
            h = w / ratio;
            mMaxY = centerY + h;
            mMinY = centerY - h;
        }
    }


    public void merge(final GeoEnvelope other)
    {
        if (isInit()) {
            mMinX = Math.min(mMinX, other.mMinX);
            mMaxX = Math.max(mMaxX, other.mMaxX);
            mMinY = Math.min(mMinY, other.mMinY);
            mMaxY = Math.max(mMaxY, other.mMaxY);
        } else {
            mMinX = other.mMinX;
            mMaxX = other.mMaxX;
            mMinY = other.mMinY;
            mMaxY = other.mMaxY;
        }
    }


    public final boolean isInit()
    {
        return mMinX != null && mMinY != null && mMaxX != null && mMaxY != null;
    }


    public void merge(
            double dfX,
            double dfY)
    {
        if (isInit()) {
            mMinX = Math.min(mMinX, dfX);
            mMaxX = Math.max(mMaxX, dfX);
            mMinY = Math.min(mMinY, dfY);
            mMaxY = Math.max(mMaxY, dfY);
        } else {
            mMinX = mMaxX = dfX;
            mMinY = mMaxY = dfY;
        }
    }


    public void intersect(final GeoEnvelope other)
    {
        if (intersects(other)) {
            if (isInit()) {
                mMinX = Math.max(mMinX, other.mMinX);
                mMaxX = Math.min(mMaxX, other.mMaxX);
                mMinY = Math.max(mMinY, other.mMinY);
                mMaxY = Math.min(mMaxY, other.mMaxY);
            } else {
                mMinX = other.mMinX;
                mMaxX = other.mMaxX;
                mMinY = other.mMinY;
                mMaxY = other.mMaxY;
            }
        } else {
            unInit();
        }
    }


    public final boolean intersects(final GeoEnvelope other)
    {
        return mMinX <= other.mMaxX && mMaxX >= other.mMinX &&
               mMinY <= other.mMaxY && mMaxY >= other.mMinY;
    }


    public final boolean contains(final GeoEnvelope other)
    {
        return mMinX <= other.mMinX && mMinY <= other.mMinY &&
               mMaxX >= other.mMaxX && mMaxY >= other.mMaxY;
    }


    public final boolean contains(final GeoPoint pt)
    {
        return mMinX <= pt.getX() && mMinY <= pt.getY() && mMaxX >= pt.getX() && mMaxY >= pt.getY();
    }


    public void offset(
            double x,
            double y)
    {
        mMinX += x;
        mMaxX += x;
        mMinY += y;
        mMaxY += y;
    }


    public void scale(double scale)
    {
        mMaxX = mMinX + width() * scale;
        mMaxY = mMinY + height() * scale;
    }


    public void fix()
    {
        if (mMinX > mMaxX) {
            double tmp = mMinX;
            mMinX = mMaxX;
            mMaxX = tmp;
        }

        if (mMinY > mMaxY) {
            double tmp = mMinY;
            mMinY = mMaxY;
            mMaxY = tmp;
        }
    }


    public String toString()
    {
        return "MinX: " + mMinX + ", MinY: " + mMinY + ", MaxX: " + mMaxX + ", MaxY: " + mMaxY;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject oJSONBBox = new JSONObject();
        oJSONBBox.put(JSON_BBOX_MINX_KEY, getMinX());
        oJSONBBox.put(JSON_BBOX_MINY_KEY, getMinY());
        oJSONBBox.put(JSON_BBOX_MAXX_KEY, getMaxX());
        oJSONBBox.put(JSON_BBOX_MAXY_KEY, getMaxY());
        return oJSONBBox;
    }


    public final double getMinX()
    {
        return mMinX;
    }


    public void setMinX(double x)
    {
        mMinX = x;
    }


    public final double getMinY()
    {
        return mMinY;
    }


    public void setMinY(double y)
    {
        mMinY = y;
    }


    public final double getMaxX()
    {
        return mMaxX;
    }


    public void setMaxX(double x)
    {
        mMaxX = x;
    }


    public final double getMaxY()
    {
        return mMaxY;
    }


    public void setMaxY(double y)
    {
        mMaxY = y;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        setMinX(jsonObject.getDouble(JSON_BBOX_MINX_KEY));
        setMinY(jsonObject.getDouble(JSON_BBOX_MINY_KEY));
        setMaxX(jsonObject.getDouble(JSON_BBOX_MAXX_KEY));
        setMaxY(jsonObject.getDouble(JSON_BBOX_MAXY_KEY));
    }

    public void set(GeoEnvelope env) {
        if (env == null)
            return;

        mMinX = env.mMinX;
        mMaxX = env.mMaxX;
        mMinY = env.mMinY;
        mMaxY = env.mMaxY;
    }


    /**
     * Sutherland-Hodgman Polygon Clipping
     * @param pt Test point
     * @param nPos Test type
     * @return true if point inside envelope or false
     */
    public boolean isInside(final GeoPoint pt, int nPos) {
        switch(nPos)
        {
            case enumGISPtPosLeft://XMin
                return (pt.getX() > mMinX);
            case enumGISPtPosRight://XMax
                return (pt.getX() < mMaxX);
            case enumGISPtPosBottom://YMin
                return (pt.getY() < mMaxY);
            case enumGISPtPosTop://YMax
                return (pt.getY() > mMinY);
        }
        return false;
    }
}
