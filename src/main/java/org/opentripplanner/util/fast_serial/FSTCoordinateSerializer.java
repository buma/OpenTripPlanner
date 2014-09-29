/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util.fast_serial;

import com.vividsolutions.jts.geom.Coordinate;
import java.io.IOException;
import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectOutput;

/**
 *
 * @author mabu
 */
public class FSTCoordinateSerializer extends FSTBasicObjectSerializer {

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite,
            FSTClazzInfo fstci, FSTClazzInfo.FSTFieldInfo fstfi, int i) throws IOException {
        Coordinate cor = (Coordinate) toWrite;
        //out.writeStringUTF("<C");
        out.writeDouble(cor.x);
        out.writeDouble(cor.y);        
        //out.writeStringUTF("C>");
        //out.writeFDouble(cor.z);
    }
    
}
