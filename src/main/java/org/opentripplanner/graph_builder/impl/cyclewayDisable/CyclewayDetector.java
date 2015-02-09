/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentripplanner.graph_builder.impl.cyclewayDisable;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.io.File;
import java.util.HashMap;
import org.opentripplanner.graph_builder.impl.PruneFloatingIslands;
import org.opentripplanner.graph_builder.impl.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.impl.osm.OSMWayIDNamer;
import org.opentripplanner.graph_builder.impl.osm.OpenStreetMapGraphBuilderImpl;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mabu
 */
public class CyclewayDetector {

    private static Logger LOG = LoggerFactory.getLogger(CyclewayDetector.class);

    @Parameter(names = { "-h", "--help"}, description = "Print this help message and exit", help = true)
    private boolean help;

    @Parameter(names = { "-g", "--graph"}, description = "path to the graph file", required = true)
    private String graphPath;

    @Parameter(names = { "-o", "--out"}, description = "output file")
    private String outPath;
    
    private JCommander jc;

    private HashMap<Class<?>, Object> extra;

    private Graph graph;
    
    public static void main(String[] args) throws Exception {
        CyclewayDetector cyclewayDetector = new CyclewayDetector(args);
        cyclewayDetector.run();
        
    }
    
    public CyclewayDetector(String[] args) {
        jc = new JCommander(this);
        //jc.addCommand(jc);
        
        try {
            jc.parse(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            jc.usage();
            System.exit(1);
        }
        
        if (help) {
            jc.usage();
            System.exit(0);
        }
    }
    
    /**
     * Creates graph from OSM and GTFS data and runs
     * {@link TransitToTaggedStopsGraphBuilderImpl} and
     * {@link TransitToStreetNetworkGraphBuilderImpl}.
     *
     * @param osm_filename filename for OSM (in resource folder of class)
     * @param gtfs_filename filename for GTFS (in resource folder of class)
     * @param wanted_con_filename filename for saved connections (in resource
     * folder of class)
     * @throws Exception
     */
    private void loadGraph(String osm_filename) throws Exception {
        graph = new Graph();
        extra = new HashMap<Class<?>, Object>();
        OpenStreetMapGraphBuilderImpl loader = new OpenStreetMapGraphBuilderImpl();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        //names streets based on osm ids (osm:way:osmid)
        loader.customNamer = new OSMWayIDNamer(loader.wayPropertySet);
        AnyFileBasedOpenStreetMapProviderImpl provider = new AnyFileBasedOpenStreetMapProviderImpl();
        loader.skipVisibility = true;
        loader.staticParkAndRide = false;
        PruneFloatingIslands pfi = new PruneFloatingIslands();
        File file = new File(osm_filename);
        provider.setPath(file);
        loader.setProvider(provider);
        loader.buildGraph(graph, extra);
//pfi.buildGraph(gg, extra);
//index = new StreetVertexIndexServiceImpl(graph);

        graph.index(new DefaultStreetVertexIndexFactory());
    }

    private void run() throws Exception {
        LOG.info("Loading graph");
        loadGraph(graphPath);
        cycleDisableBuilder cyDisableBuilder = new cycleDisableBuilder();
        cyDisableBuilder.setPath(new File(outPath));
        cyDisableBuilder.buildGraph(graph, extra);
    }

}
