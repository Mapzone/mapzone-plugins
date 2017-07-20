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

import static io.mapzone.ide.UpdateTargetParameterValues.INSTANCE;
import static io.mapzone.ide.UpdateTargetParameterValues.JENKINS;

import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.core.target.ITargetDefinition;

/**
 * Performs an update of the currently active target platform.
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
@SuppressWarnings( "deprecation" )
public class UpdateTargetHandler
        extends AbstractHandler
        implements IHandler {

    public static final String  ARENA_DOWNLOAD_URL = "http://build.mapzone.io/release/io.mapzone.arena.product/1.0.0-SNAPSHOT/io.mapzone.arena.product-1.0.0-SNAPSHOT-linux.gtk.x86_64.zip";
    
    
    @Override
    public Object execute( ExecutionEvent ev ) throws ExecutionException {
        Map<String,String> params = ev.getParameters();
        String sourceParam = params.get( UpdateTargetParameterValues.PARAMETER_NAME );

        Job job = new Job( "Update target platform" ) {
            @Override
            protected IStatus run( IProgressMonitor monitor ) {
                try {
                    switch (sourceParam) {
                        case INSTANCE: return updateFromInstance( monitor ); 
                        case JENKINS: return updateFromJenkins( monitor ); 
                        default: throw new RuntimeException( "Unknown command parameter value: " + sourceParam );
                    }
                }
                catch (Exception e) {
                    return new Status( IStatus.ERROR, IdePlugin.ID, "Exception while executing task.", e );
                }
            }
        };
        job.setUser( true );
        job.schedule();
        return null;
    }
    
    
    protected IStatus updateFromInstance( IProgressMonitor monitor ) {
        monitor.beginTask( "Update target platform", 100 );
        throw new RuntimeException( "not yet implemented" );
    }
    
    
    protected IStatus updateFromJenkins( IProgressMonitor monitor ) throws IOException, CoreException {
        monitor.beginTask( "Update target platform", 100 );
        
        // download zip
        IProgressMonitor submon = submon( monitor, 90 );
        submon.beginTask( "Downloading...", 75 );
        URL url = new URL( ARENA_DOWNLOAD_URL );
        File temp = Files.createTempDirectory( IdePlugin.ID ).toFile();
        try (
            ZipInputStream zin = new ZipInputStream( new BufferedInputStream( url.openStream(), 4096 ) );
        ){
            ZipEntry entry = null;
            while ((entry = zin.getNextEntry()) != null) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                if (!entry.isDirectory() && entry.getName().startsWith( "plugins/" )) {
                    submon.subTask( entry.getName() );
                    File f = new File( temp, new File( entry.getName() ).getName() );
                    try (
                        OutputStream fout = new BufferedOutputStream( new FileOutputStream( f ) )
                    ){
                        IOUtils.copy( zin, fout );
                    }
                }
                submon.worked( 1 );
            }
        }

        // update active target
        TargetPlatformHelper platforms = TargetPlatformHelper.instance();
        ITargetDefinition activeTarget = platforms.active( submon( monitor, 5 ) );
        platforms.updateContents( activeTarget, temp, submon( monitor, 5 ) );
        
        return Status.OK_STATUS;
    }
    

    protected IProgressMonitor submon( IProgressMonitor monitor, int ticks ) {
        return new SubProgressMonitor( monitor, ticks );
    }

}
