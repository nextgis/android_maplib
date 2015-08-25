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

import android.util.Pair;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by bishop on 21.08.15.
 */
public class GeoGeometryStorage {

    protected static final String GEOMETRY = "shape";

    protected List<Pair<Long, Long>> mOffsetList;
    protected static final long MAX_GEOMETRY_COUNT = 500000;
    protected static final long LIST_OFFSET = MAX_GEOMETRY_COUNT * (Long.SIZE + Long.SIZE) + Long.SIZE;

    protected File mPath;

    public GeoGeometryStorage(File path) {
        mPath = new File(path, GEOMETRY);
        mOffsetList = new LinkedList<>();

        init();
    }

    protected void init(){
        if(mPath.exists()){

        }
        else{

        }
    }

    public boolean addGeometry(long featureId, GeoGeometry geometry){

    }

    public boolean deleteGeometry(long featureId){

    }

    public boolean updateGeometry(long featureId, GeoGeometry geometry){

    }
}
