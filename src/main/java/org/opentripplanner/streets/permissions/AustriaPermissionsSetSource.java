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


import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This are default permissions for Austria from: https://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#Austria
 * Created by mabu on 18.9.2015.
 */
public class AustriaPermissionsSetSource extends CountryPermissionsSetSource implements
    WayPropertySetSource {

    private static final Logger LOG = LoggerFactory.getLogger(AustriaPermissionsSetSource.class);

    private final WayPropertySet wayPropertySet;




    public AustriaPermissionsSetSource() {
        super();

        wayPropertySet = new WayPropertySet();
        TransportModePermissions living_street = new TransportModePermissions();
        living_street.add(new TransportModeType[]{TransportModeType.MOTORCAR, TransportModeType.MOTORCYCLE, TransportModeType.HGV, TransportModeType.PSV, TransportModeType.MOPED}, OSMAccessPermissions.DESTINATION);
        living_street.add(
            new TransportModeType[] { TransportModeType.HORSE, TransportModeType.BICYCLE,
                TransportModeType.FOOT }, OSMAccessPermissions.YES);

        TransportModePermissions cycleway = new TransportModePermissions();
        cycleway.add(
            new TransportModeType[] { TransportModeType.MOTORCAR, TransportModeType.MOTORCYCLE,
                TransportModeType.HGV, TransportModeType.PSV, TransportModeType.MOPED,
                TransportModeType.HORSE, TransportModeType.FOOT }, OSMAccessPermissions.NO);
        cycleway.add(TransportModeType.BICYCLE, OSMAccessPermissions.DESIGNATED);


        replaceProperties("living_street", living_street);
        replaceProperties("cycleway", cycleway);
    }



    @Override
    public WayPropertySet getWayPropertySet() {
        fillWayPropertySet(this.wayPropertySet);
        return wayPropertySet;
    }
}
