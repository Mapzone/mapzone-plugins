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

import java.util.Arrays;
import java.util.List;
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
    
    // instance *******************************************
    
    @DefaultString( DEFAULT_URL )
    private Config2<Overpass,String>    baseUrl;
    
    
    public Query query() {
        return new Query();
    }
    
    
    /**
     * 
     */
    public class Query {
        
        private String          bboxString = StringUtils.EMPTY;
        
        private String          tagsString = StringUtils.EMPTY;
        

        public Query whereBBox( ReferencedEnvelope bbox ) {
            assert isBlank( bboxString );
            List<Double> values = Arrays.asList( bbox.getMinY(), bbox.getMinX(), bbox.getMaxY(), bbox.getMaxX() );
            bboxString = "(" + Joiner.on( "," ).join( values ) + ")";
            return this;
        }
        
        
        public Query whereTags( List<TagFilter> filters ) {
            assert isBlank( tagsString );
            List<String> formattedFilters = filters.stream()
                    .filter( filter -> !"*".equals( filter.key() ) )
                    .map( filter -> {
                        String filterStr;
                        String keyStr;
                        if ("".equals( filter.key() )) {
                            keyStr = "~\"^$\"";
                        }
                        else {
                            keyStr = "\"" + filter.key() + "\"";
                        }
                        if ("*".equals( filter.value() )) {
                            filterStr = keyStr;
                        }
                        else if ("".equals( filter.value() )) {
                            filterStr = keyStr + "~\"^$\"";
                        }
                        else {
                            filterStr = keyStr + "=\"" + filter.value() + "\"";
                        }
                        return filterStr;
                    })
                    .collect( Collectors.toList() );

            if (filters.size() > 0 && !"*".equals( filters.get( 0 ).key() )) {
                tagsString = "[" + Joiner.on( "][" ).join( formattedFilters ) + "]";
            }
            else {
                tagsString = "";
            }
            return this;
        }
        
        
        protected String filterString() {
            return !isBlank( bboxString ) || !isBlank( tagsString ) 
                    ? "node" + tagsString + bboxString
                    : null;
        }
        
        
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
        
        
        public URL downloadUrl( int fetchCount ) {
            return url( filterString(), "out " + fetchCount + ";" );
        }
        
        
        public int resultSize() throws IOException {
            URL countUrl = url( "[out:json]", filterString(), "out count;" );
            try (
                InputStream in = countUrl.openStream();
            ){
                String countJSONString = IOUtils.toString( in, "UTF-8" );
                JSONObject json = new JSONObject( countJSONString );
                log.info( json.toString( 4 ) );
                int result = json.getJSONArray( "elements" ).getJSONObject( 0 ).getJSONObject( "tags" ).getInt( "nodes" );
                log.info( "  -> " + result );
                return result;
            }
        }
    }


}
