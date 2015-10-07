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

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.inspector.networks.TileRendererManager;
import org.opentripplanner.reflect.ReflectiveInitializer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mabu on 25.9.2015.
 */
public class Router {

    private static final Logger LOG = LoggerFactory.getLogger(Router.class);

    public static final String ROUTER_CONFIG_FILENAME = "router-config.json";

    public final String id;
    public final TransportNetwork transportNetwork;
    public double[] timeouts = {5, 2, 1, 0.5, 0.1};

    public TileRendererManager tileRendererManager;

    // A RoutingRequest containing default parameters that will be cloned when handling each request
    public TransportNetworkRequest defaultRoutingRequest;

    public Router(String id, TransportNetwork transportNetwork) {
        this.transportNetwork = transportNetwork;
        this.id = id;

        //FIXME: temporary since startup isn't yet called
        this.defaultRoutingRequest = new TransportNetworkRequest();
        this.tileRendererManager = new TileRendererManager(this.transportNetwork);
    }

    /**
     * Start up a new router once it has been created.
     * @param config The configuration (loaded from Graph.properties for example).
     */
    public void startup(JsonNode config) {

        this.tileRendererManager = new TileRendererManager(this.transportNetwork);

        /* Create the default router parameters from the JSON router config. */
        JsonNode routingDefaultsNode = config.get("routingDefaults");
        if (routingDefaultsNode != null) {
            LOG.info("Loading default routing parameters from JSON:");
            ReflectiveInitializer<TransportNetworkRequest> scraper = new ReflectiveInitializer(TransportNetworkRequest.class);
            this.defaultRoutingRequest = scraper.scrape(routingDefaultsNode);
        } else {
            LOG.info("No default routing parameters were found in the router config JSON. Using built-in OTP defaults.");
            this.defaultRoutingRequest = new TransportNetworkRequest();
        }

        /* Apply single timeout. */
        JsonNode timeout = config.get("timeout");
        if (timeout != null) {
            if (timeout.isNumber()) {
                this.timeouts = new double[]{timeout.doubleValue()};
            } else {
                LOG.error("The 'timeout' configuration option should be a number of seconds.");
            }
        }

        /* Apply multiple timeouts. */
        JsonNode timeouts = config.get("timeouts");
        if (timeouts != null) {
            if (timeouts.isArray() && timeouts.size() > 0) {
                this.timeouts = new double[timeouts.size()];
                int i = 0;
                for (JsonNode node : timeouts) {
                    this.timeouts[i++] = node.doubleValue();
                }
            } else {
                LOG.error("The 'timeouts' configuration option should be an array of values in seconds.");
            }
        }
        LOG.info("Timeouts for router '{}': {}", this.id, this.timeouts);

        //Calls graph updater
    }
}
