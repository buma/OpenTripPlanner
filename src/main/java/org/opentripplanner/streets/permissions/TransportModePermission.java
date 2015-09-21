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

/**
 * Created by mabu on 18.9.2015.
 */
public class TransportModePermission {
    private TransportModeType transportModeType;
    private OSMAccessPermissions osmAccessPermissions;
    public TransportModePermission(TransportModeType motorcar, OSMAccessPermissions yes) {
        this.transportModeType = motorcar;
        this.osmAccessPermissions = yes;
    }

    public TransportModeType getTransportModeType() {
        return transportModeType;
    }

    public OSMAccessPermissions getOsmAccessPermissions() {
        return osmAccessPermissions;
    }

    public void setTransportModeType(TransportModeType transportModeType) {
        this.transportModeType = transportModeType;
    }

    public void setOsmAccessPermissions(OSMAccessPermissions osmAccessPermissions) {
        this.osmAccessPermissions = osmAccessPermissions;
    }

    @Override public String toString() {
        return "MODE:" + transportModeType +
            ", osmAccessPermissions=" + osmAccessPermissions;
    }
}
