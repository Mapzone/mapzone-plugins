/*
 * polymap.org
 * Copyright 2011, Falko Br�utigam, and other contributors as
 * indicated by the @authors tag. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.tutorial.osm.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.io.IOException;

import org.geotools.feature.FeatureCollection;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.jface.viewers.Viewer;

import org.polymap.rhei.table.IFeatureContentProvider;
import org.polymap.rhei.table.IFeatureTableElement;

/**
 *
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class FeatureLazyContentProvider
        implements IFeatureContentProvider {

    private static Log log = LogFactory.getLog( FeatureLazyContentProvider.class );

    private FeatureCollection           coll;


    public FeatureLazyContentProvider( FeatureCollection delegate ) {
        this.coll = delegate;
    }


    @Override
    public Object[] getElements( Object input ) {
        try {
            final List<Object> result = new ArrayList<Object>();
            coll.accepts( new FeatureVisitor() {
                public void visit( Feature feature ) {
                    result.add(new FeatureTableElement( feature ));
                }
            }, new NullProgressListener() );
            return result.toArray();
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }


    @Override
    public void dispose() {
    }


    @Override
    public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
    }

    /**
     *
     */
    public class FeatureTableElement
            implements IFeatureTableElement {

        private Feature         feature;


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

}
