package com.nextgis.maplib.datasource;

import com.nextgis.maplib.util.FileUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * NextGIS Mobile vector tile implementation
 */
public class VectorTile {

    protected List<VectorTileItem> mTileItems;

    public VectorTile() {
        mTileItems = new LinkedList<>();
    }

    public int size(){
        return mTileItems.size();
    }

    public VectorTileItem item(int index){
        return mTileItems.get(index);
    }

    public boolean load(File path){
        if(!path.exists()){
            return false;
        }

        try {
            FileInputStream fileInputStream = new FileInputStream(path);
            DataInputStream dataInputStream = new DataInputStream(fileInputStream);
            int count = dataInputStream.readInt();
            for(int i = 0; i < count; i++){
                VectorTileItem tileItem = new VectorTileItem();
                tileItem.read(dataInputStream);
                mTileItems.add(tileItem);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean save(File path){
        try {
            FileUtil.createDir(path.getParentFile());
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
            dataOutputStream.writeInt(mTileItems.size());
            for(VectorTileItem tileItem : mTileItems){
                tileItem.write(dataOutputStream);
            }
            dataOutputStream.flush();
            dataOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void add(long featureId, GeoGeometry geometry) {
        mTileItems.add(new VectorTileItem(featureId, geometry));
    }

    public void delete(long featureId) {
        Iterator<VectorTileItem> iterator = mTileItems.iterator();
        while (iterator.hasNext()) {
            VectorTileItem data = iterator.next();
            if (data.getId() == featureId) {
                iterator.remove();
            }
        }
    }

    public void update(long featureId, GeoGeometry geometry){
        for (VectorTileItem data : mTileItems) {
            if (data.getId() == featureId) {
                data.setGeometry(geometry);
                break;
            }
        }
    }

    public void changeIds(long oldFeatureId, long newFeatureId) {
        for (VectorTileItem data : mTileItems) {
            if (data.getId() == oldFeatureId) {
                data.setId(newFeatureId);
                break;
            }
        }
    }

    public class VectorTileItem {
        protected long mId;
        protected GeoGeometry mGeometry;

        public VectorTileItem() {
        }

        public VectorTileItem( long id, GeoGeometry geometry) {
            mGeometry = geometry;
            mId = id;
        }

        public long getId() {
            return mId;
        }

        public void setId(long id) {
            mId = id;
        }

        public GeoGeometry getGeometry() {
            return mGeometry;
        }

        public void setGeometry(GeoGeometry geometry) {
            mGeometry = geometry;
        }

        public void write(DataOutputStream dataOutputStream) throws IOException {
            dataOutputStream.writeLong(mId);
            mGeometry.write(dataOutputStream);
        }

        public void read(DataInputStream dataInputStream) throws IOException {
            mId = dataInputStream.readLong();
            mGeometry = GeoGeometryFactory.fromDataStream(dataInputStream);
        }
    }
}
