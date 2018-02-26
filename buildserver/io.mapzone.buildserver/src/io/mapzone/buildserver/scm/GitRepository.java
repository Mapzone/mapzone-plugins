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

import java.util.Collection;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.FluentIterable;

import org.eclipse.core.runtime.IProgressMonitor;

import io.mapzone.buildserver.BuildConfiguration.ScmConfiguration;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class GitRepository
        extends ScmRepository {

    private static final Log log = LogFactory.getLog( GitRepository.class );
    
    private File                dir;
    
    private String              origin;

    
    @Override
    public ScmRepository init( ScmConfiguration config ) {
        this.dir = new File( BASE_PATH, config.name.get() );
        this.origin = config.url.get();
        return this;
    }


    public File directory() {
        return dir;
    }


    @Override
    public void update( IProgressMonitor monitor ) throws Exception {
        if (dir.exists()) {
            pull( monitor );
        }
        else {
            cloneRepo( monitor );
        }
    }


    protected void pull( IProgressMonitor monitor ) {
        monitor.beginTask( "GIT pull: " + origin, IProgressMonitor.UNKNOWN );
        log.warn( "pull(): not yet implemented." );
        monitor.done();
    }


    protected void cloneRepo( IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "GIT clone: " + origin, IProgressMonitor.UNKNOWN );
        dir.mkdirs();
        Process process = new ProcessBuilder( "git", "clone", origin, dir.getAbsolutePath() )
                .directory( dir.getParentFile() )
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
        monitor.done();
    }


    @Override
    public void switchBranch( String branch, IProgressMonitor monitor ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public boolean copyBundle( String name, File destDir ) throws Exception {
        Collection<File> srcDir = FileUtils.listFilesAndDirs( dir, FalseFileFilter.INSTANCE, new AbstractFileFilter() {
            @Override public boolean accept( File file ) {
                return file.getName().equals( name );
            }
        });
        assert srcDir.size() <= 2;
        if (srcDir.isEmpty()) {
            return false;
        }
        else {
            File src = FluentIterable.from( srcDir ).last().get();
            File dest = new File( destDir, name );
            FileUtils.copyDirectory( src, dest );
            return true;
        }
    }
    
}
