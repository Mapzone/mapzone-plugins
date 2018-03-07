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

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfig;
import io.mapzone.buildserver.BuildConfig.TargetPlatformConfig;
import io.mapzone.buildserver.BuildConfig.TargetPlatformConfig.Type;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class ZipDownloadStrategy
        extends TargetPlatformStrategy {

    private static final Log log = LogFactory.getLog( ZipDownloadStrategy.class );

    @Override
    public boolean init( BuildConfig config ) {
        return config.targetPlatform.stream().anyMatch( tp -> tp.type.get() == Type.ZIP_DOWNLOAD );
    }


    @Override
    public void preBuild( BuildContext context, IProgressMonitor monitor ) throws Exception {
        monitor.beginTask( "Target platform", 1 );

        JSONObject config = loadConfig( context );
        JSONArray tps = config.getJSONArray( "targetPlatform" );
        for (TargetPlatformConfig c : context.config.get().targetPlatform) {
            String url = download( context, c, monitor );
            tps.put( new JSONObject()
                    .put( "type", Type.DIRECTORY.toString() )
                    .put( "url", url ) );
        }
        saveConfig( context, config );
        
        monitor.done();
    }


    protected String download( BuildContext context, TargetPlatformConfig c, IProgressMonitor monitor ) throws Exception {
        monitor.subTask( "Download: " + c.url.get() );
        
        File cacheDir = BsPlugin.cacheDir( context.config.get(), c.url.get() );
        if (cacheDir.list().length == 0) {
            URL url = new URL( c.url.get() );
            try (
                ZipInputStream zip = new ZipInputStream( url.openStream() )
            ){
                for (ZipEntry entry=zip.getNextEntry(); entry!=null; entry=zip.getNextEntry()) {
                    String path = FilenameUtils.getPath( entry.getName() );
                    if (entry.getName().endsWith( ".jar" ) && path.endsWith( "plugins/" )) {
                        File f = new File( cacheDir, FilenameUtils.getName( entry.getName() ) );
                        monitor.subTask( "Transfering " + f.getName() );
                        try (FileOutputStream out = new FileOutputStream( f )) {
                            IOUtils.copy( zip, out );
                        }
                    }
                    if (monitor.isCanceled()) {
                        throw new OperationCanceledException();
                    }
                }
            }
        }
        return cacheDir.getAbsolutePath();
    }

}
