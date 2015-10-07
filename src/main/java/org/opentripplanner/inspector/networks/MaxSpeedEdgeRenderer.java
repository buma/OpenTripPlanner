package org.opentripplanner.inspector.networks;

import org.opentripplanner.inspector.DefaultScalarColorPalette;
import org.opentripplanner.inspector.ScalarColorPalette;
import org.opentripplanner.streets.EdgeStore;
import org.opentripplanner.streets.VertexStore;
import org.opentripplanner.transit.TransportNetwork;

/**
 * Shows max speed of transportNetwork Edges
 * Created by mabu on 7.10.2015.
 */
public class MaxSpeedEdgeRenderer implements EdgeVertexTileRenderer.EdgeVertexRenderer {

    private ScalarColorPalette palette = new DefaultScalarColorPalette(10.0, 90.0, 130.0);


    /**
     * @param e     The edge being rendered.
     * @param attrs The edge visual attributes to fill-in.
     * @return True to render this edge, false otherwise.
     */
    @Override public boolean renderEdge(EdgeStore.Edge e,
        EdgeVertexTileRenderer.EdgeVisualAttributes attrs) {
        double speed = e.getSpeed()*3.6;
        attrs.color = palette.getColor(speed);
        attrs.label = String.format("%.02f", speed);
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
        return false;
    }

    /**
     * Name of this tile Render which would be shown in frontend
     *
     * @return Name of tile render
     */
    @Override public String getName() {
        return "Max speed";
    }
}
