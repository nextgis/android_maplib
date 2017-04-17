/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016-2017 NextGIS, info@nextgis.com
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

import com.nextgis.maplib.api.IJSONStore;
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

public class FieldStyleRule implements IStyleRule, IJSONStore {
    public static final String JSON_RULES_KEY = "rules";
    public static final String JSON_FIELD_KEY = "field";

    VectorLayer mLayer;
    Map<String, Style> mStyleRules;
    String mKey;

    public FieldStyleRule(VectorLayer layer) {
        mLayer = layer;
        mStyleRules = new TreeMap<>();
    }

    public void setKey(String key) {
        mKey = key;
    }

    public String getKey() {
        return mKey;
    }

    public void setStyle(String value, Style style) {
        mStyleRules.put(value, style);
    }

    public Style getStyle(String value) {
        return mStyleRules.get(value);
    }

    public void removeStyle(String value) {
        mStyleRules.remove(value);
    }

    public Map<String, Style> getStyleRules() {
        return mStyleRules;
    }

    @Override
    public void setStyleParams(Style style, long featureId) {
        if (mKey == null)
            return;

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
                markerStyle.setOutColor(ruleStyle.getOutColor());
                markerStyle.setType(ruleStyle.getType());
                markerStyle.setSize(ruleStyle.getSize());
                markerStyle.setWidth(ruleStyle.getWidth());
                markerStyle.setText(ruleStyle.getText());
                markerStyle.setField(ruleStyle.getField());
            } else if (style instanceof SimpleLineStyle) {
                SimpleLineStyle lineStyle = (SimpleLineStyle) style;
                SimpleLineStyle ruleStyle = (SimpleLineStyle) rule;
                lineStyle.setColor(ruleStyle.getColor());
                lineStyle.setOutColor(ruleStyle.getOutColor());
                lineStyle.setType(ruleStyle.getType());
                lineStyle.setWidth(ruleStyle.getWidth());
                lineStyle.setText(ruleStyle.getText());
                lineStyle.setField(ruleStyle.getField());
            } else if (style instanceof SimplePolygonStyle) {
                SimplePolygonStyle polygonStyle = (SimplePolygonStyle) style;
                SimplePolygonStyle ruleStyle = (SimplePolygonStyle) rule;
                polygonStyle.setColor(ruleStyle.getColor());
                polygonStyle.setWidth(ruleStyle.getWidth());
                polygonStyle.setFill(ruleStyle.isFill());
                polygonStyle.setText(ruleStyle.getText());
                polygonStyle.setTextSize(ruleStyle.getTextSize());
                polygonStyle.setField(ruleStyle.getField());
            }
        }
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray rules = new JSONArray();
        for (Map.Entry<String, Style> rule : mStyleRules.entrySet()) {
            JSONObject item = new JSONObject();
            item.put(Constants.JSON_VALUE_KEY, rule.getKey());
            item.put(SimpleFeatureRenderer.JSON_STYLE_KEY, rule.getValue().toJSON());
            rules.put(item);
        }

        result.put(JSON_FIELD_KEY, mKey);
        result.put(JSON_RULES_KEY, rules);

        return result;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has(JSON_FIELD_KEY))
            mKey = jsonObject.getString(JSON_FIELD_KEY);

        if (jsonObject.has(JSON_RULES_KEY)) {
            JSONArray rules = jsonObject.getJSONArray(JSON_RULES_KEY);
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
    }

    public void clearRules() {
        mStyleRules.clear();
    }

    public int size() {
        return mStyleRules.size();
    }
}
