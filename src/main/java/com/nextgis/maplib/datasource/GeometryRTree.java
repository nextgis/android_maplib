/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Based on https://github.com/rweeks/util/blob/master/src/com/newbrightidea/util/RTree.java
 * @see https://github.com/rweeks/util
 * Copyright (c) 2015-2016 NextGIS, info@nextgis.com
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

package com.nextgis.maplib.datasource;

import com.nextgis.maplib.api.IGeometryCache;
import com.nextgis.maplib.api.IGeometryCacheItem;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Implementation of an arbitrary-dimension RTree. Based on R-Trees: A Dynamic
 * Index Structure for Spatial Searching (Antonn Guttmann, 1984)
 *
 * This class is not thread-safe.
 */
public class GeometryRTree implements IGeometryCache {

    public enum SeedPicker { LINEAR, QUADRATIC }
    private final SeedPicker seedPicker;

    private int maxEntries;
    private int minEntries;
    private Node root;

    private volatile int size;

    protected File mPath;
    protected boolean mHasEdits;

    /**
     * Creates a new RTree.
     *
     * @param maxEntries
     *          maximum number of entries per node
     * @param minEntries
     *          minimum number of entries per node (except for the root node)
     */
    public GeometryRTree(int maxEntries, int minEntries, SeedPicker seedPicker){
        if (minEntries > (maxEntries / 2)) throw new AssertionError();
        this.maxEntries = maxEntries;
        this.minEntries = minEntries;
        this.seedPicker = seedPicker;
        root = buildRoot(true);
        mHasEdits = false;
    }

    public GeometryRTree(int maxEntries, int minEntries){
        this(maxEntries, minEntries, SeedPicker.LINEAR);
    }

    private Node buildRoot(boolean asLeaf){
        return new Node(new GeoEnvelope(-GeoConstants.MERCATOR_MAX, GeoConstants.MERCATOR_MAX,
                                        -GeoConstants.MERCATOR_MAX, GeoConstants.MERCATOR_MAX),
                        asLeaf);
    }

    /**
     * Builds a new RTree using default parameters: maximum 50 entries per node
     * minimum 2 entries per node
     */
    public GeometryRTree(){
        this(8, 2, SeedPicker.QUADRATIC);
    }

    /**
     * @return the maximum number of entries per node
     */
    public int getMaxEntries()
    {
        return maxEntries;
    }

    /**
     * @return the minimum number of entries per node for all nodes except the
     *         root.
     */
    public int getMinEntries()
    {
        return minEntries;
    }

    @Override
    public boolean isItemExist(long featureId) {
        return isItemExist(featureId, root);
    }

