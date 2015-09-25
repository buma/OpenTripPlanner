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

import org.joda.time.LocalDate;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;

/**
 * Created by mabu on 25.9.2015.
 */
public class TransportNetworkContext {

    private static final Logger LOG = LoggerFactory.getLogger(TransportNetworkContext.class);

    private static final double RADIUS_METERS = 300;

    /* FINAL FIELDS */

    private final Split fromSplit;

    private final Split toSplit;

    public RoutingRequest opt; // not final so we can reverse-clone

    public final TransportNetwork transportNetwork;

    public final VertexStore.Vertex fromVertex;

    public final VertexStore.Vertex toVertex;

    // origin means "where the initial state will be located" not "the beginning of the trip from the user's perspective"
    public final Split origin;

    // target means "where this search will terminate" not "the end of the trip from the user's perspective"
    public final Split target;

    private final BitSet servicesActive;

    /** An object that accumulates profiling and debugging info for inclusion in the response. */
    public DebugOutput debugOutput = new DebugOutput();

    /** Indicates that the search timed out or was otherwise aborted. */
    public boolean aborted;

    /** Indicates that a maximum slope constraint was specified but was removed during routing to produce a result. */
    public boolean slopeRestrictionRemoved = false;

    public TransportNetworkContext(RoutingRequest options, TransportNetwork transportNetwork) {
        this(options, transportNetwork, null, null, true);
    }

    public TransportNetworkContext(RoutingRequest routingRequest, TransportNetwork transportNetwork,
        VertexStore.Vertex fromVertex, VertexStore.Vertex toVertex, boolean findPlaces) {
        if (transportNetwork == null) {
            throw new GraphNotFoundException();
        }
        this.opt = routingRequest;
        this.transportNetwork = transportNetwork;
        this.debugOutput.startedCalculating();
        if (opt.modes.isTransit()) {
            Date date = routingRequest.getDateTime();
            //something about realtime stuff
            //TODO: check if timezones are OK
            LocalDate searchDate = LocalDate.fromDateFields(date);
            LOG.info("Local date:{}", searchDate);
            servicesActive = this.transportNetwork.transitLayer.getActiveServicesForDate(searchDate);
        } else {
            servicesActive = null;
        }

        if (findPlaces) {
            if (opt.batch) {
                this.toSplit = this.fromSplit = null;
                if (opt.arriveBy) {
                    //TODO: find toVertex with samples
                    this.toVertex = null;
                    this.fromVertex = null;

                } else {
                    //TODO: find fromVertex with samples
                    this.fromVertex = null;
                    this.toVertex = null;
                }
            } else {
                toSplit = this.transportNetwork.streetLayer.findSplit(opt.to.lat, opt.to.lng, RADIUS_METERS);
                fromSplit = this.transportNetwork.streetLayer.findSplit(opt.from.lat, opt.from.lng, RADIUS_METERS);
                this.fromVertex = this.toVertex = null;
            }
        } else {
            this.fromVertex = fromVertex;
            this.toVertex = toVertex;
            this.toSplit = this.fromSplit = null;
        }

        origin = opt.arriveBy ? toSplit : fromSplit;
        target = opt.arriveBy ? fromSplit : toSplit;

        if (this.origin != null) {
            LOG.info("Origin split:{}", this.origin);
        }

        if (this.target != null) {
            LOG.info("Target split:{}", this.target);
        }

    }



    public void check() {
        ArrayList<String> notFound = new ArrayList<String>();
        // check origin present when not doing an arrive-by batch search
        if (!(opt.batch && opt.arriveBy))
            if (fromSplit == null)
                notFound.add("from");

        // check destination present when not doing a depart-after batch search
        if (!opt.batch || opt.arriveBy) {
            if (toSplit == null) {
                notFound.add("to");
            }
        }
        if (notFound.size() > 0) {
            throw new VertexNotFoundException(notFound);
        }

        if (opt.modes.isTransit() && servicesActive.cardinality() > 0) {
            // user wants a path through the transit network,
            // but the date provided is outside those covered by the transit feed.
            throw new TransitTimesException();
        }
    }
}
