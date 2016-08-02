/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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

import com.nextgis.maplib.util.NGWUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Web map resource
 */
public class WebMap extends LayerWithStyles {
    private List<WebMapChild> mChildren;

    protected WebMap(Parcel in)
    {
        super(in);
    }

    public WebMap(JSONObject json, Connection connection) {
        super(json, connection);
        try {
            JSONArray layers = json.getJSONObject("webmap").getJSONObject("root_item").getJSONArray("children");
            mChildren = new ArrayList<>();
            for (int i = 0; i < layers.length(); i++)
                fill(layers.getJSONObject(i), "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fill(JSONObject item, String root) throws JSONException {
        String name = item.getString("display_name");
        switch (item.getString("item_type")) {
            case "layer":
                long id = item.getLong("layer_style_id");
                boolean visible = item.getBoolean("layer_enabled");
                mChildren.add(0, new WebMapChild(id, root + name, visible));
                break;
            case "group":
                JSONArray children = item.getJSONArray("children");
                for (int j = 0; j < children.length(); j++)
                    fill(children.getJSONObject(j), root + name + "/");
                break;
        }
    }

    public WebMap(long remoteId, Connection connection) {
        super(remoteId, connection);
    }

    public static final Parcelable.Creator<WebMap> CREATOR =
            new Parcelable.Creator<WebMap>()
            {
                public WebMap createFromParcel(Parcel in)
                {
                    return new WebMap(in);
                }


                public WebMap[] newArray(int size)
                {
                    return new WebMap[size];
                }
            };

    @Override
    public void writeToParcel(
            Parcel parcel,
            int i)
    {
        super.writeToParcel(parcel, i);
    }

    public List<WebMapChild> getChildren() {
        return mChildren;
    }

    @Override
    public int getChildrenCount() {
        return 0;
    }

    @Override
    public INGWResource getChild(int i) {
        return null;
    }

    @Override
    public String getTMSUrl(int styleNo) {
        ArrayList<Long> visibleIds = new ArrayList<>();
        for (WebMapChild child : mChildren)
            if (child.isVisible())
                visibleIds.add(child.getId());

        return NGWUtil.getTMSUrl(mConnection.getURL(), visibleIds.toArray(new Long[visibleIds.size()]));
    }
}
