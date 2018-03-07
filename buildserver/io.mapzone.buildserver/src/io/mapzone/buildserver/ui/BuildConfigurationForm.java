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
import org.polymap.core.ui.ColumnLayoutFactory;

import org.polymap.rhei.field.FormFieldEvent;
import org.polymap.rhei.field.IFormFieldListener;
import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.form.DefaultFormPage;
import org.polymap.rhei.form.FieldBuilder;
import org.polymap.rhei.form.IFormPageSite;

import io.mapzone.buildserver.BuildConfig;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildConfigurationForm 
        extends DefaultFormPage 
        implements IFormFieldListener {

    private BuildConfig      config;
    
    private boolean                 creation;
    

    public BuildConfigurationForm( BuildConfig config, boolean creation ) {
        this.config = config;
        this.creation = creation;
    }


    @Override
    public void createFormContents( IFormPageSite site ) {
        super.createFormContents( site );
        
        Composite body = site.getPageBody();
        body.setLayout( ColumnLayoutFactory.defaults().spacing( 5 ).create() );
        //site.setDefaultFieldLayout( VerticalFieldLayout.INSTANCE );

        // name
        FieldBuilder nameField = site.newFormField( new PropertyAdapter( config.name ) );
        nameField.tooltip.put( "The name of this build configuration.\nSomething like: org.example.test.product_master" );
        nameField.fieldEnabled.put( creation );
        if (creation) {
            nameField.validator.put( new NotEmptyValidator<String,String>() {
                @Override
                public String validate( String fieldValue ) {
                    String result = super.validate( fieldValue );
                    if (result == null) {
                    }
                    return result;
                }
            });
        }
        nameField.create();

        // productName
        site.newFormField( new PropertyAdapter( config.productName ) )
                .label.put( "Product" )
                .tooltip.put( "The symbolic name of the product to build" )
                .fieldEnabled.put( creation )
                .validator.put( new NotEmptyValidator() )
                .create();

        // type
        site.newFormField( new PropertyAdapter( config.type ) )
                .label.put( "Type" )
                .fieldEnabled.put( creation )
                .validator.put( new NotEmptyValidator() )
                .create();
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
