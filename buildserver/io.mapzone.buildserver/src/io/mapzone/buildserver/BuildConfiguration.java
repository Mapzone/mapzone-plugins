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

import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.Computed;
import org.polymap.model2.ComputedBidiManyAssocation;
import org.polymap.model2.Defaults;
import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.runtime.UnitOfWork;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildConfiguration
        extends Entity {

    public static BuildConfiguration    TYPE;
    
    public enum Type {
        PRODUCT, PLUGIN
    }

    public Property<String>             name;
    
    /**
     * The bundle-id of the product to build. 
     */
    public Property<String>             productName;
    
    public Property<Type>               type;
    
    public Property<String>             userId;
    
    @Defaults
    public CollectionProperty<TargetPlatformConfiguration> targetPlatform;
    
    @Defaults
    public CollectionProperty<ScmConfiguration> scm;
    
    @Computed( ComputedBidiManyAssocation.class )
    public ManyAssociation<BuildResult> buildResults;
    
    
    public UnitOfWork belongsTo() {
        return context.getUnitOfWork();
    }
    
    
    /**
     * Configuration of a SCM system.
     */
    public static class ScmConfiguration
            extends Composite {

        public enum Type {
            DIRECTORY, GIT
        }

        public Property<Type>       type;
        
        public Property<String>     name;
        
        public Property<String>     url;
        
        public Property<String>     branch;
    }

    
    /**
     * Configuration of a target platform location. 
     */
    public static class TargetPlatformConfiguration
            extends Composite {

        public enum Type {
            DIRECTORY, ZIP_DOWNLOAD
        }
        
        public Property<String>     url;
        
        public Property<Type>       type;
    }

}
