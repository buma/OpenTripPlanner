/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util.fast_serial;

import de.ruedigermoeller.serialization.FSTBasicObjectSerializer;
import de.ruedigermoeller.serialization.FSTClazzInfo;
import de.ruedigermoeller.serialization.FSTObjectOutput;
import java.io.IOException;
import org.opentripplanner.routing.graph.Edge;

/**
 *
 * @author mabu
 */
public class FSTEdgeSerializer extends FSTBasicObjectSerializer {

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo fstci, FSTClazzInfo.FSTFieldInfo fstfi, int i) throws IOException {
        
        Edge pse = (Edge) toWrite;
        out.writeInt(pse.getId());
        out.writeObjectInternal(pse.getFromVertex(), new Class[] {pse.getFromVertex().getClass()});
        out.writeObjectInternal(pse.getToVertex(), new Class[] {pse.getToVertex().getClass()});
    }
    
    
    
}
