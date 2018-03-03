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
package io.mapzone.buildserver.scm;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfiguration.ScmConfiguration;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class ScmRepository {

    private static final Log log = LogFactory.getLog( ScmRepository.class );
    
    public static final File CACHE_DIR = new File( BsPlugin.buildserverDir(), "scm" );
    
    public static ScmRepository forConfig( ScmConfiguration config ) {
        if (config.type.get() == ScmConfiguration.Type.GIT) {
            return new GitRepository().init( config );
        }
        else {
            throw new RuntimeException( "Unhandled SCM type: " + config.type.get() );
        }
    }

    // instance *******************************************
    
    public abstract ScmRepository init( ScmConfiguration config );
    
    /**
     * Bring local store of the repo in sync with remote repository. 
     * @throws Exception 
     */
    public abstract void update( IProgressMonitor monitor ) throws Exception;
    
    
    public abstract void switchBranch( String branch, IProgressMonitor monitor ) throws Exception;

    public abstract boolean copyBundle( String name, File dir ) throws Exception;
    
    
    protected Process process( String[] command, File dir, IProgressMonitor monitor ) throws Exception {
        Process process = new ProcessBuilder( command )
                .directory( dir )
                .start();
        try (
            InputStream in = process.getInputStream();
            InputStream err = process.getErrorStream();
        ){
            do {
                while (in.available() > 0) {
                    System.out.print( (char)in.read() );
                }
                while (err.available() > 0) {
                    System.out.print( (char)err.read() );
                }
                Thread.sleep( 100 );
                monitor.worked( 1 );
            } while (process.isAlive());
        }
        return process;
    }
    
}
