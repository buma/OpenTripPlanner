/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util.fast_serial;

import com.vividsolutions.jts.geom.Coordinate;
import de.ruedigermoeller.serialization.FSTBasicObjectSerializer;
import de.ruedigermoeller.serialization.FSTClazzInfo;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;
import java.io.IOException;

/**
 *
 * @author mabu
 */
public class FSTCoordinateSerializer extends FSTBasicObjectSerializer {

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite,
            FSTClazzInfo fstci, FSTClazzInfo.FSTFieldInfo fstfi, int i) throws IOException {
        Coordinate cor = (Coordinate) toWrite;
        out.writeFDouble(cor.x);
        out.writeFDouble(cor.y);
        //out.writeFDouble(cor.z);
        //System.err.println("Coor serial");
    }

    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPositioin) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Object res = new Coordinate(in.readDouble(), in.readDouble());
        in.registerObject(res, streamPositioin, serializationInfo, referencee);
        return res;
    }   
}
