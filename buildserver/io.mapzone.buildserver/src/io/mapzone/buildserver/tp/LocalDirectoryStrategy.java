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
package io.mapzone.buildserver.tp;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;

import io.mapzone.buildserver.BuildConfig;
import io.mapzone.buildserver.BuildConfig.TargetPlatformConfig;
import io.mapzone.buildserver.BuildConfig.TargetPlatformConfig.Type;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class LocalDirectoryStrategy
        extends TargetPlatformStrategy {

    private static final Log log = LogFactory.getLog( LocalDirectoryStrategy.class );

    @Override
    public boolean init( BuildConfig config ) {
        return config.targetPlatform.stream().anyMatch( tp -> tp.type.get() == Type.DIRECTORY );
    }


    @Override
    public void preBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Create target platform config", 1 );

        JSONObject config = loadConfig( context );
        JSONArray tps = config.getJSONArray( "targetPlatform" );
        for (TargetPlatformConfig c : context.config.get().targetPlatform) {
            tps.put( new JSONObject().put( "type", c.type.get() ).put( "url", c.url.get() ) );
        }
        saveConfig( context, config );
        
        monitor.done();
    }

}
