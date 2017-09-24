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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.wizard.WizardPage;

import io.mapzone.ide.apiclient.PluginEntity;
import io.mapzone.ide.util.FormDataFactory;
import io.mapzone.ide.util.InputForm;
import io.mapzone.ide.util.NotEmptyValidator;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class EditPluginDescriptionPage
        extends WizardPage {

    private WizardData          data;
    
    private InputForm           form;
    
    
    protected EditPluginDescriptionPage( WizardData data ) {
        super( "editPage" );
        setTitle( "Publish Plugin-in" );
        setDescription( "Edit Plugin-in description" );
        setPageComplete( true );
        this.data = data;
    }

    
    @Override
    public void dispose() {
        super.dispose();
        if (form != null) {
            form.dispose();
        }
    }


    @Override
    public void createControl( Composite parent ) {
        Composite container = new Composite( parent, SWT.NULL );
        setControl( container );

        form = new InputForm( container );
        ((FormLayout)container.getLayout()).marginHeight = 8;
        ((FormLayout)container.getLayout()).marginWidth = 8;
        
        try {
            PluginEntity entity = data.publishedPlugin.entity();
            form.createLabel( "Name", entity.id.get() );

            form.createText( "Title", SWT.BORDER, entity.title.newObservable(), new NotEmptyValidator() );
            
            Text description = form.createText( "Description", SWT.BORDER|SWT.MULTI|SWT.WRAP, 
                    entity.description.newObservable(), new NotEmptyValidator() );
            description.setToolTipText( "The description of the plugin. May contain markdown." );
            FormDataFactory.on( description ).noBottom().height( 75 ).width( 300 );
            
            form.createText( "Project URL", SWT.BORDER, entity.projectUrl.newObservable(), null );
            
            form.createText( "Vendor", SWT.BORDER, entity.vendor.newObservable(), null );
            
            form.createText( "Vendor URL", SWT.BORDER, entity.vendorUrl.newObservable(), null );
            
            Text feeTxt = form.createText( "Fee (not yet supported)", "0", SWT.BORDER ); //, entity.fee.newObservable(), null );
            feeTxt.setEnabled( false );

//            Button freeBtn = form.createCheckbox( "Free of charge", data.exportPlugin, SWT.BORDER );
//            freeBtn.setToolTipText( "Currently all plugins are free. Later we will add payment." );
//            freeBtn.setEnabled( false );

            Button exportBtn = form.createCheckbox( "Export a new version of the plugin", data.exportPlugin, SWT.BORDER );
            exportBtn.setToolTipText( "Build a new version of the plugin binary and send it to the catalog" );
            exportBtn.addSelectionListener( new SelectionAdapter() {
                @Override public void widgetSelected( SelectionEvent ev ) {
                    data.exportPlugin = exportBtn.getSelection();
                }
            });

            form.addAllChangeListener( ev -> {
                container.getDisplay().asyncExec( () -> { // delay after validation
                    setPageComplete( form.isValid() );
                });
            });
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }
    
}
