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
package org.polymap.tutorial.osm.importer.api;

import static org.polymap.rhei.batik.app.SvgImageRegistryHelper.NORMAL24;

import java.util.List;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Geometry;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;

import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.table.FeatureCollectionContentProvider;

import org.polymap.p4.P4Plugin;
import org.polymap.p4.data.importer.ContextOut;
import org.polymap.p4.data.importer.Importer;
import org.polymap.p4.data.importer.ImporterPrompt.Severity;
import org.polymap.p4.data.importer.ImporterSite;
import org.polymap.tutorial.osm.importer.BBOXPrompt;
import org.polymap.tutorial.osm.importer.OsmFeatureTableViewer;
import org.polymap.tutorial.osm.importer.TagFilter;
import org.polymap.tutorial.osm.importer.TagFilterPrompt;
import org.polymap.tutorial.osm.importer.api.Overpass.TagQuery;
import org.polymap.tutorial.osm.importer.taginfo.TagInfoAPI;

/**
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 * @author Falko Bräutigam <falko@mapzone.io>
 */
public class OsmApiImporter
        implements Importer {

    private static final Log log = LogFactory.getLog( Overpass.class );
    
    public static final int         ELEMENT_PREVIEW_LIMIT = 30;

    public static final int         ELEMENT_IMPORT_LIMIT  = 100000;

    @ContextOut
    protected FeatureCollection     features;

    protected ImporterSite          site;

    private Exception               exception;

    private BBOXPrompt              bboxPrompt;

    private TagFilterPrompt         tagPrompt;

    private int                     totalCount = -1;

    /** The query used to build the the result, build by {@link #verify(IProgressMonitor)}. */
    private TagQuery                query;

    private DefaultFeatureCollection preview;
    

    @Override
    public ImporterSite site() {
        return site;
    }

    
    @Override
    public void init( ImporterSite aSite, IProgressMonitor monitor ) throws Exception {
        this.site = aSite;

        site.icon.set( P4Plugin.images().svgImage( "file-multiple.svg", NORMAL24 ) );
        site.summary.set( "OpenStreetMap" );
        site.description.set( "OpenStreetMap data via Overpass API" );
        site.terminal.set( true );
    }


    @Override
    public void createPrompts( IProgressMonitor monitor ) throws Exception {
        bboxPrompt = new BBOXPrompt( site, Severity.INFO );
        tagPrompt = new TagFilterPrompt( site, Severity.INFO, new TagInfoAPI() );
    }


    @Override
    public void verify( IProgressMonitor monitor ) {
        if (!tagPrompt.isOk()) {
            return;
        }
        try {
            Class<? extends Geometry> geomType = tagPrompt.geomType;
            
            List<TagFilter> tagFilters = tagPrompt.filters;
            query = Overpass.instance().query()
                    .whereBBox( bboxPrompt.result() )
                    .whereTags( tagFilters )
                    .resultTypes( OverpassFeatureIterator.resultTypesFor( geomType ) );

            // check size
            totalCount = query.resultSize();
            if (totalCount > ELEMENT_IMPORT_LIMIT) {
                throw new IndexOutOfBoundsException( "Your query results in more than " + ELEMENT_IMPORT_LIMIT
                        + " elements. Please select a smaller bounding box or refine your tag filters." );
            }
            String schemaName = "osm-import-" + RandomStringUtils.randomNumeric( 4 );
            SimpleFeatureType schema = TagFilter.schemaOf( schemaName, tagFilters, geomType );

            features = new OverpassFeatureCollection( schema, query );

            // fetch and check
            monitor.beginTask( "Fetching data", ELEMENT_PREVIEW_LIMIT );
            query.maxResults( ELEMENT_PREVIEW_LIMIT );
            preview = new DefaultFeatureCollection( null, schema );
            features.accepts( feature -> {
                Geometry geom = (Geometry)feature.getDefaultGeometryProperty().getValue();
                if (geom == null || geom.isEmpty() || !geom.isValid()) {
                    throw new RuntimeException( "Feature has no/valid geometry.");
                }
                preview.add( (SimpleFeature)feature );
                monitor.worked( 1 );
            }, null );

            query.maxResults( ELEMENT_IMPORT_LIMIT );
            site.ok.set( true );
        }
        catch (Exception e) {
            log.warn( "", e );
            site.ok.set( false );
            exception = e;
        }
    }


    @Override
    public void createResultViewer( Composite parent, IPanelToolkit tk ) {
        if (!tagPrompt.isOk()) {
//          toolkit.createFlowText( parent,
//          "\nOSM Importer is currently deactivated" );
            return;
        }
        else if (exception != null) {
            parent.setLayout( new FillLayout() );
            tk.createFlowText( parent,
                    "\nUnable to read the data.\n\n" + "**Reason**: " + exception.getMessage() );
        }
        else {
            parent.setLayout( FormLayoutFactory.defaults().create() );
            Label l = tk.createLabel( parent, "Previewing " + preview.size() + " out of " + totalCount + " elements" );
            FormDataFactory.on( l ).fill().noBottom().control();

            OsmFeatureTableViewer table = new OsmFeatureTableViewer( parent, preview.getSchema() );
            table.setContentProvider( new FeatureCollectionContentProvider() );
            table.setInput( preview );
            FormDataFactory.on( table.getControl() ).fill().top( l );
        }
    }


    @Override
    public void execute( IProgressMonitor monitor ) throws Exception {
        // #features still uses this query; 
        // it does not cache, the next request will use this limit
        query.maxResults( ELEMENT_IMPORT_LIMIT );
    }
}
