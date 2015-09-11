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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Envelope;
import gnu.trove.set.TIntSet;
import org.opentripplanner.standalone.OTPServerWithNetworks;
import org.opentripplanner.streets.EdgeStore;
import org.opentripplanner.streets.VertexStore;
import org.opentripplanner.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.opentripplanner.api.resource.networks.Routers.Q;
import static org.opentripplanner.streets.VertexStore.floatingDegreesToFixed;

/**
 * Simple Web-based visualizer for transport networks.
 * <p>
 * Currently it just returns list of edges inside visual bounds as Geojson.
 * Created by @mattwigway
 */
@Path("routers/{routerId}/visualize/{layer}") public class TransportNetworkVisualizer {
    private static final Logger LOG = LoggerFactory.getLogger(TransportNetworkVisualizer.class);

    //Context with some kind of data is missing.
    @Context OTPServerWithNetworks otpServerWithNetworks;

    /**
     * The routerId selects between several graphs on the same server. The routerId is pulled from
     * the path, not the query parameters. However, the class RoutingResource is not annotated with
     * a path because we don't want it to be instantiated as an endpoint. Instead, the {routerId}
     * path parameter should be included in the path annotations of all its subclasses.
     */
    @PathParam("routerId") public String routerId;

    @PathParam("layer") public String layer;

    @QueryParam("n") public Double north;

    @QueryParam("s") public Double south;

    @QueryParam("e") public Double east;

    @QueryParam("w") public Double west;

    @GET @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q,
        MediaType.TEXT_XML + Q }) public Response visualize() {

        if (layer == null || north == null || south == null || east == null || west == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Envelope env = new Envelope(floatingDegreesToFixed(east), floatingDegreesToFixed(west),
            floatingDegreesToFixed(south), floatingDegreesToFixed(north));

        //TODO: support for multiple routers
        TransportNetwork network = otpServerWithNetworks.transportNetwork;
        if ("streetEdges".equals(layer)) {
            //TODO: it would be useful to also return request entity too large if Envelope is too large
            TIntSet streets = network.streetLayer.spatialIndex.query(env);

            if (streets.size() > 10_000) {
                LOG.warn("Refusing to include more than 10000 edges in result");
                return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE).build();
            }

            try {
                // write geojson to response
                ObjectMapper mapper = new ObjectMapper();
                JsonFactory factory = mapper.getFactory();
                OutputStream os = new ByteArrayOutputStream();
                JsonGenerator gen = factory.createGenerator(os);

                // geojson header
                gen.writeStartObject();
                gen.writeStringField("type", "FeatureCollection");

                gen.writeArrayFieldStart("features");

                EdgeStore.Edge cursor = network.streetLayer.edgeStore.getCursor();
                VertexStore.Vertex vcursor = network.streetLayer.vertexStore.getCursor();

                streets.forEach(s -> {
                    try {
                        cursor.seek(s);

                        gen.writeStartObject();

                        gen.writeObjectFieldStart("properties");
                        gen.writeEndObject();

                        gen.writeStringField("type", "Feature");

                        gen.writeObjectFieldStart("geometry");
                        gen.writeStringField("type", "LineString");
                        gen.writeArrayFieldStart("coordinates");

                        gen.writeStartArray();
                        vcursor.seek(cursor.getFromVertex());
                        gen.writeNumber(vcursor.getLon());
                        gen.writeNumber(vcursor.getLat());
                        gen.writeEndArray();

                        gen.writeStartArray();
                        vcursor.seek(cursor.getToVertex());
                        gen.writeNumber(vcursor.getLon());
                        gen.writeNumber(vcursor.getLat());
                        gen.writeEndArray();

                        gen.writeEndArray();

                        gen.writeEndObject();
                        gen.writeEndObject();
                        return true;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                gen.writeEndArray();
                gen.writeEndObject();

                gen.flush();
                gen.close();
                os.close();

                String json = os.toString();

                return Response.ok(json, "application/json").build();
            } catch (IOException io) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity("Unknown layer. Only streetEdges is currently supported").build();
        }
    }
}
