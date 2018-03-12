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
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.ui.UIUtils;

import io.mapzone.buildserver.BuildConfig;
import io.mapzone.buildserver.BuildConfig.ScmConfig;
import io.mapzone.buildserver.BuildStrategy;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class ScmStrategy
        extends BuildStrategy {

    private static final Log log = LogFactory.getLog( ScmStrategy.class );
    
    protected BuildConfig           config;

    protected File                  workspace;
    
    protected List<ScmConfig>       scms;
    
    
    @Override
    @SuppressWarnings( "hiding" )
    public boolean init( BuildConfig config ) {
        this.config = config;
        this.scms = config.scm.stream().filter( scm -> scm.type.get() == type() ).collect( Collectors.toList() );
        return !scms.isEmpty();
    }

    
    protected abstract ScmConfig.Type type();
    
    /**
     * Bring local store of the repo in sync with remote repository. 
     * @throws Exception 
     */
    protected abstract void updateRepo( ScmConfig scm, IProgressMonitor monitor ) throws Exception;
    
    protected abstract void switchBranch( ScmConfig scm, String branch, IProgressMonitor monitor ) throws Exception;

    protected abstract boolean copyBundle( ScmConfig scm, String name, File dir ) throws Exception;
    
    
    @Override
    public void preBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Install sources from " + type(), IProgressMonitor.UNKNOWN );
        for (ScmConfig scm : config.scm) {
            updateRepo( scm, UIUtils.submon( monitor, 1 ) );
        }
        
        workspace = context.workspace.get();
        File product = installBundleSource( config.productName.get(), UIUtils.submon( monitor, 1 ) ).get();
        List<String> dependencies = config.type.get() == BuildConfig.Type.PRODUCT
                ? findProductDependencies( product )
                : findPluginDependencies( product );
                
        for (String dependency : dependencies) {
            installBundleSource( dependency, UIUtils.submon( monitor, 1 ) );
        }
    }

    
    protected List<String> findPluginDependencies( File project ) throws IOException {
        log.warn( "!!!No plugin dependencies!!!" );
        return Collections.EMPTY_LIST;
    }

    
    protected List<String> findProductDependencies( File project ) throws IOException {
        String filename = StringUtils.appendIfMissing( project.getName(), ".product" );
        File productFile = new File( project, filename ); 

        if (!productFile.exists()) {
            throw new IOException( "No product file found: " + productFile.getName() );
        }
        
        AtomicBoolean plugins = new AtomicBoolean( false );
        return FileUtils.readLines( productFile ).stream()
                .peek( line -> {
                    if (line.contains( "<plugins>" )) {
                        plugins.set( true );
                    }
                    else if (line.contains( "</plugins>" )) {
                        plugins.set( false );
                    }
                })
                .filter( line -> plugins.get() && line.contains( "<plugin id=" ) )
                .map( line -> StringUtils.split( line, "\"" )[1] )
                .collect( Collectors.toList() );
    }
    
    
    protected Optional<File> installBundleSource( String name, IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Copy bundle source: " + name, IProgressMonitor.UNKNOWN );
        for (ScmConfig scm : scms) {
            if (copyBundle( scm, name, workspace )) {
                File project = new File( workspace, name );
                assert project.exists();
                return Optional.of( project );
            }
        }
        return Optional.empty();
    }

    
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


    protected Optional<File> findBundle( String name, File dir ) {
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


}
