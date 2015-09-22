/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.streets;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.module.osm.*;
import org.opentripplanner.openstreetmap.model.*;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.streets.permissions.AccessRestrictionsAlgorithm;
import org.opentripplanner.streets.permissions.OSMAccessPermissions;
import org.opentripplanner.streets.permissions.TransportModeType;
import org.opentripplanner.streets.permissions.USAPermissionsSetSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;

import static org.junit.Assert.*;

/**Tests of new access algorithm
 * Created by mabu on 18.9.2015.
 */
public class OSMEntityWayTest {

    private static final Logger LOG = LoggerFactory.getLogger(OSMEntityWayTest.class);
    private WayPropertySet wayPropertySet;
    private WayPropertySet newWayPropertySet;
    private AccessRestrictionsAlgorithm accessRestrictionsAlgorithm;

    @Before
    public void setUp() throws Exception {
        DefaultWayPropertySetSource defaultWayPropertySetSource = new DefaultWayPropertySetSource();
        wayPropertySet = defaultWayPropertySetSource.getWayPropertySet();

        USAPermissionsSetSource usaPermissionsSetSource = new USAPermissionsSetSource();
        newWayPropertySet = usaPermissionsSetSource.getWayPropertySet();

        accessRestrictionsAlgorithm = new AccessRestrictionsAlgorithm(usaPermissionsSetSource, usaPermissionsSetSource);

    }

    private P2<StreetTraversalPermission> getWayProperties(
        IOSMWay way) {
        StreetTraversalPermission permissions = getWayPermissions(way);
        return OSMFilter.getPermissions(permissions,
            way);
    }

    private StreetTraversalPermission getWayPermissions(
        IOSMWay way) {
        WayProperties wayData = wayPropertySet.getDataForWay(way);
        return OSMFilter
            .getPermissionsForWay(way, wayData.getPermission(), null);
    }

    private StreetTraversalPermission calculateWayPermissions(IOSMWay way) {
        EnumMap<TransportModeType, OSMAccessPermissions> mapPermissions = accessRestrictionsAlgorithm
        .calculateWayPermissions(way);
        return AccessRestrictionsAlgorithm.convertPermission(mapPermissions);
    }

    private P2<StreetTraversalPermission> calculateWayProperties(
        IOSMWay way
    ) {
        P2<EnumMap<TransportModeType, OSMAccessPermissions>> permission = accessRestrictionsAlgorithm
            .getPermissions(way);
        return new P2<StreetTraversalPermission>(AccessRestrictionsAlgorithm.convertPermission(permission.first), AccessRestrictionsAlgorithm.convertPermission(permission.second));
    }

    /*private P2<StreetTraversalPermission> calculateWayPermissions(

    )*/

    @Test
    public void testCyclewayPermissions() throws Exception {
        OSMWay osmWay = new OSMWay();
        osmWay.addTag("highway", "cycleway");
        assertEquals(getWayPermissions(osmWay), calculateWayPermissions(osmWay));

        osmWay.addTag("access", "destination");
        assertEquals(getWayPermissions(osmWay), calculateWayPermissions(osmWay));

    }

    @Test
    public void testOnewayPermissions() {
        OSMWay osmWay = new OSMWay();
        osmWay.addTag("highway", "residential");
        osmWay.addTag("oneway", "true");
        osmWay.addTag("oneway:bicycle", "no");

        final P2<StreetTraversalPermission> wayProperties = getWayProperties(osmWay);
        LOG.info("Oneway:{}", wayProperties);
        assertEquals(wayProperties, calculateWayProperties(osmWay));
    }

    @Test
    public void testLivingStreet() {
        OSMWay osmWay = new OSMWay();
        osmWay.addTag("highway", "living_street");
        assertEquals(StreetTraversalPermission.ALL, calculateWayPermissions(osmWay));
    }
}