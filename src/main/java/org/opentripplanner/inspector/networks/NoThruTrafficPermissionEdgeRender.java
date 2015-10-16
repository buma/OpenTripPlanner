package org.opentripplanner.inspector.networks;

import org.opentripplanner.streets.EdgeStore;
import org.opentripplanner.streets.VertexStore;
import org.opentripplanner.transit.TransportNetwork;

import java.awt.*;

/**
 * This is debug layer which currently shows which mode of transport has no-thru traffic on edge
 *
 * Created by mabu on 8.10.2015.
 */
public class NoThruTrafficPermissionEdgeRender implements EdgeVertexTileRenderer.EdgeVertexRenderer {
    /**
     * @param e     The edge being rendered.
     * @param attrs The edge visual attributes to fill-in.
     * @return True to render this edge, false otherwise.
     */
    @Override public boolean renderEdge(EdgeStore.Edge e,
        EdgeVertexTileRenderer.EdgeVisualAttributes attrs) {
        if (e.isNoThruTraffic()) {
            Color color = getColor(e);
            attrs.color = color;
            attrs.label = getLabel(e);
            return true;
        }
        return false;
    }

    private String getLabel(EdgeStore.Edge e) {
        StringBuilder sb = new StringBuilder();
        if (e.getFlag(EdgeStore.Flag.NO_THRU_TRAFFIC_PEDESTRIAN)) {
            sb.append("W,");
        }
        if (e.getFlag(EdgeStore.Flag.NO_THRU_TRAFFIC_BIKE)) {
            sb.append("B,");
        }
        if (e.getFlag(EdgeStore.Flag.NO_THRU_TRAFFIC_CAR)) {
            sb.append("C,");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // Remove last comma
        } else {
            sb.append("NONE");
        }
        return sb.toString();
    }

    private Color getColor(EdgeStore.Edge e) {
        /*
         * We use the trick that there are 3 main traversal modes (WALK, BIKE and CAR) and 3 color
         * channels (R, G, B).
         */
        float r = 0.2f;
        float g = 0.2f;
        float b = 0.2f;
        if (e.getFlag(EdgeStore.Flag.NO_THRU_TRAFFIC_PEDESTRIAN)) {
            g += 0.5f;
        }
        if (e.getFlag(EdgeStore.Flag.NO_THRU_TRAFFIC_BIKE)) {
            b += 0.5f;
        }
        if (e.getFlag(EdgeStore.Flag.NO_THRU_TRAFFIC_CAR)) {
            r += 0.5f;
        }
        return new Color(r, g, b, 0.5f);
    }

    /**
     * @param v                The vertex being rendered.
     * @param attrs            The vertex visual attributes to fill-in.
     * @param transportNetwork
     * @return True to render this vertex, false otherwise.
     */
    @Override public boolean renderVertex(VertexStore.Vertex v,
        EdgeVertexTileRenderer.VertexVisualAttributes attrs, TransportNetwork transportNetwork) {
        return false;
    }

    /**
     * Name of this tile Render which would be shown in frontend
     *
     * @return Name of tile render
     */
    @Override public String getName() {
        return "Renders which type of no thru traffic is used";
    }
}
