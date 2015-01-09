/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

package com.nextgis.maplib.datasource.ngw;

import android.os.Parcel;
import android.os.Parcelable;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ResourceGroup extends Resource
{
    protected List<Resource> mChildren;
    protected boolean mChildrenLoaded;

    public ResourceGroup(
            long remoteId,
            Connection connection)
    {
        super(remoteId, connection);
        mType = Connection.NGWResourceTypeResourceGroup;
        mChildren = new ArrayList<>();
        mChildrenLoaded = false;
    }


    public ResourceGroup(
            JSONObject json,
            Connection connection)
    {
        super(json, connection);
        mType = Connection.NGWResourceTypeResourceGroup;
        mChildren = new ArrayList<>();
    }

    public void loadChildren()
    {
        if(mChildrenLoaded)
            return;
        try {
            String sURL = mConnection.getURL() + "/resource/" + mRemoteId + "/child/";
            HttpGet get = new HttpGet(sURL);
            get.setHeader("Cookie", mConnection.getCookie());
            get.setHeader("Accept", "*/*");
            HttpResponse response = mConnection.getHttpClient().execute(get);
            HttpEntity entity = response.getEntity();
            JSONArray children = new JSONArray(EntityUtils.toString(entity));
            for(int i = 0; i < children.length(); i++)
            {
                addResource(children.getJSONObject(i));
            }
            mChildrenLoaded = true;
        }
        catch (IOException | JSONException e ){
            e.printStackTrace();
        }
    }

    protected void addResource(JSONObject data)
    {
        int type = getType(data);
        Resource resource = null;
        switch(type) {
            case Connection.NGWResourceTypeResourceGroup:
                resource = new ResourceGroup(data, mConnection);
                break;
            case Connection.NGWResourceTypePostgisLayer:
            case Connection.NGWResourceTypeVectorLayer:
                break;
            case Connection.NGWResourceTypeRasterLayer:
                //resource = new
                break;
        }

        if(null != resource) {
            resource.setParent(this);
            resource.fillPermissions();
            mChildren.add(resource);
        }
    }

    protected int getType(JSONObject data)
    {
        try {
            String sType = data.getJSONObject("resource").getString("cls");
            return mConnection.getType(sType);
        }
        catch (JSONException e){
            return Connection.NGWResourceTypeNone;
        }
    }


    @Override
    public void writeToParcel(
            Parcel parcel,
            int i)
    {
        super.writeToParcel(parcel, i);
        parcel.writeByte(mChildrenLoaded ? (byte)1 : (byte)0);
        parcel.writeInt(mChildren.size());
        for(Resource resource : mChildren){
            parcel.writeInt(resource.getType());
            parcel.writeParcelable(resource, i);
        }
    }


    public static final Parcelable.Creator<ResourceGroup> CREATOR =
            new Parcelable.Creator<ResourceGroup>()
            {
                public ResourceGroup createFromParcel(Parcel in)
                {
                    return new ResourceGroup(in);
                }


                public ResourceGroup[] newArray(int size)
                {
                    return new ResourceGroup[size];
                }
            };


    protected ResourceGroup(
            Parcel in)
    {
        super(in);
        mChildrenLoaded = in.readByte() == 1;
        int count = in.readInt();
        mChildren = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int type = in.readInt();
            switch (type) {
                case Connection.NGWResourceTypeResourceGroup:
                    ResourceGroup resourceGroup =
                            in.readParcelable(ResourceGroup.class.getClassLoader());
                    resourceGroup.setParent(this);
                    mChildren.add(resourceGroup);
                    break;
            }
        }
    }

    @Override
    public void setConnection(Connection connection)
    {
        super.setConnection(connection);
        for(Resource resource : mChildren){
            resource.setConnection(connection);
        }
    }

    @Override
    public INGWResource getResourceById(int id)
    {
        INGWResource ret = super.getResourceById(id);
        if(null != ret)
            return ret;
        for(Resource resource : mChildren){
            ret = resource.getResourceById(id);
            if(null != ret)
                return ret;
        }
        return super.getResourceById(id);
    }

    @Override
    public int getChildrenCount()
    {
        if(null == mChildren)
            return 0;
        return mChildren.size();
    }


    @Override
    public INGWResource getChild(int i)
    {
        if(null == mChildren)
            return null;
        return mChildren.get(i);
    }


    public boolean isChildrenLoaded()
    {
        return mChildrenLoaded;
    }
}
