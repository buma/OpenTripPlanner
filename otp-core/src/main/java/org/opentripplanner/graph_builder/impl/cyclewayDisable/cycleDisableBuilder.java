/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.graph_builder.impl.cyclewayDisable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.STRtree;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.api.model.GeometryAdapter;
import static org.opentripplanner.common.IterableLibrary.filter;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.model.json_serialization.EdgeSetJSONSerializer;
import org.opentripplanner.model.json_serialization.GeoJSONDeserializer;
import org.opentripplanner.model.json_serialization.GeoJSONSerializer;
import org.opentripplanner.model.json_serialization.SerializerUtils;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mabu
 */
public class cycleDisableBuilder implements GraphBuilder {
    
    private static final Logger LOG = LoggerFactory.getLogger(cycleDisableBuilder.class);
    
    private static final double DISTANCE_THRESHOLD = 0.0002;
    
    private File _path;
    
    Graph graph;
    
    private STRtree index;
    
    STRtree createIndex() {
        STRtree edgeIndex = new STRtree();
        for (Vertex v : graph.getVertices()) {
            for (Edge e : v.getOutgoing()) {
                if (e instanceof StreetEdge) {
                    Envelope envelope;
                    Geometry geometry = e.getGeometry();
                    envelope = geometry.getEnvelopeInternal();
                    edgeIndex.insert(envelope, e);
                }
            }
        }
        LOG.debug("Created index");
        return edgeIndex;
    }
    
