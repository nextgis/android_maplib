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

package com.nextgis.maplib.datasource.ngw;

import android.os.Parcel;

import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public abstract class Resource
        implements INGWResource
{
    protected long         mRemoteId;
    protected Connection   mConnection;
    protected boolean      mHasChildren;
    protected String       mDescription;
    protected String       mName;
    protected String       mKeyName;
    protected long         mOwnerId;
    protected JSONObject   mPermissions;
    protected int          mType;
    protected int          mId;
    protected INGWResource mParent;


    public Resource(
            long remoteId,
            Connection connection)
    {
        mRemoteId = remoteId;
        mConnection = connection;
        mId = Connections.getNewId();
    }


    public Resource(
            JSONObject json,
            Connection connection)
    {

        mConnection = connection;
        try {
            JSONObject JSONResource = json.getJSONObject("resource");

            mHasChildren = JSONResource.getBoolean("children");
            if (JSONResource.has("description")) {
                mDescription = JSONResource.getString("description");
            }

            mName = JSONResource.getString("display_name");
            mRemoteId = JSONResource.getLong("id");
            mType = mConnection.getType(JSONResource.getString("cls"));

            if (JSONResource.has("keyname")) {
                mKeyName = JSONResource.getString("keyname");
            }
            if (JSONResource.has("owner_user")) {
                JSONObject jsonObjectOwnerUser = JSONResource.getJSONObject("owner_user");
                if (jsonObjectOwnerUser.has("id") && !jsonObjectOwnerUser.isNull("id")) {
                    mOwnerId = jsonObjectOwnerUser.getLong("id");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mId = Connections.getNewId();
    }


    public void fillPermissions()
    {
        try {
            String sURL = mConnection.getURL() + "/api/resource/" + mRemoteId + "/permission";
            HttpResponse response =
                    NetworkUtil.get(sURL, mConnection.getLogin(), mConnection.getPassword(), false);
            if (!response.isOk())
                return;
            mPermissions = new JSONObject(response.getResponseBody());
            if (!mPermissions.has(Constants.JSON_RESOURCE_KEY))
                mPermissions = null;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getName()
    {
        return mName;
    }

    public String getDescription() {
        return mDescription;
    }

    @Override
    public String getKey() {
        return mKeyName;
    }

    @Override
    public int getType()
    {
        return mType;
    }


    @Override
    public int getId()
    {
        return mId;
    }


    @Override
    public INGWResource getResourceById(int id)
    {
        if (mId == id) {
            return this;
        }
        return null;
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
        parcel.writeByte(mHasChildren ? (byte) 1 : (byte) 0);
        parcel.writeString(mDescription);
        parcel.writeString(mKeyName);
        parcel.writeLong(mOwnerId);
        boolean hasPermissions = null != mPermissions;
        parcel.writeByte(hasPermissions ? (byte) 1 : (byte) 0);
        if (hasPermissions) {
            parcel.writeString(mPermissions.toString());
        }
        parcel.writeInt(mType);
        parcel.writeInt(mId);
    }


    protected Resource(
            Parcel in)
    {
        mName = in.readString();
        mHasChildren = in.readByte() == 1;
        mDescription = in.readString();
        mKeyName = in.readString();
        mOwnerId = in.readLong();
        boolean hasPermissions = in.readByte() == 1;
        if (hasPermissions) {
            try {
                mPermissions = new JSONObject(in.readString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        mType = in.readInt();
        mId = in.readInt();
    }


    public void setConnection(Connection connection)
    {
        mConnection = connection;
    }


    @Override
    public INGWResource getParent()
    {
        return mParent;
    }


    @Override
    public void setParent(INGWResource resource)
    {
        mParent = resource;
    }


    public long getRemoteId()
    {
        return mRemoteId;
    }


    public Connection getConnection()
    {
        return mConnection;
    }
}
