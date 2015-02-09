/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package org.opentripplanner.graph_builder.impl.osm;

import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

/**
 *
 * @author mabu
 */
public class OSMWayIDNamer implements CustomNamer {

    private static final String name_format = "osm:way:%d:::%s";

    private WayPropertySet wayPropertySet;

    public OSMWayIDNamer(WayPropertySet wayPropertySet) {
        this.wayPropertySet = wayPropertySet;
    }

    @Override
    public String name(OSMWithTags way, String defaultName) {
        String default_name = wayPropertySet.getCreativeNameForWay(way);
        if (default_name == null) {
            default_name = defaultName;
        }
        return String.format(name_format, way.getId(), default_name);
    }

    @Override
    public void nameWithEdge(OSMWithTags way, StreetEdge edge) {
        String name = name(way, "");
        String default_name = wayPropertySet.getCreativeNameForWay(way);
        if (default_name == null) {
            default_name = "";
        }
        //This was previously just compared to edge names,
        //and labels were used as unique ID, because they were OSM ID,
        //but now because labels are gone name is Unique ID:::name and we have to somehow
        //find the type of edges
        //This is little hackish because this values can change depending on Locale setting
        switch(default_name) {
            case "path":
                edge.setPath(true);
                break;
            case "bike path":
                edge.setBikePath(true);
                break;
            case "footbridge":
                edge.setFootBridge(true);
                break;
            case "track":
                edge.setTrack(true);
                break;
        }
        edge.setName(name);
    }

    @Override
    public void postprocess(Graph graph) {    }
}
