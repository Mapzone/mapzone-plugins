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
package io.mapzone.ide;

import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.eclipse.ui.IStartup;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class PeriodicGCJobStartup
        implements IStartup {

    @Override
    public void earlyStartup() {
        PeriodicGCJob.start();
    }
    

    /**
     * Helps G1GC to release unused memory to the system.
     */
    static class PeriodicGCJob
            extends Job {

        public static final long    DELAY_MILLIS = TimeUnit.MINUTES.toMillis( 5 );
     
        public static PeriodicGCJob start() {
            PeriodicGCJob result = new PeriodicGCJob();
            result.schedule( DELAY_MILLIS );
            System.out.println( "GC: job started" );
            return result;
        }
        
        protected PeriodicGCJob() {
            super( PeriodicGCJob.class.getSimpleName() );
            setUser( false );
            setPriority( DECORATE );
        }

        @Override
        protected IStatus run( IProgressMonitor monitor ) {
            try {
                // don't kick in build and refresh jobs
                getJobManager().join( ResourcesPlugin.FAMILY_MANUAL_BUILD, monitor );
                getJobManager().join( ResourcesPlugin.FAMILY_AUTO_BUILD, monitor );
                getJobManager().join( ResourcesPlugin.FAMILY_MANUAL_REFRESH, monitor );
                getJobManager().join( ResourcesPlugin.FAMILY_AUTO_REFRESH, monitor );
                
                Runtime rt = Runtime.getRuntime();
                long before = rt.totalMemory() - rt.freeMemory();

                System.gc();

                long now = rt.totalMemory() - rt.freeMemory();
                System.out.println( "GC: "
                        + FileUtils.byteCountToDisplaySize( now ) 
                        + " - " + FileUtils.byteCountToDisplaySize( before-now ) + " reclaimed" );
                return Status.OK_STATUS;
            }
            catch (InterruptedException e) {
                return Status.CANCEL_STATUS;
            }
            finally {
                schedule( DELAY_MILLIS );
            }
        }
    }

}
