/* 
 * polymap.org
 * Copyright (C) 2017, the @authors. All rights reserved.
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
package org.polymap.tutorial.osm.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.apache.commons.io.FileUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import org.polymap.tutorial.osm.importer.TagFilter;
import org.polymap.tutorial.osm.importer.api.Overpass;
import org.polymap.tutorial.osm.importer.api.Overpass.Query;
import org.polymap.tutorial.osm.importer.api.Overpass.TagQuery;
import org.polymap.tutorial.osm.importer.api.OverpassFeatureCollection;
import org.polymap.tutorial.osm.importer.api.OverpassFeatureIterator;

/**
 * 
 * 
 * @author Falko Bräutigam
 */
public class OverpassTest {

    public static final ReferencedEnvelope LE = new ReferencedEnvelope( 
            12.263489, 12.453003, 51.28597, 51.419764, DefaultGeographicCRS.WGS84 );
    
//    public static final ReferencedEnvelope LE2 = new ReferencedEnvelope( 
//            , DefaultGeographicCRS.WGS84 );
//    
//    50.98952205260841,11.751845655493021,51.693024888038394,12.917836288741062
    
    public static final PrintStream out = System.out;
    
    public static File              cacheDir;
    
    public static Overpass          overpass = Overpass.instance();
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        cacheDir = new File( "/tmp/" + OverpassTest.class.getName() );
        cacheDir.mkdir();
    }

    public static void printResultSize( Query query ) throws IOException {
        out.println( "Result size: " + query.resultSize() );        
    }

    protected static void printResults( Query query, SimpleFeatureType schema ) throws IOException {
        OverpassFeatureCollection fc = new OverpassFeatureCollection( schema, query );
        fc.accepts( feature -> {
            System.out.println( "    " + feature.getDefaultGeometryProperty().getValue() );        
        }, null );
    }

    protected InputStream cachedContent( String testname, URL url ) throws Exception {
        String key = testname + ".osm.gz";
        File f = new File( cacheDir, key );
        if (!f.exists()) {
            URLConnection conn = url.openConnection();
            conn.setRequestProperty( "Accept-Encoding", "gzip" );
            try (InputStream in = conn.getInputStream()) {
                out.print( "Reading: " + f.getName() + " ..." );
                assert "gzip".equals( conn.getContentEncoding() );
                FileUtils.copyInputStreamToFile( in, f );
                out.println( " done." );
            }
        }
        return new GZIPInputStream( new FileInputStream( f ), 4*1024 );
    }
    
    protected SimpleFeatureType schema( Class<? extends Geometry> geom, String... names ) throws Exception {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName( "Test" ); //new NameImpl("PersonOrganisation_" + sdf.format(new Date())));
        builder.setCRS( DefaultGeographicCRS.WGS84 );
        builder.setDefaultGeometry( "geom" );
        builder.add( "geom", geom );
        Arrays.stream( names ).forEach( name -> builder.add( name, String.class ) );
        return builder.buildFeatureType();
    }
    
    
    @Test
    public void test7477221() throws Exception {
        Query query = overpass.prepared( "(rel(7477221););out geom;" );
        OverpassFeatureIterator it = new OverpassFeatureIterator( schema( MultiPolygon.class ), 
                cachedContent( "test7477221", query.downloadUrl() ) );
        assertTrue( it.hasNext() );
        SimpleFeature feature = it.next();
        assertFalse( ((MultiPolygon)feature.getDefaultGeometry()).isEmpty() );
        assertFalse( it.hasNext() );        
    }
    
    
    @Test
    public void testBuildings() throws Exception {
        Class<? extends Geometry> geomType = MultiPolygon.class;
        List<TagFilter> filters = Arrays.asList( 
                //TagFilter.of( "name", "*" ), 
                TagFilter.of( "building", "*" ) );
        TagQuery query = Overpass.instance().query()
                .whereBBox( LE )
                .whereTags( filters )
                .resultTypes( OverpassFeatureIterator.resultTypesFor( geomType ) );
        SimpleFeatureType schema = TagFilter.schemaOf( "test", filters, geomType );
        
        //assertEquals( 1975, fc.size() );
        
        OverpassFeatureIterator it = new OverpassFeatureIterator( schema, cachedContent( "buildings", query.downloadUrl() ));
        int c = 0;
        while (it.hasNext()) {
            Geometry geom = (Geometry)it.next().getDefaultGeometryProperty().getValue();
            assertTrue( "Geometry is not simple: " + geom, geom.isSimple() );
            assertTrue( geom.isValid() );
            c ++;
        }
        assertEquals( 1972, c );        
    }
    
}
