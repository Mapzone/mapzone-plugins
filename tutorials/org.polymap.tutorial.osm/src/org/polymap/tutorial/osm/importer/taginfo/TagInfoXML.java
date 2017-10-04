/*
 * polymap.org 
 * Copyright (C) 2015-2017 individual contributors as indicated by the
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
package org.polymap.tutorial.osm.importer.taginfo;

import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import java.io.InputStream;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;

/**
 * Gets tag info from OSM XML file.
 * <p/>
 * <b>Not yet ported to new TagInfo API!</b>
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 */
public class TagInfoXML
        extends TagInfo {

    @Override
    public ResultSet<String> keys( String query, Sort sort, int maxResults ) throws Exception {
        throw new RuntimeException( "not yet ported to new TagInfo API" );
    }


    @Override
    public ResultSet<String> values( String key, String query, Sort sort, int maxResults ) {
        throw new RuntimeException( "not yet ported to new TagInfo API" );
    }

    
    public static TreeMap<String,SortedSet<String>> getTagsFromContent( InputStream input ) throws Exception {
        TreeMap<String,SortedSet<String>> allTags = new TreeMap<String,SortedSet<String>>();
        OsmIterator iterator = new OsmXmlIterator( input, false );
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Node) {
                OsmNode node = (OsmNode)container.getEntity();
                Map<String,String> tags = OsmModelUtil.getTagsAsMap( node );
                for (Entry<String,String> entry : tags.entrySet()) {
                    collectTags( allTags, entry.getKey(), entry.getValue() );
                }
            }
        }
        return allTags;
    }


    private static void collectTags( SortedMap<String,SortedSet<String>> tags, String key, String value ) {
        SortedSet<String> values = tags.get( key );
        if (values == null) {
            values = new TreeSet<String>();
            tags.put( key, values );
        }
        if (value != null && !values.contains( value )) {
            values.add( value );
        }
    }
}
