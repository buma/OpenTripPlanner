package org.opentripplanner.inspector.networks;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import org.geotools.geometry.Envelope2D;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mabu on 7.10.2015.
 */
public class TileRendererManager {
    private static final Logger LOG = LoggerFactory.getLogger(TileRendererManager.class);

    private Map<String, TileRenderer> renderers = new HashMap<>();

    private TransportNetwork transportNetwork;

    public TileRendererManager(TransportNetwork transportNetwork) {
        this.transportNetwork = transportNetwork;

        // Register layers
        renderers.put("max-speed", new EdgeVertexTileRenderer(new MaxSpeedEdgeRenderer()));
        renderers.put("traversal", new EdgeVertexTileRenderer(new TraversalPermissionEdgeRender()));
        renderers.put("no-thru", new EdgeVertexTileRenderer(new NoThruTrafficPermissionEdgeRender()));
    }

    public BufferedImage renderTile(final TileRequest tileRequest, String layer) {

        TileRenderer.TileRenderContext context = new TileRenderer.TileRenderContext() {
            @Override public Envelope expandPixels(double marginXPixels, double marginYPixels) {
                Envelope retval = new Envelope(bbox);
                retval
                    .expandBy(marginXPixels / tileRequest.width * (bbox.getMaxX() - bbox.getMinX()),
                        marginYPixels / tileRequest.height * (bbox.getMaxY() - bbox.getMinY()));
                return retval;
            }
        };

        context.transportNetwork = transportNetwork;

        TileRenderer renderer = renderers.get(layer);

        if (renderer == null)
            throw new IllegalArgumentException("Unknown layer: " + layer);

        // The best place for caching tiles may be here
        BufferedImage image = new BufferedImage(tileRequest.width, tileRequest.height,
            renderer.getColorModel());
        context.graphics = image.createGraphics();
        Envelope2D trbb = tileRequest.bbox;
        context.bbox = new Envelope(trbb.x, trbb.x + trbb.width, trbb.y, trbb.y + trbb.height);
        context.transform = new AffineTransformation();
        double xScale = tileRequest.width / trbb.width;
        double yScale = tileRequest.height / trbb.height;

        context.transform.translate(-trbb.x, -trbb.y - trbb.height);
        context.transform.scale(xScale, -yScale);
        context.metersPerPixel = Math.toRadians(trbb.height) * 6371000 / tileRequest.height;
        context.tileWidth = tileRequest.width;
        context.tileHeight = tileRequest.height;

        long start = System.currentTimeMillis();
        renderer.renderTile(context);
        LOG.debug("Rendered tile at {},{} in {} ms", tileRequest.bbox.y, tileRequest.bbox.x,
            System.currentTimeMillis() - start);
        return image;
    }

    /**
     * Gets all renderers
     * <p>
     * Used to return list of renderers to client.
     * Could be also used to show legend.
     *
     * @return
     */
    public Map<String, TileRenderer> getRenderers() {
        return renderers;
    }
}