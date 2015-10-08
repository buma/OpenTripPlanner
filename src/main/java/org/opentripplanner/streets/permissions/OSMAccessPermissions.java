package org.opentripplanner.streets.permissions;

import com.google.common.collect.Sets;
import org.opentripplanner.openstreetmap.model.IOSMWithTags;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by mabu on 18.9.2015.
 */
public enum OSMAccessPermissions {
    YES,
    NO,
    DESTINATION,
    DISMOUNT,
    DESIGNATED,
    CUSTOMERS,
    DELIVERY,
    FORESTRY,
    RESIDENTS,
    AGRICULTURAL,
    NO_THRU_TRAFFIC,
    EMERGENCY,
    PRIVATE,
    PERMISSIVE,
    INHERITED_YES,
    INHERITED_NO,
    INHERITED_DESIGNATED,
    UNKNOWN,
    //This two are basically used for testing if road is routable.
    //YES, DESIGNATED, true, 1, and for no thru traffic streets:
    // DESTINATION, DELIVERY, PRIVATE this all have CAN_TRAVERSE (DESTINATION, DELIVERY, PRIVATE also have NO_THRU_TRAFFIC)
    CAN_TRAVERSE,
    //NO, DISMOUNT, EMERGENCY, restricted, prohibited, license, false, 0 has CANNOT_TRAVERSE and aren't routable
    CANNOT_TRAVERSE;


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
    public static Set<OSMAccessPermissions> toPermission(String access) {
        //Some access tags are like designated;yes no idea why
        if (access.contains(";")) {
            access = access.split(";")[0];
        }
        EnumSet<OSMAccessPermissions> returnset;
        boolean destination = "destination".equals(access)
            || "customers".equals(access) || "delivery".equals(access)
            || "forestry".equals(access)  || "agricultural".equals(access)
            || "residents".equals(access) || "resident".equals(access)
            || "customer".equals(access)
            || "private".equals(access) ;
        if (destination) {
            returnset = EnumSet.of(NO_THRU_TRAFFIC, CAN_TRAVERSE);

            if ("destination".equals(access)) {
                returnset.add(DESTINATION);
            } else if (access.startsWith("customer")) {
                returnset.add(CUSTOMERS);
            } else if (access.startsWith("resident")) {
                returnset.add(RESIDENTS);
            } else if ("forestry".equals(access)) {
                returnset.add(FORESTRY);
            } else if ("agricultural".equals(access)) {
                returnset.add(AGRICULTURAL);
            } else if ("delivery".equals(access)) {
                returnset.add(DELIVERY);
            } else if ("private".equals(access)) {
                returnset.add(PRIVATE);
            }
            return returnset;
        }
        if (IOSMWithTags.isTrue(access) || access.equals("official") || access.equals("unknown")
            || access.equals("public")) {
            return OSMAccessPermissions.yes;
        } else if (IOSMWithTags.isFalse(access) || access.equals("license") || access
            .equals("restricted") || access.equals("prohibited") || access.equals("emergency")) {
            returnset = OSMAccessPermissions.mutable_no();
            if (access.equals("emergency")) {
                returnset.add(EMERGENCY);
            }
            return returnset;
        }
        returnset = EnumSet.of(valueOf(access.toUpperCase(Locale.ENGLISH)));
        if (returnset.contains(DISMOUNT)) {
            returnset.add(CANNOT_TRAVERSE);
        } else if (returnset.contains(DESIGNATED) || returnset.contains(PERMISSIVE)) {
            returnset.add(CAN_TRAVERSE);
        }
        return returnset;
    }

    public static final Set<OSMAccessPermissions> designated = Sets.immutableEnumSet(DESIGNATED,
        CAN_TRAVERSE);
    public static final Set<OSMAccessPermissions> yes = Sets.immutableEnumSet(YES, CAN_TRAVERSE);
    public static final Set<OSMAccessPermissions> no = Sets.immutableEnumSet(NO, CANNOT_TRAVERSE);
    public static final Set<OSMAccessPermissions> inherited_any = Sets.immutableEnumSet(
        INHERITED_DESIGNATED, INHERITED_NO, INHERITED_YES);

    public static EnumSet<OSMAccessPermissions> mutable_no() {
        return EnumSet.copyOf(no);
    }
}
