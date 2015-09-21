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
 * This interface returns Transport mode hierarchy which is used to calculate road access permissions
 *
 * Sample tree which is implemented in default {@link CountryPermissionsSetSource} is from
 * here: https://wiki.openstreetmap.org/wiki/File:TransportModeHierarchy.png
 *
 * If country doesn't implement different tree default one is used.
 * Some countries have horses as vehicles or some other changes.
 *
 * Created by mabu on 18.9.2015.
 */
public interface TransportModeHierarchyTree {
    TransportModeTreeItem getTransportModeHierarchyTree();
}
