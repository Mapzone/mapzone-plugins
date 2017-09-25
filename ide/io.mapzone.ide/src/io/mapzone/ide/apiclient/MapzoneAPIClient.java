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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.google.common.base.Throwables;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class MapzoneAPIClient {
        //implements AutoCloseable {

    public static final String  DEFAULT_HOST = "http://mapzone.io:80";
    
    public static final IPath   WEBDAV = Path.forPosix( "webdav/" );
    
    public static final IPath   PROJECTS = WEBDAV.append( "projects" );
    
    public static final IPath   PLUGINS = WEBDAV.append( "plugins" );
    
    public enum PluginsFolder {
        my, all
    }
    
    // instance ****************************************
    
    protected Sardine           sardine;

    private String              host;
    
    private String              username;
    
    /**
     * 
     * 
     * @param baseUrl
     * @throws Exception 
     * @throws {@link MapzoneAPIException} If connection failed or login failed.
     */
    public MapzoneAPIClient( String username, String password ) throws MapzoneAPIException {
        try {
            host = System.getProperty( "io.mapzone.ide.apiclient.host", DEFAULT_HOST );
            System.out.println( "Host: " + host );
            
            sardine = SardineFactory.begin( username, password );
            this.username = username;
            
            URL url = new URL( host.toString() );
            sardine.enablePreemptiveAuthentication( url.getHost(), url.getPort(), -1 );
            
            list( WEBDAV );  // check connection
        }
        catch (MapzoneAPIException e) {
            throw e;
        }
        catch (Exception e) {
            close();            
            Throwable root = Throwables.getRootCause( e );
            throw new MapzoneAPIException( root.getLocalizedMessage(), root );
        }
    }

    //@Override
    public void close() {
        if (sardine != null) {
            try {
                sardine.shutdown();
            }
            catch (Exception e) {
                System.err.println( e.toString() );
            }
            sardine = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }


    public String username() {
        return username;
    }


    public List<MapzoneProject> findProjects( String organization ) throws MapzoneAPIException {
        try {
            return list( PROJECTS.append( organization ) ).stream()
                    .map( child -> new MapzoneProject( this, child, organization ) )
                    .collect( toList() );
        }
        catch (Exception e) {
            throw propagate( e );
        }
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
     * @throws MapzoneAPIException If the pluginId already exists. Does not check for
     *         revoked or not yet released plugins
     */
    public PublishedPlugin newPlugin( String pluginId ) throws MapzoneAPIException {
        // fail fast if pluginId already exists; 
        // does not work for revoked or not yet released plugins
        if (findPlugin( PluginsFolder.all, pluginId ).isPresent()) {
            throw new MapzoneAPIException( "Plugin Id exists already." );
        }
        if (findPlugin( PluginsFolder.my, pluginId ).isPresent()) {
            throw new MapzoneAPIException( "Plugin Id exists already." );
        }
        return new PublishedPlugin( this, pluginId );
    }

    
    /**
     * 
     *
     * @param folderName "my" or "all"
     * @return List of newly created {@link PublishedPlugin} instances.
     */
    public List<PublishedPlugin> findPlugins( PluginsFolder folder ) throws MapzoneAPIException {
        try {
            return list( PLUGINS.append( folder.name() ) ).stream()
                    .map( child -> new PublishedPlugin( this, child ) )
                    .collect( toList() );
        }
        catch (Exception e) {
            throw propagate( e );
        }
    }

    
    public Optional<PublishedPlugin> findPlugin( PluginsFolder folder, String pluginId ) {
        return findPlugins( folder ).stream().filter( p -> p.id().equals( pluginId ) ).findAny();
    }
    
    // low level API **************************************
    
    public List<IPath> list( IPath path ) throws IOException {
        String url = url( path );
        return sardine.list( url, 1 ).stream()
                .skip( 1 )  // parent
                .map( res -> path( res ) )
                .collect( Collectors.toList() );
    }

    public void createFolder( IPath path ) throws IOException {
        sardine.createDirectory( url( path ) );
    }
    
    public InputStream get( IPath path ) throws IOException {
        return sardine.get( url( path ) );
    }

    public void put( IPath path, InputStream in, long length ) throws IOException {
        try {
            sardine.put( url( path ), in );
        }
        finally {
            IOUtils.closeQuietly( in );
        }
    }

    public void delete( IPath path ) throws IOException {
        sardine.delete( url( path ) );
    }

    public String url( IPath path ) {
        return host + "/" + path.toString();
    }

    public IPath path( DavResource res ) {
        return Path.forPosix( res.getHref().toString() );
    }

    static RuntimeException propagate( Throwable e ) {
        return e instanceof RuntimeException ? (RuntimeException)e : new MapzoneAPIException( e );
    }
    
    
    // Test ***********************************************
    
    public static void main( String[] args ) throws Exception {
        String pwd = System.getProperty( "io.mapzone.ide.MapzoneAPIClient.pwd" );
        MapzoneAPIClient client = new MapzoneAPIClient( "falko", pwd );

        for (PublishedPlugin plugin : client.findPlugins( PluginsFolder.my )) {
            System.out.println( "Plugin: " + plugin.id() );
            System.out.println( "    " + plugin.entity().id.get() );
            System.out.println( "    " + plugin.entity().vendor.get() );
            System.out.println( "    " + plugin.entity().vendorUrl.get() );
            System.out.println( "    " + plugin.entity().created.get() );
            System.out.println( "    " + plugin.entity().updated.get() );
            System.out.println( "    " + plugin.entity().isReleased.get() );
        }
        
        for (MapzoneProject p : client.findProjects( "falko" )) {
            System.out.println( "Project: " + p.organization() + " / " + p.name() );
            System.out.println( "    " + p.exists() );
        }
    }

}
