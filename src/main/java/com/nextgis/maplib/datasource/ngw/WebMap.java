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
    private List<Long> mIds;

    public WebMap(JSONObject json, Connection connection) {
        super(json, connection);
        try {
            JSONArray layers = json.getJSONObject("webmap").getJSONObject("root_item").getJSONArray("children");
            mIds = new ArrayList<>();
            for (int i = 0; i < layers.length(); i++) {
                JSONObject item = layers.getJSONObject(i);
                switch (item.getString("item_type")) {
                    case "layer":
                        mIds.add(0, item.getLong("layer_style_id"));
                        break;
                    case "group":
                        JSONArray children = item.getJSONArray("children");
                        for (int j = 0; j < children.length(); j++) {
                            mIds.add(0, children.getJSONObject(j).getLong("layer_style_id"));
                        }
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public WebMap(long remoteId, Connection connection) {
        super(remoteId, connection);
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
        return NGWUtil.getTMSUrl(mConnection.getURL(), mIds.toArray(new Long[mIds.size()]));
    }
}
