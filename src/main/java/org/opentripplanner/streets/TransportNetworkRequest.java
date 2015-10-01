package org.opentripplanner.streets;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.transit.TransportNetwork;
import org.opentripplanner.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by mabu on 28.9.2015.
 */
public class TransportNetworkRequest  extends RoutingRequest implements Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(TransportNetworkRequest.class);

    private TransportNetworkContext transportContext;

    public TransportNetworkRequest bikeWalkingOptions;

    /** If this is true time is ignored when checking time dependant speeds/ turn restrictions etc
     * This is true when searching on street network between stops, to speed up actual searches
     * and when pruning street graph **/
    private boolean timeIndependantSearch = false;

    private ZonedDateTime zonedDateTime;

    /** If true when routing all the modes are tested and first which is allowed to traverse is used
     * This is used when pruning graph **/
    private boolean switchMode = false;

    /**
     * Constructor for options; modes defaults to walk and transit
     */
    public TransportNetworkRequest() {
        super();
        zonedDateTime = ZonedDateTime.now(Clock.systemUTC());
        bikeWalkingOptions = this;
    }

    public TransportNetworkRequest(TraverseModeSet modes) {
        this();
        this.setModes(modes);
    }

    public void setModes(TraverseModeSet modes) {
        this.modes = modes;
        if (modes.getBicycle()) {
            // This alternate routing request is used when we get off a bike to take a shortcut and are
            // walking alongside the bike. FIXME why are we only copying certain fields instead of cloning the request?
            bikeWalkingOptions = new TransportNetworkRequest();
            bikeWalkingOptions.setArriveBy(this.arriveBy);
            bikeWalkingOptions.maxWalkDistance = maxWalkDistance;
            bikeWalkingOptions.maxPreTransitTime = maxPreTransitTime;
            bikeWalkingOptions.walkSpeed = walkSpeed * 0.8; // walking bikes is slow
            bikeWalkingOptions.walkReluctance = walkReluctance * 2.7; // and painful
            bikeWalkingOptions.optimize = optimize;
            bikeWalkingOptions.modes = modes.clone();
            bikeWalkingOptions.modes.setBicycle(false);
            bikeWalkingOptions.modes.setWalk(true);
            bikeWalkingOptions.walkingBike = true;
            bikeWalkingOptions.bikeSwitchTime = bikeSwitchTime;
            bikeWalkingOptions.bikeSwitchCost = bikeSwitchCost;
            bikeWalkingOptions.stairsReluctance = stairsReluctance * 5; // carrying bikes on stairs is awful
        } else if (modes.getCar()) {
            bikeWalkingOptions = new TransportNetworkRequest();
            bikeWalkingOptions.setArriveBy(this.arriveBy);
            bikeWalkingOptions.maxWalkDistance = maxWalkDistance;
            bikeWalkingOptions.maxPreTransitTime = maxPreTransitTime;
            bikeWalkingOptions.modes = modes.clone();
            bikeWalkingOptions.modes.setBicycle(false);
            bikeWalkingOptions.modes.setWalk(true);
        }
    }

    public TransportNetworkContext getTransportContext() {
        return transportContext;
    }

    /**
     * Sets context without searching for start/end vertex. It is used in batch searches
     * like prunning or {@link org.opentripplanner.transit.TransferFinder}
     * @param transportNetwork
     */
    public void setDummyRoutingContext(TransportNetwork transportNetwork) {
        transportContext = new TransportNetworkContext(this, transportNetwork, null, null, false);
    }

    public void setTransportContext(TransportNetwork transportNetwork) {
        if (transportContext == null) {
            this.transportContext = new TransportNetworkContext(this, transportNetwork);
            this.transportContext.check();
        } else {
            if (transportContext.transportNetwork == transportNetwork) {
                LOG.debug("keeping existing routing context");
                return;
            } else {
                LOG.error("attempted to reset routing context using a different transport network");
                return;
            }
        }
    }

    @Override
    public TransportNetworkRequest clone() {
        return (TransportNetworkRequest) super.clone();
    }

    public TransportNetworkRequest reversedClone() {
        LOG.info("Reverse cloned request");
        TransportNetworkRequest ret = this.clone();
        ret.setArriveBy(!ret.arriveBy);
        ret.reverseOptimizing = !ret.reverseOptimizing; // this is not strictly correct
        ret.useBikeRentalAvailabilityInformation = false;
        return ret;
    }

    public void setTimeIndependantSearch(boolean timeIndependantSearch) {
        this.timeIndependantSearch = timeIndependantSearch;
    }

    @Override
    public void setDateTime(String date, String time, TimeZone tz) {
        Date dateObject = DateUtils.toDate(date, time, tz);
        if (dateObject == null) {
            throw new RuntimeException("Date time format is invalid!");
        }
        GregorianCalendar calendar = new GregorianCalendar(tz);
        calendar.setTime(dateObject);
        zonedDateTime = calendar.toZonedDateTime();
        super.setDateTime(dateObject);
    }

    public void setDateTime(Date date, TimeZone tz) {
        GregorianCalendar calendar = new GregorianCalendar(tz);
        calendar.setTime(date);
        zonedDateTime = calendar.toZonedDateTime();
        LOG.info("DateTime:{}", zonedDateTime);
        super.setDateTime(date);
    }

    public Instant getInstant() {
        return zonedDateTime.toInstant();
    }

    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public void setSwitchMode(boolean switchMode) {
        this.switchMode = switchMode;
    }

    public boolean isSwitchMode() {
        return switchMode;
    }
}
