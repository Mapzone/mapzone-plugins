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

import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.Defaults;
import org.polymap.model2.Entity;
import org.polymap.model2.Property;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildConfiguration
        extends Entity {

    /**
     * The bundle-id of the product to build. 
     */
    public Property<String>         productName;
    
    public Property<File>           targetDir;

    @Defaults
    public CollectionProperty<TargetPlatformConfiguration> targetPlatform;
    
    @Defaults
    public CollectionProperty<ScmConfiguration>            scm;
    
    
    /**
     * Configuration of a SCM system.
     */
    public static class ScmConfiguration
            extends Composite {

        public enum TYPE {
            Directory, Git
        }

        public Property<TYPE>       type;
        
        public Property<String>     name;
        
        public Property<String>     url;
        
        public Property<String>     branch;
    }

    
    /**
     * Configuration of a target platform location. 
     */
    public static class TargetPlatformConfiguration
            extends Composite {

        public enum TYPE {
            Directory, ZipDownload
        }
        
        public Property<String>     url;
        
        public Property<TYPE>       type;
    }
    
}
