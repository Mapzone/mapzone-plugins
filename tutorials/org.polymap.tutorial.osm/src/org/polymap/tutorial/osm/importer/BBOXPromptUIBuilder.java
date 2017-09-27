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

import java.math.BigDecimal;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.polymap.rhei.batik.toolkit.IPanelToolkit;

import org.polymap.p4.data.importer.ImporterPrompt;
import org.polymap.p4.data.importer.ImporterPrompt.PromptUIBuilder;
import org.polymap.rap.openlayers.base.OlMap;
import org.polymap.rap.openlayers.control.ZoomControl;
import org.polymap.rap.openlayers.layer.TileLayer;
import org.polymap.rap.openlayers.source.MapQuestSource;
import org.polymap.rap.openlayers.types.Coordinate;
import org.polymap.rap.openlayers.types.Projection;
import org.polymap.rap.openlayers.types.Projection.Units;
import org.polymap.rap.openlayers.view.View;

/**
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 * @author Falko Bräutigam <falko@mapzone.io>
 */
public abstract class BBOXPromptUIBuilder
        implements PromptUIBuilder {

    private static final double EQUATOR_LENGTH = 40075016.68557849;

    private Label         label;


    @Override
    public void createContents( ImporterPrompt prompt, Composite parent, IPanelToolkit tk ) {
        parent.getShell().setSize( 500, 500 );
        parent.setLayout( new GridLayout() );
        label = new Label( parent, SWT.NONE );
        label.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        updateLabel();
        OlMap map = new OlMap(
                parent,
                SWT.MULTI | SWT.WRAP | SWT.BORDER,
                new View().projection.put( new Projection( "EPSG:3857", Units.m ) ).center.put( getCenter() ).resolution
                        .put( getResolution() ) );
        map.addLayer( new TileLayer().source.put( new MapQuestSource( MapQuestSource.Type.osm ) ) );
        map.view.get().addPropertyChangeListener( ev -> {
            JSONObject jsonObject = ev.properties();
            JSONArray centerCoord = (JSONArray)jsonObject.get( "center" );
            Double x = Double.valueOf( String.valueOf( centerCoord.get( 0 ) ) );
            Double y = Double.valueOf( String.valueOf( centerCoord.get( 1 ) ) );
            Float resolution = Float.valueOf( String.valueOf( jsonObject.get( "resolution" ) ) );
            updateBBOX( x, y, resolution );
            updateLabel();
        } );
        map.addControl( new ZoomControl() );
        GridData data = new GridData();
        data.widthHint = 400;
        data.heightHint = 400;
        map.getControl().setLayoutData( data );
    }


    private void updateLabel() {
        label.getDisplay().asyncExec( () -> label.setText( "BBOX: " + getBBOXStr() ) );
    }


    private float getResolution() {
        ReferencedEnvelope bbox = getBBOX();
        com.vividsolutions.jts.geom.Coordinate minCoord = convertFromDegreeToMeter( bbox.getMinX(), bbox.getMinY() );
        com.vividsolutions.jts.geom.Coordinate maxCoord = convertFromDegreeToMeter( bbox.getMaxX(), bbox.getMaxY() );

        double xDiff = Math.abs( maxCoord.x - minCoord.x );
        double yDiff = Math.abs( maxCoord.y - minCoord.y );

        return Double.valueOf( xDiff > yDiff ? xDiff / 256 : yDiff / 256 ).floatValue();
    }


    private void updateBBOX( double x, double y, float resolution ) {
        float halfExtend = resolution * 128;
        com.vividsolutions.jts.geom.Coordinate min = convertFromMeterToDegree( x - halfExtend, y - halfExtend );
        com.vividsolutions.jts.geom.Coordinate max = convertFromMeterToDegree( x + halfExtend, y + halfExtend );
        try {
            ReferencedEnvelope bbox = new ReferencedEnvelope( roundToFive( min.x ), roundToFive( max.x ),
                    roundToFive( min.y ), roundToFive( max.y ), CRS.decode( getCRS() ) );
            setBBOX( bbox );
        }
        catch (MismatchedDimensionException | FactoryException e) {
            e.printStackTrace();
        }
    }


    private double roundToFive( double original ) {
        return BigDecimal.valueOf( original ).setScale( 5, BigDecimal.ROUND_HALF_UP ).doubleValue();
    }


    protected abstract void setBBOX( ReferencedEnvelope bbox );


    private Coordinate getCenter() {
        ReferencedEnvelope bbox = getBBOX();
        com.vividsolutions.jts.geom.Coordinate minCoord = convertFromDegreeToMeter( bbox.getMinX(), bbox.getMinY() );
        com.vividsolutions.jts.geom.Coordinate maxCoord = convertFromDegreeToMeter( bbox.getMaxX(), bbox.getMaxY() );

        double xDiff = Math.abs( maxCoord.x - minCoord.x );
        double yDiff = Math.abs( maxCoord.y - minCoord.y );
        double x = minCoord.x + (xDiff / 2);
        double y = minCoord.y + (yDiff / 2);
        return new Coordinate( x, y );
    }


    private com.vividsolutions.jts.geom.Coordinate convertFromDegreeToMeter( double x, double y ) {
        CoordinateReferenceSystem srcCRS = DefaultGeographicCRS.WGS84;
        try {
            CoordinateReferenceSystem destCRS = CRS.decode( "EPSG:3857" );
            return convertCoordinate( x, y, srcCRS, destCRS );
        }
        catch (FactoryException e) {
            e.printStackTrace();
        }
        return new com.vividsolutions.jts.geom.Coordinate( x, y );
    }


    private com.vividsolutions.jts.geom.Coordinate convertFromMeterToDegree( double x, double y ) {
        try {
            CoordinateReferenceSystem srcCRS = CRS.decode( "EPSG:3857" );
            CoordinateReferenceSystem destCRS = DefaultGeographicCRS.WGS84;
            return convertCoordinate( x, y, srcCRS, destCRS );
        }
        catch (FactoryException e) {
            e.printStackTrace();
        }
        return new com.vividsolutions.jts.geom.Coordinate( x, y );
    }


    private com.vividsolutions.jts.geom.Coordinate convertCoordinate( double x, double y,
            CoordinateReferenceSystem srcCRS, CoordinateReferenceSystem destCRS ) {
        try {
            boolean lenient = true;
            MathTransform transform = CRS.findMathTransform( srcCRS, destCRS, lenient );
            return JTS.transform(
                    new com.vividsolutions.jts.geom.Coordinate( x, y ), null, transform );
        }
        catch (FactoryException | TransformException e) {
            e.printStackTrace();
        }
        return new com.vividsolutions.jts.geom.Coordinate( x, y );
    }


    protected abstract ReferencedEnvelope getBBOX();


    protected abstract String getBBOXStr();


    protected abstract String getCRS();
}
