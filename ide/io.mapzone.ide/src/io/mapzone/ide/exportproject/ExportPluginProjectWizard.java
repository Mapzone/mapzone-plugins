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
package io.mapzone.ide.exportproject;

import java.io.File;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.pde.internal.core.exports.PluginExportOperation;

import io.mapzone.ide.ExportPluginHelper;
import io.mapzone.ide.ExportPluginHelper.PluginExportOperation2;
import io.mapzone.ide.MapzonePluginProject;

/**
 * Uses the PDE {@link PluginExportOperation} to create a plugin jar and then send it
 * to the mapzone project.
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
@SuppressWarnings( "restriction" )
public class ExportPluginProjectWizard
        extends Wizard
        implements IExportWizard {

    private IStructuredSelection    selection;
    
    private WizardData              data = new WizardData();
    

    @Override
    public void init( IWorkbench workbench, @SuppressWarnings( "hiding" ) IStructuredSelection selection ) {
        this.selection = selection;
        setNeedsProgressMonitor( true );
        
        if (selection.isEmpty()) {
            throw new IllegalStateException( "Choose the plugin-in project to export." );
        }
        else if (selection.size() > 1) {
            throw new IllegalStateException( "Choose just one plugin-in project to export." );
        }
        else if (!(selection.getFirstElement() instanceof IProject)) {
            throw new IllegalStateException( "Choose an IProject to export." );
        }
        data.mproject = MapzonePluginProject.of( (IProject)selection.getFirstElement() );
    }

    
    @Override
    public void addPages() {
        addPage( new ExportPluginLoginPage( data ) );
    }

    
    @Override
    public boolean performFinish() {
        scheduleExportJob();
        return true;
    }
    
    
    protected void scheduleExportJob() {
        Project project = (Project)selection.getFirstElement();
        PluginExportOperation2 job = ExportPluginHelper.createExportJob( project );
        job.addJobChangeListener( new JobChangeAdapter() {
            @Override
            public void done( IJobChangeEvent ev ) {
                if (ev.getResult().isOK()) {
                    // install if OK
                    scheduleInstallJob( new File( job.getDestinationDirectory() ) );
                }
            }
        });
        job.schedule();
    }

    
    protected void scheduleInstallJob( File exportDir ) {
        Job job = new Job( "Install plugin" ) {
            @Override
            protected IStatus run( IProgressMonitor monitor ) {
                File dir = new File( exportDir, "plugins" );
                File[] files = dir.listFiles();
                assert files.length == 1;
                data.mproject.connect( null, null ).installBundle( files[0], monitor );
                return Status.OK_STATUS;
            }
        };
        job.setUser( true );
        job.setPriority( Job.BUILD );
        job.schedule();
    }
    
}
