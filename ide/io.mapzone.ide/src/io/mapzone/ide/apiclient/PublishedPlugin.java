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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;

import org.apache.commons.io.IOUtils;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.mapzone.ide.apiclient.MapzoneAPIClient.PluginsFolder;

/**
 * 
 * 
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class PublishedPlugin
        extends APIObject {
    
    public static final String  CONTENTS_JSON = "contents.json";
    
    private boolean         isNew;
    
    private String          pluginId;
    
    private PluginEntity    entity;

    private IPath           folderPath;
    

    public PublishedPlugin( MapzoneAPIClient client, String pluginId ) {
        super( client );
        this.isNew = true;
        this.pluginId = pluginId;
        this.entity = new PluginEntity( new JSONObject() );
        this.entity.id.set( pluginId );
    }
    
    public PublishedPlugin( MapzoneAPIClient client, IPath folderPath ) {
        super( client );
        this.isNew = false;
        this.folderPath = folderPath;
        this.pluginId = folderPath.lastSegment();
    }
    
    public boolean isNew() {
        return isNew;
    }
    
    /** The plugin id. */
    public String id() {
        return pluginId;
    }
    
    
    public PluginEntity entity() throws IOException {
        if (entity == null) {
            try (
                InputStream in = client().get( folderPath.append( CONTENTS_JSON ) );
            ){
                String content = IOUtils.toString( in, "UTF-8" );
                JSONObject json = new JSONObject( content );
                entity = new PluginEntity( json );
            }
        }
        return entity;
    }

    
    public void submitChanges( IProgressMonitor monitor ) throws IOException {
        monitor.beginTask( "Apply changes", 4 );
        
        // isNew
        if (isNew) {
            monitor.subTask( "Creating folder" );
            folderPath = MapzoneAPIClient.PLUGINS.append( PluginsFolder.my.name() ).append( id() );
            client().createFolder( folderPath );
        }
        monitor.worked( 2 );
        
        monitor.subTask( "Send " + CONTENTS_JSON );
        String json = entity.toJsonString( 4 );
        byte[] bytes = json.getBytes( "UTF-8" );        
        
        client().put( folderPath.append( CONTENTS_JSON ), new ByteArrayInputStream( bytes ), bytes.length );
        monitor.done();
    }

    
    /**
     * Sends a new version of the plugin to the catalog.
     *
     * @param f The plugin *.jar file.
     * @param monitor
     * @throws IOException 
     */
    public void updateContents( java.io.File f, IProgressMonitor monitor ) throws IOException {
        monitor = monitor != null ? monitor : new NullProgressMonitor();
        monitor.beginTask( "Update plugin contents", 3 );
        
        // delete current version
        for (IPath child : client.list( folderPath )) {
            if (!child.lastSegment().equals( CONTENTS_JSON )) {
                client().delete( child );
            }
        }
        monitor.worked( 1 );

        InputStream content = new BufferedInputStream( new FileInputStream( f ) );
        client().put( folderPath.append( f.getName() ), content, f.length() );
        
        monitor.done();
    }
    

    /**
     * Deletes this plugin on the server.
     * <p/>
     * <b>JUST FOR TESTS!</b> Do NOT use for regular client code! 
     */
    public void delete() throws IOException {
        client().delete( folderPath );
        client = null;
        entity = null;
        pluginId = null;
        folderPath = null;
    }
    
}
