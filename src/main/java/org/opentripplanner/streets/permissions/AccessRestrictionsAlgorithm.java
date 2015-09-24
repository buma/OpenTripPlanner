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
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.module.osm.WayProperties;
import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.opentripplanner.openstreetmap.model.IOSMWay;
import org.opentripplanner.openstreetmap.model.IOSMWithTags;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.streets.OSMWay;
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
    final TransportModeTreeItem transportModeHierarchy;
    final WayPropertySet wayPropertySet;
    final TransportModePermissions defaultTransportModePermissions;

    public AccessRestrictionsAlgorithm(WayPropertySetSource wayPropertySetSource, TransportModeHierarchyTree transportModeHierarchyTree) {
        this(wayPropertySetSource.getWayPropertySet(), transportModeHierarchyTree.getTransportModeHierarchyTree());
    }

    public AccessRestrictionsAlgorithm(WayPropertySet wayPropertySet,
        TransportModeTreeItem transportModeHierarchyTree) {
        this.wayPropertySet = wayPropertySet;
        this.transportModeHierarchy = transportModeHierarchyTree;
        defaultTransportModePermissions = getDefaultPermissions();
    }

    private TransportModePermissions getDefaultPermissions() {
        OSMWay osmWay = new OSMWay();
        osmWay.addTag("highway", "road");
        return wayPropertySet.getDataForWay(osmWay).getModePermissions();
    }

    private void reset() {
        TransportModeTreeItem root = (TransportModeTreeItem) transportModeHierarchy.getRoot();
        for (Enumeration bfsTree = root.breadthFirstEnumeration(); bfsTree.hasMoreElements();) {
            TransportModeTreeItem currentLeaf = (TransportModeTreeItem) bfsTree.nextElement();
            currentLeaf.setPermission(OSMAccessPermissions.UNKNOWN);
        }
    }

    public EnumMap<TransportModeType, OSMAccessPermissions> calculateWayPermissions(IOSMWay way) {
        reset();

        WayProperties wayData = wayPropertySet.getDataForWay(way);
        LOG.info("Tags:{}", way.getTags());
        EnumMap<TransportModeType, OSMAccessPermissions> nonInheritedPermissions;
        if (wayData == null) {
            LOG.warn("No waydata for road:{}", way.getTags());
            nonInheritedPermissions = defaultTransportModePermissions.getNonInheritedPermissions();
        } else if( wayData.getModePermissions() == null) {
            LOG.warn("Waydata mode permissions is null:{}", way.getTags());
            nonInheritedPermissions = defaultTransportModePermissions.getNonInheritedPermissions();
        } else {
            nonInheritedPermissions = wayData.getModePermissions().getNonInheritedPermissions();
        }
        //Gets specific access for highway of way based on provided wayPropertySetSource
        LOG.info("wayData: {}", wayData.getModePermissions());

        TransportModeTreeItem tree = transportModeHierarchy;

        //we label access as Unknown which is default
        TransportModeTreeItem root = (TransportModeTreeItem) tree.getRoot();
        LOG.info("ROOT: {}", root);
        root.setPermission(OSMAccessPermissions.UNKNOWN);

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
            if (transportModeType == TransportModeType.BICYCLE && tag.value.toLowerCase().equals("use_sidepath")) {
                specificPermissions.put(transportModeType, OSMAccessPermissions.NO);
                continue;
            }
            try {
                OSMAccessPermissions osmAccessPermissions = OSMAccessPermissions
                    .valueOf(tag.value.toUpperCase(Locale.ENGLISH));
                specificPermissions.put(transportModeType, osmAccessPermissions);
                LOG.info("Added {} -> {}", transportModeType, osmAccessPermissions);
            } catch (IllegalArgumentException ial) {
                LOG.warn("\"{}\" is not valid OSM access permission for {}", tag.value, tag.key);
            }
        }

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
            permissionsMap.put(map.getKey(), permissions);
        }

        return permissionsMap;
    }

    //TODO: it would be much smarted to do this when we still have a labeled tree
    public P2<EnumMap<TransportModeType, OSMAccessPermissions>> getPermissions(IOSMWay way) {
        EnumMap<TransportModeType, OSMAccessPermissions> permissions = calculateWayPermissions(way);

        EnumMap<TransportModeType, OSMAccessPermissions> permissionsFront = new EnumMap<>(permissions);
        EnumMap<TransportModeType, OSMAccessPermissions> permissionsBack = new EnumMap<>(permissions);

        if (way.isOneWayForwardDriving() || way.isRoundabout()) {
            permissionsBack.replace(TransportModeType.BICYCLE, OSMAccessPermissions.NO);
            permissionsBack.replace(TransportModeType.MOTORCAR, OSMAccessPermissions.NO);
        }

        if (way.isOneWayReverseDriving()) {
            permissionsFront.replace(TransportModeType.BICYCLE, OSMAccessPermissions.NO);
            permissionsFront.replace(TransportModeType.MOTORCAR, OSMAccessPermissions.NO);
        }

        if (way.isOneWayForwardBicycle()) {
            permissionsBack.replace(TransportModeType.BICYCLE, OSMAccessPermissions.NO);
        }

        if (way.isOneWayReverseBicycle()) {
            permissionsFront.replace(TransportModeType.BICYCLE, OSMAccessPermissions.NO);
        }

        // TODO(flamholz): figure out what this is for.
        String oneWayBicycle = way.getTag("oneway:bicycle");
        if (IOSMWithTags.isFalse(oneWayBicycle) || way.isTagTrue("bicycle:backwards")) {
            if (permissions.get(TransportModeType.BICYCLE)==OSMAccessPermissions.YES) {
                permissionsFront.replace(TransportModeType.BICYCLE, OSMAccessPermissions.YES);
                permissionsBack.replace(TransportModeType.BICYCLE, OSMAccessPermissions.YES);
            }
        }

        //This needs to be after adding permissions for oneway:bicycle=no
        //removes bicycle permission when bicycles need to use sidepath
        //TAG: bicycle:forward=use_sidepath
        if (way.isForwardDirectionSidepath()) {
            permissionsFront.replace(TransportModeType.BICYCLE, OSMAccessPermissions.NO);
        }

        //TAG bicycle:backward=use_sidepath
        if (way.isReverseDirectionSidepath()) {
            permissionsBack.replace(TransportModeType.BICYCLE, OSMAccessPermissions.NO);
        }

        if (way.isOpposableCycleway()) {
            permissionsBack.replace(TransportModeType.BICYCLE, OSMAccessPermissions.YES);
        }

        //TODO: sidewalk support

        return new P2<EnumMap<TransportModeType, OSMAccessPermissions>>(permissionsFront, permissionsBack);
    }



    /**
     * Converts permissions from map to StreetTraversalPermission which is used in tests
     * @param mapPermissions
     * @return
     */
    public static StreetTraversalPermission convertPermission(
        EnumMap<TransportModeType, OSMAccessPermissions> mapPermissions) {
        StreetTraversalPermission permission = StreetTraversalPermission.NONE;
        for (final Map.Entry<TransportModeType, OSMAccessPermissions> map : mapPermissions.entrySet()) {
            OSMAccessPermissions permissions = map.getValue();
            if (map.getKey() == TransportModeType.FOOT) {
                if (!(permissions == OSMAccessPermissions.NO || permissions == OSMAccessPermissions.PRIVATE)) {
                    permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
                }
            } else if (map.getKey() == TransportModeType.BICYCLE) {
                if (!(permissions == OSMAccessPermissions.NO || permissions == OSMAccessPermissions.PRIVATE ||
                    permissions == OSMAccessPermissions.DISMOUNT)) {
                    permission = permission.add(StreetTraversalPermission.BICYCLE);
                }
            } else if (map.getKey() == TransportModeType.MOTORCAR) {
                if (!(permissions == OSMAccessPermissions.NO || permissions == OSMAccessPermissions.PRIVATE)) {
                    permission = permission.add(StreetTraversalPermission.CAR);
                }
            }
        }
        return permission;
    }
}
