package org.opentripplanner.streets;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * This routes over the street layer of a TransitNetwork.
 * It is a throw-away calculator object that retains routing state and after the search is finished.
 * Additional functions are called to retrieve the routing results from that state.
 */
public class StreetRouter {

    private static final Logger LOG = LoggerFactory.getLogger(StreetRouter.class);

    private static final boolean DEBUG_OUTPUT = false;

    public static final int ALL_VERTICES = -1;

    public final StreetLayer streetLayer;

    public int distanceLimitMeters = 2_000;

    TIntObjectMap<State> bestStates = new TIntObjectHashMap<>();

    BinHeap<State> queue = new BinHeap<>();

    boolean goalDirection = false;

    double targetLat, targetLon; // for goal direction heuristic

    // If you set this to a non-negative number, the search will be directed toward that vertex .
    public int toVertex = ALL_VERTICES;

    public State getLastState() {
        return lastState;
    }

    //Last state when target vertex was found
    private State lastState = null;

    private final TransportNetworkRequest transportNetworkRequest;

    /**
     * @return a map from transit stop indexes to their distances from the origin.
     * Note that the TransitLayer contains all the information about which street vertices are transit stops.
     */
    public TIntIntMap getReachedStops() {
        TIntIntMap result = new TIntIntHashMap();
        // Convert stop vertex indexes in street layer to transit layer stop indexes.
        bestStates.forEachEntry((vertexIndex, state) -> {
            int stopIndex = streetLayer.linkedTransitLayer.stopForStreetVertex.get(vertexIndex);
            // -1 indicates no value, this street vertex is not a transit stop
            if (stopIndex >= 0) {
                result.put(stopIndex, state.weight);
            }
            return true; // continue iteration
        });
        return result;
    }

    public List<Integer> getVisitedEdges() {
        LOG.info("Best states:{}", bestStates.size());

        LinkedList<Integer> edges = new LinkedList<>();
        if (lastState == null) {
            LOG.error("Path not found");
            return edges;
        }
        LinkedList<State> states = new LinkedList<>();

        for (State cur = lastState; cur != null; cur = cur.backState) {
            states.addFirst(cur);
            if (cur.backEdge != -1 && cur.backState != null) {
                edges.addFirst(cur.backEdge);
            }
        }
        return edges;
    }

    /**
     * Get a distance table to all street vertices touched by the last search operation on this StreetRouter.
     * @return A packed list of (vertex, distance) for every reachable street vertex.
     * This is currently returning the weight, which is the distance in meters.
     */
    public int[] getStopTree () {
        TIntList result = new TIntArrayList(bestStates.size() * 2);
        // Convert stop vertex indexes in street layer to transit layer stop indexes.
        bestStates.forEachEntry((vertexIndex, state) -> {
            result.add(vertexIndex);
            result.add(state.weight);
            return true; // continue iteration
        });
        return result.toArray();
    }

    @Deprecated
    public StreetRouter (StreetLayer streetLayer) {
        this.streetLayer = streetLayer;
        this.transportNetworkRequest = new TransportNetworkRequest();
    }

    public StreetRouter(TransportNetworkRequest options) {
        this.streetLayer = options.getTransportContext().transportNetwork.streetLayer;
        this.transportNetworkRequest = options;
    }

    public void setOrigin(Split split, TransportNetworkRequest options) {
        bestStates.clear();
        queue.reset();
        lastState = null;
        State startState0 = new State(split.vertex0, -1, options.getZonedDateTime(), options);
        State startState1 = new State(split.vertex1, -1, options.getZonedDateTime(), options);
        // TODO walk speed, assuming 1 m/sec currently.
        startState0.weight = split.distance0_mm / 1000;
        startState1.weight = split.distance1_mm / 1000;
        bestStates.put(split.vertex0, startState0);
        bestStates.put(split.vertex1, startState1);
        queue.insert(startState0, startState0.weight);
        queue.insert(startState1, startState1.weight);
    }

    /**
     * @param lat Latitude in floating point (not fixed int) degrees.
     * @param lon Longitude in flating point (not fixed int) degrees.
     */
    @Deprecated
    public void setOrigin (double lat, double lon) {
        Split split = streetLayer.findSplit(lat, lon, 300);
        if (split == null) {
            LOG.info("No street was found near the specified origin point of {}, {}.", lat, lon);
            return;
        }
        setOrigin(split, transportNetworkRequest);
    }

    @Deprecated
    public void setOrigin (int fromVertex) {
        bestStates.clear();
        queue.reset();
        lastState = null;
        State startState = new State(fromVertex, -1, transportNetworkRequest.getZonedDateTime(), transportNetworkRequest);
        bestStates.put(fromVertex, startState);
        queue.insert(startState, 0);
    }

    /**
     * Call one of the setOrigin functions first.
     */
    public void route () {

        if (bestStates.size() == 0 || queue.size() == 0) {
            LOG.warn("Routing without first setting an origin, no search will happen.");
        }

        PrintStream printStream; // for debug output
        if (DEBUG_OUTPUT) {
            File debugFile = new File(String.format("street-router-debug.csv"));
            OutputStream outputStream;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(debugFile));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            printStream = new PrintStream(outputStream);
            printStream.println("lat,lon,weight");
        }

