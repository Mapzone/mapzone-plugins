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
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.model2.runtime.UnitOfWork;

/**
 * Creates {@link BuildContext#result} and manages its {@link BuildResult#status}.
 * Also commits {@link UnitOfWork}. 
 *
 * @author Falko Bräutigam
 */
public class InitBuildResultStrategy
        extends BuildStrategy {

    private static final Log log = LogFactory.getLog( InitBuildResultStrategy.class );

    @Override
    public boolean init( BuildConfig config ) {
        return true;
    }

    
    @Override
    public void preBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {
        BuildConfig config = context.config.get();
        UnitOfWork uow = config.belongsTo();

        context.result.set( uow.createEntity( BuildResult.class, null, (BuildResult proto) -> {
            proto.config.set( config );
            proto.started.set( new Date() );
            proto.status.set( BuildResult.Status.RUNNING );

            File dataDir = new File( BsPlugin.exportDataDir(), proto.id().toString() );
            dataDir.mkdir();
            proto.dataDir.set( dataDir.getAbsolutePath() );

            return proto;
        }));
        uow.commit();
    }

    
    @Override
    public void cleanup( BuildContext context, IProgressMonitor monitor ) throws Exception {
        BuildConfig config = context.config.get();
        UnitOfWork uow = config.belongsTo();
        
        context.result.get().status.set( context.exception.isPresent() 
                ? BuildResult.Status.FAILED 
                : BuildResult.Status.OK );
        pruneResults( config );
        
        uow.commit();
    }


    protected void pruneResults( BuildConfig config ) {
        Map<Date,BuildResult> sorted = new TreeMap();
        config.buildResults.forEach( result -> sorted.put( result.started.get(), result ) );
        for (Iterator<BuildResult> it=sorted.values().iterator(); it.hasNext() && sorted.size() > 3; ) {
            BuildResult result = it.next(); it.remove();
            result.destroy();
        }
        config.belongsTo().commit();
    }    

}
