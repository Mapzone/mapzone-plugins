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
import java.util.EventObject;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.mapzone.buildserver.BuildStrategy.BuildContext;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class PrintProgressMonitor
        extends NullProgressMonitor {

    private static final Log log = LogFactory.getLog( PrintProgressMonitor.class );
    
    private IProgressMonitor        delegate;
    
    private List<String>            lines = new ArrayList( 1024 );

    private int                     totalWork;
    
    private double                  worked;

    
    public PrintProgressMonitor( IProgressMonitor delegate ) {
        this.delegate = delegate;
    }

    public int percentDone() {
        return (int)Math.round( 100 * worked / totalWork );
    }
    
    @Override
    public void internalWorked( double work ) {
        this.worked += work;
    }

    @Override
    public void worked( int work ) {
        this.worked += work;
    }

    @Override
    public boolean isCanceled() {
        return delegate.isCanceled();
    }

    @Override
    @SuppressWarnings( "hiding" )
    public void beginTask( String name, int totalWork ) {
        this.totalWork = totalWork;
        log.info( "  " + name );
        lines.add( name );
    }

    @Override
    public void subTask( String name ) {
        if (!StringUtils.isBlank( name )) {
            log.info( "    " + name );
            lines.add( name );
        }
    }

    @Override
    public void setTaskName( String name ) {
        log.info( "    " + name );
    }

    @Override
    public void done() {
    }

    public void writeLinesTo( File f ) {
        try {
            try (PrintWriter out = new PrintWriter( f, "UTF-8" )){
                for (String line : lines) {
                    out.println( line );
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }
    
    protected abstract void fireProgressEvent();
    

    /**
     * 
     */
    public static class ProgressEvent
            extends EventObject {

        public ProgressEvent( BuildContext source ) {
            super( source );
        }

        @Override
        public BuildContext getSource() {
            return (BuildContext)super.getSource();
        }
    }
    
}
