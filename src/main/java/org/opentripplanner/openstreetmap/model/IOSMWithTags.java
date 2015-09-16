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

package org.opentripplanner.openstreetmap.model;

/**
 * Interface for {@link OSMWithTags} so that same functions for getting
 * OSM street speed and permissions can be used from {@link com.conveyal.osmlib.OSMEntity}
 * in Transport network and {@link OSMWithTags} in Graph mode.
 *
 * Currently supports enough methods that speeds can be read in TransportNetwork.
 */
public interface IOSMWithTags {
    /** @return a tag's value, converted to lower case. */
    String getTag(String tag);

    /**
     * Is the tag defined?
     */
    boolean hasTag(String tag);
}
