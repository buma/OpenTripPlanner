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

package org.opentripplanner.routing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.graph_builder.module.osm.CreativeNamer;
import org.opentripplanner.graph_builder.module.osm.OSMSpecifier;
import org.opentripplanner.graph_builder.module.osm.SpeedPicker;
import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.streets.permissions.PortlandPermissionsSetSource;
import org.opentripplanner.streets.permissions.TransportModeTreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.measure.Measurable;
import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Velocity;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import static javax.measure.unit.NonSI.*;
import static javax.measure.unit.SI.*;

/**
 * This class reads default street speed information. Those were previously set in {@link org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource}
 *
 * It is used for setting speeds for street edges. Speed is first read from maxspeed tags and if those do not exist speed is set based on OSM tags.
 *
 * Speeds can be read in multiple units (km/h, mph, knots) all are converted to m/s internally.
 *
 * Created by mabu on 16.9.2015.
 */
public class SpeedsFactory {
    private static final Logger LOG = LoggerFactory.getLogger(SpeedsFactory.class);

    private WayPropertySet props;
    private final TransportModeTreeItem tree;

    //those two are only used for toString method
    //Converter from m/s to inputUnit
    private UnitConverter toUnit;
    //In which unit were speeds read
    private String unit;


    /**
     * Constructs speed factory with default values
     *
     * Those values are copied from {@link org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource}.
     */
    public SpeedsFactory() {
        props = new WayPropertySet();

        /*
         * Automobile speeds in the United States: Based on my (mattwigway) personal experience, primarily in California
         */
        setCarSpeed(props, "highway=motorway", 29); // 29 m/s ~= 65 mph
        setCarSpeed(props, "highway=motorway_link", 15); // ~= 35 mph
        setCarSpeed(props, "highway=trunk", 24.6f); // ~= 55 mph
        setCarSpeed(props, "highway=trunk_link", 15); // ~= 35 mph
        setCarSpeed(props, "highway=primary", 20); // ~= 45 mph
        setCarSpeed(props, "highway=primary_link", 11.2f); // ~= 25 mph
        setCarSpeed(props, "highway=secondary", 15); // ~= 35 mph
        setCarSpeed(props, "highway=secondary_link", 11.2f); // ~= 25 mph
        setCarSpeed(props, "highway=tertiary", 11.2f); // ~= 25 mph
        setCarSpeed(props, "highway=tertiary_link", 11.2f); // ~= 25 mph
        setCarSpeed(props, "highway=living_street", 2.2f); // ~= 5 mph

        props.defaultSpeed = 11.2f; // ~= 25 mph ~= 40 km/h

        addNamers();

        final PortlandPermissionsSetSource portlandPermissionsSetSource = new PortlandPermissionsSetSource(props);
        props = portlandPermissionsSetSource.getWayPropertySet();
        tree = portlandPermissionsSetSource.getTransportModeHierarchyTree();

        this.unit = "mph";
        toUnit = METERS_PER_SECOND.getConverterTo(MILES_PER_HOUR);
    }

    /**
     * Speed factory with speeds set in propertySet
     *
     * @param propertySet Here are all the speeds saved
     * @param unitConverter Converter from m/s to speeds in provided unit
     * @param unit in which speeds were read (only used in toString method)
     */
    public SpeedsFactory(WayPropertySet propertySet, UnitConverter unitConverter, String unit) {
        final PortlandPermissionsSetSource portlandPermissionsSetSource = new PortlandPermissionsSetSource(propertySet);
        this.props = portlandPermissionsSetSource.getWayPropertySet();

        addNamers();

        this.tree = portlandPermissionsSetSource.getTransportModeHierarchyTree();
        this.toUnit = unitConverter;
        this.unit = unit;
    }

