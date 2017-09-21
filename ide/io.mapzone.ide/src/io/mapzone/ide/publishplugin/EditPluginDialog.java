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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.dialogs.TitleAreaDialog;

import io.mapzone.ide.apiclient.PluginEntity;

/**
 * Edit the properties of a {@link PluginEntity}.
 *
 * @deprecated As of {@link EditPluginDescriptionPage}
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class EditPluginDialog
        extends TitleAreaDialog {

    private PluginEntity        pluginEntity;
    
    
    public EditPluginDialog( PluginEntity pluginEntity ) {
        super( Display.getCurrent().getActiveShell() );
        this.pluginEntity = pluginEntity;
    }
    
    @Override
    public void create() {
        super.create();
        setTitle( "Publish plugin" );
        setMessage( "Publish this plugin on mapzone.io");
    }
    
    @Override
    protected Control createDialogArea( Composite parent ) {
        //Composite container = (Composite)super.createDialogArea( parent );
        Composite container = new Composite( parent, SWT.NULL );
        container.setLayoutData( new GridData( GridData.FILL_BOTH ) );
//
//        InputForm form = new InputForm( container );
//        ((FormLayout)container.getLayout()).marginHeight = 8;
//        ((FormLayout)container.getLayout()).marginWidth = 8;
//        
//        form.createLabel( "Name", pluginEntity.id.get() );
//
//        form.createText( "Title", pluginEntity.title.newObservable() );
//        
//        Text description = form.createText( "Description", pluginEntity.description, SWT.MULTI );
//        description.setToolTipText( "The description of the plugin. May contain markdown." );
//        FormDataFactory.on( description ).noBottom().height( 75 );
//        
//        form.createText( "Project URL", pluginEntity.projectUrl );
//        
//        form.createText( "Vendor", pluginEntity.vendor );
//        
//        form.createText( "Vendor URL", pluginEntity.vendorUrl );
//        
        return container;
    }

}
