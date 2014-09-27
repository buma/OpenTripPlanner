/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util.fast_serial;

import com.vividsolutions.jts.geom.Coordinate;
import de.ruedigermoeller.serialization.FSTConfiguration;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.graph.Vertex;

/**
 *
 * @author mabu
 */
public class MyFSTConfiguration {
    
    static FSTConfiguration singletonConf = FSTConfiguration.createDefaultConfiguration();
    
    static {
        //singletonConf.registerClass(PlainStreetEdge.class);
        singletonConf.registerSerializer(PlainStreetEdge.class, new FSTPlainStreetEdgeSerializer(), false);
        singletonConf.registerSerializer(Vertex.class, new FSTVertexSerializer(), false);
        singletonConf.registerSerializer(Coordinate.class, new FSTCoordinateSerializer(), false);
    }
    
    public static FSTConfiguration getInstance() {
        return singletonConf;
    }
}