    public boolean isItemExist(long featureId, Node n) {
        if (n.mLeaf)
        {
            for (Node e : n.mChildren){
                Entry entry = (Entry)e;
                if (entry.getFeatureId() == featureId){
                    return true;
                }
            }
        }
        else{
            for (Node c : n.mChildren)
            {
                if (isItemExist(featureId, c))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public IGeometryCacheItem addItem(long id, GeoEnvelope envelope) {
        mHasEdits = true;
        return insert(id, envelope);
    }

    @Override
    public IGeometryCacheItem getItem(long featureId) {
        return getItem(featureId, root);
    }

    @Override
    public void changeId(long oldFeatureId, long newFeatureId) {
        IGeometryCacheItem item = getItem(oldFeatureId);
        if(null != item)
            item.setFeatureId(newFeatureId);
    }

    @Override
    public synchronized void save(File path) {

        boolean isSameFile = null != mPath && mPath.equals(path);

        if(isSameFile && !mHasEdits)
            return;

        try {
            FileUtil.createDir(path.getParentFile());
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

            dataOutputStream.writeInt(maxEntries);
            dataOutputStream.writeInt(minEntries);
            dataOutputStream.writeInt(size);

            root.write(dataOutputStream);

            dataOutputStream.flush();
            dataOutputStream.close();
            fileOutputStream.close();

            mHasEdits = false;
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void load(File path) {
        clear();

        if (!path.exists()) {
            return;
        }

        mPath = path;

        try {
            FileInputStream fileInputStream = new FileInputStream(path);
            DataInputStream dataInputStream = new DataInputStream(fileInputStream);

            maxEntries = dataInputStream.readInt();
            minEntries = dataInputStream.readInt();
            size = dataInputStream.readInt();

            dataInputStream.readBoolean();
            root = new Node();
            root.read(dataInputStream);

            dataInputStream.close();
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public IGeometryCacheItem getItem(long featureId, Node n) {
        if (n.mLeaf){
            for (Node e : n.mChildren){
                if (e instanceof Entry) {
                    Entry entry = (Entry)e;
                    if (entry.getFeatureId() == featureId){
                        return entry;
                    }
                }
            }
        }
        else{
            for (Node c : n.mChildren)
            {
                IGeometryCacheItem entry = getItem(featureId, c);
                if (null != entry){
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * @return the number of items in this tree.
     */
    public int size(){
        return size;
    }

    /**
     * Searches the RTree for objects overlapping with the given rectangle.
     *
     * @param extent
     *          the envelope to search
     * @return a list of objects whose envelopes overlap with the given
     *         envelope.
     */
    @Override
    public synchronized List<IGeometryCacheItem> search(GeoEnvelope extent){
        LinkedList<IGeometryCacheItem> results = new LinkedList<>();
        search(extent, root, results);
        return results;
    }

    @Override
    public synchronized List<IGeometryCacheItem> getAll() {
        LinkedList<IGeometryCacheItem> result = new LinkedList<>();
        getAll(root, result);
        return result;
    }

    protected void getAll(Node n, LinkedList<IGeometryCacheItem> results){
        if (n.mLeaf){
            for (Node e : n.mChildren){
                Entry entry = (Entry)e;
                results.add(entry);
            }
        }
        else{
            for (Node c : n.mChildren){
                getAll(c, results);
            }
        }
    }

    private void search(GeoEnvelope extent, Node n,
                        LinkedList<IGeometryCacheItem> results){
        if (n.mLeaf)
        {
            for (Node e : n.mChildren)
            {
                if (e.mCoords.intersects(extent) && e instanceof Entry) {
                    Entry entry = (Entry)e;
                    results.add(entry);
                }
            }
        }
        else
        {
            for (Node c : n.mChildren)
            {
                if (c.mCoords.intersects(extent))
                {
                    search(extent, c, results);
                }
            }
        }
    }

    /**
     * Deletes the entry associated with the given rectangle from the RTree
     *
     * @param featureId
     *          the feature id to delete
     * @return true if the entry was deleted from the RTree.
     */
    @Override
    public IGeometryCacheItem removeItem(long featureId){
        Node l = findLeaf(root, featureId);
        if ( l == null ) {
            return null;
        }

        mHasEdits = true;

        condenseTree(l);
            size--;

        if ( size == 0 ){
            root = buildRoot(true);
        }

        if(l instanceof IGeometryCache)
            return (IGeometryCacheItem) l;
        return null;
    }

    private Node findLeaf(Node n, long featureId){
        if (n.mLeaf){
            for (Node c : n.mChildren){
                if (((Entry) c).mFeatureId == featureId)
                {
                    return c;
                }
            }
        }
        else{
            for (Node c : n.mChildren){
                Node result = findLeaf(c, featureId);
                if (result != null)
                {
                    return result;
                }
            }
        }
        return null;
    }

    private void condenseTree(Node n){
        Set<Node> q = new HashSet<>();
        while (n != root){
            if (n.mLeaf && (n.mChildren.size() < minEntries)){
                q.addAll(n.mChildren);
                n.mParent.mChildren.remove(n);
            }
            else if (!n.mLeaf && (n.mChildren.size() < minEntries)){
                // probably a more efficient way to do this...
                LinkedList<Node> toVisit = new LinkedList<>(n.mChildren);
                while (!toVisit.isEmpty()){
                    Node c = toVisit.removeFirst();
                    if (c.mLeaf){
                        q.addAll(c.mChildren);

                    }
                    else{
                        toVisit.addAll(c.mChildren);
                    }
                }
                n.mParent.mChildren.remove(n);
            }
            else{
                tighten(n);
            }
            n = n.mParent;
        }
        if ( root.mChildren.isEmpty() ){
            root = buildRoot(true);
        }
        else if ( (root.mChildren.size() == 1) && (!root.mLeaf) ){
            root = root.mChildren.get(0);
            root.mParent = null;
        }
        else{
            tighten(root);
        }

        for (Node ne : q){
            @SuppressWarnings("unchecked")
            Entry e = (Entry) ne;
            insert(e.mFeatureId, e.mCoords);
        }
        size -= q.size();
    }

    /**
     * Empties the RTree
     */
    public void clear(){
        root = buildRoot(true);
        mHasEdits = false;
        // let the GC take care of the rest.
    }

    /**
     * Inserts the given entry into the RTree, associated with the given
     * rectangle.
     *
     * @param featureId
     *          a feature identificator
     * @param envelope
     *          an envelope
     */
    public IGeometryCacheItem insert(long featureId, GeoEnvelope envelope){
        Entry e = new Entry(featureId, envelope);
        Node l = chooseLeaf(root, e);
        if(l == null)
            l = root;

        l.add(e);
        size++;
        if (l.mChildren.size() > maxEntries){
            Node[] splits = splitNode(l);
            adjustTree(splits[0], splits[1]);
        }
        else{
            adjustTree(l, null);
        }

        return e;
    }

    private void adjustTree(Node n, Node nn){
        if (n == root){
            if (nn != null){
                // build new root and add children.
                root = buildRoot(false);
                root.add(n);
                root.add(nn);
            }
            tighten(root);
            return;
        }
        tighten(n);
        if (nn != null){
            tighten(nn);
            if (n.mParent.mChildren.size() > maxEntries){
                Node[] splits = splitNode(n.mParent);
                adjustTree(splits[0], splits[1]);
            }
        }
        if (n.mParent != null){
            adjustTree(n.mParent, null);
        }
    }

    private Node[] splitNode(Node n){
        // TODO: this class probably calls "tighten" a little too often.
        // For instance the call at the end of the "while (!cc.isEmpty())" loop
        // could be modified and inlined because it's only adjusting for the addition
        // of a single node.  Left as-is for now for readability.
        @SuppressWarnings("unchecked")
        Node[] nn = new GeometryRTree.Node[]{ n, new Node(n.mCoords, n.mLeaf) };
        nn[1].mParent = n.mParent;
        if (nn[1].mParent != null){
            nn[1].mParent.add(nn[1]);
        }
        LinkedList<Node> cc = new LinkedList<>(n.mChildren);
        n.mChildren.clear();
        Node[] ss = seedPicker == SeedPicker.LINEAR ? lPickSeeds(cc) : qPickSeeds(cc);
        nn[0].add(ss[0]);
        nn[1].add(ss[1]);
        tighten(nn);
        while (!cc.isEmpty()){
            if ((nn[0].mChildren.size() >= minEntries) && (nn[1].mChildren.size() + cc.size() == minEntries)){
                nn[1].addAll(cc);
                cc.clear();
                tighten(nn); // Not sure this is required.
                return nn;
            }
            else if ((nn[1].mChildren.size() >= minEntries) && (nn[0].mChildren.size() + cc.size() == minEntries)){
                nn[0].addAll(cc);
                cc.clear();
                tighten(nn); // Not sure this is required.
                return nn;
            }

            Node c = seedPicker == SeedPicker.LINEAR ? lPickNext(cc) : qPickNext(cc, nn);
            if (c == null)
                return nn;

            Node preferred;
            double e0 = getRequiredExpansion(nn[0].mCoords, c);
            double e1 = getRequiredExpansion(nn[1].mCoords, c);
            if (e0 < e1){
                preferred = nn[0];
            }
            else if (e0 > e1){
                preferred = nn[1];
            }
            else{
                double a0 = nn[0].mCoords.getArea();
                double a1 = nn[1].mCoords.getArea();
                if (a0 < a1){
                    preferred = nn[0];
                }
                else if (e0 > a1){
                    preferred = nn[1];
                }
                else{
                    if (nn[0].mChildren.size() < nn[1].mChildren.size()){
                        preferred = nn[0];
                    }
                    else if (nn[0].mChildren.size() > nn[1].mChildren.size()){
                        preferred = nn[1];
                    }
                    else{
                        preferred = nn[(int) Math.round(Math.random())];
                    }
                }
            }
            preferred.add(c);
            tighten(preferred);
        }
        return nn;
    }

    // Implementation of Quadratic PickSeeds
    private Node[] qPickSeeds(LinkedList<Node> nn){
        @SuppressWarnings("unchecked")
        Node[] bestPair = new Node[2];
        double maxWaste = -1.0f * Double.MAX_VALUE;
        for (Node n1: nn){
            for (Node n2: nn){
                if (n1 == n2)
                    continue;
                double n1a = n1.mCoords.getArea();
                double n2a = n2.mCoords.getArea();
                double ja = 1.0f;
                double jc0 = Math.min(n1.mCoords.getMinX(), n2.mCoords.getMinX());
                double jc1 = Math.max(n1.mCoords.getMaxX(), n2.mCoords.getMaxX());
                ja *= (jc1 - jc0);
                jc0 = Math.min(n1.mCoords.getMinY(), n2.mCoords.getMinY());
                jc1 = Math.max(n1.mCoords.getMaxY(), n2.mCoords.getMaxY());
                ja *= (jc1 - jc0);

                double waste = ja - n1a - n2a;
                if ( waste > maxWaste )
                {
                    maxWaste = waste;
                    bestPair[0] = n1;
                    bestPair[1] = n2;
                }
            }
        }
        nn.remove(bestPair[0]);
        nn.remove(bestPair[1]);
        return bestPair;
    }

    /**
     * Implementation of QuadraticPickNext
     * @param cc the children to be divided between the new nodes, one item will be removed from this list.
     * @param nn the candidate nodes for the children to be added to.
     */
    private Node qPickNext(LinkedList<Node> cc, Node[] nn){
        double maxDiff = -1.0f * Double.MAX_VALUE;
        Node nextC = null;
        for ( Node c: cc )
        {
            double n0Exp = getRequiredExpansion(nn[0].mCoords, c);
            double n1Exp = getRequiredExpansion(nn[1].mCoords, c);
            double diff = Math.abs(n1Exp - n0Exp);
            if (diff > maxDiff){
                maxDiff = diff;
                nextC = c;
            }
        }
        assert (nextC != null) : "No node selected from qPickNext";
        cc.remove(nextC);
        return nextC;
    }

    // Implementation of LinearPickSeeds
    private Node[] lPickSeeds(LinkedList<Node> nn){
        @SuppressWarnings("unchecked")
        Node[] bestPair = new Node[2];
        boolean foundBestPair = false;
        double bestSep = 0.0f;

        double dimLb = Double.MAX_VALUE, dimMinUb = Double.MAX_VALUE;
        double dimUb = Double.MIN_VALUE, dimMaxLb = Double.MIN_VALUE;
        Node nMaxLb = null, nMinUb = null;
        for (Node n : nn)
        {
            if (n.mCoords.getMinX() < dimLb)
            {
                dimLb = n.mCoords.getMinX();
            }
            if (n.mCoords.getMaxX() > dimUb)
            {
                dimUb = n.mCoords.getMaxX();
            }
            if (n.mCoords.getMinX() > dimMaxLb)
            {
                dimMaxLb = n.mCoords.getMinX();
                nMaxLb = n;
            }
            if (n.mCoords.getMaxX() < dimMinUb)
            {
                dimMinUb = n.mCoords.getMaxX();
                nMinUb = n;
            }


        }
        double sep = (nMaxLb == nMinUb) ? -1.0f :
                Math.abs((dimMinUb - dimMaxLb) / (dimUb - dimLb));
        if (sep >= bestSep)
        {
            // FIXME
            // nMaxLb/nMinLb is null if dimMaxLb/dimMinUb is not set
            bestPair[0] = nMaxLb;
            bestPair[1] = nMinUb;
            bestSep = sep;
            foundBestPair = true;
        }

        dimLb = Double.MAX_VALUE;
        dimMinUb = Double.MAX_VALUE;
        dimUb = Double.MIN_VALUE;
        dimMaxLb = Double.MIN_VALUE;
        nMaxLb = null;
        nMinUb = null;

        for (Node n : nn)
        {
            if (n.mCoords.getMinY() < dimLb)
            {
                dimLb = n.mCoords.getMinY();
            }
            if (n.mCoords.getMaxY() > dimUb)
            {
                dimUb = n.mCoords.getMaxY();
            }
            if (n.mCoords.getMinY() > dimMaxLb)
            {
                dimMaxLb = n.mCoords.getMinY();
                nMaxLb = n;
            }
            if (n.mCoords.getMaxY() < dimMinUb)
            {
                dimMinUb = n.mCoords.getMaxY();
                nMinUb = n;
            }

        }
        sep = (nMaxLb == nMinUb) ? -1.0f :
                Math.abs((dimMinUb - dimMaxLb) / (dimUb - dimLb));
        if (sep >= bestSep)
        {
            bestPair[0] = nMaxLb;
            bestPair[1] = nMinUb;
            foundBestPair = true;
        }

        // In the degenerate case where all points are the same, the above
        // algorithm does not find a best pair.  Just pick the first 2
        // children.
        if ( !foundBestPair ){
            bestPair = new Node[] { nn.get(0), nn.get(1) };
        }
        nn.remove(bestPair[0]);
        nn.remove(bestPair[1]);
        return bestPair;
    }

    /**
     * Implementation of LinearPickNext
     * @param cc the children to be divided between the new nodes, one item will be removed from this list.
     */
    private Node lPickNext(LinkedList<Node> cc){
        return cc.removeFirst();
    }

    private void tighten(Node... nodes){
        if (nodes.length < 1) throw new AssertionError("Pass some nodes to tighten!");
        for (Node n: nodes) {
            if (n.mChildren.size() <= 0) throw new AssertionError("tighten() called on empty node!");
            n.mCoords.unInit();

            for (Node c : n.mChildren)
            {
                // we may have bulk-added a bunch of children to a node (eg. in
                // splitNode)
                // so here we just enforce the child->parent relationship.
                //c.mParent = n;

                n.mCoords.merge(c.mCoords);
            }
        }
    }

    private Node chooseLeaf(Node n, Entry e)
    {
        if (n == null)
            return null;

        if (n.mLeaf)
            return n;

        double minInc = Double.MAX_VALUE;
        Node next = null;
        for (Node c : n.mChildren) {
            double inc = getRequiredExpansion(c.mCoords, e);
            if (inc < minInc) {
                minInc = inc;
                next = c;
            } else if (inc == minInc) {
                if (next == null)
                    continue;

                double curArea = next.mCoords.getArea();
                double thisArea = c.mCoords.getArea();
                if (thisArea < curArea) {
                    next = c;
                }
            }
        }

        if (next == null)
            return n;

        return chooseLeaf(next, e);
    }

    /**
     * Returns the increase in area necessary for the given rectangle to cover the
     * given entry.
     */
    private double getRequiredExpansion(GeoEnvelope envelope, Node e)
    {
        double area = envelope.getArea();
        double deltaX = 0.0, deltaY = 0.0;
        if (envelope.mMaxX < e.mCoords.mMaxX){
            deltaX = e.mCoords.mMaxX - envelope.mMaxX;
        }
        else if (envelope.mMaxX > e.mCoords.mMaxX){
            deltaX = envelope.mMinX - e.mCoords.mMinX;
        }

        if (envelope.mMaxY < e.mCoords.mMaxY){
            deltaY = e.mCoords.mMaxY - envelope.mMaxY;
        }
        else if (envelope.mMaxY > e.mCoords.mMaxY){
            deltaY = envelope.mMinY - e.mCoords.mMinY;
        }

        double expanded = (envelope.width() + deltaX) * (envelope.height() + deltaY);
        return (expanded - area);
    }

    protected class Node
    {
        protected GeoEnvelope mCoords;
        protected LinkedList<Node> mChildren;
        protected boolean mLeaf;

        protected Node mParent;

        protected Node(){
            mCoords = new GeoEnvelope();
            mChildren = new LinkedList<>();
        }

        public int size(){
            return mChildren.size();
        }

        public void add(Node node){
            mChildren.add(node);
            node.setParent(this);
        }

        public void addAll(LinkedList<Node> children){
            for (Node node : children){
                add(node);
            }
        }

        protected Node(GeoEnvelope coords, boolean leaf)
        {
            mCoords = new GeoEnvelope(coords);
            mLeaf = leaf;
            mChildren = new LinkedList<>();
        }

        public void write(DataOutputStream stream) throws IOException {
            stream.writeBoolean(isNode());
            stream.writeBoolean(mLeaf);
            stream.writeDouble(mCoords.getMinX());
            stream.writeDouble(mCoords.getMinY());
            stream.writeDouble(mCoords.getMaxX());
            stream.writeDouble(mCoords.getMaxY());
            stream.writeInt(mChildren.size());
            for(Node node : mChildren){
                node.write(stream);
            }
        }

        public void read(DataInputStream stream) throws IOException {
            mLeaf = stream.readBoolean();
            double minX = stream.readDouble();
            double minY = stream.readDouble();
            double maxX = stream.readDouble();
            double maxY = stream.readDouble();
            mCoords.setMin(minX, minY);
            mCoords.setMax(maxX, maxY);
            int size = stream.readInt();
            for(int i = 0; i < size; i++){
                if(stream.readBoolean()){
                    Node childNode = new Node();
                    childNode.read(stream);
                    add(childNode);
                }
                else {
                    Entry childEntry = new Entry();
                    childEntry.read(stream);
                    add(childEntry);
                }
            }
        }

        protected boolean isNode(){ return true; }

        public void setParent(Node parent) {
            mParent = parent;
        }
    }

    protected class Entry extends Node implements IGeometryCacheItem
    {
        protected long mFeatureId;

        protected Entry()
        {
            super();
        }

        protected Entry(long featureId, GeoEnvelope envelope)
        {
            super(envelope, true);
            mFeatureId = featureId;
        }

        public boolean intersects(GeoEnvelope extent) {
            return mCoords.intersects(extent);
        }

        @Override
        public GeoEnvelope getEnvelope() {
            return mCoords;
        }

        @Override
        public long getFeatureId() {
            return mFeatureId;
        }

        @Override
        public void setFeatureId(long id) {
            mFeatureId = id;
        }

        @Override
        public void read(DataInputStream stream) throws IOException {
            super.read(stream);
            mFeatureId = stream.readLong();
        }

        @Override
        public void write(DataOutputStream stream) throws IOException {
            super.write(stream);
            stream.writeLong(mFeatureId);
        }

        @Override
        protected boolean isNode() {
            return false;
        }
    }
}
