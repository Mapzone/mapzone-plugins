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

import java.util.ArrayList;
import java.util.List;

import java.awt.image.RenderedImage;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.parameter.Parameter;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.spatial.BBOX;
import org.opengis.geometry.BoundingBox;
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
import org.polymap.core.data.feature.ModifyFeaturesRequest;
import org.polymap.core.data.feature.RemoveFeaturesRequest;
import org.polymap.core.data.feature.TransactionRequest;
import org.polymap.core.data.pipeline.DataSourceDescriptor;
import org.polymap.core.data.pipeline.PipelineExecutor.ProcessorContext;
import org.polymap.core.data.pipeline.PipelineProcessorSite;
import org.polymap.core.data.pipeline.ProcessorResponse;
import org.polymap.core.data.pipeline.TerminalPipelineProcessor;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class Raster2PointProcessor
        implements TerminalPipelineProcessor, FeaturesProducer {

    private static final Log log = LogFactory.getLog( Raster2PointProcessor.class );

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
        ftb.add( "aspect", Integer.class );
        ftb.add( "sample", Double.class );
        schema = ftb.buildFeatureType();
    }


    @Override
    public void getFeatureTypeRequest( GetFeatureTypeRequest request, ProcessorContext context ) throws Exception {
        context.sendResponse( new GetFeatureTypeResponse( schema ) );
        context.sendResponse( ProcessorResponse.EOP );
    }


    protected GeneralParameterValue generalParameter( BoundingBox bounds ) {
        Parameter<GridGeometry2D> param = new Parameter<GridGeometry2D>( AbstractGridFormat.READ_GRIDGEOMETRY2D );
        GridEnvelope2D gridEnvelope = new GridEnvelope2D( 0, 0, 256, 256 );
        Envelope env;
//        if (crs != null) {
            env = new ReferencedEnvelope( bounds ); //, schema.getCoordinateReferenceSystem() );
//        }
//        else {
//            DirectPosition2D minDp = new DirectPosition2D( west, south );
//            DirectPosition2D maxDp = new DirectPosition2D( east, north );
//            env = new Envelope2D( minDp, maxDp );
//        }
        param.setValue( new GridGeometry2D( gridEnvelope, env ) );
        return param;
    }

    
    @Override
    public void getFeatureRequest( GetFeaturesRequest request, ProcessorContext context ) throws Exception {
        BoundingBox bounds = ReferencedEnvelope.EVERYTHING;
        if (request.getQuery().getFilter() instanceof BBOX) {
            bounds = ((BBOX)request.getQuery().getFilter()).getBounds();
        }
//        else {
//            throw new RuntimeException( "No BBOX query, not implemented.");
//        }
        
        GeneralParameterValue param = generalParameter( bounds );
        GridCoverage2D raster = reader.read( coverageName, new GeneralParameterValue[] {param} );
        RenderedImage renderedImage = raster.getRenderedImage();
        RandomIter it = RandomIterFactory.create( renderedImage, null );
        GeometryFactory gf = new GeometryFactory();
        
        int width = 256, height = 256;
        int breakpoints = 10;
        List<Feature> features = new ArrayList( breakpoints*breakpoints );
        log.info( "Sample: " + bounds.getMinX() + " - " + bounds.getMinY() );
        for (int x=0; x<breakpoints; x++) {
            for (int y=0; y<breakpoints; y++) {
                SimpleFeatureBuilder fb = new SimpleFeatureBuilder( schema );
                double sample = it.getSampleDouble( width/breakpoints*x, height/breakpoints*y, 0 );
                fb.set( "sample", Math.max( 0, sample ) );  // novalue
                fb.set( "aspect", 0d );
                double lon = (bounds.getWidth() / breakpoints) * x + bounds.getMinX();
                double lat = (bounds.getHeight() / breakpoints) * y + bounds.getMinY();
                fb.set( "geom", gf.createPoint( new Coordinate( lon, lat ) ) );
                features.add( fb.buildFeature( null ) );
            }
        }
        context.sendResponse( new GetFeaturesResponse( features ) );
        context.sendResponse( ProcessorResponse.EOP );
    }


    @Override
    public void getFeatureSizeRequest( GetFeaturesSizeRequest request, ProcessorContext context ) throws Exception {
        throw new RuntimeException( "not yet implemented." );
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
