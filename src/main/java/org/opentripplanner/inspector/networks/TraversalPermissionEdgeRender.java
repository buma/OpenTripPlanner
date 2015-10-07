package org.opentripplanner.inspector.networks;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.streets.EdgeStore;
import org.opentripplanner.streets.VertexStore;
import org.opentripplanner.transit.TransportNetwork;

import java.awt.*;

/**
 * Created by mabu on 7.10.2015.
 */
public class TraversalPermissionEdgeRender implements EdgeVertexTileRenderer.EdgeVertexRenderer {

    private static final Color LINK_COLOR_EDGE = Color.ORANGE;

    private static final Color STAIRS_COLOR_EDGE = Color.PINK;

    private static final Color STREET_COLOR_VERTEX = Color.DARK_GRAY;

    private static final Color TRANSIT_STOP_COLOR_VERTEX = new Color(0.0f, 0.0f, 0.8f);

    private static final Color TRANSIT_STATION_COLOR_VERTEX = new Color(0.4f, 0.0f, 0.8f);

    private static final Color BIKE_RENTAL_COLOR_VERTEX = new Color(0.0f, 0.7f, 0.0f);

    private static final Color PARK_AND_RIDE_COLOR_VERTEX = Color.RED;

    /**
     * @param e     The edge being rendered.
     * @param attrs The edge visual attributes to fill-in.
     * @return True to render this edge, false otherwise.
     */
    @Override public boolean renderEdge(EdgeStore.Edge e,
        EdgeVertexTileRenderer.EdgeVisualAttributes attrs) {
        if (e.getPermissions().allowsNothing()) {
            return false;
        }
        if (e.getFlag(EdgeStore.Flag.STAIRS)) {
            attrs.color = STAIRS_COLOR_EDGE;
            attrs.label = "stairs";
        } else {
            attrs.color = getColor(e.getPermissions());
            attrs.label = getLabel(e.getPermissions());
        }
        return true;
    }

    /**
     * @param v     The vertex being rendered.
     * @param attrs The vertex visual attributes to fill-in.
     * @param transportNetwork
     * @return True to render this vertex, false otherwise.
     */
    @Override public boolean renderVertex(VertexStore.Vertex v,
        EdgeVertexTileRenderer.VertexVisualAttributes attrs, TransportNetwork transportNetwork) {

        int stopIndex = transportNetwork.streetLayer.linkedTransitLayer.stopForStreetVertex.get(v.index);
        if (stopIndex >= 0) {
            String name = transportNetwork.streetLayer.linkedTransitLayer.namesForStop.get(stopIndex);
            attrs.color = TRANSIT_STOP_COLOR_VERTEX;
            attrs.label = name;
        }
        //transportNetwork.streetLayer.
        attrs.color = STREET_COLOR_VERTEX;
        return true;
    }

    private Color getColor(StreetTraversalPermission permissions) {
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

    private String getLabel(StreetTraversalPermission permissions) {
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

    @Override
    public String getName() {
        return "Traversal permissions";
    }
}
