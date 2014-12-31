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

import android.os.Parcel;
import android.os.Parcelable;

public class TileItem implements Parcelable {
    protected int mZoomLevel;
    protected int mX;
    protected int mY;
    protected GeoPoint mTopLeftCorner;

    public TileItem(int x, int y, int zoom, GeoPoint topLeftCorner){
        mZoomLevel = zoom;
        mX = x;
        mY = y;
        mTopLeftCorner = topLeftCorner;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mX);
        parcel.writeInt(mY);
        parcel.writeInt(mZoomLevel);
        parcel.writeDouble(mTopLeftCorner.getX());
        parcel.writeDouble(mTopLeftCorner.getY());
    }

    public static final Creator<TileItem> CREATOR
            = new Creator<TileItem>() {
        public TileItem createFromParcel(Parcel in) {
            return new TileItem(in);
        }

        public TileItem[] newArray(int size) {
            return new TileItem[size];
        }
    };

    private TileItem(Parcel in) {
        mZoomLevel = in.readInt();
        mX = in.readInt();
        mY = in.readInt();
        double x = in.readDouble();
        double y = in.readDouble();
        mTopLeftCorner = new GeoPoint(x, y);
    }

    public final GeoPoint getPoint() {
        return mTopLeftCorner;
    }

    public final int getZoomLevel() {
        return mZoomLevel;
    }

    public final int getX(){
        return mX;
    }

    public final int getY(){
        return mY;
    }

    public String toString(final String pattern){
        String out = pattern;
        out = out.replace("{z}", Integer.toString(getZoomLevel()));
        out = out.replace("{x}", Integer.toString(getX()));
        out = out.replace("{y}", Integer.toString(getY()));
        return out;

    }
}
