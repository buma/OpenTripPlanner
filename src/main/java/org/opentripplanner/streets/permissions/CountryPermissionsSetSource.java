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

import org.opentripplanner.graph_builder.module.osm.OSMSpecifier;
import org.opentripplanner.graph_builder.module.osm.WayProperties;
import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This implements default access permissions from OSM https://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#Default
 * It should not be used by itself.
 * Created by mabu on 18.9.2015.
 */
public class CountryPermissionsSetSource implements WayPropertySetSource, TransportModeHierarchyTree {

    private static final Logger LOG = LoggerFactory.getLogger(CountryPermissionsSetSource.class);

    static final Map<String, Boolean> validTags;

    static {
        Map<String, Boolean> validTags1;
        validTags1 = new HashMap<>(16);
        validTags1.put("motorway", true);
        validTags1.put("trunk", true);
        validTags1.put("primary", true);
        validTags1.put("secondary", true);
        validTags1.put("tertiary", true);
        validTags1.put("unclassified", false);
        validTags1.put("residential", false);
        validTags1.put("living_street", false);
        validTags1.put("road", false);
        validTags1.put("pedestrian", false);
        validTags1.put("path", false);
        validTags1.put("bridleway", false);
        validTags1.put("cycleway", false);
        validTags1.put("footway", false);
        validTags = Collections.unmodifiableMap(validTags1);
    }

    /**
     * This are wayProperties which are default Each country extends this class and overrides them.
     * @return
     */
    @Override
    public WayPropertySet getWayPropertySet() {
        WayPropertySet props = new WayPropertySet();

        TransportModePermission[] motorwayPermissionsArray = new TransportModePermission[]{
            new TransportModePermission(TransportModeType.ACCESS, OSMAccessPermissions.DESIGNATED),
            new TransportModePermission(TransportModeType.MOTORCAR, OSMAccessPermissions.INHERITED_DESIGNATED),
            new TransportModePermission(TransportModeType.MOTORCYCLE, OSMAccessPermissions.INHERITED_DESIGNATED),
            new TransportModePermission(TransportModeType.HGV, OSMAccessPermissions.INHERITED_DESIGNATED),
            new TransportModePermission(TransportModeType.PSV, OSMAccessPermissions.INHERITED_DESIGNATED),
            new TransportModePermission(TransportModeType.MOPED, OSMAccessPermissions.NO),
            new TransportModePermission(TransportModeType.HORSE, OSMAccessPermissions.NO),
            new TransportModePermission(TransportModeType.BICYCLE, OSMAccessPermissions.NO),
            new TransportModePermission(TransportModeType.FOOT, OSMAccessPermissions.NO)};

            TransportModePermission[]trunkPermissionsArray = new TransportModePermission[] {
            new TransportModePermission(TransportModeType.ACCESS, OSMAccessPermissions.YES),
            new TransportModePermission(TransportModeType.MOTORCAR,
                OSMAccessPermissions.INHERITED_YES),
            new TransportModePermission(TransportModeType.MOTORCYCLE,
                OSMAccessPermissions.INHERITED_YES),
            new TransportModePermission(TransportModeType.HGV,
                OSMAccessPermissions.INHERITED_YES),
            new TransportModePermission(TransportModeType.PSV,
                OSMAccessPermissions.INHERITED_YES),
            new TransportModePermission(TransportModeType.MOPED, OSMAccessPermissions.INHERITED_YES),
            new TransportModePermission(TransportModeType.HORSE, OSMAccessPermissions.INHERITED_YES),
            new TransportModePermission(TransportModeType.BICYCLE, OSMAccessPermissions.INHERITED_YES),
            new TransportModePermission(TransportModeType.FOOT, OSMAccessPermissions.INHERITED_YES) };

        TransportModePermissions otherStreets = TransportModePermissions.makeFromArray(trunkPermissionsArray);


        TransportModePermissions pedestrian = new TransportModePermissions();
        pedestrian.add(TransportModeType.ACCESS, OSMAccessPermissions.NO);
        pedestrian.add(new TransportModeType[]{TransportModeType.MOTORCAR,
            TransportModeType.MOTORCYCLE, TransportModeType.HGV, TransportModeType.PSV,
            TransportModeType.MOPED, TransportModeType.HORSE, TransportModeType.BICYCLE},
            OSMAccessPermissions.INHERITED_NO);
        pedestrian.add(TransportModeType.FOOT, OSMAccessPermissions.YES);

        TransportModePermissions cycleway = new TransportModePermissions();
        cycleway.add(TransportModeType.ACCESS, OSMAccessPermissions.NO);
        cycleway.add(new TransportModeType[]{TransportModeType.MOTORCAR,
            TransportModeType.MOTORCYCLE, TransportModeType.HGV, TransportModeType.PSV,
            TransportModeType.MOPED, TransportModeType.HORSE, TransportModeType.FOOT}, OSMAccessPermissions.INHERITED_NO);
        cycleway.add(TransportModeType.BICYCLE, OSMAccessPermissions.DESIGNATED);


        TransportModePermissions motorwayPermissions = TransportModePermissions.makeFromArray(motorwayPermissionsArray);

        setProperties(props, "motorway", motorwayPermissions);
        setProperties(props, "trunk|primary|secondary|tertiary|unclassified|residential|living_street|road", otherStreets);
        setProperties(props, "pedestrian", pedestrian);
        setProperties(props, "cycleway", cycleway);

        return props;
    }

