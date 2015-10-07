package org.opentripplanner.inspector.networks;

import com.jhlabs.awt.ShapeStroke;
import com.jhlabs.awt.TextStroke;
import com.vividsolutions.jts.awt.IdentityPointTransformation;
import com.vividsolutions.jts.awt.PointShapeFactory;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.operation.buffer.OffsetCurveBuilder;
import gnu.trove.set.TIntSet;
import org.opentripplanner.streets.EdgeStore;
import org.opentripplanner.streets.VertexStore;
import org.opentripplanner.transit.TransportNetwork;

import java.awt.*;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

/**
 * Created by mabu on 7.10.2015.
 */
public class EdgeVertexTileRenderer implements TileRenderer {

    public class EdgeVisualAttributes {

        public Color color;

        public String label;
    }

    public class VertexVisualAttributes {

        public Color color;

        public String label;
    }

    public interface EdgeVertexRenderer {

        /**
         * @param e     The edge being rendered.
         * @param attrs The edge visual attributes to fill-in.
         * @return True to render this edge, false otherwise.
         */
        boolean renderEdge(EdgeStore.Edge e, EdgeVisualAttributes attrs);

        /**
         * @param v     The vertex being rendered.
         * @param attrs The vertex visual attributes to fill-in.
         * @param transportNetwork
         * @return True to render this vertex, false otherwise.
         */
        boolean renderVertex(VertexStore.Vertex v, VertexVisualAttributes attrs,
            TransportNetwork transportNetwork);

        /**
         * Name of this tile Render which would be shown in frontend
         *
         * @return Name of tile render
         */
        String getName();
    }

     @Override
    public int getColorModel() {
        return BufferedImage.TYPE_INT_ARGB;
    }

    private EdgeVertexRenderer evRenderer;

    public EdgeVertexTileRenderer(EdgeVertexRenderer evRenderer) {
        this.evRenderer = evRenderer;
    }

    @Override
    public String getName() {
        return evRenderer.getName();
    }

