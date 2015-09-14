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

package org.opentripplanner.api.resource.networks;

import com.conveyal.gtfs.model.Stop;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.CoordinateArrayListSequence;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.streets.EdgeStore;
import org.opentripplanner.streets.StreetRouter;
import org.opentripplanner.streets.TransportNetworkPath;
import org.opentripplanner.streets.VertexStore;
import org.opentripplanner.transit.TransportNetwork;
import org.opentripplanner.util.PolylineEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by mabu on 14.9.2015.
 */
public class TransportNetworkPathToTripPlanConverter {
    private static final Logger LOG = LoggerFactory
        .getLogger(TransportNetworkPathToTripPlanConverter.class);

    /**
     * Generates a TripPlan from a set of paths
     */
    public static TripPlan generatePlan(List<TransportNetworkPath> paths, RoutingRequest request) {
        Locale requestedLocale = request.locale;

        TransportNetworkPath exemplar = paths.get(0);
        VertexStore.Vertex tripStartVertex = exemplar.getStartVertex();
        VertexStore.Vertex tripEndVertex = exemplar.getEndVertex();

        String startName = tripStartVertex.getName(requestedLocale);
        String endName = tripEndVertex.getName(requestedLocale);

        // Use vertex labels if they don't have names
        if (startName == null) {
            startName = tripStartVertex.getLabel();
        }
        if (endName == null) {
            endName = tripEndVertex.getLabel();
        }

        Place from = new Place(tripStartVertex.getLon(), tripStartVertex.getLat(), startName);
        Place to = new Place(tripEndVertex.getLon(), tripEndVertex.getLat(), endName);

        from.orig = request.from.name;
        to.orig = request.to.name;

        TripPlan plan = new TripPlan(from, to, request.getDateTime());
        for (TransportNetworkPath path : paths) {
            Itinerary itinerary = generateItinerary(path, request.showIntermediateStops, requestedLocale);
            itinerary = adjustItinerary(request, itinerary);
            plan.addItinerary(itinerary);
        }
        if (plan != null) {
            for (Itinerary i : plan.itinerary) {
                /* Communicate the fact that the only way we were able to get a response was by removing a slope limit. */
                i.tooSloped = false; // request.rctx.slopeRestrictionRemoved;
                /* fix up from/to on first/last legs */
                if (i.legs.size() == 0) {
                    LOG.warn("itinerary has no legs");
                    continue;
                }
                Leg firstLeg = i.legs.get(0);
                firstLeg.from.orig = plan.from.orig;
                Leg lastLeg = i.legs.get(i.legs.size() - 1);
                lastLeg.to.orig = plan.to.orig;
            }
        }
//        request.rctx.debugOutput.finishedRendering();
        return plan;
    }

    /**
     * Check whether itinerary needs adjustments based on the request.
     * @param itinerary is the itinerary
     * @param request is the request containing the original trip planning options
     * @return the (adjusted) itinerary
     */
    private static Itinerary adjustItinerary(RoutingRequest request, Itinerary itinerary) {
        // Check walk limit distance
        if (itinerary.walkDistance > request.maxWalkDistance) {
            itinerary.walkLimitExceeded = true;
        }
        // Return itinerary
        return itinerary;
    }

    /**
     * Generate an itinerary from a {@link GraphPath}. This method first slices the list of states
     * at the leg boundaries. These smaller state arrays are then used to generate legs. Finally the
     * rest of the itinerary is generated based on the complete state array.
     *
     * @param path The graph path to base the itinerary on
     * @param showIntermediateStops Whether to include intermediate stops in the itinerary or not
     * @return The generated itinerary
     */
    private static Itinerary generateItinerary(TransportNetworkPath path, boolean showIntermediateStops,
        Locale requestedLocale) {
        Itinerary itinerary = new Itinerary();

        StreetRouter.State[] states = new StreetRouter.State[path.states.size()];
        StreetRouter.State lastState = path.states.getLast();
        states = path.states.toArray(states);

        //Edges for elevation calculation

        //Should probably be some kind of transit context
        TransportNetwork transportNetwork = path.getTransportNetwork();


        //TODO: sliceStates (if it is actually necessary)
        StreetRouter.State[][] legsStates = new StreetRouter.State[1][];
        legsStates[0]=states;
        for (StreetRouter.State[] legStates : legsStates) {
            itinerary.addLeg(generateLeg(transportNetwork, legStates, showIntermediateStops, requestedLocale));
        }

        itinerary.duration = lastState.getElapsedTimeSeconds();
        itinerary.startTime = makeCalendar(states[0], transportNetwork);
        itinerary.endTime = makeCalendar(lastState, transportNetwork);

        return itinerary;
    }

