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

package org.opentripplanner.streets;

import com.conveyal.osmlib.OSMEntity;
import com.conveyal.osmlib.Way;
import org.opentripplanner.graph_builder.module.osm.WayProperties;
import org.opentripplanner.openstreetmap.model.IOSMWithTags;
import org.opentripplanner.streets.permissions.TransportModeType;

import java.util.*;

/**
 * Created by mabu on 18.9.2015.
 */ //This class just basically wraps way for now because all specifiers for speeds and permissions expects OSMWithTags
//Way basically implements needed function but its type is incompatible.
//So OSMWAY implements IOSMWithTags, same as OSMWithTags
//When Graph is removed this won't be used anymore because OSMEntity function will be called instead of OSMWithTags.
abstract class OSMEntityWithTags extends OSMEntity implements IOSMWithTags {

    public OSMEntityWithTags() {
    }

    public OSMEntityWithTags(OSMEntity entity) {
        this.tags = entity.tags;
    }

    /**
     * Checks is a tag contains the specified value.
     *
     * @param tag
     * @param value
     */
    @Override public boolean isTag(String tag, String value) {
        if (!hasNoTags()) {
            String tagValue = getTag(tag);
            if (tagValue != null)
                return value.equals(tagValue);
        }

        return false;
    }

    @Override public boolean doesTagAllowAccess(String tag) {
        if (hasNoTags()) {
            return false;
        }
        if (tagIsTrue(tag)) {
            return true;
        }
        String value = getTag(tag);
        return IOSMWithTags.isTagAccessAllowed(value);
    }

    /**
     * Determines if a tag contains a false value. 'no', 'false', and '0' are considered false.
     *
     * @param tag
     */
    @Override public boolean isTagFalse(String tag) {
        return !hasNoTags() && IOSMWithTags.isFalse(getTag(tag));

    }

    /**
     * Determines if a tag contains a true value. 'yes', 'true', and '1' are considered true.
     *
     * @param tag
     */
    @Override
    public boolean isTagTrue(String tag) {
        return !hasNoTags() && IOSMWithTags.isTrue(getTag(tag));

    }

    @Override
    public Map<String, String> getTags() {
        if (hasNoTags()) {
            return null;
        }
        Map<String, String>  mapTags = new HashMap<>(tags.size());
        for (Tag tag: tags) {
            mapTags.put(tag.key, tag.value);
        }
        return mapTags;
    }

    @Override public Type getType() {
        return null;
    }

    @Override public Collection<Tag> getPermissionTags() {
        if (hasNoTags()) {
            return new ArrayList<>();
        }
        Set<String> permissionTagKeys = new HashSet<>(TransportModeType.values().length);
        for (TransportModeType modeType : TransportModeType.values()) {
            permissionTagKeys.add(modeType.toString().toLowerCase());
        }

        List<OSMEntity.Tag> permissionTags = new ArrayList<>(tags.size());
        for (final Tag tag : tags) {
            if (permissionTagKeys.contains(tag.key)) {
                permissionTags.add(tag);
            }
        }
        return permissionTags;
    }
}
