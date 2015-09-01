package com.nextgis.maplib.datasource;

import com.nextgis.maplib.util.GeoConstants;


/**
 * Created by bishop on 29.08.15.
 */
public class MultiTiledPolygon
        extends GeoGeometryCollection{

    @Override
    public void add(GeoGeometry geometry)
            throws ClassCastException
    {
        if (!(geometry instanceof TiledPolygon)) {
            throw new ClassCastException("MultiTiledPolygon: geometry is not TiledPolygon type.");
        }

        super.add(geometry);
    }


    @Override
    public GeoPolygon get(int index)
    {
        return (GeoPolygon) mGeometries.get(index);
    }

    public TiledPolygon item(int index){
        return (TiledPolygon) mGeometries.get(index);
    }

    @Override
    public int getType()
    {
        return GeoConstants.GTMultiTiledPolygon;
    }


    public void add(TiledPolygon polygon)
    {
        super.add(polygon);
    }

    @Override
    protected GeoGeometryCollection getInstance() {
        return new MultiTiledPolygon();
    }
}
