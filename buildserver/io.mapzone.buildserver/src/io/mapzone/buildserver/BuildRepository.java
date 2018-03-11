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
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.core.CorePlugin;
import org.polymap.core.runtime.session.SessionSingleton;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.locking.OptimisticLocking;
import org.polymap.model2.store.recordstore.RecordStoreAdapter;
import org.polymap.recordstore.lucene.LuceneRecordStore;

import io.mapzone.buildserver.BuildConfig.ScmConfig;
import io.mapzone.buildserver.BuildConfig.TargetPlatformConfig;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildRepository {

    private static final Log log = LogFactory.getLog( BuildRepository.class );
    
    private static BuildRepository  instance;
    
    static void init() throws IOException {
        File storeDir = new File( CorePlugin.getDataLocation( BsPlugin.instance() ), "store" );
        instance = new BuildRepository( storeDir );
        try (
            UnitOfWork uow = instance.newUnitOfWork();
        ){
            if (uow.query( BuildConfig.class ).execute().size() == 0) {
                instance.createTestConfigurations( uow );
                uow.commit();
            }
        }
    }
    
    public static BuildRepository instance() {
        return instance;
    }
    
    public static UnitOfWork session() {
        return SessionHolder.instance( SessionHolder.class ).uow;
    }
    
    static class SessionHolder
            extends SessionSingleton {
        UnitOfWork uow = BuildRepository.instance().newUnitOfWork();  
    }
    
    // instance *******************************************
    
    private EntityRepository    repo;
    
    
    /**
     * Constructs a repository in RAM, for testing.
     */
    public BuildRepository() throws IOException {
        init( new LuceneRecordStore() );
    }
    
    
    public BuildRepository( File dir ) throws IOException {
        init( new LuceneRecordStore( dir, false) );
    }
    
    
    protected void init( LuceneRecordStore store ) {
        repo = EntityRepository.newConfiguration()
                .entities.set( new Class[] {
                        BuildConfig.class, 
                        BuildResult.class } )
                .store.set( 
                        new OptimisticLocking( 
                        new RecordStoreAdapter( store ) ) )
                .create();
    }


    public UnitOfWork newUnitOfWork() {
        return repo.newUnitOfWork();
    }


    protected void createTestConfigurations( UnitOfWork uow ) {
        // core.plugin
        uow.createEntity( BuildConfig.class, "core.plugin", (BuildConfig proto) -> {
            BuildConfig.defaults().initialize( proto );
            proto.name.set( "org.polymap.core" );
            proto.productName.set( "org.polymap.core" );
            proto.userId.set( "Test" );
            proto.type.set( BuildConfig.Type.PLUGIN );
            proto.targetPlatform.createElement( (TargetPlatformConfig proto2) -> {
                proto2.type.set( TargetPlatformConfig.Type.ZIP_DOWNLOAD );
                proto2.url.set( "file:///home/falko/servers/polymap4-targetplatform.zip" );
                return proto2;
            });
            proto.scm.createElement( (ScmConfig proto2) -> {
                proto2.type.set( ScmConfig.Type.GIT );
                proto2.url.set( "https://github.com/Polymap4/polymap4-core.git" );
                return proto2;
            });
            return proto;
        });
        // atlas.plugin
        uow.createEntity( BuildConfig.class, "atlas.plugin", (BuildConfig proto) -> {
            BuildConfig.defaults().initialize( proto );
            proto.name.set( "io.mapzone.atlas_master" );
            proto.productName.set( "io.mapzone.atlas" );
            proto.type.set( BuildConfig.Type.PLUGIN );
            proto.userId.set( "Test" );
            proto.targetPlatform.createElement( (TargetPlatformConfig proto2) -> {
                proto2.type.set( TargetPlatformConfig.Type.ZIP_DOWNLOAD );
                proto2.url.set( "file:///home/falko/servers/polymap4-targetplatform.zip" );
                return proto2;
            });
            proto.scm.createElement( (ScmConfig proto2) -> {
                proto2.type.set( ScmConfig.Type.GIT );
                proto2.url.set( "git@github.com:Mapzone/mapzone-atlas-plugin.git" );
                return proto2;
            });
            return proto;
        });
        // arena.product
        uow.createEntity( BuildConfig.class, "arena.product", (BuildConfig proto) -> {
            BuildConfig.defaults().initialize( proto );
            proto.name.set( "io.mapzone.arena.product CA" );
            proto.productName.set( "io.mapzone.arena.product" );
            proto.userId.set( "Test" );
            proto.type.set( BuildConfig.Type.PRODUCT );
            proto.targetPlatform.createElement( (TargetPlatformConfig proto2) -> {
                proto2.type.set( TargetPlatformConfig.Type.ZIP_DOWNLOAD );
                proto2.url.set( "file:///home/falko/servers/polymap4-targetplatform.zip" );
                return proto2;
            });
            proto.scm.createElement( (ScmConfig proto2) -> {
                proto2.type.set( ScmConfig.Type.GIT );
                proto2.url.set( "https://github.com/Polymap4/polymap4-core.git" );
                return proto2;
            });
            proto.scm.createElement( (ScmConfig proto2) -> {
                proto2.type.set( ScmConfig.Type.GIT );
                proto2.url.set( "git@github.com:Polymap4/polymap4-rap.git" );
                return proto2;
            });
            proto.scm.createElement( (ScmConfig proto2) -> {
                proto2.type.set( ScmConfig.Type.GIT );
                proto2.url.set( "git@github.com:Polymap4/polymap4-rhei.git" );
                return proto2;
            });
            proto.scm.createElement( (ScmConfig proto2) -> {
                proto2.type.set( ScmConfig.Type.GIT );
                proto2.url.set( "git@github.com:Polymap4/polymap4-p4.git" );
                return proto2;
            });
            proto.scm.createElement( (ScmConfig proto2) -> {
                proto2.type.set( ScmConfig.Type.GIT );
                proto2.url.set( "git@github.com:Polymap4/polymap4-model.git" );
                return proto2;
            });
            proto.scm.createElement( (ScmConfig proto2) -> {
                proto2.type.set( ScmConfig.Type.GIT );
                proto2.url.set( "git@github.com:Mapzone/mapzone.git" );
                return proto2;
            });
            return proto; 
        });
    }
    
}
