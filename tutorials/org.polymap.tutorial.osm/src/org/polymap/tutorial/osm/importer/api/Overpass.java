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
package org.polymap.tutorial.osm.importer.api;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.polymap.tutorial.osm.importer.TagFilter.ONE;
import static org.polymap.tutorial.osm.importer.TagFilter.ONE_OR_MORE;
import static org.polymap.tutorial.osm.importer.TagFilter.WILDCARDS;
import static org.polymap.tutorial.osm.importer.TagFilter.ZERO_OR_MORE;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Joiner;
import org.polymap.core.runtime.config.Config2;
import org.polymap.core.runtime.config.Configurable;
import org.polymap.core.runtime.config.DefaultString;

import org.polymap.tutorial.osm.importer.TagFilter;

/**
 * Builds URLs to query the Overpass API.
 *
 * @author Falko Bräutigam
 */
public class Overpass
        extends Configurable {

    private static final Log log = LogFactory.getLog( Overpass.class );
    
    public static final String DEFAULT_URL = "http://www.overpass-api.de/api/interpreter?data=";

    private static final Overpass   instance = new Overpass();
    
    /**
     * The global instance.
     */
    public static final Overpass instance() {
        return instance;    
    }
    
    public enum ResultType {
        Node, Way, Relation
    }
    
    // instance *******************************************
    
    @DefaultString( DEFAULT_URL )
    private Config2<Overpass,String>    baseUrl;
    
    
    public Query prepared( String query ) {
        return new Query() {
            @Override
            public URL downloadUrl() {
                return url( query );
            }
            @Override
            public int resultSize() throws IOException {
                throw new RuntimeException( "not yet implemented." );
            }
        };
    }

    
    public TagQuery query() {
        return new TagQuery();
    }
    
    
    /**
     * 
     */
    public abstract class Query {
        
        public abstract URL downloadUrl();
        
        public abstract int resultSize() throws IOException;

        protected URL url( String... parts ) {
            try {
                String queryString = Joiner.on( ";" ).skipNulls().join( parts );
                log.info( queryString );
                queryString = URLEncoder.encode( queryString, "UTF-8" );
                return new URL( baseUrl.get() + queryString );
            }
            catch (UnsupportedEncodingException | MalformedURLException e) {
                throw new RuntimeException( e );
            }
        }
    }
    
    
    /**
     * 
     */
    public class TagQuery
            extends Query {
        
        private String          bboxString = StringUtils.EMPTY;
        
        private String          tagsString = StringUtils.EMPTY;
        
        private int             maxResults = Integer.MAX_VALUE;
        
        private List<ResultType> types;
        

        public TagQuery whereBBox( ReferencedEnvelope bbox ) {
            assert isBlank( bboxString );
            List<Double> values = Arrays.asList( bbox.getMinY(), bbox.getMinX(), bbox.getMaxY(), bbox.getMaxX() );
            bboxString = "(" + Joiner.on( "," ).join( values ) + ")";
            return this;
        }
        
        
        public TagQuery whereTags( List<TagFilter> filters ) {
            assert isBlank( tagsString );
            tagsString = filters.stream()
                    .map( filter -> {
                        if (StringUtils.isBlank( filter.value() )) {
                            return "";  // don't filter empty values
                        }
                        else if (filter.value().equals( ZERO_OR_MORE )) {
                            return "[\"" + filter.key() + "\"]";
                        }
                        else {
                            String k = filter.key();

                            String v = StringUtils.replace( filter.value(), ONE, "." );
                            v = StringUtils.replace( v, ZERO_OR_MORE, ".*" );
                            v = StringUtils.replace( v, ONE_OR_MORE, ".+" );

                            String op = StringUtils.containsAny( filter.value(), WILDCARDS ) ? "~" : "=";

                            return Joiner.on( "\"" ).join( "[", k, op, v, "]" );
                        }
                    })
                    .collect( Collectors.joining() );            
            return this;
        }
        
        @SuppressWarnings( "hiding" )
        public TagQuery resultTypes( ResultType... types ) {
            assert this.types == null;
            this.types = Arrays.asList( types );
            return this;
        }
        
        public TagQuery maxResults( @SuppressWarnings( "hiding" ) int maxResults ) {
            this.maxResults = maxResults;
            return this;
        }
        
        
        protected String filterString() {
            if (isBlank( bboxString ) && isBlank( tagsString )) {
                return "";
            }
            StringJoiner result = new StringJoiner( "", "(", ")" );
            for (ResultType type : types) {
                result.add( type.name().toLowerCase() ).add( tagsString ).add( bboxString ).add( ";" );
                //result.add( ">;" );  // include required ways/nodes
            }
            return result.toString();
        }
        
        
        @Override
        public URL downloadUrl() {
            // create the OSM XML format with required nodes inside way entity
            return url( filterString(), "out geom " + maxResults + ";" );
        }
        
        
        @Override
        public int resultSize() throws IOException {
            URL countUrl = url( "[out:json]", filterString(), "out count;" );
            try (
                InputStream in = countUrl.openStream();
            ){
                String countJSONString = IOUtils.toString( in, "UTF-8" );
                JSONObject json = new JSONObject( countJSONString );
                log.info( json.toString( 4 ) );
                JSONObject tags = json.getJSONArray( "elements" ).getJSONObject( 0 ).getJSONObject( "tags" );
                int result = tags.getInt( "total" );
                log.info( "  -> " + result );
                return result;
            }
        }


        @Override
        public String toString() {
            return "TagQuery[" + filterString() + "]";
        }
    }

}
