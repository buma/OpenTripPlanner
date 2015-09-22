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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This implements default access permissions from OSM https://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#Default
 * It should not be used by itself.
 * Created by mabu on 18.9.2015.
 */
public class CountryPermissionsSetSource implements TransportModeHierarchyTree {

    private static final Logger LOG = LoggerFactory.getLogger(CountryPermissionsSetSource.class);

    protected Map<OSMSpecifier, TransportModePermissions> permissionsForRoadType;

    static final Map<String, Boolean> validTags;

    private boolean filledWayPropertySet = false;

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
        validTags1.put("service", false);
        validTags1.put("track", false);
        validTags1.put("pedestrian", false);
        validTags1.put("path", false);
        validTags1.put("bridleway", false);
        validTags1.put("cycleway", false);
        validTags1.put("footway", false);
        validTags1.put("steps", false);
        validTags = Collections.unmodifiableMap(validTags1);
    }

    public CountryPermissionsSetSource() {
        permissionsForRoadType = new HashMap<>(10);
        TransportModePermissions motorwayPermissions = new TransportModePermissions();
        motorwayPermissions.add(TransportModeType.ACCESS, OSMAccessPermissions.DESIGNATED);
        motorwayPermissions.add(
            new TransportModeType[] { TransportModeType.MOTORCAR, TransportModeType.MOTORCYCLE,
                TransportModeType.HGV, TransportModeType.PSV },
            OSMAccessPermissions.INHERITED_DESIGNATED);
        motorwayPermissions.add(
            new TransportModeType[] { TransportModeType.MOPED, TransportModeType.HORSE,
                TransportModeType.BICYCLE, TransportModeType.FOOT }, OSMAccessPermissions.NO);

        TransportModePermissions otherStreets = new TransportModePermissions();
        otherStreets.add(TransportModeType.ACCESS, OSMAccessPermissions.YES);
        otherStreets.add(
            new TransportModeType[] { TransportModeType.MOTORCAR, TransportModeType.MOTORCYCLE,
                TransportModeType.HGV, TransportModeType.PSV, TransportModeType.MOPED,
                TransportModeType.HORSE, TransportModeType.BICYCLE, TransportModeType.FOOT },
            OSMAccessPermissions.INHERITED_YES);


        TransportModePermissions pedestrian = new TransportModePermissions();
        pedestrian.add(TransportModeType.ACCESS, OSMAccessPermissions.NO);
        pedestrian.add(new TransportModeType[]{TransportModeType.MOTORCAR,
                TransportModeType.MOTORCYCLE, TransportModeType.HGV, TransportModeType.PSV,
                TransportModeType.MOPED, TransportModeType.HORSE, TransportModeType.BICYCLE},
            OSMAccessPermissions.INHERITED_NO);
        pedestrian.add(TransportModeType.FOOT, OSMAccessPermissions.YES);

        TransportModePermissions path = new TransportModePermissions();
        path.add(TransportModeType.ACCESS, OSMAccessPermissions.NO);
        path.add(new TransportModeType[] { TransportModeType.MOTORCAR, TransportModeType.MOTORCYCLE,
                TransportModeType.HGV, TransportModeType.PSV, TransportModeType.MOPED, },
            OSMAccessPermissions.INHERITED_NO);
        path.add(new TransportModeType[] { TransportModeType.HORSE, TransportModeType.BICYCLE,
            TransportModeType.FOOT }, OSMAccessPermissions.YES);

        TransportModePermissions bridleway = new TransportModePermissions();
        bridleway.add(TransportModeType.ACCESS, OSMAccessPermissions.NO);
        bridleway.add(
            new TransportModeType[] { TransportModeType.MOTORCAR, TransportModeType.MOTORCYCLE,
                TransportModeType.HGV, TransportModeType.PSV, TransportModeType.MOPED,
                TransportModeType.BICYCLE, TransportModeType.FOOT },
            OSMAccessPermissions.INHERITED_NO);
        bridleway.add(TransportModeType.HORSE, OSMAccessPermissions.DESIGNATED);

        TransportModePermissions cycleway = new TransportModePermissions();
        cycleway.add(TransportModeType.ACCESS, OSMAccessPermissions.NO);
        cycleway.add(new TransportModeType[]{TransportModeType.MOTORCAR,
            TransportModeType.MOTORCYCLE, TransportModeType.HGV, TransportModeType.PSV,
            TransportModeType.MOPED, TransportModeType.HORSE, TransportModeType.FOOT}, OSMAccessPermissions.INHERITED_NO);
        cycleway.add(TransportModeType.BICYCLE, OSMAccessPermissions.DESIGNATED);

        TransportModePermissions footway = new TransportModePermissions();
        footway.add(TransportModeType.ACCESS, OSMAccessPermissions.NO);
        footway.add(
            new TransportModeType[] { TransportModeType.MOTORCAR, TransportModeType.MOTORCYCLE,
                TransportModeType.HGV, TransportModeType.PSV, TransportModeType.MOPED,
                TransportModeType.HORSE, TransportModeType.BICYCLE },
            OSMAccessPermissions.INHERITED_NO);
        footway.add(TransportModeType.FOOT, OSMAccessPermissions.DESIGNATED);

        prepareProperties("motorway", motorwayPermissions);
        prepareProperties(
            "trunk|primary|secondary|tertiary|unclassified|residential|living_street|road|service|track",
            otherStreets);
        prepareProperties("pedestrian", pedestrian);
        prepareProperties("path", path);
        prepareProperties("bridleway", bridleway);
        prepareProperties("cycleway", cycleway);
        //TODO: some steps have bicycle and wheelchair ramps
        prepareProperties("footway|steps", footway);
    }

    /**
     * Adds tagSpecifiers to local map
     *
     * This map is used in classes which extend this as default permissions. Non overridden permissions are used.
     * @param tagSpecifiers
     * @param tagPermissions
     */
    private void prepareProperties(String tagSpecifiers, TransportModePermissions tagPermissions) {
        for (final OSMSpecifier specifier : getSpecifiers(tagSpecifiers)) {
            permissionsForRoadType.put(specifier, tagPermissions);
        }
    }

    public final Map<OSMSpecifier, TransportModePermissions> getPermissionsForRoadType() {
        return Collections.unmodifiableMap(permissionsForRoadType);
    }


    /**
     * Replaces current properties of osmSpecifier with new properties
     *
     * It is used in Country specific road access permissions which replaces the default ones.
     * @param tagSpecifiers
     * @param transportModePermissions
     */
    protected final void replaceProperties(String tagSpecifiers,
        TransportModePermissions transportModePermissions) {
        //Those are permissions which are default
        Map<OSMSpecifier, TransportModePermissions> previousModePermissions = getPermissionsForRoadType();
        for (final OSMSpecifier osmSpecifier : getSpecifiers(tagSpecifiers)) {
            TransportModePermissions oldRoadPermissions = previousModePermissions.get(osmSpecifier);
            if (oldRoadPermissions != null) {
                transportModePermissions.fromOld(oldRoadPermissions);
            }
            permissionsForRoadType.put(osmSpecifier, transportModePermissions);
        }
    }

    /**
     * From list of highway tags separated with | creates list of OSMSpecifiers with those tags
     *
     * In highway tags that exist in both x and x_link it creates both.
     * @param tagSpecifiers
     * @return
     */
    protected final Collection<OSMSpecifier> getSpecifiers(String tagSpecifiers) {
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

    /**
     * Fills wayPropertySet with transport mode permissions
     *
     * Set is filled only once.
     */
    protected void fillWayPropertySet(WayPropertySet wayPropertySet) {
        if (filledWayPropertySet) {
            return;
        }
        for (final Map.Entry<OSMSpecifier, TransportModePermissions> entry : permissionsForRoadType
            .entrySet()) {
            WayProperties properties = new WayProperties();
            properties.setModePermissions(entry.getValue());
            wayPropertySet.addProperties(entry.getKey(), properties);
        }
        filledWayPropertySet = true;
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
