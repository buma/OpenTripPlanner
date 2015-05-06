package org.opentripplanner.openstreetmap.model;

import com.google.common.collect.*;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.module.osm.OSMSpecifier;

import java.util.*;

/**
 * This class is used in counting how are way permissions calculated.
 *
 * Created by mabu on 6.5.2015.
 */
public class OSMWayForPerm extends OSMWay {

    static private ImmutableSetMultimap<String, String> tagsForPermissions;
    static private ImmutableSet<String> tagsMatchAllValues;

    public OSMWayForPerm(OSMWay osmWay) {
        _nodes = osmWay._nodes;
        _tags = osmWay._tags;
        id = osmWay.id;
    }

    private Map<String, String> cachedStrippedTags = null;

    public static void fillPermissionsTags(List<OSMSpecifier> specifierList) {
        SetMultimap<String, String> tagsForPermissionsB = HashMultimap.create(specifierList.size(), 5);
        Set<String> tagsMatchAllValuesB = new HashSet<String>(5);
        for (OSMSpecifier osmSpecifier: specifierList) {
            //All the tags of one specifier
            for (P2<String> pair : osmSpecifier.kvpairs) {
                //Match all tags goes into specific Set
                if (pair.second.equals("*")) {
                    tagsMatchAllValuesB.add(pair.first);
                } else {
                    tagsForPermissionsB.put(pair.first, pair.second);
                }
            }
        }
        tagsForPermissionsB.put("junction", "roundabout");
        tagsForPermissionsB.put("public_transport", "platform");
        tagsForPermissionsB.put("railway", "platform");
        tagsForPermissionsB.put("usage", "tourism");
        List<String> specificTags = Arrays.asList(
            //specific modes:
            "motor_vehicle", "motorcar", "bicycle", "foot", "bicycle:forward",
            //Cycleways
            "cycleway", "cycleway:left", "cycleway:right",
            //oneways
            "oneway","oneway:bicycle", "bicycle:backward",
            //Public transport
            "tram", "subway", "train", "monorail",
            "sidewalk",
            "access"
            );
        for (String specificTag : specificTags) {
            tagsMatchAllValuesB.add(specificTag);
        }
        //Removes all tags that have match all values since they are already in tagsMachAllValues set
        for (String matchAllTag : tagsMatchAllValuesB) {
            tagsForPermissionsB.removeAll(matchAllTag);
        }
        tagsForPermissions = ImmutableSetMultimap.copyOf(tagsForPermissionsB);
        tagsMatchAllValues = ImmutableSet.copyOf(tagsMatchAllValuesB);

    }

    public static ImmutableSet<String> getTagsMatchAllValues() {
        return tagsMatchAllValues;
    }

    public Map<String, String> getStrippedTags() {
        if (_tags == null) {
            return null;
        } else {
            if (cachedStrippedTags == null) {
                cachedStrippedTags = stripTags();
            }
            return cachedStrippedTags;
        }
    }

    private Map<String, String> stripTags() {
        Map<String, String> strippedTags = new HashMap<String, String>(_tags.size());
        for (Map.Entry<String, String> tag :_tags.entrySet()) {
            //If this tag has matchAll values it is added on each appearance
            if (tagsMatchAllValues.contains(tag.getKey())) {
                strippedTags.put(tag.getKey(), tag.getValue());
                //Otherwise it is added only if tag and value matches
            } else {
                Set<String> values = tagsForPermissions.get(tag.getKey());
                if (values != null) {
                    if (values.contains(tag.getValue())) {
                        strippedTags.put(tag.getKey(), tag.getValue());
                    }
                }
            }
        }
        if (strippedTags.isEmpty()) {
            return null;
        }
        return strippedTags;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OSMWayForPerm that = (OSMWayForPerm) o;
        return Objects.equals(getStrippedTags(), that.getStrippedTags());
    }

    @Override public int hashCode() {
        return Objects.hash(getStrippedTags());
    }

    public String stripTagsToString() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> tag :getStrippedTags().entrySet()) {
            builder.append(tag.getKey());
            builder.append("=");
            builder.append(tag.getValue());
            builder.append(";");
        }
        builder.deleteCharAt(builder.length() - 1); // remove trailing semicolon
        return builder.toString();
    }
}
