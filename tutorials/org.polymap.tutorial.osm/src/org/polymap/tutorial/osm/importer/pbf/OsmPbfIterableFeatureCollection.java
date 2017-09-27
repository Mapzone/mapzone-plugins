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
package org.polymap.tutorial.osm.importer.pbf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.geotools.feature.SchemaException;
import org.geotools.feature.collection.AbstractFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Point;

/**
 * @author Joerg Reichert <joerg@mapzone.io>
 *
 */
public class OsmPbfIterableFeatureCollection
        extends AbstractFeatureCollection {

    private ReferencedEnvelope              env                 = null;

    private Double                          minLon              = -1d;

    private Double                          maxLon              = -1d;

    private Double                          minLat              = -1d;

    private Double                          maxLat              = -1d;

    private final List<Pair<String,String>> filters;

    private final URL                       url;

    private Exception                       exception           = null;

    private List<OsmPbfFeatureIterator>     osmFeatureIterators = new ArrayList<OsmPbfFeatureIterator>();

    private int                             size;

    private int                             limit               = -1;


    public OsmPbfIterableFeatureCollection( String typeName, File file, List<Pair<String,String>> filters )
            throws FileNotFoundException, MalformedURLException {
        super( getMemberType( typeName, getKeys( filters ) ) );
        this.url = file.toURI().toURL();
        this.filters = filters;
    }


    static List<String> getKeys( List<Pair<String,String>> filters ) {
        return filters.stream().map( tag -> tag.getKey() ).collect( Collectors.toList() );
    }


    public OsmPbfIterableFeatureCollection( String typeName, URL url, List<Pair<String,String>> filters )
            throws SchemaException, IOException
    {
        super( getMemberType( typeName, getKeys( filters ) ) );
        this.url = url;
        this.filters = new ArrayList<Pair<String,String>>();
    }


    private static SimpleFeatureType getMemberType( String typeName, List<String> keys ) {
        final SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.setName( typeName );
        featureTypeBuilder.setCRS( DefaultGeographicCRS.WGS84 );
        featureTypeBuilder.setDefaultGeometry( "theGeom" );
        featureTypeBuilder.add( "theGeom", Point.class );
        keys.forEach( key -> featureTypeBuilder.add( key, String.class ) );
        return featureTypeBuilder.buildFeatureType();
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.feature.collection.AbstractFeatureCollection#openIterator()
     */
    @Override
    protected Iterator<SimpleFeature> openIterator() {
        try {
            OsmPbfFeatureIterator osmFeatureIterator = new OsmPbfFeatureIterator( this, limit );
            osmFeatureIterators.add( osmFeatureIterator );
            return osmFeatureIterator;
        }
        catch (IOException e) {
            exception = e;
            return new ArrayList<SimpleFeature>().iterator();
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.feature.collection.AbstractFeatureCollection#size()
     */
    @Override
    public int size() {
        if (size == -1) {
            if (osmFeatureIterators.size() == 0) {
                openIterator();
            }
            if (osmFeatureIterators.size() > 0) {
                OsmPbfFeatureIterator osmFeatureIterator = osmFeatureIterators.get( 0 );
                size = osmFeatureIterator.size();
            }
        }
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
            env = new ReferencedEnvelope( minLon, maxLon, minLat, maxLat, schema.getCoordinateReferenceSystem() );
        }
        return env;
    }


    void updateBBOX( double longitude, double latitude ) {
        boolean changed = false;
        if (this.minLon == -1 || this.minLon > longitude) {
            this.minLon = longitude;
            changed = true;
        }
        if (this.maxLon == -1 || this.maxLon < longitude) {
            this.maxLon = longitude;
            changed = true;
        }
        if (this.minLat == -1 || this.minLat > latitude) {
            this.minLat = latitude;
            changed = true;
        }
        if (this.maxLat == -1 || this.maxLat < latitude) {
            this.maxLat = latitude;
            changed = true;
        }
        if (changed) {
            this.env = null;
        }
    }


    public void complete() {
        for (OsmPbfFeatureIterator osmFeatureIterator : osmFeatureIterators) {
            osmFeatureIterator.complete();
        }
    }


    public Exception getException() {
        return exception;
    }


    public void setException( Exception e ) {
        this.exception = e;
    }


    public List<Pair<String,String>> getFilters() {
        return filters;
    }


    public URL getUrl() {
        return url;
    }


    public void setLimit( int limit ) {
        this.limit = limit;
        this.size = limit;
    }    
}
