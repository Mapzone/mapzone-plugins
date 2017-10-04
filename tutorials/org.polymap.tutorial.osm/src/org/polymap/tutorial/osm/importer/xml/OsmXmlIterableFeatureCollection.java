/*
 * polymap.org 
 * Copyright (C) 2015 individual contributors as indicated by the
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.feature.collection.AbstractFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Point;

import org.polymap.tutorial.osm.importer.TagFilter;

/**
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 */
public class OsmXmlIterableFeatureCollection
        extends AbstractFeatureCollection {

    public static List<String> getKeys( List<TagFilter> filters ) {
        return filters.stream().map( tag -> tag.key() ).collect( Collectors.toList() );
    }


    public static SimpleFeatureType schema( String typeName, List<String> keys ) {
        final SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.setName( new NameImpl( null, typeName ) );
        featureTypeBuilder.setCRS( DefaultGeographicCRS.WGS84 );
        featureTypeBuilder.setDefaultGeometry( "theGeom" );
        featureTypeBuilder.add( "theGeom", Point.class );
        keys.forEach( key -> featureTypeBuilder.add( key, String.class ) );
        return featureTypeBuilder.buildFeatureType();
    }

    // instance *******************************************

    private ReferencedEnvelope              env;

    private Double                          minLon = -1d;

    private Double                          maxLon = -1d;

    private Double                          minLat = -1d;

    private Double                          maxLat = -1d;

    private final Map<String,String>        filters = new HashMap();

    private final URL                       url;

    private Exception                       exception;

    private List<OsmXmlFeatureIterator>     osmFeatureIterators = new ArrayList<OsmXmlFeatureIterator>();

    private int                             size;

    private int                             limit = -1;


    public OsmXmlIterableFeatureCollection( String typeName, File file, List<TagFilter> filters )
            throws SchemaException, IOException {
        this( typeName, file.toURI().toURL(), filters );
    }


    public OsmXmlIterableFeatureCollection( String typeName, URL url, List<TagFilter> filters )
            throws SchemaException, IOException {
        super( schema( typeName, getKeys( filters ) ) );
        this.url = url;

        for (TagFilter filter : filters) {
            if (this.filters.put( filter.key(), filter.value() ) != null) {
                throw new RuntimeException( "TagFilter already exists: " + filter );
            }
        }
    }


    @Override
    protected Iterator<SimpleFeature> openIterator() {
        try {
            OsmXmlFeatureIterator osmFeatureIterator = new OsmXmlFeatureIterator( this, limit );
            osmFeatureIterators.add( osmFeatureIterator );
            return osmFeatureIterator;
        }
        catch (IOException e) {
            exception = e;
            return new ArrayList<SimpleFeature>().iterator();
        }
    }


    @Override
    public int size() {
        if (size == -1) {
            if (osmFeatureIterators.size() == 0) {
                openIterator();
            }
            if (osmFeatureIterators.size() > 0) {
                OsmXmlFeatureIterator osmFeatureIterator = osmFeatureIterators.get( 0 );
                size = osmFeatureIterator.size();
            }
        }
        return size;
    }


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
        for (OsmXmlFeatureIterator osmFeatureIterator : osmFeatureIterators) {
            osmFeatureIterator.complete();
        }
    }


    public Exception getException() {
        return exception;
    }


    public void setException( Exception e ) {
        this.exception = e;
    }


    public Map<String,String> getFilters() {
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
