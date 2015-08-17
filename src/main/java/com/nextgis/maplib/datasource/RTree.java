/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Based on https://github.com/rweeks/util/blob/master/src/com/newbrightidea/util/RTree.java
 * @see https://github.com/rweeks/util
 * Copyright (c) 2015-2015. NextGIS, info@nextgis.com
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

import com.nextgis.maplib.util.GeoConstants;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Implementation of an arbitrary-dimension RTree. Based on R-Trees: A Dynamic
 * Index Structure for Spatial Searching (Antonn Guttmann, 1984)
 *
 * This class is not thread-safe.
 */
public class RTree {
    public enum SeedPicker { LINEAR, QUADRATIC }

    private final int maxEntries;
    private final int minEntries;

    private final SeedPicker seedPicker;

    private Node root;

    private volatile int size;

    /**
     * Creates a new RTree.
     *
     * @param maxEntries
     *          maximum number of entries per node
     * @param minEntries
     *          minimum number of entries per node (except for the root node)
     */
    public RTree(int maxEntries, int minEntries, SeedPicker seedPicker){
        assert (minEntries <= (maxEntries / 2));
        this.maxEntries = maxEntries;
        this.minEntries = minEntries;
        this.seedPicker = seedPicker;
        root = buildRoot(true);
    }

