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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;

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

    public static final Pattern LOG_LINE = Pattern.compile( "::[ ]*(.+)[ ]*@@(\\d+)/(\\d+)" );

    private static final String BUILDRUNNER_LOG = "buildrunner.log";
    
    private Process             process;


    @Override
    public boolean init( BuildConfig config ) {
        return true;
    }


    @Override
    public void preBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {
        //File buildrunner = new File( BsPlugin.buildserverDir(), "runners/test/buildrunner.sh" );
        File buildrunner = new File( BsPlugin.buildserverDir(), "runners/eclipse-neon/buildrunner.sh" );

        process = new ProcessBuilder( buildrunner.getAbsolutePath(), 
                context.workspace.get().getAbsolutePath(), 
                context.config.get().productName.get(), 
                context.export.get().getAbsolutePath() )
                .directory( context.workspace.get() )
                .start();

        String exceptionLine = null;
        try (
            LineNumberReader in = new LineNumberReader( new InputStreamReader( process.getInputStream(), "UTF-8" ) );
            LineNumberReader err = new LineNumberReader( new InputStreamReader( process.getErrorStream(), "UTF-8" ) );
            PrintWriter logFile = new PrintWriter( new File( context.export.get(), BUILDRUNNER_LOG ) );
        ){
            int worked = 0;
            while (process.isAlive() || in.ready() || err.ready()) {
                while (in.ready()) {
                    String line = in.readLine();
                    if (line.contains( "Exception" )) {
                        exceptionLine = line;
                    }
                    log.info( line );
                    
                    // parse monitor line
                    Matcher matcher = LOG_LINE.matcher( line );
                    if (matcher.find()) {
                        String text = matcher.group( 1 );
                        int newWorked = Integer.parseInt( matcher.group( 2 ) );
                        int total = Integer.parseInt( matcher.group( 3 ) );
                        if (worked == 0) {
                            monitor.beginTask( "Runner", total );                                
                        }
                        else {
                            monitor.subTask( text );
                        }
                        monitor.worked( newWorked-worked );
                        worked = newWorked;
                        logFile.println( text );
                    }
                }
                while (err.ready()) {
                    String line = err.readLine();
                    logFile.println( line );
                    if (line.contains( "Exception" )) {
                        exceptionLine = line;
                    }
                    log.error( line );
                }
                if (monitor.isCanceled()) {
                    throw new CancellationException( "Cancel requested" );
                }
                Thread.sleep( 250 );
            }
        }

        if (process.exitValue() != 0) {
            throw new Exception( "The compiler/packager has encountered an error:\n    -> " + exceptionLine );
        }
    }


    @Override
    public void postBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {
        // copy files
        File dataDir = new File( BsPlugin.exportDataDir(), context.config.get().productName.get()+System.currentTimeMillis() );
        dataDir.mkdir();
        
        context.result.get().dataDir.set( dataDir.getAbsolutePath() );
        
        File exportZip = new File( context.export.get(), "product.zip" );
        if (exportZip.exists()) {
            FileUtils.copyFileToDirectory( exportZip, dataDir, true );
        }
        File logsZip = new File( context.export.get(), "logs.zip" );
        if (logsZip.exists()) {
            FileUtils.copyFileToDirectory( logsZip, dataDir, true );
            context.exception.set( new Exception( "There are compiler errors." ) );
        }
        
        File logFile = new File( context.export.get(), BuildResult.CONSOLE_LOG );
        if (logFile.exists()) {
            FileUtils.copyFileToDirectory( logFile, dataDir, true );
        }
        File buildrunnerLogFile = new File( context.export.get(), BUILDRUNNER_LOG );
        if (buildrunnerLogFile.exists()) {
            FileUtils.copyFileToDirectory( buildrunnerLogFile, dataDir, true );
        }
    }
    
}
