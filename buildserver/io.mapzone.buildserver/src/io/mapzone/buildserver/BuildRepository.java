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

import org.polymap.core.runtime.session.SessionSingleton;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.locking.OptimisticLocking;
import org.polymap.model2.store.recordstore.RecordStoreAdapter;
import org.polymap.recordstore.lucene.LuceneRecordStore;

import io.mapzone.buildserver.BuildConfiguration.ScmConfiguration;
import io.mapzone.buildserver.BuildConfiguration.TargetPlatformConfiguration;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildRepository {

    private static final Log log = LogFactory.getLog( BuildRepository.class );
    
    private static BuildRepository  instance;
    
    static void init() throws IOException {
        instance = new BuildRepository();
        try (
            UnitOfWork uow = instance.newUnitOfWork();
        ){
            if (uow.query( BuildConfiguration.class ).execute().size() == 0) {
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
                        BuildConfiguration.class, 
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
        uow.createEntity( BuildConfiguration.class, "core.plugin", (BuildConfiguration proto) -> {
            proto.name.set( "org.polymap.core" );
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
        // atlas.plugin
        uow.createEntity( BuildConfiguration.class, "atlas.plugin", (BuildConfiguration proto) -> {
            proto.name.set( "io.mapzone.atlas_master" );
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
        // arena.product
        uow.createEntity( BuildConfiguration.class, "arena.product", (BuildConfiguration proto) -> {
            proto.name.set( "io.mapzone.arena.product CA" );
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
    }
    
}
