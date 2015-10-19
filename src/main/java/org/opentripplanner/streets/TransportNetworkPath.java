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

package org.opentripplanner.streets;

import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * Created by mabu on 14.9.2015.
 */
public class TransportNetworkPath {

    private static final Logger LOG = LoggerFactory.getLogger(TransportNetworkPath.class);

    private final int lastWeight;

    public LinkedList<StreetRouter.State> states;

    public LinkedList<Integer> edges;

    private TransportNetwork transportNetwork;

    private StreetRouter.State lastState;

    //TODO maybe we'll need also optimize parameter or at least a parameter to tell if we route arrive by or depart from

    public TransportNetworkPath(StreetRouter.State s, TransportNetwork transportNetwork, boolean arriveBy) {
        //FIXME: temporary
        if (s == null) {
            throw new PathNotFoundException();
        }
        this.transportNetwork = transportNetwork;
        this.lastWeight = s.weight;
        edges = new LinkedList<>();
        states = new LinkedList<>();
        this.lastState = s;

        if (arriveBy) {
            this.lastState = s.reverse(transportNetwork);
        }


        /*
         * Starting from latest (time-wise) state, copy states to the head of a list in reverse
         * chronological order. List indices will thus increase forward in time, and backEdges will
         * be chronologically 'back' relative to their state.
         */
        for (StreetRouter.State cur = lastState; cur != null; cur = cur.backState) {
            states.addFirst(cur);
            if (cur.backEdge != -1 && cur.backState != null) {
                edges.addFirst(cur.backEdge);
            }
        }
    }

    public VertexStore.Vertex getStartVertex() {
        return states.getFirst().getVertex(transportNetwork);
    }

    public VertexStore.Vertex getEndVertex() {
        return states.getLast().getVertex(transportNetwork);
    }

    public TransportNetwork getTransportNetwork() {
        return transportNetwork;
    }

    public long getEndTime() {
        return 0;
    }

    public int getlastWeight() {
        return lastWeight;
    }
}
