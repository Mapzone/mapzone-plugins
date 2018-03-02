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
package io.mapzone.buildserver.ui;

import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.IPanelFilter;

import io.mapzone.buildserver.BsPlugin;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BsPanelFilter
        implements IPanelFilter {

    @Override
    public boolean apply( IPanel panel ) {
        return panel.getClass().getName().startsWith( BsPlugin.ID );
    }
    
}
