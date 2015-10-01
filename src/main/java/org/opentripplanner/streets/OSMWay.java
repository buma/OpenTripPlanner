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
import org.opentripplanner.openstreetmap.model.IOSMWay;
import org.opentripplanner.util.I18NString;

import java.util.Arrays;

/**
 * Created by mabu on 18.9.2015.
 */
public class OSMWay extends OSMEntityWithTags implements IOSMWay {
    public long[] nodes;
    private I18NString creativeName = null;

    public OSMWay(Way way) {
        super(way);
        this.nodes = way.nodes;
    }

    public OSMWay() {
        super();
    }

    @Override
    public String toString() {
        return String.format("Way with %d tags and %d nodes", tags.size(), nodes.length);
    }

    @Override
    public Type getType() {
        return Type.WAY;
    }

    @Override
    public boolean equals(Object other) {
        if ( ! (other instanceof Way)) return false;
        Way otherWay = (Way) other;
        return Arrays.equals(this.nodes, otherWay.nodes) && this.tagsEqual(otherWay);
    }

    @Override
    public boolean hasCreativeName() {
        return this.creativeName != null;
    }

    @Override
    public I18NString getCreativeName() {
        return this.creativeName;
    }

    @Override
    public void setCreativeName(I18NString creativeName) {
        this.creativeName = creativeName;
    }
}
