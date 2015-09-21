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

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Its a leaf in Transport Model Hierarchy tree.
 *
 * It is used in {@link AccessRestrictionsAlgorithm} to get access permissions for specific roads.
 * Created by mabu on 18.9.2015.
 */
public class TransportModeTreeItem extends DefaultMutableTreeNode {

    public TransportModeTreeItem(TransportModeType transportModeType, OSMAccessPermissions accessPermissions) {
        super(new TransportModePermission(transportModeType, accessPermissions));

    }

    public TransportModeTreeItem(TransportModeType transportModeType) {
        super(new TransportModePermission(transportModeType, OSMAccessPermissions.UNKNOWN));
    }

    /**
     * Sets permissions for current leaf
     */
    public void setPermission(OSMAccessPermissions osmAccessPermissions) {
        ((TransportModePermission)this.getUserObject()).setOsmAccessPermissions(
            osmAccessPermissions);
    }

    /**
     * @return permission from current leaf it can also be unknown
     */
    public OSMAccessPermissions getPermission() {
        return ((TransportModePermission)this.getUserObject()).getOsmAccessPermissions();
    }

    /**
     * @return transportModeType from current leaf it can also be access
     */
    public TransportModeType getTransportModeType() {
        return ((TransportModePermission)this.getUserObject()).getTransportModeType();
    }

    /**
     * Searches for valid permissions from current leaf up the tree until it finds valid one (non unknown)
     */
    public OSMAccessPermissions getFullPermission() {
        if (getTransportModeType()==TransportModeType.ACCESS || getPermission() != OSMAccessPermissions.UNKNOWN) {
            return getPermission();
        }
        return ((TransportModeTreeItem) getParent()).getFullPermission();
    }
}
