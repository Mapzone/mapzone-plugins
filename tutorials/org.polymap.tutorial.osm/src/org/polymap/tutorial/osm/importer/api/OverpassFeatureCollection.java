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

import java.util.Iterator;

import java.io.IOException;
import java.net.URL;

import org.geotools.feature.collection.AdaptorFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.tutorial.osm.importer.xml.OsmXmlFeatureIterator;

/**
 * Provides the resultig features of an {@link Overpass#query()}.
 *
 * @author Falko Bräutigam
 */
public class OverpassFeatureCollection
        extends AdaptorFeatureCollection {

    private static final Log log = LogFactory.getLog( OverpassFeatureCollection.class );
    
    private Overpass.Query      query;
    
    
    public OverpassFeatureCollection( SimpleFeatureType schema, Overpass.Query query ) {
        super( "OverpassFeatureCollection", schema );
//        Class<?> geomType = schema.getGeometryDescriptor().getType().getBinding();
//        query.resultTypes( OverpassFeatureIterator.resultTypesFor( (Class<? extends Geometry>)geomType ) );
        this.query = query;
    }
    
    
    @Override
    public int size() {
        try {
            return query.resultSize();
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }


    @Override 
    protected Iterator<SimpleFeature> openIterator() {
        try {
            URL url = query.downloadUrl();
            return new OverpassFeatureIterator( schema, url );
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }
    
    
    @Override 
    protected void closeIterator( Iterator<SimpleFeature> it ) {
        ((OsmXmlFeatureIterator)it).close();
    }

}
