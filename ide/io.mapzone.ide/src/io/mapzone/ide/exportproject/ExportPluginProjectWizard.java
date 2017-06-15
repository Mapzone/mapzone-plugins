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
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;

import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.PluginExportOperation;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;

import io.mapzone.ide.IdePlugin;
import io.mapzone.ide.MapzonePluginProject;

/**
 * Uses the PDE {@link PluginExportOperation} to create a plugin jar and than send it
 * to the mapzone project.
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
@SuppressWarnings( "restriction" )
public class ExportPluginProjectWizard
        extends Wizard
        implements IExportWizard {

    private IStructuredSelection    selection;
    
    private File                    exportDir;

    private WizardData              data = new WizardData();
    

    @Override
    public void init( IWorkbench workbench, @SuppressWarnings( "hiding" ) IStructuredSelection selection ) {
        try {
            this.selection = selection;
            setNeedsProgressMonitor( true );
            exportDir = Files.createTempDirectory( IdePlugin.ID + ".export" ).toFile();
            exportDir.deleteOnExit();
            
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
        catch (IOException e) {
            throw new RuntimeException( e );
        }
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
        PluginModelManager modelManager = PluginModelManager.getInstance();
        IPluginModelBase model = modelManager.findModel( project );        
        
        // NOTE: Any changes to the content here must also be copied to generateAntTask() and PluginExportTask
        final FeatureExportInfo info = new FeatureExportInfo();
        info.toDirectory = true;  //fPage.doExportToDirectory();
        info.useJarFormat = true;  //fPage.useJARFormat();
        info.exportSource = false;  //fPage.doExportSource();
        info.exportSourceBundle = false;  //fPage.doExportSourceBundles();
        info.allowBinaryCycles = true; //fPage.allowBinaryCycles();
        info.useWorkspaceCompiledClasses = true;  //fPage.useWorkspaceCompiledClasses();
        info.destinationDirectory = exportDir.getAbsolutePath();  //fPage.getDestination();
        info.zipFileName = null;  //"temp.zip";  //fPage.getFileName();
        info.items = new Object[] {model};  //fPage.getSelectedItems();
        info.signingInfo = null;  //fPage.useJARFormat() ? fPage.getSigningInfo() : null;
        info.qualifier = null;  //"1";  //fPage.getQualifier();

//        final boolean installAfterExport = fPage.doInstall();
//        if (installAfterExport) {
//            RuntimeInstallJob.modifyInfoForInstall(info);
//        }

        try {
            FileUtils.cleanDirectory( exportDir );
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }
        
        PluginExportOperation job = new PluginExportOperation( info, PDEUIMessages.PluginExportJob_name );
        job.setUser( true );
        job.setRule( ResourcesPlugin.getWorkspace().getRoot() );
        job.setProperty( IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PLUGIN_OBJ );
        job.addJobChangeListener( new JobChangeAdapter() {
            @Override
            public void done( IJobChangeEvent ev ) {
                if (ev.getResult().isOK()) {
                    // install if OK
                    scheduleInstallJob();
                }
                if (job.hasAntErrors()) {
                    // If there were errors when running the ant scripts, inform the
                    // user where the logs can be found.
                    final File logLocation = new File( info.destinationDirectory, "logs.zip" ); //$NON-NLS-1$
                    if (logLocation.exists()) {
                        Display display = PlatformUI.getWorkbench().getDisplay();
                        display.syncExec( () -> {
                            MessageDialog.openError( display.getActiveShell(), "Error", "" + logLocation );
                        });
                    }
                }
//                else if (event.getResult().isOK() && installAfterExport) {
//                    // Install the export into the current running platform
//                    RuntimeInstallJob installJob = new RuntimeInstallJob( PDEUIMessages.PluginExportWizard_InstallJobName, info );
//                    installJob.setUser( true );
//                    installJob.setProperty( IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_FEATURE_OBJ );
//                    installJob.schedule();
//                }
            }
        });
        job.schedule();
    }

    
    protected void scheduleInstallJob() {
        Job job = new Job( "Install plugin" ) {
            @Override
            protected IStatus run( IProgressMonitor monitor ) {
                File dir = new File( exportDir, "plugins" );
                File[] files = dir.listFiles();
                assert files.length == 1;
                data.mproject.connect( null ).installBundle( files[0], monitor );
                return Status.OK_STATUS;
            }
        };
        job.setUser( true );
        job.setPriority( Job.BUILD );
        job.schedule();
    }
    
}
