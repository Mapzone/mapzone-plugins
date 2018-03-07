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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import org.polymap.model2.runtime.UnitOfWork;

import io.mapzone.buildserver.BuildStrategy.BuildContext;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildManager {

    private static final Log log = LogFactory.getLog( BuildManager.class );
    
    public static final String BUILDRUNNER_LOG = "buildrunner.log";

    /** Maps {@link BuildConfig#id()} into {@link BuildManager} instances. */
    private static Map<String,BuildManager> instances = new ConcurrentHashMap();
    
    /**
     * The singleton for the given config.
     */
    public static BuildManager of( BuildConfig config ) {
        return instances.computeIfAbsent( (String)config.id(), key -> new BuildManager( config.id() ) );
    }
    
    
    // instance *******************************************
    
    private Object                      configId;
    
    private volatile BuildProcess       running;
    
    
    private BuildManager( Object configId ) {
        this.configId = configId;
    }


    public Optional<BuildProcess> running() {
        return Optional.ofNullable( running );
    }

    
    public synchronized BuildProcess startNewBuild() {
        if (running == null) {
            running = new BuildProcess();
            running.addJobChangeListener( new JobChangeAdapter() {
                @Override public void done( IJobChangeEvent ev ) {
                    running = null;
                }
            });
            running.schedule();
        }
        return running;
    }
    
        
    /**
     * 
     */
    public class BuildProcess
            extends Job {
    
        private Process             process;
        
        private File                logFile;
        
        public BuildProcess() {
            super( "Build: " + configId );
            setSystem( true );
        }
        
        @Override
        protected IStatus run( IProgressMonitor monitor ) {            
            BuildContext context = new BuildContext();
            PrintProgressMonitor printMonitor = new PrintProgressMonitor( monitor );

            try (
                UnitOfWork uow = BuildRepository.instance().newUnitOfWork();
            ){
                BuildConfig config = uow.entity( BuildConfig.class, configId );
                context.config.set( config );
                List<BuildStrategy> strategies = BuildStrategy.availableFor( config );
                try {
                    // pre
                    for (BuildStrategy strategy : strategies) {
                        strategy.preBuild( context, printMonitor );
                        checkCancel( monitor );
                    }
                    // post
                    for (BuildStrategy strategy : Lists.reverse( strategies )) {
                        strategy.postBuild( context, printMonitor );
                        checkCancel( monitor );
                    }
                }
                catch (Exception e) {
                    context.exception.set( e );
                    log.warn( "", e );
                }                
                // dispose
                for (BuildStrategy strategy : Lists.reverse( strategies )) {
                    try {
                        strategy.cleanup( context, printMonitor );
                    }
                    catch (Exception e) {
                        log.warn( "", e );
                    }
                }
            }
            return Status.OK_STATUS;
        }

        
        protected void checkCancel( IProgressMonitor monitor ) {
            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }
        }
        
    }
    
}
