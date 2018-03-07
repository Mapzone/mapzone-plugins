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

import org.osgi.framework.BundleContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.polymap.core.CorePlugin;

import org.polymap.rhei.batik.app.SvgImageRegistryHelper;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BsPlugin
        extends AbstractUIPlugin {

    private static final Log log = LogFactory.getLog( BsPlugin.class );

    public static final String  ID = "io.mapzone.buildserver";
    
    private static BsPlugin     instance;

    public static BsPlugin instance() {
        return instance;
    }

    /**
     * Shortcut for <code>instance().images</code>.
     */
    public static SvgImageRegistryHelper images() {
        return instance().images;
    }
    
    /**
     * The directory to store {@link BuildResult} files.
     */
    public static File exportDataDir() {
        return new File( CorePlugin.getDataLocation( instance() ), "exports" );
    }

    /**
     * The directory where runners and SCM cache are stored.
     * 
     * @param userId 
     * @param name 
     */
    public static File buildserverDir() {
        // FIXME
        return new File( "/home/falko/servers/buildserver" );
    }

    public static File createTempDir() {
        File tmp = new File( buildserverDir(), "tmp" );
        File result = new File( tmp, String.valueOf( System.currentTimeMillis() ) );
        result.mkdirs();
        result.deleteOnExit();
        return result;
    }
    
    /**
     * The cache directory for a given config and ...thing.
     * 
     * @param userId 
     * @param name 
     */
    public static File cacheDir( BuildConfig config, String name ) {
        File root = new File( buildserverDir(), "cache" );
        File userDir = new File( root, config.userId.get() );
        File result = new File( userDir, "@"+name.hashCode() );
        result.mkdirs();
        return result;
    }


    // instance *******************************************

    public SvgImageRegistryHelper   images = new SvgImageRegistryHelper( this );
    
    @Override
    public void start( BundleContext context ) throws Exception {
        instance = this;
        log.info( "Start" );

        // create repo
        BuildRepository.init();
    }

    
    @Override
    public void stop( BundleContext context ) throws Exception {
        log.info( "Stop" );
    }
    
}
