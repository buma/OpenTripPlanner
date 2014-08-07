/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.graph_builder.impl.cyclewayDisable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import java.io.IOException;

/**
 *
 * @author mabu
 */
public class StreetFeatureSerializer extends JsonSerializer<StreetFeature> {

    @Override
    public void serialize(StreetFeature v, JsonGenerator jgen, SerializerProvider sp) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("type", v.getType());
        jgen.writeFieldName("geometry");
        jgen.writeStartObject();
        if (v.getGeometry() instanceof LineString) {
            jgen.writeStringField("type", "LineString");
        } else if (v.getGeometry() instanceof Polygon) {
            
            jgen.writeStringField("type", "Polygon");
        }
        jgen.writeFieldName("coordinates");
        if (v.getGeometry() instanceof Polygon) {
           jgen.writeStartArray();
        }
        jgen.writeObject(v.getGeometry().getCoordinates());
        if (v.getGeometry() instanceof Polygon) {
           jgen.writeEndArray();
        }
        jgen.writeEndObject();
        jgen.writeFieldName("properties");
        jgen.writeObject(v.getProperties());
        jgen.writeEndObject();
    }

    @Override
    public Class<StreetFeature> handledType() {
        return StreetFeature.class;
    }
    
    
    
}
