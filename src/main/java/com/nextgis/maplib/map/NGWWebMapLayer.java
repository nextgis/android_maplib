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

package com.nextgis.maplib.map;

import android.content.Context;

import com.nextgis.maplib.datasource.ngw.WebMapChild;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.NGWUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NGWWebMapLayer extends NGWRasterLayer {
    protected List<WebMapChild> mChildren;

    public NGWWebMapLayer(Context context, File path) {
        super(context, path);
        mChildren = new ArrayList<>();
        mLayerType = Constants.LAYERTYPE_NGW_WEBMAP;
        setTileMaxAge(Constants.ONE_DAY / 2); // TODO move to settings
        mExtentReceived = true;
    }

    public void setChildren(List<WebMapChild> children) {
        mChildren = children;
    }

    public String updateURL() {
        ArrayList<Long> visibleIds = new ArrayList<>();
        for (WebMapChild child : mChildren)
            if (child.isVisible())
                visibleIds.add(child.getId());

        String server = getURL().replaceAll("/api/component/render/.*", "");
        server = NGWUtil.getTMSUrl(server, visibleIds.toArray(new Long[visibleIds.size()]));
        setURL(server);
        return server;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject root = super.toJSON();
        JSONArray children = new JSONArray();
        for (WebMapChild layer : mChildren) {
            JSONObject object = new JSONObject();
            object.put(Constants.JSON_ID_KEY, layer.getId());
            object.put(Constants.JSON_NAME_KEY, layer.getName());
            object.put(Constants.JSON_VISIBILITY_KEY, layer.isVisible());
            children.put(object);
        }

        root.put(Constants.JSON_LAYERS_KEY, children);
        return root;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        super.fromJSON(jsonObject);

        JSONArray children = jsonObject.getJSONArray(Constants.JSON_LAYERS_KEY);
        for (int i = 0; i < children.length(); i++) {
            JSONObject child = children.getJSONObject(i);
            long id = child.getLong(Constants.JSON_ID_KEY);
            String name = child.getString(Constants.JSON_NAME_KEY);
            boolean visible = child.getBoolean(Constants.JSON_VISIBILITY_KEY);
            mChildren.add(new WebMapChild(id, name, visible));
        }
    }
}
