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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class PrintProgressMonitor
        extends NullProgressMonitor {

    private static final Log log = LogFactory.getLog( PrintProgressMonitor.class );

    @Override
    public void beginTask( String name, int totalWork ) {
        log.info( "  " + name );
    }

    @Override
    public void subTask( String name ) {
        log.info( "    " + name );
    }

    @Override
    public void setTaskName( String name ) {
        log.info( "    " + name );
    }

    @Override
    public void done() {
        log.info( "  done." );
    }
    
}
