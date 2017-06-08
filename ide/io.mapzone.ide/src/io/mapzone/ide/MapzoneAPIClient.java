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
package io.mapzone.ide;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.milton.common.Path;
import io.milton.httpclient.File;
import io.milton.httpclient.Folder;
import io.milton.httpclient.Host;
import io.milton.httpclient.ProgressListener;
import io.milton.httpclient.Resource;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class MapzoneAPIClient {

    public static final String  WEBDAV_PATH = "/webdav";
    
    public static final Path    PROJECTS = Path.path( "projects" );
    
    private Host                host;
    
    /**
     * 
     * 
     * @param baseUrl
     * @throws Exception
     */
    public MapzoneAPIClient( String server, int port, String user, String password ) {
        host = new Host( server, WEBDAV_PATH, port, user, password, null, 30000, null, null );
    }

    
//    public Optional<MapzoneProject> findProject( String organization, String name ) 
//            throws NotAuthorizedException, BadRequestException, IOException, HttpException {
//        MapzoneProject project = new MapzoneProject( organization, name );
//        return Optional.ofNullable( project.exists() ? project : null );
//    }
    
    
    public List<MapzoneProject> findProjects( String organization ) throws MapzoneAPIException {
        try {
            String path = PROJECTS.child( organization ).toPath();
            Resource folder = host.find( path );
            if (folder == null) {
                return Collections.EMPTY_LIST;
            }
            else if (folder instanceof Folder) {
                return ((Folder)folder).children().stream()
                        .map( child -> new MapzoneProject( (Folder)child, organization ) )
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

    
    protected RuntimeException propagate( Throwable e ) {
        return e instanceof RuntimeException ? (RuntimeException)e : new MapzoneAPIException( e );
    }
    
    
    /**
     * 
     */
    public class MapzoneProject {
        
        private String      organization;
        
        private Folder      folder;
        
        
        protected MapzoneProject( Folder folder, String organization ) {
            this.organization = organization;
            this.folder = folder;
        }

        public String organization() {
            return organization;
        }
        
        public String name() {
            return folder.displayName;
        }

        public boolean exists() throws MapzoneAPIException {
            try {
                String path = PROJECTS.child( organization ).child( name() ).toPath();
                Resource res = host.find( path );
                return res != null && res instanceof Folder;
            }
            catch (Exception e) {
                throw propagate( e );
            }
        }

        public void downloadBundles( java.io.File destDir, IProgressMonitor monitor ) throws MapzoneAPIException {
            try {
                Folder pluginsFolder = (Folder)folder.child( "plugins" );
                List<? extends Resource> children = pluginsFolder.children();
                monitor.beginTask( "Download bundles", children.size() );

                for (Resource child : children) {
                    try {
                        monitor.subTask( child.displayName );
                        ((File)child).downloadTo( destDir, null );
                    }
                    catch (Exception e) {
                        System.out.println( e );
                    }
                    monitor.worked( 1 );
                }
                monitor.done();
            }
            catch (Exception e) {
                throw propagate( e );
            }
        }
        
        public void installBundle( java.io.File bundle, IProgressMonitor monitor ) throws MapzoneAPIException {
            try {
                monitor.beginTask( "Upload bundle " + bundle.getName(), (int)bundle.length() );
                Folder pluginsFolder = (Folder)folder.child( "plugins" );
                pluginsFolder.upload( bundle, new ProgressListenerAdapter( monitor ) );
                monitor.done();
            }
            catch (Exception e) {
                propagate( e );
            }
        }
    }

    
    static class ProgressListenerAdapter
            implements ProgressListener {

        private IProgressMonitor    delegate;
        
        public ProgressListenerAdapter( IProgressMonitor delegate ) {
            this.delegate = delegate;
        }
        @Override 
        public void onRead( int bytes ) { }
        @Override 
        public void onProgress( long bytesRead, Long totalBytes, String fileName ) {
            delegate.worked( (int)bytesRead );
        }
        @Override 
        public void onComplete( String fileName ) { }
        @Override 
        public boolean isCancelled() {
            return delegate.isCanceled();
        }
    }
    
    
    // Test ***********************************************
    
    public static void main( String[] args ) throws Exception {
        MapzoneAPIClient service = new MapzoneAPIClient( "localhost", 8090, "falko", "???" );
        
        MapzoneProject project = service.findProjects( "falko" ).stream()
                .filter( p -> p.name().equalsIgnoreCase( "develop" ) )
                .findAny().get();
        
        System.out.println( "Project: " + project.organization + " / " + project.name() );
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
