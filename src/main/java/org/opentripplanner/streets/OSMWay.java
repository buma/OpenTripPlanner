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

import com.conveyal.osmlib.Way;
import org.opentripplanner.openstreetmap.model.IOSMWithTags;

/**
 * Created by mabu on 18.9.2015.
 */ //This class just basically wraps way for now because all specifiers for speeds and permissions expects OSMWithTags
//Way basically implements needed function but its type is incompatible.
//So OSMWAY implements IOSMWithTags, same as OSMWithTags
//When Graph is removed this won't be used anymore because OSMEntity function will be called instead of OSMWithTags.
class OSMWay extends Way implements IOSMWithTags {

    public OSMWay(Way way) {
        this.nodes = way.nodes;
        this.tags = way.tags;
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
}
