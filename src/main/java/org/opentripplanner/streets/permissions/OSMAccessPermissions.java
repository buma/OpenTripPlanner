package org.opentripplanner.streets.permissions;

import org.opentripplanner.openstreetmap.model.IOSMWithTags;

import java.util.Locale;

/**
 * Created by mabu on 18.9.2015.
 */
public enum OSMAccessPermissions {
    YES,
    NO,
    DESTINATION,
    DISMOUNT,
    DESIGNATED,
    PRIVATE,
    PERMISSIVE,
    INHERITED_YES,
    INHERITED_NO,
    INHERITED_DESIGNATED,
    UNKNOWN;

    /**
     * Changes OSM access string to OSMAccessPermission
     *
     * Permissions are read this way:
     * - DESTINATION from destination, customers, delivery, agricultural, forestry, resident, residents, customer
     * - YES from yes, 1, true, official, unknown, public
     * - NO from no, 0, false, license, restricted, prohibited, emergency
     * Rest are same as enum names:
     * - dismount, private, permissive
     * @param access lowercase OSM access tag value
     * @return OSMAccesPermissions
     */
    public static OSMAccessPermissions toPermission(String access) {
        //Some access tags are like designated;yes no idea why
        if (access.contains(";")) {
            access = access.split(";")[0];
        }
        boolean destination = "destination".equals(access)
            || "customers".equals(access) || "delivery".equals(access)
            || "forestry".equals(access)  || "agricultural".equals(access)
            || "residents".equals(access) || "resident".equals(access)
            || "customer".equals(access);
        if (destination) {
            return DESTINATION;
        }
        if (IOSMWithTags.isTrue(access) || access.equals("official") || access.equals("unknown")
            || access.equals("public")) {
            return YES;
        } else if (IOSMWithTags.isFalse(access) || access.equals("license") || access
            .equals("restricted") || access.equals("prohibited") || access.equals("emergency")) {
            return NO;
        }
        return valueOf(access.toUpperCase(Locale.ENGLISH));
    }
}