    /**
     * Build a speed factory given a config node, or fallback to the default if none is specified.
     *
     * Units can be (km/h|kmh|kmph|kph or mph or knots) all values needs to be in provided units.
     * Values are OSM tag=tag value and speed. All tags need to be provided.
     * Default speed is speed if none of tags matches and street doesn't have maxspeed.
     * Accepts this format:
     * <pre>
     * speeds:{
     *   units:"km/h",
     *   values:{
     *       "highway=motorway": 130,
     *       "highway=motorway_link": 100,
     *       "highway=trunk": 110,
     *       "highway=trunk_link": 100,
     *       "highway=primary": 90,
     *       "highway=primary_link": 90,
     *       "highway=secondary": 50,
     *       "highway=secondary_link": 40,
     *       "highway=tertiary": 40,
     *       "highway=tertiary_link": 40,
     *       "highway=living_street": 10,
     *       "highway=pedestrian": 5,
     *       "highway=residential": 50,
     *       "highway=unclassified": 40,
     *       "highway=service": 25,
     *       "highway=track": 16,
     *       "highway=road": 40
     *   },
     *   "defaultSpeed":40
     * }
     * </pre>
     */
    public static SpeedsFactory fromConfig(JsonNode config) {
        if (config == null) {
            LOG.info("No speeds found in config file. Using defaults.");
            return new SpeedsFactory();
        }

        if (config.has("units") && config.has("values") && config.has("defaultSpeed")) {
            WayPropertySet propertySet = new WayPropertySet();
            Measurable<Velocity> speedUnit;
            UnitConverter unitConverter;

            String unit = config.get("units").asText();
            switch (unit) {
            case "km/h":
            case "kmh":
            case "kmph":
            case "kph":
                unitConverter = KILOMETERS_PER_HOUR.getConverterTo(METERS_PER_SECOND);
                break;
            case "mph":
                unitConverter = MILES_PER_HOUR.getConverterTo(METERS_PER_SECOND);
                break;
            case "knots":
                unitConverter = KNOT.getConverterTo(METERS_PER_SECOND);
                break;
            default:
                throw new IllegalArgumentException(
                    "Unknown unit:" + unit + " supported units are km/h|kmh|kmph|kph, mph and knots");
            }
            LOG.info("Unit converter: {}", unitConverter);

            JsonNode values = config.get("values");
            Iterator<Map.Entry<String, JsonNode>> fieldIterator = values.fields();
            while (fieldIterator.hasNext()) {
                Map.Entry<String, JsonNode> field = fieldIterator.next();
                String osmSpecifier = field.getKey();
                Double speedInUnit = field.getValue().asDouble();
                float speedInMs = (float) unitConverter.convert(speedInUnit);
                setCarSpeed(propertySet, osmSpecifier, speedInMs);
                LOG.info("specifier.:{} speed:{} {} -> {} m/s", osmSpecifier, speedInUnit, unit, speedInMs);
            }

            float defaultSpeed = (float) unitConverter.convert(config.get("defaultSpeed").asDouble());
            propertySet.defaultSpeed = defaultSpeed;
            return new SpeedsFactory(propertySet, unitConverter.inverse(), unit);
        }
        LOG.info("Speeds:{}", config.asText());
        LOG.info("units, values or defaultSpeed is missing. Using default speeds");
        return new SpeedsFactory();
    }

    @Override
    public String toString() {
        /*Locale locale = Locale.getDefault();
        if (locale.getCountry().equals("US") || locale.getCountry().equals("UK")) {
            unit = MILES_PER_HOUR.toString();
            toUnit = METERS_PER_SECOND.getConverterTo(MILES_PER_HOUR);
        } else {
            unit = KILOMETERS_PER_HOUR.toString();
            toUnit = METERS_PER_SECOND.getConverterTo(KILOMETERS_PER_HOUR);
        }*/
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\n");
        stringBuilder.append("  units = ").append(unit).append("\n");
        stringBuilder.append("  values = {\n");
        for (SpeedPicker speedPicker: props.getSpeedPickers()) {
            stringBuilder.append(String.format(Locale.US,"   %s = %.2f\n", speedPicker.specifier.toString(), toUnit.convert(speedPicker.speed)));
        }
        stringBuilder.append("  }\n");
        stringBuilder.append(String.format(Locale.US,"  defaultSpeed = %.2f\n", toUnit.convert(props.defaultSpeed)));
        stringBuilder.append("}\n");



        return stringBuilder.toString();
    }

