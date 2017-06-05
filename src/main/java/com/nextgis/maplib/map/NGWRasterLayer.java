/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.Geo;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import static com.nextgis.maplib.util.Constants.JSON_EXTENT_KEY;
import static com.nextgis.maplib.util.Constants.JSON_ID_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MAX_LAT_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MAX_LON_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MIN_LAT_KEY;
import static com.nextgis.maplib.util.Constants.JSON_MIN_LON_KEY;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_RASTER;
import static com.nextgis.maplib.util.GeoConstants.MERCATOR_MAX;

public class NGWRasterLayer extends RemoteTMSLayer implements INGWLayer {
    protected String mAccountName;
    protected String mCacheLogin;
    protected String mCachePassword;
    protected long mRemoteId;
    protected boolean mExtentReceived = false;

    protected final static short MAX_THREAD_COUNT = 8;
    protected static final String JSON_ACCOUNT_KEY = "account";

    public NGWRasterLayer(Context context, File path) {
        super(context, path);
        mLayerType = LAYERTYPE_NGW_RASTER;
        mExtents.set(new GeoEnvelope(-MERCATOR_MAX, MERCATOR_MAX, -MERCATOR_MAX, MERCATOR_MAX));
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_ID_KEY, mRemoteId);
        rootConfig.put(JSON_ACCOUNT_KEY, mAccountName);
        rootConfig.put(Constants.JSON_BBOX_MAXX_KEY, mExtents.getMaxX());
        rootConfig.put(Constants.JSON_BBOX_MINX_KEY, mExtents.getMinX());
        rootConfig.put(Constants.JSON_BBOX_MAXY_KEY, mExtents.getMaxY());
        rootConfig.put(Constants.JSON_BBOX_MINY_KEY, mExtents.getMinY());

        return rootConfig;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        super.fromJSON(jsonObject);
        setAccountName(jsonObject.getString(JSON_ACCOUNT_KEY));
        mRemoteId = jsonObject.optInt(JSON_ID_KEY);
        mExtents.setMaxX(jsonObject.optDouble(Constants.JSON_BBOX_MAXX_KEY, MERCATOR_MAX));
        mExtents.setMinX(jsonObject.optDouble(Constants.JSON_BBOX_MINX_KEY, -MERCATOR_MAX));
        mExtents.setMaxY(jsonObject.optDouble(Constants.JSON_BBOX_MAXY_KEY, MERCATOR_MAX));
        mExtents.setMinY(jsonObject.optDouble(Constants.JSON_BBOX_MINY_KEY, -MERCATOR_MAX));
    }

    public void setAccountCacheData() {
        IGISApplication app = (IGISApplication) mContext.getApplicationContext();
        Account account = app.getAccount(mAccountName);

        if (null != account) {
            mCacheLogin = app.getAccountLogin(account);
            mCachePassword = app.getAccountPassword(account);
            if (Constants.DEBUG_MODE)
                Log.d(Constants.TAG, "Get account. User: " + mCacheLogin);
        } else if (Constants.DEBUG_MODE) {
            Log.d(Constants.TAG, "Failed to get account for name: " + mAccountName);
        }
    }

    @Override
    public void sync(String authority, Pair<Integer, Integer> ver, SyncResult syncResult) {
        // TODO: 26.07.15 By now nothing to sync
    }

    @Override
    public int getSyncType() {
        return Constants.SYNC_NONE;
    }

    @Override
    public void setSyncType(int syncType) {

    }

    @Override
    public long getRemoteId() {
        return mRemoteId;
    }

    @Override
    public void setRemoteId(long remoteId) {
        mRemoteId = remoteId;
    }

    @Override
    public int getMaxThreadCount() {
        return MAX_THREAD_COUNT;
    }

    @Override
    public String getAccountName() {
        return mAccountName;
    }

    @Override
    public void setAccountName(String accountName) {
        mAccountName = accountName;
        setAccountCacheData();
    }

    @Override
    public String getLogin() {
        return mCacheLogin;
    }

    @Override
    public void setLogin(String login) {
        throw new AssertionError("NGWRasterLayer.setLogin() can not be used");
    }

    @Override
    public String getPassword() {
        return mCachePassword;
    }

    @Override
    public void setPassword(String password) {
        throw new AssertionError("NGWRasterLayer.setPassword() can not be used");
    }

    @Override
    public Bitmap getBitmap(TileItem tile) {
        if (!mExtentReceived) {
            HttpResponse result = null;
            try {
                AccountUtil.AccountData accountData = AccountUtil.getAccountData(mContext, getAccountName());
                result = NetworkUtil.get(NGWUtil.getExtent(accountData.url, mRemoteId), getLogin(), getPassword(), false);
                if (!result.isOk()) {
                    throw new IllegalStateException("");
                }
                JSONObject extent = new JSONObject(result.getResponseBody()).getJSONObject(JSON_EXTENT_KEY);
                double x = Geo.wgs84ToMercatorSphereX(extent.getDouble(JSON_MAX_LON_KEY));
                double y = Geo.wgs84ToMercatorSphereY(extent.getDouble(JSON_MAX_LAT_KEY));
                mExtents.setMax(x, y);
                x = Geo.wgs84ToMercatorSphereX(extent.getDouble(JSON_MIN_LON_KEY));
                y = Geo.wgs84ToMercatorSphereY(extent.getDouble(JSON_MIN_LAT_KEY));
                mExtents.setMin(x, y);
                mExtentReceived = true;
            } catch (IOException | JSONException | IllegalStateException ignored) {
                if (result != null && result.getResponseCode() == 404)
                    mExtentReceived = true;
            }
        }

        if (mExtents.intersects(tile.getEnvelope()))
            return super.getBitmap(tile);

        return null;
    }
}
