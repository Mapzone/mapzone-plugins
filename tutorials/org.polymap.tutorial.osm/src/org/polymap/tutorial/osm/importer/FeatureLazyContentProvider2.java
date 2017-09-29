/*
 * polymap.org Copyright 2011, Falko Br�utigam, and other contributors as indicated
 * by the @authors tag. All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.tutorial.osm.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.polymap.rhei.table.FeatureTableViewer;
import org.polymap.rhei.table.IFeatureTableElement;

/**
 *
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class FeatureLazyContentProvider2
        implements ILazyContentProvider {

    private static Log                log             = LogFactory.getLog( FeatureLazyContentProvider2.class );

    private FeatureCollection         coll;

    private FeatureIterator           featureIterator = null;

    private List<FeatureTableElement> cached          = null;

    private FeatureTableViewer        viewer          = null;


    public FeatureLazyContentProvider2( FeatureTableViewer viewer, FeatureCollection delegate ) {
        this.viewer = viewer;
        this.coll = delegate;
    }


    /**
     *
     */
    public class FeatureTableElement
            implements IFeatureTableElement {

        private Feature feature;


        protected FeatureTableElement( Feature feature ) {
            this.feature = feature;
        }


        public Feature getFeature() {
            return feature;
        }


        public Object getValue( String name ) {
            return feature.getProperty( name ).getValue();
        }


        public void setValue( String name, Object value ) {
            feature.getProperty( name ).setValue( value );
        }


        public String fid() {
            return feature.getIdentifier().getID();
        }


        @Override
        public <T> Optional<T> unwrap( Class<T> targetClass ) {
            // XXX Auto-generated method stub
            throw new RuntimeException( "not yet implemented." );
        }

    }


    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.viewers.ILazyContentProvider#updateElement(int)
     */
    @Override
    public void updateElement( int index ) {
        if (featureIterator == null) {
            featureIterator = coll.features();
        }
        if (cached == null) {
            cached = new ArrayList<FeatureTableElement>();
        }
        if (index < cached.size()) {
            viewer.update( cached.get( index ), null );
        }
        else {
            int min = cached.size() == 0 ? 0 : cached.size() - 1;
            FeatureTableElement fte;
            for (int i = min; i < index && featureIterator.hasNext(); i++) {
                fte = new FeatureTableElement( featureIterator.next() );
                cached.add( fte );
                viewer.update( fte, null );
            }
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    @Override
    public void dispose() {
        cached = null;
        if(featureIterator != null) {
            featureIterator.close();
        }
        featureIterator = null;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.
     * viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    @Override
    public void inputChanged( @SuppressWarnings( "hiding" ) Viewer viewer, Object oldInput, Object newInput ) {
        this.viewer = (FeatureTableViewer)viewer;
        if(featureIterator != null) {
            featureIterator.close();
        }
        cached = null;
        featureIterator = null;
    }
}
