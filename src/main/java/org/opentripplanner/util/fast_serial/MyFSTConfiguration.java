/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util.fast_serial;

import de.ruedigermoeller.serialization.FSTConfiguration;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;

/**
 *
 * @author mabu
 */
public class MyFSTConfiguration {
    
    static FSTConfiguration singletonConf = FSTConfiguration.createDefaultConfiguration();
    
    static {
        singletonConf.registerClass(PlainStreetEdge.class);
    }
    
    public static FSTConfiguration getInstance() {
        return singletonConf;
    }
}
