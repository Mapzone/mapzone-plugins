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

import java.util.Optional;
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

        context.result.set( uow.createEntity( BuildResult.class, null, BuildResult.defaults( config ) ) );
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
        Optional<BuildResult> pinned = config.latestSuccessfullResult();
        
        config.buildResults.stream()
                .sorted( (r1,r2) -> r1.started.get().compareTo( r2.started.get() ) )
                .filter( r -> pinned.map( p -> r != p ).orElse( true ) )
                .limit( Math.max( 0, config.buildResults.size() - 3 ) )
                .forEach( r -> r.destroy() );
        
//        // FIXME keep latest successfull build
//        Map<Date,BuildResult> sorted = new TreeMap();
//        config.buildResults.forEach( result -> sorted.put( result.started.get(), result ) );
//        
//        for (Iterator<BuildResult> it=sorted.values().iterator(); it.hasNext() && sorted.size() > 3; ) {
//            BuildResult result = it.next(); it.remove();
//            result.destroy();
//        }
        config.belongsTo().commit();
    }    

}
