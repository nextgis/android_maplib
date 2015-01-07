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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class Resource
{
    protected long mRemoteId;
    protected Connection mConnection;
    protected boolean mHasChildren;
    protected String mDescription;
    protected String mName;
    protected String mKeyName;
    protected long mOwnerId;
    protected JSONObject mPermissions;
    protected int mType;

    public Resource(
            long remoteId,
            Connection connection)
    {
        mRemoteId = remoteId;
        mConnection = connection;
    }

    public Resource(JSONObject json,
                    Connection connection)
    {

        mConnection = connection;
        try {
            JSONObject JSONResource = json.getJSONObject("resource");

            mHasChildren = JSONResource.getBoolean("children");
            if (JSONResource.has("description"))
                mDescription = JSONResource.getString("description");

            mName = JSONResource.getString("display_name");
            mRemoteId = JSONResource.getLong("id");
            if (JSONResource.has("keyname"))
                mKeyName = JSONResource.getString("keyname");
            mOwnerId = JSONResource.getLong("owner_user");
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void fillPermissions()
    {
        try {
            String sURL = mConnection.getURL() + "/api/resource/" + mRemoteId + "/permission";
            HttpGet get = new HttpGet(sURL);
            get.setHeader("Cookie", mConnection.getCookie());
            get.setHeader("Accept", "*/*");
            HttpResponse response = mConnection.getHttpClient().execute(get);
            HttpEntity entity = response.getEntity();
            mPermissions = new JSONObject(EntityUtils.toString(entity));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
