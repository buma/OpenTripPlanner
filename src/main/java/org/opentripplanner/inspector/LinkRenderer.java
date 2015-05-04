package org.opentripplanner.inspector;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.ParkAndRideLinkEdge;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.routing.vertextype.TransitStation;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.awt.*;

/**
 * Created by mabu on 14.4.2015.
 */
public class LinkRenderer implements EdgeVertexTileRenderer.EdgeVertexRenderer {
    private static final Color LINK_COLOR_EDGE = Color.ORANGE;

    private static final Color TRANSIT_STOP_COLOR_VERTEX = new Color(0.0f, 0.0f, 0.8f);

    private static final Color TRANSIT_STATION_COLOR_VERTEX = new Color(0.4f, 0.0f, 0.8f);

    private static final Color BIKE_RENTAL_COLOR_VERTEX = new Color(0.0f, 0.7f, 0.0f);

    private static final Color PARK_AND_RIDE_COLOR_VERTEX = Color.RED;

    @Override
    public boolean renderEdge(Edge e, EdgeVertexTileRenderer.EdgeVisualAttributes attrs) {

    if (e instanceof StreetTransitLink) {
        attrs.color = LINK_COLOR_EDGE;
        attrs.label = "link";
    } else if (e instanceof StreetBikeRentalLink) {
        attrs.color = LINK_COLOR_EDGE;
        attrs.label = "link";
    } else if (e instanceof ParkAndRideLinkEdge) {
        attrs.color = LINK_COLOR_EDGE;
        attrs.label = "link";
    } else {
        return false;
    }
    return true;
    }

    @Override
    public boolean renderVertex(Vertex v, EdgeVertexTileRenderer.VertexVisualAttributes attrs) {

        if (v instanceof TransitStop) {
            if (!((TransitStop) v).getModes().contains(TraverseMode.FERRY)) {
                return false;
            }
            boolean alreadyLinked = false;
            TransitStop transitStop = (TransitStop) v;
            for (Edge e : transitStop.getOutgoing()) {
                if (e instanceof StreetTransitLink) {
                    alreadyLinked = true;
                    break;
                }
            }
            if (alreadyLinked) {
                attrs.color = TRANSIT_STOP_COLOR_VERTEX;
            } else {
                attrs.color = Color.RED;
            }
            attrs.label = "G" + (v.getName() != null ? v.getName() : v.getLabel());
            attrs.label += " (" + v.getOutgoingStreetEdges().size() + ")";
        } else if (v instanceof TransitStation) {
            attrs.color = TRANSIT_STATION_COLOR_VERTEX;
            attrs.label = v.getName();
        } else if (v instanceof BikeRentalStationVertex) {
            attrs.color = BIKE_RENTAL_COLOR_VERTEX;
            attrs.label = v.getName();
        } else if (v instanceof ParkAndRideVertex) {
            attrs.color = PARK_AND_RIDE_COLOR_VERTEX;
            attrs.label = v.getName();
        } else {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "Links renderer";
    }
}
