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

import com.google.common.base.Throwables;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.wizard.WizardPage;

import io.mapzone.ide.apiclient.MapzoneAPIClient;
import io.mapzone.ide.util.InputForm;

/**
 * 
 * 
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class LoginPage
        extends WizardPage {

    private WizardData          data;
    
    private InputForm           form;
    
    private Text                userText;

    private Text                projectText;

    private Text                pwdText;

    private MapzoneAPIClient    apiClient;


    protected LoginPage( WizardData data ) {
        super( "loginPage" );
        setTitle( "Publish Plugin-in" );
        setDescription( "Login to your mapzone.io account" );
        setPageComplete( false );
        this.data = data;
    }

    
    @Override
    public void createControl( Composite parent ) {
        Composite container = new Composite( parent, SWT.NULL );
        form = new InputForm( container );
        
        userText = form.createText( "Account name", data.mproject.username() );
        userText.setToolTipText( "mapzone.io account name" );

        pwdText = form.createText( "Password", "", SWT.PASSWORD );
        pwdText.setToolTipText( "The password of the given mapzone.io account" );
        pwdText.forceFocus();
        pwdText.addModifyListener( ev -> {
            try {
                apiClient = data.mproject.connectServer( userText.getText(), pwdText.getText() );
                setPageComplete( true );
                setErrorMessage( null );
            }
            catch (Exception e) {
                setPageComplete( false );
                setErrorMessage( Throwables.getRootCause( e ).getMessage() );
            }
        });

        setControl( container );
    }
    
}