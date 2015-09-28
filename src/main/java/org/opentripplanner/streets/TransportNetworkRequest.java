package org.opentripplanner.streets;

import org.opentripplanner.routing.core.RoutingRequest;


/**
 * Created by mabu on 28.9.2015.
 */
public class TransportNetworkRequest  extends RoutingRequest implements Cloneable {
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

    public void setTransportContext(TransportNetworkContext transportContext) {
        this.transportContext = transportContext;
    }

    @Override
    public TransportNetworkRequest clone() {
        return (TransportNetworkRequest) super.clone();
    }
}
