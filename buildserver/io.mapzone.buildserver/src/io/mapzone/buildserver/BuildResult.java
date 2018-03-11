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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Throwables;

import org.polymap.model2.Association;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;
import org.polymap.model2.runtime.ValueInitializer;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildResult
        extends BuildObject {

    private static final Log log = LogFactory.getLog( BuildResult.class );
    
    public static BuildResult           TYPE;
    
    public static final ValueInitializer<BuildResult> defaults( BuildConfig config ) {
        return (BuildResult proto) -> {
            proto.config.set( config );
            proto.started.set( new Date() );
            proto.status.set( Status.RUNNING );
            proto.downloadPath.set( RandomStringUtils.random( 6, true, true ) );
            File dataDir = new File( BsPlugin.exportDataDir(), proto.id().toString() );
            dataDir.mkdirs();
            proto.dataDir.set( dataDir.getAbsolutePath() );
            return proto;
        };
    }
    
    public enum Status {
        RUNNING, OK, FAILED
    }
    
    private static final Pattern        SEVERITY = Pattern.compile( "^[0-9]+\\. ([A-Za-z]+) " );
    
    public static final String          CONSOLE_LOG = "console.log";

    // instance *******************************************
    
    public Association<BuildConfig>     config;
    
    public Property<Status>             status;

    public Property<Date>               started;

    @Nullable
    public Property<String>             dataDir;
    
    /** The servlet path where this build can be accessed, if it was successfull. */
    public Property<String>             downloadPath;
    
    /**
     * The directory where build results and logs are stored.
     */
    public File dataDir() {
        return new File( dataDir.get() );
    }

    /**
     * The packed build result.
     */
    public File zipFile() {
        return new File( dataDir(), "download.zip" );
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


    public Optional<String> console() {
        try {
            File f = new File( dataDir(), CONSOLE_LOG );
            return Optional.ofNullable( f.exists() ? FileUtils.readFileToString( f, "UTF-8" ) : null );
        }
        catch (IOException e) {
            log.warn( "", e );
            return Optional.empty();
        }
    }
    
    
    public List<LogEntry> logEntries( int maxResults, LogEntry.Severity... severities  ) {
        File f = new File( dataDir(), "logs.zip" );
        if (!f.exists()) {
            return Collections.EMPTY_LIST;
        }
        
        try (
            ZipFile zip = new ZipFile( f );
        ){
            List<LogEntry> result = new ArrayList( maxResults );
            for (ZipEntry file : Collections.list( zip.entries() )) {
                log.info( "  " + file.getName() );
                if (file.getName().endsWith( "@dot.log" )) {
                    LineNumberReader reader = new LineNumberReader( new InputStreamReader( zip.getInputStream( file ), "ISO-8859-1" ) );
                    LogEntry logEntry = null;
                    for (String line=reader.readLine(); line != null; line=reader.readLine()) {
                        if (line.startsWith( "#" )) { // skip
                        }
                        else if (line.startsWith( "----" )) {
                            logEntry = null;
                            if (result.size() >= maxResults) {
                                return result;
                            }
                        }
                        else if (logEntry != null) {
                            assert logEntry.head != null;
                            logEntry.text.add( line );
                        }
                        else {
                            Matcher matcher = SEVERITY.matcher( line );
                            if (!matcher.find()) {
                                log.info( "Skipping line: " + line );  // summary line
                            }
                            else {
                                LogEntry.Severity severity = LogEntry.Severity.valueOf( matcher.group( 1 ) );
                                logEntry = new LogEntry();
                                logEntry.severity = severity;
                                logEntry.bundleId = StringUtils.substringBefore( file.getName(), "_" );  // version number
                                logEntry.head = StringUtils.substringAfter( line, logEntry.bundleId );
                                
                                if (severities.length == 0 || ArrayUtils.contains( severities, severity )) {
                                    result.add( logEntry );
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }
        catch (Exception e) {
            throw Throwables.propagate( e );
        }
    }
    

    /**
     * 
     */
    public static class LogEntry {
        
        public enum Severity {
            WARNING, ERROR
        }
        
        public Severity     severity;
        
        public String       bundleId;
        
        public String       head;
        
        public List<String> text = new ArrayList();
    }
    
}
