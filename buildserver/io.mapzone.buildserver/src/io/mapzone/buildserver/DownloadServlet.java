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

import java.util.Iterator;
import java.util.Optional;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.UnitOfWork;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class DownloadServlet
        extends HttpServlet {

    private static final Log log = LogFactory.getLog( DownloadServlet.class );

    public static final String      ALIAS = "/builds/download";
    
    public static String downloadZip( BuildConfig config ) {
        try {
            return URLEncoder.encode( config.name.opt().orElse( "name_to_be_specified" ), "UTF-8" ) + ".zip";
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException( "Should never happen." );
        }
    }
    
    
    // instance *******************************************
    
    @Override
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
        log.info( "Starting..." );
    }


    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        String path = request.getPathInfo();
        log.info( "Path: " + path );
        try (
            UnitOfWork uow = BuildRepository.instance().newUnitOfWork();
            ResultSet<BuildConfig> rs = uow.query( BuildConfig.class )
                    .where( Expressions.eq( BuildConfig.TYPE.downloadPath, path.substring( 1 ) ) )
                    .execute();
        ){
            Iterator<BuildConfig> it = rs.iterator();
            // BuildConfig exists
            if (it.hasNext()) {
                BuildConfig config = it.next();
                assert !it.hasNext() : "Multiple BuildConfigs with same downloadPath!";
                
                Optional<BuildResult> latest = config.latestSuccessfullResult();
                if (latest.isPresent()) {
                    try (
                        OutputStream out = response.getOutputStream();
                        InputStream in = new FileInputStream( latest.get().zipFile() );   
                    ){
                        IOUtils.copy( in, out );
                    }
                }
                else {
                    response.sendError( 404, "There is no successfull build available." );
                }
            }
            // nothing
            else {
                response.sendError( 404, "No such build configuration." );
            }
        }
    }
    
}
