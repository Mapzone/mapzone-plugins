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
package io.mapzone.ide.apiclient;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Throwables;

import org.eclipse.core.runtime.NullProgressMonitor;

import io.milton.common.Path;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.httpclient.Folder;
import io.milton.httpclient.Host;
import io.milton.httpclient.Resource;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class MapzoneAPIClient {
        //implements AutoCloseable {

    public static final String  DEFAULT_HOST = "mapzone.io:80";
    
    public static final String  WEBDAV_PATH = "/webdav";
    
    public static final Path    PROJECTS = Path.path( "projects" );
    
    public static final Path    PLUGINS = Path.path( "plugins" );
    
    protected Host              host;
    
    /**
     * 
     * 
     * @param baseUrl
     * @throws RuntimeException If login failed.
     */
    public MapzoneAPIClient( String user, String password ) {
        try {
            String[] hostProp = System.getProperty( "io.mapzone.ide.apiclient.host", DEFAULT_HOST ).split( ":" );
            assert hostProp.length == 2 : "Malformed property io.mapzone.ide.apiclient.host" ;
            System.out.println( "Host: " + Arrays.asList( hostProp ) );
            
            host = new Host( hostProp[0], WEBDAV_PATH, Integer.parseInt( hostProp[1] ), user, password, null, 30000, null, null );
            findPlugins( "my" );  // check connection
        }
        catch (Exception e) {
            // close host!?
            host = null;
            
            Throwable root = Throwables.getRootCause( e );
            if (root instanceof NotAuthorizedException) {
                throw new RuntimeException( "Username or password is not correct." );
            }
            throw e;
        }
    }

    //@Override
    public void close() throws Exception {
        host.getClient().getConnectionManager().shutdown();
        host = null;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    public String hostname() {
        return host.server;
    }

    public Integer port() {
        return host.port;
    }

    public String username() {
        return host.user;
    }


    public List<MapzoneProject> findProjects( String organization ) throws MapzoneAPIException {
        try {
            String path = PROJECTS.child( organization ).toPath();
            Resource folder = host.find( path );
            if (folder == null) {
                return Collections.EMPTY_LIST;
            }
            else if (folder instanceof Folder) {
                return ((Folder)folder).children().stream()
                        .map( child -> new MapzoneProject( this, (Folder)child, organization ) )
                        .collect( toList() );
            }
            else {
                throw new IllegalArgumentException( "Resource is not a folder: " + folder );
            }
        }
        catch (Exception e) {
            throw propagate( e );
        }
    }

    
    /**
     * 
     *
     * @param folderName "my" or "all"
     * @return List of newly created {@link PublishedPlugin} instances.
     */
    public List<PublishedPlugin> findPlugins( String folderName ) throws MapzoneAPIException {
        try {
            String path = PLUGINS.child( folderName ).toPath();
            Resource folder = host.find( path );
            if (folder == null) {
                return Collections.EMPTY_LIST;
            }
            else if (folder instanceof Folder) {
                return ((Folder)folder).children().stream()
                        .map( child -> new PublishedPlugin( this, (Folder)child ) )
                        .collect( toList() );
            }
            else {
                throw new IllegalArgumentException( "Resource is not a folder: " + folder );
            }
        }
        catch (Exception e) {
            throw propagate( e );
        }
    }

    
    public Optional<PublishedPlugin> findPlugin( String folderName, String pluginId ) {
        return findPlugins( folderName ).stream().filter( p -> p.id().equals( pluginId ) ).findAny();
    }
    
    
    public interface Initializer<T> {
        public void init( T prototype ) throws Exception;
    }
    
    
    /**
     * Creates a new plugin instance that is not yet
     * {@link PublishedPlugin#applyChanges() applied} to the server.
     *
     * @param pluginName
     * @return Newly create {@link PublishedPlugin} instance.
     */
    public PublishedPlugin newPlugin( String pluginId ) {
        return new PublishedPlugin( this, pluginId );
    }
    
    
    static RuntimeException propagate( Throwable e ) {
        return e instanceof RuntimeException ? (RuntimeException)e : new MapzoneAPIException( e );
    }
    
    
    // Test ***********************************************
    
    public static void main( String[] args ) throws Exception {
        String pwd = System.getProperty( "io.mapzone.ide.MapzoneAPIClient.pwd" );
//        MapzoneAPIClient client = new MapzoneAPIClient( "mapzone.io", 80, "falko", pwd );
        MapzoneAPIClient client = new MapzoneAPIClient( "falko", pwd );

//        PublishedPlugin newPlugin = client.newPlugin( "test.plugin6" );
//        newPlugin.entity().vendor.set( "Abend" );
//        newPlugin.applyChanges( null );
//                
//        for (PublishedPlugin plugin : client.findPlugins( "my" )) {
//            System.out.println( "Plugin: " + plugin.id() );
//            System.out.println( "    " + plugin.entity().id.get() );
//            System.out.println( "    " + plugin.entity().vendor.get() );
//            System.out.println( "    " + plugin.entity().vendorUrl.get() );
//            System.out.println( "    " + plugin.entity().created.get() );
//            System.out.println( "    " + plugin.entity().updated.get() );
//            System.out.println( "    " + plugin.entity().isReleased.get() );
//        }
        
//        PublishedPlugin plugin = client.findPlugins( "all" ).stream().filter( p -> p.id().equals( "test.plugin1" ) ).findAny().get();
//        plugin.entity().vendor.set( "Polymap GmbH" );
//        plugin.applyChanges();
        
        
        client.findProjects( "falko" ).forEach( p ->
                System.out.println( "Project: " + p.organization() + " / " + p.name() ) );
            
//        downloadTarget( service, "falko", "develop" );
    }

    
    protected static void downloadTarget( MapzoneAPIClient client, String org, String projectname ) {
        MapzoneProject project = client.findProjects( org ).stream()
                .filter( p -> p.name().equalsIgnoreCase( projectname ) )
                .findAny().get();

        System.out.println( "Project: " + project.organization() + " / " + project.name() );
        java.io.File dir = new java.io.File( "/tmp", "test.target" );
        dir.mkdir();
        dir.deleteOnExit();
        
        project.downloadBundles( dir, new NullProgressMonitor() {
            @Override
            public void beginTask( String name, int totalWork ) {
                System.out.println( name );
            }
            @Override
            public void subTask( String name ) {
                System.out.println( "    " + name );
            }
        });
    }
    
}
