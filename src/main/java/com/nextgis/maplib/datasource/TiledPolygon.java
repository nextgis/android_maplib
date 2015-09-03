package com.nextgis.maplib.datasource;

import com.nextgis.maplib.util.GeoConstants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by bishop on 28.08.15.
 */
public class TiledPolygon extends GeoPolygon {
    protected static final long serialVersionUID = -1241179697270831780L;

    protected List<GeoLineString> mBorders;

    public TiledPolygon() {
        super();
        mBorders = new LinkedList<>();
    }

    @Override
    public int getType() {
        return GeoConstants.GTTiledPolygon;
    }

    @Override
    public void read(DataInputStream stream) throws IOException {
        super.read(stream);
        int borderCount = stream.readInt();
        for (int i = 0; i < borderCount; i++){
            GeoGeometry geometry = GeoGeometryFactory.fromDataStream(stream);
            if(null != geometry && geometry instanceof GeoLineString){
                mBorders.add((GeoLineString) geometry);
            }
        }
    }

    @Override
    public void write(DataOutputStream stream) throws IOException {
        super.write(stream);
        stream.writeInt(mBorders.size());
        for(int i = 0; i < mBorders.size(); i++){
            mBorders.get(i).write(stream);
        }
    }

    @Override
    public void clear() {
        super.clear();
        mBorders.clear();
    }

    public List<GeoLineString> getBorders() {
        return mBorders;
    }
}