    public void setPath(File path) {
        _path = path;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Output path is:{}", _path);
        this.graph = graph;
        this.index = new STRtree();
        //index.build();
        
        /*SimpleFeatureTypeBuilder cycleType = new SimpleFeatureTypeBuilder();
        
        cycleType.setName("Cycleway");
        cycleType.add("name", String.class);
        cycleType.add("permissions", String.class);
        cycleType.add("stroke", String.class);
        
        cycleType.setCRS(DefaultGeographicCRS.WGS84);
        cycleType.add("location", LineString.class);
        
        final SimpleFeatureType CYCLE = cycleType.buildFeatureType();
        
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(CYCLE);*/
        
        Collection<Edge> allEdges = graph.getEdges();
        List<PlainStreetEdge> streets = Lists.newArrayList(filter(allEdges, PlainStreetEdge.class));
        List<PlainStreetEdge> cycleways = new ArrayList<PlainStreetEdge>();
        List<StreetFeature> cycleways_features = new ArrayList<StreetFeature>();
        
        //SimpleFeatureCollection features = new DefaultFeatureCollection(null, null);
        
        TraverseModeSet cycling = new TraverseModeSet(TraverseMode.BICYCLE);
        TraverseModeSet driving = new TraverseModeSet(TraverseMode.CAR);
        for(PlainStreetEdge current_street: streets) {
            if (!current_street.back && !current_street.getLabel().contains(":area:")) {
                if (!current_street.canTraverse(driving) && current_street.canTraverse(cycling)
                        && (current_street.getName().equals("path")
                        || current_street.getName().equals("bike path"))
                        && (current_street.getDistance() > 25)) {
                    /*if (cycleways_features.size() > 20) {
                        continue;
                    }*/
                    if(cycleways.isEmpty()) {
                        LOG.info("AZIMUT:{}", current_street.getAzimuth());
                        LOG.info("BACK AZIMUT:{} {}", current_street.getToVertex().azimuthTo(current_street.getFromVertex()));
                    }
                    cycleways.add(current_street);
                    StreetFeature feature = StreetFeature.createCycleFeature(
                            current_street.getName(), current_street.getPermission().toString(), current_street.getGeometry());
                    feature.addPropertie("label", current_street.getLabel());
                    feature.addPropertie("azimuth", new Double(current_street.getAzimuth()));
                    //feature.addPropertie("back", new Boolean(current_street.back).toString());
                    feature.addPropertie("distance", new Double(current_street.getDistance()).toString());
                    //feature.addPropertie("string", current_street.toString());
                    //LOG.info("OSMID:{}, BACK:{}", current_street.getLabel(), current_street.back);

                    cycleways_features.add(feature);
                    /*builder.add(current_street.getName());
                     builder.add(current_street.getPermission().toString());
                     builder.add("#00FF00");
                     builder.add(current_street.getGeometry());*/


                } else if (current_street.canTraverse(driving)) {
                    Envelope envelope;
                    Geometry geometry = current_street.getGeometry();
                    envelope = geometry.getEnvelopeInternal();
                    this.index.insert(envelope, current_street);
                    
                    /*StreetFeature feature1 = StreetFeature.createRoadFeature(current_street.getName(),
                            current_street.getPermission().toString(),
                            current_street.getGeometry());
                    feature1.addPropertie("label", current_street.getLabel());
                    cycleways_features.add(feature1);*/
                }
            }
        }
        LOG.info("Created index");
        this.index.build();
        
        //sort on length:
        Comparator<PlainStreetEdge> comparator = new Comparator<PlainStreetEdge>() {

            @Override
            public int compare(PlainStreetEdge o1, PlainStreetEdge o2) {
                return new Double(o1.getDistance()).compareTo(o2.getDistance());
            }
        };
        
        Collections.sort(cycleways, Collections.reverseOrder(comparator));
        
        LOG.info("{} cycle streets", cycleways.size());
        LOG.info("{} main streets", this.index.size());
        
        double distanceTreshold = DISTANCE_THRESHOLD;
        
        
        for (PlainStreetEdge current_street: cycleways) {
            /*LOG.info("Searching street near:{} {} {}", current_street.getName(),
                    current_street.getLabel(),
                    current_street.getDistance());*/
            distanceTreshold = DISTANCE_THRESHOLD;
            Envelope envelope = current_street.getGeometry().getEnvelopeInternal();
            List<PlainStreetEdge> nearbyEdges = index.query(envelope);
            
            
            /*while (nearbyEdges.isEmpty()) {
                Geometry polygon = GeometryUtils.getGeometryFactory().toGeometry(envelope);
                StreetFeature polygonFeature = StreetFeature.createPolygonFeature(current_street.getLabel(), polygon);
                polygonFeature.addPropertie("distanceT", new Double(distanceTreshold).toString());
                polygonFeature.addPropertie("found", "false");
                cycleways_features.add(polygonFeature);
                envelope.expandBy(distanceTreshold);
                LOG.info("Using distance treshold:{}", distanceTreshold);
                distanceTreshold *= 2;
                nearbyEdges = index.query(envelope);
            }*/
            /*if(current_street.getLabel().equals("osm:way:266592491")) {
            Geometry polygon = GeometryUtils.getGeometryFactory().toGeometry(envelope);
                StreetFeature polygonFeature = StreetFeature.createPolygonFeature(current_street.getLabel(), polygon);
                polygonFeature.addPropertie("distanceT", new Double(distanceTreshold).toString());
                polygonFeature.addPropertie("found", "true");
                cycleways_features.add(polygonFeature); 
            }*/
            //LOG.info("Found {} streets", nearbyEdges.size());
            for (PlainStreetEdge nearStreet: nearbyEdges) {
                StreetFeature feature1 = StreetFeature.createRoadFeature(nearStreet.getName(),
                        nearStreet.getPermission().toString(),
                        nearStreet.getGeometry());
                feature1.addPropertie("label", nearStreet.getLabel());
                feature1.addPropertie("cycleway", current_street.getLabel());
                //feature1.addPropertie("distanceT", new Double(distanceTreshold).toString());
                feature1.addPropertie("azimuth", nearStreet.getAzimuth());
                cycleways_features.add(feature1);
                //compare azimuth if azimuth is negative add 180
                //What to do with one way streets?
                //if azimuts almost match save connection between cycleway and street
                //Go one direction on cycleway and see which streets are connected
                    //remember visited cycleway edges
                //Go other direction on cycleway and see which streets are connected
                    //remember visited cycleway edges
                
            }
            //remember cycleways with no connections.
            //make distance treshold larger and repeat
            //some streets are so parallel that default distance tresholds doesn't find streets
            //some are nowhere near other streets
        }
        
        if (!cycleways.isEmpty()) {
            FileOutputStream fileOutputStream = null;
            try {
                //PlainStreetEdge current_street = cycleways.get(0);
                //StreetFeature geoStreet = StreetFeature.createCycleFeature(
                //        current_street.getName(), current_street.getPermission().toString(), current_street.getGeometry());
                
                ObjectMapper mapper = SerializerUtils.getMapper();
                SimpleModule module = SerializerUtils.getSerializerModule();
                module.addSerializer(new StreetFeatureSerializer());
                module.addSerializer(new FeatureCollectionSerializer());
                mapper.registerModule(module);
                fileOutputStream = new FileOutputStream(_path);
                JsonGenerator jsonGen = mapper.getJsonFactory().createJsonGenerator(fileOutputStream);
                
                mapper.writeValue(jsonGen, new StreetFeatureCollection(cycleways_features));
            } catch (FileNotFoundException ex) {
                java.util.logging.Logger.getLogger(cycleDisableBuilder.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(cycleDisableBuilder.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                LOG.error("Exception:{}", ex);
            } finally {
                try {
                    fileOutputStream.close();
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(cycleDisableBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        
    }

    @Override
    public List<String> provides() {
        return Arrays.asList("cycle_streets");
    }

    @Override
    public List<String> getPrerequisites() {
        return Arrays.asList("streets");
    }

    @Override
    public void checkInputs() {
        return;
    }
    
}


