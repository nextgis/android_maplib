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

package com.nextgis.maplib.display;

import com.nextgis.maplib.api.IStyleRule;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

public class FieldStyleRule implements IStyleRule {
    VectorLayer mLayer;
    Map<String, Style> mStyleRules;
    String mKey;

    public FieldStyleRule(VectorLayer layer, JSONArray rules) {
        mLayer = layer;
        mStyleRules = new TreeMap<>();

        if (rules == null)
            return;

        try {
            for (int i = 0; i < rules.length(); i++) {
                JSONObject ruleObject = rules.getJSONObject(i);
                AtomicReference<Style> reference = new AtomicReference<>();
                SimpleFeatureRenderer.fromJSON(ruleObject, reference);

                if (reference.get() != null) {
                    String value = ruleObject.getString(Constants.JSON_VALUE_KEY);
                    mStyleRules.put(value, reference.get());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setKey(String key) {
        mKey = key;
    }

    public void setStyle(String value, Style style) {
        mStyleRules.put(value, style);
    }

    @Override
    public void setStyleParams(Style style, long featureId) {
        Feature feature = mLayer.getFeature(featureId);
        String value = mKey.equals(Constants.FIELD_ID) ? feature.getId() + "" : feature.getFieldValueAsString(mKey);

        if (value != null) {
            Style rule = mStyleRules.get(value);
            if (rule == null)
                return;

            if (style instanceof SimpleMarkerStyle) {
                SimpleMarkerStyle markerStyle = (SimpleMarkerStyle) style;
                SimpleMarkerStyle ruleStyle = (SimpleMarkerStyle) rule;
                markerStyle.setColor(ruleStyle.getColor());
                markerStyle.setOutlineColor(ruleStyle.getColor());
                markerStyle.setType(ruleStyle.getType());
                markerStyle.setSize(ruleStyle.getSize());
                markerStyle.setWidth(ruleStyle.getWidth());
            } else if (style instanceof SimpleLineStyle) {
                SimpleLineStyle lineStyle = (SimpleLineStyle) style;
                SimpleLineStyle ruleStyle = (SimpleLineStyle) rule;
                lineStyle.setColor(ruleStyle.getColor());
                lineStyle.setOutColor(ruleStyle.getOutColor());
                lineStyle.setType(ruleStyle.getType());
                lineStyle.setWidth(ruleStyle.getWidth());
            } else if (style instanceof SimplePolygonStyle) {
                SimplePolygonStyle polygonStyle = (SimplePolygonStyle) style;
                SimplePolygonStyle ruleStyle = (SimplePolygonStyle) rule;
                polygonStyle.setColor(ruleStyle.getColor());
                polygonStyle.setWidth(ruleStyle.getWidth());
                polygonStyle.setFill(ruleStyle.isFill());
            }
        }
    }
}
