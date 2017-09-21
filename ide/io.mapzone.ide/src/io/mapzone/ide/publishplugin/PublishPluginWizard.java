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
package io.mapzone.ide.publishplugin;

import java.io.File;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import io.mapzone.ide.ExportPluginHelper;
import io.mapzone.ide.ExportPluginHelper.PluginExportOperation2;
import io.mapzone.ide.IdePlugin;
import io.mapzone.ide.MapzonePluginProject;
import io.mapzone.ide.apiclient.MapzoneAPIClient;
import io.mapzone.ide.util.UIUtils;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class PublishPluginWizard
        extends Wizard {

    protected WizardData                data = new WizardData();
    
    protected LoginPage                 loginPage;

    protected EditPluginDescriptionPage editPage;
    

    public PublishPluginWizard() {
        setNeedsProgressMonitor( true );
        setWindowTitle( "Publish Plug-in" );

        IProject selection = UIUtils.currentSelection();
        data.mproject = MapzonePluginProject.of( selection );
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
                .findPlugin( "my", pluginId )
                .orElseGet( () -> client.newPlugin( pluginId ) );
    }

    
    @Override
    public boolean performFinish() {
        try {
            // update description
            getContainer().run( false, false, monitor -> {
                try {
                    data.publishedPlugin.applyChanges( monitor );
                }
                catch (Exception e) {
                    IdePlugin.logException( e );
                }
            });
            
            // export plugin
            if (data.exportPlugin) {
                PluginExportOperation2 job = ExportPluginHelper.createExportJob( data.mproject.project() );
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
