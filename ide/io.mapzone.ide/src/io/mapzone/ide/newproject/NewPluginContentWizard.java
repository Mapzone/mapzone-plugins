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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.ui.templates.PDETemplateSection;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.pde.ui.templates.AbstractNewPluginTemplateWizard;
import org.eclipse.pde.ui.templates.ITemplateSection;


/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
@SuppressWarnings( "restriction" )
public class NewPluginContentWizard
        extends AbstractNewPluginTemplateWizard
        implements IPluginContentWizard {

    @Override
    protected void addAdditionalPages() {
    }

    @Override
    public ITemplateSection[] getTemplateSections() {
        return new ITemplateSection[] { new PDETemplateSection() {
            
            @Override
            public String getUsedExtensionPoint() {
                // XXX Auto-generated method stub
                throw new RuntimeException( "not yet implemented." );
            }
            
            @Override
            protected void updateModel( IProgressMonitor monitor ) throws CoreException {
                // XXX Auto-generated method stub
                throw new RuntimeException( "not yet implemented." );
            }
            
            @Override
            public String getSectionId() {
                // XXX Auto-generated method stub
                throw new RuntimeException( "not yet implemented." );
            }
        }};
    }

}
