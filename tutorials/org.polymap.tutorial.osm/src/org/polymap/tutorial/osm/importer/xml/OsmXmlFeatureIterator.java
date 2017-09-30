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
package org.polymap.tutorial.osm.importer.xml;

import java.util.Iterator;
import java.util.Map;

import java.io.IOException;
import java.io.InputStream;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;

/**
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 */
class OsmXmlFeatureIterator
        implements Iterator<SimpleFeature> {

    private final OsmXmlIterableFeatureCollection fc;

    private final SimpleFeatureBuilder            featureBuilder;

    private final InputStream                     input;

    private final OsmXmlIterator                  iterator;

    private final int                             limit;

    private int                                   index;

    private OsmNode                               currentNode;

    private int                                   size = -1;


    public OsmXmlFeatureIterator( OsmXmlIterableFeatureCollection fc, int limit )
            throws IOException {
        this.fc = fc;
        this.limit = limit;
        featureBuilder = new SimpleFeatureBuilder( fc.getSchema() );
        input = this.fc.getUrl().openStream();
        iterator = new OsmXmlIterator( input, false );
    }


    @Override
    public boolean hasNext() {
        return hasNext( false );
    }


    private boolean hasNext( boolean countMode ) {
        if (limit >= 0 && index > limit) {
            return false;
        }
        if (!countMode && currentNode != null) {
            return true;
        }
        EntityContainer container;
        OsmNode node;
        while (iterator.hasNext()) {
            container = iterator.next();
            if (container.getType() == EntityType.Node) {
                node = (OsmNode)container.getEntity();
                Map<String,String> tags = OsmModelUtil.getTagsAsMap( node );
                boolean matches = fc.getFilters().size() > 0;
                for (Map.Entry<String,String> filter : fc.getFilters().entrySet()) {
                    if (!(filter.getKey() == "*"
                            || (tags.containsKey( filter.getKey() ) && (filter.getValue() == "*") 
                            || (filter.getValue() != null && filter.getValue().equals( (tags.get( filter.getKey() )) ))))) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    if (!countMode) {
                        currentNode = node;
                        index++;
                    }
                    return true;
                }
            }
        }
        complete();
        return false;
    }


    @Override
    public SimpleFeature next() {
        double longitude = currentNode.getLongitude();
        double latitude = currentNode.getLatitude();
        GeometryFactory gf = new GeometryFactory();
        Point point = gf.createPoint( new Coordinate( longitude, latitude ) );
        featureBuilder.add( point );
        Map<String,String> attributes = OsmModelUtil.getTagsAsMap( currentNode );
        fc.updateBBOX( longitude, latitude );
        for (String key : fc.getFilters().keySet()) {
            featureBuilder.add( attributes.get( key ) );
        }
        currentNode = null;
        if (!hasNext()) {
            complete();
        }
        return featureBuilder.buildFeature( null );
    }


    public void complete() {
        iterator.complete();
        try {
            input.close();
        }
        catch (IOException e) {
            this.fc.setException( e );
        }
    }


    public int size() {
        if (size == -1) {
            try {
                // by this a new input stream is created for the URL
                // trade-off: two (API/file) requests (with stream same content)
                // (this is the current way) vs.
                // one (API/file) request and then storing node objects while
                // counting an reusing them when building feature
                OsmXmlFeatureIterator osmFeatureIterator = new OsmXmlFeatureIterator( this.fc,
                        -1 );
                int count = 0;
                while (osmFeatureIterator.hasNext( true )) {
                    count++;
                }
                size = count;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return size;
    }
}