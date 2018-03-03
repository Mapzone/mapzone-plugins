/* 
 * polymap.org
 * Copyright (C) 2017-2018, the @authors. All rights reserved.
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
package io.mapzone.ide.publishplugin;

import java.io.File;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;

import io.mapzone.ide.IdePlugin;
import io.mapzone.ide.MapzonePluginProject;
import io.mapzone.ide.apiclient.MapzoneAPIClient;
import io.mapzone.ide.apiclient.MapzoneAPIClient.PluginsFolder;
import io.mapzone.ide.build.ExportHelper;
import io.mapzone.ide.build.ExportHelper.PluginExportOperation2;
import io.mapzone.ide.util.UIUtils;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
@SuppressWarnings( "restriction" )
public class PublishPluginWizard
        extends Wizard {

    protected WizardData                data = new WizardData();
    
    protected LoginPage                 loginPage;

    protected EditPluginDescriptionPage editPage;
    

    public PublishPluginWizard() {
        setNeedsProgressMonitor( true );
        setWindowTitle( "Publish Plug-in" );

        Object selection = UIUtils.currentSelection();
        if (selection instanceof IProject) {
            data.mproject = MapzonePluginProject.of( (IProject)selection );            
        }
        else if (selection instanceof JavaProject) {
            data.mproject = MapzonePluginProject.of( ((JavaProject)selection).getProject() );            
        }
        else {
            throw new RuntimeException( "Unknown selection type: " + selection.getClass().getName() );
        }
    }


    @Override
    public void createPageControls( Composite pageContainer ) {
        // delay editPage until successfully logged in
        loginPage.createControl( pageContainer );
    }


    @Override
    public void addPages() {
        addPage( loginPage = new LoginPage( data ) );
        addPage( editPage = new EditPluginDescriptionPage( data ) );
        
        ((WizardDialog)getContainer()).addPageChangingListener( ev -> {
            if (ev.getCurrentPage() == loginPage) {
                assert ev.getTargetPage() == editPage;
                performLogin();
            }
        });
    }

    
    protected void performLogin() {
        MapzoneAPIClient client = data.mproject.connectServer( null, null );
        String pluginId = data.mproject.project().getName();
        data.publishedPlugin = client
                .findPlugin( PluginsFolder.my, pluginId )
                .orElseGet( () -> client.newPlugin( pluginId ) );
    }

    
    @Override
    public boolean performFinish() {
        try {
            // update description
            getContainer().run( false, false, monitor -> {
                try {
                    data.publishedPlugin.submitChanges( monitor );
                }
                catch (Exception e) {
                    IdePlugin.logException( e );
                }
            });
            
            // export plugin
            if (data.exportPlugin) {
                FeatureExportInfo info = ExportHelper.standardExportInfo( data.mproject.project() );
                PluginExportOperation2 job = ExportHelper.createPluginExportJob( info );
                job.addJobChangeListener( ExportHelper.informUserAboutErrors() );
                job.addJobChangeListener( new JobChangeAdapter() {
                    @Override public void done( IJobChangeEvent ev ) {
                        try {
                            File dir = new File( job.getDestinationDirectory(), "plugins" );
                            File[] files = dir.listFiles();
                            assert files.length == 1;

                            data.publishedPlugin.updateContents( files[0], null );
                        }
                        catch (Exception e) {
                            IdePlugin.logException( e );
                        }
                    }
                });
                job.schedule();
            }
            return true;
        }
        catch (Exception e) {
            e.printStackTrace( System.err );
            return false;
        }
    }

}
