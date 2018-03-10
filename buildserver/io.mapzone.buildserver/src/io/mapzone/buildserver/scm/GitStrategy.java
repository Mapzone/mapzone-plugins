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

import java.util.Optional;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfig.ScmConfig;
import io.mapzone.buildserver.BuildConfig.ScmConfig.Type;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class GitStrategy
        extends ScmStrategy {

    private static final Log log = LogFactory.getLog( GitStrategy.class );


    @Override
    protected Type type() {
        return Type.GIT;
    }


    @Override
    protected void updateRepo( ScmConfig scm, IProgressMonitor monitor ) throws Exception {
        File cacheDir = BsPlugin.userCacheDir( config, scm.url.get() );

        if (cacheDir.exists() && cacheDir.list().length > 0) {
            // pull
            monitor.beginTask( "GIT pull: " + scm.url.get(), IProgressMonitor.UNKNOWN );
            String[] command = {"git", "pull", "--verbose"};
            process( command, cacheDir, monitor );
        }
        else {
            // clone
            monitor.beginTask( "GIT clone: " + scm.url.get(), IProgressMonitor.UNKNOWN );
            String[] command = {"git", "clone", scm.url.get(), cacheDir.getAbsolutePath(), "--verbose"};
            process( command, cacheDir.getParentFile(), monitor );
        }
        monitor.done();
    }


    @Override
    protected void switchBranch( ScmConfig scm, String branch, IProgressMonitor monitor ) throws Exception {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    protected boolean copyBundle( ScmConfig scm, String name, File dir ) throws Exception {
        File cacheDir = BsPlugin.userCacheDir( config, scm.url.get() );
        Optional<File> srcDir = findBundle( name, cacheDir );
        
        if (!srcDir.isPresent()) {
            return false;
        }
        else {
            FileUtils.copyDirectory( srcDir.get(), new File( dir, name ) );
            return true;
        }
    }
    
}
