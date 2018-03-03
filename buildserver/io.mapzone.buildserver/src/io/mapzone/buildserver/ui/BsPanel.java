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

import org.polymap.rhei.batik.DefaultPanel;
import org.polymap.rhei.batik.toolkit.md.MdToolkit;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class BsPanel
        extends DefaultPanel {

    public static final int                 SIDE_PANEL_WIDTH = 400;
    
    public static final int                 SIDE_PANEL_WIDTH2 = 450;
    

    @Override
    public void init() {
        site().setSize( SIDE_PANEL_WIDTH, SIDE_PANEL_WIDTH2, SIDE_PANEL_WIDTH2 );
    }

    
    public MdToolkit tk() {
        return (MdToolkit)site().toolkit();
    }

}
