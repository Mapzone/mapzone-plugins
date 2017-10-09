/* 
 * polymap.org
 * Copyright (C) 2017, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.tutorial.osm.importer.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.core.runtime.Timer;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class OsmXmlParser<T>
        implements Iterator<T>, AutoCloseable {

    private static final Log log = LogFactory.getLog( OsmXmlParser.class );
    
    /** The initial capacity of Node/NodeRef lists. */
    public static final int         INITIAL_ARRAY_SIZE = 100;
    
    private Map<String,Class>       types = new HashMap();

    private Unmarshaller            um;
    
    private XMLStreamReader         reader;
    
    private T                       next;

    /**
     * 
     * 
     * @param in
     * @param types Array of root element types to parse.
     * @throws JAXBException
     * @throws XMLStreamException
     */
    public OsmXmlParser( InputStream in, Class<?>... types ) 
            throws JAXBException, XMLStreamException {
        JAXBContext jaxbContext = JAXBContext.newInstance( types );
        um = jaxbContext.createUnmarshaller();
 
        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        reader = xmlFactory.createXMLStreamReader( in );
        
        for (Class<?> type : types) {
            XmlRootElement a = type.getAnnotation( XmlRootElement.class );
            this.types.put( a.name(), type );
        }

    }
    
    @Override
    public void close() {
        if (reader != null) {
            try { reader.close(); }
            catch (XMLStreamException e) {
                throw new RuntimeException( e );
            }
            reader = null;
        }
    }

    /**
     * 
     * @throws RuntimeException Wrapping {@link JAXBElement}
     */
    @Override
    public boolean hasNext() {
        try {
            if (next == null && reader.hasNext()) {
                next = parseNext();
            }
            return next != null;
        }
        catch (JAXBException | XMLStreamException e) {
            throw new RuntimeException( e );
        }
    }


    /**
     *
     * @return The next valid parsed root element, or null.
     */
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        try {
            return next;
        }
        finally {
            next = null;
        }
    }
    

    /**
     * 
     *
     * @return The next valid parsed root element, or null.
     */
    public T parseNext() throws XMLStreamException, JAXBException {
        // skip to the next element
        while (reader.hasNext() && 
                (!reader.isStartElement() || !types.containsKey( reader.getLocalName() ))) {
            reader.next();
        }
        // read an entity
        if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            Class type = types.get( reader.getLocalName() );
            JAXBElement<?> element = um.unmarshal( reader, type );
            return (T)element.getValue();
        }
        else {
            return null;
        }
    }
    

    /** Base class for OSM XML entity classes. */
    @XmlSeeAlso( Tag.class )
    public static class Entity {

        @XmlAttribute( name="id" )
        public long         id;
        
        @XmlElement( name="tag" )
        public List<Tag>    tags = new ArrayList( 10 );
    }

    
    /** */
    public static class Tag {

        @XmlAttribute( name="k" )
        public String       key;
        
        @XmlAttribute( name="v" )
        public String       value;

        @Override
        public String toString() {
            return "Tag[key=" + key + ", value=" + value + "]";
        }
    }

    
    /** */
    @XmlRootElement( name="node" )
    public static class Node
            extends Entity {

        @XmlAttribute( name="lat" )
        public double       lat = Double.NaN;
        
        @XmlAttribute( name="lon" )
        public double       lon = Double.NaN;

        @Override
        public String toString() {
            return "Node[id=" + id + ", tags=" + tags + ", lat=" + lat + ", lon=" + lon + "]";
        }
    }
    

    /** */
    @XmlRootElement( name="way" )
    @XmlSeeAlso( {NodeRef.class, Bounds.class} )
    public static class Way
            extends Entity {

        @XmlElement( name="bounds" )
        public Bounds        bounds;
        
        @XmlElement( name="nd" )
        public List<NodeRef> nodes = new ArrayList( INITIAL_ARRAY_SIZE );

        @Override
        public String toString() {
            return "Way [nodes=" + nodes + "]";
        }
    }

    
    /** */
    public static class Bounds {
        
        @XmlAttribute
        public double       minlat;
        
        @XmlAttribute
        public double       minlon;
        
        @XmlAttribute
        public double       maxlat;
        
        @XmlAttribute
        public double       maxlon;
    }

    
    /** */
    public static class NodeRef {
        
        @XmlAttribute( name="ref" )
        public Long         nodeId;
        
        @XmlAttribute( name="lat" )
        public double       lat;
        
        @XmlAttribute( name="lon" )
        public double       lon;

        public boolean equalsGeom( NodeRef other ) {
            return lat == other.lat && lon == other.lon;
        }

        @Override
        public String toString() {
            return "NodeRef[" + lat + "-" + lon + "]";
        }
    }

    
    /** */
    @XmlRootElement( name="relation" )
    @XmlSeeAlso( Member.class )
    public static class Relation 
            extends Entity {

        @XmlElement( name="bounds" )
        public Bounds       bounds;
        
        @XmlElement( name="member" )
        public List<Member> members = new ArrayList( INITIAL_ARRAY_SIZE );

        @Override
        public String toString() {
            return "Relation[id=" + id + ", members=" + members + "]";
        }
    }

    
    /** */
    public static class Member {
        
        @XmlAttribute( name="type" )
        public String       type;

        @XmlAttribute( name="ref" )
        public String       entityId;

        @XmlAttribute( name="role" )
        public String       role;

        @XmlElement( name="nd" )
        public List<NodeRef> nodes;

        @Override
        public String toString() {
            return "Member[type=" + type + ", role=" + role + ", nodes=" + nodes + "]";
        }
    }
    
    
    /**
     * Test
     */
    @SuppressWarnings( "unused" )
    public static void main( String[] args ) throws Exception {
        try (
            InputStream in = new BufferedInputStream( new FileInputStream( new File( "/home/falko/Data/osm/andorra-latest.osm.xml" ) ) );
            OsmXmlParser<Entity> parser = new OsmXmlParser( in, Node.class, Way.class, Relation.class );
        ){  
            Timer timer = new Timer();
            int count = 0;
            while (parser.hasNext()) {
                Entity entity = parser.next();
//                if (entity instanceof Relation) {
//                    System.out.println( "Entity: " + entity );
//                }
                count ++;
            }
            System.out.println( "Parsed: " + count + " entities in " + timer.elapsedTime() + "ms" );
        }
    }
    
}
