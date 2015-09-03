/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.nextgis.maplib.api.IGeometryCache;
import com.nextgis.maplib.api.IGeometryCacheItem;
import com.nextgis.maplib.api.IStyleRule;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.VectorLayer;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.nextgis.maplib.util.Constants.*;


public class RuleFeatureRenderer
        extends SimpleFeatureRenderer
{
    protected IStyleRule mStyleRule;
    protected Map<Long, Style> mParametrizedStyles = new TreeMap<>();


    public RuleFeatureRenderer(Layer layer)
    {
        super(layer);
        mStyleRule = null;
        init();
    }


    public RuleFeatureRenderer(
            Layer layer,
            IStyleRule styleRule)
    {
        super(layer);
        mStyleRule = styleRule;
        init();
    }


    public RuleFeatureRenderer(
            Layer layer,
            IStyleRule styleRule,
            Style style)
    {
        super(layer, style);
        mStyleRule = styleRule;
        init();
    }


    protected void init()
    {
        // register events from layers modify in services or other applications
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VectorLayer.NOTIFY_DELETE);
        intentFilter.addAction(VectorLayer.NOTIFY_DELETE_ALL);
        intentFilter.addAction(VectorLayer.NOTIFY_INSERT);
        intentFilter.addAction(VectorLayer.NOTIFY_UPDATE);
        intentFilter.addAction(VectorLayer.NOTIFY_UPDATE_FIELDS);
        mLayer.getContext().registerReceiver(new VectorLayerNotifyReceiver(), intentFilter);

        initParametrizedStyles();
    }


    protected void initParametrizedStyles()
    {
        mParametrizedStyles.clear();

        if (null == mStyleRule) {
            return;
        }

        final VectorLayer vectorLayer = (VectorLayer) mLayer;
        try {

            for (Long featureId : vectorLayer.query(null)) {
                putParametrizedStyle(featureId);
            }

        } catch (CloneNotSupportedException e) {
            Log.d(
                    TAG,
                    "Warning, mParametrizedStyles is not initialised: " + e.getLocalizedMessage());
            mParametrizedStyles.clear();
        }
    }


    protected void putParametrizedStyle(long featureId)
            throws CloneNotSupportedException
    {
        Style styleClone = mStyle.clone();
        mStyleRule.setStyleParams(styleClone, featureId);
        mParametrizedStyles.put(featureId, styleClone);
    }


    @Override
    protected Style getStyle(long featureId)
    {
        if (null == mStyleRule || mParametrizedStyles.size() == 0) {
            return mStyle;
        }

        Style style = mParametrizedStyles.get(featureId);

        return null == style ? mStyle : style;
    }


    public IStyleRule getStyleRule()
    {
        return mStyleRule;
    }


    public void setStyleRule(IStyleRule styleRule)
    {
        mStyleRule = styleRule;
        initParametrizedStyles();
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


    protected class VectorLayerNotifyReceiver
            extends BroadcastReceiver
    {
        @Override
        public void onReceive(
                Context context,
                Intent intent)
        {
            if (null == mStyleRule) {
                return;
            }

            // extreme logging commented
            //Log.d(TAG, "Receive notify: " + intent.getAction());

            try {
                long featureId;

                switch (intent.getAction()) {

                    case VectorLayer.NOTIFY_DELETE:
                        featureId = intent.getLongExtra(FIELD_ID, NOT_FOUND);
                        mParametrizedStyles.remove(featureId);
                        break;

                    case VectorLayer.NOTIFY_DELETE_ALL:
                        mParametrizedStyles.clear();
                        break;

                    case VectorLayer.NOTIFY_UPDATE:
                    case VectorLayer.NOTIFY_UPDATE_FIELDS:
                        featureId = intent.getLongExtra(FIELD_ID, NOT_FOUND);
                        putParametrizedStyle(featureId);

                        if (intent.hasExtra(FIELD_OLD_ID)) {
                            long oldFeatureId = intent.getLongExtra(FIELD_OLD_ID, NOT_FOUND);
                            mParametrizedStyles.remove(oldFeatureId);
                        }

                        break;

                    case VectorLayer.NOTIFY_UPDATE_ALL:
                        initParametrizedStyles();
                        break;

                    case VectorLayer.NOTIFY_INSERT:
                        featureId = intent.getLongExtra(FIELD_ID, NOT_FOUND);
                        putParametrizedStyle(featureId);
                        break;
                }

            } catch (CloneNotSupportedException e) {
                Log.d(
                        TAG, "Warning, mParametrizedStyles is not initialised: " +
                             e.getLocalizedMessage());
                mParametrizedStyles.clear();
            }
        }
    }
}
