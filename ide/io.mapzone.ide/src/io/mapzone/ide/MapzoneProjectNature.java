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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class MapzoneProjectNature
        implements IProjectNature {
    
    public static final String  ID = IdePlugin.ID + ".MapzoneNature";

    private IProject            project;

    
    @Override
    public void configure() throws CoreException {
    }


    @Override
    public void deconfigure() throws CoreException {
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public IProject getProject() {
        return project;
    }


    @Override
    public void setProject( IProject project ) {
        this.project = project;
    }
}
