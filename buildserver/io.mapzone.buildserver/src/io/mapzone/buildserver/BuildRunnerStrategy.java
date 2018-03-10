/* 
 * polymap.org
 * Copyright (C) 2018, the @authors. All rights reserved.
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
package io.mapzone.buildserver;

import java.util.concurrent.CancellationException;

import java.io.File;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Executes ../buildrunner.sh script and copies results into
 * {@link BuildContext#export} dir.
 *
 * @author Falko Bräutigam
 */
public class BuildRunnerStrategy
        extends BuildStrategy {

    private static final Log log = LogFactory.getLog( BuildRunnerStrategy.class );
    
    private Process         process;


    @Override
    public boolean init( BuildConfig config ) {
        return true;
    }


    @Override
    public void preBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Runner", IProgressMonitor.UNKNOWN );
        File buildrunner = new File( BsPlugin.buildserverDir(), "runners/test/buildrunner.sh" );
        //File buildrunner = new File( BsPlugin.buildserverDir(), "runners/eclipse-neon/buildrunner.sh" );

        process = new ProcessBuilder( buildrunner.getAbsolutePath(), 
                context.workspace.get().getAbsolutePath(), 
                context.config.get().productName.get(), 
                context.export.get().getAbsolutePath() )
                .directory( context.workspace.get() )
//                .redirectOutput( logFile )
//                .redirectError( logFile )
                .start();
        
        try (
            LineNumberReader in = new LineNumberReader( new InputStreamReader( process.getInputStream(), "UTF-8" ) );
            LineNumberReader err = new LineNumberReader( new InputStreamReader( process.getErrorStream(), "UTF-8" ) );
        ){
            while (process.isAlive() || in.ready() || err.ready()) {
                while (in.ready()) {
                    monitor.subTask( in.readLine() );
                }
                while (err.ready()) {
                    monitor.subTask( "ERROR: " + err.readLine() );
                }
                if (monitor.isCanceled()) {
                    throw new CancellationException( "Cancel requested" );
                }
                Thread.sleep( 1000 );
            }
        }
    }


    @Override
    public void postBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {
        // copy files
        File dataDir = new File( BsPlugin.exportDataDir(), context.config.get().productName.get()+System.currentTimeMillis() );
        dataDir.mkdir();
        
        context.result.get().dataDir.set( dataDir.getAbsolutePath() );
        
        File exportZip = new File( context.export.get(), "..." );
        if (exportZip.exists()) {
            FileUtils.copyFileToDirectory( exportZip, dataDir, true );
        }
        File logsZip = new File( context.export.get(), "logs.zip" );
        if (logsZip.exists()) {
            FileUtils.copyFileToDirectory( logsZip, dataDir, true );
        }
        
        File logFile = new File( context.export.get(), BuildResult.CONSOLE_LOG );
        if (logFile.exists()) {
            FileUtils.copyFileToDirectory( logFile, dataDir, true );
        }
    }
    
}
