/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.inspector;

import java.awt.Color;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PublicTransitEdge;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

/**
 *
 * @author mabu
 */
public class TransitStreetEdgeRenderer implements EdgeVertexRenderer  {
    
    private ScalarColorPalette palette = new DefaultScalarColorPalette(1.0, 3.0, 10.0);

    private static final Color BIKE_RENTAL_COLOR_VERTEX = new Color(0.0f, 0.7f, 0.0f);

    public TransitStreetEdgeRenderer() {
    }

    @Override
    public boolean renderEdge(Edge e, EdgeVertexTileRenderer.EdgeVisualAttributes attrs) {
        if (e instanceof StreetTransitLink) {
            StreetTransitLink ste = (StreetTransitLink) e;
            attrs.color = Color.BLUE;
            attrs.label = "link";
        } else if (e instanceof StreetBikeRentalLink) {
            StreetBikeRentalLink sbl = (StreetBikeRentalLink) e;
            attrs.color = Color.MAGENTA;
            attrs.label = "BR link";
        } else if (e instanceof PublicTransitEdge) {
            PublicTransitEdge publicTransitEdge = (PublicTransitEdge) e;
            if ((publicTransitEdge.getPublicTransitType() == TraverseMode.FERRY)) {
                attrs.color = Color.GREEN;
                attrs.label = publicTransitEdge.getName();
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean renderVertex(Vertex v, EdgeVertexTileRenderer.VertexVisualAttributes attrs) {
        if (v instanceof TransitStop) {
            attrs.color = Color.CYAN;
            attrs.label = v.getName();
        } else if (v instanceof BikeRentalStationVertex) {
            attrs.color = BIKE_RENTAL_COLOR_VERTEX;
            attrs.label = v.getName();
        /*} else if (v instanceof IntersectionVertex) {
            attrs.color = Color.DARK_GRAY;*/
        } else {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "Transit/Bikeshare links";
    }
}
