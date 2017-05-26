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

import java.io.ByteArrayOutputStream;

import io.milton.common.Path;
import io.milton.httpclient.File;
import io.milton.httpclient.Folder;
import io.milton.httpclient.Host;
import io.milton.httpclient.Resource;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class MapzoneService {

    public static final String  WEBDAV_PATH = "/webdav";
    
    public static final Path    PROJECTS = Path.path( "projects" );
    
    private Host                host;
    
    /**
     * 
     * 
     * @param baseUrl
     * @throws Exception
     */
    public MapzoneService( String server, int port, String user, String password ) {
        host = new Host( server, WEBDAV_PATH, port, user, password, null, 10000, null, null );
    }

    
//    public Optional<MapzoneProject> findProject( String organization, String name ) 
//            throws NotAuthorizedException, BadRequestException, IOException, HttpException {
//        MapzoneProject project = new MapzoneProject( organization, name );
//        return Optional.ofNullable( project.exists() ? project : null );
//    }
    
    
    public List<MapzoneProject> findProjects( String organization ) {
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
        return e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException( e );
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

        public boolean exists() {
            try {
                String path = PROJECTS.child( organization ).child( name() ).toPath();
                Resource res = host.find( path );
                return res != null && res instanceof Folder;
            }
            catch (Exception e) {
                throw propagate( e );
            }
        }

        public void downloadTargetPlatform() {
            try {
                Folder pluginsFolder = (Folder)folder.child( "plugins" );
                for (Resource child : pluginsFolder.children()) {
                    File f = (File)child;
                    System.out.println( "   Plugin: " + f.displayName );
                    
                    if (f.displayName.startsWith( "org.polymap.core_" )) {
                        System.out.print( "       " + f.contentLength );
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        f.download( buf, null );
                        System.out.println( " " + buf.size() );
                    }
                }
            }
            catch (Exception e) {
                throw propagate( e );
            }
        }
    }

    
    // Test ***********************************************
    
    public static void main( String[] args ) throws Exception {
        MapzoneService service = new MapzoneService( "localhost", 8090, "falko", "" );
        
        List<MapzoneProject> projects = service.findProjects( "falko" );
        for (MapzoneProject project : projects) {
            System.out.println( "Project: " + project.organization + " / " + project.name() );
            
            project.downloadTargetPlatform();
        }
    }
    
}
