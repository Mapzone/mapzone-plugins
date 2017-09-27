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

import java.util.Arrays;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Joiner;

import org.polymap.p4.data.importer.ImporterPrompt;
import org.polymap.p4.data.importer.ImporterPrompt.Severity;
import org.polymap.p4.data.importer.ImporterSite;

/**
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 * @author Falko Bräutigam <falko@mapzone.io>
 */
public class BBOXPrompt {

    private ImporterSite         site;

    private ReferencedEnvelope   selection;

    private final ImporterPrompt prompt;


    public BBOXPrompt( ImporterSite site, CoordinateReferenceSystem crs ) {
        this.site = site;

        selection = getDefaultBBOX( crs );

        prompt = site.newPrompt( "bboxFilter" )
                .summary.put( "BBOX selector" )
                .description.put( "Narrow down feature selection by bounding box" )
                .value.put( getBBOXStr( selection ) )
                .severity.put( Severity.VERIFY )
                .ok.put( false )
                .extendedUI.put( new BBOXPromptUIBuilder() {

                    private ReferencedEnvelope bbox = null;

                    @Override
                    protected ReferencedEnvelope getBBOX() {
                        if (bbox == null) {
                            bbox = BBOXPrompt.this.selection;
                        }
                        return bbox;
                    }

                    @Override
                    protected void setBBOX( ReferencedEnvelope bbox ) {
                        this.bbox = bbox;
                    }

                    @Override
                    protected String getBBOXStr() {
                        return BBOXPrompt.this.getBBOXStr( this.bbox );
                    }

                    @Override
                    protected String getCRS() {
                        return "EPSG:4326";
                    }

                    @Override
                    public void submit( ImporterPrompt ip ) {
                        BBOXPrompt.this.selection = bbox;
                        ip.ok.set( true );
                    }
                } );
    }


    private String getBBOXStr( ReferencedEnvelope bbox ) {
        if (bbox != null) {
            List<Double> values = Arrays.asList( bbox.getMinY(), bbox.getMinX(), bbox.getMaxY(), bbox.getMaxX() );
            return "(" + Joiner.on( "," ).join( values ) + ")";
        }
        return "";
    }


    public ReferencedEnvelope selection() {
        return selection;
    }


    private static ReferencedEnvelope getDefaultBBOX( CoordinateReferenceSystem crs ) {
        // FIXME Leipzig
        double minLon = 12.263489;
        double maxLon = 12.453003;
        double minLat = 51.28597;
        double maxLat = 51.419764;
        try {
            return new ReferencedEnvelope( minLon, maxLon, minLat, maxLat, crs );
        }
        catch (MismatchedDimensionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
