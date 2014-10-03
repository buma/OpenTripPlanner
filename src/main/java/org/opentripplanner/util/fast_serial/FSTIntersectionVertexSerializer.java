/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util.fast_serial;

import de.ruedigermoeller.serialization.FSTClazzInfo;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;
import java.io.IOException;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 *
 * @author mabu
 */
public class FSTIntersectionVertexSerializer extends FSTVertexSerializer {

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo fstci, FSTClazzInfo.FSTFieldInfo fstfi, int i) throws IOException {
        super.writeObject(out, toWrite, fstci, fstfi, i); //To change body of generated methods, choose Tools | Templates.
        IntersectionVertex v = (IntersectionVertex) toWrite;
        out.writeBoolean(v.trafficLight);
        out.writeBoolean(v.freeFlowing);
    }
    
    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPositioin) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        int index = in.readInt();
        int groupIndex = in.readInt();
        String label = in.readStringUTF();
        if (label.isEmpty()) {
            label = null;
        }
        String name = in.readStringUTF();
        double x = in.readFDouble();
        double y = in.readFDouble();
        double distance = in.readDouble();
        boolean trafficLight = in.readBoolean();
        boolean freeFlowing = in.readBoolean();
        IntersectionVertex vert = new IntersectionVertex(null, label, x, y, name);
        vert.setDistanceToNearestTransitStop(distance);
        vert.setGroupIndex(groupIndex);
        vert.freeFlowing = freeFlowing;
        vert.trafficLight = trafficLight;
        vert.setIndex(index);
        in.registerObject(vert, streamPositioin, serializationInfo, referencee);
        return vert;
        
    }
    
}
