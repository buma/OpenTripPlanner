package org.opentripplanner.streets;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.transit.TransportNetwork;
import org.opentripplanner.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    /** If this is true time is ignored when checking time dependant speeds/ turn restrictions etc
     * This is true when searching on street network between stops, to speed up actual searches
     * and when pruning street graph **/
    private boolean timeIndependantSearch = false;

    private ZonedDateTime zonedDateTime;

    /**
     * Constructor for options; modes defaults to walk and transit
     */
    public TransportNetworkRequest() {
        super();
    }

    public TransportNetworkRequest(TraverseModeSet modes) {
        this();
        this.setModes(modes);
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
        LOG.info("DateTime:{}", zonedDateTime);
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
}
