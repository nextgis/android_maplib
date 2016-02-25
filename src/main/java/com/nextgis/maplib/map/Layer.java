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

package com.nextgis.maplib.map;

import android.content.Context;
import android.util.Log;
import com.nextgis.maplib.api.ILayerView;
import com.nextgis.maplib.api.IRenderer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.display.GISDisplay;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.nextgis.maplib.util.Constants.*;


public class Layer extends Table
        implements ILayerView, IRenderer
{
    protected boolean     mIsVisible;
    protected float       mMaxZoom;
    protected float       mMinZoom;
    protected IRenderer   mRenderer;
    protected GeoEnvelope mExtents;


    public Layer(
            Context context,
            File path)
    {
        super(context, path);
        mExtents = new GeoEnvelope();
    }

    @Override
    public void runDraw(GISDisplay display)
    {
        if (mRenderer != null) {
            mRenderer.runDraw(display);
            //onDrawFinished(this.getId(), 1.0f);
        }
    }

    @Override
    public void cancelDraw()
    {
        if (mRenderer != null) {
            mRenderer.cancelDraw();
        }
    }

    @Override
    public boolean isVisible()
    {
        return mIsVisible;
    }

    @Override
    public void setVisible(boolean visible)
    {
        mIsVisible = visible;
        notifyLayerChanged();
    }

    @Override
    public float getMaxZoom()
    {
        return Float.compare(mMaxZoom, mMinZoom) == 0 ? GeoConstants.DEFAULT_MAX_ZOOM : mMaxZoom;
    }

    @Override
    public void setMaxZoom(float maxZoom)
    {
        mMaxZoom = maxZoom;
    }

    @Override
    public float getMinZoom()
    {
        return mMinZoom;
    }

    @Override
    public void setMinZoom(float minZoom)
    {
        mMinZoom = minZoom;
    }

    @Override
    public JSONObject toJSON()
            throws JSONException
    {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_MAXLEVEL_KEY, getMaxZoom());
        rootConfig.put(JSON_MINLEVEL_KEY, getMinZoom());
        rootConfig.put(JSON_VISIBILITY_KEY, isVisible());
        return rootConfig;
    }

    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        super.fromJSON(jsonObject);
        if (jsonObject.has(JSON_MAXLEVEL_KEY)) {
            mMaxZoom = (float) jsonObject.getDouble(JSON_MAXLEVEL_KEY);
        } else {
            mMaxZoom = GeoConstants.DEFAULT_MAX_ZOOM;
        }
        if (jsonObject.has(JSON_MINLEVEL_KEY)) {
            mMinZoom = (float) jsonObject.getDouble(JSON_MINLEVEL_KEY);
        } else {
            mMinZoom = GeoConstants.DEFAULT_MIN_ZOOM;
        }

        mIsVisible = jsonObject.getBoolean(JSON_VISIBILITY_KEY);

        if(Constants.DEBUG_MODE){
            Log.d(Constants.TAG, "Layer " + getName() + " is visible " + mIsVisible);
            Log.d(Constants.TAG, "Layer " + getName() + " zoom limits from " + mMinZoom + " to " + mMaxZoom);
        }
    }

    @Override
    public void onDrawFinished(
            int id,
            float percent)
    {
        if (mParent != null && mParent instanceof ILayerView) {
            ILayerView layerView = (ILayerView) mParent;
            layerView.onDrawFinished(id, percent);
        }
    }

    @Override
    public void setViewSize(
            int w,
            int h)
    {

    }

    @Override
    public GeoEnvelope getExtents()
    {
        return mExtents;
    }

    @Override
    public IRenderer getRenderer()
    {
        return mRenderer;
    }

    @Override
    public void setRenderer(IRenderer renderer) {
        mRenderer = renderer;
    }
}