    @Override
    public void renderTile(TileRenderContext context) {

        float lineWidth = (float) (1.0f + 3.0f / Math.sqrt(context.metersPerPixel));

        // Grow a bit the envelope to prevent rendering glitches between tiles
        Envelope bboxWithMargins = context.expandPixels(lineWidth * 2.0, lineWidth * 2.0);

        Envelope fixedBboxWithMargins = TileRenderContext.toFixedEnvelope(bboxWithMargins);

        TIntSet edges = context.transportNetwork.streetLayer.spatialIndex.query(fixedBboxWithMargins);


        //TODO: vertices

        // Note: we do not use the transform inside the shapeWriter, but do it ourselves
        // since it's easier for the offset to work in pixel size.
        ShapeWriter shapeWriter = new ShapeWriter(new IdentityPointTransformation(),
            new PointShapeFactory.Point());
        GeometryFactory geomFactory = new GeometryFactory();

        Stroke stroke = new BasicStroke(lineWidth * 1.4f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_BEVEL);
        Stroke halfStroke = new BasicStroke(lineWidth * 0.6f + 1.0f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_BEVEL);
        Stroke halfDashedStroke = new BasicStroke(lineWidth * 0.6f + 1.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL, 1.0f, new float[] { 4 * lineWidth, lineWidth },
            2 * lineWidth);
        Stroke arrowStroke = new ShapeStroke(new Polygon(new int[] { 0, 0, 30 }, new int[] { 0, 20,
            10 }, 3), lineWidth / 2, 5.0f * lineWidth, 2.5f * lineWidth);
        BasicStroke thinStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_BEVEL);

        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(lineWidth));
        Font largeFont = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(lineWidth * 1.5f));
        FontMetrics largeFontMetrics = context.graphics.getFontMetrics(largeFont);
        context.graphics.setFont(largeFont);
        context.graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        context.graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        BufferParameters bufParams = new BufferParameters();
        bufParams.setSingleSided(true);
        bufParams.setJoinStyle(BufferParameters.JOIN_BEVEL);

        EdgeStore.Edge edgeCursor = context.transportNetwork.streetLayer.edgeStore.getCursor();
        VertexStore.Vertex vcursor = context.transportNetwork.streetLayer.vertexStore.getCursor();

        // Render all edges
        EdgeVisualAttributes evAttrs = new EdgeVisualAttributes();
        // Render all vertices
        VertexVisualAttributes vvAttrs = new VertexVisualAttributes();
        edges.forEach(s -> {
            evAttrs.color = null;
            evAttrs.label = null;
            edgeCursor.seek(s);

            vvAttrs.color = null;
            vvAttrs.label = null;
            vcursor.seek(edgeCursor.getFromVertex());
            boolean renderVertex = evRenderer.renderVertex(vcursor, vvAttrs,
                context.transportNetwork);
            if (renderVertex) {
                renderVertex(vcursor, context, geomFactory, vvAttrs, stroke, shapeWriter,
                    largeFontMetrics, lineWidth);
            }


            if (edgeCursor.getFromVertex() != edgeCursor.getToVertex()) {
                vcursor.seek(edgeCursor.getToVertex());
                vvAttrs.color = null;
                vvAttrs.label = null;
                renderVertex = evRenderer.renderVertex(vcursor, vvAttrs, context.transportNetwork);
                if (renderVertex) {
                    renderVertex(vcursor, context, geomFactory, vvAttrs, stroke, shapeWriter,
                        largeFontMetrics, lineWidth);
                }
            }

            if (!fixedBboxWithMargins.intersects(edgeCursor.getEnvelope())) {
                return true;
            }
            boolean render = evRenderer.renderEdge(edgeCursor, evAttrs);
            if (!render)
                return true;
            renderEdge(edgeCursor, context, bufParams, lineWidth, geomFactory, shapeWriter, halfStroke, halfDashedStroke, arrowStroke, thinStroke, font, evAttrs);

            //backward edge
            edgeCursor.advance();
            evAttrs.color = null;
            evAttrs.label = null;
            render = evRenderer.renderEdge(edgeCursor, evAttrs);
            if (!render)
                return true;
            renderEdge(edgeCursor, context, bufParams, lineWidth, geomFactory, shapeWriter,
                halfStroke, halfDashedStroke, arrowStroke, thinStroke, font, evAttrs);

            return true;
        });
    }

    private void renderVertex(VertexStore.Vertex vcursor, TileRenderContext context,
        GeometryFactory geomFactory, VertexVisualAttributes vvAttrs, Stroke stroke,
        ShapeWriter shapeWriter, FontMetrics largeFontMetrics, float lineWidth) {
        Point point = geomFactory.createPoint(vcursor.getCoordinate());

        Point tilePoint = (Point) context.transform.transform(point);
        Shape shape = shapeWriter.toShape(tilePoint);

        context.graphics.setColor(vvAttrs.color);
        context.graphics.setStroke(stroke);
        context.graphics.draw(shape);
        if (vvAttrs.label != null && lineWidth > 6.0f
            && context.bbox.contains(point.getCoordinate())) {
            context.graphics.setColor(Color.BLACK);
            int labelWidth = largeFontMetrics.stringWidth(vvAttrs.label);
                /*
                 * Poor man's solution: stay on the tile if possible. Otherwise the renderer would
                 * need to expand the envelope by an unbounded amount (max label size).
                 */
            double x = tilePoint.getX();
            if (x + labelWidth > context.tileWidth)
                x -= labelWidth;
            context.graphics.drawString(vvAttrs.label, (float) x, (float) tilePoint.getY());
        }
    }

    private void renderEdge(EdgeStore.Edge edgeCursor, TileRenderContext context,
        BufferParameters bufParams, float lineWidth, GeometryFactory geomFactory,
        ShapeWriter shapeWriter, Stroke halfStroke, Stroke halfDashedStroke, Stroke arrowStroke,
        BasicStroke thinStroke, Font font, EdgeVisualAttributes evAttrs) {
        Geometry edgeGeom = edgeCursor.getGeometry();
        boolean hasGeom = true;
        if (edgeGeom.getNumPoints() <= 2) {
            hasGeom = false;
        }
        Geometry midLineGeom = context.transform.transform(edgeGeom);
        OffsetCurveBuilder offsetBuilder = new OffsetCurveBuilder(new PrecisionModel(),
            bufParams);
        Coordinate[] coords = offsetBuilder.getOffsetCurve(midLineGeom.getCoordinates(),
            lineWidth * 0.4);
        if (coords.length < 2)
            return; // Can happen for very small edges (<1mm)
        LineString offsetLine = geomFactory.createLineString(coords);
        Shape midLineShape = shapeWriter.toShape(midLineGeom);
        Shape offsetShape = shapeWriter.toShape(offsetLine);

        context.graphics.setStroke(hasGeom ? halfStroke : halfDashedStroke);
        context.graphics.setColor(evAttrs.color);
        context.graphics.draw(offsetShape);
        if (lineWidth > 6.0f) {
            context.graphics.setColor(Color.WHITE);
            context.graphics.setStroke(arrowStroke);
            context.graphics.draw(offsetShape);
        }
        if (lineWidth > 4.0f) {
            context.graphics.setColor(Color.BLACK);
            context.graphics.setStroke(thinStroke);
            context.graphics.draw(midLineShape);
        }
        if (evAttrs.label != null && lineWidth > 8.0f) {
            context.graphics.setColor(Color.BLACK);
            context.graphics.setStroke(new TextStroke("    " + evAttrs.label
                + "                              ", font, false, true));
            context.graphics.draw(offsetShape);
        }
    }

}
