package org.opentripplanner.graph_builder.module.map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.EdgeInfo;
import org.opentripplanner.routing.graph.Edge;

/**
 * Created by mabu on 13.4.2015.
 */
public class EdgePoint {
    private Edge edge;
    private Coordinate closestPoint;
    private TraverseMode traverseMode;
    private double minDistance;

    public EdgePoint(Edge closestEdge, LineString GTFSShape, TraverseMode traverseMode,
        double minDistance) {
        this.edge = closestEdge;
        this.closestPoint = GTFSShape.getCoordinateN(0);
        this.traverseMode = traverseMode;
        this.minDistance = minDistance;
    }

    public Edge getEdge() {
        return edge;
    }

    public Coordinate getClosestPoint() {
        return closestPoint;
    }

    public TraverseMode getTraverseMode() {
        return traverseMode;
    }

    public double getMinDistance() {
        return minDistance;
    }

    public Integer getLevel() {
        return ((EdgeInfo)edge).getLevel();
    }

    public EdgeInfo getEdgeInfo() {
        return (EdgeInfo) edge;
    }
}
