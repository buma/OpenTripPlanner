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

package org.opentripplanner.streets;

import com.google.common.collect.Lists;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mabu on 25.9.2015.
 */
public class TransportNetworkFinder {

    private static final Logger LOG = LoggerFactory.getLogger(TransportNetworkFinder.class);
    private static final double DEFAULT_MAX_WALK = 2000;
    private static final double CLAMP_MAX_WALK = 15000;

    Router router;
    public TransportNetworkFinder(Router router) {
        this.router = router;
    }

    public List<TransportNetworkPath> transportNetworkEntryPoint(RoutingRequest request) {
        List<TransportNetworkPath> paths = null;
        try {
            paths = getGraphPathsConsideringIntermediates(request);
            if (paths == null && request.wheelchairAccessible) {
                // There are no paths that meet the user's slope restrictions.
                // Try again without slope restrictions, and warn the user in the response.
                RoutingRequest relaxedRequest = request.clone();
                relaxedRequest.maxSlope = Double.MAX_VALUE;
  //              request.rctx.slopeRestrictionRemoved = true;
                paths = getGraphPathsConsideringIntermediates(relaxedRequest);
            }
//            request.rctx.debugOutput.finishedCalculating();
        } catch (VertexNotFoundException e) {
            LOG.info("Vertex not found: " + request.from + " : " + request.to);
            throw e;
        }

        if (paths == null || paths.size() == 0) {
            LOG.debug("Path not found: " + request.from + " : " + request.to);
//            request.rctx.debugOutput.finishedRendering(); // make sure we still report full search time
            throw new PathNotFoundException();
        }
        return paths;
    }

    private List<TransportNetworkPath> getGraphPathsConsideringIntermediates(RoutingRequest request) {
        if (request.hasIntermediatePlaces()) {
            LOG.error("INTERMEDIATES DOESN'T WORK!!");
            long time = request.dateTime;
            GenericLocation from = request.from;
            List<GenericLocation> places = Lists.newLinkedList(request.intermediatePlaces);
            places.add(request.to);
            request.clearIntermediatePlaces();
            List<TransportNetworkPath> paths = new ArrayList<>();

            for (GenericLocation to : places) {
                request.dateTime = time;
                request.from = from;
                request.to = to;
                request.rctx = null;
                //request.setRoutingContext(router.transportNetwork);
                // TODO request only one itinerary here

                List<TransportNetworkPath> partialPaths = getPaths(request);
                if (partialPaths == null || partialPaths.size() == 0) {
                    return null;
                }

                TransportNetworkPath path = partialPaths.get(0);
                paths.add(path);
                from = to;
                time = path.getEndTime();
            }

            //return Arrays.asList(joinPaths(paths));
            return paths;
        } else {
            return getPaths(request);
        }
    }

    private TransportNetworkPath joinPaths(List<TransportNetworkPath> paths) {
        return null;
    }

    private List<TransportNetworkPath> getPaths(RoutingRequest options) {
        if (options == null) {
            LOG.error("PathService was passed a null routing request.");
            return null;
        }

        TransportNetworkContext ctx = null;

        if (ctx == null) {
            ctx = new TransportNetworkContext(options, router.transportNetwork);
            ctx.check();
        }
        // without transit, we'd just just return multiple copies of the same on-street itinerary
        if (!options.modes.isTransit()) {
            options.numItineraries = 1;
        }

        /* In long distance mode, maxWalk has a different meaning. It's the radius around the origin or destination
         * within which you can walk on the streets. If no value is provided, max walk defaults to the largest
         * double-precision float. This would cause long distance mode to do unbounded street searches and consider
         * the whole graph walkable. */
        if (options.maxWalkDistance == Double.MAX_VALUE) options.maxWalkDistance = DEFAULT_MAX_WALK;
        if (options.maxWalkDistance > CLAMP_MAX_WALK) options.maxWalkDistance = CLAMP_MAX_WALK;
        long searchBeginTime = System.currentTimeMillis();
        LOG.debug("BEGIN SEARCH");
        List<TransportNetworkPath> paths = Lists.newArrayList();
        StreetRouter streetRouter = new StreetRouter(router.transportNetwork.streetLayer);
        streetRouter.setOrigin(ctx.origin);
        streetRouter.toVertex = ctx.target.vertex0;
        streetRouter.route();
        TransportNetworkPath first = new TransportNetworkPath(streetRouter.getLastState(), router.transportNetwork, options.arriveBy);
        //paths.add(first);
        streetRouter.setOrigin(ctx.origin);
        streetRouter.toVertex = ctx.target.vertex1;
        streetRouter.route();
        TransportNetworkPath second = new TransportNetworkPath(streetRouter.getLastState(), router.transportNetwork, options.arriveBy);

        //TODO: TransportNetworkPath needs to reverse path if arriveBy is true. Otherwise path is in reverse
        //paths.add(second);
        LOG.info("First:{}, Second:{}", first.getlastWeight(), second.getlastWeight());
        if (second.getlastWeight() < first.getlastWeight()) {
            paths.add(second);
            LOG.info("Added second");
        } else {
            paths.add(first);
            LOG.info("Added first");
        }

        return paths;
    }
}
