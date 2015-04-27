/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.map;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.math3.util.FastMath;
import org.geotools.math.Statistics;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.annotation.StopUnlinked;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.edgetype.loader.LinkRequest;
import org.opentripplanner.routing.edgetype.loader.NetworkLinkerLibrary;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.CandidateEdge;
import org.opentripplanner.routing.impl.CandidateEdgeBundle;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.GeometryCSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by mabu on 8.4.2015.
 */
public class TransitToStreetNetworkLinkerModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory
        .getLogger(TransitToStreetNetworkLinkerModule.class);

    private final double DISTANCE_THRESHOLD = SphericalDistanceLibrary.metersToDegrees(22);

    private SpatialIndex index;

    private Graph graph;

    private GeometryCSVWriter writerStopShapesGeo;

    private GeometryCSVWriter writerTransitStop;

    private GeometryCSVWriter writerMatchedStreets;

    private GeometryCSVWriter writerClosestPoints;

    private GeometryCSVWriter writerAzimuth;

    private GeometryCSVWriter writerPoint;

    private GeometryCSVWriter writerPoly;

    private GeometryCSVWriter writerParalelRoads;

    private GeometryCSVWriter writerPointSV;

    private GeometryCSVWriter writerCe;

    private GeometryCSVWriter writeMNS;

    private Multiset<Integer> counts;

    private DistanceStatistics statsMinDistance;

    private DistanceStatistics statsMinDistance1;

    private StreetMatcher streetMatcher;

    private int numOfStopsEdgesNotMatchedToShapes;

    private NetworkLinkerLibrary networkLinkerLibrary;

    private static final TraverseModeSet cycling = new TraverseModeSet(TraverseMode.BICYCLE);

    private static final TraverseModeSet walking = new TraverseModeSet(TraverseMode.WALK);

    private static final TraverseModeSet driving = new TraverseModeSet(TraverseMode.CAR);

    private static final double ANGLE_DIFF = Math.PI / 18; // 10 degrees

    STRtree createIndex() {
        STRtree edgeIndex = new STRtree();
        for (Vertex v : graph.getVertices()) {
            for (Edge e : v.getOutgoing()) {
                if (e instanceof StreetEdge || e instanceof PublicTransitEdge) {
                    Envelope envelope;
                    Geometry geometry = e.getGeometry();
                    envelope = geometry.getEnvelopeInternal();
                    edgeIndex.insert(envelope, e);
                }
            }
        }
        edgeIndex.build();
        LOG.debug("Created index");
        return edgeIndex;
    }

    public List<String> provides() {
        return Arrays.asList("street to transit", "linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets"); // why not "transit" ?
    }

    /**
     * Gets which vertex is closer to the start shape vertex
     * <p/>
     * This vertex needs to be extended to see if better matching can be found
     *
     * @param shape         Part of GTFS shape from shape_dist_traveled +4 vertices
     * @param closestStreet found closest street with {@link StreetMatcher}
     * @return True if it is from vertex in closestStreet false otherwise
     */
    //private boolean getClosestPoint(LineString shape, Edge closestStreet) {
    private boolean getClosestPoint(LineString shape, Geometry closestStreetGeometry) {
        Coordinate firstC, lastC;
        if (closestStreetGeometry instanceof LineString) {
            firstC = ((LineString) closestStreetGeometry).getCoordinateN(0);
            lastC = ((LineString) closestStreetGeometry)
                .getCoordinateN(closestStreetGeometry.getNumPoints() - 1);
        } else if (closestStreetGeometry instanceof MultiLineString) {
            firstC = ((LineString) closestStreetGeometry.getGeometryN(0)).getCoordinateN(0);
            LineString lastGeometry = (LineString) closestStreetGeometry
                .getGeometryN(closestStreetGeometry.getNumGeometries() - 1);
            lastC = lastGeometry.getCoordinateN(lastGeometry.getNumPoints() - 1);
        } else {

            //TODO: exception
            return false;
        }
        //First vertex from shape is always checked since this should be vertex
        // which should be closest since from this point on transit geometry will be drawn.
        //LineString closestStreetGeometry = closestStreet.getGeometry();
        Coordinate a0 = null, cur_a;
        Coordinate b0 = null, cur_b;
        boolean fromVertex;
        double distance = Double.MAX_VALUE;
        //A0 vs B0
        cur_a = shape.getCoordinateN(0);
        cur_b = firstC;
        fromVertex = true;
        double cur_distance = cur_a.distance(cur_b);
        if (cur_distance < distance) {
            a0 = cur_a;
            b0 = cur_b;
            distance = cur_distance;
        }

        //A0 vs BLast
        cur_b = lastC;
        cur_distance = cur_a.distance(cur_b);
        if (cur_distance < distance) {
            a0 = cur_a;
            b0 = cur_b;
            fromVertex = false;
            distance = cur_distance;
        }

        //We only need to check first shape vertices. because this is where shape starts
        /*
        //ALast vs BLast
        cur_a = shape.getCoordinateN(shape.getNumPoints() - 1);
        cur_distance = cur_a.distance(cur_b);
        if (cur_distance < distance) {
            a0 = cur_a;
            b0 = cur_b;
            fromVertex = false;
            distance = cur_distance;
        }

        //Alast vs B0
        cur_b = closestStreetGeometry.getCoordinateN(0);
        cur_distance = cur_a.distance(cur_b);
        if (cur_distance < distance) {
            a0 = cur_a;
            b0 = cur_b;
            fromVertex = true;
            distance = cur_distance;
        }
        */

        return fromVertex;
    }

    /**
     * Returns list of edges and points which are closest to GTFS shapes. This can be {@link StreetEdge} where bus drives
     * or {@link PublicTransitEdge} where TRAM/RAIL/subway drives. And a point which is a point where would edge
     * is closest to transitStop according to GTFS.
     * <p/>
     * List is returned because some stops are connected to multiple bus routes or to different transit types. (Bus/tram most often)
     *
     * @param transitStop
     * @return
     */
    private List<EdgePoint> getTransitEdges(TransitStop transitStop) {
        int geometries_count = transitStop.geometries.size();
        List<EdgePoint> closestEdges = new ArrayList<>(geometries_count);

        if (geometries_count != 1) {
            // System.out.println("Stop ID: " + transitStop.getLabel() + "(" + transitStop.getName() + "|" + geometries_count + ")");
        }
        writerTransitStop.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(),
            Integer.toString(geometries_count), transitStop.getModes().toString()),
            transitStop.getCoordinate());

        counts.add(geometries_count);

        //All geometries near stop
        for (T2<LineString, TraverseMode> GTFSShape : transitStop.geometries) {
            if (geometries_count != 1) {
                // System.out.println("  shape:" + GTFSShape.first.toString() + "[" + GTFSShape.first.hashCode() + "]||" + GTFSShape.second.toString());
            }
            writerStopShapesGeo.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(),
                    transitStop.getLabel() + " " + GTFSShape.second.toString(),
                    Integer.toString(geometries_count), Double.toString(FastMath.toDegrees(Angle
                            .normalizePositive(DirectionUtils.getAzimuthRad(GTFSShape.first))))),
                GTFSShape.first);
            List<Edge> edges = streetMatcher.match(GTFSShape.first, GTFSShape.second);
            if (edges == null) {
                numOfStopsEdgesNotMatchedToShapes++;
                continue;
            }
            transitStop.addLevel(((EdgeInfo) edges.get(0)).getLevel());
            LineString[] lss = new LineString[edges.size()];
            int edge_idx = 0;
            TLongSet edge_ids = new TLongHashSet(5);
            double minDistance = Double.MAX_VALUE;
            for (Edge edge : edges) {
                //TODO: minDistance should be calculated between projection of STOP to found? This means that broken GTFS shapes isn't such a big problem
                minDistance = Math.min(minDistance, SphericalDistanceLibrary
                    .fastDistance(GTFSShape.first.getCoordinateN(0), edge.getGeometry()));
                lss[edge_idx] = edge.getGeometry();
                edge_idx++;
                if (edge instanceof EdgeInfo) {
                    edge_ids.add(((EdgeInfo) edge).getOsmID());
                }
            }
            /*if (minDistance <= 20) {
                statsMinDistance.add(minDistance);
            }*/
            String edge_ids_show;
            if (!edge_ids.isEmpty()) {
                edge_ids_show = "";
                for (long edge_id : edge_ids.toArray()) {
                    edge_ids_show += Long.toString(edge_id) + "|";
                }
            } else {
                edge_ids_show = "??";
            }
            //LineMerger lineMerger = new LineMerger();
            //lineMerger.add(edges);
            LineString foundEdgeGeometry = edges.get(0)
                .getGeometry(); //(LineString) lineMerger.getMergedLineStrings().iterator().next();
            MultiLineString multiLineString = GeometryUtils.getGeometryFactory()
                .createMultiLineString(lss);

            boolean isFromVertexClosest = getClosestPoint(GTFSShape.first, multiLineString);

            Edge cur_edge = null;
            double prevMinDistance = minDistance;
            if (minDistance <= 20) {

                //This tries to lengthen start or end of found edge to try to find even better matching
                //because sometimes point where we should link is right after ending of edge or right before it starts.
                if (isFromVertexClosest) {
                    for (Edge edge : edges.get(0).getFromVertex().getIncoming()) {
                        if (edge instanceof EdgeInfo) {
                            double cur_distance = SphericalDistanceLibrary
                                .fastDistance(GTFSShape.first.getCoordinateN(0),
                                    edge.getGeometry());
                            if (cur_distance < minDistance) {
                                minDistance = cur_distance;
                                cur_edge = edge;
                            }
                        }
                    }
                    if (cur_edge != null) {
                        LOG.info("Found edge closer in fromVertex for stop: {} prev:{} now:{}",
                            transitStop.getLabel(), prevMinDistance, minDistance);
                        foundEdgeGeometry = cur_edge.getGeometry();
                        LineString[] lss1 = new LineString[lss.length + 1];
                        lss1[0] = cur_edge.getGeometry();
                        for (int i = 0; i < lss.length; i++) {
                            lss1[i + 1] = lss[i];
                        }
                        edge_ids_show =
                            Long.toString(((EdgeInfo) cur_edge).getOsmID()) + "|" + edge_ids_show;
                        multiLineString = GeometryUtils.getGeometryFactory()
                            .createMultiLineString(lss1);
                    }
                } else {
                    for (Edge edge : edges.get(edges.size() - 1).getToVertex().getOutgoing()) {
                        if (edge instanceof EdgeInfo) {
                            double cur_distance = SphericalDistanceLibrary
                                .fastDistance(GTFSShape.first.getCoordinateN(0),
                                    edge.getGeometry());
                            if (cur_distance < minDistance) {
                                minDistance = cur_distance;
                                cur_edge = edge;
                            }
                        }
                    }
                    if (cur_edge != null) {
                        LOG.info("Found edge closer  in toVertex for stop: {} prev:{} now:{}",
                            transitStop.getLabel(), prevMinDistance, minDistance);
                        foundEdgeGeometry = cur_edge.getGeometry();
                        LineString[] lss1 = new LineString[lss.length + 1];
                        for (int i = 0; i < lss.length; i++) {
                            lss1[i] = lss[i];
                        }
                        lss1[lss1.length - 1] = cur_edge.getGeometry();
                        edge_ids_show += Long.toString(((EdgeInfo) cur_edge).getOsmID());
                        multiLineString = GeometryUtils.getGeometryFactory()
                            .createMultiLineString(lss1);
                    }
                }
                //Some edge which is even closer then previous was found
                if (cur_edge != null) {

                    closestEdges.add(new EdgePoint(cur_edge, GTFSShape.first, GTFSShape.second, minDistance));
                } else {
                    closestEdges
                        .add(new EdgePoint(edges.get(0), GTFSShape.first, GTFSShape.second, minDistance));
                }

            } else {
                LOG.warn("STOP: {} found edge is too far: {}", transitStop.getLabel(), minDistance);
            }
            statsMinDistance1.add(minDistance);


            String distance = Double.toString(
                minDistance), max_distance = "0.0", trimm_distance = "0.0", nearness_distance = "0.0";
            String closest = "", closest_max = "", closest_trim = "", closest_nearnes = "";
            String distant = "";
            String parallel = "0";
            if (parallel(GTFSShape.first, foundEdgeGeometry)) {
                parallel = "1";
            }

            writerMatchedStreets.add(Arrays
                    .asList(transitStop.getLabel(), transitStop.getName(), distance, max_distance,
                        trimm_distance, nearness_distance, closest, closest_max, closest_trim,
                        closest_nearnes, Integer.toString(geometries_count), edge_ids_show, distant,
                        parallel, Double.toString(FastMath.toDegrees(Angle.normalizePositive(
                                DirectionUtils.getAzimuthRad(foundEdgeGeometry))))),
                multiLineString);

            Point[] pts = new Point[2];
            pts[0] = GeometryUtils.getGeometryFactory()
                .createPoint(GTFSShape.first.getCoordinateN(0));
            if (isFromVertexClosest) {
                pts[1] = GeometryUtils.getGeometryFactory()
                    .createPoint(foundEdgeGeometry.getCoordinateN(0));
            } else {
                pts[1] = GeometryUtils.getGeometryFactory().createPoint(
                    foundEdgeGeometry.getCoordinateN(foundEdgeGeometry.getNumPoints() - 1));
            }
            Geometry closestPoints = GeometryUtils.getGeometryFactory().createMultiPoint(pts);
            writerClosestPoints.add(Arrays
                .asList(transitStop.getLabel(), transitStop.getName(), closest, closest_max,
                    closest_trim, closest_nearnes, edge_ids_show, distant, parallel),
                closestPoints);
            if (geometries_count != 1) {
               /* System.out.println("    (" + closest + "|" + closest_max + "|" + closest_trim +
                        "|" + closest_nearnes + "||" + parallel + ")" + " edgeID:" + edge_ids_show);
                        */
            }
        }
        return closestEdges;
    }

    private boolean parallel(LineString GTFSShape, Edge foundEdge) {
        double shapeAzimuthRad = DirectionUtils.getAzimuthRad(GTFSShape);
        double edgeAzimuthRad = DirectionUtils
            .getAzimuthRad(foundEdge.getFromVertex().getCoordinate(),
                foundEdge.getToVertex().getCoordinate());
        return parallel(shapeAzimuthRad, edgeAzimuthRad);
    }

    private boolean parallel(LineString GTFSShape, LineString edge) {
        double shapeAzimuthRad = DirectionUtils.getAzimuthRad(GTFSShape);
        double edgeAzimuthRad = DirectionUtils.getAzimuthRad(edge);
        return parallel(shapeAzimuthRad, edgeAzimuthRad);
    }

    private boolean parallel(double shapeAzimuthRad, double edgeAzimuthRad) {
        double angDiff = Angle.diff(shapeAzimuthRad, edgeAzimuthRad);
        double angInvDiff = Angle.diff(shapeAzimuthRad, Angle.normalize(edgeAzimuthRad + Math.PI));

        double minAngDiff = Math.min(angDiff, angInvDiff);
        return minAngDiff < ANGLE_DIFF;
        //TODO: some problems with round streets -116 shape -145 matched -172 shape all basically parallel the only problem is that there is very little overlap
    }

    @Override public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Linking transit stops to streets...with help of GTFS shapes");
        this.graph = graph;
        this.index = createIndex();
        ArrayList<Vertex> vertices = new ArrayList<>();
        vertices.addAll(graph.getVertices());

        this.networkLinkerLibrary = new NetworkLinkerLibrary(graph, extra);

        final boolean mainShouldWrite = false;
        final boolean shouldWrite = false;
        writerStopShapesGeo = new GeometryCSVWriter(
            Arrays.asList("stop_id", "stop_name", "trip_pattern", "num_geo", "azimuth", "geo"),
            "geo", "outMatcher/MBpatterns.csv", mainShouldWrite);
        writerTransitStop = new GeometryCSVWriter(
            Arrays.asList("stop_id", "stop_name", "num_geo", "geo", "modes"), "geo",
            "outMatcher/MBpattern_stop.csv", mainShouldWrite);
        writerMatchedStreets = new GeometryCSVWriter(Arrays
            .asList("stop_id", "stop_name", "distance", "max_distance", "trimmed_distance",
                "nearness_distance", "is_closest", "is_closest_max_d", "is_closest_trim_d",
                "is_nearness_d", "num_geo", "edge_id", "distant", "parallel", "azimuth", "geo"),
            "geo", "outMatcher/MBpattern_match.csv", mainShouldWrite);
        writerClosestPoints = new GeometryCSVWriter(Arrays
            .asList("stop_id", "stop_name", "is_closest", "is_closest_max_d", "is_closest_trim_d",
                "is_nearness_d", "edge_id", "distant", "parallel", "geo"), "geo",
            "outMatcher/pattern_point.csv", mainShouldWrite);
        //writerAzimuth = new GeometryCSVWriter(Arrays.asList("stop_id", "stop_name", ))
        writerPoint = new GeometryCSVWriter(Arrays.asList("stop_id", "stop_name", "mode", "type", "geo"),
            "geo", "outMatcher/MBpattern_wpoint.csv", shouldWrite);
        writerPoly = new GeometryCSVWriter(Arrays.asList("stop_id", "stop_name", "mode", "type", "geo"),
            "geo", "outMatcher/MBpatterns_street_poly.csv", shouldWrite);
        writerParalelRoads = new GeometryCSVWriter(Arrays.asList("stop_id", "stop_name", "mode",
            "permissions", "is_parallel", "same_level", "azimuth", "idx_edge", "edge_id", "geo"), "geo",
            "outMatcher/MBpatterns_parallel.csv", shouldWrite);
        writerPointSV = new GeometryCSVWriter(Arrays.asList("stop_id", "stop_name", "mode",
        "is_sidwewalk", "geo"), "geo", "outMatcher/MBStreet_v_P.csv", shouldWrite);
        writerCe = new GeometryCSVWriter(Arrays.asList("stop_id", "stop_name", "mode",
            "distance", "score", "endwise", "geo"), "geo", "outMatcher/MBcedges.csv", shouldWrite);
        writeMNS = new GeometryCSVWriter(Arrays.asList("stop_id", "stop_name", "mode", "type", "main_edge_id", "street_edge_id",
            "geo"), "geo", "outMatcher/MB_mns.csv", shouldWrite);
        counts = HashMultiset.create(10);
        statsMinDistance = new DistanceStatistics(true);
        statsMinDistance1 = new DistanceStatistics(true);
        streetMatcher = new StreetMatcher(graph, (STRtree) index);
        numOfStopsEdgesNotMatchedToShapes = 0;
        int nUnlinked = 0;
        //first we need to figure out on which streets PT is driving
        //for each TransitStop we found street on which PT is driving
        for (TransitStop transitStop : Iterables.filter(vertices, TransitStop.class)) {
            // if the street is already linked there is no need to linked it again,
            // could happened if using the prune isolated island
            boolean alreadyLinked = false;
            for (Edge e : transitStop.getOutgoing()) {
                if (e instanceof StreetTransitLink) {
                    alreadyLinked = true;
                    break;
                }
            }

            if (alreadyLinked)
                continue;

            //Here we get edges and points to where we should link according to GTFS which are closest to the path where PT drives
            //This can be roads on which bus drives or TRAM/RAIL/SUBWAY PublicTransitEdges
            List<EdgePoint> edges = getTransitEdges(transitStop);

            //statsMinDistance.add(transitStop.getLevels().size());

            if (edges.isEmpty()) {
                LOG.info(graph.addBuilderAnnotation(new StopUnlinked(transitStop)));
                nUnlinked += 1;
                continue;
            }

            //Now for each of this edges we look around to find if there are some parallel streets which can be walked on
            //Streets which have sidewalks are skipped
            CandidateEdgeBundle candidateEdges = new CandidateEdgeBundle();
            int edge_index = 0;
            for (EdgePoint edgePoint : edges) {
                StreetEdge se = null;
                double minDistance = edgePoint.getMinDistance();
                Coordinate coordinate = edgePoint.getClosestPoint();
                StreetEdge foundClosestSidewalk = null;

                //Current mambo jambo to add current closest street if it is streetEdge and reverse Edge
                //TODO: check if street can actually be walked on. Better reverse detection
                if (edgePoint.getEdge() instanceof StreetEdge) {
                    candidateEdges.add(new CandidateEdge((StreetEdge) edgePoint.getEdge(), transitStop.getCoordinate(), 1.0, transitStop.getModes()));
                    for (Edge anEdge : edgePoint.getEdge().getToVertex().getOutgoing()) {
                        if (anEdge.isReverseOf(edgePoint.getEdge())) {
                            candidateEdges.add(new CandidateEdge((StreetEdge) anEdge, transitStop.getCoordinate(), 1.0, transitStop.getModes()));
                            break;
                        }
                    }
                }




                boolean hasSw = edgePoint.getEdgeInfo().hasSidewalk();
                /*if (transitStop.getLabel().endsWith(":689")) {
                    hasSw = false;
                }*/
                //Current connected street doesn't have a sidewalk
                //We are searching for a parallel street which is a sidewalk
                //Parallel street needs to be inside current street polygon be on same level and must be walkable but not drivable
                if (!hasSw) {

                    //writerPoint.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(), transitStop.getModes().toString()), edgePoint.getClosestPoint());
                    Geometry streetGeo = edgePoint.getEdge().getGeometry().getEnvelope();
                    writerPoly.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(),
                        transitStop.getModes().toString(), "envelope"), streetGeo);
                    Geometry streetPolygon = (Geometry) edgePoint.getEdge().getGeometry()
                        .buffer(0.00021);
                    writerPoly.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(),
                        transitStop.getModes().toString(), "polygon"), streetPolygon);
                    //List<Edge> closestEdges = this.index.query(edgePoint.getEdge().getGeometry().getEnvelopeInternal());
                    List<Edge> closestEdges = this.index.query(streetPolygon.getEnvelopeInternal());
                    Integer foundEdgeIdx = 0;
                    double minDistanceSidewalk = Double.MAX_VALUE;

                    if (edgePoint.getEdge() instanceof StreetEdge) {
                        //TODO: maybe distance to point on current best edge would be better
                        minDistanceSidewalk = SphericalDistanceLibrary
                            .fastDistance(transitStop.getCoordinate(), edgePoint.getClosestPoint());
                        //LOG.info("actualMinDistance: {}", minDistanceSidewalk);
                    }
                    LOG.info("Stop: {} conn to {} ({}) no SW cur min distance: {}",
                        transitStop.getLabel(), edgePoint.getEdge().getName(), edgePoint.getEdgeInfo().getOsmID(),
                         minDistanceSidewalk);


                    for (StreetEdge closestStreetEdge : Iterables
                        .filter(closestEdges, StreetEdge.class)) {
                        if (closestStreetEdge.isStairs()) {
                            continue;
                        }
                        LineString closestStreetEdgeGeo = closestStreetEdge.getGeometry();
                        //street geometry is looked only in the part where it is closest to closestStreetEdge
                        //closestStreetEdgeGeo = MaximalNearestSubline.getMaximalNearestSubline(closestStreetEdgeGeo, edgePoint.getEdge().getGeometry());
                        /*LineString mainGeo = MaximalNearestSubline.getMaximalNearestSubline(
                            edgePoint.getEdge().getGeometry(), closestStreetEdge.getGeometry());*/
                        writeMNS.add(Arrays.asList( transitStop.getLabel(), transitStop.getName(), transitStop.getModes().toString(), "edge",
                            "0", Long.toString(closestStreetEdge.getOsmID())), (Geometry) closestStreetEdgeGeo);
                        /*writeMNS.add(Arrays.asList( transitStop.getLabel(), transitStop.getName(), transitStop.getModes().toString(), "main",
                            Long.toString(edgePoint.getEdgeInfo().getOsmID()), Long.toString(closestStreetEdge.getOsmID())), (Geometry) mainGeo);*/
                        if (!streetPolygon.covers(closestStreetEdgeGeo)) {
                            continue;
                        }

                        String is_parallel = "0";
                        String same_level = "0";
                        if (closestStreetEdge.getLevel().equals(edgePoint.getLevel())) {
                            same_level = "1";
                        }
                        double azimuthClosestStreetEdge = DirectionUtils
                            .getAzimuthRad(closestStreetEdgeGeo);
                        //TODO: check for level, angle it should be around 90 between transitStop projection and edge
                        //And connection can't intersect with any edge
                        if (parallel(DirectionUtils.getAzimuthRad(edgePoint.getEdge().getGeometry()),
                            azimuthClosestStreetEdge)) {
                            is_parallel = "1";
                            //we search for street that can be walked on (sidewalk usually)
                            if (closestStreetEdge.canTraverse(walking) &&
                                !closestStreetEdge.canTraverse(driving) &&
                                same_level.equals("1")) {
                                candidateEdges.add(new CandidateEdge(closestStreetEdge, transitStop.getCoordinate(), 3.0, transitStop.getModes()));
                                for (Edge anEdge : closestStreetEdge.getToVertex().getOutgoing()) {
                                    if (anEdge instanceof StreetEdge && anEdge.isReverseOf(closestStreetEdge)) {
                                        candidateEdges.add(new CandidateEdge((StreetEdge) anEdge, transitStop.getCoordinate(), 3.0, transitStop.getModes()));
                                        break;
                                    }
                                }
                                double curMinDistance = SphericalDistanceLibrary
                                    .fastDistance(transitStop.getCoordinate(),
                                        closestStreetEdge.getGeometry());
                                if (curMinDistance < minDistanceSidewalk) {
                                    minDistanceSidewalk = curMinDistance;
                                    foundClosestSidewalk = closestStreetEdge;
                                    LOG.info("  CLOSE street par, level, walked: {} {} ({}) {}", curMinDistance, closestStreetEdge.getName(), closestStreetEdge.getOsmID(), closestStreetEdge.getPermission());
                                } else {
                                    LOG.info("   far street par, level, walked: {} {} ({}) {}", curMinDistance, closestStreetEdge.getName(), closestStreetEdge.getOsmID(), closestStreetEdge.getPermission());
                                }
                            }
                        }
                        writerParalelRoads.add(Arrays
                            .asList(transitStop.getLabel(), transitStop.getName(),
                                transitStop.getModes().toString(),
                                closestStreetEdge.getPermission().toString(), is_parallel,
                                same_level,
                                Double.toString(FastMath.toDegrees(azimuthClosestStreetEdge)),
                                foundEdgeIdx.toString(), Long.toString(closestStreetEdge.getOsmID())), closestStreetEdge.getGeometry());
                        foundEdgeIdx++;
                    }

                    //Previously found streetEdge
                    if (edgePoint.getEdge() instanceof StreetEdge) {
                        se = (StreetEdge) edgePoint.getEdge();
                    }


                    //if sidewalk was found we connect to the projected point on sidewalk
                    if (foundClosestSidewalk != null) {
                        LOG.info("  Found closer street for {} : {} ({}) {}", transitStop.getLabel(),
                            foundClosestSidewalk.getName(), foundClosestSidewalk.getOsmID(), foundClosestSidewalk.getPermission());
                        LocationIndexedLine indexedSidewalk = new LocationIndexedLine(
                            foundClosestSidewalk.getGeometry());
                        LinearLocation stopLocation = indexedSidewalk
                            .project(transitStop.getCoordinate());

                        coordinate = stopLocation.getCoordinate(foundClosestSidewalk.getGeometry());
                        minDistance = minDistanceSidewalk;
                        se = foundClosestSidewalk;
                    }
                } else {
                    if (edgePoint.getEdge() instanceof StreetEdge) {
                        se = (StreetEdge) edgePoint.getEdge();
                        LOG.info("Stop: {} connected to road with sidewalk", transitStop);
                    }
                }

                Collection<CandidateEdgeBundle> bundles = candidateEdges.binByDistanceAndAngle();
                // initially set best bundle to the closest bundle
                CandidateEdgeBundle best = null;
                for (CandidateEdgeBundle bundle : bundles) {
                    if (best == null || bundle.best.score < best.best.score) {
                        best = bundle;
                        LOG.info("  New best: {}", bundle);
                    } else {
                        LOG.info("  NOT best: {}", bundle);
                    }
                }

                if (se == null && best == null) {
                    LOG.info("Skipping transitMode {} in stop: {}", edgePoint.getTraverseMode(),
                        transitStop);
                    continue;
                }



                boolean foundSameEdge = false;
                if (best != null) {
                    for (CandidateEdge candidateEdge : best) {
                        if (se != null && se.equals(candidateEdge.edge)) {
                            foundSameEdge = true;
                        }
                        writerCe.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(),
                                transitStop.getModes().toString(), Double.toString(candidateEdge.distance),
                                Double.toString(candidateEdge.score), Boolean.toString(candidateEdge.endwise())),
                            candidateEdge.edge.getGeometry());
                        writerPoint.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(),
                                transitStop.getModes().toString(), "ce"), candidateEdge.nearestPointOnEdge);
                    }
                    LOG.info("CE has {} edges", best.size());
                }

                if (!foundSameEdge) {
                    LOG.info(" Different edge found in SE then CE");
                }

                LinkRequest request = new LinkRequest(networkLinkerLibrary);
                //request.connectVertexToStreets(edgePoint.getClosestPoint(), transitStop.hasWheelchairEntrance());
                String vertexLabel;
                vertexLabel = "link for " + transitStop.getStopId();

                statsMinDistance.add(minDistance);


                /*LocationIndexedLine indexedEdge = new LocationIndexedLine(edgePoint.getEdge().getGeometry());
                LinearLocation stopLocation = indexedEdge.project(transitStop.getCoordinate());

                coordinate = stopLocation.getCoordinate(edgePoint.getEdge().getGeometry());*/
                writerPoint.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(),
                    transitStop.getModes().toString(), "found"), coordinate);

                // if the bundle was caught endwise (T intersections and dead ends),
                // get the intersection instead.
                Collection<StreetVertex> nearbyStreetVertices = null; // new ArrayList<>(5);
                //if (edges.endwise())
                //TODO: numOfStopsEdgesNotMatchedToShapes
                //else
                /* is the stop right at an intersection? */
                /*StreetVertex atIntersection = networkLinkerLibrary.index.getIntersectionAt(coordinate);
                // if so, the stop can be linked directly to all vertices at the intersection
                if (atIntersection != null) {
                    //if intersection isn't publicTransit intersection
                    if (!atIntersection.getOutgoingStreetEdges().isEmpty()) {
                        nearbyStreetVertices = Arrays.asList(atIntersection);
                        LOG.info("Connecting stop {} to intersection", transitStop);
                    }
                }
                */

                if (nearbyStreetVertices == null) {

                    if (best != null) {
                        nearbyStreetVertices = request
                            .getSplitterVertices(vertexLabel, best.toEdgeList(), transitStop.getCoordinate());
                        LOG.info(" linking with CE");
                    } else {
                        nearbyStreetVertices = request
                            .getSplitterVertices(vertexLabel, Arrays.asList(se), coordinate);
                        LOG.info(" linking normal");
                    }
                    //LOG.info("Splitting edge for stop {}", transitStop);
                }

                if (nearbyStreetVertices != null) {
                    boolean wheelchairAccessible = transitStop.hasWheelchairEntrance();
                    //Actual linking happens
                    for (StreetVertex sv : nearbyStreetVertices) {
                        new StreetTransitLink(sv, transitStop, wheelchairAccessible);
                        new StreetTransitLink(transitStop, sv, wheelchairAccessible);
                        String isSidewalk = "0";
                        if (foundClosestSidewalk != null) {
                            isSidewalk = "1";
                        }
                        writerPointSV.add(Arrays.asList(transitStop.getLabel(), transitStop.getName(),
                            transitStop.getModes().toString(), isSidewalk), sv.getCoordinate());
                    }
                } else {
                    LOG.info(graph.addBuilderAnnotation(new StopUnlinked(transitStop)));
                    nUnlinked += 1;
                }
                edge_index++;
            }

        }
        if (nUnlinked > 0) {
            LOG.warn(
                "{} transit stops were not close enough to the street network to be connected to it.",
                nUnlinked);
        }
        LOG.info("Search finished");
        writerStopShapesGeo.close();
        writerClosestPoints.close();
        writerTransitStop.close();
        writerMatchedStreets.close();
        writerPoint.close();
        writerPoly.close();
        writerParalelRoads.close();
        writerPointSV.close();
        writerCe.close();
        writeMNS.close();
        for (Multiset.Entry<Integer> stop : counts.entrySet()) {
            System.out.println(stop.getElement() + ": " + stop.getCount());
        }
        System.out.println("Distance between shapes & found edges:");
        System.out.println(statsMinDistance.toString());
        System.out.println("distance in getTransit:");
        System.out.println(statsMinDistance1.toString());
        System.out
            .println("numOfStopsEdgesNotMatchedToShapes: " + numOfStopsEdgesNotMatchedToShapes);


        //remove replaced edges
        for (HashSet<StreetEdge> toRemove : networkLinkerLibrary.getReplacements().keySet()) {
            for (StreetEdge edge : toRemove) {
                edge.getFromVertex().removeOutgoing(edge);
                edge.getToVertex().removeIncoming(edge);
            }
        }
        //and add back in replacements
        for (LinkedList<P2<StreetEdge>> toAdd : networkLinkerLibrary.getReplacements().values()) {
            for (P2<StreetEdge> edges : toAdd) {
                StreetEdge edge1 = edges.first;
                if (edge1.getToVertex().getLabel().startsWith("split ") || edge1.getFromVertex()
                    .getLabel().startsWith("split ")) {
                    continue;
                }
                edge1.getFromVertex().addOutgoing(edge1);
                edge1.getToVertex().addIncoming(edge1);
                StreetEdge edge2 = edges.second;
                if (edge2 != null) {
                    edge2.getFromVertex().addOutgoing(edge2);
                    edge2.getToVertex().addIncoming(edge2);
                }
            }
        }
        //TODO: problems, when GTFS doesn't have shapes or train/bus/tram/subway big stations
    }

    @Override public void checkInputs() {
        //no inputs
    }

    /**
     * Class that in addition to statistics counts how many
     * bigger then 1 and bigger then 20 distances are in
     */
    private class DistanceStatistics extends Statistics {
        private int bigger1;
        private int bigger20;
        private boolean ignoreBigger20;

        /**
         *
         * @param ignoreBigger20 if true distances larger then 20 are ignored
         */
        public DistanceStatistics(boolean ignoreBigger20) {
            super();
            this.bigger1 = 0;
            this.bigger20 = 0;
            this.ignoreBigger20 = ignoreBigger20;
        }

        @Override public void add(double sample) {
            if (this.ignoreBigger20 && sample >= 20) {
                return;
            }
            if (sample >= 1) {
                this.bigger1++;
                if (sample >= 20) {
                    this.bigger20++;
                }
            }
            super.add(sample);
        }

        @Override public String toString(Locale locale, boolean tabulations) {
            String text = super.toString(locale, tabulations);

            text += "minDistance > 1 : " + bigger1 + ", > 20 : " + bigger20 + "\n";
            return text;
        }
    }
}
