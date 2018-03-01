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
package io.mapzone.buildserver.tp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.target.ITargetLocation;

import io.mapzone.buildserver.BuildConfiguration.TargetPlatformConfiguration;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class LocationHandler {

    private static final Log log = LogFactory.getLog( LocationHandler.class );

    public abstract LocationHandler init( TargetPlatformHelper helper, TargetPlatformConfiguration config );
    
    public abstract ITargetLocation create( IProgressMonitor monitor );
    
}
