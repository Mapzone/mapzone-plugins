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

import org.osgi.framework.BundleContext;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * 
 *
 * @author Falko Bräutigam
 */
@SuppressWarnings( "restriction" )
public class IdePlugin
        extends AbstractUIPlugin {
    
    public static final String      ID = "io.mapzone.ide";
    
    private static IdePlugin        instance;
    
    public static IdePlugin instance() {
        return instance;    
    }
    
    @Override
    public void start( BundleContext context ) throws Exception {
        super.start( context );
        instance = this;
    }

    
    @Override
    public void stop( BundleContext context ) throws Exception {
        instance = null;
        super.stop( context );
    }

    
    public File targetDir() {
        File result = new File( getStateLocation().toFile(), "targets" );
        result.mkdir();
        return result;
    }

    
    public static void logException( Throwable e ) {
        e.printStackTrace( System.err );
        PDEPlugin.logException( e );
    }
    
}
