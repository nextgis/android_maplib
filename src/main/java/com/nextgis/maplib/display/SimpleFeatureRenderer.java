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

package com.nextgis.maplib.display;

import com.nextgis.maplib.datasource.GeoEnvelope;

import com.nextgis.maplib.map.Layer;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.Constants.*;

public class SimpleFeatureRenderer extends Renderer{

    protected Style mStyle;

    public static final String JSON_STYLE_KEY = "style";

    public SimpleFeatureRenderer(Layer layer, Style style) {
        super(layer);
        mStyle = style;
    }

    @Override
    public void runDraw(GISDisplay display)
    {
        GeoEnvelope env = display.getBounds();

        //TODO: add drawing routine

        /*final List<Feature> features = mGeoJsonLayer.getFeatures(env);
        for(int i = 0; i < features.size(); i++){
            Feature feature = features.get(i);
            GeoGeometry geometry = feature.getGeometry();

            mStyle.onDraw(geometry, display);

            if(handler != null){
                Bundle bundle = new Bundle();
                bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
                bundle.putInt(BUNDLE_TYPE_KEY, msgtype);
                bundle.putInt(BUNDLE_LAYERNO_KEY, mLayer.getId());
                bundle.putFloat(BUNDLE_DONE_KEY, (float) i / features.size());

                Message msg = new Message();
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        }*/
    }


    @Override
    public void cancelDraw(){

    }

    public Style getStyle() {
        return mStyle;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootJsonObject = new JSONObject();
        rootJsonObject.put(JSON_STYLE_KEY, mStyle.toJSON());
        return rootJsonObject;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        JSONObject styleJsonObject = jsonObject.getJSONObject(JSON_STYLE_KEY);
        String styleName = styleJsonObject.getString(JSON_NAME_KEY);
        switch (styleName)
        {
            case "SimpleMarkerStyle":
                mStyle = new SimpleMarkerStyle();
                mStyle.fromJSON(styleJsonObject);
                break;
            case "SimpleLineStyle":
                mStyle = new SimpleLineStyle();
                mStyle.fromJSON(styleJsonObject);
                break;
        }
    }
}