    /**
     * Set transportModePermissions for specified highwayTags
     * @param props current wayPropertySet
     * @param tagSpecifiers highway tags separated with |
     * @param tagPermissions Transport mode permissions for specified highway tags
     */
    protected void setProperties(WayPropertySet props, String tagSpecifiers, TransportModePermissions tagPermissions) {
        WayProperties properties = new WayProperties();
        properties.setModePermissions(tagPermissions);
        Collection<OSMSpecifier> specifiers = getSpecifiers(tagSpecifiers);
        for (OSMSpecifier osmSpecifier: specifiers) {
            props.addProperties(osmSpecifier, properties);
        }
    }

    /**
     * From list of highway tags separated with | creates list of OSMSpecifiers with those tags
     *
     * In highway tags that exist in both x and x_link it creates both.
     * @param tagSpecifiers
     * @return
     */
    protected Collection<OSMSpecifier> getSpecifiers(String tagSpecifiers) {
        Set<OSMSpecifier> specifiers = new HashSet<>(10);
        if (tagSpecifiers.contains("|")) {
            for (String specifier: tagSpecifiers.split("\\|")) {
                if (validTags.containsKey(specifier)) {
                    specifiers.add(new OSMSpecifier("highway", specifier));
                    if (validTags.get(specifier)) {
                        specifiers.add(new OSMSpecifier("highway", specifier+"_link"));
                    }
                } else {
                    LOG.warn("Tag \"{}\" is not valid highway tag!", specifier);
                }
            }
        } else {
            if (validTags.containsKey(tagSpecifiers)) {
                specifiers.add(new OSMSpecifier("highway", tagSpecifiers));
                if (validTags.get(tagSpecifiers)) {
                    specifiers.add(new OSMSpecifier("highway", tagSpecifiers+"_link"));
                }
            } else {
                LOG.warn("Tag \"{}\" is not valid highway tag!", tagSpecifiers);
            }
        }
        return specifiers;
    }

    @Override
    public TransportModeTreeItem getTransportModeHierarchyTree() {
        TransportModeTreeItem root = new TransportModeTreeItem(TransportModeType.ACCESS);
        root.add(new TransportModeTreeItem(TransportModeType.FOOT));
        TransportModeTreeItem vehicle = new TransportModeTreeItem(TransportModeType.VEHICLE);
        root.add(vehicle);
        root.add(new TransportModeTreeItem(TransportModeType.HORSE));
        vehicle.add(new TransportModeTreeItem(TransportModeType.BICYCLE));
        TransportModeTreeItem motor_vehicle = new TransportModeTreeItem(TransportModeType.MOTOR_VEHICLE);
        vehicle.add(motor_vehicle);
        motor_vehicle.add(new TransportModeTreeItem(TransportModeType.HGV));
        motor_vehicle.add(new TransportModeTreeItem(TransportModeType.MOTORCYCLE));
        motor_vehicle.add(new TransportModeTreeItem(TransportModeType.MOPED));
        motor_vehicle.add(new TransportModeTreeItem(TransportModeType.MOTORCAR));
        TransportModeTreeItem psv = new TransportModeTreeItem(TransportModeType.PSV);
        motor_vehicle.add(psv);
        psv.add(new TransportModeTreeItem(TransportModeType.TAXI));
        psv.add(new TransportModeTreeItem(TransportModeType.BUS));
        return root;
    }
}
