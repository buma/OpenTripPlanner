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
import com.vividsolutions.jts.geom.Geometry;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author mabu
 */
public class FeatureCollectionSerializer extends JsonSerializer<StreetFeatureCollection> {

    @Override
    public void serialize(StreetFeatureCollection value, JsonGenerator jgen, SerializerProvider sp) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeObjectField("type", "FeatureCollection");
        jgen.writeFieldName("features");
        jgen.writeStartArray();
        for(StreetFeature feature: value.getFeatures()) {
            jgen.writeObject(feature);
        }
        jgen.writeEndArray();
        jgen.writeEndObject();
    }

    @Override
    public Class<StreetFeatureCollection> handledType() {
        return StreetFeatureCollection.class;
    }
    
    
    
}
