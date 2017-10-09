/*
 * polymap.org 
 * Copyright (C) 2017 individual contributors as indicated by the
 * @authors tag. All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.tutorial.osm.importer.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Entity;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Node;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Way;

/**
 * Transforms OSM entities created by an {@link OsmXmlParser} into
 * {@link SimpleFeature} instances.
 * 
 * @author Falko Bräutigam <falko@mapzone.io>
 */
public class OsmXmlFeatureIterator
        extends OsmXmlParserFeatureIterator {

    private static final Log log = LogFactory.getLog( OsmXmlFeatureIterator.class );

    private Map<Long,OsmXmlParser.Node>     nodes = new HashMap( 1024 );

    private Map<Long,OsmXmlParser.Way>      ways = new HashMap( 1024 );


    public OsmXmlFeatureIterator( SimpleFeatureType schema, InputStream in )
            throws IOException, JAXBException, XMLStreamException {
        super( schema, in );
    }


    public OsmXmlFeatureIterator( SimpleFeatureType schema, URL url ) 
            throws IOException, JAXBException, XMLStreamException {
        super( schema, url );
    }


    @Override
    public boolean hasNext() {
        Class<?> geomType = schema.getGeometryDescriptor().getType().getBinding();
        while (next == null && parser.hasNext()) {
            Entity entity = parser.next();
            log.info( "" + entity );
            
            // try to build geometry
            Geometry geom = null;
            if (entity instanceof Node && geomType.isAssignableFrom( Point.class )) {
                geom = createPoint( entity );
            }
            if (entity instanceof Way && geomType.isAssignableFrom( LineString.class )) {
                geom = createLineString( entity );
            }
            
            // target geometry type -> build feature
            if (geom != null) {
                createAttributes( entity );
                fb.set( "geom", geom );
                next = fb.buildFeature( null );
            }
            // store node
            else if (entity instanceof Node) {
                nodes.put( entity.id, (Node)entity );
            }
            // store way
            else if (entity instanceof Way) {
                ways.put( entity.id, (Way)entity );
            }
            else {
                throw new RuntimeException();
            }
        }
        return next != null;
    }


    @Override
    public SimpleFeature next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        try { return next; } finally { next = null; }
    }
    

    protected Point createPoint( Entity entity) {
        return gf.createPoint( new Coordinate( ((Node)entity).lon, ((Node)entity).lat ) );
    }
    
    
    protected LineString createLineString( Entity entity) {
        return gf.createLineString( ((Way)entity).nodes.stream()
                .map( nodeRef -> {
                    if (nodeRef.lat > 0) { 
                        return new Coordinate( nodeRef.lon, nodeRef.lat );
                    }
                    else {
                        Node node = nodes.get( nodeRef.nodeId );
                        return new Coordinate( node.lon, node.lat );              
                    }
                })
                .toArray( Coordinate[]::new ) );
    }

}