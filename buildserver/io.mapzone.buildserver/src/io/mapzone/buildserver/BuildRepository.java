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

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.locking.OptimisticLocking;
import org.polymap.model2.store.recordstore.RecordStoreAdapter;
import org.polymap.recordstore.lucene.LuceneRecordStore;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildRepository {

    private static final Log log = LogFactory.getLog( BuildRepository.class );
    
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
    
}
