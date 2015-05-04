package org.opentripplanner.inspector;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mabu on 23.4.2015.
 */
public class PatternHopRenderer implements EdgeVertexTileRenderer.EdgeVertexRenderer {

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
        if (e instanceof PatternHop) {
            PatternHop patternHop = (PatternHop) e;
            /*if (patternHop.getMode().equals(TraverseMode.RAIL) || patternHop.getMode().equals(TraverseMode.BUS)) {
                return false;
            }*/
            attrs.color = modes.get(patternHop.getMode());
            attrs.label = patternHop.getMode() + " " +patternHop.getName();
            return true;
        }
        return false;
    }

    @Override public boolean renderVertex(Vertex v,
        EdgeVertexTileRenderer.VertexVisualAttributes attrs) {
        return false;
    }

    @Override public String getName() {
        return "patternHop render";
    }
}
