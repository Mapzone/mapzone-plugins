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

import java.io.File;
import org.apache.commons.io.FileUtils;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class InitWorkspaceStrategy
        extends BuildStrategy {

    @Override
    public boolean init( BuildConfig config ) {
        return true;
    }

    @Override
    public void preBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {
        File dir = BsPlugin.createTempDir();
        monitor.beginTask( "Build directory: " + dir.getName(), 1 );
        context.workspace.set( new File( dir, "workspace" ) );
        context.workspace.get().mkdir();
        context.export.set( new File( dir, "export" ) );
        context.export.get().mkdir();
        monitor.done();
    }

    @Override
    public void postBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {
        File dir = context.workspace.map( workspace -> workspace.getParentFile() ).orElse( null );
        if (dir != null && dir.exists()) {
            FileUtils.deleteDirectory( dir );
        }
    }
}
