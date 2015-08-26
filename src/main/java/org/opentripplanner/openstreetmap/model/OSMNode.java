/*
 Copyright 2008 Brian Ferris

 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.openstreetmap.model;

public class OSMNode extends OSMWithTags {

    public double lat;
    public double lon;

    public String toString() {
        return "osm node " + id;
    }

    /**
     * Returns the capacity of this node if defined, or 0.
     * 
     * @return
     */
    public int getCapacity() throws NumberFormatException {
        String capacity = getTag("capacity");
        if (capacity == null) {
            return 0;
        }
        
        return Integer.parseInt(getTag("capacity"));
    }

    /**
     * Is this a multi-level node that should be decomposed to multiple coincident nodes? Currently returns true only for elevators.
     * 
     * @return whether the node is multi-level
     * @author mattwigway
     */
    public boolean isMultiLevel() {
        return hasTag("highway") && "elevator".equals(getTag("highway"));
    }
    
    public boolean hasTrafficLight() {
        return hasTag("highway") && "traffic_signals".equals(getTag("highway"));
    }

    /**
     * Gets direction of traffic lights set on a node
     *
     * This is used with tag all incoming ways type of tagging. Usually traffic lights are tagged on
     * incoming (to the intersection) oneway ways. Or on dual way ways with direction tags.
     *
     * This can be forward or backward. If it is empty or direction is missing. Direction "both" is assumed.
     *
     * @return {@link OSMTrafficLightDirection} with traffic signals direction
     * @throws Exception if direction is not forward/backward
     */
    public OSMTrafficLightDirection getTrafficLightDirection() throws Exception {
        if (!hasTag("traffic_signals:direction")) {
            return OSMTrafficLightDirection.BOTH;
        }
        final String traffic_signals_direction = getTag("traffic_signals:direction");
        if (traffic_signals_direction.trim().isEmpty()) {
            return OSMTrafficLightDirection.BOTH;
        }
        switch (traffic_signals_direction) {
        case "forward":
            return OSMTrafficLightDirection.FORWARD;
        case "backward":
            return OSMTrafficLightDirection.BACKWARD;
        default:
            throw new Exception("Strange traffic_signals:direction value: \"" + traffic_signals_direction + "\" ignoring node.");
        }
    }

    /**
     * Is this a public transport stop that can be linked to a transit stop vertex later on.
     *
     * @return whether the node is a transit stop
     * @author hannesj
     */
    public boolean isStop() {
        return "bus_stop".equals(getTag("highway"))
                || "tram_stop".equals(getTag("railway"))
                || "station".equals(getTag("railway"))
                || "halt".equals(getTag("railway"))
                || "bus_station".equals(getTag("amenity"));
    }

    /**
     * TODO Maybe all those methods (isSomething...) in OSMXxx should be moved to a dedicated OSM
     * filtering class.
     * 
     * @return True if this node is a bike rental station.
     */
    public boolean isBikeRental() {
        return isTag("amenity", "bicycle_rental");
    }
}
