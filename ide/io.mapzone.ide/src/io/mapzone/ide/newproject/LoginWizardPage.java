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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.wizard.WizardPage;

import io.mapzone.ide.MapzoneAPIClient;
import io.mapzone.ide.MapzoneAPIClient.MapzoneProject;
import io.mapzone.ide.MapzoneAPIException;
import io.mapzone.ide.util.FormDataFactory;
import io.mapzone.ide.util.FormLayoutFactory;

/**
 * 
 * 
 * @author Falko Bräutigam
 */
public class LoginWizardPage 
        extends WizardPage {
    
    private ListViewer          projectsList;

    private Text                userText;

    private Text                pwdText;

    private WizardData          wizardData;


    public LoginWizardPage( WizardData wizardData ) {
        super( "projectPage" );
        this.wizardData = wizardData;
        setTitle( "New plug-in project" );
        setDescription( "Login to your mapzone.io account" );
        setPageComplete( false );
    }


    public void createControl( Composite parent ) {
        Composite container = new Composite( parent, SWT.NULL );

        // username / password
        userText = new Text( container, SWT.BORDER );
        userText.setToolTipText( "mapzone.io account name" );
        
        pwdText = new Text( container, SWT.BORDER|SWT.PASSWORD );
        pwdText.setToolTipText( "Password" );
        
        Button loginBtn = new Button( container, SWT.PUSH );
        loginBtn.setText( "Login" );
        loginBtn.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent ev ) {
                doLogin();
            }
        });
        
        // label
        Label label = new Label( container, SWT.NULL );
        label.setText( "Choose a mapzone.io project to connect the new IDE project with" );

        // projectsList
        projectsList = new ListViewer( container, SWT.BORDER|SWT.SINGLE|SWT.V_SCROLL );
        projectsList.setContentProvider( ArrayContentProvider.getInstance() );
        projectsList.setLabelProvider( new LabelProvider() {
            @Override
            public String getText( Object elm ) {
                return ((MapzoneProject)elm).name();
            }
        });
        projectsList.addSelectionChangedListener( ev -> {
            wizardData.mapzoneProject = (MapzoneProject)projectsList.getStructuredSelection().getFirstElement();
            updateStatus();
        });

        // layout
        container.setLayout( FormLayoutFactory.defaults().spacing( 8 ).create() );
        FormDataFactory.on( userText ).fill().noBottom();
        FormDataFactory.on( pwdText ).fill().top( userText ).noBottom();
        FormDataFactory.on( loginBtn ).fill().top( pwdText ).noBottom();
        FormDataFactory.on( label ).fill().top( loginBtn ).noBottom();
        FormDataFactory.on( projectsList.getControl() ).fill().top( label, -5 );

        setControl( container );
    }

    
    protected void doLogin() {
        try {
            String username = userText.getText();
            MapzoneAPIClient service = new MapzoneAPIClient( "localhost", 8090, username, pwdText.getText() );
            List<MapzoneProject> projects = service.findProjects( username );
            
            projectsList.setInput( projects );
            updateStatus();
            
            wizardData.mapzoneClient = service;
        }
        catch (MapzoneAPIException e) {
            setErrorMessage( e.getLocalizedMessage() );
            setPageComplete( false );
        }
    }
    
    
    protected void updateStatus() {
        String error = null;
        String description = null;
        boolean complete = true;
        
        if (wizardData.mapzoneClient == null) {
            complete = false;
        }
        else if (wizardData.mapzoneProject == null) {
            description = "Choose a project to connect to.";
            complete = false;
        }
        
        setErrorMessage( error );
        setDescription( description );
        setPageComplete( complete );
    }

}
