/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util.fast_serial;

import java.io.IOException;
import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectOutput;
import org.opentripplanner.routing.graph.Vertex;

/**
 *
 * @author mabu
 */
public class FSTVertexSerializer extends FSTBasicObjectSerializer {

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite,
            FSTClazzInfo fstci, FSTClazzInfo.FSTFieldInfo fstfi, int i) throws IOException {
        Vertex v = (Vertex) toWrite;
        out.writeInt(v.getGroupIndex());
        String label = v.getLabel();
        if(label == null) {
            label = new String();
        }
        //out.writeStringUTF("<V");
        //doesn't like null
        out.writeStringUTF(label);
        out.writeStringUTF(v.getName());
        out.writeDouble(v.getX());
        out.writeDouble(v.getY());
        out.writeDouble(v.getDistanceToNearestTransitStop());
        //out.writeStringUTF("V>");
        //incoming, outgoing, index and maxIndex are created during read
    }
    
}
