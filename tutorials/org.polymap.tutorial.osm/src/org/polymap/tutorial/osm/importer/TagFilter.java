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
package org.polymap.tutorial.osm.importer;

import java.util.List;

import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class TagFilter {

    private static final Log log = LogFactory.getLog( TagFilter.class );
    
    /** Wildcard: zero, one or more characters. */
    public static final String          ZERO_OR_MORE = "*";
    
    /** Wildcard: zero, one or more characters. */
    public static final String          ONE_OR_MORE = "+";
    
    /** Wildcard: exactly one character. */
    public static final String          ONE = "?";
    
    public static final String          WILDCARDS = ONE_OR_MORE + ZERO_OR_MORE + ONE;
    
    public static TagFilter of( String key, String value ) {
        return new TagFilter( key, value );
    }
    
    /**
     * Builds a {@link SimpleFeatureType} out of the keys of the given
     * list of {@link TagFilter}s.
     * @param class1 
     */
    public static SimpleFeatureType schemaOf( String typeName, List<TagFilter> filters, Class<? extends Geometry> geom ) {
        final SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName( new NameImpl( null, typeName ) );
        ftb.setCRS( DefaultGeographicCRS.WGS84 );
        ftb.setDefaultGeometry( "geom" );
        ftb.add( "geom", geom );
        filters.forEach( filter -> ftb.add( filter.key(), String.class ) );
        return ftb.buildFeatureType();
    }

    // instance *******************************************
    
    private String          key;
    
    private String          value;
    
    
    public TagFilter( String key, String value ) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "TagFilter[key=" + key + ", value=" + value + "]";
    }

    public String key() {
        return key;
    }
    
    public String value() {
        return value;
    }

    public boolean matches( String matchKey, String matchValue ) {
        throw new RuntimeException( "not yet implemented" );
    }
    
    public boolean matches( SimpleFeature feature ) {
        throw new RuntimeException( "not yet implemented" );
    }
    
//    public boolean matches( OsmEntity entity ) {
//        for (int i=0; i<entity.getNumberOfTags(); i++) {
//            OsmTag tag = entity.getTag( i );
//            if (matches( tag.getKey(), tag.getValue() )) {
//                return true;
//            }
//        }
//        return false;
//    }
}
