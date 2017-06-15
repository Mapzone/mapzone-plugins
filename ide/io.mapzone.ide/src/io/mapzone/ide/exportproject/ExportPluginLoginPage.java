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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.wizard.WizardPage;

import io.mapzone.ide.util.InputForm;

/**
 * 
 * 
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class ExportPluginLoginPage
        extends WizardPage {

    private WizardData      data;
    
    private InputForm       form;
    
    private Text            userText;

    private Text            projectText;

    private Text            pwdText;


    protected ExportPluginLoginPage( WizardData data ) {
        super( "export1Page" );
        setTitle( "Login" );
        setDescription( "Install plugin-in in project on mapzone.io" );
        setPageComplete( false );
        this.data = data;
    }

    @Override
    public void createControl( Composite parent ) {
        Composite container = new Composite( parent, SWT.NULL );
        form = new InputForm( container );
        
        userText = form.createText( "Account name", data.mproject.username() );
        userText.setEnabled( false );
        userText.setToolTipText( "mapzone.io account name" );

        projectText = form.createText( "Project", data.mproject.projectname() );
        projectText.setEnabled( false );
        projectText.setToolTipText( "mapzone.io project name" );

        pwdText = form.createText( "Password", null, SWT.PASSWORD );
        pwdText.setToolTipText( "The password of the given mapzone.io account" );
        pwdText.addModifyListener( ev -> {
            try {
                data.mproject.connect( pwdText.getText() );
                setPageComplete( true );
                setErrorMessage( null );
            }
            catch (Exception e) {
                setPageComplete( false );
                setErrorMessage( e.getMessage() );
            }
        });

        setControl( container );
    }
    
}