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

package org.opentripplanner.streets.permissions;

import com.conveyal.osmlib.OSMEntity;
import org.opentripplanner.graph_builder.module.osm.WayProperties;
import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.opentripplanner.openstreetmap.model.IOSMWay;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Access restriction algorithm itself
 *
 * It is implementation of this algorithm:
 * https://wiki.openstreetmap.org/wiki/Computing_access_restrictions#Algorithm
 *
 * First we take wayProperties for specific country to get default permissions for ways
 * and transport mode hierarchy tree for specific country.
 *
 * Then for each way we label the tree based on default permissions and specific access permissions in this way.
 * Result is labeled transport mode hierarchy tree. We can read the tree from wanted leaf up to get first leaf with specific permissions.
 * Created by mabu on 18.9.2015.
 */
public class AccessRestrictionsAlgorithm {
    private static final Logger LOG = LoggerFactory.getLogger(AccessRestrictionsAlgorithm.class);
    TransportModeTreeItem transportModeHierarchy;
    WayPropertySet wayPropertySet;

    public AccessRestrictionsAlgorithm(WayPropertySetSource wayPropertySetSource, TransportModeHierarchyTree transportModeHierarchyTree) {
        this.wayPropertySet = wayPropertySetSource.getWayPropertySet();
        this.transportModeHierarchy = transportModeHierarchyTree.getTransportModeHierarchyTree();
    }

    public StreetTraversalPermission calculateWayPermissions(IOSMWay way) {
        WayProperties wayData = wayPropertySet.getDataForWay(way);
        //Gets specific access for highway of way based on provided wayPropertySetSource
        LOG.info("wayData: {}", wayData.getModePermissions());

        TransportModeTreeItem tree = transportModeHierarchy;

        //we label access as Unknown which is default
        TransportModeTreeItem root = (TransportModeTreeItem) tree.getRoot();
        LOG.info("ROOT: {}", root);
        root.setPermission(OSMAccessPermissions.UNKNOWN);

        EnumMap<TransportModeType, OSMAccessPermissions> nonInheritedPermissions = wayData.getModePermissions().getNonInheritedPermissions();

        EnumMap<TransportModeType, TransportModeTreeItem> usefulTransportModes = new EnumMap<>(
            TransportModeType.class);

        for (Enumeration bfsTree = root.breadthFirstEnumeration(); bfsTree.hasMoreElements();) {
            TransportModeTreeItem currentLeaf = (TransportModeTreeItem) bfsTree.nextElement();
            TransportModeType currentTransportMode = currentLeaf.getTransportModeType();
            OSMAccessPermissions defaultHighwayTypePermission = nonInheritedPermissions.getOrDefault(currentTransportMode,
                OSMAccessPermissions.UNKNOWN);
            if (defaultHighwayTypePermission != OSMAccessPermissions.UNKNOWN) {
                currentLeaf.setPermission(defaultHighwayTypePermission);
            }
            LOG.info("Depth: {} info:{}", currentLeaf.getDepth(), currentLeaf);
            if (currentTransportMode == TransportModeType.FOOT
                || currentTransportMode == TransportModeType.BICYCLE
                || currentTransportMode == TransportModeType.MOTORCAR) {
                usefulTransportModes.put(currentTransportMode, currentLeaf);
            }
        }

        //TODO add changing of permissions for specific modes

        Collection<OSMEntity.Tag> permissionTags = way.getPermissionTags();
        EnumMap<TransportModeType, OSMAccessPermissions> specificPermissions = new EnumMap<TransportModeType, OSMAccessPermissions>(TransportModeType.class);
        for (final OSMEntity.Tag tag : permissionTags) {
            LOG.info("TAG:{}", tag);
            TransportModeType transportModeType = TransportModeType.valueOf(
                tag.key.toUpperCase(Locale.ENGLISH));
            try {
                OSMAccessPermissions osmAccessPermissions = OSMAccessPermissions
                    .valueOf(tag.value.toUpperCase(Locale.ENGLISH));
                specificPermissions.put(transportModeType, osmAccessPermissions);
                LOG.info("Added {} -> {}", transportModeType, osmAccessPermissions);
            } catch (IllegalArgumentException ial) {
                LOG.warn("\"{}\" is not valid OSM acces permission", tag.value);
            }
        }

        StreetTraversalPermission permission = StreetTraversalPermission.NONE;
        //Setting specific permission exceptions
        for (Enumeration bfsTree = root.breadthFirstEnumeration(); bfsTree.hasMoreElements();) {
            TransportModeTreeItem currentLeaf = (TransportModeTreeItem) bfsTree.nextElement();
            TransportModeType currentTransportMode = currentLeaf.getTransportModeType();
            OSMAccessPermissions defaultHighwayTypePermission = specificPermissions.getOrDefault(currentTransportMode,
                OSMAccessPermissions.UNKNOWN);
            if (defaultHighwayTypePermission != OSMAccessPermissions.UNKNOWN) {
                currentLeaf.setPermission(defaultHighwayTypePermission);
            }
            LOG.info("Depth: {} info:{}", currentLeaf.getDepth(), currentLeaf);
        }

        EnumMap<TransportModeType, OSMAccessPermissions> permissionsMap = new EnumMap<>(
            TransportModeType.class);

        //Getting permissions from the tree
        //We actually loose some information since we get information if mode is designated yes or dismount
        for (Map.Entry<TransportModeType, TransportModeTreeItem> map: usefulTransportModes.entrySet()) {
            OSMAccessPermissions permissions = map.getValue().getFullPermission();
            LOG.info("MODE:{} permission:{}", map.getKey(), permissions);
            if (map.getKey() == TransportModeType.FOOT) {
                if (permissions != OSMAccessPermissions.NO) {
                    permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
                }
            } else if(map.getKey() == TransportModeType.BICYCLE) {
                if (!(permissions == OSMAccessPermissions.NO || permissions == OSMAccessPermissions.DISMOUNT)) {
                    permission = permission.add(StreetTraversalPermission.BICYCLE);
                }
            } else if (map.getKey() == TransportModeType.MOTORCAR) {
                if (permissions != OSMAccessPermissions.NO) {
                    permission = permission.add(StreetTraversalPermission.CAR);
                }
            }
        }

        return permission;
    }
}
