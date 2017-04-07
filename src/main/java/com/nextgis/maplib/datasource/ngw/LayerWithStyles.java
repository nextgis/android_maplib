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
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.JSON_EXTENT_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MAX_LAT_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MAX_LON_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MIN_LAT_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MIN_LON_KEY;

public class LayerWithStyles
        extends Resource
{
    private List<Long> mStyles;
    private GeoEnvelope mExtent;

    protected LayerWithStyles(Parcel in)
    {
        super(in);
        mStyles = new ArrayList<>();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            mStyles.add(in.readLong());
        }
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
        if (null == mStyles) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(mStyles.size());
            for (Long style : mStyles) {
                parcel.writeLong(style);
            }
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


    public void fillStyles()
    {
        mStyles = new ArrayList<>();
        try {
            String sURL = mConnection.getURL() + "/resource/" + mRemoteId + "/child/";
            HttpResponse response =
                    NetworkUtil.get(sURL, mConnection.getLogin(), mConnection.getPassword(), false);
            if (!response.isOk())
                return;
            JSONArray children = new JSONArray(response.getResponseBody());
            for (int i = 0; i < children.length(); i++) {
                //Only store style id
                //To get more style properties need to create style class extended from Resource
                //Style extends Resource
                //mStyles.add(new Style(styleObject, mConnection);
                JSONObject styleObject = children.getJSONObject(i);
                JSONObject JSONResource = styleObject.getJSONObject("resource");

                JSONArray interfaces = JSONResource.getJSONArray("interfaces");
                for (int j = 0; j < interfaces.length(); j++) {
                    if (interfaces.getString(j).equals("IRenderableStyle")) {
                        long remoteId = JSONResource.getLong("id");
                        mStyles.add(remoteId);
                        break;
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }


    public int getStyleCount()
    {
        if (null == mStyles) {
            return 0;
        }
        return mStyles.size();
    }


    public Long getStyleId(int i)
    {
        if (mStyles != null && mStyles.size() > 0)
            return mStyles.get(i);

        return mRemoteId;
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
        return mConnection.getURL() + "/resource/" + mRemoteId;
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
