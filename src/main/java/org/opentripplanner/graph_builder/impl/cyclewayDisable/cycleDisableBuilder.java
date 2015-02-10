/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.graph_builder.impl.cyclewayDisable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.STRtree;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.impl.osm.OSMWayIDNamer;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.model.json_serialization.SerializerUtils;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This is builder which can find which streets have cycleway next to them
 * 
 * This cycleways are outputed as list of ids in geoJSON which can be used to visually inspect,
 * another geoJSON file which can be used for debugging to see the reason why street wasn't chosen
 * and as OSM files which can be opened in JSOM
 * and then File/Update data (Ctrl+U) all geometry data for candidate streets is downloaded from OSM
 * And streets can be edited and use_sidepath can be added for both or one direction.
 * 
 * Algorithm works quite good in Maribor for other places it probably needs some tweaking.
 * 
 * It has a bug if cycleways are in a circle it stack overflows because of recursion.
 * Quick fix is to split those roads in JOSM or somewhere else.
 * Ids will be temporary but at least one will still be the old one.
 * 
 * It works like that:
 * First list of all edges is separated on cycleways 
 * (edges which can be driven by bike but not CAR, and if it is (path, bike path
 * or footbridge) according to OSM tags)
 * And edges which can be driven by a CAR.
 * 
 * Cycleways are sorted on length from longest to shortest.
 * For each cycleway nearest car drivable roads are searched with help of R-TREE index
 * And if roads are near enough and parallel they are street candidates.
 * Each cycleway that has street candidates is lengthened in forward and backward direction 
 * until it's direction is too different, or isn't cycleway anymore.
 * 
 * This lengthened cycleway is expanded to polygon and all car drivable roads
 * that are covered by this polygon are then outputted as streets that have cycleways next to them.
 * 
 * TODO: why do we even have street candidates?
 * TODO: before final streets are printed out a check if roads are connected to any other found roads can be made currently it is commented out
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

    /**
     * Gets name part from streetName which is osmwayid:::name
     * @param name Street name
     * @return 
     */
    private String getName(String name) {
        return name.split(":::")[1];
    }

    /**
     * Gets label part from streetName which is osmwayid:::name
     * @param name OSM way ID
     * @return 
     */
    private String getLabel(String name) {
        return name.split(":::")[0];
    }

    private static double makeAzimuthPositive(double azimuth) {
        if(azimuth < 0) {
            return azimuth + 180.0;
        } else {
            return azimuth;
        }
    }
    
    /**
     * Compares azimuth of two streetEdges
     * 
     * If azimuth is negative 180 is added so that they can be compared
     * @param cycleway
     * @param street
     * @return Absolute difference between always pozitive azimuth of cycleway and streetEdge
     */
    private static double compareAzimuth(StreetEdge cycleway, StreetEdge street) {
        double cycleway_azimuth = cycleway.getAzimuth();
        double street_azimuth = street.getAzimuth();
        
        cycleway_azimuth = makeAzimuthPositive(cycleway_azimuth);
        street_azimuth = makeAzimuthPositive(street_azimuth);
        
        return Math.abs(cycleway_azimuth-street_azimuth);
    }
    
    /*private Geometry expandCycleway(PlainStreetEdge cycleway) {
        LOG.info("Expanding {} {}", getName(cycleway.getName()), cycleway.getLabel());
    }*/
    
    /**
     * Returns true if current street can be traversed with bike and not a car 
     * and if it is (path, bike path or footbridge) according to OSM tags
     * @param current_street
     * @return 
     */
    private static boolean is_cycleway(StreetEdge current_street) {
        return (!current_street.canTraverse(driving) && current_street.canTraverse(cycling)
                        && (current_street.isPath()
                        || current_street.isBikePath()
                        || current_street.isFootBridge()));
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
        List<StreetEdge> streets = Lists.newArrayList(Iterables.filter(allEdges, StreetEdge.class));
        //all the cycleways
        List<StreetEdge> cycleways = new ArrayList<StreetEdge>();
        //All the cycleways as streetFeatures which are used for outputing to geoJSON
        List<StreetFeature> cycleways_features = new ArrayList<StreetFeature>();
        
        List<StreetEdge> candidateStreets = new ArrayList<StreetEdge>(100);
        List<Geometry> full_cycleway_polygons = new ArrayList<Geometry>(100);
        List<StreetFeature> street_candidate_feat = new ArrayList<StreetFeature>();
        Set<Geometry> geometry_streets = new HashSet<Geometry>();
        
        Set<Geometry> seen_cycleway;
        Set<Geometry> seen_cycleway_polygons = new HashSet<Geometry>();
        
        //SimpleFeatureCollection features = new DefaultFeatureCollection(null, null);
        

        //Go over all street edges and skip tracks, areaEdges and reversed edges
        for(StreetEdge current_street: streets) {
            if (!current_street.isBack() &&
                    !(current_street instanceof AreaEdge) &&
                    !(current_street.isTrack())) {
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
                            getName(current_street.getName()), current_street.getPermission().toString(), current_street.getGeometry());
                    feature.addPropertie("label", getLabel(current_street.getName()));
                    feature.addPropertie("azimuth", new Double(current_street.getAzimuth()));
                    //feature.addPropertie("back", new Boolean(current_street.back).toString());
                    feature.addPropertie("distance", new Double(current_street.getDistance()).toString());
                    //feature.addPropertie("string", current_street.toString());
                    //LOG.info("OSMID:{}, BACK:{}", current_street.getLabel(), current_street.back);

                    //cycleways_features.add(feature);
                    /*builder.add(getName(current_street.getName()));
                     builder.add(current_street.getPermission().toString());
                     builder.add("#00FF00");
                     builder.add(current_street.getGeometry());*/


                //Car driven streets are put into Rtree index
                } else if (current_street.canTraverse(driving)) {
                    Envelope envelope;
                    Geometry geometry = current_street.getGeometry();
                    envelope = geometry.getEnvelopeInternal();
                    this.index.insert(envelope, current_street);
                    
                    /*StreetFeature feature1 = StreetFeature.createRoadFeature(getName(current_street.getName()),
                            current_street.getPermission().toString(),
                            current_street.getGeometry());
                    feature1.addPropertie("label", current_street.getLabel());
                    cycleways_features.add(feature1);*/
                }
            }
        }
        LOG.info("Created index");
        this.index.build();
        
        seen_cycleway = new HashSet<Geometry>(cycleways.size());
        
        //sort cycleways on length so that longer cycleways are seen sooner
        //Shorter are usually parts of cycleways in intersections
        Comparator<StreetEdge> comparator = new Comparator<StreetEdge>() {

            @Override
            public int compare(StreetEdge o1, StreetEdge o2) {
                return new Double(o1.getDistance()).compareTo(o2.getDistance());
            }
        };
        
        Collections.sort(cycleways, Collections.reverseOrder(comparator));
        
        LOG.info("{} cycle streets", cycleways.size());
        LOG.info("{} main streets", this.index.size());
        
        double distanceTreshold = DISTANCE_THRESHOLD;
        
        int number_of_processed_ways = 0;
        
        //Here we find candidates for streets next to cycleways
        for (StreetEdge current_cycleway: cycleways) {
            //We look at each cycleway only once
            if (seen_cycleway.contains(current_cycleway.getGeometry())) {
                LOG.info("Skipping seen cycleway");
                continue;
            }
            seen_cycleway.add(current_cycleway.getGeometry());
            LOG.info("Searching street near:{} {} {}", getName(current_cycleway.getName()),
                    getLabel(current_cycleway.getName()),
                    current_cycleway.getDistance());
            distanceTreshold = DISTANCE_THRESHOLD;
            Envelope envelope = current_cycleway.getGeometry().getEnvelopeInternal();
            List<StreetEdge> nearbyEdges = index.query(envelope);
            
            StreetFeature feature = StreetFeature.createCycleFeature(
                            getName(current_cycleway.getName()), current_cycleway.getPermission().toString(), current_cycleway.getGeometry());
                    feature.addPropertie("label", getLabel(current_cycleway.getName()));
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
            //Search envelope for nearest street to cycleway is expanded once
            if (nearbyEdges.isEmpty()) {
                LOG.info("Expanding envelope");
                distanceTreshold = DISTANCE_THRESHOLD/2;
                envelope.expandBy(distanceTreshold);
                nearbyEdges = index.query(envelope);
            }
            
           // LOG.info("Found {} streets", nearbyEdges.size());
            
            List<StreetEdge> street_candidates = new ArrayList<StreetEdge>(nearbyEdges.size());
            boolean hasCandidateEdges = !nearbyEdges.isEmpty();
            //if(current_street.getLabel().equals("osm:way:266592491")) {
            /*Geometry polygon = GeometryUtils.getGeometryFactory().toGeometry(envelope);
                StreetFeature polygonFeature = StreetFeature.createPolygonFeature(current_cycleway.getLabel(), polygon);
                polygonFeature.addPropertie("distanceT", new Double(distanceTreshold).toString());
                polygonFeature.addPropertie("found", hasCandidateEdges);
                cycleways_features.add(polygonFeature); */
            //}
            //For each street which is near cycleway distance and direction
            //to cycleway are checked
            //If azimuts are similar and near enough street is considered for further processing
            for (StreetEdge nearStreet: nearbyEdges) {
                
                StreetFeature feature1 = StreetFeature.createRoadFeature(getName(nearStreet.getName()),
                        nearStreet.getPermission().toString(),
                        nearStreet.getGeometry());
                feature1.addPropertie("label", getLabel(nearStreet.getName()));
                feature1.addPropertie("cycleway", getLabel(current_cycleway.getName()));
                //feature1.addPropertie("distanceT", new Double(distanceTreshold).toString());
                double compAzimuth = compareAzimuth(current_cycleway, nearStreet);
                feature1.addPropertie("azimuth", nearStreet.getAzimuth());
                feature1.addPropertie("compAzimuth", compAzimuth);
                
                double distance = current_cycleway.getGeometry().distance(nearStreet.getGeometry());
                feature1.addPropertie("distanceFrom", distance);
                
                boolean roadCandidate = true;
                //Road needs to be in the same direction and near cycleway enough
                //To be a candidate for cycleway restriction
                if (compAzimuth > 10) {
                    feature1.addPropertie("stroke", "#FF0000");
                    feature1.addPropertie("info", "to large azimuth");
                    roadCandidate = false;
                } else {
                    if (distance >  0.00029) {
                        feature1.addPropertie("stroke", "#FF8000");
                        feature1.addPropertie("info", "distance to large");
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
                //Finds part of cycleway from current part to the start (incoming edges)
                LineString full_cycleway = lengthen_cycleway(current_cycleway, street_candidates, cycleways_features, false, fullCyclewayIter);
                //moves current iterator position to current cycleway (which should be at the end currently)
                while(fullCyclewayIter.hasNext()) {
                    StreetEdge partCycleway = fullCyclewayIter.next();
                //    LOG.info("PART:{}-{}", partCycleway.getFromVertex().getLabel(), partCycleway.getToVertex().getLabel());
                }
                //Finds part of cycleway from current part to the end (outgoing edges)
                full_cycleway = lengthen_cycleway(current_cycleway, street_candidates, cycleways_features, true, fullCyclewayIter);
                List<Coordinate> cyclewayCoordinates = new LinkedList<Coordinate>();
                //Connects together all cycleway parts
                for(StreetEdge partCycleway: fullCycleway) {
                    cyclewayCoordinates.addAll(Arrays.asList(partCycleway.getGeometry().getCoordinates()));
                    seen_cycleway.add(partCycleway.getGeometry());
                    //LOG.info("PART:{}-{}", partCycleway.getFromVertex().getLabel(), partCycleway.getToVertex().getLabel());
                }
                
                full_cycleway = GeometryUtils.getGeometryFactory().createLineString(cyclewayCoordinates.toArray(new Coordinate[cyclewayCoordinates.size()]));
                
                /*StreetFeature full_feature = StreetFeature.createRoadFeature("Full cycleway", current_cycleway.getPermission().toString(), full_cycleway);
                full_feature.addPropertie("stroke", "#000000");
                full_feature.addPropertie("stroke-opacity", "0.4");
                full_feature.addPropertie("stroke-width", "4.0");
                cycleways_features.add(full_feature);*/
                
                //expands found cycleway to polygon
                Geometry cylceway_polygon = (Geometry)full_cycleway.buffer(0.00021);
                
                if(!seen_cycleway_polygons.contains(cylceway_polygon)) {
                
                    full_cycleway_polygons.add(cylceway_polygon);
                
                    StreetFeature feature_poly = StreetFeature.createPolygonFeature("full polygon", cylceway_polygon);
                    cycleways_features.add(feature_poly);
                    seen_cycleway_polygons.add(cylceway_polygon);
                }
            }
            //remember cycleways with no connections.
            //make distance treshold larger and repeat
            //some streets are so parallel that default distance tresholds doesn't find streets
            //some are nowhere near other streets
            
            number_of_processed_ways++;
            
            /*if (number_of_processed_ways > 40) {
                break;
            }*/
        }
        
        //For each extended cycleway polygons
        //We add all streets that are near enough and covered in polygon to candidateStreets
        for(Geometry cycleway_polygon: full_cycleway_polygons) {
            distanceTreshold = DISTANCE_THRESHOLD;
            Envelope envelope = cycleway_polygon.getEnvelopeInternal();
            List<StreetEdge> nearbyEdges = index.query(envelope);
            //LOG.info("Found good {} streets", nearbyEdges.size());
            
            //List<PlainStreetEdge> street_candidates = new ArrayList<PlainStreetEdge>(nearbyEdges.size());
            
            /*Geometry polygon = GeometryUtils.getGeometryFactory().toGeometry(envelope);
            StreetFeature polygon1 = StreetFeature.createPolygonFeature("envelope", polygon);
            street_candidate_feat.add(polygon1);*/
            boolean hasCandidateEdges = !nearbyEdges.isEmpty();
            for (StreetEdge nearStreet: nearbyEdges) {
                if (geometry_streets.contains(nearStreet.getGeometry())) {
                    continue;
                }
                StreetFeature feature1 = StreetFeature.createRoadFeature(
                    getName(nearStreet.getName()),
                    nearStreet.getPermission().toString(),
                    nearStreet.getGeometry()
                );
                feature1.addPropertie("label", getLabel(nearStreet.getName()));
                feature1.addPropertie("length", nearStreet.getDistance());
                feature1.addPropertie("distance", cycleway_polygon.distance(nearStreet.getGeometry()));
                if (cycleway_polygon.covers(nearStreet.getGeometry())) {
                    
                    if (nearStreet.getDistance() <= 15) { //TODO: check if 15 is still OK because unit has changed
                        feature1.addPropertie("stroke", "#ADD8E6");
                        feature1.addPropertie("info", "to short");
                    }
                    if (!nearStreet.canTraverse(cycling)) {
                        feature1.addPropertie("stroke", "#ff0000");
                        feature1.addPropertie("info", "bicycles can't ride it");
                    } else {
                        
                        
                        
                        candidateStreets.add(nearStreet);
                    }
                    /*if (!(is_connected(nearStreet, geometry_streets))) {
                        feature1.addPropertie("stroke", "#00ff00");
                        feature1.addPropertie("info", "Not connected");
                        feature1.addPropertie("id", nearStreet.getId());
                    }*/
                    street_candidate_feat.add(feature1);
                    //Used so each street is checked only once
                    geometry_streets.add(nearStreet.getGeometry());

                } /*else if (cycleway_polygon.isWithinDistance(nearStreet.getGeometry(), 0)) {
                    feature1.addPropertie("stroke", "#FF7F00");
                    feature1.addPropertie("info", "contains");
                    street_candidate_feat.add(feature1);
//                    geometry_streets.add(nearStreet.getGeometry());
                } else {
                    
                    feature1.addPropertie("stroke", "#7F00FF");
                    feature1.addPropertie("info", "ostalo");
                    street_candidate_feat.add(feature1);
                }*/
            }
        }
        
        //check for connectivenes after every connected street is added (Currently used only in geoJSON)
        for(Geometry cycleway_polygon: full_cycleway_polygons) {
            distanceTreshold = DISTANCE_THRESHOLD;
            Envelope envelope = cycleway_polygon.getEnvelopeInternal();
            List<StreetEdge> nearbyEdges = index.query(envelope);
            for (StreetEdge nearStreet: nearbyEdges) {
                StreetFeature feature1 = StreetFeature.createRoadFeature(
                    getName(nearStreet.getName()),
                    nearStreet.getPermission().toString(),
                    nearStreet.getGeometry()
                );
                feature1.addPropertie("label", getLabel(nearStreet.getName()));
                feature1.addPropertie("length", nearStreet.getDistance());
                feature1.addPropertie("distance", cycleway_polygon.distance(nearStreet.getGeometry()));
                if (cycleway_polygon.covers(nearStreet.getGeometry())) {
                    if (!(is_connected(nearStreet, geometry_streets))) {
                        feature1.addPropertie("stroke", "#00ff00");
                        feature1.addPropertie("info", "Not connected");
                        feature1.addPropertie("stroke-width", 4);
                        feature1.addPropertie("id", nearStreet.getId());
                    }
                    street_candidate_feat.add(feature1);
                }
            }
        }
        //Outputs all street candidates ids in ids.txt geoJSON and OSM file
        if (!cycleways_features.isEmpty()) {
            FileOutputStream fileOutputStream = null;
            try {
                //PlainStreetEdge current_street = cycleways.get(0);
                //StreetFeature geoStreet = StreetFeature.createCycleFeature(
                //        getName(current_street.getName()), current_street.getPermission().toString(), current_street.getGeometry());
                
                ObjectMapper mapper = SerializerUtils.getMapper();
                SimpleModule module = SerializerUtils.getSerializerModule();
                module.addSerializer(new StreetFeatureSerializer());
                module.addSerializer(new FeatureCollectionSerializer());
                mapper.registerModule(module);
                fileOutputStream = new FileOutputStream(new File(_path, "data.geojson"));
                JsonGenerator jsonGen = mapper.getJsonFactory().createJsonGenerator(fileOutputStream);
                
                mapper.writeValue(jsonGen, new StreetFeatureCollection(cycleways_features));
                
                JsonGenerator jsonGen1 = mapper.getJsonFactory().createJsonGenerator(new FileOutputStream(new File(_path, "cand_streets.geojson")));
                
                
                mapper.writeValue(jsonGen1, new StreetFeatureCollection(street_candidate_feat));
                
                FileWriter outFile = new FileWriter(new File(_path, "ids.txt"));
                PrintWriter pw = new PrintWriter(outFile);
                
                /* Sorting doesn't matter. It's unsorted after requesting data from OSM API
                //sorts streets by ID (merging doesn't work otherwise
                Comparator<StreetEdge> comparatorID = new Comparator<StreetEdge>() {

                    @Override
                    public int compare(StreetEdge o1, StreetEdge o2) {
                        Long lo1 = new Long(o1.getLabel().split(":")[2]);
                        Long lo2 = new Long(o2.getLabel().split(":")[2]);
                        return lo1.compareTo(lo2);
                    }
                };
        
                Collections.sort(candidateStreets, comparatorID);*/
                
                //Creates OSM file with streets which should have cycleway
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = docFactory.newDocumentBuilder();

                //root element
                Document doc = documentBuilder.newDocument();
                Element rootElement = doc.createElement("osm");
                rootElement.setAttribute("version", "0.6");
                rootElement.setAttribute("generator", "My Java generator");

                doc.appendChild(rootElement);

                Element note = doc.createElement("note");
                note.appendChild(doc.createTextNode("This is generated from tsv file"));
                rootElement.appendChild(note);

                Element meta = doc.createElement("meta");
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

                meta.setAttribute("osm_base", df.format(new Date()));
                rootElement.appendChild(meta);
                
                
                Set<String> addedIds = new HashSet<String>();
                for(StreetEdge cand_street_edge: candidateStreets) {
                    if (!addedIds.contains(getLabel(cand_street_edge.getName()))) {
                        /*if(!(is_connected(cand_street_edge, geometry_streets))) {
                            continue;
                        }*/
                        String wayID = getLabel(cand_street_edge.getName()).split(":")[2];
                        //First way is always added
                        if (addedIds.isEmpty()) {
                            pw.print(wayID);

                            Element way = doc.createElement("way");
                            way.setAttribute("visible", "true");
                            way.setAttribute("version", "1");
                            way.setAttribute("id", wayID);
                            rootElement.appendChild(way);
                        } else {
                            //Prints 25 way ids in each line
                            if ((addedIds.size() == 1) || ((addedIds.size()-1)%25!=0)) {
                                pw.print(",");
                            }
                            pw.print(wayID);
                            Element way = doc.createElement("way");
                            way.setAttribute("visible", "true");
                            way.setAttribute("version", "1");
                            way.setAttribute("id", wayID);
                            rootElement.appendChild(way);
                            if (addedIds.size()%25==0) {
                                pw.println();
                            }
                        }
                        
                        addedIds.add(getLabel(cand_street_edge.getName()));
                    }
                }
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(_path, "candidate_streets.osm"));
        
                transformer.transform(source, result);
                pw.close();
                jsonGen1.close();
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

    /**
     * Recorsively walks cycleway until it the direction is much different as before or it stops being cycleway
     * @param current_street Start of recursion
     * @param street_candidates Seems to be unused
     * @param cycleways_features List of GeoJSON data used for visual debugging
     * @param forward true - search is forward, false - search is backward
     * @param iter - cycleway parts are added to this iterator
     * @return 
     */
    private LineString lengthen_cycleway(StreetEdge current_street,
            List<StreetEdge> street_candidates,
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
        
        Comparator<T2<StreetEdge, Integer>> t2_comp = new Comparator<T2<StreetEdge, Integer>>() {

            @Override
            public int compare(T2<StreetEdge, Integer> o1, T2<StreetEdge, Integer> o2) {
                return o1.second.compareTo(o2.second);
            }
        };
        //Cycleway and score how good is the continuation of cycleway
        //Scores are:
        //100 - same name
        //80  - azimuth difference <= 10 and is still cycleway
        //60  - angle of street continuation (current street outangle vs. street inAngle) <= 10 and is still cycleway
        List<T2<StreetEdge, Integer>> candidates = new ArrayList<T2<StreetEdge, Integer>>(nextEdges.size());
        
        //Look at all incoming/outgoing streetedges
        for (Edge edge : nextEdges) {
            if (!(edge instanceof StreetEdge)) {
                continue;
            }
                    StreetEdge sedge = (StreetEdge) edge;
                    if (!sedge.isReverseOf(current_street)) {
//                        LOG.info("Found candidate edge:{} {}", getLabel(sedge.getName()), sedge.getDistance());
                        StreetFeature candidateFeature = StreetFeature.createCycleFeature(getName(sedge.getName()), sedge.getPermission().toString(), sedge.getGeometry());
                        candidateFeature.addPropertie("label", getLabel(sedge.getName()));
                        candidateFeature.addPropertie("stroke", "#FFFF00");
                        candidateFeature.addPropertie("stroke-opacity", "0.5");
                        candidateFeature.addPropertie("stroke-width", "2.0");
                        double compAzimuth = compareAzimuth(current_street, sedge);
                        candidateFeature.addPropertie("compAzimuth", compAzimuth);
                        //Calculates how much angle is between this and next cycleway part
                        int diff = 0;
                        if (forward) {
                            int outAngle = current_street.getOutAngle();
                            int sedgeInAngle = sedge.getInAngle();
                            diff = Math.abs(outAngle-sedgeInAngle);
                        } else {
                            int inAngle = current_street.getInAngle();
                            int endeOutAngle = sedge.getOutAngle();
                            diff = Math.abs(inAngle-endeOutAngle);
                        }
                        candidateFeature.addPropertie("compAngle", diff);
                        
                        //double distance = current_street.getGeometry().distance(sedge.getGeometry());
                        //candidateFeature.addPropertie("distanceFrom", distance);
                        
                        //Candidate scores are assigned
                        if (getLabel(current_street.getName()).equals(getLabel(sedge.getName()))) {
                            candidates.add(new T2(sedge, 100));
                        } else if (compAzimuth <= 10  && is_cycleway(sedge)) {
                            candidates.add(new T2(sedge, 80));
                        } else if (diff <= 10 && is_cycleway(sedge)) {
                            candidates.add(new T2(sedge, 60));
                        }
                        
                        //creating usefull geoJSON for debugging (So that can be seen why some streets weren't found)
                        if (getLabel(current_street.getName()).equals(getLabel(sedge.getName())) 
                                || (compAzimuth <= 10  && is_cycleway(sedge))) {
                            //LOG.info("Added edge:{}, {}", getLabel(sedge.getName()), sedge.getDistance());
                            /*if (forward) {
                                iter.add(sedge);
                            } else {
                                iter.add(sedge);
                                iter.previous();
                            }*/
                            
                            cycleways_features.add(candidateFeature);
                            //lengthen_cycleway(sedge, street_candidates, cycleways_features, forward, iter);
                        } else {
                            if (is_cycleway(sedge)) {
                                candidateFeature.addPropertie("stroke", "#40E0D0");
                                candidateFeature.addPropertie("info", "just cycleway (azimuth larg)");
                                if(diff < 20 ) {
                                    candidateFeature.addPropertie("stroke", "#8a8d8f");
                                    candidateFeature.addPropertie("info", "cycleway (angle small)");
                                }
                            } else {
                                candidateFeature.addPropertie("info", "not cycleway (azimuth larg)");
                                candidateFeature.addPropertie("stroke", "#7F00FF");
                            }
                            cycleways_features.add(candidateFeature);
                            
                        } 
                    }
                }
        //Collections.sort(candidates, t2_comp);
        try {
            //We get max score
            T2<StreetEdge, Integer> bestCandidate = Collections.max(candidates, t2_comp);
            StreetEdge sedge = bestCandidate.first;
            LOG.info("Best ca : {} {} {} ({}) {}", getLabel(sedge.getName()), sedge.getDistance(), compareAzimuth(current_street, sedge), bestCandidate.second, candidates.size());
            if (forward) {
                iter.add(sedge);
            } else {
                iter.add(sedge);
                iter.previous();
            }
            lengthen_cycleway(sedge, street_candidates, cycleways_features, forward, iter);
        } catch (NoSuchElementException e) {
        }
        return null;
    }

    /**
     * Checks if cand_street_edge is connected to any other found road
     * 
     * Sometimes roads are found which aren't connected to other parts of road network..
     * This is usually not the correct street which has cycleway next to it.
     * @param cand_street_edge Street that is checked
     * @param geometry_streets geometries of all street candidates
     * @return 
     */
    private boolean is_connected(StreetEdge cand_street_edge, Set<Geometry> geometry_streets) {
        List<Edge> outEdges = cand_street_edge.getToVertex().getOutgoingStreetEdges();
        //checks if this road is connected to any other found road
        for(Edge outEdge: outEdges) {
            if (outEdge.isReverseOf(cand_street_edge)) {
                continue;
            }
            StreetEdge soutEdge = (StreetEdge) outEdge;
            if (geometry_streets.contains(soutEdge.getGeometry())) {
                return true;
            }
        }
        
        //Same with incoming roads
        Collection<Edge> inEdges = cand_street_edge.getFromVertex().getIncoming();
        for(Edge inEdge: inEdges) {
            if (inEdge.isReverseOf(cand_street_edge) || (!(inEdge instanceof StreetEdge))) {
                continue;
            }
            StreetEdge sinEdge = (StreetEdge) inEdge;
            if (geometry_streets.contains(sinEdge.getGeometry())) {
                return true;
            }
        }
        return false;
    }    
}


