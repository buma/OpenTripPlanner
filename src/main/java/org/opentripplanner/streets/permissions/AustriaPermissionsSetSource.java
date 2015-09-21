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


import org.opentripplanner.graph_builder.module.osm.OSMSpecifier;
import org.opentripplanner.graph_builder.module.osm.WayProperties;
import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * This are default permissions for Austria from: https://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#Austria
 * Created by mabu on 18.9.2015.
 */
public class AustriaPermissionsSetSource extends CountryPermissionsSetSource {

    private static final Logger LOG = LoggerFactory.getLogger(AustriaPermissionsSetSource.class);

    @Override public WayPropertySet getWayPropertySet() {
        WayPropertySet wayPropertySet = super.getWayPropertySet();

        TransportModePermissions living_street = new TransportModePermissions();
        living_street.add(new TransportModeType[]{TransportModeType.MOTORCAR, TransportModeType.MOTORCYCLE, TransportModeType.HGV, TransportModeType.PSV, TransportModeType.MOPED}, OSMAccessPermissions.DESTINATION);
        living_street.add(
            new TransportModeType[] { TransportModeType.HORSE, TransportModeType.BICYCLE,
                TransportModeType.FOOT }, OSMAccessPermissions.YES);
        replaceProperties(wayPropertySet, "living_street", living_street);

        TransportModePermissions cycleway = new TransportModePermissions();
        cycleway.add(
            new TransportModeType[] { TransportModeType.MOTORCAR, TransportModeType.MOTORCYCLE,
                TransportModeType.HGV, TransportModeType.PSV, TransportModeType.MOPED,
                TransportModeType.HORSE, TransportModeType.FOOT }, OSMAccessPermissions.NO);
        cycleway.add(TransportModeType.BICYCLE, OSMAccessPermissions.DESIGNATED);
        replaceProperties(wayPropertySet, "cycleway", cycleway);

        return wayPropertySet;
    }

    /**
     * Updates existing properties with country specific ones.
     *
     * @param props
     * @param tagSpecifier
     * @param tagPermissions
     */
    private void replaceProperties(WayPropertySet props, String tagSpecifier,
        TransportModePermissions tagPermissions) {
        WayProperties properties = new WayProperties();
        properties.setModePermissions(tagPermissions);
        Collection<OSMSpecifier> specifiers = getSpecifiers(tagSpecifier);
        for (OSMSpecifier osmSpecifier: specifiers) {
            props.replaceProperties(osmSpecifier, properties);
        }
    }
}
