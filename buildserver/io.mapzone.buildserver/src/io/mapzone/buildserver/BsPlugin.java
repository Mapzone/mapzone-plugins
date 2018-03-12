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

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.polymap.core.CorePlugin;

import org.polymap.rhei.batik.app.SvgImageRegistryHelper;

import io.mapzone.buildserver.ui.OAuthSetup;

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
        return new File( System.getProperty( ID, System.getProperty( "user.home" ) + "/buildserver" ) );
    }

    public static File tmpDir() {
        File tmp = new File( buildserverDir(), "tmp" );
        return tmp;
    }
    
    public static File createTempDir() {
        File result = new File( tmpDir(), String.valueOf( System.currentTimeMillis() ) );
        result.mkdirs();
        result.deleteOnExit();
        return result;
    }
    
    /**
     * The cache directory for a given config.
     */
    public static File cacheDir( BuildConfig config, String name ) {
        File root = new File( buildserverDir(), "cache" );
        File result = new File( root, "@"+name.hashCode() );
        result.mkdirs();
        return result;
    }

    /**
     * The cache directory for a given config.
     */
    public static File userCacheDir( BuildConfig config, String name ) {
        File root = new File( buildserverDir(), "cache" );
        File userDir = new File( root, config.userId.get() );
        File result = new File( userDir, "@"+name.hashCode() );
        result.mkdirs();
        return result;
    }


    // instance *******************************************

    public SvgImageRegistryHelper   images = new SvgImageRegistryHelper( this );

    public OAuthSetup               oauth;
    
    
    @Override
    public void start( BundleContext context ) throws Exception {
        instance = this;
        log.info( "Start" );

        File f = new File( CorePlugin.getDataLocation( instance() ).getParentFile().getParentFile(), "oauth.conf" );
        oauth = OAuthSetup.readJsonConfig( FileUtils.readFileToString( f ) );
        
        if (tmpDir().exists()) {
            log.info( "Cleaning up tmp: " + tmpDir().getAbsolutePath() );
            FileUtils.cleanDirectory( tmpDir() );
        }
        // create repo
        BuildRepository.init();
    }

    
    @Override
    public void stop( BundleContext context ) throws Exception {
        log.info( "Stop" );
    }
    
}
