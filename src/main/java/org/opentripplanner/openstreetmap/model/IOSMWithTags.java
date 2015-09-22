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

import com.conveyal.osmlib.OSMEntity;

import java.util.Collection;
import java.util.Map;

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

    /**
     * Checks is a tag contains the specified value.
     */
    boolean isTag(String tag, String value);

    boolean doesTagAllowAccess(String tag);

    /**
     * Determines if a tag contains a false value. 'no', 'false', and '0' are considered false.
     */
    boolean isTagFalse(String tag);

    /**
     * Determines if a tag contains a true value. 'yes', 'true', and '1' are considered true.
     */
    boolean isTagTrue(String tag);

    static boolean isFalse(String tagValue) {
        return ("no".equals(tagValue) || "0".equals(tagValue) || "false".equals(tagValue));
    }

    static boolean isTrue(String tagValue) {
        return ("yes".equals(tagValue) || "1".equals(tagValue) || "true".equals(tagValue));
    }

    static boolean isTagAccessAllowed(String tagValue) {
        return ("designated".equals(tagValue) || "official".equals(tagValue)
            || "permissive".equals(tagValue) || "unknown".equals(tagValue));
    }

    /**
     * Returns true if this element is under construction.
     *
     * @return
     */
    default boolean isUnderConstruction() {
        String highway = getTag("highway");
        String cycleway = getTag("cycleway");
        return "construction".equals(highway) || "construction".equals(cycleway);
    }

    /**
     * Returns true if this tag is explicitly access to this entity.
     *
     * @param tagName
     * @return
     */
    default boolean isTagDeniedAccess(String tagName) {
        String tagValue = getTag(tagName);
        return "no".equals(tagValue) || "license".equals(tagValue);
    }

    /**
     * Returns true if access is generally denied to this element (potentially with exceptions).
     *
     * @return
     */
    default boolean isGeneralAccessDenied() {
        return isTagDeniedAccess("access");
    }

    /**
     * Returns true if cars are explicitly denied access.
     *
     * @return
     */
    default boolean isMotorcarExplicitlyDenied() {
        return isTagDeniedAccess("motorcar");
    }

    /**
     * Returns true if cars are explicitly allowed.
     *
     * @return
     */
    default boolean isMotorcarExplicitlyAllowed() {
        return doesTagAllowAccess("motorcar");
    }

    /**
     * Returns true if cars/motorcycles/HGV are explicitly denied access.
     *
     * @return
     */
    default boolean isMotorVehicleExplicitlyDenied() {
        return isTagDeniedAccess("motor_vehicle");
    }

    /**
     * Returns true if cars/motorcycles/HGV are explicitly allowed.
     *
     * @return
     */
    default boolean isMotorVehicleExplicitlyAllowed() {
        return doesTagAllowAccess("motor_vehicle");
    }


    /**
     * Returns true if bikes are explicitly denied access.
     *
     * bicycle is denied if bicycle:no, bicycle:license or bicycle:use_sidepath
     * @return
     */
    default boolean isBicycleExplicitlyDenied() {
        return isTagDeniedAccess("bicycle") || "use_sidepath".equals(getTag("bicycle"));
    }

    /**
     * Returns true if bikes are explicitly allowed.
     *
     * @return
     */
    default boolean isBicycleExplicitlyAllowed() {
        return doesTagAllowAccess("bicycle");
    }

    /**
     * Returns true if pedestrians are explicitly denied access.
     *
     * @return
     */
    default boolean isPedestrianExplicitlyDenied() {
        return isTagDeniedAccess("foot");
    }

    /**
     * Returns true if pedestrians are explicitly allowed.
     *
     * @return
     */
    default boolean isPedestrianExplicitlyAllowed() {
        return doesTagAllowAccess("foot");
    }

    /**
     * Returns true if through traffic is not allowed.
     *
     * @return
     */
   default boolean isThroughTrafficExplicitlyDisallowed() {
        String access = getTag("access");
        boolean noThruTraffic = "destination".equals(access) || "private".equals(access)
            || "customers".equals(access) || "delivery".equals(access)
            || "forestry".equals(access) || "agricultural".equals(access);
        return noThruTraffic;
    }

    /**
     * @return True if this node / area is a park and ride.
     */
    default boolean isParkAndRide() {
        String parkingType = getTag("parking");
        String parkAndRide = getTag("park_ride");
        return isTag("amenity", "parking")
            && (parkingType != null && parkingType.contains("park_and_ride"))
            || (parkAndRide != null && !parkAndRide.equalsIgnoreCase("no"));
    }



    /**
     * @return True if this node / area is a bike parking.
     */
    default boolean isBikeParking() {
        return isTag("amenity", "bicycle_parking") && !isTag("access", "private")
            && !isTag("access", "no");
    }

    Map<String,String> getTags();

    default long getId() {
        //FIXME: this is temporary. Graph OSMWithTags overloads it OSMEntity doesn't
        return 5;
    }

    Collection<OSMEntity.Tag> getPermissionTags();
}
