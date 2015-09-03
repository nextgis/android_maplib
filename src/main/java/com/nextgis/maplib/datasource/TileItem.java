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

package com.nextgis.maplib.datasource;

import android.os.Parcel;
import android.os.Parcelable;

import com.nextgis.maplib.util.FileUtil;


public class TileItem
        implements Parcelable
{
    public static final Creator<TileItem> CREATOR = new Creator<TileItem>()
    {
        public TileItem createFromParcel(Parcel in)
        {
            return new TileItem(in);
        }


        public TileItem[] newArray(int size)
        {
            return new TileItem[size];
        }
    };

    protected int      mZoomLevel;
    protected int      mX;
    protected int      mY;
    protected GeoEnvelope mEnvelope;


    public TileItem(
            int x,
            int y,
            int zoom,
            GeoEnvelope envelope)
    {
        mZoomLevel = zoom;
        mX = x;
        mY = y;
        mEnvelope = envelope;
    }


    private TileItem(Parcel in)
    {
        mZoomLevel = in.readInt();
        mX = in.readInt();
        mY = in.readInt();
        double minX = in.readDouble();
        double minY = in.readDouble();
        double maxX = in.readDouble();
        double maxY = in.readDouble();
        mEnvelope = new GeoEnvelope(minX, maxX, minY, maxY);
    }


    @Override
    public int describeContents()
    {
        return 0;
    }


    @Override
    public void writeToParcel(
            Parcel parcel,
            int i)
    {
        parcel.writeInt(mX);
        parcel.writeInt(mY);
        parcel.writeInt(mZoomLevel);
        parcel.writeDouble(mEnvelope.getMinX());
        parcel.writeDouble(mEnvelope.getMinY());
        parcel.writeDouble(mEnvelope.getMaxX());
        parcel.writeDouble(mEnvelope.getMaxY());
    }

    public final String toString(final String pattern)
    {
        String out = pattern;
        out = out.replace("{z}", Integer.toString(getZoomLevel()));
        out = out.replace("{x}", Integer.toString(getX()));
        out = out.replace("{y}", Integer.toString(getY()));
        return out;

    }

    public final String toString(){
        return "" + getZoomLevel() + FileUtil.getPathSeparator() + getX() + FileUtil.getPathSeparator() + getY();
    }

    public final GeoPoint getPoint(){
        return new GeoPoint(mEnvelope.getMinX(), mEnvelope.getMaxY());
    }

    public final GeoEnvelope getEnvelope(){
        return mEnvelope;
    }

    public final int getZoomLevel()
    {
        return mZoomLevel;
    }


    public final int getX()
    {
        return mX;
    }


    public final int getY()
    {
        return mY;
    }


    public final String getHash()
    {
        return "z" + mZoomLevel + "." + mX + "." + mY;
    }
}
