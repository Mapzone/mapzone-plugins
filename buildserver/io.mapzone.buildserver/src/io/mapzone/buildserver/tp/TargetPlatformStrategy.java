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

import java.io.File;
import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.mapzone.buildserver.BuildStrategy;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class TargetPlatformStrategy
        extends BuildStrategy {

    private static final Log log = LogFactory.getLog( TargetPlatformStrategy.class );


//    @Override
//    public void beforeBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {
//        monitor.beginTask( "Create target platform config", 1 );
//
//        JSONArray tps = new JSONArray();
//        for (TargetPlatformConfig c : config.targetPlatform) {
//            tps.put( new JSONObject().put( "type", c.type.get() ).put( "url", c.url.get() ) );
//        }
//        JSONObject json = new JSONObject().put( "targetPlatform", tps );
//        
//        FileUtils.write( new File( workspace, "config" ), json.toString( 2 ), "UTF-8" );
//        monitor.done();
//    }

    
    protected JSONObject loadConfig( BuildContext context ) throws Exception {
        File f = new File( context.workspace.get(), "config" );
        return f.exists()
                ? new JSONObject( FileUtils.readFileToString( f, "UTF-8" ) )
                : new JSONObject().put( "targetPlatform", new JSONArray() );
    }

    
    protected void saveConfig( BuildContext context, JSONObject json ) throws Exception {
        File f = new File( context.workspace.get(), "config" );
        FileUtils.write( f, json.toString( 2 ), "UTF-8" );                
    }
    
}
