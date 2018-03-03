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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import java.io.File;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.ui.UIUtils;

import io.mapzone.buildserver.BuildConfiguration.ScmConfiguration;
import io.mapzone.buildserver.BuildConfiguration.TargetPlatformConfiguration;
import io.mapzone.buildserver.scm.ScmRepository;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class Build {

    private static final Log log = LogFactory.getLog( Build.class );

    private BuildConfiguration      config;
    
    private File                    workspace;
    
    private CoreException           exception;

    
    public Build( BuildConfiguration config, File workspace ) {
        this.config = config;
        this.workspace = workspace;
    }


    public void run( IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Build", 20 );
        try {
            createWorkspace( UIUtils.submon( monitor, 1 ) );
            installTargetPlatform( UIUtils.submon( monitor, 4 ) );
            installBundleSources( UIUtils.submon( monitor, 4 ) );
            
            log.info( "DONE." );
        }
        catch (CoreException e) {
            log.warn( "", e );
            exception = e;
        }
        monitor.done();
    }

    
    public void cleanup() throws IOException {
        if (workspace.exists()) {
            FileUtils.deleteDirectory( workspace );
        }
    }


    protected void createWorkspace( IProgressMonitor monitor ) {
        monitor.beginTask( "Create workspace in: " + workspace.getAbsolutePath(), 1 );
        workspace.mkdir();
        monitor.done();
    }


    protected void installTargetPlatform( IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Create target platform config", 1 );

        JSONArray tps = new JSONArray();
        for (TargetPlatformConfiguration c : config.targetPlatform) {
            tps.put( new JSONObject().put( "type", c.type.get() ).put( "url", c.url.get() ) );
        }
        JSONObject json = new JSONObject().put( "targetPlatform", tps );
        
        FileUtils.write( new File( workspace, "config" ), json.toString( 2 ), "UTF-8" );
        monitor.done();
    }


    protected void installBundleSources( IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Install sources", IProgressMonitor.UNKNOWN );
        List<ScmRepository> scms = new ArrayList();
        for (ScmConfiguration c : config.scm) {
            ScmRepository scm = ScmRepository.forConfig( c );
            scm.update( UIUtils.submon( monitor, 1 ) );
            scms.add( scm );
        }
        
        File product = installBundleSource( config.productName.get(), scms, UIUtils.submon( monitor, 1 ) ).get();
        
        List<String> dependencies = config.type.get() == BuildConfiguration.Type.PRODUCT
                ? findProductDependencies( product )
                : findPluginDependencies( product );
                
        for (String dependency : dependencies) {
            installBundleSource( dependency, scms, UIUtils.submon( monitor, 1 ) );
        }
    }

    
    protected List<String> findPluginDependencies( File project ) throws IOException {
        log.warn( "!!!No plugin dependencies!!!" );
        return Collections.EMPTY_LIST;
    }

    
    protected List<String> findProductDependencies( File project ) throws IOException {
        File productFile = new File( project, project.getName() );
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
    
    
    protected Optional<File> installBundleSource( String name, List<ScmRepository> scms, IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Copy bundle source: " + name, IProgressMonitor.UNKNOWN );
        for (ScmRepository scm : scms) {
            if (scm.copyBundle( name, workspace )) {
                File project = new File( workspace, name );
                assert project.exists();
                return Optional.of( project );
            }
        }
        return Optional.empty();
    }

}
