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

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.eclipse.core.runtime.IProgressMonitor;

import io.milton.httpclient.File;
import io.milton.httpclient.Folder;
import io.milton.httpclient.Resource;

/**
 * Represents a project on the mapzone server. 
 * 
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class MapzoneProject
        extends APIObject {
    
    private String      organization;
    
    private Folder      folder;
    
    
    protected MapzoneProject( MapzoneAPIClient client, Folder folder, String organization ) {
        super( client );
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
            String path = MapzoneAPIClient.PROJECTS.child( organization ).child( name() ).toPath();
            Resource res = client.host.find( path );
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
            monitor.beginTask( "Downloading bundles", children.size() );

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
            monitor.beginTask( "Installing bundle " + bundle.getName(), (int)bundle.length() );
            Folder pluginsFolder = (Folder)folder.child( "plugins" );
            
            // delete previous versions
            String basename = StringUtils.substringBefore( bundle.getName(), "_" );
            for (Resource child : pluginsFolder.children()) {
                if (child.name.startsWith( basename )) {
                    monitor.subTask( "Delete previous version " + child.name );
                    child.delete();
                }
            }
            
            // upload
            monitor.subTask( "Uploading" );
            pluginsFolder.upload( bundle, new ProgressListenerAdapter( monitor ) );
            monitor.done();
        }
        catch (Exception e) {
            propagate( e );
        }
    }
}