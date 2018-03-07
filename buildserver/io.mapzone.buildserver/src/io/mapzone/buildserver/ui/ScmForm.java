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

import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.field.VerticalFieldLayout;
import org.polymap.rhei.form.DefaultFormPage;
import org.polymap.rhei.form.IFormPageSite;

import io.mapzone.buildserver.BuildConfig.ScmConfig;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class ScmForm 
        extends DefaultFormPage {

    private ScmConfig        config;
    

    public ScmForm( ScmConfig config ) {
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
                .field.put( EnumPicklistFormField.create( ScmConfig.Type.values() ) )
                .fieldEnabled.put( false )
                .create()
                .setLayoutData( ColumnDataFactory.defaults().widthHint( 350 ).create() );

        // url
        site.newFormField( new PropertyAdapter( config.url ) )
                .label.put( "URL" )
                .tooltip.put( "The URL of this entry, depending on the type" )
                .validator.put( new NotEmptyValidator() )
                .create()
                .forceFocus();

        // branch
        site.newFormField( new PropertyAdapter( config.branch ) )
                .label.put( "Branch" )
                .tooltip.put( "The branch to work with" )
                .validator.put( new NotEmptyValidator() )
                .create();
    }
    
}
