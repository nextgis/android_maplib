/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2019 NextGIS, info@nextgis.com
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

import com.nextgis.maplib.datasource.Geo;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.JSON_EXTENT_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MAX_LAT_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MAX_LON_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MIN_LAT_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MIN_LON_KEY;

public class LayerWithStyles
        extends Resource
{
    private Map<Long ,String> mStylesMap;
    private Map<Long ,String> mFormsMap = new HashMap<>();

    private GeoEnvelope mExtent;

    protected LayerWithStyles(Parcel in)
    {
        super(in);
        mStylesMap = new HashMap<>();
        mFormsMap = new HashMap<>();
    }


    public LayerWithStyles(
            JSONObject data,
            Connection connection)
    {
        super(data, connection);
    }


    public LayerWithStyles(
            long remoteId,
            Connection connection)
    {
        super(remoteId, connection);
    }


    public static final Parcelable.Creator<LayerWithStyles> CREATOR =
            new Parcelable.Creator<LayerWithStyles>()
            {
                public LayerWithStyles createFromParcel(Parcel in)
                {
                    return new LayerWithStyles(in);
                }


                public LayerWithStyles[] newArray(int size)
                {
                    return new LayerWithStyles[size];
                }
            };


    @Override
    public void writeToParcel(
            Parcel parcel,
            int i)
    {
        super.writeToParcel(parcel, i);
        if (null == mStylesMap) {
            parcel.writeInt(0);
        } else {
        }
        if (null == mFormsMap) {
            parcel.writeInt(0);
        } else {
        }
    }

    @Override
    public int getChildrenCount()
    {
        return 0;
    }


    @Override
    public INGWResource getChild(int i)
    {
        return null;
    }


    public static void fillStyles(String url, String user, String pass, Long remoteId,
                                     Map<Long ,String> stylesMap,
                                     Map<Long ,String> formsMap) {
        try {
            String sURL = NGWUtil.getResourceChildrenUrl(url, remoteId);
            HttpResponse response = NetworkUtil.get(sURL, user, pass, false);
            if (!response.isOk())
                return;

            JSONArray children = new JSONArray(response.getResponseBody());
            for (int i = 0; i < children.length(); i++) {
                //Only store style id
                //To get more style properties need to create style class extended from Resource
                //Style extends Resource
                //mStyles.add(new Style(styleObject, mConnection);
                JSONObject styleObject = children.getJSONObject(i);
                JSONObject jsonNResource = styleObject.getJSONObject("resource");

                JSONArray interfaces = jsonNResource.getJSONArray("interfaces");
                for (int j = 0; j < interfaces.length(); j++) {
                    if (interfaces.getString(j).equals("IRenderableStyle")) {
                        long cRemoteId = jsonNResource.getLong("id");
                        String name = jsonNResource.getString("display_name");
                        if (stylesMap != null)
                            stylesMap.put(cRemoteId, name);
                        break;
                    }
                }

                if (jsonNResource.optString("cls", "").equals("formbuilder_form")) {
                    long cRemoteId = jsonNResource.getLong("id");
                    String name = jsonNResource.getString("display_name");
                    if (formsMap != null) {
                        formsMap.put(cRemoteId, name);
                    }
                }
            }
        } catch (IOException |  JSONException e) {
            e.printStackTrace();

        } catch (Exception  e) {
            e.printStackTrace();
            return;
        }
    }

    public void fillStyles()
    {
        mStylesMap = new HashMap<>();
        mFormsMap = new HashMap<>();
        fillStyles(mConnection.getURL(), mConnection.getLogin(), mConnection.getPassword(),
                mRemoteId, mStylesMap, mFormsMap);
    }

    public int getStyleCount()
    {
        if (null == mStylesMap) {
            return 0;
        }
        return mStylesMap.size();
    }

    public int getFormCount()
    {
        if (null == mFormsMap) {
            return 0;
        }
        return mFormsMap.size();
    }


    public Map<Long ,String> getForms()
    {
        return mFormsMap;
    }

    public Map<Long ,String> getStyles()
    {
        return mStylesMap;
    }

    public Long getStyleId(int i)
    {
        if (mStylesMap.size() >0)
            return mStylesMap.keySet().iterator().next();
        return 0L;
    }

    public Long getFormId(int i)
    {
        if (!mFormsMap.isEmpty())
            return mFormsMap.keySet().iterator().next();
        else
            return -1L;
    }

    public String getTMSUrlByRemoteId(long remoteId)
    {
        Long id = mRemoteId;
        if (getType() == Connection.NGWResourceTypePostgisLayer || getType() == Connection.NGWResourceTypeWMSClient ||
                getType() == Connection.NGWResourceTypeRasterLayer || getType() == Connection.NGWResourceTypeVectorLayer)
            id = remoteId;

        return NGWUtil.getTMSUrl(mConnection.getURL(), new Long[]{id});
    }


    public String getTMSUrl(int styleNo)
    {
        Long id = mRemoteId;
        if (getType() == Connection.NGWResourceTypePostgisLayer || getType() == Connection.NGWResourceTypeWMSClient ||
                getType() == Connection.NGWResourceTypeRasterLayer || getType() == Connection.NGWResourceTypeVectorLayer)
            id = getStyleId(styleNo);

        return NGWUtil.getTMSUrl(mConnection.getURL(), new Long[]{id});
    }


    public String getGeoJSONUrl()
    {
        return NGWUtil.getGeoJSONUrl(mConnection.getURL(), mRemoteId);
    }


    public String getResourceUrl()
    {
        return NGWUtil.getResourceUrl(mConnection.getURL(), mRemoteId);
    }

    public void fillExtent() {
        try {
            mExtent = new GeoEnvelope();
            String url = NGWUtil.getExtent(mConnection.getURL(), mRemoteId);
            HttpResponse response =
                    NetworkUtil.get(url, mConnection.getLogin(), mConnection.getPassword(), false);
            if (!response.isOk())
                return;
            JSONObject extent =
                    new JSONObject(response.getResponseBody()).getJSONObject(JSON_EXTENT_KEY);
            double x = Geo.wgs84ToMercatorSphereX(extent.getDouble(JSON_MAX_LON_KEY));
            double y = Geo.wgs84ToMercatorSphereY(extent.getDouble(JSON_MAX_LAT_KEY));
            mExtent.setMax(x, y);
            x = Geo.wgs84ToMercatorSphereX(extent.getDouble(JSON_MIN_LON_KEY));
            y = Geo.wgs84ToMercatorSphereY(extent.getDouble(JSON_MIN_LAT_KEY));
            mExtent.setMin(x, y);
        } catch (IOException | JSONException ignored) { }
    }

    public GeoEnvelope getExtent() {
        return mExtent;
    }
}
