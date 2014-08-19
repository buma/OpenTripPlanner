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
import com.vividsolutions.jts.geom.Coordinate;
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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
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
    
    private DistanceLibrary distanceLibrary;
    
    private static final TraverseModeSet cycling = new TraverseModeSet(TraverseMode.BICYCLE);
    private static final TraverseModeSet driving = new TraverseModeSet(TraverseMode.CAR);
    
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
    
    private static double makeAzimuthPositive(double azimuth) {
        if(azimuth < 0) {
            return azimuth + 180.0;
        } else {
            return azimuth;
        }
    }
    
    private static double compareAzimuth(StreetEdge cycleway, StreetEdge street) {
        double cycleway_azimuth = cycleway.getAzimuth();
        double street_azimuth = street.getAzimuth();
        
        cycleway_azimuth = makeAzimuthPositive(cycleway_azimuth);
        street_azimuth = makeAzimuthPositive(street_azimuth);
        
        return Math.abs(cycleway_azimuth-street_azimuth);
    }
    
    /*private Geometry expandCycleway(PlainStreetEdge cycleway) {
        LOG.info("Expanding {} {}", cycleway.getName(), cycleway.getLabel());
    }*/
    
    private static boolean is_cycleway(StreetEdge current_street) {
        return (!current_street.canTraverse(driving) && current_street.canTraverse(cycling)
                        && (current_street.getName().equals("path")
                        || current_street.getName().equals("bike path")));
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Output path is:{}", _path);
        this.graph = graph;
        this.index = new STRtree();
        distanceLibrary = SphericalDistanceLibrary.getInstance();
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
        

        for(PlainStreetEdge current_street: streets) {
            if (!current_street.back &&
                    !current_street.getLabel().contains(":area:") &&
                    !current_street.getName().equals("track")) {
                if (is_cycleway(current_street)
                        && (current_street.getDistance() > 40)) {
                    /*if (cycleways_features.size() > 20) {
                        continue;
                    }*/
                    /*if(cycleways.isEmpty()) {
                        LOG.info("AZIMUT:{}", current_street.getAzimuth());
                        LOG.info("BACK AZIMUT:{} {}", current_street.getToVertex().azimuthTo(current_street.getFromVertex()));
                    }*/
                    cycleways.add(current_street);
                    StreetFeature feature = StreetFeature.createCycleFeature(
                            current_street.getName(), current_street.getPermission().toString(), current_street.getGeometry());
                    feature.addPropertie("label", current_street.getLabel());
                    feature.addPropertie("azimuth", new Double(current_street.getAzimuth()));
                    //feature.addPropertie("back", new Boolean(current_street.back).toString());
                    feature.addPropertie("distance", new Double(current_street.getDistance()).toString());
                    //feature.addPropertie("string", current_street.toString());
                    //LOG.info("OSMID:{}, BACK:{}", current_street.getLabel(), current_street.back);

                    //cycleways_features.add(feature);
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
        
        //removes cycleways that are at graph edge
        cycleways.remove(0); //removes cycleway in kamnica
        cycleways.remove(0); //removes cycleway in duplek
        
        LOG.info("{} cycle streets", cycleways.size());
        LOG.info("{} main streets", this.index.size());
        
        double distanceTreshold = DISTANCE_THRESHOLD;
        
        int number_of_processed_ways = 0;
        
        for (PlainStreetEdge current_cycleway: cycleways) {
            LOG.info("Searching street near:{} {} {}", current_cycleway.getName(),
                    current_cycleway.getLabel(),
                    current_cycleway.getDistance());
            distanceTreshold = DISTANCE_THRESHOLD;
            Envelope envelope = current_cycleway.getGeometry().getEnvelopeInternal();
            List<PlainStreetEdge> nearbyEdges = index.query(envelope);
            
            StreetFeature feature = StreetFeature.createCycleFeature(
                            current_cycleway.getName(), current_cycleway.getPermission().toString(), current_cycleway.getGeometry());
                    feature.addPropertie("label", current_cycleway.getLabel());
                    feature.addPropertie("azimuth", new Double(current_cycleway.getAzimuth()));
                    //feature.addPropertie("back", new Boolean(current_street.back).toString());
                    feature.addPropertie("distance", new Double(current_cycleway.getDistance()).toString());
                    feature.addPropertie("order", number_of_processed_ways);
                    cycleways_features.add(feature);
            
            
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
            LOG.info("Found {} streets", nearbyEdges.size());
            
            List<PlainStreetEdge> street_candidates = new ArrayList<PlainStreetEdge>(nearbyEdges.size());
            boolean hasCandidateEdges = !nearbyEdges.isEmpty();
            for (PlainStreetEdge nearStreet: nearbyEdges) {
                
                StreetFeature feature1 = StreetFeature.createRoadFeature(nearStreet.getName(),
                        nearStreet.getPermission().toString(),
                        nearStreet.getGeometry());
                feature1.addPropertie("label", nearStreet.getLabel());
                feature1.addPropertie("cycleway", current_cycleway.getLabel());
                //feature1.addPropertie("distanceT", new Double(distanceTreshold).toString());
                double compAzimuth = compareAzimuth(current_cycleway, nearStreet);
                feature1.addPropertie("azimuth", nearStreet.getAzimuth());
                feature1.addPropertie("compAzimuth", compAzimuth);
                
                double distance = current_cycleway.getGeometry().distance(nearStreet.getGeometry());
                feature1.addPropertie("distanceFrom", distance);
                
                boolean roadCandidate = true;
                if (compAzimuth > 10) {
                    feature1.addPropertie("stroke", "#FF0000");
                    roadCandidate = false;
                } else {
                    if (distance >  0.00029) {
                        feature1.addPropertie("stroke", "#FF8000");
                        roadCandidate = false;
                    }
                }
                /*if (!roadCandidate) {
                    feature1.addPropertie("stroke-width", "0.7");
                }*/
                if (roadCandidate) {
                    street_candidates.add(nearStreet);
                }
                //hasCandidateEdges = hasCandidateEdges || roadCandidate;
                cycleways_features.add(feature1);
                

                //compare azimuth if azimuth is negative add 180
                //What to do with one way streets?
                //if azimuts almost match save connection between cycleway and street
                //Go one direction on cycleway and see which streets are connected
                    //remember visited cycleway edges
                //Go other direction on cycleway and see which streets are connected
                    //remember visited cycleway edges
                
            }
            if (!street_candidates.isEmpty()) {
                LOG.info("Has candidate edges");
                List<StreetEdge> fullCycleway = new LinkedList<StreetEdge>();
                ListIterator<StreetEdge> fullCyclewayIter = fullCycleway.listIterator();
                fullCyclewayIter.add(current_cycleway);
                fullCyclewayIter.previous();
                //Finds part of cycleway from current to the start
                LineString full_cycleway = lengthen_cycleway(current_cycleway, street_candidates, cycleways_features, false, fullCyclewayIter);
                //moves current iterator position to current cycleway (which should be at the end currently)
                while(fullCyclewayIter.hasNext()) {
                    StreetEdge partCycleway = fullCyclewayIter.next();
                //    LOG.info("PART:{}-{}", partCycleway.getFromVertex().getLabel(), partCycleway.getToVertex().getLabel());
                }
                //Finds part of cycleway from current to the end
                full_cycleway = lengthen_cycleway(current_cycleway, street_candidates, cycleways_features, true, fullCyclewayIter);
                List<Coordinate> cyclewayCoordinates = new LinkedList<Coordinate>();
                for(StreetEdge partCycleway: fullCycleway) {
                    cyclewayCoordinates.addAll(Arrays.asList(partCycleway.getGeometry().getCoordinates()));
                    LOG.info("PART:{}-{}", partCycleway.getFromVertex().getLabel(), partCycleway.getToVertex().getLabel());
                }
                
                full_cycleway = GeometryUtils.getGeometryFactory().createLineString(cyclewayCoordinates.toArray(new Coordinate[cyclewayCoordinates.size()]));
                
                StreetFeature full_feature = StreetFeature.createRoadFeature("Full cycleway", current_cycleway.getPermission().toString(), full_cycleway);
                full_feature.addPropertie("stroke", "#000000");
                full_feature.addPropertie("stroke-opacity", "0.4");
                full_feature.addPropertie("stroke-width", "4.0");
                cycleways_features.add(full_feature);
                
                
                
            }
            //remember cycleways with no connections.
            //make distance treshold larger and repeat
            //some streets are so parallel that default distance tresholds doesn't find streets
            //some are nowhere near other streets
            
            number_of_processed_ways++;
            
            if (number_of_processed_ways > 40) {
                break;
            }
        }
        
        if (!cycleways_features.isEmpty()) {
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

    private LineString lengthen_cycleway(StreetEdge current_street,
            List<PlainStreetEdge> street_candidates,
            List<StreetFeature> cycleways_features,
            boolean forward,
            ListIterator<StreetEdge> iter) {
        if (current_street == null) {
            return null;
        }
        Collection<Edge> nextEdges;
        if (forward) {
            nextEdges = current_street.getToVertex().getOutgoingStreetEdges();
        } else {
            nextEdges = current_street.getFromVertex().getIncoming();
        }
        for (Edge edge : nextEdges) {
            if (!(edge instanceof StreetEdge)) {
                continue;
            }
                    StreetEdge sedge = (StreetEdge) edge;
                    if (!sedge.isReverseOf(current_street)) {
                        LOG.info("Found candidate edge:{} {}", sedge.getLabel(), sedge.getLength());
                        StreetFeature candidateFeature = StreetFeature.createCycleFeature(sedge.getName(), sedge.getPermission().toString(), sedge.getGeometry());
                        candidateFeature.addPropertie("label", sedge.getLabel());
                        candidateFeature.addPropertie("stroke", "#FFFF00");
                        candidateFeature.addPropertie("stroke-opacity", "0.5");
                        candidateFeature.addPropertie("stroke-width", "2.0");
                        double compAzimuth = compareAzimuth(current_street, sedge);
                        candidateFeature.addPropertie("compAzimuth", compAzimuth);
                        
                        //double distance = current_street.getGeometry().distance(sedge.getGeometry());
                        //candidateFeature.addPropertie("distanceFrom", distance);
                        
                        
                        if (current_street.getLabel().equals(sedge.getLabel())
                                || (compAzimuth <= 10 && is_cycleway(sedge))) {
                            LOG.info("Added edge:{}, {}", sedge.getLabel(), sedge.getLength());
                            if (forward) {
                                iter.add(sedge);
                            } else {
                                iter.add(sedge);
                                iter.previous();
                            }
                            
                            cycleways_features.add(candidateFeature);
                            lengthen_cycleway(sedge, street_candidates, cycleways_features, forward, iter);
                        } else {
                            candidateFeature.addPropertie("stroke", "#7F00FF");
                            cycleways_features.add(candidateFeature);
                            
                        } 
                    }
                }
        return null;
    }
    
}


