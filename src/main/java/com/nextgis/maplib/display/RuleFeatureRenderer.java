/*******************************************************************************
 * Project:  NextGIS mobile apps for Compulink
 * Purpose:  Mobile GIS for Android
 * Authors:  Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 *           NikitaFeodonit, nfeodonit@yandex.com
 * *****************************************************************************
 * Copyright (C) 2014-2015 NextGIS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.nextgis.maplib.display;

import com.nextgis.maplib.map.Layer;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;


public class RuleFeatureRenderer
        extends SimpleFeatureRenderer
{
    protected IStyleRule mStyleRule;


    public RuleFeatureRenderer(Layer layer)
    {
        super(layer);
        mStyleRule = null;
    }


    public RuleFeatureRenderer(
            Layer layer,
            IStyleRule styleRule)
    {
        super(layer);
        mStyleRule = styleRule;
    }


    public RuleFeatureRenderer(
            Layer layer,
            IStyleRule styleRule,
            Style style)
    {
        super(layer, style);
        mStyleRule = styleRule;
    }


    protected void setStyleParams(Style style, long featureId)
    {
        if (null != mStyleRule) {
            mStyleRule.setStyleParams(mStyle, featureId);
        }
    }


    public IStyleRule getStyleRule()
    {
        return mStyleRule;
    }


    public void setStyleRule(IStyleRule styleRule)
    {
        mStyleRule = styleRule;
    }


    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootJsonObject = super.toJSON();
        rootJsonObject.put(JSON_NAME_KEY, "RuleFeatureRenderer");
        return rootJsonObject;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
    }
}
