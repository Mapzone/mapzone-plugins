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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.polymap.core.ui.UIUtils;

import io.mapzone.buildserver.BuildConfiguration.ScmConfiguration;
import io.mapzone.buildserver.BuildConfiguration.TargetPlatformConfiguration;
import io.mapzone.buildserver.scm.ScmRepository;
import io.mapzone.buildserver.targetplatform.LocationHandler;
import io.mapzone.buildserver.targetplatform.TargetPlatformHelper;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class Build {

    private static final Log log = LogFactory.getLog( Build.class );

    private BuildConfiguration      config;
    
    private IWorkspace              workspace;

    private CoreException           exception;
    
    
    public Build( BuildConfiguration config ) {
        this.config = config;
    }


    public void run( IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Build", 20 );
        try {
            createWorkspace( UIUtils.submon( monitor, 1 ) );
            enableAutoBuild( false, UIUtils.submon( monitor, 1 ) );
            installTargetPlatform( UIUtils.submon( monitor, 4 ) );
            installBundleSources( UIUtils.submon( monitor, 4 ) );
            
            workspace.build( IncrementalProjectBuilder.FULL_BUILD, UIUtils.submon( monitor, 10 ) );
            reportProblems( UIUtils.submon( monitor, 1 ) );
        }
        catch (CoreException e) {
            log.warn( "", e );
            exception = e;
        }
        monitor.done();
    }

    
    protected void createWorkspace( IProgressMonitor monitor ) {
        workspace = ResourcesPlugin.getWorkspace();
        File root = workspace.getRoot().getRawLocation().toFile();
        monitor.beginTask( "Create workspace in: " + root, 1 );
        if (root.list().length > 0) {
            log.warn( "Workspace is not empty." );
        }
        monitor.done();
    }


    protected void enableAutoBuild( boolean enable, IProgressMonitor monitor ) throws CoreException {
        monitor.beginTask( (enable?"Enable":"Disable") + " auto build", 1 );
        final IWorkspaceDescription description = workspace.getDescription();
        description.setAutoBuilding( enable );
        workspace.setDescription( description );
        monitor.done();
    }


    protected void installTargetPlatform( IProgressMonitor monitor ) throws Exception {
        TargetPlatformHelper helper = new TargetPlatformHelper();
        assert helper.list( monitor ).isEmpty();
        
        List<LocationHandler> locations = new ArrayList();
        for (TargetPlatformConfiguration c : config.targetPlatform) {
            locations.add( helper.newLocation( c ) );
        }
        
        helper.create( "Default Target", locations, true, monitor );
        //helper.list( monitor ).forEach( target -> log.info( "Target: " + target ) );
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
        
        IProject product = installBundleSource( config.productName.get(), scms, UIUtils.submon( monitor, 1 ) );
        
        // XXX find dependencies
    }

    
    protected IProject installBundleSource( String name, List<ScmRepository> scms, IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Copy bundle source: " + name, IProgressMonitor.UNKNOWN );
        for (ScmRepository scm : scms) {
            if (scm.copyBundle( name, workspace.getRoot().getRawLocation().toFile() )) {
                break;
            }
        }
        IProject project = workspace.getRoot().getProject( name );
        project.create( UIUtils.submon( monitor, 1 ) );
        project.open( UIUtils.submon( monitor, 1 ) );
        project.refreshLocal( IResource.DEPTH_INFINITE, UIUtils.submon( monitor, 1 ) );
        return project;
    }

    
    protected void reportProblems( IProgressMonitor monitor ) throws CoreException {
        monitor.beginTask( "Report", IProgressMonitor.UNKNOWN );
        IMarker[] markers = workspace.getRoot().findMarkers( null, true, IResource.DEPTH_INFINITE );
        for (int i=0; i<10 && i<markers.length; i++) {
            monitor.subTask( "[" + severity(markers[i]) + "] " 
                    + markers[i].getResource().getName() 
                    + ":" + markers[i].getAttribute( IMarker.LINE_NUMBER, -1 )
                    + ": " + markers[i].getAttribute( IMarker.MESSAGE, "" ) );
        }
        if (markers.length > 3) {
            monitor.subTask( "... " + markers.length + " problems" );
        }
        monitor.subTask( "" );
        monitor.done();
    }
    
    protected String severity( IMarker marker ) throws CoreException {
        int severity = (Integer)marker.getAttribute( IMarker.SEVERITY );
        switch (severity) {
            case IMarker.SEVERITY_ERROR: return "ERROR";
            case IMarker.SEVERITY_WARNING: return "WARN";
            case IMarker.SEVERITY_INFO: return "INFO";
            default: throw new RuntimeException( "Unknown severity: " + severity );
        }
    }
}
