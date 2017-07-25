/* 
 * polymap.org
 * Copyright (C) 2017, the @authors. All rights reserved.
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
package io.mapzone.ide;

import java.io.File;
import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.FluentIterable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class TargetPlatformHelper {
    
    private static TargetPlatformHelper instance = new TargetPlatformHelper(); 
    
    public static TargetPlatformHelper instance() {
        return instance;
    }
    
    // instance *******************************************
    
    ITargetPlatformService      service;
    
    
    public TargetPlatformHelper() {
        BundleContext bundleContext = IdePlugin.instance().getBundle().getBundleContext();
        ServiceReference<ITargetPlatformService> ref = bundleContext.getServiceReference( ITargetPlatformService.class );
        service = bundleContext.getService( ref );
    }
    

    public FluentIterable<ITargetDefinition> list( IProgressMonitor monitor ) {
        monitor = monitor != null ? monitor : new NullProgressMonitor();
        return FluentIterable.from( service.getTargets( monitor ) )
                .transform( handle -> {
                    try {
                        return handle.getTargetDefinition();
                    }
                    catch (CoreException e) {
                        throw new RuntimeException( e );
                    }
                });
    }
    
    
    public ITargetDefinition active( IProgressMonitor monitor ) throws CoreException {
        return service.getWorkspaceTargetDefinition();    
    }
    
    
    /**
     * Gets the bundle contents (location) of the given target.
     *
     * @param target
     * @param clear True signals that the entire contents should be deleted before returning. 
     * @throws CoreException 
     */
    public File contents( ITargetDefinition target ) throws CoreException {
        ITargetLocation[] locations = target.getTargetLocations();
        assert locations != null && locations.length == 1 : "Should be exactly 1 target location.";
        return new File( locations[0].getLocation( true ) );
    }
    
    
    public void updateContents( ITargetDefinition target, File temp, IProgressMonitor monitor ) 
            throws CoreException, IOException {
        File contents = contents( target );
        FileUtils.cleanDirectory( contents );
        FileUtils.copyDirectory( temp, contents );
        
        target.resolve( monitor );
        LoadTargetDefinitionJob.load( target );
    }
    
    
    public ITargetDefinition create( String name, File bundleDir, boolean load, IProgressMonitor monitor ) throws CoreException {
        ITargetDefinition target = service.newTarget();
        target.setName( name );
        
        ITargetLocation[] locations = {service.newDirectoryLocation( bundleDir.getAbsolutePath() )};
        target.setTargetLocations( locations );
        service.saveTargetDefinition( target );

        if (load) {
            LoadTargetDefinitionJob.load( target );
        }
        return target;
        
//        ITargetDefinition target = service.getWorkspaceTargetHandle().getTargetDefinition();
//        IBundleContainer[] bundles = target.getBundleContainers();
//        String myDirectory = "C:\\directory";
//        boolean containsMyDirectory = false;
//
//        for (IBundleContainer bundle : bundles) {
//            if (bundle.toString().contains(myDirectory.toString())) {
//                containsMyDirectory = true;
//                break;
//            }
//        }
//
//        if (!containsMyDirectory) {
//            bundles = Arrays.copyOf(bundles, bundles.length + 1);
//            bundles[bundles.length - 1] = service.newDirectoryContainer(myDirectory.toString());
//            target.setBundleContainers(bundles);
//            service.saveTargetDefinition(target);
//            LoadTargetDefinitionJob.load(target);
//        }
    }

}
