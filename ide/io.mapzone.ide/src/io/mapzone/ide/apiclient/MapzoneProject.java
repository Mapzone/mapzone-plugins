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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents a project on the mapzone server. 
 * 
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class MapzoneProject
        extends APIObject {
    
    private String      organization;
    
    private IPath       folderPath;
    
    
    protected MapzoneProject( MapzoneAPIClient client, IPath folderPath, String organization ) {
        super( client );
        this.organization = organization;
        this.folderPath = folderPath;
    }

    public String organization() {
        return organization;
    }
    
    public String name() {
        try {
            return URLDecoder.decode( folderPath.lastSegment(), "UTF-8" );
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException( e );
        }
    }

    public boolean exists() throws MapzoneAPIException {
        try {
            return client().sardine.exists( client().url( folderPath ) );
            //return folder.getStatusCode() != 200;
        }
        catch (Exception e) {
            throw propagate( e );
        }
    }

    public void downloadBundles( java.io.File destDir, IProgressMonitor monitor ) throws MapzoneAPIException {
        try {
            List<IPath> children = client().list( folderPath.append( "plugins" ) );
            monitor.beginTask( "Downloading bundles", children.size() );

            for (IPath child : children) {
                monitor.subTask( child.lastSegment() );
                try (
                    InputStream in = client().get( child );
                ){
                    File f = new File( destDir, child.lastSegment() );
                    FileUtils.copyInputStreamToFile( in, f );
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

    /**
     * @deprecated See {@link ExportPluginProjectWizard}
     */
    public void installBundle( java.io.File bundle, IProgressMonitor monitor ) throws MapzoneAPIException {
        try {
            monitor.beginTask( "Installing bundle " + bundle.getName(), (int)bundle.length() );
            IPath pluginsPath = folderPath.append( "plugins" );
            
            // delete previous versions
            String basename = bundle.getName().split( "_" )[0];
            for (IPath child : client().list( pluginsPath )) {
                if (child.lastSegment().startsWith( basename )) {
                    monitor.subTask( "Delete previous version " + child.lastSegment() );
                    client.delete( child );
                }
            }
            
            // upload
            try (
                InputStream in = new BufferedInputStream( new FileInputStream( bundle ) );
            ){
                monitor.subTask( "Uploading" );
                client().put( pluginsPath.append( bundle.getName() ), in, bundle.length() );
            }
            monitor.done();
        }
        catch (Exception e) {
            propagate( e );
        }
    }
    
}