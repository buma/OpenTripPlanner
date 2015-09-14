package org.opentripplanner.streets;

import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.opentripplanner.util.WorldEnvelope;

import java.io.Serializable;
import java.util.Locale;

/**
 *
 */
public class VertexStore implements Serializable {

    // TODO direct mm_per_fixed_degree conversion, work entirely in mm and fixed degrees.

    public int nVertices = 0;
    public static final double FIXED_FACTOR = 1e7; // we could just reuse the constant from osm-lib Node.
    public TIntList fixedLats;
    public TIntList fixedLons;
    public TLongList osmids;
    public TLongObjectHashMap<String> osmids_names;
    public WorldEnvelope envelope;

    public static final long INVALID_OSM_ID = 0;

    //This is generated osmId of vertices which are not from OSM
    //OSMID is negative value of this number
    private int createdVerticesId = 0;

    public VertexStore (int initialSize) {
        fixedLats = new TIntArrayList(initialSize);
        fixedLons = new TIntArrayList(initialSize);
        osmids = new TLongArrayList(initialSize);
        osmids_names = new TLongObjectHashMap<>(initialSize);
        envelope = new WorldEnvelope();
    }

    /**
     * Add a vertex, specifying its coordinates in double-precision floating point degrees.
     * @return the index of the new vertex.
     */
    public int addVertex(double lat, double lon, long osmNodeId, String name) {
        envelope.expandToInclude(lon, lat);
        return addVertexFixed(floatingDegreesToFixed(lat), floatingDegreesToFixed(lon), osmNodeId,
            name);
    }

    /**
     * Add a vertex, specifying its coordinates in fixed-point lat and lon.
     * @return the index of the new vertex.
     */
    public int addVertexFixed(int fixedLat, int fixedLon, long osmNodeId, String name) {
        int vertexIndex = nVertices++;
        fixedLats.add(fixedLat);
        fixedLons.add(fixedLon);
        if (osmNodeId == INVALID_OSM_ID) {
            //This is for vertices which are created because of split roads
            //We first increase the number because if we insert ID as 0 this means no value for TLongList
            createdVerticesId++;
            osmNodeId = -createdVerticesId;
        }
        osmids.add(osmNodeId);
        if (name != null) {
            name = name.intern();
            osmids_names.put(osmNodeId, name);
        }

        return vertexIndex;
    }

    public class Vertex {

        public int index;

        /** Must call advance() before use, e.g. while (vertex.advance()) {...} */
        public Vertex () {
            this (-1);
        }

        public Vertex (int index) {
            this.index = index;
        }

        /** @return whether this cursor is still within the list (there is a vertex to read). */
        public boolean advance () {
            index += 1;
            return index < nVertices;
        }

        public void seek (int index) {
            this.index = index;
        }

        public void setLat(double lat) {
            fixedLats.set(index, (int)(lat * FIXED_FACTOR));
        }

        public void setLon(double lon) {
            fixedLons.set(index, (int)(lon * FIXED_FACTOR));
        }

        public void setLatLon(double lat, double lon) {
            setLat(lat);
            setLon(lon);
        }

        public double getLat() {
            return fixedLats.get(index) / FIXED_FACTOR;
        }

        public double getLon() {
            return fixedLons.get(index) / FIXED_FACTOR;
        }

        public int getFixedLat() {
            return fixedLats.get(index);
        }

        public int getFixedLon() {
            return fixedLons.get(index);
        }

        public long getOSMID() {
            return osmids.get(index);
        }

        //TODO: add localized name
        public String getName(Locale requestedLocale) {
            String name = osmids_names.get(getOSMID());
            if (name == null) {
                return null;
            } else {
                return name;
            }
        }

        public String getLabel() {
            return "osm:id:" + getOSMID();
        }
    }

    public Vertex getCursor() {
        return new Vertex();
    }

    public Vertex getCursor(int index) {
        return new Vertex(index);
    }

    public static int floatingDegreesToFixed(double degrees) {
        return (int)(degrees * FIXED_FACTOR);
    }

    public static double fixedDegreesToFloating(int fixed) {
        return fixed / FIXED_FACTOR;
    }

}
