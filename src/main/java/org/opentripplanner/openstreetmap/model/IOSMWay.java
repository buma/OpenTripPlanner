/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (props, at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.openstreetmap.model;

/**
 * Tag testing function refactored from {@link OSMWay}
 * Created by mabu on 18.9.2015.
 */
public interface IOSMWay extends IOSMWithTags {
    /**
     * Returns true if bicycle dismounts are forced.
     *
     * @return
     */
    default boolean isBicycleDismountForced() {
        String bicycle = getTag("bicycle");
        return isTag("cycleway", "dismount") || "dismount".equals(bicycle);
    }

    /**
     * Returns true if these are steps.
     *
     * @return
     */
    default boolean isSteps() {
        return "steps".equals(getTag("highway"));
    }

    /**
     * Is this way a roundabout?
     *
     * @return
     */
    default boolean isRoundabout() {
        return "roundabout".equals(getTag("junction"));
    }

    /**
     * Returns true if this is a one-way street for driving.
     *
     * @return
     */
    default boolean isOneWayForwardDriving() {
        return isTagTrue("oneway");
    }



    /**
     * Returns true if this way is one-way in the opposite direction of its definition.
     *
     * @return
     */
    default boolean isOneWayReverseDriving() {
        return isTag("oneway", "-1");
    }

    /**
     * Returns true if bikes can only go forward.
     *
     * @return
     */
    default boolean isOneWayForwardBicycle() {
        String oneWayBicycle = getTag("oneway:bicycle");
        return IOSMWithTags.isTrue(oneWayBicycle) || isTagFalse("bicycle:backwards");
    }



    /**
     * Returns true if bikes can only go in the reverse direction.
     *
     * @return
     */
    default boolean isOneWayReverseBicycle() {
        return "-1".equals(getTag("oneway:bicycle"));
    }

    /**
     * Returns true if bikes must use sidepath in forward direction
     *
     * @return
     */
    default boolean isForwardDirectionSidepath() {
        return "use_sidepath".equals(getTag("bicycle:forward"));
    }

    /**
     * Returns true if bikes must use sidepath in reverse direction
     *
     * @return
     */
    default boolean isReverseDirectionSidepath() {
        return "use_sidepath".equals(getTag("bicycle:backward"));
    }

    /**
     * Some cycleways allow contraflow biking.
     *
     * @return
     */
    default boolean isOpposableCycleway() {
        // any cycleway which is opposite* allows contraflow biking
        String cycleway = getTag("cycleway");
        String cyclewayLeft = getTag("cycleway:left");
        String cyclewayRight = getTag("cycleway:right");

        return (cycleway != null && cycleway.startsWith("opposite"))
            || (cyclewayLeft != null && cyclewayLeft.startsWith("opposite"))
            || (cyclewayRight != null && cyclewayRight.startsWith("opposite"));
    }
}
