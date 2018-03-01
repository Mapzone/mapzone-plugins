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

import org.osgi.framework.BundleContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Throwables;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.polymap.model2.runtime.UnitOfWork;

import io.mapzone.buildserver.BuildConfiguration.ScmConfiguration;
import io.mapzone.buildserver.BuildConfiguration.TargetPlatformConfiguration;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BsPlugin
        extends AbstractUIPlugin {

    private static final Log log = LogFactory.getLog( BsPlugin.class );
    
    private static BsPlugin     instance;

    public static BsPlugin instance() {
        return instance;
    }

    // instance *******************************************

    @Override
    public void start( BundleContext context ) throws Exception {
        instance = this;
        log.info( "Start" );

        //FileUtils.cleanDirectory( ResourcesPlugin.getWorkspace().getRoot().getRawLocation().toFile() );
        
        // create repo
        BuildRepository repo = new BuildRepository();
        createArenaProductConfiguration( repo );
        createAtlasPluginConfiguration( repo );
        createCorePluginConfiguration( repo );

        // build
        Job buildJob = new Job( "Build" ) {
            @Override
            protected IStatus run( IProgressMonitor monitor ) {
                try (
                    UnitOfWork uow = repo.newUnitOfWork();
                ){
                    PrintProgressMonitor printMonitor = new PrintProgressMonitor();
                    BuildConfiguration config = uow.entity( BuildConfiguration.class, "arena.product" );
                    //BuildConfiguration config = uow.entity( BuildConfiguration.class, "core.plugin" );
                    Build build = new Build( config );
                    build.run( printMonitor );
                    printMonitor.done();
                    return Status.OK_STATUS;
                }
                catch (Exception e) {
                    log.warn( "", e );
                    throw Throwables.propagate( e );
                }
            }
        };
        buildJob.setSystem( true );
        buildJob.schedule( 2000 );
    }

    
    @Override
    public void stop( BundleContext context ) throws Exception {
        log.info( "Stop" );
    }

    
    protected void createCorePluginConfiguration( BuildRepository repo ) {
        try (
            UnitOfWork uow = repo.newUnitOfWork();
        ){
            uow.createEntity( BuildConfiguration.class, "core.plugin", (BuildConfiguration proto) -> {
                proto.productName.set( "org.polymap.core" );
                proto.type.set( BuildConfiguration.Type.PLUGIN );
                proto.targetPlatform.createElement( (TargetPlatformConfiguration proto2) -> {
                    proto2.type.set( TargetPlatformConfiguration.Type.DIRECTORY );
                    proto2.url.set( "/home/falko/servers/polymap4_target/plugins/" );
                    return proto2;
                });
                proto.scm.createElement( (ScmConfiguration proto2) -> {
                    proto2.type.set( ScmConfiguration.Type.GIT );
                    proto2.name.set( "polymap4-core" );
                    proto2.url.set( "https://github.com/Polymap4/polymap4-core.git" );
                    return proto2;
                });
                return proto;
            });
            uow.commit();
        }
    }
    
    
    protected void createAtlasPluginConfiguration( BuildRepository repo ) {
        try (
            UnitOfWork uow = repo.newUnitOfWork();
        ){
            uow.createEntity( BuildConfiguration.class, "atlas.plugin", (BuildConfiguration proto) -> {
                proto.productName.set( "io.mapzone.atlas" );
                proto.type.set( BuildConfiguration.Type.PLUGIN );
                proto.targetPlatform.createElement( (TargetPlatformConfiguration proto2) -> {
                    proto2.type.set( TargetPlatformConfiguration.Type.DIRECTORY );
                    proto2.url.set( "/home/falko/servers/polymap4-targetplatform/polymap4_target/plugins/" );
                    return proto2;
                });
                proto.scm.createElement( (ScmConfiguration proto2) -> {
                    proto2.type.set( ScmConfiguration.Type.GIT );
                    proto2.name.set( "mapzone-atlas-plugin" );
                    proto2.url.set( "git@github.com:Mapzone/mapzone-atlas-plugin.git" );
                    return proto2;
                });
                return proto;
            });
            uow.commit();
        }
    }
    
    
    protected void createArenaProductConfiguration( BuildRepository repo ) {
        try (
            UnitOfWork uow = repo.newUnitOfWork();
        ){
            uow.createEntity( BuildConfiguration.class, "arena.product", (BuildConfiguration proto) -> {
                proto.productName.set( "io.mapzone.arena.product" );
                proto.type.set( BuildConfiguration.Type.PRODUCT );
                proto.targetPlatform.createElement( (TargetPlatformConfiguration proto2) -> {
                    proto2.type.set( TargetPlatformConfiguration.Type.DIRECTORY );
                    proto2.url.set( "/home/falko/servers/polymap4-targetplatform/polymap4_target/plugins/" );
                    return proto2;
                });
                proto.scm.createElement( (ScmConfiguration proto2) -> {
                    proto2.type.set( ScmConfiguration.Type.GIT );
                    proto2.name.set( "polymap4-core" );
                    proto2.url.set( "https://github.com/Polymap4/polymap4-core.git" );
                    return proto2;
                });
                proto.scm.createElement( (ScmConfiguration proto2) -> {
                    proto2.type.set( ScmConfiguration.Type.GIT );
                    proto2.name.set( "polymap4-rap" );
                    proto2.url.set( "git@github.com:Polymap4/polymap4-rap.git" );
                    return proto2;
                });
                proto.scm.createElement( (ScmConfiguration proto2) -> {
                    proto2.type.set( ScmConfiguration.Type.GIT );
                    proto2.name.set( "polymap4-rhei" );
                    proto2.url.set( "git@github.com:Polymap4/polymap4-rhei.git" );
                    return proto2;
                });
                proto.scm.createElement( (ScmConfiguration proto2) -> {
                    proto2.type.set( ScmConfiguration.Type.GIT );
                    proto2.name.set( "polymap4-p4" );
                    proto2.url.set( "git@github.com:Polymap4/polymap4-p4.git" );
                    return proto2;
                });
                proto.scm.createElement( (ScmConfiguration proto2) -> {
                    proto2.type.set( ScmConfiguration.Type.GIT );
                    proto2.name.set( "polymap4-model" );
                    proto2.url.set( "git@github.com:Polymap4/polymap4-model.git" );
                    return proto2;
                });
                proto.scm.createElement( (ScmConfiguration proto2) -> {
                    proto2.type.set( ScmConfiguration.Type.GIT );
                    proto2.name.set( "mapzone" );
                    proto2.url.set( "git@github.com:Mapzone/mapzone.git" );
                    return proto2;
                });
                return proto; 
            });
            uow.commit();
        }
    }
    
}
