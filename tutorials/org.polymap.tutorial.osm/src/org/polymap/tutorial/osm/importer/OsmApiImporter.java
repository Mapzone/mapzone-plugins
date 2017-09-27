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

import static org.polymap.rhei.batik.app.SvgImageRegistryHelper.NORMAL24;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rap.json.JsonArray;
import org.eclipse.rap.json.JsonObject;

import org.polymap.rhei.batik.toolkit.IPanelToolkit;

import org.polymap.p4.P4Plugin;
import org.polymap.p4.data.importer.ContextOut;
import org.polymap.p4.data.importer.Importer;
import org.polymap.p4.data.importer.ImporterSite;

/**
 * @author Joerg Reichert <joerg@mapzone.io>
 *
 */
public class OsmApiImporter
        implements Importer {

    private static int                        ELEMENT_PREVIEW_LIMIT = 100;

    private static int                        ELEMENT_IMPORT_LIMIT  = 50000;

    @ContextOut
    protected OsmXmlIterableFeatureCollection features;

    protected ImporterSite                    site;

    private Exception                         exception;

    private BBOXPrompt                        bboxPrompt;

    private TagFilterPrompt                   tagPrompt;

    private int                               totalCount            = -1;


    /*
     * (non-Javadoc)
     * 
     * @see org.polymap.p4.data.importer.Importer#site()
     */
    @Override
    public ImporterSite site() {
        return site;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.polymap.p4.data.importer.Importer#init(org.polymap.p4.data.importer.ImporterSite
     * , org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void init( ImporterSite aSite, IProgressMonitor monitor ) throws Exception {
        this.site = aSite;

        site.icon.set( P4Plugin.images().svgImage( "file-multiple.svg", NORMAL24 ) );
        site.summary.set( "OpenStreetMap" );
        site.description.set( "Importing OpenStreetMap data via API." );
        site.terminal.set( true );
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.polymap.p4.data.importer.Importer#createPrompts(org.eclipse.core.runtime
     * .IProgressMonitor)
     */
    @Override
    public void createPrompts( IProgressMonitor monitor ) throws Exception {
        // TODO get from CRS Prompt (not yet merged to master)
        CoordinateReferenceSystem crs = CRS.decode( "EPSG:4326" );
        bboxPrompt = new BBOXPrompt( site, crs );
        tagPrompt = new TagFilterPrompt( site );
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.polymap.p4.data.importer.Importer#verify(org.eclipse.core.runtime.
     * IProgressMonitor)
     */
    @Override
    public void verify( IProgressMonitor monitor ) {
        if (tagPrompt.isOk()) {
            try {
                String bboxStr = getBBOXStr( bboxPrompt.selection() );
                List<Pair<String,String>> tagFilters = tagPrompt.selection();
                String tagFilterStr = getTagFilterString( tagFilters );
                String filterStr = bboxStr.length() + tagFilterStr.length() > 0 ? "node" + tagFilterStr + bboxStr + ";"
                        : "";
                String baseURL = "http://www.overpass-api.de/api/interpreter?data=";
                // TODO make encoding configurable?
                URL countUrl = new URL( baseURL + URLEncoder.encode( "[out:json];" + filterStr + "out count;", "UTF-8" ) );
                String countJSONString = CharStreams.toString( new InputStreamReader( countUrl.openStream(), "UTF-8" ) );
                JsonObject root = JsonObject.readFrom( countJSONString );
                JsonArray elements = (JsonArray)root.get( "elements" );
                totalCount = Integer.valueOf( String.valueOf( ((JsonObject)((JsonObject)elements.get( 0 ))
                        .get( "count" )).get( "nodes" ) ) );
                if (totalCount > ELEMENT_IMPORT_LIMIT) {
                    throw new IndexOutOfBoundsException( "Your query results in more than " + ELEMENT_IMPORT_LIMIT
                            + " elements. Please select a smaller bounding box or refine your tag filters." );
                }
                int fetchCount = totalCount > ELEMENT_PREVIEW_LIMIT ? ELEMENT_PREVIEW_LIMIT : totalCount;
                // TODO make encoding configurable?
                URL url = new URL( baseURL + URLEncoder.encode( filterStr + "out " + fetchCount + ";", "UTF-8" ) );
                features = new OsmXmlIterableFeatureCollection( "org.polymap.tutorial.osm.importer", url, tagFilters );
                if (features.iterator().hasNext() && features.getException() == null) {
                    site.ok.set( true );
                }
                else {
                    exception = features.getException();
                    site.ok.set( false );
                }
            }
            catch (SchemaException | IOException | IndexOutOfBoundsException e) {
                site.ok.set( false );
                exception = e;
            }
        }
    }


    private String getTagFilterString( List<Pair<String,String>> filters ) throws UnsupportedEncodingException {
        List<String> formattedFilters = filters.stream().filter( filter -> !"*".equals( filter.getKey() ) )
                .map( filter -> {
                    String filterStr;
                    String keyStr;
                    if ("".equals( filter.getKey() )) {
                        keyStr = "~\"^$\"";
                    }
                        else {
                            keyStr = "\"" + filter.getKey() + "\"";
                        }
                        if ("*".equals( filter.getValue() )) {
                            filterStr = keyStr;
                        }
                        else if ("".equals( filter.getValue() )) {
                            filterStr = keyStr + "~\"^$\"";
                        }
                        else {
                            filterStr = keyStr + "=\"" + filter.getValue() + "\"";
                        }
                        return filterStr;
                    } ).collect( Collectors.toList() );

        if (filters.size() > 0 && !"*".equals( filters.get( 0 ).getKey() )) {
            return "[" + Joiner.on( "][" ).join( formattedFilters ) + "]";
        }
        else {
            return "";
        }
    }


    private String getBBOXStr( ReferencedEnvelope bbox ) throws UnsupportedEncodingException {
        List<Double> values = Arrays.asList( bbox.getMinY(), bbox.getMinX(), bbox.getMaxY(), bbox.getMaxX() );
        return "(" + Joiner.on( "," ).join( values ) + ")";
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.polymap.p4.data.importer.Importer#createResultViewer(org.eclipse.swt.widgets
     * .Composite, org.polymap.rhei.batik.toolkit.IPanelToolkit)
     */
    @Override
    public void createResultViewer( Composite parent, IPanelToolkit toolkit ) {
        if (tagPrompt.isOk()) {
            if (exception != null) {
                toolkit.createFlowText( parent,
                        "\nUnable to read the data.\n\n" + "**Reason**: " + exception.getMessage() );
            }
            else {
                SimpleFeatureType schema = (SimpleFeatureType)features.getSchema();
                OsmFeatureTableViewer table = new OsmFeatureTableViewer( parent, schema );
                if (totalCount > ELEMENT_PREVIEW_LIMIT) {
                    toolkit.createFlowText( parent, "\nShowing " + ELEMENT_PREVIEW_LIMIT + " items of totally found "
                            + totalCount + " elements." );
                    features.setLimit( ELEMENT_PREVIEW_LIMIT );
                }
                table.setContentProvider( new FeatureLazyContentProvider( features ) );
                table.setInput( features );
            }
        }
        else {
            toolkit.createFlowText( parent,
                    "\nOSM Importer is currently deactivated" );
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.polymap.p4.data.importer.Importer#execute(org.eclipse.core.runtime.
     * IProgressMonitor)
     */
    @Override
    public void execute( IProgressMonitor monitor ) throws Exception {
        // create all params for contextOut
        // all is done in verify
        if (totalCount > ELEMENT_IMPORT_LIMIT) {
            features.setLimit( ELEMENT_IMPORT_LIMIT );
        }
    }
}
