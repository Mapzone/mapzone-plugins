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
package io.mapzone.ide.build;

import java.util.Optional;

import java.io.File;
import java.lang.reflect.Method;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.apache.commons.io.FileUtils;
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
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.ProductExportOperation;

import io.mapzone.ide.IdePlugin;
import io.mapzone.ide.util.UIUtils;

/**
 * This application entry point starts the Eclipse instance just for
 * building/exporting. It is controlled by the Mapzone Buildserver.
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
@SuppressWarnings( "restriction" )
public class BuildRunner
        implements IApplication {

    private static final Log log = LogFactory.getLog( BuildRunner.class );

    private IWorkspace          workspace;
    

    @Override
    public Object start( IApplicationContext context ) throws Exception {
        log.info( "START" );

        long start = System.currentTimeMillis();
        workspace = ResourcesPlugin.getWorkspace();
        log.info( "Workspace: " + workspace.getRoot().getRawLocation() );

        // args
        String[] args = (String[])context.getArguments().get( IApplicationContext.APPLICATION_ARGS );
        String productName = argument( args, "-export", (String)null )
                .orElseThrow( () -> new RuntimeException( "Missing argument: -export <product>" ) );
        String exportDir = argument( args, "-exportDir", (String)null )
                .orElseThrow( () -> new RuntimeException( "Missing argument: -exportDir <directory>" ) );
        
        // refresh
        PrintProgressMonitor monitor = new PrintProgressMonitor();
        monitor.beginTask( "Build", 27 );
        enableAutoBuild( false, submon( monitor, 1 ) );
        refreshWorkspace( submon( monitor, 2 ) );
        installTargetPlatform( submon( monitor, 2 ) );

        // find product
        IProject project = workspace.getRoot().getProject( productName );        
        if (project == null || !project.exists()) {
            throw new RuntimeException( "Project does not exist: " + productName );
        }

        // build / export
//        build( submon( monitor, 10 ) );
//        reportProblems( monitor );
        export( project, new File( exportDir ), submon( monitor, 10 ) );

        workspace.save( true, submon( monitor, 1 ) );
        System.out.println( "Time: " + (System.currentTimeMillis()-start)/1000 + "s"  );
        return IApplication.EXIT_OK;
    }


    @Override
    public void stop() {
    }

    
    protected <T> Optional<T> argument( String[] args, String name, T defaultValue ) {
        for (int i=0; i<args.length; i++) {
            if (args[i].equals( name )) {
                return i+1 < args.length 
                        ? Optional.ofNullable( (T)args[i+1] )
                        : Optional.empty();
            }
        }
        return Optional.ofNullable( defaultValue );
    }
    

    protected void enableAutoBuild( boolean enable, IProgressMonitor monitor ) throws CoreException {
        monitor.beginTask( (enable?"Enable":"Disable") + " auto build", 1 );
        final IWorkspaceDescription description = workspace.getDescription();
        description.setAutoBuilding( enable );
        workspace.setDescription( description );
        monitor.done();
    }


    protected void installTargetPlatform( IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Target platform", 2 );
        BundleContext bundleContext = IdePlugin.instance().getBundle().getBundleContext();
        ServiceReference<ITargetPlatformService> ref = bundleContext.getServiceReference( ITargetPlatformService.class );
        ITargetPlatformService service = bundleContext.getService( ref );

        ITargetHandle[] targets = service.getTargets( submon( monitor, 1 ) );
        ITargetHandle buildRunnerTarget = null;
        for (ITargetHandle t : targets) {
            if (t.getTargetDefinition().getName().equals( "BuildRunner.target" ) ) {
                buildRunnerTarget = t; break;
            }
        }
        if (buildRunnerTarget != null) {
            monitor.subTask( "Target: " + buildRunnerTarget.getTargetDefinition().getName() );
        }
        else {
            monitor.subTask( "Create" );
            ITargetDefinition target = service.newTarget();
            target.setName( "BuildRunner.target" );

            File configFile = new File( workspace.getRoot().getRawLocation().toFile(), "config" );
            JSONObject config = new JSONObject( FileUtils.readFileToString( configFile, "UTF-8" ) );
            JSONArray entries = config.getJSONArray( "targetPlatform" );
            ITargetLocation[] locations = new ITargetLocation[entries.length()];
            for (int i=0; i<entries.length(); i++) {
                JSONObject entry = entries.getJSONObject( i );
                switch (entry.getString( "type" )) {
                    case "DIRECTORY": {
                        locations[i] = service.newDirectoryLocation( entry.getString( "url" ) ); break;
                    }
                    default: {
                        throw new RuntimeException( "Unknown target platform entry type: " + entry.getString( "type" ) );
                    }
                }
            }
            target.setTargetLocations( locations );
            service.saveTargetDefinition( target );
            monitor.worked( 1 );

            // load
            monitor.subTask( "Make active" );
            LoadTargetDefinitionJob job = new LoadTargetDefinitionJob( target );
            job.run( submon( monitor, 1 ) );
//            job.setUser( false );
//            job.setSystem( true );
//            job.schedule();
//            job.join();
//            monitor.worked( 1 );
        }
        monitor.done();
    }
    
    
    protected void refreshWorkspace( IProgressMonitor monitor ) throws CoreException {
        monitor.beginTask( "Refresh workspace", IProgressMonitor.UNKNOWN );
        File root = workspace.getRoot().getRawLocation().toFile();
        for (File f : root.listFiles()) {
            if (f.isDirectory() && !f.getName().startsWith( "." )) {
                IProject project = workspace.getRoot().getProject( f.getName() );
                if (!project.exists()) {
                    project.create( submon( monitor, 1 ) );
                }
                project.open( submon( monitor, 1 ) );
            }
        }
        monitor.done();
    }


    protected void export( IProject project, File exportDir, IProgressMonitor monitor ) throws Exception, InterruptedException {
        FeatureExportInfo info = ExportHelper.standardExportInfo( project );
        info.destinationDirectory = exportDir.getAbsolutePath();
        info.useWorkspaceCompiledClasses = false;
        info.toDirectory = false;
        info.zipFileName = new File( exportDir, project.getName() + ".zip" ).getAbsolutePath(); 
        ProductExportOperation job = ExportHelper.createProductExportJob( project, info );
        
        // we want it to use our monitor but run() is protected; sub-class does not work
        Method runMethod = ProductExportOperation.class.getDeclaredMethod( "run", new Class[] {IProgressMonitor.class} );
        runMethod.setAccessible( true );
        runMethod.invoke( job, new Object[] {monitor} );
        
//        job.schedule();
//        job.join();
    }


    protected void build( IProgressMonitor monitor ) throws CoreException {
        workspace.build( IncrementalProjectBuilder.FULL_BUILD, monitor );
        refreshWorkspace( monitor );
//        workspace.build( IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor );        
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
        Integer severity = (Integer)marker.getAttribute( IMarker.SEVERITY );
        if (severity == null) {
            return "???";
        }
        switch (severity) {
            case IMarker.SEVERITY_ERROR: return "ERROR";
            case IMarker.SEVERITY_WARNING: return "WARN";
            case IMarker.SEVERITY_INFO: return "INFO";
            default: throw new RuntimeException( "Unknown severity: " + severity );
        }
    }

    
    protected IProgressMonitor submon( IProgressMonitor monitor, int ticks ) {
        return UIUtils.submon( monitor, ticks );
    }
    
}
