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
package org.polymap.tutorial.osm.importer.api;

import static org.polymap.tutorial.osm.importer.api.Overpass.ResultType.Node;
import static org.polymap.tutorial.osm.importer.api.Overpass.ResultType.Relation;
import static org.polymap.tutorial.osm.importer.api.Overpass.ResultType.Way;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

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
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.polymap.tutorial.osm.importer.api.Overpass.Query;
import org.polymap.tutorial.osm.importer.api.Overpass.ResultType;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Bounds;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Entity;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Member;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Node;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.NodeRef;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Relation;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Tag;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Way;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParserFeatureIterator;

/**
 * Transforms OSM entities fetched from an Overpass {@link Query} and created by an
 * {@link OsmXmlParser} into {@link SimpleFeature} instances.
 * <p/>
 * Overpass supports a special style ("out geom;") of OSM XML entities that
 * incoorporates all required nodes in the root entitity. This iterator uses this
 * format for efficiently streaming data.
 * 
 * @author Falko Bräutigam <falko@mapzone.io>
 */
public class OverpassFeatureIterator
        extends OsmXmlParserFeatureIterator {

    private static final Log log = LogFactory.getLog( OverpassFeatureIterator.class );

    public static ResultType[] resultTypesFor( Class<? extends Geometry> geomType ) {
        // Point
        if (geomType.isAssignableFrom( Point.class )) {
            return new ResultType[] {Node};
        }
        // MultiPoint
        else if (geomType.isAssignableFrom( MultiPoint.class )) {
            return new ResultType[] {Node, Relation};
        }
        // LineString, Polygon
        else if (geomType.isAssignableFrom( LineString.class )
                || geomType.isAssignableFrom( Polygon.class )) {
            return new ResultType[] {Way};
        }
        // MultLineString, Polygon
        else if (geomType.isAssignableFrom( MultiLineString.class )
                || geomType.isAssignableFrom( MultiPolygon.class )) {
            return new ResultType[] {Way, Relation};
        }
        else {
            throw new RuntimeException( "unsupported geom type: " + geomType );
        }
    }

    // instance *******************************************
    
    public OverpassFeatureIterator( SimpleFeatureType schema, InputStream in )
            throws IOException, JAXBException, XMLStreamException {
        super( schema, in );
    }


    public OverpassFeatureIterator( SimpleFeatureType schema, URL url )
            throws IOException, JAXBException, XMLStreamException {
        super( schema, url );
    }


    @Override
    public boolean hasNext() {
        Class<?> geomType = schema.getGeometryDescriptor().getType().getBinding();
        while (next == null && parser.hasNext()) {
            try {
                Entity entity = parser.next();
               // log.info( "" + entity );
                
                // attributes
                createAttributes( entity );
                
                // geom
                Geometry geom = null;
                if (geomType.isAssignableFrom( Point.class )) {
                    geom = createPoint( (Node)entity );
                }
//                else if (geomType.isAssignableFrom( LineString.class )) {
//                    Way way = (Way)entity;
//                    geom = createLineString( way.nodes, way.bounds );
//                }
//                else if (geomType.isAssignableFrom( Polygon.class )) {
//                    Way way = (Way)entity;
//                    geom = createPolygon( way.nodes, way.bounds, true ).orElse( null );
//                }
                else if (geomType.isAssignableFrom( MultiLineString.class )) {
                    geom = entity instanceof Way
                            ? createLineString( ((Way)entity).nodes, ((Way)entity).bounds )
                            : createMultiLineString( (Relation)entity );        
                }
                else if (geomType.isAssignableFrom( MultiPolygon.class )) {
                    if (entity instanceof Way) {
                        Way way = (Way)entity;
                        geom = createPolygon( way.nodes, way.bounds, false )
                                .map( polygon -> gf.createMultiPolygon( new Polygon[] {polygon} ) )
                                .orElse( null );
                    }
                    else {
                        geom = createMultiPolygon( (Relation)entity );
                        
                    }
                }
                else {
                    throw new RuntimeException( "Unsupported geom type: " + geomType );
                }
                if (geom != null) {
                    fb.set( "geom", geom );
                    next = fb.buildFeature( null );
                }
            }
            catch (Exception e) {
                log.warn( "", e );
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
    

    protected void createAttributes( Entity entity ) {
        for (Tag tag : entity.tags) {
            String name = tag.key.replaceAll( ":", "_" );
            if (schema.getDescriptor( name ) != null) {
                fb.set( name, tag.value );
            }
        }
    }
    
    
    protected Point createPoint( Node node ) {
        return gf.createPoint( new Coordinate( node.lon, node.lat ) );
    }
    
    
    protected LineString createLineString( List<NodeRef> nodes, Bounds bounds ) {
        return gf.createLineString( new NodeSequence( nodes, bounds ) );
    }

    
    protected Optional<Polygon> createPolygon( List<NodeRef> nodes, Bounds bounds, boolean close ) {
        NodeSequence sequence = new NodeSequence( nodes, bounds );
        Polygon result = null;
        if (!sequence.isValidPolygon()) {
            // not valid
            log.info( "No valid polygon sequence: " + nodes );
        }
        else if (!sequence.isClosedRing()) {
            if (close) {
                result = gf.createPolygon( sequence.closeRing() );
            }
            else {
                log.info( "No closed ring: " + nodes );
            }
        }
        else {
            result = gf.createPolygon( sequence );
        }
        result = normalize( result );
        return Optional.ofNullable( result );
    }

    
    protected MultiLineString createMultiLineString( Relation rel ) {
        return gf.createMultiLineString( rel.members.stream()
                .filter( member ->
                    "way".equals( member.type ) ||  // rivers have a node role="spring"
                    "relation".equals( member.type )  // pyramide
                )
                .map( member -> {
                    if ("way".equals( member.type )) {
                        return createLineString( member.nodes, null );
                    }
                    else if ("relation".equals( member.type )) {
                        throw new RuntimeException( "Unsupported pyramidal construction: " + member + "" );
                    }
                    throw new RuntimeException( "Unsupported member type: " + member );
                })
                .toArray( LineString[]::new ) );
    }


    /** Hack to get around non-final-var-inside-lambda problem */
    private Geometry _result;
    
    protected MultiPolygon createMultiPolygon( Relation rel ) {
        // outer
        List<Polygon> outers = new ArrayList( rel.members.size() );
        for (Member member : rel.members) {
            if ("outer".equals( member.role ) && "way".equals( member.type )) {
                createPolygon( member.nodes, null, true )
                        .ifPresent( outer -> outers.add( outer ) );
            }
        }
        _result = gf.createMultiPolygon( outers.toArray( new Polygon[outers.size()] ) );

        // inner
        for (Member member : rel.members) {
            if ("inner".equals( member.role ) && "way".equals( member.type )) {
                _result = createPolygon( member.nodes, null, true )
                        .map( inner -> _result.difference( inner ) )
                        .orElse( _result );
            }
        }
        _result = normalize( _result );
        return _result instanceof Polygon
                ? gf.createMultiPolygon( new Polygon[] {(Polygon)_result} )
                : (MultiPolygon)_result;
    }


    /**
     * Repairs invalid polygons.
     */
    protected <G extends Geometry> G normalize( G geom ) {
        if (geom != null && !geom.isValid()) {
            geom = (G)geom.buffer( 0 );
            assert geom.isValid();
        }
        return geom;
    }
    
    
    /**
     * 
     */
    protected static class NodeSequence
            implements CoordinateSequence {

        protected List<OsmXmlParser.NodeRef>    nodes;
        
        private Bounds                          bounds;
        

        public NodeSequence( List<NodeRef> nodes, Bounds bounds ) {
            assert nodes.stream().allMatch( n -> n.lat != 0 && n.lon != 0 );
            this.nodes = nodes;
            this.bounds = bounds;
        }

        public NodeSequence closeRing() {
            NodeRef first = nodes.get( 0 );
            NodeRef last = nodes.get( nodes.size()-1 );
            if (!last.equalsGeom( first )) {
                nodes.add( first );
            }
            return this;
        }

        public boolean isClosedRing() {
            if (nodes.size() >= 3) {
                NodeRef first = nodes.get( 0 );
                NodeRef last = nodes.get( nodes.size()-1 );
                return first.equalsGeom( last );
            }
            else {
                return false;
            }
        }
        
        public boolean isValidPolygon() {
            return nodes.size() > 3 /*&& isClosedRing()*/;
        }
        
        @Override
        public int getDimension() {
            return 2;
        }

        @Override
        public Coordinate getCoordinate( int i ) {
            NodeRef node = nodes.get( i );
            return new Coordinate( node.lon, node.lat );
        }

        @Override
        public Coordinate getCoordinateCopy( int i ) {
            return getCoordinate( i );
        }

        @Override
        public void getCoordinate( int i, Coordinate coord ) {
            NodeRef node = nodes.get( i );
            coord.x = node.lon;
            coord.y = node.lat;
        }

        @Override
        public double getX( int i ) {
            return nodes.get( i ).lon;
        }

        @Override
        public double getY( int i ) {
            return nodes.get( i ).lat;
        }

        @Override
        public double getOrdinate( int i, int ordinateIndex ) {
            switch (ordinateIndex) {
                case 0: return nodes.get( i ).lon;
                case 1: return nodes.get( i ).lat;
                default: throw new RuntimeException();
            }
        }

        @Override
        public int size() {
            return nodes.size();
        }

        @Override
        public void setOrdinate( int index, int ordinateIndex, double value ) {
            throw new RuntimeException( "not yet implemented." );
        }

        @Override
        public Coordinate[] toCoordinateArray() {
            return nodes.stream().map( node -> new Coordinate( node.lon, node.lat ) ).toArray( Coordinate[]::new );
        }

        @Override
        public Envelope expandEnvelope( Envelope env ) {
            if (bounds != null) {
                assert bounds.minlon != 0 && bounds.minlat != 0 && bounds.maxlon != 0 && bounds.maxlat != 0;
                env.expandToInclude( bounds.minlon, bounds.minlat );
                env.expandToInclude( bounds.maxlon, bounds.maxlat );
            }
            else {
                nodes.stream().forEach( node -> env.expandToInclude( node.lon, node.lat ) );
            }
            return env;
        }

        @Override
        public Object clone() {
            throw new RuntimeException( "not yet implemented." );
        }
    }
    
}