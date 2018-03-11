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

import org.polymap.core.runtime.event.EventManager;

import org.polymap.model2.Entity;
import org.polymap.model2.runtime.Lifecycle;
import org.polymap.model2.runtime.UnitOfWork;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class BuildObject
        extends Entity 
        implements Lifecycle {

    public UnitOfWork belongsTo() {
        return context.getUnitOfWork();
    }

    @Override
    public void onLifecycleChange( State state ) {
        if (state == State.AFTER_COMMIT) {
            EventManager.instance().publish( new BuildObjectCommittedEvent( BuildObject.this ) );
        }
    }

}
