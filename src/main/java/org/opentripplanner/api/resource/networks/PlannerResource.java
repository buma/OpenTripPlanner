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

import org.opentripplanner.api.common.TransportNetworkRoutingResource;
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
@Path("routers/{routerId}/plan")
public class PlannerResource extends TransportNetworkRoutingResource {
    private static final Logger LOG = LoggerFactory.getLogger(PlannerResource.class);

    // We inject info about the incoming request so we can include the incoming query
    // parameters in the outgoing response. This is a TriMet requirement.
    // Jersey uses @Context to inject internal types and @InjectParam or @Resource for DI objects.
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public Response plan(@Context OTPServerWithNetworks otpServer, @Context UriInfo uriInfo) {
        Response response = new Response(uriInfo);
        LOG.info("FROM:{} TO:{}, router:{}", fromPlace, toPlace, routerId);

        RoutingRequest routingRequest = null;
        try {
            routingRequest = super.buildRequest();
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

            TripPlan plan = TransportNetworkPathToTripPlanConverter
                .generatePlan(paths, routingRequest);
            response.setPlan(plan);
        }catch (Exception e) {
            PlannerError error = new PlannerError(e);
            if(!PlannerError.isPlanningError(e.getClass()))
                LOG.warn("Error while planning path: ", e);
            response.setError(error);
        } finally {
            if (routingRequest != null) {
                if (routingRequest.rctx != null) {
                    response.debugOutput = routingRequest.rctx.debugOutput;
                }
                routingRequest.cleanup(); // TODO verify that this cleanup step is being done on Analyst web services
            }
        }

        return response;
    }
}
