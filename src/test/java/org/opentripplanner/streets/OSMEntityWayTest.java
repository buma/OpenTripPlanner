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
import org.opentripplanner.streets.permissions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

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

        PortlandPermissionsSetSource portlandPermissionsSetSource = new PortlandPermissionsSetSource();
        newWayPropertySet = portlandPermissionsSetSource.getWayPropertySet();

        accessRestrictionsAlgorithm = new AccessRestrictionsAlgorithm(portlandPermissionsSetSource, portlandPermissionsSetSource);

        LOG.info("Current default highway permissions:");
        for (Map.Entry<OSMSpecifier, TransportModePermissions> entry : portlandPermissionsSetSource.getPermissionsForRoadType().entrySet()) {
            LOG.info("TAG:{} modes:{}", entry.getKey(), entry.getValue().getNonInheritedPermissions());
        }

    }

    private P2<StreetTraversalPermission> getWayProperties(
        IOSMWay way) {
        StreetTraversalPermission permissions = getWayPermissions(way);
        return OSMFilter.getPermissions(permissions, way);
    }

    private StreetTraversalPermission getWayPermissions(
        IOSMWay way) {
        WayProperties wayData = wayPropertySet.getDataForWay(way);
        return OSMFilter
            .getPermissionsForWay(way, wayData.getPermission(), null);
    }

    private StreetTraversalPermission calculateWayPermissions(IOSMWay way) {
        EnumMap<TransportModeType, Set<OSMAccessPermissions>> mapPermissions = accessRestrictionsAlgorithm
        .calculateWayPermissions(way);
        return AccessRestrictionsAlgorithm.convertPermission(mapPermissions);
    }

    private P2<StreetTraversalPermission> calculateWayProperties(
        IOSMWay way
    ) {
        P2<EnumMap<TransportModeType, Set<OSMAccessPermissions>>> permission = accessRestrictionsAlgorithm
            .getPermissions(way);
        return new P2<>(AccessRestrictionsAlgorithm.convertPermission(permission.first),
            AccessRestrictionsAlgorithm.convertPermission(permission.second));
    }

    /*private P2<StreetTraversalPermission> calculateWayPermissions(

    )*/

    @Test
    public void testCyclewayPermissions() throws Exception {
        OSMWay osmWay = new OSMWay();
        osmWay.addTag("highway", "cycleway");
        assertEquals(getWayPermissions(osmWay), calculateWayPermissions(osmWay));

        osmWay.addTag("access", "destination");
        P2<EnumMap<TransportModeType, Set<OSMAccessPermissions>>> permission = accessRestrictionsAlgorithm
            .getPermissions(osmWay);
        LOG.info("Way:{} Perm:{}", osmWay.getTags(), permission);
        EnumMap<TransportModeType, Set<OSMAccessPermissions>> expected_permission = new EnumMap<>(
            TransportModeType.class);
        //Foot and bicycle permissions are from default cycleway permission
        expected_permission.put(TransportModeType.FOOT, OSMAccessPermissions.yes);
        expected_permission.put(TransportModeType.BICYCLE, OSMAccessPermissions.designated);
        //CAR permissions are because access is destination
        expected_permission.put(TransportModeType.MOTORCAR, EnumSet
            .of(OSMAccessPermissions.NO_THRU_TRAFFIC, OSMAccessPermissions.DESTINATION,
                OSMAccessPermissions.CAN_TRAVERSE));
        assertEquals(expected_permission, permission.first);
        assertEquals(expected_permission, permission.second);

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

    @Test
    public void testPath() throws Exception {
        OSMWay osmWay = makeOSMWayFromTags("highway=path;access=private");

        P2<EnumMap<TransportModeType, Set<OSMAccessPermissions>>> permission = accessRestrictionsAlgorithm
            .getPermissions(osmWay);
        LOG.info("Way:{} Perm:{}", osmWay.getTags(), permission);
        EnumMap<TransportModeType, Set<OSMAccessPermissions>> expected_permission = new EnumMap<>(
            TransportModeType.class);
        //Foot and bicycle permissions are from default path permission
        expected_permission.put(TransportModeType.FOOT, OSMAccessPermissions.yes);
        expected_permission.put(TransportModeType.BICYCLE, OSMAccessPermissions.yes);
        //CAR permissions are because access is private
        expected_permission.put(TransportModeType.MOTORCAR, EnumSet
            .of(OSMAccessPermissions.NO_THRU_TRAFFIC, OSMAccessPermissions.PRIVATE,
                OSMAccessPermissions.CAN_TRAVERSE));
        assertEquals(expected_permission, permission.first);
        assertEquals(expected_permission, permission.second);
    }

    @Test
    public void testPlatform() throws Exception {
        OSMWay osmWay = makeOSMWayFromTags("highway=platform;public_transport=platform");
        final P2<StreetTraversalPermission> wayProperties = getWayProperties(osmWay);
        LOG.info("Platform:{}", wayProperties);
        assertEquals(wayProperties, calculateWayProperties(osmWay));
    }

    @Test public void testPrivate() throws Exception {
        OSMWay osmWay = makeOSMWayFromTags("highway=residential;access=private;surface=asphalt");
        final P2<StreetTraversalPermission> wayProperties = getWayProperties(osmWay);
        LOG.info("Private:{}", wayProperties);
        assertEquals(wayProperties, calculateWayProperties(osmWay));

    }

    @Test
    public void testSidewalk() throws Exception {
        OSMWay osmWay = new OSMWay();
        osmWay.addTag("highway", "footway");
        P2<StreetTraversalPermission> wayProperties = getWayProperties(osmWay);
        LOG.info("Footway:{}", wayProperties);
        assertEquals(wayProperties, calculateWayProperties(osmWay));

        osmWay = makeOSMWayFromTags("footway=sidewalk;highway=footway");
        wayProperties = getWayProperties(osmWay);
        LOG.info("Sidewalk:{}", wayProperties);
        assertEquals(wayProperties, calculateWayProperties(osmWay));
    }


    private static P2<StreetTraversalPermission> makePermissionsFromString(String orig_permissions) {
        String permissions = orig_permissions.replace("P2(", "").replace(")", "");
        String[] permission = permissions.split(",", 2);
        StreetTraversalPermission first = StreetTraversalPermission.valueOf(permission[0].trim());
        StreetTraversalPermission second = StreetTraversalPermission.valueOf(permission[1].trim());

        P2<StreetTraversalPermission> P2persmisions = P2.createPair(first, second);

        if (!P2persmisions.toString().equals(orig_permissions)) {
            throw new IllegalArgumentException("Permissions couldn't be converted to P2: " + permissions);
        }
        return P2persmisions;
    }

    private static OSMWay makeOSMWayFromTags(String tags) {
        OSMWay osmWay = new OSMWay();
        String[] pairs = tags.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            osmWay.addTag(kv[0], kv[1]);
        }
        return osmWay;
    }
}