    public RTree(int maxEntries, int minEntries){
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
    public RTree(){
        this(50, 4, SeedPicker.LINEAR);
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
    public List<Entry> search(GeoEnvelope extent){
        LinkedList<Entry> results = new LinkedList<>();
        search(extent, root, results);
        return results;
    }

    private void search(GeoEnvelope extent, Node n,
                        LinkedList<Entry> results){
        if (n.mLeaf)
        {
            for (Node e : n.mChildren)
            {
                Entry entry = (Entry)e;
                if (entry.intersects(extent))
                {
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
    public boolean delete(long featureId){
        Node l = findLeaf(root, featureId);
        if ( l == null ) {
            return false;
        }

        ListIterator<Node> li = l.mChildren.listIterator();
        boolean result = false;
        while (li.hasNext())
        {
            @SuppressWarnings("unchecked")
            Entry e = (Entry) li.next();
            if (e.mFeatureId == featureId)
            {
                result = true;
                li.remove();
                break;
            }
        }
        if (result)
        {
            condenseTree(l);
            size--;
        }
        if ( size == 0 )
        {
            root = buildRoot(true);
        }
        return result;
    }

    private Node findLeaf(Node n, long featureId){
        if (n.mLeaf)
        {
            for (Node c : n.mChildren)
            {
                if (((Entry) c).mFeatureId == featureId)
                {
                    return n;
                }
            }
            return null;
        }
        else
        {
            for (Node c : n.mChildren)
            {
                Node result = findLeaf(c, featureId);
                if (result != null)
                {
                    return result;
                }
            }
            return null;
        }
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
                    Node c = toVisit.pop();
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
            insert(e.mFeatureId, e.mGeometry, e.mLabel);
        }
        size -= q.size();
    }

    /**
     * Empties the RTree
     */
    public void clear(){
        root = buildRoot(true);
        // let the GC take care of the rest.
    }

    /**
     * Inserts the given entry into the RTree, associated with the given
     * rectangle.
     *
     * @param featureId
     *          the feature identificator
     * @param geometry
     *          the geometry
     * @param label
     *          the label to show on map
     */
    public void insert(long featureId, GeoGeometry geometry, String label){
        Entry e = new Entry(featureId, geometry, label);
        Node l = chooseLeaf(root, e);
        l.mChildren.add(e);
        size++;
        e.mParent = l;
        if (l.mChildren.size() > maxEntries)
        {
            Node[] splits = splitNode(l);
            adjustTree(splits[0], splits[1]);
        }
        else
        {
            adjustTree(l, null);
        }
    }

    private void adjustTree(Node n, Node nn){
        if (n == root){
            if (nn != null){
                // build new root and add children.
                root = buildRoot(false);
                root.mChildren.add(n);
                n.mParent = root;
                root.mChildren.add(nn);
                nn.mParent = root;
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
        Node[] nn = new RTree.Node[]
                { n, new Node(n.mCoords, n.mLeaf) };
        nn[1].mParent = n.mParent;
        if (nn[1].mParent != null){
            nn[1].mParent.mChildren.add(nn[1]);
        }
        LinkedList<Node> cc = new LinkedList<>(n.mChildren);
        n.mChildren.clear();
        Node[] ss = seedPicker == SeedPicker.LINEAR ? lPickSeeds(cc) : qPickSeeds(cc);
        nn[0].mChildren.add(ss[0]);
        nn[1].mChildren.add(ss[1]);
        tighten(nn);
        while (!cc.isEmpty()){
            if ((nn[0].mChildren.size() >= minEntries)
                    && (nn[1].mChildren.size() + cc.size() == minEntries)){
                nn[1].mChildren.addAll(cc);
                cc.clear();
                tighten(nn); // Not sure this is required.
                return nn;
            }
            else if ((nn[1].mChildren.size() >= minEntries)
                    && (nn[0].mChildren.size() + cc.size() == minEntries)){
                nn[0].mChildren.addAll(cc);
                cc.clear();
                tighten(nn); // Not sure this is required.
                return nn;
            }
            Node c = seedPicker == SeedPicker.LINEAR ? lPickNext(cc) : qPickNext(cc, nn);
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
            preferred.mChildren.add(c);
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
        double dimUb = -1.0f * Double.MAX_VALUE, dimMaxLb = -1.0f * Double.MAX_VALUE;
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
            bestPair[0] = nMaxLb;
            bestPair[1] = nMinUb;
            bestSep = sep;
            foundBestPair = true;
        }

        dimLb = Double.MAX_VALUE;
        dimMinUb = Double.MAX_VALUE;
        dimUb = -1.0f * Double.MAX_VALUE;
        dimMaxLb = -1.0f * Double.MAX_VALUE;
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
        return cc.pop();
    }

    private void tighten(Node... nodes){
        assert(nodes.length >= 1): "Pass some nodes to tighten!";
        for (Node n: nodes) {
            assert(n.mChildren.size() > 0) : "tighten() called on empty node!";
            GeoEnvelope minMaxCoords = new GeoEnvelope();

            for (Node c : n.mChildren)
            {
                // we may have bulk-added a bunch of children to a node (eg. in
                // splitNode)
                // so here we just enforce the child->parent relationship.
                c.mParent = n;

                minMaxCoords.merge(c.mCoords);
            }

            n.mCoords.setMin(minMaxCoords.getMinX(), minMaxCoords.getMinY());
            n.mCoords.setMax(minMaxCoords.getMaxX(), minMaxCoords.getMaxY());
        }
    }

    private Node chooseLeaf(Node n, Entry e)
    {
        if (n.mLeaf){
            return n;
        }

        double minInc = Double.MAX_VALUE;
        Node next = null;
        for (Node c : n.mChildren)
        {
            double inc = getRequiredExpansion(c.mCoords, e);
            if (inc < minInc){
                minInc = inc;
                next = c;
            }
            else if (inc == minInc){
                double curArea = next.mCoords.getArea();
                double thisArea = c.mCoords.getArea();
                if (thisArea < curArea)
                {
                    next = c;
                }
            }
        }
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

    private class Node
    {
        protected final GeoEnvelope mCoords;
        protected final LinkedList<Node> mChildren;
        protected final boolean mLeaf;

        protected Node mParent;

        private Node(GeoEnvelope coords, boolean leaf)
        {
            mCoords = coords;
            mLeaf = leaf;
            mChildren = new LinkedList<>();
        }

    }

    private class Entry extends Node
    {
        protected final long mFeatureId;
        protected final GeoGeometry mGeometry;
        protected String mLabel;

        // TODO: 17.08.15 add pyramid geom levels Map<level, GeoGeometry>

        public Entry(long featureId, GeoGeometry geometry, String label)
        {
            super(geometry.getEnvelope(), true);
            mGeometry = geometry;
            mFeatureId = featureId;
            mLabel = label;
        }

        public boolean intersects(GeoEnvelope extent) {
            // TODO: 17.08.15 more intellectual intersect - geometry or geom for specific zoom level
            return mCoords.intersects(extent);
        }
    }
}
