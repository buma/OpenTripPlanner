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

import org.opentripplanner.graph_builder.module.osm.WayPropertySet;

/**
 * This changes some USA permissions so that they are the same as they were previously in OTP
 *
 * Changes:
 * - Trunk, trunk_link doesn't allow moped, horse, bicycle and foot
 * - highway=footway allows cycling
 * - highway=footway footway=sidewalk allows only foot
 * Created by mabu on 24.9.2015.
 */
public class PortlandPermissionsSetSource extends USAPermissionsSetSource {
    public PortlandPermissionsSetSource() {
        this(new WayPropertySet());
    }

    public PortlandPermissionsSetSource(WayPropertySet wayPropertySet) {
        super(wayPropertySet);

        TransportModePermissions trunkPermissions = new TransportModePermissions();

        trunkPermissions.add(
            new TransportModeType[] { TransportModeType.MOPED, TransportModeType.HORSE,
                TransportModeType.BICYCLE, TransportModeType.FOOT }, OSMAccessPermissions.no);

        TransportModePermissions footway = new TransportModePermissions();

        footway.add(TransportModeType.FOOT, OSMAccessPermissions.designated);
        footway.add(TransportModeType.BICYCLE, OSMAccessPermissions.yes);

        TransportModePermissions sidewalk = new TransportModePermissions();
        sidewalk.add(TransportModeType.ACCESS, OSMAccessPermissions.no);
        sidewalk.add(TransportModeType.FOOT, OSMAccessPermissions.designated);

        replaceProperties("trunk", trunkPermissions);
        replaceProperties("footway", footway);
        replaceProperties("footway=sidewalk;highway=footway", sidewalk);
    }
}
