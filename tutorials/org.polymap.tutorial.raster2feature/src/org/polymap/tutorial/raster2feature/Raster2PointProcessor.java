/* 
 * polymap.org
 * Copyright (C) 2018, the @authors. All rights reserved.
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
package org.polymap.tutorial.raster2feature;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import java.awt.geom.AffineTransform;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.spatial.BBOX;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import org.polymap.core.data.feature.AddFeaturesRequest;
import org.polymap.core.data.feature.FeaturesProducer;
import org.polymap.core.data.feature.GetBoundsRequest;
import org.polymap.core.data.feature.GetFeatureTypeRequest;
import org.polymap.core.data.feature.GetFeatureTypeResponse;
import org.polymap.core.data.feature.GetFeaturesRequest;
import org.polymap.core.data.feature.GetFeaturesResponse;
import org.polymap.core.data.feature.GetFeaturesSizeRequest;
import org.polymap.core.data.feature.GetFeaturesSizeResponse;
import org.polymap.core.data.feature.ModifyFeaturesRequest;
import org.polymap.core.data.feature.RemoveFeaturesRequest;
import org.polymap.core.data.feature.TransactionRequest;
import org.polymap.core.data.pipeline.DataSourceDescriptor;
import org.polymap.core.data.pipeline.PipelineExecutor.ProcessorContext;

import org.polymap.p4.project.ProjectRepository;

import org.polymap.core.data.pipeline.PipelineProcessorSite;
import org.polymap.core.data.pipeline.ProcessorResponse;
import org.polymap.core.data.pipeline.TerminalPipelineProcessor;
import org.polymap.core.project.IMap;

import org.polymap.model2.runtime.UnitOfWork;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class Raster2PointProcessor
        implements TerminalPipelineProcessor, FeaturesProducer {

    private static final Log log = LogFactory.getLog( Raster2PointProcessor.class );

    public static final double          RADTODEG = 360.0 / (2.0 * PI);

    public static final int             TILE_SIZE = 256;
    /** Breakpoint per rendered tile. */
    public static final int             BREAKPOINTS = 10;
    /** The name of the feature attribute. */
    public static final String          ASPECT = "aspect";
    /** The name of the feature attribute. */
    public static final String          SAMPLE = "sample";
    
    private GridCoverage2DReader        reader;

    private String                      coverageName;

    private SimpleFeatureType           schema;
    
    
    @Override
    public boolean isCompatible( DataSourceDescriptor dsd ) {
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public void init( PipelineProcessorSite site ) throws Exception {
        reader = (GridCoverage2DReader)site.dsd.get().service.get();
        coverageName = site.dsd.get().resourceName.get();
        
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setCRS( reader.getCoordinateReferenceSystem() );
        ftb.setName( "Raster2Point" );
        ftb.add( "geom", Point.class, reader.getCoordinateReferenceSystem() );
        ftb.add( ASPECT, Integer.class );
        ftb.add( SAMPLE, Double.class );
        schema = ftb.buildFeatureType();
    }


    @Override
    public void getFeatureTypeRequest( GetFeatureTypeRequest request, ProcessorContext context ) throws Exception {
        context.sendResponse( new GetFeatureTypeResponse( schema ) );
        context.sendResponse( ProcessorResponse.EOP );
    }


    protected GeneralParameterValue generalParameter( BoundingBox bounds ) {
        Parameter<GridGeometry2D> param = new Parameter<GridGeometry2D>( AbstractGridFormat.READ_GRIDGEOMETRY2D );
        GridEnvelope2D gridRange = new GridEnvelope2D( 0, 0, TILE_SIZE, TILE_SIZE );
        Envelope userRange = new ReferencedEnvelope( bounds ); //, schema.getCoordinateReferenceSystem() );
        param.setValue( new GridGeometry2D( gridRange, userRange ) );
        return param;
    }

    
    @Override
    public void getFeatureRequest( GetFeaturesRequest request, ProcessorContext context ) throws Exception {
        int breakpoints = BREAKPOINTS;
        BoundingBox bounds = null;
        
        DefaultFilterVisitor bboxFinder = new DefaultFilterVisitor() {
            @Override public Object visit( BBOX bbox, Object data ) {
                ((AtomicReference)data).set( bbox );
                return data;
            }
        };
        AtomicReference<BBOX> queriedBBox = (AtomicReference)request.getQuery().getFilter().accept( bboxFinder, new AtomicReference() );
        if (queriedBBox.get() != null) {
            bounds = queriedBBox.get().getBounds();
        }
        else {
            // XXX a bit hacky
            try (UnitOfWork uow = ProjectRepository.newUnitOfWork()) {
                IMap map = uow.entity( IMap.class, ProjectRepository.ROOT_MAP_ID );
                bounds = map.maxExtent();
                breakpoints = 30;
            }
        }
        
        GeneralParameterValue param = generalParameter( bounds );
        GridCoverage2D raster = reader.read( coverageName, new GeneralParameterValue[] {param} );
        AffineTransform gridToCRS = (AffineTransform) raster.getGridGeometry().getGridToCRS();
        double xRes = XAffineTransform.getScaleX0( gridToCRS );
        double yRes = XAffineTransform.getScaleY0( gridToCRS );
        GeometryFactory gf = new GeometryFactory();
        
        List<Feature> features = new ArrayList( breakpoints*breakpoints );
        int gridSize = (breakpoints * 2) + 1;
        SimpleFeature[][] grid = new SimpleFeature[ gridSize ][ gridSize ];
        
        // read samples and generate Features
        double[] sample = new double[1];
        for (int x=0; x < gridSize; x++) {
            for (int y=0; y < gridSize; y++) {
                double lon = bounds.getWidth() / gridSize * x + bounds.getMinX();
                double lat = bounds.getHeight() / gridSize * y + bounds.getMinY();
                try {
                    raster.evaluate( (DirectPosition)new DirectPosition2D( bounds.getCoordinateReferenceSystem(), lon, lat ), sample );
                }
                catch (PointOutsideCoverageException e) {
                    sample[0] = -1;
                }

                SimpleFeatureBuilder fb = new SimpleFeatureBuilder( schema );
                fb.set( SAMPLE, Math.max( 0, sample[0] ) );  // novalue
                fb.set( "geom", gf.createPoint( new Coordinate( lon, lat ) ) );
                SimpleFeature feature = fb.buildFeature( null );
                grid[x][y] = feature;
            }
        }
        // calculate aspect
        for (int x=1; x<gridSize; x+=2) {
            for (int y=1; y<gridSize; y+=2) {
                int aspect = calculateAspect( grid, x, y, xRes, yRes );
                SimpleFeature feature = grid[x][y];
                feature.setAttribute( "aspect", aspect );
                features.add( feature );
            }
        }
        
        context.sendResponse( new GetFeaturesResponse( features ) );
        context.sendResponse( ProcessorResponse.EOP );
    }

    
    /**
     * Calculates the aspect in a given {@link GridNode}.
     * <p/>
     * This code is from OsmAspect, part of JGrasstools (http://www.jgrasstools.org)
     * (C) HydroloGIS - www.hydrologis.com
     * 
     * @param node the current grid node.
     * @param radtodeg radiants to degrees conversion factor. Use
     *        {@link NumericsUtilities#RADTODEG} if you want degrees, use 1 if you
     *        want radiants.
     * @param doRound if <code>true</code>, values are round to integer.
     * @return the value of aspect.
     */
    protected int calculateAspect( SimpleFeature[][] grid, int x, int y, double xRes, double yRes ) {
        double radtodeg = RADTODEG;
        double doubleNovalue = Double.NaN;
        
        double aspect = doubleNovalue;
        // the value of the x and y derivative
        double aData = 0.0;
        double bData = 0.0;
        double centralValue = (Double)grid[x][y].getAttribute( SAMPLE );
        double nValue = (Double)grid[x][y+1].getAttribute( SAMPLE );
        double sValue = (Double)grid[x][y-1].getAttribute( SAMPLE );
        double wValue = (Double)grid[x-1][y].getAttribute( SAMPLE );
        double eValue = (Double)grid[x+1][y].getAttribute( SAMPLE );

        if (!isNovalue( centralValue )) {
            boolean sIsNovalue = isNovalue( sValue );
            boolean nIsNovalue = isNovalue( nValue );
            boolean wIsNovalue = isNovalue( wValue );
            boolean eIsNovalue = isNovalue( eValue );

            if (!sIsNovalue && !nIsNovalue) {
                aData = atan( (nValue - sValue) / (2 * yRes) );
            }
            else if (nIsNovalue && !sIsNovalue) {
                aData = atan( (centralValue - sValue) / (yRes) );
            }
            else if (!nIsNovalue && sIsNovalue) {
                aData = atan( (nValue - centralValue) / (yRes) );
            }
            else if (nIsNovalue && sIsNovalue) {
                aData = doubleNovalue;
            }
            else {
                // can't happen
                throw new RuntimeException();
            }
            if (!wIsNovalue && !eIsNovalue) {
                bData = atan( (wValue - eValue) / (2 * xRes) );
            }
            else if (wIsNovalue && !eIsNovalue) {
                bData = atan( (centralValue - eValue) / (xRes) );
            }
            else if (!wIsNovalue && eIsNovalue) {
                bData = atan( (wValue - centralValue) / (xRes) );
            }
            else if (wIsNovalue && eIsNovalue) {
                bData = doubleNovalue;
            }
            else {
                // can't happen
                throw new RuntimeException();
            }

            double delta = 0.0;
            // calculate the aspect value
            if (aData < 0 && bData > 0) {
                delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                aspect = delta * radtodeg;
            } else if (aData > 0 && bData > 0) {
                delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                aspect = (PI - delta) * radtodeg;
            } else if (aData > 0 && bData < 0) {
                delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                aspect = (PI + delta) * radtodeg;
            } else if (aData < 0 && bData < 0) {
                delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                aspect = (2 * PI - delta) * radtodeg;
            } else if (aData == 0 && bData > 0) {
                aspect = (PI / 2.) * radtodeg;
            } else if (aData == 0 && bData < 0) {
                aspect = (PI * 3. / 2.) * radtodeg;
            } else if (aData > 0 && bData == 0) {
                aspect = PI * radtodeg;
            } else if (aData < 0 && bData == 0) {
                aspect = 2.0 * PI * radtodeg;
            } else if (aData == 0 && bData == 0) {
                aspect = 0.0;
            } else if (isNovalue(aData) || isNovalue(bData)) {
                aspect = doubleNovalue;
            } else {
                // can't happen
                throw new RuntimeException();
            }
        }
        return (int)Math.round( aspect );
    }

    
    protected boolean isNovalue( double sample ) {
        return sample < 0;
    }
    
    
    @Override
    public void getFeatureSizeRequest( GetFeaturesSizeRequest request, ProcessorContext context ) throws Exception {
        context.sendResponse( new GetFeaturesSizeResponse( (BREAKPOINTS+1)^2 ) );
        context.sendResponse( ProcessorResponse.EOP );
    }


    @Override
    public void getFeatureBoundsRequest( GetBoundsRequest request, ProcessorContext context ) throws Exception {
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public void setTransactionRequest( TransactionRequest request, ProcessorContext context ) throws Exception {
        throw new RuntimeException( "No modification of raster generated features." );
    }

    @Override
    public void modifyFeaturesRequest( ModifyFeaturesRequest request, ProcessorContext context ) throws Exception {
        throw new RuntimeException( "No modification of raster generated features." );
    }

    @Override
    public void removeFeaturesRequest( RemoveFeaturesRequest request, ProcessorContext context ) throws Exception {
        throw new RuntimeException( "No modification of raster generated features." );
    }

    @Override
    public void addFeaturesRequest( AddFeaturesRequest request, ProcessorContext context ) throws Exception {
        throw new RuntimeException( "No modification of raster generated features." );
    }
    
}
