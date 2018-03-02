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

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

import java.io.File;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Throwables;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import org.polymap.core.CorePlugin;

import org.polymap.model2.runtime.UnitOfWork;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildManager {

    private static final Log log = LogFactory.getLog( BuildManager.class );

    private static Map<String,BuildManager> instances = new ConcurrentHashMap();
    
    /**
     * The singleton for the given config.
     */
    public static BuildManager of( BuildConfiguration config ) {
        return instances.computeIfAbsent( (String)config.id(), key -> new BuildManager( config ) );
    }
    
    
    // instance *******************************************
    
    private BuildConfiguration      config;
    
    private volatile BuildProcess   running;
    
    
    private BuildManager( BuildConfiguration config ) {
        this.config = config;
    }


    public Optional<BuildProcess> running() {
        return Optional.ofNullable( running );
    }

    
    public synchronized BuildProcess start() {
        if (running == null) {
            running = new BuildProcess();
            running.addJobChangeListener( new JobChangeAdapter() {
                @Override public void done( IJobChangeEvent ev ) {
                    running = null;
                    pruneResults();
                }
            });
            running.schedule();
        }
        return running;
    }
    
    
    protected void pruneResults() {
        Map<Date,BuildResult> sorted = new TreeMap();
        config.buildResults.forEach( result -> sorted.put( result.started.get(), result ) );
        for (Iterator<BuildResult> it=sorted.values().iterator(); it.hasNext() && sorted.size() > 3; ) {
            BuildResult result = it.next(); it.remove();
            result.destroy();
        }
        config.belongsTo().commit();
    }
    
    
    /**
     * 
     */
    public class BuildProcess
            extends Job {
    
        private Process             process;
        
        private File                logFile;
        
        public BuildProcess() {
            super( "Build: " + config.name.get() );
            setSystem( true );
        }

        @Override
        protected IStatus run( IProgressMonitor monitor ) {
            UnitOfWork uow = config.belongsTo();

            BuildResult buildResult = uow.createEntity( BuildResult.class, null, (BuildResult proto) -> {
                proto.config.set( config );
                proto.started.set( new Date() );
                proto.status.set( BuildResult.Status.RUNNING );
                return proto;
            });
            uow.commit();
            
            try {
                
                File workspaceDir = Files.createTempDirectory( "buildserver.workspace." ).toFile();
                File exportDir = Files.createTempDirectory( "buildserver.export." ).toFile();
                logFile = new File( exportDir, "log" );

                PrintProgressMonitor printMonitor = new PrintProgressMonitor( monitor );
                
                // prepare workspace
                workspaceDir.mkdir();
                Build build = new Build( config, workspaceDir );
                build.run( printMonitor );
                printMonitor.done();
                
                // BuildRunner process
                exportDir.mkdir();
                File buildrunner = new File( CorePlugin.getDataLocation( BsPlugin.instance() ), "../../buildrunner.sh" );
                process = new ProcessBuilder( buildrunner.getAbsolutePath(), config.productName.get(), exportDir.getAbsolutePath() )
                        .directory( workspaceDir )
                        .redirectOutput( logFile ).redirectError( logFile )
                        .start();
                while (process.isAlive()) {
                    if (monitor.isCanceled()) {
                        throw new CancellationException( "Cancel requested" );
                    }
                    Thread.sleep( 1000 );
                }
                
                // copy files
                File exportZip = new File( exportDir, "..." ); 
                File logsZip = new File( exportDir, "logs.zip" ); 
                File dataDir = new File( BsPlugin.exportDataDir(), config.productName.get()+System.currentTimeMillis() );
                dataDir.mkdir();
                FileUtils.copyFileToDirectory( exportZip, dataDir, true );
                FileUtils.copyFileToDirectory( logsZip, dataDir, true );
                
                // commit result
                buildResult.status.set( BuildResult.Status.OK );
                buildResult.dataDir.set( dataDir.getAbsolutePath() );
                uow.commit();
                return Status.OK_STATUS;
            }
            catch (Exception e) {
                log.warn( "", e );
                buildResult.status.set( BuildResult.Status.FAILED );
                uow.commit();
                throw Throwables.propagate( e );
            }
        }

//        public synchronized void stop() {
//        }
    }
    
}
