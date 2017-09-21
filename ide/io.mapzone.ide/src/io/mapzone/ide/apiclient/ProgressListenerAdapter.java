/* 
 * polymap.org
 * Copyright (C) 2017, the @authors. All rights reserved.
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
package io.mapzone.ide.apiclient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.milton.httpclient.ProgressListener;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
class ProgressListenerAdapter
        implements ProgressListener {

    private IProgressMonitor    delegate;
    
    /**
     * 
     * 
     * @param delegate The delegate, or null for {@link NullProgressMonitor}.
     */
    public ProgressListenerAdapter( IProgressMonitor delegate ) {
        this.delegate = delegate != null ? delegate : new NullProgressMonitor();
    }

    @Override 
    public void onRead( int bytes ) { }
    
    @Override 
    public void onProgress( long bytesRead, Long totalBytes, String fileName ) {
        delegate.worked( (int)bytesRead );
    }
    
    @Override 
    public void onComplete( String fileName ) { }
    
    @Override 
    public boolean isCancelled() {
        return delegate.isCanceled();
    }
    
}