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

import java.util.Objects;

import org.polymap.core.runtime.event.EventManager;

import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.Computed;
import org.polymap.model2.ComputedBidiManyAssocation;
import org.polymap.model2.Defaults;
import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;
import org.polymap.model2.runtime.Lifecycle;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildConfig
        extends Entity 
        implements Lifecycle {

    public static BuildConfig    TYPE;
    
    public static final ValueInitializer<BuildConfig> defaults() {
        return (BuildConfig proto) -> {
            proto.type.set( Type.PRODUCT );
            proto.userId.set( "Test" );  // FIXME
            return proto;
        };
    }
    
    public enum Type {
        PRODUCT, PLUGIN
    }

    @Nullable
    public Property<String>             name;
    
    /** The bundle-id of the product to build. */
    @Nullable
    public Property<String>             productName;
    
    public Property<Type>               type;
    
    public Property<String>             userId;
    
    @Defaults
    public CollectionProperty<TargetPlatformConfig> targetPlatform;
    
    @Defaults
    public CollectionProperty<ScmConfig> scm;
    
    @Computed( ComputedBidiManyAssocation.class )
    public ManyAssociation<BuildResult> buildResults;
    
    
    public UnitOfWork belongsTo() {
        return context.getUnitOfWork();
    }
    
    @Override
    public void onLifecycleChange( State state ) {
        if (state == State.AFTER_COMMIT) {
            EventManager.instance().publish( new BuildConfigCommittedEvent( BuildConfig.this ) );
        }
    }


    /**
     * Configuration of a SCM system.
     */
    public static class ScmConfig
            extends Composite {

        public static final ValueInitializer<ScmConfig> defaults() {
            return (ScmConfig proto) -> {
                proto.type.set( Type.GIT );
                proto.url.set( "" );
                proto.branch.set( "master" );
                return proto;
            };
        }
        
        public enum Type {
            DIRECTORY, GIT
        }

        public Property<Type>       type;
        
        public Property<String>     url;
        
        @Nullable
        public Property<String>     branch;

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) {
                return true;
            }
            else if (obj instanceof TargetPlatformConfig) {
                ScmConfig other = (ScmConfig)obj;
                return url.get().equals( other.url.get() ) && type.get().equals( other.type.get() );
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash( url.get(), type.get() );
        }
    }

    
    /**
     * Configuration of a target platform location. 
     */
    public static class TargetPlatformConfig
            extends Composite {

        public static final ValueInitializer<TargetPlatformConfig> defaults() {
            return (TargetPlatformConfig proto) -> {
                proto.type.set( Type.ZIP_DOWNLOAD );
                proto.url.set( "" );
                return proto;
            };
        }
        
        public enum Type {
            DIRECTORY, ZIP_DOWNLOAD
        }
        
        public Property<String>     url;
        
        public Property<Type>       type;

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) {
                return true;
            }
            else if (obj instanceof TargetPlatformConfig) {
                TargetPlatformConfig other = (TargetPlatformConfig)obj;
                return url.get().equals( other.url.get() ) && type.get().equals( other.type.get() );
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash( url.get(), type.get() );
        }
    }

}
