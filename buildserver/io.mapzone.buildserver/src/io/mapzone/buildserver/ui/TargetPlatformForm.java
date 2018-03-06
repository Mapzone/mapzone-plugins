/* 
 * polymap.org
 * Copyright (C) 2018, the @authors. All rights reserved.
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
package io.mapzone.buildserver.ui;

import org.eclipse.swt.widgets.Composite;

import org.polymap.core.ui.ColumnDataFactory;
import org.polymap.core.ui.ColumnLayoutFactory;

import org.polymap.rhei.field.FormFieldEvent;
import org.polymap.rhei.field.IFormFieldListener;
import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.field.VerticalFieldLayout;
import org.polymap.rhei.form.DefaultFormPage;
import org.polymap.rhei.form.IFormPageSite;

import io.mapzone.buildserver.BuildConfiguration.TargetPlatformConfiguration;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class TargetPlatformForm 
        extends DefaultFormPage 
        implements IFormFieldListener {

    private TargetPlatformConfiguration config;
    

    public TargetPlatformForm( TargetPlatformConfiguration config ) {
        this.config = config;
    }


    @Override
    public void createFormContents( IFormPageSite site ) {
        super.createFormContents( site );
        
        Composite body = site.getPageBody();
        body.setLayout( ColumnLayoutFactory.defaults().spacing( 5 ).create() );
        site.setDefaultFieldLayout( VerticalFieldLayout.INSTANCE );

        // type
        site.newFormField( new PropertyAdapter( config.type ) )
                .label.put( "Type" )
                .validator.put( new NotEmptyValidator() )
                .create();

        // url
        site.newFormField( new PropertyAdapter( config.url ) )
                .label.put( "URL" )
                .tooltip.put( "The URL of this entry, depending on the type" )
                .validator.put( new NotEmptyValidator() )
                .create()
                .setLayoutData( ColumnDataFactory.defaults().widthHint( 350 ).create() );
    }
    

    @Override
    public void fieldChange( FormFieldEvent ev ) {
        if (ev.getEventCode() == VALUE_CHANGE) {
        }
    }


    protected void updateEnabled() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

}
