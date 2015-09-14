package org.opentripplanner.api.resource.networks;

import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.api.resource.Response;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.standalone.OTPServerWithNetworks;
import org.opentripplanner.streets.Split;
import org.opentripplanner.streets.StreetRouter;
import org.opentripplanner.streets.TransportNetworkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

import static org.opentripplanner.api.resource.networks.Routers.Q;

/**
 * Created by mabu on 10.9.2015.
 */
@Path("routers/{routerId}/plan") public class PlannerResource {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerResource.class);

    /**
     * The routerId selects between several graphs on the same server. The routerId is pulled from
     * the path, not the query parameters. However, the class RoutingResource is not annotated with
     * a path because we don't want it to be instantiated as an endpoint. Instead, the {routerId}
     * path parameter should be included in the path annotations of all its subclasses.
     */
    @PathParam("routerId") public String routerId;

    /**
     * The start location -- either latitude, longitude pair in degrees or a Vertex
     * label. For example, <code>40.714476,-74.005966</code> or
     * <code>mtanyctsubway_A27_S</code>.
     */
    @QueryParam("fromPlace") protected String fromPlace;

    /**
     * The end location (see fromPlace for format).
     */
    @QueryParam("toPlace") protected String toPlace;

    // We inject info about the incoming request so we can include the incoming query
    // parameters in the outgoing response. This is a TriMet requirement.
    // Jersey uses @Context to inject internal types and @InjectParam or @Resource for DI objects.
    @GET @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q,
        MediaType.TEXT_XML + Q }) public Response plan(@Context OTPServerWithNetworks otpServer,
        @Context UriInfo uriInfo) {
        Response response = new Response(uriInfo);
        LOG.info("FROM:{} TO:{}, router:{}", fromPlace, toPlace, routerId);

        RoutingRequest routingRequest = new RoutingRequest();
        routingRequest.routerId = routerId;
        if (fromPlace != null)
            routingRequest.setFromString(fromPlace);

        if (toPlace != null)
            routingRequest.setToString(toPlace);

        LOG.info("read request: {}", routingRequest);

        StreetRouter router = new StreetRouter(otpServer.transportNetwork.streetLayer);
        router.setOrigin(routingRequest.from.lat, routingRequest.from.lng);

        Split split = otpServer.transportNetwork.streetLayer
            .findSplit(routingRequest.to.lat, routingRequest.to.lng, 300);
        if (split == null) {
            Exception e = new Exception("No vertex wasn't found near to coordinate!");
            PlannerError error = new PlannerError(e);
            if (!PlannerError.isPlanningError(e.getClass()))
                LOG.warn("Error while planning path: ", e);
            response.setError(error);
            return response;
        }

        //TODO: what about vertex1 It could also be a destination?
        router.toVertex = split.vertex0;
        router.route();

        List<TransportNetworkPath> paths = new ArrayList<>(1);
        paths.add(new TransportNetworkPath(router.getLastState(), otpServer.transportNetwork));

        TripPlan plan = TransportNetworkPathToTripPlanConverter.generatePlan(paths, routingRequest);
        response.setPlan(plan);

        return response;
    }
}
