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
package org.polymap.tutorial.osm.importer.xml;

import static org.polymap.rhei.batik.app.SvgImageRegistryHelper.NORMAL24;

import java.util.Iterator;
import java.util.List;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.collection.AdaptorFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Iterators;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;

import org.polymap.rhei.batik.toolkit.IPanelToolkit;

import org.polymap.p4.P4Plugin;
import org.polymap.p4.data.importer.ContextIn;
import org.polymap.p4.data.importer.ContextOut;
import org.polymap.p4.data.importer.Importer;
import org.polymap.p4.data.importer.ImporterPrompt.Severity;
import org.polymap.p4.data.importer.ImporterSite;
import org.polymap.tutorial.osm.importer.FeatureLazyContentProvider;
import org.polymap.tutorial.osm.importer.OsmFeatureTableViewer;
import org.polymap.tutorial.osm.importer.TagFilter;
import org.polymap.tutorial.osm.importer.TagFilterPrompt;
import org.polymap.tutorial.osm.importer.taginfo.TagInfoAPI;

/**
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 * @author Falko Bräutigam <falko@mapzone.io>
 */
public class OsmXmlFileImporter
        implements Importer {

    private static final Log log = LogFactory.getLog( OsmXmlFileImporter.class );

    private static final int                  ELEMENT_PREVIEW_LIMIT = 100;

    private static final int                  ELEMENT_IMPORT_LIMIT  = 50000;

    @ContextIn
    protected File                            file;

    @ContextOut
    protected FeatureCollection               features;

    protected ImporterSite                    site;

    private Exception                         exception;

    private TagFilterPrompt                   tagPrompt;

    private int                               totalCount = -1;


    @Override
    public ImporterSite site() {
        return site;
    }


    @Override
    public void init( ImporterSite aSite, IProgressMonitor monitor ) throws Exception {
        this.site = aSite;

        site.icon.set( P4Plugin.images().svgImage( "file-multiple.svg", NORMAL24 ) );
        site.summary.set( "OSM-Import" );
        site.description.set( "Importing an OSM XML file." );
        site.terminal.set( true );
    }


    @Override
    public void createPrompts( IProgressMonitor monitor ) throws Exception {
        log.warn( "TagInfoXML is not yet ready -> using TagInfoAPI" );  // XXX
        tagPrompt = new TagFilterPrompt( site, Severity.REQUIRED, new TagInfoAPI() );
    }


    @Override
    public void verify( IProgressMonitor monitor ) {
        try {
            List<TagFilter> tagFilters = tagPrompt.filters;
            String schemaName = "osm-import-" + RandomStringUtils.randomNumeric( 4 );
            SimpleFeatureType schema = TagFilter.schemaOf( schemaName, tagFilters, tagPrompt.geomType );
            features = new OsmXmlFeatureCollection( schemaName, schema, tagFilters );
            
            totalCount = features.size();
            if (totalCount > ELEMENT_IMPORT_LIMIT) {
                throw new IndexOutOfBoundsException( "Your query results in more than " + ELEMENT_IMPORT_LIMIT
                        + " elements. Please provide a smaller OSM extract or refine your tag filters." );
            }
            ((OsmXmlFeatureCollection)features).maxResults = ELEMENT_PREVIEW_LIMIT;
        }
        catch (Exception e) {
            log.warn( "", e );
            site.ok.set( false );
            exception = e;
        }
    }


    @Override
    public void createResultViewer( Composite parent, IPanelToolkit tk ) {
        if (tagPrompt.isOk()) {
            if (exception != null) {
                parent.setLayout( new FillLayout() );
                tk.createFlowText( parent,
                        "\nUnable to read the data.\n\n" + "**Reason**: " + exception.getMessage() );
            }
            else {
                parent.setLayout( FormLayoutFactory.defaults().create() );
                Label l = tk.createLabel( parent, "Previewing " + features.size() + " out of " + totalCount + " elements" );
                FormDataFactory.on( l ).fill().noBottom().control();
                
                OsmFeatureTableViewer table = new OsmFeatureTableViewer( parent, (SimpleFeatureType)features.getSchema() );
                table.setContentProvider( new FeatureLazyContentProvider( features ) );
                table.setInput( features );
                FormDataFactory.on( table.getControl() ).fill().top( l );
            }
        }
//        else {
//            toolkit.createFlowText( parent,
//                    "\nOSM Importer is currently deactivated" );
//        }
    }


    @Override
    public void execute( IProgressMonitor monitor ) throws Exception {
        ((OsmXmlFeatureCollection)features).maxResults = ELEMENT_IMPORT_LIMIT;
    }


    /**
     * 
     */
    protected class OsmXmlFeatureCollection
            extends AdaptorFeatureCollection {

        public int              maxResults = Integer.MAX_VALUE;
        
        public List<TagFilter>  tagFilters;
        
        protected OsmXmlFeatureCollection( String id, SimpleFeatureType schema, List<TagFilter> tagFilters ) {
            super( id, schema );
            this.tagFilters = tagFilters;
        }
    
        @Override
        public int size() {
            Iterator<SimpleFeature> it = openIterator();
            try {
                return Iterators.size( it );
            } 
            finally {
                closeIterator( it );
            }
        }
    
        @Override
        protected Iterator<SimpleFeature> openIterator() {
            try {
                InputStream in = new BufferedInputStream( new FileInputStream( file ) );
                Iterator<SimpleFeature> result = new OsmXmlFeatureIterator( schema, in );
                
                result = Iterators.filter( result, feature -> 
                        tagFilters.stream().anyMatch( filter -> filter.matches( feature ) ) );
                result = Iterators.limit( result, maxResults );
                return result;
            }
            catch (Exception e) {
                throw new RuntimeException( e );
            }
        }
    
        @Override
        protected void closeIterator( Iterator<SimpleFeature> close ) {
            // FIXME
            log.warn( "OsmXmlFeatureCollection: iterator is NOT PROPERLY CLOSED!" );
            //((OsmXmlFeatureIterator)close).close();
        }
    }
    
}
