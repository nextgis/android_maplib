/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2015. NextGIS, info@nextgis.com
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
 */

package com.nextgis.maplib.datasource;

import com.nextgis.maplib.api.IGeometryCache;
import com.nextgis.maplib.api.IGeometryCacheItem;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Geometry cache based on plain list (ArrayList)
 */
public class GeometryPlainList implements IGeometryCache {
    protected List<VectorCacheItem> mVectorCacheItems;

    public GeometryPlainList() {
        mVectorCacheItems = new LinkedList<>();
    }

    @Override
    public boolean isItemExist(long featureId) {
        for(VectorCacheItem item : mVectorCacheItems){
            if(item.getFeatureId() == featureId)
                return true;
        }
        return false;
    }

    @Override
    public IGeometryCacheItem addItem(long id, GeoEnvelope envelope) {
        final VectorCacheItem item = new VectorCacheItem(envelope, id);
        mVectorCacheItems.add(item);
        return item;
    }

    @Override
    public IGeometryCacheItem getItem(long featureId) {
        for (VectorCacheItem cacheItem : mVectorCacheItems) {
            if (cacheItem.getFeatureId() == featureId) {
                return cacheItem;
            }
        }
        return null;
    }

    @Override
    public IGeometryCacheItem removeItem(long featureId) {
        Iterator<VectorCacheItem> cacheItemIterator = mVectorCacheItems.iterator();
        while (cacheItemIterator.hasNext()) {
            VectorCacheItem cacheItem = cacheItemIterator.next();

            if (cacheItem.getFeatureId() == featureId) {
                cacheItemIterator.remove();

                return cacheItem;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return mVectorCacheItems.size();
    }

    @Override
    public void clear() {
        mVectorCacheItems.clear();
    }

    @Override
    public List<IGeometryCacheItem> search(final GeoEnvelope extent) {

        final List<IGeometryCacheItem> ret = new LinkedList<>();
        for (VectorCacheItem cacheItem : mVectorCacheItems) {
            if (cacheItem.getEnvelope().intersects(extent)) {
                ret.add(cacheItem);
            }
        }
        return ret;
    }

    @Override
    public List<IGeometryCacheItem> getAll() {
        LinkedList<IGeometryCacheItem> result = new LinkedList<>();
        for(IGeometryCacheItem item : mVectorCacheItems){
            result.add(item);
        }
        return result;
    }

    @Override
    public void changeId(long oldFeatureId, long newFeatureId) {
        IGeometryCacheItem item = getItem(oldFeatureId);
        if (null != item)
            item.setFeatureId(newFeatureId);
    }

    @Override
    public void save(File path) {
        // TODO: 02.09.15
    }

    @Override
    public void load(File path) {
        // TODO: 02.09.15
    }

    protected class VectorCacheItem implements IGeometryCacheItem
    {
        protected GeoEnvelope mEnvelope;
        protected long        mId;


        public VectorCacheItem(
                GeoEnvelope envelope,
                long id)
        {
            mEnvelope = envelope;
            mId = id;
        }


        @Override
        public GeoEnvelope getEnvelope()
        {
            return mEnvelope;
        }


        public void setEnvelope(GeoEnvelope envelope)
        {
            mEnvelope = envelope;
        }


        @Override
        public long getFeatureId()
        {
            return mId;
        }

        @Override
        public void setFeatureId(long id)
        {
            mId = id;
        }
    }
}
