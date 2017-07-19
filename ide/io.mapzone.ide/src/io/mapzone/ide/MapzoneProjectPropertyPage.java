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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;

import io.mapzone.ide.util.InputForm;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class MapzoneProjectPropertyPage
        extends PropertyPage //FieldEditorPreferencePage
        implements IWorkbenchPropertyPage {

    private Text            userText;
    
    private Text            projectText;


    @Override
    protected Control createContents( Composite parent ) {
        Composite container = new Composite( parent, SWT.NULL );
        InputForm form = new InputForm( container );
        
        MapzonePluginProject mproject = MapzonePluginProject.of( project() );

        // hostname
        form.createLabel( "Host", mproject.hostname() );

        // username
        userText = form.createText( "Account name", mproject.username() );
        userText.setEditable( false );
        userText.setToolTipText( "mapzone.io account name" );

        projectText = form.createText( "Project", mproject.projectname() );
        projectText.setEditable( false );
        projectText.setToolTipText( "mapzone.io project name" );

        return container;
    }
    
    
    protected IProject project() {
        final IAdaptable adaptable = getElement();
        return adaptable == null ? null : (IProject) adaptable.getAdapter(IProject.class);
    }
    
}
