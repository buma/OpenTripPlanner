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

package org.opentripplanner.streets.permissions;

import java.util.*;

/**
 * Class saves transport mode permissions for specific highway type(s).
 *
 * It is used to calculate OSM access permissions.
 *
 * Created by mabu on 18.9.2015.
 */
public class TransportModePermissions {

    private EnumMap<TransportModeType, Set<OSMAccessPermissions>> permissionsEnumMap;

    public TransportModePermissions() {
        this.permissionsEnumMap = new EnumMap<>(TransportModeType.class);
    }

    public static TransportModePermissions makeFromArray(
        TransportModePermission[] motorwayPermissionsArray) {
        TransportModePermissions permissions = new TransportModePermissions();
        for (TransportModePermission permission: motorwayPermissionsArray) {
            permissions.add(permission.getTransportModeType(), permission.getOsmAccessPermissions());
        }
        //TODO: add option to add implied tags
        return permissions;
    }

    /**
     * Adds permissions for one transportModeType
     * @param transportModeType for which transport mode is this permissions
     * @param osmAccessPermissions YES, NO, DESTINATION, INHERITED YES etc
     */
    public void add(TransportModeType transportModeType, Set<OSMAccessPermissions> osmAccessPermissions) {
        permissionsEnumMap.put(transportModeType, osmAccessPermissions);
    }

    /**
     * Adds permissions for multiple transportModeTypes
     * @param transportModeTypes for which transport modes is this permissions
     * @param osmAccessPermissions YES, NO, DESTINATION, INHERITED YES etc
     */
    public void add(TransportModeType[] transportModeTypes, Set<OSMAccessPermissions> osmAccessPermissions) {
        for (TransportModeType transportModeType: transportModeTypes) {
            add(transportModeType, osmAccessPermissions);
        }
    }

    @Override
    public String toString() {
        return "TransportModePermissions{" +
            "permissionsEnumMap=" + permissionsEnumMap +
            '}';
    }

    /**
     * @return map of all transportModeTypes and permissions which aren't INHERITED_*
     */
    public EnumMap<TransportModeType, Set<OSMAccessPermissions>> getNonInheritedPermissions() {
        EnumMap<TransportModeType, Set<OSMAccessPermissions>> nonInheritedMap = new EnumMap<>(TransportModeType.class);

        for (Map.Entry<TransportModeType, Set<OSMAccessPermissions>> map: permissionsEnumMap.entrySet()) {
            if (Collections.disjoint(map.getValue(), OSMAccessPermissions.inherited_any)) {
                nonInheritedMap.put(map.getKey(), map.getValue());
            }
        }
        return nonInheritedMap;
    }

    /**
     * Creates new TransportModePermissions from old TransportPermissions
     *
     * TransportModeTypes which exists in both are overwritten with values from new.
     *
     * It is used in country specific [Name of the Country]SetSource so that country specifics gets
     * overridden but permissions that aren't overridden are read from default.
     * @param oldTransportModePermissions Default Transport mode permissions which are overridden if they exists in current.
     */
    public void fromOld(TransportModePermissions oldTransportModePermissions) {
        for (Map.Entry<TransportModeType, Set<OSMAccessPermissions>> oldModePermissions: oldTransportModePermissions.permissionsEnumMap.entrySet()) {
            if (!permissionsEnumMap.containsKey(oldModePermissions.getKey())) {
                permissionsEnumMap.put(oldModePermissions.getKey(), oldModePermissions.getValue());
            }
        }
    }
}
