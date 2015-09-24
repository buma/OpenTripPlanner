package org.opentripplanner.streets.permissions;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * This is for Transport mode hierarchy graph
 * from https://wiki.openstreetmap.org/wiki/File:TransportModeHierarchy.png
 *
 * Ski and boat types are skipped
 * Created by mabu on 18.9.2015.
 */
public enum  TransportModeType {
    ACCESS,
    FOOT,
    VEHICLE,
    HORSE,
    BICYCLE,
    MOTOR_VEHICLE,
    HGV,
    MOTORCYCLE,
    MOPED,
    MOTORCAR,
    PSV,
    TAXI,
    BUS;

    private static final Set<String> permissionTagKeys;

    static {
        permissionTagKeys = new HashSet<>(TransportModeType.values().length);
        for (TransportModeType modeType : TransportModeType.values()) {
            permissionTagKeys.add(modeType.toString().toLowerCase(Locale.ENGLISH));
        }
    }

    public static Set<String> getPermissionTagKeys() {
        return permissionTagKeys;
    }
}
