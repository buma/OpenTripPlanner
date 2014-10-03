/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util.fast_serial;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import de.ruedigermoeller.serialization.FSTBasicObjectSerializer;
import de.ruedigermoeller.serialization.FSTClazzInfo;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
public class FSTPlainStreetEdgeSerializer extends FSTEdgeSerializer {

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite,
            FSTClazzInfo fstci, FSTClazzInfo.FSTFieldInfo referencedBy,
            int i) throws IOException {
        PlainStreetEdge pse = (PlainStreetEdge) toWrite;
        //out.writeStringUTF("<psSTART");
        //Writes edge part
        super.writeObject(out, toWrite, fstci, referencedBy, i);
        out.writeObjectInternal(Arrays.asList(pse.getGeometry().getCoordinates()), new Class[]{ArrayList.class, Coordinate.class});
        out.writeStringUTF(pse.getName());
        out.writeDouble(pse.getLength());
        out.writeObjectInternal(pse.getPermission(), new Class[] {StreetTraversalPermission.class});
        
        out.writeFFloat(pse.getCarSpeed());
        
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
        out.writeObjectInternal(pse.getWheelchairNotes(), new Class[] {Set.class, Alert.class});
        out.writeObject(pse.getTurnRestrictions(), new Class[] {ArrayList.class, TurnRestriction.class});
        //out.writeStringUTF("psEND>");
        //System.err.println("PSE serial");
        
        
    }   

    /*
    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPositioin) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        int id = in.readInt();
        StreetVertex fromv = (StreetVertex) in.readObjectInternal(IntersectionVertex.class, StreetVertex.class);
        StreetVertex tov = (StreetVertex) in.readObjectInternal(IntersectionVertex.class, StreetVertex.class);
        Object cor = in.readObjectInternal(ArrayList.class, Coordinate.class);
        List<Coordinate> coordinates = (List<Coordinate>) cor;
        Coordinate[] coordinatesArr = new Coordinate[coordinates.size()];
        coordinatesArr = coordinates.toArray(coordinatesArr);
        String name = in.readStringUTF();
        double length = in.readDouble();
        StreetTraversalPermission permission = (StreetTraversalPermission) in.readObjectInternal(StreetTraversalPermission.class);
        float carSpeed = in.readFFloat();
        ElevationProfileSegment ele = (ElevationProfileSegment) in.readObjectInternal(ElevationProfileSegment.class);
        String label = in.readStringUTF();
        if (label.isEmpty()) {
            label = null;
        }
        int streetClass = in.readInt();
        Set<Alert> notes = (Set<Alert>) in.readObjectInternal(Set.class, Alert.class);
        boolean back = in.readBoolean();
        boolean wheelchair = in.readBoolean();
        boolean roundabout = in.readBoolean();
        boolean bogusName = in.readBoolean();
        boolean traffic = in.readBoolean();
        boolean stairs = in.readBoolean();
        boolean toll = in.readBoolean();
        Set<Alert> wheelChairnotes = (Set<Alert>) in.readObjectInternal(Set.class, Alert.class);
        List<TurnRestriction> turnRestrictions = (List<TurnRestriction>) in.readObjectInternal(ArrayList.class, TurnRestriction.class);
        PlainStreetEdge res = new PlainStreetEdge(fromv, tov,
                GeometryUtils.getGeometryFactory().createLineString(coordinatesArr),
                name, length, permission, back, carSpeed);
        //res.setElevationProfile(ele.getElevationProfile(), toll)
        res.setLabel(label);
        res.setWheelchairAccessible(wheelchair);
        res.setStreetClass(streetClass);
        res.setRoundabout(roundabout);
        res.setNote(notes);
        res.setHasBogusName(bogusName);
        res.setNoThruTraffic(traffic);
        res.setStairs(stairs);
        res.setToll(toll);
        res.setWheelchairNote(wheelChairnotes);
        for(TurnRestriction turnRestriction: turnRestrictions) {
            res.addTurnRestriction(turnRestriction);
        }
        in.registerObject(res, streamPositioin, serializationInfo, referencee);
        return res;
    }
    */   
    
    
}
