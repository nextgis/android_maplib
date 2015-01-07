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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.nextgis.maplib.util.Constants.*;


public class Connection
{
    protected String        mLogin;
    protected String        mPassword;
    protected String        mURL;
    protected boolean       mIsConnected;
    protected String        mCookie;
    protected List<Integer> mSupportedTypes;
    protected ResourceGroup mRootResource;

    public final static int NGWResourceTypeNone = 0;
    public final static int NGWResourceTypeResourceGroup = 1;
    public final static int NGWResourceTypePostgisLayer = 2;
    public final static int NGWResourceTypePostgisConnection = 3;
    public final static int NGWResourceTypeWMSServerService = 4;
    public final static int NGWResourceTypeBaseLayers = 5;
    public final static int NGWResourceTypeWebMap = 6;
    public final static int NGWResourceTypeWFSServerService = 7;
    public final static int NGWResourceTypeVectorLayer = 8;
    public final static int NGWResourceTypeRasterLayer = 9;
    public final static int NGWResourceTypeVectorLayerStyle = 10;
    public final static int NGWResourceTypeRasterLayerStyle = 11;
    public final static int NGWResourceTypeFileSet = 12;


    public Connection(
            String login,
            String password,
            String URL)
    {
        mLogin = login;
        mPassword = password;
        mURL = URL;
        mIsConnected = false;
    }


    public boolean connect()
    {
        try {
            HttpPost httppost = new HttpPost(mURL);
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("login", mLogin));
            nameValuePairs.add(new BasicNameValuePair("password", mPassword));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpClient httpclient = getHttpClient();

            HttpResponse response = httpclient.execute(httppost);
            //2 get cookie
            Header head = response.getFirstHeader("Set-Cookie");
            if (head == null)
                return false;
            mCookie = head.getValue();

            mIsConnected = true;

            fillCapabilities();

            mRootResource = new ResourceGroup(0, this);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public HttpClient getHttpClient()
    {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, APP_USER_AGENT);
        httpclient.getParams()
                  .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, TIMEOUT_CONNECTION);
        httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, TIMEOUT_SOKET);
        httpclient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);

        return httpclient;
    }


    public String getCookie()
    {
        return mCookie;
    }


    public String getURL()
    {
        return mURL;
    }


    protected void fillCapabilities(){
        mSupportedTypes.clear();
        try {
            String sURL = mURL + "/resource/schema";
            HttpGet get = new HttpGet(sURL);
            get.setHeader("Cookie", getCookie());
            get.setHeader("Accept", "*/*");
            HttpResponse response = getHttpClient().execute(get);
            HttpEntity entity = response.getEntity();

            JSONObject schema = new JSONObject(EntityUtils.toString(entity));
            JSONObject resources = schema.getJSONObject("resources");
            if(null != resources){
                Iterator<String> keys = resources.keys();
                while(keys.hasNext()){
                    int type = getType(keys.next());
                    if (type != NGWResourceTypeNone)
                    {
                        if (mSupportedTypes.isEmpty())
                            mSupportedTypes.add(type);
                        else if (!isTypeSupported(type))
                            mSupportedTypes.add(type);
                    }
                }
            }
        }
        catch (IOException | JSONException e){
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
            default:
                return NGWResourceTypeNone;
        }
    }


    public ResourceGroup getRootResource()
    {
        return mRootResource;
    }
}
