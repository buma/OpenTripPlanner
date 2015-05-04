package org.opentripplanner.inspector;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopStreetVertex;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mabu on 24.4.2015.
 */
public class OSMStopRenderer implements EdgeVertexTileRenderer.EdgeVertexRenderer {
    private static Map<TraverseMode, Color> modes;

    static {
        modes = new HashMap<>(5);
        modes.put(TraverseMode.BUS, Color.CYAN);
        modes.put(TraverseMode.RAIL, Color.orange);
        modes.put(TraverseMode.TRAM, Color.RED);
        modes.put(TraverseMode.SUBWAY, Color.GREEN);
        modes.put(TraverseMode.GONDOLA, Color.pink);
        modes.put(TraverseMode.FERRY, Color.GRAY);
        modes.put(TraverseMode.FUNICULAR, Color.magenta);
        modes.put(TraverseMode.LEG_SWITCH, new Color(7, 100, 59));
    }

    @Override public boolean renderEdge(Edge e, EdgeVertexTileRenderer.EdgeVisualAttributes attrs) {
        return false;
    }

    @Override public boolean renderVertex(Vertex v,
        EdgeVertexTileRenderer.VertexVisualAttributes attrs) {
        if (v instanceof TransitStopStreetVertex) {
            TransitStopStreetVertex transitStopStreetVertex = (TransitStopStreetVertex) v;
            if (transitStopStreetVertex.mode != TraverseMode.FERRY) {
                return false;
            }
            attrs.color = modes.get(transitStopStreetVertex.mode);
            attrs.label = v.getName() != null ? v.getName() : v.getLabel();
            attrs.label += " (" + v.getOutgoingStreetEdges().size() + ")";
            return true;
        }
        return false;
    }

    @Override public String getName() {
        return "OSM stops";
    }
}
