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
package org.polymap.tutorial.osm.importer;

import java.text.NumberFormat;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Envelope;

import org.eclipse.swt.widgets.Composite;

import org.polymap.core.mapeditor.MapViewer;
import org.polymap.core.project.ILayer;
import org.polymap.core.runtime.Polymap;
import org.polymap.rhei.batik.BatikApplication;
import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;

import org.polymap.p4.P4Plugin;
import org.polymap.p4.data.importer.ImporterPrompt;
import org.polymap.p4.data.importer.ImporterPrompt.PromptUIBuilder;
import org.polymap.p4.data.importer.ImporterPrompt.Severity;
import org.polymap.p4.data.importer.ImporterSite;

/**
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 * @author Falko Bräutigam <falko@mapzone.io>
 */
public class BBOXPrompt {

    private static final Log log = LogFactory.getLog( BBOXPrompt.class );
    
    @Scope( P4Plugin.Scope )
    protected Context<MapViewer<ILayer>> mainMapViewer;
    
    private ImporterSite         site;

    private final ImporterPrompt prompt;


    /**
     * 
     * 
     * @param site
     * @param crs
     * @param severity {@link Severity#INFO} : collapsed importer on startup
     */
    public BBOXPrompt( ImporterSite site, Severity severity ) {
        this.site = site;
        BatikApplication.instance().getContext().propagate( this );

        log.info( " " + result() );

        prompt = site.newPrompt( "bboxFilter" )
                .summary.put( "Bounding box" )
                .description.put( "Bounding box" )
                .value.put( "current map extent" )
                .severity.put( severity )
                .ok.put( true )
                .extendedUI.put( new PromptUIBuilder() {
                    @Override
                    public void createContents( ImporterPrompt _prompt, Composite parent, IPanelToolkit tk ) {
                        NumberFormat nf = NumberFormat.getInstance( Polymap.getSessionLocale() );
                        nf.setMaximumFractionDigits( 5 );
                        ReferencedEnvelope bbox = result();
                        tk.createLabel( parent, nf.format( bbox.getMinX() ) + " : " + nf.format( bbox.getMaxX() ) + " -- "
                                + nf.format( bbox.getMinY() ) + " : " + nf.format( bbox.getMaxY() ) );
                    }
                    @Override
                    public void submit( ImporterPrompt _prompt ) {
                    }
                });
    }


//    protected String getBBOXStr( ReferencedEnvelope bbox ) {
//        if (bbox != null) {
//            List<Double> values = Arrays.asList( bbox.getMinY(), bbox.getMinX(), bbox.getMaxY(), bbox.getMaxX() );
//            return "(" + Joiner.on( "," ).join( values ) + ")";
//        }
//        return "";
//    }


    public ReferencedEnvelope result() {
        try {
            CoordinateReferenceSystem crs = mainMapViewer.get().maxExtent.get().getCoordinateReferenceSystem();
            Envelope extent = mainMapViewer.get().mapExtent.get();
            ReferencedEnvelope result = new ReferencedEnvelope( extent, crs );
            result = result.transform( DefaultGeographicCRS.WGS84, true );
            return result;
        }
        catch (MismatchedDimensionException | TransformException | FactoryException e) {
            throw new RuntimeException( e );
        }
    }


//    private static ReferencedEnvelope defaultBBOX( CoordinateReferenceSystem crs ) {
//        // FIXME Leipzig
//        double minLon = 12.263489;
//        double maxLon = 12.453003;
//        double minLat = 51.28597;
//        double maxLat = 51.419764;
//        try {
//            return new ReferencedEnvelope( minLon, maxLon, minLat, maxLat, crs );
//        }
//        catch (MismatchedDimensionException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
}
