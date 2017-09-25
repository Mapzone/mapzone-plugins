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
package io.mapzone.ide.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.commons.io.FileUtils;

import org.eclipse.core.runtime.NullProgressMonitor;

import io.mapzone.ide.apiclient.MapzoneAPIClient;
import io.mapzone.ide.apiclient.MapzoneAPIClient.PluginsFolder;
import io.mapzone.ide.apiclient.MapzoneAPIException;
import io.mapzone.ide.apiclient.MapzoneProject;
import io.mapzone.ide.apiclient.PublishedPlugin;


public class MapzoneAPIClientTest {

    private static final PrintStream    log = System.out;
    
    private static final String         PLUGIN_NAME = MapzoneAPIClientTest.class.getName();
    
    private static MapzoneAPIClient     client;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        String pwd = System.getProperty( "io.mapzone.ide.MapzoneAPIClient.pwd" );
        System.setProperty( "io.mapzone.ide.apiclient.host", "http://localhost:8090" );

        // test wrong password
        try {
            new MapzoneAPIClient( "falko", "WRONG" );
            log.println( "Logged in using wrong password!!!" );
            //fail( "Logged in using wrong password." );
        }
        catch (Exception e) {
            // ok
            log.println( e );
        }
        
        client = new MapzoneAPIClient( "falko", pwd );
        
        // remove pending plugin
        PublishedPlugin old = client.findPlugin( PluginsFolder.my, PLUGIN_NAME ).orElse( null );
        if (old != null) {
            old.delete();
            log.println( "Left-over plugin deleted." );
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    
    // tests **********************************************
    
    private PublishedPlugin newPlugin;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        if (newPlugin != null) {
            newPlugin.delete();
            newPlugin = null;
        }
    }


    @Test
    public void testFindPlugins() throws IOException {
        for (PublishedPlugin plugin : client.findPlugins( PluginsFolder.my )) {
            System.out.println( "Plugin: " + plugin.id() );
            System.out.println( "    " + plugin.entity().id.get() );
            System.out.println( "    " + plugin.entity().vendor.get() );
            System.out.println( "    " + plugin.entity().vendorUrl.get() );
            System.out.println( "    " + plugin.entity().created.get() );
            System.out.println( "    " + plugin.entity().updated.get() );
            System.out.println( "    " + plugin.entity().isReleased.get() );
        }
    }

    
    @Test
    public void testFindProjects() throws IOException {
        for (MapzoneProject p : client.findProjects( "falko" )) {
            System.out.println( "Project: " + p.organization() + " / " + p.name() );
            assertEquals( "falko", p.organization() );
            assertTrue( p.exists() );
        }
    }
    
    
    @Test
    public void testCreateModifyDeletePlugin() throws IOException {
        // create
        assertNull( newPlugin );
        newPlugin = client.newPlugin( PLUGIN_NAME );
        assertEquals( PLUGIN_NAME, newPlugin.id() );
        newPlugin.entity().vendor.set( "me" );
        assertEquals( "me", newPlugin.entity().vendor.get() );
        newPlugin.entity().title.set( getClass().getSimpleName() );
        newPlugin.entity().description.set( "description" );

        // submit
        newPlugin.submitChanges( new NullProgressMonitor() );
    
        // check properties
        PublishedPlugin plugin = client.findPlugin( PluginsFolder.my, PLUGIN_NAME ).get();
        assertEquals( PLUGIN_NAME, plugin.id() );
        assertEquals( "me", plugin.entity().vendor.get() );
        assertEquals( getClass().getSimpleName(), plugin.entity().title.get() );
        assertEquals( "description", plugin.entity().description.get() );
        
        // check existing ID
        try {
            client.newPlugin( PLUGIN_NAME );
            fail( "Duplicate was not detected." );
        }
        catch (MapzoneAPIException e) {
            // ok
        }
        
        // delete
        newPlugin.delete();
        newPlugin = null;
        assertFalse( client.findPlugin( PluginsFolder.my, PLUGIN_NAME ).isPresent() );
    }

    
    @Test
    public void testUpdateContents() throws IOException {
        assertNull( newPlugin );
        newPlugin = client.newPlugin( PLUGIN_NAME );
        newPlugin.entity().title.set( getClass().getSimpleName() );
        newPlugin.submitChanges( new NullProgressMonitor() );
        
        File f = Files.createTempFile( getClass().getName()+"-", null ).toFile();
        FileUtils.write( f, "Initial version." );
        newPlugin.updateContents( f, new NullProgressMonitor() );

        File f2 = Files.createTempFile( getClass().getName()+"-", null ).toFile();
        FileUtils.write( f2, "Second version." );
        newPlugin.updateContents( f2, new NullProgressMonitor() );
    }

    
    @Test
    public void testRevoke() throws IOException {
        fail( "not yet implemented" );
    }
    
    
    //@Test
    public void testProjectDownloadBundles() throws IOException {
        MapzoneProject project = client.findProjects( "falko" ).stream()
                .filter( p -> p.name().equalsIgnoreCase( "develop" ) )
                .findAny().get();

        File dir = Files.createTempDirectory( MapzoneAPIClientTest.class.getName()+"-" ).toFile();
        dir.deleteOnExit();
        
        project.downloadBundles( dir, new NullProgressMonitor() {
            @Override
            public void beginTask( String name, int totalWork ) {
                log.println( name );
            }
            @Override
            public void subTask( String name ) {
                log.println( "    Downloading: " + name );
            }
        });
        assertTrue( dir.listFiles().length > 0 );
    }

}
