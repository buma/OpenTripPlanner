package org.opentripplanner.inspector.networks;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import org.opentripplanner.inspector.InspectorLayerName;
import org.opentripplanner.streets.VertexStore;
import org.opentripplanner.transit.TransportNetwork;

import java.awt.*;

/**
 * Interface for a slippy map tile renderer.
 *
 * @author laurent
 */
public interface TileRenderer extends InspectorLayerName {

    /**
     * Context used for rendering a tile.
     *
     */
    public abstract class TileRenderContext {

        /** Graphics where to paint tile to, in pixel CRS (no transform set) */
        public Graphics2D graphics;

        /** The JTS transform that convert from WGS84 CRS to pixel CRS */
        public AffineTransformation transform;

        /** The graph being processed */
        public TransportNetwork transportNetwork;

        /** Bounding box of the rendered tile in WGS84 CRS, w/o margins */
        public Envelope bbox;

        /** Ground pixel density inverse */
        public double metersPerPixel;

        /** Tile size in pixels */
        public int tileWidth, tileHeight;

        /** Expand the bounding box to add some margins, in pixel size. */
        public abstract Envelope expandPixels(double marginXPixels, double marginYPixels);

        /**
         * Changes envelope coordinates to fixed coordinates (integer latitude and longitude)
         *
         * This is used in VertexStore
         * @param envelope
         * @return
         */
        public static Envelope toFixedEnvelope(Envelope envelope) {
            Envelope fixedEnvelope = new Envelope(
                VertexStore.floatingDegreesToFixed(envelope.getMinX()),
                VertexStore.floatingDegreesToFixed(envelope.getMaxX()), VertexStore.floatingDegreesToFixed(envelope.getMinY()),
                VertexStore.floatingDegreesToFixed(envelope.getMaxY()));
            return fixedEnvelope;
        }
    }

    /** Return the BufferedImage color model the renderer would like to use */
    public abstract int getColorModel();

    /** Implementation of the tile rendering */
    public abstract void renderTile(TileRenderContext context);

}
