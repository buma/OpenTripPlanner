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
import org.opentripplanner.index.model.StopShort;
import org.opentripplanner.standalone.OTPServerWithNetworks;
import org.opentripplanner.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by mabu on 14.9.2015.
 */
@Path("/routers/{routerId}/index")    // It would be nice to get rid of the final /index.
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class IndexAPIResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndexAPIResource.class);
    private static final double MAX_STOP_SEARCH_RADIUS = 5000;

    private final TransportNetwork transportNetwork;

    public IndexAPIResource (@Context OTPServerWithNetworks otpServer, @PathParam("routerId") String routerId) {
        //TODO: choose router
        transportNetwork = otpServer.transportNetwork;
    }
    /* Needed to check whether query parameter map is empty, rather than chaining " && x == null"s */
    @Context UriInfo uriInfo;

    /** Return a list of all stops within a circle around the given coordinate. */
    @GET
    @Path("/stops")
    public Response getStopsInRadius (
        @QueryParam("minLat") Double minLat,
        @QueryParam("minLon") Double minLon,
        @QueryParam("maxLat") Double maxLat,
        @QueryParam("maxLon") Double maxLon,
        @QueryParam("lat")    Double lat,
        @QueryParam("lon")    Double lon,
        @QueryParam("radius") Double radius) {
        Collection<Stop> stops = transportNetwork.transitLayer.stopForIndex;
        //Currently always returns empty list since stopForIndex is empty
        if (stops == null) {
            return Response.ok(new ArrayList<StopShort>()).build();
        } else {
            return Response.status(Response.Status.OK).entity(StopShort.list_gtfs_stop(stops))
                .build();
        }
    }
}
