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
package io.mapzone.ide.build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.osgi.framework.Version;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.PluginExportOperation;
import org.eclipse.pde.internal.core.exports.ProductExportOperation;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.internal.ui.PDEPluginImages;

import io.mapzone.ide.IdePlugin;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
@SuppressWarnings( "restriction" )
public class ExportHelper {

    public static PluginExportOperation2 createPluginExportJob( FeatureExportInfo info ) {
        PluginExportOperation2 job = new PluginExportOperation2( info, "Export" );
        job.setUser( true );
        job.setRule( ResourcesPlugin.getWorkspace().getRoot() );
        job.setProperty( IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PLUGIN_OBJ );
        return job;
    }

    
    public static ProductExportOperation createProductExportJob( IProject project, FeatureExportInfo info, File rootDir ) throws Exception {
        // XXX find product file
        IFile productFile = (IFile)Arrays.stream( project.members() )
                .filter( child -> child instanceof IFile && child.getName().contains( "product" ) )
                .findAny().orElseThrow( () -> new RuntimeException( "No product file found in: " + project.getName() ) );
        WorkspaceProductModel productModel = new WorkspaceProductModel( productFile, false );
        productModel.load();
        
        State state = TargetPlatformHelper.getState();
        IProductPlugin[] plugins = productModel.getProduct().getPlugins();
        List items = new ArrayList();
        for (int i = 0; i < plugins.length; i++) {
            BundleDescription bundle = null;
            String v = plugins[i].getVersion();
            if (v != null && v.length() > 0) {
                bundle = state.getBundle( plugins[i].getId(), Version.parseVersion( v ) );
            }
            // if there's no version, just grab a bundle like before
            if (bundle == null) {
                bundle = state.getBundle( plugins[i].getId(), null );
            }
            if (bundle != null) {
                items.add( bundle );
            }
        }
        info.items = items.toArray( new BundleDescription[items.size()] );
        
        return new ProductExportOperation( info, "Export", productModel.getProduct(), "" ); //rootDir.getAbsolutePath() );
    }
    
    
    public static FeatureExportInfo standardExportInfo( IProject project ) {
        File exportDir = createExportDir();
        final FeatureExportInfo info = new FeatureExportInfo();
        info.toDirectory = true;
        info.useJarFormat = true;
        info.exportSource = false;
        info.exportSourceBundle = false;
        info.allowBinaryCycles = true;
        info.useWorkspaceCompiledClasses = true;
        info.destinationDirectory = exportDir.getAbsolutePath();
        info.zipFileName = null;
        info.signingInfo = null;
        info.qualifier = null;  //"1";

        PluginModelManager modelManager = PluginModelManager.getInstance();
        IPluginModelBase model = modelManager.findModel( project );
        info.items = new Object[] {model};
        return info;
    }


    public static File createExportDir() {
        try {
            File exportDir = Files.createTempDirectory( IdePlugin.ID + ".export." ).toFile();
            exportDir.deleteOnExit();
            return exportDir;
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }

    
    public static JobChangeAdapter informUserAboutErrors() {
        return new JobChangeAdapter() {
            @Override
            public void done( IJobChangeEvent ev ) {
                if (ev.getResult().isOK()) {
                    // install if OK
                    //scheduleInstallJob();
                }
                PluginExportOperation2 job = (PluginExportOperation2)ev.getJob();
                if (job.hasAntErrors()) {
                    // If there were errors when running the ant scripts, inform the
                    // user where the logs can be found.
                    final File logLocation = new File( job.getDestinationDirectory(), "logs.zip" ); //$NON-NLS-1$
                    if (logLocation.exists()) {
                        Display display = PlatformUI.getWorkbench().getDisplay();
                        display.syncExec( () -> {
                            MessageDialog.openError( display.getActiveShell(), "Error", "" + logLocation );
                        });
                    }
                }
            }
        };
    }


    /** */
    public static class PluginExportOperation2
            extends PluginExportOperation {
    
        public PluginExportOperation2( FeatureExportInfo info, String name ) {
            super( info, name );
        }
        
        public String getDestinationDirectory() {
            return fInfo.destinationDirectory;
        }
    }

}
