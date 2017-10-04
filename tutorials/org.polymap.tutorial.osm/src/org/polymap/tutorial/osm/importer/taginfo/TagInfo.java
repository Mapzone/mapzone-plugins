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
package org.polymap.tutorial.osm.importer.taginfo;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class TagInfo {

    private static final Log log = LogFactory.getLog( TagInfo.class );
    
//    private static final TagInfo    instance = new TagInfoAPI();
//    
//    /**
//     * The global instance.
//     */
//    public static TagInfo instance() {
//        return instance;
//    }

    /**
     * 
     */
    public static abstract class ResultSet<T>
            implements Iterable<T> {
    
        public abstract int size();
        
        public Stream<T> stream() {
            return StreamSupport.stream( spliterator(), false );
        }
    }

    /**
     * 
     */
    public static enum Sort {
        key, count_all, count_nodes, count_ways, count_relations, values_all, users_all, in_wiki, length
    }
    
    // instance *******************************************

    /**
     * Query tag keys.
     *
     * @param query Only show keys matching this query (substring match, optional).
     * @param sort
     * @param maxResults
     * @throws Exception 
     */
    public abstract ResultSet<String> keys( String query, Sort sort, int maxResults ) throws Exception;
    
    /**
     * Query values for the given key.
     *
     * @param key The tag key.
     * @param query Only show results where the value matches this query (substring match, optional).
     * @param sort
     * @param maxResults
     * @throws Exception 
     */
    public abstract ResultSet<String> values( String key, String query, Sort sort, int maxResults ) throws Exception;
    
}
