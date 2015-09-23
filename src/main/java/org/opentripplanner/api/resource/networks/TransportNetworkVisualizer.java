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
import org.opentripplanner.inspector.DefaultScalarColorPalette;
import org.opentripplanner.inspector.ScalarColorPalette;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.standalone.OTPServerWithNetworks;
import org.opentripplanner.streets.EdgeStore;
import org.opentripplanner.streets.VertexStore;
import org.opentripplanner.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.measure.converter.UnitConverter;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.opentripplanner.api.resource.networks.Routers.Q;
import static org.opentripplanner.streets.VertexStore.floatingDegreesToFixed;

import static javax.measure.unit.SI.METERS_PER_SECOND;
import static javax.measure.unit.NonSI.KILOMETERS_PER_HOUR;

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

    private static final UnitConverter unitConverter = METERS_PER_SECOND.getConverterTo(KILOMETERS_PER_HOUR);

    /**
     * The routerId selects between several graphs on the same server. The routerId is pulled from
     * the path, not the query parameters. However, the class RoutingResource is not annotated with
     * a path because we don't want it to be instantiated as an endpoint. Instead, the {routerId}
     * path parameter should be included in the path annotations of all its subclasses.
     */
    @PathParam("routerId") public String routerId;

    @PathParam("layer") public String layer;

    @QueryParam("detail") private boolean detail = false;

    @QueryParam("n") public Double north;

    @QueryParam("s") public Double south;

    @QueryParam("e") public Double east;

    @QueryParam("w") public Double west;

    //Gets colors for speeds in km/h
    private ScalarColorPalette palette = new DefaultScalarColorPalette(10.0, 90.0, 130.0);

    private Color getPermissionColor(StreetTraversalPermission permissions) {
        /*
         * We use the trick that there are 3 main traversal modes (WALK, BIKE and CAR) and 3 color
         * channels (R, G, B).
         */
        float r = 0.2f;
        float g = 0.2f;
        float b = 0.2f;
        if (permissions.allows(StreetTraversalPermission.PEDESTRIAN))
            g += 0.5f;
        if (permissions.allows(StreetTraversalPermission.BICYCLE))
            b += 0.5f;
        if (permissions.allows(StreetTraversalPermission.CAR))
            r += 0.5f;
        // TODO CUSTOM_VEHICLE (?)
        return new Color(r, g, b);
    }

    private String getPermissionLabel(StreetTraversalPermission permissions) {
        StringBuffer sb = new StringBuffer();
        if (permissions.allows(StreetTraversalPermission.PEDESTRIAN))
            sb.append("walk,");
        if (permissions.allows(StreetTraversalPermission.BICYCLE))
            sb.append("bike,");
        if (permissions.allows(StreetTraversalPermission.CAR))
            sb.append("car,");
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // Remove last comma
        } else {
            sb.append("none");
        }
        return sb.toString();
    }

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
                        //skips edges which are actually outside envelope
                        if (!(env.intersects(cursor.getEnvelope()))){
                            return true;
                        }

                        gen.writeStartObject();

                        gen.writeObjectFieldStart("properties");
                        if (detail) {
                            gen.writeNumberField("osm_id", cursor.getOSMID());
                            gen.writeStringField("name", cursor.getName());
                            double speedMs = cursor.getSpeed() / VertexStore.FIXED_FACTOR;
                            Color color = palette
                                .getColor(Math.round(unitConverter.convert(speedMs)));
                            String hexColor = String
                                .format("#%02x%02x%02x", color.getRed(), color.getGreen(),
                                    color.getBlue());
                            gen.writeStringField("speed",
                                String.format("%.2f km/h", unitConverter.convert(speedMs)));
                            //gen.writeNumberField("speed_ms", speedMs);
                            gen.writeStringField("color", hexColor);
                        }
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
        } else if ("permEdges".equals(layer)) {
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
                        //skips edges which are actually outside envelope
                        if (!(env.intersects(cursor.getEnvelope()))){
                            return true;
                        }

                        gen.writeStartObject();

                        gen.writeObjectFieldStart("properties");
                        if (detail) {
                            gen.writeNumberField("osm_id", cursor.getOSMID());
                            gen.writeStringField("name", cursor.getName());
                            StreetTraversalPermission streetPermission = cursor.getPermissions();
                            String label = getPermissionLabel(streetPermission);
                            Color color = getPermissionColor(streetPermission);
                            String hexColor = String
                                .format("#%02x%02x%02x", color.getRed(), color.getGreen(),
                                    color.getBlue());
                            gen.writeStringField("label", label);
                            //gen.writeNumberField("speed_ms", speedMs);
                            gen.writeStringField("color", hexColor);
                        }
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