    /**
     * Generate one leg of an itinerary from a {@link State} array.
     *
     * @param states The array of states to base the leg on
     * @param showIntermediateStops Whether to include intermediate stops in the leg or not
     * @return The generated leg
     */
    private static Leg generateLeg(TransportNetwork transportNetwork,
        StreetRouter.State[] states, boolean showIntermediateStops, Locale requestedLocale) {
        Leg leg = new Leg();

        EdgeStore.Edge[] edges = new EdgeStore.Edge[states.length -1];

        leg.startTime = makeCalendar(states[0], transportNetwork);
        leg.endTime = makeCalendar(states[states.length - 1], transportNetwork);

        leg.distance = 0.0;
        //TODO: Maybe this can be made better with streams
        for (int i = 0; i < edges.length; i++) {
            int edge_idx = states[i + 1].backEdge;
            if (edge_idx == -1) {
                LOG.warn("Edge index is missing:{} idx:{}", edge_idx, i);
                continue;
            }
            EdgeStore.Edge edge = transportNetwork.streetLayer.edgeStore.getCursor(edge_idx);
            leg.distance += edge.getLengthM();
            edges[i] = edge;
            //LOG.info("Edge: {} (F:{}), geo:{}", edge, edge.isForward(), geometry);
            LOG.info("Edge: {}, {}", edge.getOSMID(), edge.getName());
        }

        //add mode and alerts
        leg.mode = TraverseMode.WALK.toString();

        TimeZone timeZone = leg.startTime.getTimeZone();
        leg.agencyTimeZoneOffset = timeZone.getOffset(leg.startTime.getTimeInMillis());

        addPlaces(leg, states, edges, showIntermediateStops, transportNetwork, requestedLocale);

        CoordinateArrayListSequence coordinates = makeCoordinates(edges);
        com.vividsolutions.jts.geom.Geometry geometry = GeometryUtils.getGeometryFactory().createLineString(coordinates);

        leg.legGeometry = PolylineEncoder.createEncodings(geometry);

        return leg;
    }

    /**
     * Add {@link Place} fields to a {@link Leg}.
     * There is some code duplication because of subtle differences between departure, arrival and
     * intermediate stops.
     *  @param leg The leg to add the places to
     * @param states The states that go with the leg
     * @param edges The edges that go with the leg
     * @param showIntermediateStops Whether to include intermediate stops in the leg or not
     * @param transportNetwork
     */
    private static void addPlaces(Leg leg, StreetRouter.State[] states, EdgeStore.Edge[] edges,
        boolean showIntermediateStops, TransportNetwork transportNetwork, Locale requestedLocale) {
        VertexStore.Vertex firstVertex = states[0].getVertex(transportNetwork);
        VertexStore.Vertex lastVertex = states[states.length - 1].getVertex(transportNetwork);

        Stop firstStop = null;
        Stop lastStop = null;
        TripTimes tripTimes = null;

        leg.from = makePlace(states[0], firstVertex, edges[0], firstStop, tripTimes,
            requestedLocale);
        leg.from.arrival = null;
        leg.to = makePlace(states[states.length - 1], lastVertex, null, lastStop, tripTimes, requestedLocale);
        leg.to.departure = null;

    }

    /**
     * Make a {@link Place} to add to a {@link Leg}.
     *
     * @param state The {@link State} that the {@link Place} pertains to.
     * @param vertex The {@link Vertex} at the {@link State}.
     * @param edge The {@link Edge} leading out of the {@link Vertex}.
     * @param stop The {@link org.onebusaway.gtfs.model.Stop} associated with the {@link Vertex}.
     * @param tripTimes The {@link TripTimes} associated with the {@link Leg}.
     * @return The resulting {@link Place} object.
     */
    private static Place makePlace(StreetRouter.State state, VertexStore.Vertex vertex,
        EdgeStore.Edge edge, Stop stop, TripTimes tripTimes, Locale requestedLocale) {

        Place place = new Place(vertex.getLon(), vertex.getLat(), vertex.getName(requestedLocale));

        //TODO stop data
        return place;
    }

    private static Calendar makeCalendar(StreetRouter.State state,
        TransportNetwork transportNetwork) {
        TimeZone timeZone = transportNetwork.getTimeZone();
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(state.getTimeInMillis());
        return calendar;
    }

    /**
     * Generate a {@link CoordinateArrayListSequence} based on an {@link Edge} array.
     *
     * @param edges The array of input edges
     * @return The coordinates of the points on the edges
     */
    private static CoordinateArrayListSequence makeCoordinates(EdgeStore.Edge[] edges) {
        CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();

        for (EdgeStore.Edge edge : edges) {
            LineString geometry = edge.getGeometry();

            if (geometry != null) {
                if (coordinates.size() == 0) {
                    coordinates.extend(geometry.getCoordinates());
                } else {
                    coordinates.extend(geometry.getCoordinates(), 1); // Avoid duplications
                }
            }
        }

        return coordinates;
    }
}
