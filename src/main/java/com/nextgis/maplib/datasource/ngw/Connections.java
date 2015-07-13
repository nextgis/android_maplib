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

package com.nextgis.maplib.datasource.ngw;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;


public class Connections
        implements INGWResource
{
    protected String           mName;
    protected List<Connection> mConnections;
    protected        int mId    = 0;
    protected static int mNewId = 1;


    public Connections(String name)
    {
        mName = name;
        mConnections = new ArrayList<>();
    }


    public static int getNewId()
    {
        return mNewId++;
    }


    public void add(Connection connection)
    {
        if (null != mConnections) {
            connection.setParent(this);
            mConnections.add(connection);
        }
    }


    @Override
    public INGWResource getChild(int i)
    {
        if (null == mConnections || i < 0 || i >= mConnections.size()) {
            return null;
        }
        return mConnections.get(i);
    }


    @Override
    public String getName()
    {
        return mName;
    }


    @Override
    public int getType()
    {
        return Connection.NGWResourceTypeConnections;
    }


    @Override
    public int getId()
    {
        return mId;
    }


    @Override
    public INGWResource getResourceById(int id)
    {
        if (id == mId) {
            return this;
        }
        for (Connection connection : mConnections) {
            INGWResource ret = connection.getResourceById(id);
            if (null != ret) {
                return ret;
            }
        }
        return null;
    }


    @Override
    public int getChildrenCount()
    {
        if (null == mConnections) {
            return 0;
        }
        return mConnections.size();
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
        parcel.writeString(mName);
        parcel.writeInt(mId);
        parcel.writeInt(mConnections.size());
        for (Connection connection : mConnections) {
            parcel.writeParcelable(connection, i);
        }
    }


    public static final Parcelable.Creator<Connections> CREATOR =
            new Parcelable.Creator<Connections>()
            {
                public Connections createFromParcel(Parcel in)
                {
                    return new Connections(in);
                }


                public Connections[] newArray(int size)
                {
                    return new Connections[size];
                }
            };


    protected Connections(Parcel in)
    {
        mName = in.readString();
        mId = in.readInt();
        int count = in.readInt();
        mConnections = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Connection it = in.readParcelable(Connection.class.getClassLoader());
            it.setParent(this);
            mConnections.add(it);
        }
    }


    @Override
    public INGWResource getParent()
    {
        return null;
    }


    @Override
    public void setParent(INGWResource resource)
    {

    }

    @Override
    public String getKey() {
        return "";
    }
}