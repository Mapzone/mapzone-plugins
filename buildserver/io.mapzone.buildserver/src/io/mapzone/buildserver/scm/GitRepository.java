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

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
        this.dir = new File( CACHE_DIR, config.name.get() );
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


    protected void pull( IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "GIT pull: " + origin, IProgressMonitor.UNKNOWN );
        String[] command = {"git", "pull", "--verbose"};
        process( command, dir, monitor );
        monitor.done();
    }


    protected void cloneRepo( IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "GIT clone: " + origin, IProgressMonitor.UNKNOWN );
        dir.mkdirs();
        String[] command = {"git", "clone", origin, dir.getAbsolutePath(), "--verbose"};
        process( command, dir.getParentFile(), monitor );
        monitor.done();
    }


    @Override
    public void switchBranch( String branch, IProgressMonitor monitor ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public boolean copyBundle( String name, File destDir ) throws Exception {
        Optional<File> srcDir = findBundle( name, dir );
        if (!srcDir.isPresent()) {
            return false;
        }
        else {
            FileUtils.copyDirectory( srcDir.get(), new File( destDir, name ) );
            return true;
        }
    }
    

    protected Optional<File> findBundle( String name, @SuppressWarnings( "hiding" ) File dir ) {
        int startLevel = StringUtils.split( dir.getAbsolutePath(), "/" ).length;
        int maxLevel = startLevel + 2;
        Deque<File> deque = new LinkedList();
        deque.addLast( dir );
        while (!deque.isEmpty()) {
            File f = deque.removeFirst();
            //log.info( f.getAbsolutePath() );
            if (f.getName().equals( name )) {
                return Optional.of( f );
            }
            else {
                int level = StringUtils.split( f.getAbsolutePath(), "/" ).length;
                if (level < maxLevel) {
                    Arrays.stream( f.listFiles( child -> child.isDirectory() ) ).forEach( child -> deque.addLast( child ) );
                }
            }
        }
        return Optional.empty();
    }

    
//    protected Optional<File> findBundle( String name, @SuppressWarnings( "hiding" ) File dir ) {
//        for (File f: dir.listFiles()) {
//            if (f.getName().equals( name )) {
//                return Optional.of( f );
//            }
//            else if (f.isDirectory()) {
//                Optional<File> result = findBundle( name, f );
//                if (result.isPresent()) {
//                    return result;
//                }
//            }
//        }
//        return Optional.empty();
//    }
}
