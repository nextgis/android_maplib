/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2018 NextGIS, info@nextgis.com
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

import android.util.Pair;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Connection
        implements INGWResource
{
    protected int mNgwVersionMajor = Constants.NOT_FOUND;
    protected int mNgwVersionMinor = Constants.NOT_FOUND;

    protected String        mName;
    protected String        mLogin;
    protected String        mPassword;
    protected String        mURL;
    protected boolean       mIsConnected;
    protected String        mCookie;
    protected List<Integer> mSupportedTypes;
    protected ResourceGroup mRootResource;
    protected int           mId;
    protected INGWResource  mParent;

    public final static int NGWResourceTypeNone              = 1 << 0;
    public final static int NGWResourceTypeResourceGroup     = 1 << 1;
    public final static int NGWResourceTypePostgisLayer      = 1 << 2;
    public final static int NGWResourceTypePostgisConnection = 1 << 3;
    public final static int NGWResourceTypeWMSServerService  = 1 << 4;
    public final static int NGWResourceTypeBaseLayers        = 1 << 5;
    public final static int NGWResourceTypeWebMap            = 1 << 6;
    public final static int NGWResourceTypeWFSServerService  = 1 << 7;
    public final static int NGWResourceTypeVectorLayer       = 1 << 8;
    public final static int NGWResourceTypeRasterLayer       = 1 << 9;
    public final static int NGWResourceTypeVectorLayerStyle  = 1 << 10;
    public final static int NGWResourceTypeRasterLayerStyle  = 1 << 11;
    public final static int NGWResourceTypeFileSet           = 1 << 12;
    public final static int NGWResourceTypeConnection        = 1 << 13;
    public final static int NGWResourceTypeConnections       = 1 << 14;
    public final static int NGWResourceTypeWMSClient         = 1 << 15;
    public final static int NGWResourceTypeLookupTable       = 1 << 16;


    public Connection(
            String name,
            String login,
            String password,
            String URL)
    {
        mName = name;
        mLogin = login;
        mPassword = password;
        if (URL.startsWith("http")) {
            mURL = URL;
        } else {
            mURL = "http://" + URL;
        }
        mIsConnected = false;
        mId = Connections.getNewId();
        mSupportedTypes = new ArrayList<>();
    }

    public boolean connect(boolean guest) {
        setNgwVersion();
        fillCapabilities();

        mRootResource = new ResourceGroup(0, this);
        mRootResource.setParent(this);

        if (!guest) {
            try {
                AtomicReference<String> reference = new AtomicReference<>(mURL);
                mCookie = NGWUtil.getConnectionCookie(reference, mLogin, mPassword);
                if (null == mCookie) {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        mIsConnected = true;
        return true;
    }

    protected void setNgwVersion()
    {
        Pair<Integer, Integer> ver = null;
        try {
            ver = NGWUtil.getNgwVersion(mURL, mLogin, mPassword);
        } catch (IOException | JSONException | NumberFormatException ignored) { }

        if (null != ver) {
            mNgwVersionMajor = ver.first;
            mNgwVersionMinor = ver.second;
        }
    }


    public int getNgwVersionMajor()
    {
        return mNgwVersionMajor;
    }


    public int getNgwVersionMinor()
    {
        return mNgwVersionMinor;
    }


    public String getCookie()
    {
        return mCookie;
    }


    public String getURL()
    {
        return mURL;
    }


    protected void fillCapabilities()
    {
        mSupportedTypes.clear();
        try {
            String sURL = mURL + "/resource/schema";
            HttpResponse response = NetworkUtil.get(sURL, getLogin(), getPassword(), false);
            if (!response.isOk())
                return;
            JSONObject schema = new JSONObject(response.getResponseBody());
            JSONObject resources = schema.getJSONObject("resources");
            if (null != resources) {
                Iterator<String> keys = resources.keys();
                while (keys.hasNext()) {
                    int type = getType(keys.next());
                    if (type != NGWResourceTypeNone) {
                        if (mSupportedTypes.isEmpty()) {
                            mSupportedTypes.add(type);
                        } else if (!isTypeSupported(type)) {
                            mSupportedTypes.add(type);
                        }
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }


    public boolean isTypeSupported(int type)
    {
        return mSupportedTypes.isEmpty() || mSupportedTypes.contains(type);
    }


    public int getType(String sType)
    {
        switch (sType) {
            case "resource_group":
                return NGWResourceTypeResourceGroup;
            case "postgis_layer":
                return NGWResourceTypePostgisLayer;
            case "wmsserver_service":
                return NGWResourceTypeWMSServerService;
            case "baselayers":
                return NGWResourceTypeBaseLayers;
            case "postgis_connection":
                return NGWResourceTypePostgisConnection;
            case "webmap":
                return NGWResourceTypeWebMap;
            case "wfsserver_service":
                return NGWResourceTypeWFSServerService;
            case "vector_layer":
                return NGWResourceTypeVectorLayer;
            case "raster_layer":
                return NGWResourceTypeRasterLayer;
            case "file_bucket":
                return NGWResourceTypeFileSet;
            case "wmsclient_layer":
                return NGWResourceTypeWMSClient;
            case "lookup_table":
                return NGWResourceTypeLookupTable;
            default:
                return NGWResourceTypeNone;
        }
    }


    public ResourceGroup getRootResource()
    {
        return mRootResource;
    }


    @Override
    public String getName()
    {
        return mName;
    }


    @Override
    public int getType()
    {
        return NGWResourceTypeConnection;
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
        if (null == mRootResource) //not connected
        {
            return null;
        }
        return mRootResource.getResourceById(id);
    }


    @Override
    public int getChildrenCount()
    {
        if (null == mRootResource) {
            return 0;
        }
        return mRootResource.getChildrenCount();
    }


    @Override
    public INGWResource getChild(int i)
    {
        if (null == mRootResource) {
            return null;
        }
        return mRootResource.getChild(i);
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

    @Override
    public String getKey() {
        return "";
    }


    public void loadChildren()
    {
        if (null != mRootResource) {
            mRootResource.loadChildren();
        }
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
        parcel.writeString(mLogin);
        parcel.writeString(mPassword);
        parcel.writeString(mURL);
        parcel.writeByte(mIsConnected ? (byte) 1 : (byte) 0);
        parcel.writeString(mCookie);
        parcel.writeInt(mId);
        parcel.writeInt(mSupportedTypes.size());
        for (Integer type : mSupportedTypes) {
            parcel.writeInt(type);
        }
        parcel.writeParcelable(mRootResource, i);
    }


    public static final Parcelable.Creator<Connection> CREATOR =
            new Parcelable.Creator<Connection>()
            {
                public Connection createFromParcel(Parcel in)
                {
                    return new Connection(in);
                }


                public Connection[] newArray(int size)
                {
                    return new Connection[size];
                }
            };


    protected Connection(Parcel in)
    {
        mName = in.readString();
        mLogin = in.readString();
        mPassword = in.readString();
        mURL = in.readString();
        mIsConnected = in.readByte() == 1;
        mCookie = in.readString();
        mId = in.readInt();
        int count = in.readInt();
        mSupportedTypes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            mSupportedTypes.add(in.readInt());
        }
        mRootResource = in.readParcelable(ResourceGroup.class.getClassLoader());
        if (null != mRootResource) {
            mRootResource.setConnection(this);
            mRootResource.setParent(this);
        }
    }


    public boolean isConnected()
    {
        return mIsConnected;
    }


    public String getLogin()
    {
        return mLogin;
    }


    public String getPassword()
    {
        return mPassword;
    }
}

