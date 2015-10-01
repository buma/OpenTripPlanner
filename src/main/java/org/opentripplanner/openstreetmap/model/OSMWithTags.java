/* 
 Copyright 2008 Brian Ferris
 This program is free software: you can redistribute it and/or
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

import java.util.*;

import com.conveyal.osmlib.OSMEntity;
import org.opentripplanner.graph_builder.module.osm.TemplateLibrary;
import org.opentripplanner.streets.permissions.TransportModeType;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslatedString;

/**
 * A base class for OSM entities containing common methods.
 */

public class OSMWithTags implements IOSMWithTags {

    /* To save memory this is only created when an entity actually has tags. */
    private Map<String, String> _tags;

    protected long id;

    protected I18NString creativeName;

    /**
     * Gets the id.
     */
    public long getId() {
        return id;
    }

    @Override
    public Collection<OSMEntity.Tag> getPermissionTags() {
        if (_tags == null) {
            return new ArrayList<OSMEntity.Tag>();
        }
        Set<String> permission_tags = TransportModeType.getPermissionTagKeys();

        List<OSMEntity.Tag> tags = new ArrayList<>(_tags.size());
        for (final Map.Entry<String, String> entry : _tags.entrySet()) {
            if (permission_tags.contains(entry.getKey())) {
                tags.add(new OSMEntity.Tag(entry.getKey(), entry.getValue()));
            }
        }
        return tags;
    }

    /**
     * Sets the id.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Adds a tag.
     */
    public void addTag(OSMTag tag) {
        if (_tags == null)
            _tags = new HashMap<String, String>();

        _tags.put(tag.getK().toLowerCase(), tag.getV());
    }

    /**
     * Adds a tag.
     */
    public void addTag(String key, String value) {
        if (key == null || value == null)
            return;

        if (_tags == null)
            _tags = new HashMap<String, String>();

        _tags.put(key.toLowerCase(), value);
    }

    /**
     * The tags of an entity.
     */
    public Map<String, String> getTags() {
        return _tags;
    }

    /**
     * Is the tag defined?
     */
    public boolean hasTag(String tag) {
        tag = tag.toLowerCase();
        return _tags != null && _tags.containsKey(tag);
    }

    /**
     * Determines if a tag contains a false value. 'no', 'false', and '0' are considered false.
     */
    public boolean isTagFalse(String tag) {
        tag = tag.toLowerCase();
        if (_tags == null)
            return false;

        return IOSMWithTags.isFalse(getTag(tag));
    }

    /**
     * Determines if a tag contains a true value. 'yes', 'true', and '1' are considered true.
     */
    public boolean isTagTrue(String tag) {
        tag = tag.toLowerCase();
        if (_tags == null)
            return false;

        return IOSMWithTags.isTrue(getTag(tag));
    }

    public boolean doesTagAllowAccess(String tag) {
        if (_tags == null) {
            return false;
        }
        if (isTagTrue(tag)) {
            return true;
        }
        tag = tag.toLowerCase();
        String value = getTag(tag);
        return IOSMWithTags.isTagAccessAllowed(value);
    }

    /** @return a tag's value, converted to lower case. */
    public String getTag(String tag) {
        tag = tag.toLowerCase();
        if (_tags != null && _tags.containsKey(tag)) {
            return _tags.get(tag);
        }
        return null;
    }

    /**
     * Checks is a tag contains the specified value.
     */
    public boolean isTag(String tag, String value) {
        tag = tag.toLowerCase();
        if (_tags != null && _tags.containsKey(tag) && value != null)
            return value.equals(_tags.get(tag));

        return false;
    }


    public void setCreativeName(I18NString creativeName) {
        this.creativeName = creativeName;
    }

    @Override
    public boolean hasCreativeName() {
        return this.creativeName != null;
    }

    @Override
    public I18NString getCreativeName() {
        return this.creativeName;
    }
}
