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

package org.opentripplanner.standalone;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.opentripplanner.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by mabu on 10.9.2015.
 */
public class OTPServerWithNetworks {
    private static final Logger LOG = LoggerFactory.getLogger(OTPServerWithNetworks.class);

    public CommandLineParameters params;

    public TransportNetwork transportNetwork = null;

    public OTPServerWithNetworks(CommandLineParameters params) {
        LOG.info("Wiring up and configuring transit networks server.");

        this.params = params;

    }

    /**
     * @return A list of all router IDs currently available.
     */
    public Collection<String> getRouterIds() {
        return Arrays.asList("default");
    }

    /*public Router getRouter(String routerId) throws GraphNotFoundException {
        //return graphService.getRouter(routerId);
    }*/

    /**
     * Return an HK2 Binder that injects this specific OTPServer instance into Jersey web resources.
     * This should be registered in the ResourceConfig (Jersey) or Application (JAX-RS) as a singleton.
     * Jersey forces us to use injection to get application context into HTTP method handlers, but in OTP we always
     * just inject this OTPServer instance and grab anything else we need (routers, graphs, application components)
     * from this single object.
     *
     * More on custom injection in Jersey 2:
     * http://jersey.576304.n2.nabble.com/Custom-providers-in-Jersey-2-tp7580699p7580715.html
     */
    public AbstractBinder makeBinder() {
        return new AbstractBinder() {
            @Override
            protected void configure() {
                bind(OTPServerWithNetworks.this).to(OTPServerWithNetworks.class);
            }
        };
    }
}
