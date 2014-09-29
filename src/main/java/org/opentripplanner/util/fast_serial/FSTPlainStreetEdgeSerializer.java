/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util.fast_serial;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectOutput;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.util.ElevationProfileSegment;

/**
 *
 * @author mabu
 */
public class FSTPlainStreetEdgeSerializer extends FSTBasicObjectSerializer {

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite,
            FSTClazzInfo fstci, FSTClazzInfo.FSTFieldInfo referencedBy,
            int i) throws IOException {
        PlainStreetEdge pse = (PlainStreetEdge) toWrite;
        //out.writeStringUTF("<psSTART");
        out.writeObjectInternal(pse.getFromVertex(), new Class[] {Vertex.class});
        out.writeObjectInternal(pse.getToVertex(), new Class[] {Vertex.class});
        out.writeInt(pse.getId());

        out.writeObjectInternal(Arrays.asList(pse.getGeometry().getCoordinates()), new Class[]{ArrayList.class, Coordinate.class});
        out.writeStringUTF(pse.getName());
        out.writeDouble(pse.getLength());
        out.writeObjectInternal(pse.getPermission(), new Class[] {StreetTraversalPermission.class});
        
        out.writeFloat(pse.getCarSpeed());
        
        out.writeObjectInternal(pse.getElevationProfileSegment(), new Class[] {ElevationProfileSegment.class});
        String label = pse.getLabel();
        if(label == null) {
            label = new String();
        }
        //doesn't like null
        out.writeStringUTF(label);
        
        out.writeInt(pse.getStreetClass());
        
        out.writeObjectInternal(pse.getNotes(), new Class[] {Set.class, Alert.class});
        out.writeBoolean(pse.isBack());
        out.writeBoolean(pse.isWheelchairAccessible());
        out.writeBoolean(pse.isRoundabout());
        out.writeBoolean(pse.hasBogusName());
        out.writeBoolean(pse.isNoThruTraffic());
        out.writeBoolean(pse.isStairs());
        out.writeBoolean(pse.isToll());
        out.writeObjectInternal(pse.getNotes(), new Class[] {Set.class, Alert.class});
        out.writeObject(pse.getTurnRestrictions(), new Class[] {ArrayList.class, TurnRestriction.class});
        //out.writeStringUTF("psEND>");
        
        
    }   
    
    
}
