package org.opentripplanner.streets;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mabu on 28.9.2015.
 */
public class TransportNetworkRequest  extends RoutingRequest implements Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(TransportNetworkRequest.class);

    private TransportNetworkContext transportContext;

    /**
     * Constructor for options; modes defaults to walk and transit
     */
    public TransportNetworkRequest() {
        super();
    }

    public TransportNetworkContext getTransportContext() {
        return transportContext;
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
}
