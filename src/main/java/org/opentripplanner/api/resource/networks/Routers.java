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

package org.opentripplanner.api.resource.networks;

import org.opentripplanner.api.model.RouterInfo;
import org.opentripplanner.api.model.RouterList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by mabu on 9.9.2015.
 */
@Path("/routers")
@PermitAll
public class Routers {
    private static final Logger LOG = LoggerFactory.getLogger(Routers.class);

    /** Quality value prioritizes MIME types */
    static final String Q = ";qs=0.5";

    //Context with some kind of data is missing.

    /**
     * Returns a list of routers and their bounds.
     * @return a representation of the graphs and their geographic bounds, in JSON or XML depending
     * on the Accept header in the HTTP request.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public RouterList getRouterIds() {
        RouterList routerList = new RouterList();
        RouterInfo routerInfo = new RouterInfo("default");
        routerList.routerInfo.add(routerInfo);
        return routerList;
    }
}
