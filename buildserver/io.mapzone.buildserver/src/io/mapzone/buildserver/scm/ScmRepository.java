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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;

import io.mapzone.buildserver.BuildConfiguration.ScmConfiguration;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class ScmRepository {

    private static final Log log = LogFactory.getLog( ScmRepository.class );
    
    public static final String  BASE_PATH = "/home/falko/servers/buildserver/scm";
    
    public static ScmRepository forConfig( ScmConfiguration config ) {
        if (config.type.get() == ScmConfiguration.TYPE.Git) {
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
}
