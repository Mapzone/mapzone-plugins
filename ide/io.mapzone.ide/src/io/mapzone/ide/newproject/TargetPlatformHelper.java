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
package io.mapzone.ide.newproject;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetPlatformService;

import io.mapzone.ide.IdePlugin;

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
        service = bundleContext.getService(ref);
    }
    
    
    public void list() {
        ITargetHandle[] handles = service.getTargets( new NullProgressMonitor() );
        for (ITargetHandle handle : handles) {
            System.out.println( "" + handle );
        }
    }
    
    
//    public void create() {
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
//    }
}
