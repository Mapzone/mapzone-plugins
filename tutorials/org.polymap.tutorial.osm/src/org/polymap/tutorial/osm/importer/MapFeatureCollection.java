/*
 * polymap.org Copyright (C) 2015 individual contributors as indicated by the
 * 
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
package org.polymap.tutorial.osm.importer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.collection.AbstractFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Joerg Reichert <joerg@mapzone.io>
 *
 */
public class MapFeatureCollection
        extends AbstractFeatureCollection {

    private ReferencedEnvelope              env      = null;

    private int                             size     = 0;

    public static String                    LAT_KEY  = "LAT";

    public static String                    LONG_KEY = "LON";

    private final List<String>              keys;

    private Double                          minLon  = -1d, maxLon = -1d, minLat = -1d, maxLat = -1d;

    private Map<Integer,Map<String,String>> features = new HashMap<Integer,Map<String,String>>();

    private final SimpleFeatureBuilder      featureBuilder;


    public MapFeatureCollection( String typeName, List<String> keys ) throws SchemaException {
        super( getMemberType( typeName, keys ) );
        featureBuilder = new SimpleFeatureBuilder( super.getSchema() );
        this.keys = keys;
    }


    private static SimpleFeatureType getMemberType( String typeName, List<String> keys ) throws SchemaException {
        StringBuffer typeSpec = new StringBuffer();
        typeSpec.append( "LAT" );
        typeSpec.append( "LONG" );
        keys.stream().forEach( key -> typeSpec.append( key ).append( ":String" ) );
        return DataUtilities.createType( typeName, typeSpec.toString() );
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.feature.collection.AbstractFeatureCollection#openIterator()
     */
    @Override
    protected Iterator<SimpleFeature> openIterator() {
        return new Iterator<SimpleFeature>() {

            private int currentPosition = 0;


            @Override
            public boolean hasNext() {
                return currentPosition < size;
            }


            @Override
            public SimpleFeature next() {
                Map<String,String> feature = features.get( currentPosition );
                featureBuilder.add( feature.get( LAT_KEY ) );
                featureBuilder.add( feature.get( LONG_KEY ) );
                for (String key : keys) {
                    featureBuilder.add( feature.get( key ) );
                }
                return featureBuilder.buildFeature( null );
            }
        };
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.feature.collection.AbstractFeatureCollection#size()
     */
    @Override
    public int size() {
        return size;
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.feature.collection.AbstractFeatureCollection#getBounds()
     */
    @Override
    public ReferencedEnvelope getBounds() {
        if (env == null) {
            env = new ReferencedEnvelope(minLon, maxLon, minLat, maxLat, schema.getCoordinateReferenceSystem());
        }
        return env;
    }

    public void add( double longitude, double latitude, Map<String,String> feature ) {
        feature.put( LAT_KEY, String.valueOf( latitude ) );
        feature.put( LONG_KEY, String.valueOf( longitude ) );
        boolean changed = false;
        if(minLon == -1 || minLon > longitude ) {
            minLon = longitude;
            changed = true;
        }
        if(maxLon == -1 || maxLon < longitude ) {
            maxLon = longitude;
            changed = true;
        }
        if(minLat == -1 || minLat > latitude ) {
            minLat = latitude;
            changed = true;
        }
        if(maxLat == -1 || maxLat < latitude ) {
            maxLat = latitude;
            changed = true;
        }
        features.put( size, feature );
        size++;
        if(changed) {
            env = null;
        }
    }
}