    /**
     *
     * @param propset WayPropertySet on which this is applied
     * @param spec tag=value this if this matches specified speed is applied
     * @param speed in m/s
     */
    private static void setCarSpeed(WayPropertySet propset, String spec, float speed) {
        SpeedPicker picker = new SpeedPicker();
        picker.specifier = new OSMSpecifier(spec);
        picker.speed = speed;
        propset.addSpeedPicker(picker);
    }

    public WayPropertySet getProps() {
        return props;
    }

    public TransportModeTreeItem getTree() {
        return tree;
    }

    private void addNamers() {

        /* and some names */
        // Basics
        createNames(props, "highway=cycleway", "name.bike_path");
        createNames(props, "cycleway=track", "name.bike_path");
        createNames(props, "highway=pedestrian", "name.pedestrian_path");
        createNames(props, "highway=pedestrian;area=yes", "name.pedestrian_area");
        createNames(props, "highway=path", "name.path");
        createNames(props, "highway=footway", "name.pedestrian_path");
        createNames(props, "highway=bridleway", "name.bridleway");
        createNames(props, "highway=footway;bicycle=no", "name.pedestrian_path");

        // Platforms
        createNames(props, "otp:route_ref=*", "name.otp_route_ref");
        createNames(props, "highway=platform;ref=*", "name.platform_ref");
        createNames(props, "railway=platform;ref=*", "name.platform_ref");
        createNames(props, "railway=platform;highway=footway;footway=sidewalk", "name.platform");
        createNames(props, "railway=platform;highway=path;path=sidewalk", "name.platform");
        createNames(props, "railway=platform;highway=pedestrian", "name.platform");
        createNames(props, "railway=platform;highway=path", "name.platform");
        createNames(props, "railway=platform;highway=footway", "name.platform");
        createNames(props, "highway=platform", "name.platform");
        createNames(props, "railway=platform", "name.platform");
        createNames(props, "railway=platform;highway=footway;bicycle=no", "name.platform");

        // Bridges/Tunnels
        createNames(props, "highway=pedestrian;bridge=*", "name.footbridge");
        createNames(props, "highway=path;bridge=*", "name.footbridge");
        createNames(props, "highway=footway;bridge=*", "name.footbridge");

        createNames(props, "highway=pedestrian;tunnel=*", "name.underpass");
        createNames(props, "highway=path;tunnel=*", "name.underpass");
        createNames(props, "highway=footway;tunnel=*", "name.underpass");

        // Basic Mappings
        createNames(props, "highway=motorway", "name.road");
        createNames(props, "highway=motorway_link", "name.ramp");
        createNames(props, "highway=trunk", "name.road");
        createNames(props, "highway=trunk_link", "name.ramp");

        createNames(props, "highway=primary", "name.road");
        createNames(props, "highway=primary_link", "name.link");
        createNames(props, "highway=secondary", "name.road");
        createNames(props, "highway=secondary_link", "name.link");
        createNames(props, "highway=tertiary", "name.road");
        createNames(props, "highway=tertiary_link", "name.link");
        createNames(props, "highway=unclassified", "name.road");
        createNames(props, "highway=residential", "name.road");
        createNames(props, "highway=living_street", "name.road");
        createNames(props, "highway=road", "name.road");
        createNames(props, "highway=service", "name.service_road");
        createNames(props, "highway=service;service=alley", "name.alley");
        createNames(props, "highway=service;service=parking_aisle", "name.parking_aisle");
        createNames(props, "highway=byway", "name.byway");
        createNames(props, "highway=track", "name.track");

        createNames(props, "highway=footway;footway=sidewalk", "name.sidewalk");
        createNames(props, "highway=path;path=sidewalk", "name.sidewalk");

        createNames(props, "highway=steps", "name.steps");

        createNames(props, "amenity=bicycle_parking;name=*", "name.bicycle_parking_name");
        createNames(props, "amenity=bicycle_parking", "name.bicycle_parking");

        createNames(props, "amenity=parking;name=*", "name.park_and_ride_name");
        createNames(props, "amenity=parking", "name.park_and_ride_station");
    }


    private void createNames(WayPropertySet propset, String spec, String patternKey) {
        String pattern = patternKey;
        CreativeNamer namer = new CreativeNamer(pattern);
        propset.addCreativeNamer(new OSMSpecifier(spec), namer);
    }
}
