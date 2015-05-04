package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;

public class TransitStopStreetVertex extends IntersectionVertex {

    public final String stopCode;

    public final TraverseMode mode;

    public TransitStopStreetVertex(Graph g, String label, double x, double y, String name,
        String stopCode, TraverseMode publicTransitType) {
        super(g, label, x, y, name);
        this.stopCode = stopCode;
        this.mode = publicTransitType;
    }
}
