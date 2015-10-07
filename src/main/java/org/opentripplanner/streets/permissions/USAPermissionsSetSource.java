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
 * This are default roads permissions for USA from https://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#United_States_of_America
 * Created by mabu on 21.9.2015.
 */
public class USAPermissionsSetSource extends CountryPermissionsSetSource implements
    WayPropertySetSource {
    private static final Logger LOG = LoggerFactory.getLogger(USAPermissionsSetSource.class);

    private final WayPropertySet wayPropertySet;

    public USAPermissionsSetSource(WayPropertySet wayPropertySet) {
        super();
        this.wayPropertySet = wayPropertySet;

        TransportModePermissions motorwayPermissions = new TransportModePermissions();

        motorwayPermissions.add(
            new TransportModeType[] { TransportModeType.MOPED, TransportModeType.HORSE,
                TransportModeType.BICYCLE, TransportModeType.FOOT }, OSMAccessPermissions.no);


        TransportModePermissions pedestrian = new TransportModePermissions();

        pedestrian.add(new TransportModeType[]{ TransportModeType.FOOT, TransportModeType.BICYCLE},
            OSMAccessPermissions.yes);

        TransportModePermissions path = new TransportModePermissions();

        path.add(new TransportModeType[] {TransportModeType.MOPED, TransportModeType.HORSE, TransportModeType.BICYCLE,
            TransportModeType.FOOT }, OSMAccessPermissions.yes);

        TransportModePermissions bridleway = new TransportModePermissions();

        bridleway.add(TransportModeType.HORSE, OSMAccessPermissions.designated);
        bridleway.add(new TransportModeType[]{TransportModeType.BICYCLE, TransportModeType.FOOT}, OSMAccessPermissions.yes);

        TransportModePermissions cycleway = new TransportModePermissions();

        cycleway.add(TransportModeType.BICYCLE, OSMAccessPermissions.designated);
        cycleway.add(TransportModeType.FOOT, OSMAccessPermissions.yes);

        TransportModePermissions footway = new TransportModePermissions();

        footway.add(TransportModeType.FOOT, OSMAccessPermissions.designated);

        replaceProperties("motorway", motorwayPermissions);
        //Same as default
        /*replaceProperties(
            "trunk|primary|secondary|tertiary|unclassified|residential|living_street|road",
            otherStreets);*/
        replaceProperties("pedestrian", pedestrian);
        replaceProperties("path", path);
        replaceProperties("bridleway", bridleway);
        replaceProperties("cycleway", cycleway);
        replaceProperties("footway", footway);
    }

    public USAPermissionsSetSource() {
        this(new WayPropertySet());
    }

    @Override
    public WayPropertySet getWayPropertySet() {
        fillWayPropertySet(this.wayPropertySet);
        return wayPropertySet;
    }
}
