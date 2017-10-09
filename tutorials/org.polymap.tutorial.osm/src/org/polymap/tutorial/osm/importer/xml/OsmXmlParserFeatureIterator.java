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

import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.GeometryFactory;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Entity;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Node;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Relation;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Tag;
import org.polymap.tutorial.osm.importer.xml.OsmXmlParser.Way;

/**
 * Transforms OSM entities created by an {@link OsmXmlParser} into
 * {@link SimpleFeature} instances.
 * 
 * @author Falko Bräutigam <falko@mapzone.io>
 */
public abstract class OsmXmlParserFeatureIterator
        implements Iterator<SimpleFeature> {

    private static final Log log = LogFactory.getLog( OsmXmlParserFeatureIterator.class );

    public static final GeometryFactory     gf = new GeometryFactory();

    protected final SimpleFeatureBuilder    fb;

    protected URL                           url; 
    
    protected SimpleFeatureType             schema; 

    protected OsmXmlParser<Entity>          parser;

    protected InputStream                   in;

    protected SimpleFeature                 next;


    public OsmXmlParserFeatureIterator( SimpleFeatureType schema, URL url ) 
            throws IOException, JAXBException, XMLStreamException {
        this.url = url;
        this.schema = schema;
        fb = new SimpleFeatureBuilder( schema );

        URLConnection conn = url.openConnection();
        conn.setRequestProperty( "Accept-Encoding", "gzip" );
        in = conn.getInputStream();

        log.info( "Encoding: " + conn.getContentEncoding() );
        if ("gzip".equals( conn.getContentEncoding() )) {
            in = new GZIPInputStream( in, 4*1024 );
        }
        
        parser = new OsmXmlParser( in, Node.class, Way.class, Relation.class );
    }


    public OsmXmlParserFeatureIterator( SimpleFeatureType schema, InputStream in ) 
            throws IOException, JAXBException, XMLStreamException {
        this.schema = schema;
        fb = new SimpleFeatureBuilder( schema );
        parser = new OsmXmlParser( in, Node.class, Way.class, Relation.class );
    }


    protected void createAttributes( Entity entity ) {
        for (Tag tag : entity.tags) {
            if (schema.getDescriptor( tag.key ) != null) {
                fb.set( tag.key, tag.value );
            }
        }
    }
    
    
    public void close() {
        if (in != null) {
            IOUtils.closeQuietly( in );
            in = null;
            parser.close();
            parser = null;
        }
    }


    @Override
    protected void finalize() throws Throwable {
        close();
    }

}