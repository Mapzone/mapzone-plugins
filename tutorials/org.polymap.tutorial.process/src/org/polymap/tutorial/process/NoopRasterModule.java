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
package org.polymap.tutorial.process;

import org.geotools.coverage.grid.GridCoverage2D;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * A module processing raster data ({@link GridCoverage2D}). This module does nothing
 * useful, it just waits for 10 seconds.
 *
 * @author Falko Bräutigam
 */
public class NoopRasterModule
        extends TutorialModule {

    private static final Log log = LogFactory.getLog( NoopRasterModule.class );

    public GridCoverage2D       input;
    
    public int                  countInput = 5;
    
    public int                  numOutput = -1;
    
    
    @Override
    public void execute( IProgressMonitor monitor ) throws OperationCanceledException, Exception {
        monitor.beginTask( "Counting", countInput );
        for (int i=0; i<countInput; i++) {
            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            Thread.sleep( 1000 );
            monitor.worked( 1 );
            log.info( "." );
        }
        monitor.done();
    }
    
}
