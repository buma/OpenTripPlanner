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

package org.opentripplanner.inspector;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import java.awt.*;

/**
 * Renders traffic lights on edges (when tagging traffic lights on way nodes) and intersections
 * as dark red. It is used for debugging traffic lights problems
 *
 * @author mabu
 */
public class TrafficLightsEdgeRenderer implements EdgeVertexRenderer {

    private static final Color TRAFFIC_LIGHT_COLOR = new Color(120, 60, 60);

    @Override public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
        if (e instanceof StreetEdge) {
            StreetEdge pse = (StreetEdge) e;
            if (pse.isWayTrafficLight()) {
                attrs.color = TRAFFIC_LIGHT_COLOR;
                attrs.label = pse.getName();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean renderVertex(Vertex v,
        VertexVisualAttributes attrs) {
        if (v instanceof IntersectionVertex) {
            if (((IntersectionVertex) v).trafficLight) {
                attrs.color = TRAFFIC_LIGHT_COLOR;
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "Trafic lights renderer";
    }
}