        // Set up goal direction if a to vertex was supplied.
        if (toVertex > 0) {
            goalDirection = true;
            VertexStore.Vertex vertex = streetLayer.vertexStore.getCursor(toVertex);
            targetLat = vertex.getLat();
            targetLon = vertex.getLon();
        }

        EdgeStore.Edge edge = streetLayer.edgeStore.getCursor();
        while (!queue.empty()) {
            State s0 = queue.extract_min();
            if (bestStates.get(s0.vertex) != s0) {
                continue; // state was dominated after being enqueued
            }
            int v0 = s0.vertex;
            if (goalDirection && v0 == toVertex) {
                LOG.debug("Found destination vertex. Tree size is {}.", bestStates.size());
                lastState = s0;
                break;
            }
            if (DEBUG_OUTPUT) {
                VertexStore.Vertex vertex = streetLayer.vertexStore.getCursor(v0);
                printStream.printf("%f,%f,%d\n", vertex.getLat(), vertex.getLon(), s0.weight);
            }
            //if arriveBy == true
            TIntList edgeList = null;
            if (transportNetworkRequest.arriveBy) {
                edgeList = streetLayer.incomingEdges.get(v0);
            } else {
                edgeList = streetLayer.outgoingEdges.get(v0);
            }
            edgeList.forEach(edgeIndex -> {
                edge.seek(edgeIndex);
                State s1 = edge.traverse(s0);
                if (s1 == null) {
                    return true;
                }
                if (!goalDirection && s1.weight > distanceLimitMeters) {
                    return true; // Iteration over edges should continue.
                }
                State existingBest = bestStates.get(s1.vertex);
                if (existingBest == null || existingBest.weight > s1.weight) {
                    bestStates.put(s1.vertex, s1);
                }
                int remainingWeight = goalDirection ? heuristic(s1) : 0;
                queue.insert(s1, s1.weight + remainingWeight);
                return true; // Iteration over edges should continue.
            });
        }
        if (DEBUG_OUTPUT) {
            printStream.close();
        }
    }

    /**
     * Estimate remaining weight to destination. Must be an underestimate.
     */
    private int heuristic (State s) {
        VertexStore.Vertex vertex = streetLayer.vertexStore.getCursor(s.vertex);
        double lat = vertex.getLat();
        double lon = vertex.getLon();
        return (int)SphericalDistanceLibrary.fastDistance(lat, lon, targetLat, targetLon);
    }

    public int getTravelTimeToVertex (int vertexIndex) {
        State state = bestStates.get(vertexIndex);
        if (state == null) {
            return Integer.MAX_VALUE; // Unreachable
        }
        return state.weight; // TODO true walk speed
    }

    public static class State implements Cloneable {


        public int vertex;
        public int weight;
        public int backEdge;
        boolean traversingBackward;

        // the current time at this state, in milliseconds UNIX time
        protected Instant time;

        // date time when this search was started in seconds UNIX time
        protected ZonedDateTime startTime;

        private double nonTransitDistance;

        protected TransportNetworkRequest options;

        protected TraverseMode nonTransitMode;

        /**
         * The mode that was used to traverse the backEdge
         */
        protected TraverseMode backMode;

        public State backState; // previous state in the path chain
        public State nextState; // next state at the same location (for turn restrictions and other cases with co-dominant states)

        private boolean backWalkingBike;

        public State(int atVertex, int viaEdge) {
            this.vertex = atVertex;
            this.backEdge = viaEdge;
            this.backState = null;
            this.traversingBackward = false;
            this.nonTransitDistance = 0;
            TraverseModeSet modes = options.modes;
            if (modes.getCar())
                nonTransitMode = TraverseMode.CAR;
            else if (modes.getWalk())
                nonTransitMode = TraverseMode.WALK;
            else if (modes.getBicycle())
                nonTransitMode = TraverseMode.BICYCLE;
            else
                nonTransitMode = null;
        }

        public State(int atVertex, int viaEdge, ZonedDateTime startTime,
            TransportNetworkRequest options) {
            this(atVertex, viaEdge, startTime.toInstant(), startTime, options);
        }

        @Override
        public String toString() {
            return "State{" +
                "weight=" + weight +
                ", time=" + time +
                ", vertex=" + vertex +
                ", backMode=" + backMode +
                ", nonTransitDistance=" + nonTransitDistance +
                '}';
        }

        public State(int origin, int viaEdge, Instant timeSeconds, ZonedDateTime startTime,
            TransportNetworkRequest options) {
            this.weight = 0;
            this.vertex = origin;
            this.backEdge = viaEdge;
            this.time = timeSeconds;
            this.startTime = startTime;
            this.options = options;
            this.traversingBackward = options.arriveBy;
            this.nonTransitDistance = 0;
            TraverseModeSet modes = options.modes;
            if (modes.getCar())
                nonTransitMode = TraverseMode.CAR;
            else if (modes.getWalk())
                nonTransitMode = TraverseMode.WALK;
            else if (modes.getBicycle())
                nonTransitMode = TraverseMode.BICYCLE;
            else
                nonTransitMode = null;
        }

        public int getWeightDelta() {
            return this.weight - backState.weight;
        }

        protected State clone() {
            State ret;
            try {
                ret = (State) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException("This is not happening");
            }
            return ret;
        }

        /**
         * Reverses order of states in arriveBy=true searches. Because start and target are reversed there
         * @param transportNetwork this is used for getting from/to vertex in backEdge
         * @return last edge in reversed order
         */
        public State reverse(TransportNetwork transportNetwork) {
            State orig = this;
            State ret = orig.reversedClone();
            int edge = -1;
            while (orig.backState != null) {
                edge = orig.backEdge;
                State child = ret.clone();
                child.backState = ret;
                child.backEdge = edge;
                EdgeStore.Edge origBackEdge = orig.getBackEdge(transportNetwork);
                if (origBackEdge.getFromVertex() == origBackEdge.getToVertex()
                    && ret.vertex == origBackEdge.getFromVertex()) {
                    traversingBackward = ret.getOptions().arriveBy;
                    child.vertex = origBackEdge.getToVertex();
                } else if (ret.vertex == origBackEdge.getFromVertex()) {
                    child.vertex = origBackEdge.getToVertex();
                    traversingBackward = false;
                }else if (ret.vertex == origBackEdge.getToVertex()) {
                    child.vertex = origBackEdge.getFromVertex();
                    traversingBackward = true;
                }
                if (traversingBackward != ret.getOptions().arriveBy) {
                    LOG.error("Actual traversal direction does not match traversal direction in TraverseOptions.");
                    //defectiveTraversal = true;
                }
                child.incrementWeight(orig.getWeightDelta());
                child.incrementTimeInSeconds(orig.getAbsTimeDeltaSeconds());
                child.incrementNonTransitDistance(orig.getNonTransitDistance());
                child.setBackMode(orig.getBackMode());
                ret = child;
                orig = orig.backState;
            }
            return ret;
        }

        public State reversedClone() {
            State newState = new State(this.vertex, -1, time, startTime, options.reversedClone());
            return newState;
        }

        public Instant getTime() {
            return time;
        }

        //TODO: are all vertices in Street vertex store? What about transit stops?
        public VertexStore.Vertex getVertex(TransportNetwork transportNetwork) {
            return transportNetwork.streetLayer.vertexStore.getCursor(vertex);
        }

        //FIXME: this doesn't work correctly yet (it is usually 0)
        public long getElapsedTimeSeconds() {
            return Duration.between(startTime.toInstant(), time).abs().getSeconds();
        }

        public EdgeStore.Edge getBackEdge(TransportNetwork transportNetwork) {
            return transportNetwork.streetLayer.edgeStore.getCursor(backEdge);
        }

        public TransportNetworkRequest getOptions() {
            return options;
        }

        public long getAbsTimeDeltaSeconds() {
            return Math.abs(getTimeDeltaSeconds());
        }

        public long getTimeDeltaSeconds() {
            if (backState != null) {
                return Duration.between(backState.time, time).getSeconds();
            }
            return 0;
        }

        public double getNonTransitDistance() {
            if (backState != null) {
                return Math.abs(this.nonTransitDistance - backState.nonTransitDistance);
            } else {
                return 0.0;
            }
        }

        public void incrementTimeInSeconds(long seconds) {
            if (seconds < 0) {
                LOG.warn("A state's time is being incremented by a negative amount while traversing edge "
                    );
                //defectiveTraversal = true;
                return;
            }
            if (traversingBackward) {
                time = time.minusSeconds(seconds);
            } else {
                time = time.plusSeconds(seconds);
            }
        }

        public void incrementNonTransitDistance(double length) {
            if (length < 0) {
                LOG.warn("A state's non transit distance is being incremented by a negative amount.");
                //defectiveTraversal = true;
                return;
            }
            nonTransitDistance += length;
        }

        public TraverseMode getNonTransitMode() {
            return nonTransitMode;
        }

        public TraverseMode getBackMode() {
            return backMode;
        }

        public void setBackMode(TraverseMode backMode) {
            this.backMode = backMode;
        }

        public void incrementWeight(double weight) {
            if (Double.isNaN(weight)) {
                LOG.warn("A state's weight is being incremented by NaN while traversing edge "
                    + backEdge);
                //defectiveTraversal = true;
                return;
            }
            if (weight < 0) {
                LOG.warn("A state's weight is being incremented by a negative amount while traversing edge "
                    + backEdge);
                //defectiveTraversal = true;
                return;
            }
            this.weight += weight;
        }

        public boolean isBackWalkingBike() {
            return backWalkingBike;
        }

        public void setBackWalkingBike(boolean backWalkingBike) {
            this.backWalkingBike = backWalkingBike;
        }
    }

}
