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

import static io.mapzone.ide.util.UIUtils.submon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.mapzone.ide.util.UIUtils;
import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.httpclient.File;
import io.milton.httpclient.Folder;
import io.milton.httpclient.HttpException;
import io.milton.httpclient.HttpResult;
import io.milton.httpclient.Resource;
import io.milton.httpclient.Utils;
import net.sf.json.JSONObject;

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

    private Folder          folder;
    
    private File            entityContentsFile;


    public PublishedPlugin( MapzoneAPIClient client, String pluginId ) {
        super( client );
        this.isNew = true;
        this.pluginId = pluginId;
        this.entity = new PluginEntity( new JSONObject() );
        this.entity.id.set( pluginId );
    }
    
    public PublishedPlugin( MapzoneAPIClient client, Folder folder ) {
        super( client );
        this.isNew = false;
        this.folder = folder;
        this.pluginId = folder.name;
    }
    
    public boolean isNew() {
        return isNew;
    }
    
    /** The plugin id. */
    public String id() {
        return pluginId;
    }
    
    
    public PluginEntity entity() throws NotAuthorizedException, BadRequestException, IOException, HttpException {
        if (entity == null) {
            entityContentsFile = (File)folder.child( CONTENTS_JSON );
            try (
                ByteArrayOutputStream out = new ByteArrayOutputStream( 1024 );
            ){
                entityContentsFile.download( out, null );
                JSONObject json = JSONObject.fromObject( out.toString( "UTF-8" ) );
                entity = new PluginEntity( json );
            }
        }
        return entity;
    }

    
    public void applyChanges( IProgressMonitor monitor )
            throws NotAuthorizedException, BadRequestException, IOException, HttpException, ConflictException, NotFoundException {
        monitor.beginTask( "Apply changes", 4 );
        
        // isNew
        if (isNew) {
            monitor.subTask( "Creating folder" );
            Path path = MapzoneAPIClient.PLUGINS.child( "my" ).child( id() );
            client().host.createFolder( path.toPath() );
            monitor.worked( 1 );
            
            monitor.subTask( "Retrieve " + CONTENTS_JSON );
            folder = (Folder)client().host.find( path.toPath(), true );
            entityContentsFile = (File)folder.child( "contents.json" );
            monitor.worked( 1 );
        }
        else {
            monitor.worked( 2 );
        }
        
        monitor.subTask( "Send " + CONTENTS_JSON );
        String json = entity.toJsonString( 4 );
        byte[] bytes = json.getBytes( "UTF-8" );        
        ByteArrayInputStream in = new ByteArrayInputStream( bytes );
        
        // XXX ETag handling in milton client File does not work for me
        String newUri = entityContentsFile.encodedUrl();
        HttpResult result = client().host.doPut( newUri, in, (long)bytes.length, null, null/*new IfMatchCheck(etag)*/,
                new ProgressListenerAdapter( UIUtils.submon( monitor, 2 ) ) );
        //String etag = result.getHeaders().get( Response.Header.ETAG.code );
        int resultCode = result.getStatusCode();
        Utils.processResultCode( resultCode, newUri );
        
//      entityContentsFile.setContent( in, (long)bytes.length, null );
        monitor.done();
    }

    
    /**
     * Sends a new version of the plugin to the catalog.
     *
     * @param f The plugin *.jar file.
     * @param monitor
     * @throws HttpException 
     * @throws IOException 
     * @throws BadRequestException 
     * @throws NotAuthorizedException 
     * @throws NotFoundException 
     * @throws ConflictException 
     */
    public void updateContents( java.io.File f, IProgressMonitor monitor ) 
            throws NotAuthorizedException, BadRequestException, IOException, HttpException, ConflictException, NotFoundException {
        monitor = monitor != null ? monitor : new NullProgressMonitor();
        monitor.beginTask( "Update plugin contents", 3 );
        
        // delete current version
        for (Resource child : folder.children()) {
            if (child instanceof File && !((File)child).name.equals( CONTENTS_JSON )) {
                ((File)child).delete();
            }
        }
        monitor.worked( 1 );
        
        folder.upload( f, new ProgressListenerAdapter( submon( monitor, 2 ) ) );
        
        monitor.done();
    }
    
}
