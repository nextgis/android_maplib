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

import android.util.Log;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.VectorCacheItem;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;
import static com.nextgis.maplib.util.Constants.TAG;


public class RuleFeatureRenderer
        extends SimpleFeatureRenderer
{
    protected IStyleRule       mStyleRule;
    protected ArrayList<Style> mParametrizedStyles;


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
            return mStyle;
        }

        final VectorLayer vectorLayer = (VectorLayer) mLayer;
        final List<VectorCacheItem> cache = vectorLayer.getVectorCache();

        try {

            if (null == mParametrizedStyles) {
                mParametrizedStyles = new ArrayList<>(cache.size());

                for (int i = 0; i < cache.size(); i++) {
                    final VectorCacheItem item = cache.get(i);

                    Style styleClone = mStyle.clone();
                    mStyleRule.setStyleParams(styleClone, item.getId());
                    mParametrizedStyles.add(styleClone);
                }
            }

            return mParametrizedStyles.get((int) featureId);

        } catch (CloneNotSupportedException e) {
            Log.d(TAG, "Returned not parametrized style, " + e.getLocalizedMessage());
            mParametrizedStyles = null;
            return mStyle;
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
