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

import java.util.Date;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.Association;
import org.polymap.model2.Entity;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildResult
        extends Entity {

    private static final Log log = LogFactory.getLog( BuildResult.class );
    
    public static BuildResult               TYPE;
    
    public enum Status {
        RUNNING, OK, FAILED
    }
    
    public Association<BuildConfiguration>  config;
    
    public Property<Status>                 status;

    public Property<Date>                   started;

    @Nullable
    public Property<String>                 dataDir;
    
    
    public File dataDir() {
        return new File( dataDir.get() );
    }

    
    public void destroy() {
        try {
            FileUtils.deleteDirectory( dataDir() );
        }
        catch (Exception e) {
            log.warn( "", e );
        }
        context.getUnitOfWork().removeEntity( this );
    }
    
}
