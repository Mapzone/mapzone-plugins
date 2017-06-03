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
package io.mapzone.ide.newproject;

import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

import io.mapzone.ide.IdePlugin;

/**
 * 
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class ProjectNewWizard 
        extends Wizard 
        implements INewWizard {
    
    private LoginWizardPage         loginPage;
    
    private TargetPlatformWizardPage platformPage;
    
    private ISelection              selection;
    
    private State                   state;

    /**
     * The state of the wizard, filled by the pages and input of #doFinish().  
     */
    protected class State {
        
        public String               projectName;
    }
    
    
    public ProjectNewWizard() {
        setNeedsProgressMonitor( true );
    }

    
    @Override
    public void init( IWorkbench workbench, @SuppressWarnings( "hiding" ) IStructuredSelection selection ) {
        this.selection = selection;
    }


    @Override
    public void addPages() {
        addPage( loginPage = new LoginWizardPage() );
        addPage( platformPage = new TargetPlatformWizardPage() );
    }

    
    @Override
    public boolean performFinish() {
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run( IProgressMonitor monitor ) throws InvocationTargetException {
                try {
                    doFinish( monitor );
                } 
                catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } 
                finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run( true, false, op );
        } 
        catch (InterruptedException e) {
            return false;
        } 
        catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError( getShell(), "Error", realException.getMessage() );
            return false;
        }
        return true;
    }
    

    protected void doFinish( IProgressMonitor monitor ) throws CoreException {
        monitor.beginTask( "Creating " + state.projectName, 10 );
        
        // create project
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = workspaceRoot.getProject( state.projectName );
        project.create( submon( monitor, 1 ) );
        project.open( submon( monitor, 1 ) );
        
        // add Java nature
        IProjectDescription description = project.getDescription();
        description.setNatureIds( new String[] { JavaCore.NATURE_ID });
        project.setDescription( description, submon( monitor, 1 ) );
        IJavaProject javaProject = JavaCore.create( project );
        
        // bin
        IFolder binFolder = project.getFolder( "bin" );
        binFolder.create( false, true, submon( monitor, 1 ) );
        javaProject.setOutputLocation( binFolder.getFullPath(), submon( monitor, 1 ) );

        // classpath: Java runtime libs
        List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
        IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
        LibraryLocation[] locations = JavaRuntime.getLibraryLocations( vmInstall );
        for (LibraryLocation element : locations) {
            entries.add( JavaCore.newLibraryEntry( element.getSystemLibraryPath(), null, null ) );
        }
        
        // src
        IFolder sourceFolder = project.getFolder( "src" );
        sourceFolder.create( false, true, submon( monitor, 1 ) );
        
        // classpath
        IPackageFragmentRoot root = javaProject.getPackageFragmentRoot( sourceFolder );
        IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
        IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
        System.arraycopy( oldEntries, 0, newEntries, 0, oldEntries.length );
        newEntries[oldEntries.length] = JavaCore.newSourceEntry( root.getPath() );
        javaProject.setRawClasspath( newEntries, null );
        
        //
        IPackageFragment pack = javaProject.getPackageFragmentRoot(sourceFolder).createPackageFragment(packageName, false, null);
        StringBuffer buffer = new StringBuffer();
        buffer.append("package " + pack.getElementName() + ";\n");
        buffer.append("\n");
        buffer.append(source);
        ICompilationUnit cu = pack.createCompilationUnit(className, buffer.toString(), false, null);
        
        //add libs to project class path
        javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), null);
        
        monitor.worked(1);
        monitor.setTaskName("Opening file for editing...");
        monitor.worked(1);
    }

    
    protected void fillProject( IJavaProject javaProject ) {
        
    }
    
    protected IProgressMonitor submon( IProgressMonitor monitor, int ticks ) {
        return new SubProgressMonitor( monitor, ticks );
    }

    
    protected void throwCoreException( String msg ) throws CoreException {
        throw new CoreException( new Status( IStatus.ERROR, IdePlugin.ID, IStatus.OK, msg, null) );
    }
    
}