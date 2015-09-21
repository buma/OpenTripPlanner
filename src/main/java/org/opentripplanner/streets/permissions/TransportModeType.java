package org.opentripplanner.streets.permissions;

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
    BUS
}
