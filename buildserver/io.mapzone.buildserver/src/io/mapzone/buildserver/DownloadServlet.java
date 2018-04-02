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

import java.util.Arrays;
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
import org.apache.commons.lang3.StringUtils;
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
    
    public static final String      LOGS_ZIP = "logs.zip";
    

    public static String productZipUrl( BuildConfig config ) {
        return ALIAS + "/" + config.downloadPath.get() + "/" + config.name.opt().orElse( "name_to_be_specified" ) + ".zip";
    }
    
    public static String errorLogZipUrl( BuildResult result ) {
        return ALIAS + "/" + result.downloadPath.get() + "/" + LOGS_ZIP;
    }
    
    public static String encoded( String s ) {
        try {
            return URLEncoder.encode( s, "UTF-8" );
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
        String pathInfo = request.getPathInfo();
        String[] path = StringUtils.split( pathInfo, "/" );
        log.info( "Path: " + Arrays.toString( path ) );
        
        if (path.length != 2) {
            response.sendError( 404 );
        }
        else if (path[1].equals( LOGS_ZIP )) {
            doGetLogsZip( request, response, path[0] );
        }
        else {
            doGetProductZip( request, response, path[0] );
        }
    }
    

    protected void doGetLogsZip( HttpServletRequest request, HttpServletResponse response, String configPath )
            throws ServletException, IOException {
        try (
            UnitOfWork uow = BuildRepository.instance().newUnitOfWork();
            ResultSet<BuildResult> rs = uow.query( BuildResult.class )
                    .where( Expressions.eq( BuildResult.TYPE.downloadPath, configPath ) )
                    .execute();
        ){
            Iterator<BuildResult> it = rs.iterator();
            // exists
            if (it.hasNext()) {
                BuildResult result = it.next();
                assert !it.hasNext() : "Multiple BuildResults with same downloadPath!";
                try (
                    OutputStream out = response.getOutputStream();
                    InputStream in = new FileInputStream( result.logsFile() );   
                ){
                    IOUtils.copy( in, out );
                }
            }
            // nothing
            else {
                response.sendError( 404, "No such build configuration." );
            }
        }
    }
    
    protected void doGetProductZip( HttpServletRequest request, HttpServletResponse response, String configPath )
            throws ServletException, IOException {
        try (
            UnitOfWork uow = BuildRepository.instance().newUnitOfWork();
            ResultSet<BuildConfig> rs = uow.query( BuildConfig.class )
                    .where( Expressions.eq( BuildConfig.TYPE.downloadPath, configPath ) )
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
