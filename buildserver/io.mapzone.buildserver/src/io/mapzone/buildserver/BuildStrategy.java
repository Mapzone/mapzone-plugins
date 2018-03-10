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

import java.util.ArrayList;
import java.util.List;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.polymap.core.runtime.config.Config2;
import org.polymap.core.runtime.config.Configurable;
import org.polymap.core.runtime.config.Immutable;

import io.mapzone.buildserver.scm.GitStrategy;
import io.mapzone.buildserver.tp.ZipDownloadStrategy;

/**
 * Build strategies provide actions that have to be performed in order to build a
 * given {@link BuildConfig}.
 *
 * @author Falko Bräutigam
 */
public abstract class BuildStrategy {

    private static final Log log = LogFactory.getLog( BuildStrategy.class );

    public static final Class<?>[] EXTENSIONS = {
            InitBuildResultStrategy.class, 
            InitWorkspaceStrategy.class, 
            GitStrategy.class,
            ZipDownloadStrategy.class,
            //LocalDirectoryStrategy.class,
            BuildRunnerStrategy.class};
    
    
    public static List<BuildStrategy> availableFor( BuildConfig config ) {
        List<BuildStrategy> result = new ArrayList();
        for (Class<?> ext : EXTENSIONS) {
            try {
                BuildStrategy instance = (BuildStrategy)ext.newInstance();
                if (instance.init( config )) {
                    result.add( instance );
                }
            }
            catch (Exception e) {
                log.error( "", e );
            }
        }
        return result;
    }

    
    // instance *******************************************
    
    public abstract boolean init( BuildConfig config );
    
    public abstract void preBuild( BuildContext context, IProgressMonitor monitor ) throws Exception;

    public void postBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {}
    
    public void cleanup( BuildContext context, IProgressMonitor monitor ) throws Exception {}

    //public void dispose() {}

    protected IProgressMonitor submon( IProgressMonitor monitor, int ticks ) {
        return new SubProgressMonitor( monitor, ticks ) {
            
        };
    }
    
    
    /**
     * 
     */
    public static class BuildContext
            extends Configurable {
        
        @Immutable
        public Config2<BuildContext,BuildConfig>    config; 

        @Immutable
        public Config2<BuildContext,BuildConfig>    logFile; 

        @Immutable
        public Config2<BuildContext,BuildResult>    result; 

        @Immutable
        public Config2<BuildContext,Exception>      exception; 

        @Immutable
        public Config2<BuildContext,File>           workspace; 

        @Immutable
        public Config2<BuildContext,File>           export; 
    }

}
