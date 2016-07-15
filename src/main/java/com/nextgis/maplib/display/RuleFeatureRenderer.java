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

package com.nextgis.maplib.display;

import com.nextgis.maplib.api.IJSONStore;
import com.nextgis.maplib.api.IStyleRule;
import com.nextgis.maplib.map.Layer;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;
import static com.nextgis.maplib.util.Constants.JSON_STYLE_RULE_KEY;


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


    @Override
    protected Style getStyle(long featureId)
    {
        if (null == mStyleRule) {
            return super.getStyle(featureId);
        }

        try {
            Style styleClone = mStyle.clone();
            mStyleRule.setStyleParams(styleClone, featureId);
            applyField(styleClone, featureId);
            return styleClone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return super.getStyle(featureId);
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

        if (mStyleRule instanceof IJSONStore)
            rootJsonObject.put(JSON_STYLE_RULE_KEY, ((IJSONStore) mStyleRule).toJSON());

        return rootJsonObject;
    }
}
