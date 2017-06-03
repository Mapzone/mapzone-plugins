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

import java.util.HashMap;
import java.util.Map;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.wizards.plugin.NewPluginProjectWizard;

import io.mapzone.ide.IdePlugin;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
@SuppressWarnings( "restriction" )
public class NewPluginProjectWizard2
        extends NewPluginProjectWizard {

    protected WizardData        wizardData = new WizardData( this );
    
    
    public NewPluginProjectWizard2() {
        super();
    }

    public NewPluginProjectWizard2( String osgiFramework ) {
        super( osgiFramework );
    }

    
    @Override
    public void addPages() {
        addPage( new LoginWizardPage( wizardData ) );
        super.addPages();
    }


    public ElementList getAvailableCodegenWizards() {
        // suppress default wizards; add our templates
        ElementList wizards = new ElementList( "CodegenWizards" );
//        wizards.add( new WizardElement( new FakeConfigElement()
//                .addChild( WizardElement.TAG_DESCRIPTION, "Description" )
//                .addAttribute( WizardElement.ATT_NAME, "Fake" )
//                .addAttribute( WizardElement.ATT_ID, "fake.id" )
//                .addAttribute( WizardElement.ATT_CLASS, NewPluginContentWizard.class.getName() ) ) );
        return wizards;
    }

    
    @Override
    public boolean performFinish() {
        try {
            // check/create target platform
            getContainer().run( true, true, monitor -> {
                try {
                    monitor.beginTask( "Target platform", 10 );
                    String targetName = getPluginId() + ".target";
                    File bundlesDir = new File( "/tmp", targetName );
                    bundlesDir.mkdir();
                    
                    wizardData.mapzoneProject.downloadBundles( bundlesDir, submon( monitor, 8 ) );
                    TargetPlatformHelper.instance().create( targetName, bundlesDir, true, submon( monitor, 2 ) );
                    monitor.done();
                }
                catch (Exception e) {
                    throw new InvocationTargetException( e );
                }
            });
            
            // create project
            boolean success = super.performFinish();
            
//            // adapt project
//            if (success) {
//                getContainer().run( true, true, new WorkspaceModifyOperation() {
//                    @Override
//                    protected void execute( IProgressMonitor monitor ) throws CoreException, InvocationTargetException, InterruptedException {
//                        IPluginReference[] dependencies = getDependencies();
//                        for (int i = 0; i < dependencies.length; i++) {
//                            IPluginReference ref = dependencies[i];
//                            IPluginImport iimport = fModel.getPluginFactory().createImport();
//                            iimport.setId(ref.getId());
//                            iimport.setVersion(ref.getVersion());
//                            iimport.setMatch(ref.getMatch());
//                            pluginBase.add(iimport);
//                        }
//                    }
//                });            
//            }
            return success;
        }
        catch (InvocationTargetException e) {
            PDEPlugin.logException( e.getTargetException() );
        }
        catch (InterruptedException e) {
            PDEPlugin.logException( e );
        }
        return false;
    }


    @SuppressWarnings( "deprecation" )
    protected IProgressMonitor submon( IProgressMonitor monitor, int ticks ) {
        return new SubProgressMonitor( monitor, ticks );
    }


    class FakeConfigElement 
            implements IConfigurationElement {
    
        private Map<String,String>      attributes = new HashMap();
        
        private Map<String,String>      children = new HashMap();
        
        public FakeConfigElement addAttribute( String k, String v ) {
            attributes.put( k, v );
            return this;
        }
        
        public FakeConfigElement addChild( String k, String v ) {
            attributes.put( k, v );
            return this;
        }
        
        @Override
        public Object createExecutableExtension( String propertyName ) throws CoreException {
            try {
                String classname = attributes.get( propertyName );
                Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass( classname );
                return cl.newInstance();
            }
            catch (Exception e) {
                throw new RuntimeException( e );
            }
        }
        @Override
        public String getAttribute( String name ) throws InvalidRegistryObjectException {
            return attributes.get( name );
        }
        @Override
        public String getAttribute( String attrName, String locale ) throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public String getAttributeAsIs( String name ) throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public String[] getAttributeNames() throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public IConfigurationElement[] getChildren() throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public IConfigurationElement[] getChildren( String name ) throws InvalidRegistryObjectException {
            return new IConfigurationElement[] {};
        }
        @Override
        public IExtension getDeclaringExtension() throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public String getName() throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public Object getParent() throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public String getValue() throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public String getValue( String locale ) throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public String getValueAsIs() throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public String getNamespace() throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public String getNamespaceIdentifier() throws InvalidRegistryObjectException {
            return IdePlugin.ID;
        }
        @Override
        public IContributor getContributor() throws InvalidRegistryObjectException {
            throw new RuntimeException( "not yet implemented." );
        }
        @Override
        public boolean isValid() {
            throw new RuntimeException( "not yet implemented." );
        }
    }
    
}
