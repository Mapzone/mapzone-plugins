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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.rap.rwt.RWT;

import org.polymap.core.ui.ColumnLayoutFactory;

import org.polymap.rhei.field.IFormField;
import org.polymap.rhei.field.IFormFieldSite;
import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.form.DefaultFormPage;
import org.polymap.rhei.form.FieldBuilder;
import org.polymap.rhei.form.IFormPageSite;
import org.polymap.rhei.form.IFormToolkit;

import io.mapzone.buildserver.BuildConfig;
import io.mapzone.buildserver.DownloadServlet;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildConfigForm 
        extends DefaultFormPage {

    private BuildConfig         config;
    
    private boolean             created;
    

    public BuildConfigForm( BuildConfig config, boolean created ) {
        this.config = config;
        this.created = created;
    }


    @Override
    public void createFormContents( IFormPageSite site ) {
        super.createFormContents( site );
        
        Composite body = site.getPageBody();
        body.setLayout( ColumnLayoutFactory.defaults().spacing( 5 ).create() );

        // name
        FieldBuilder nameField = site.newFormField( new PropertyAdapter( config.name ) );
        nameField.tooltip.put( "The name of this build configuration.\nSomething like: org.example.test.product_master" );
        if (created) {
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
                .validator.put( new NotEmptyValidator() )
                .create();

        // type
        site.newFormField( new PropertyAdapter( config.type ) )
                .label.put( "Type" )
                .field.put( EnumPicklistFormField.create( BuildConfig.Type.values() ) )
                .validator.put( new NotEmptyValidator() )
                .create();

        // download link
        site.newFormField( new PropertyAdapter( config.downloadPath ) )
                .label.put( "Download" )
                .field.put( new LinkFormField() )
                .validator.put( new NotEmptyValidator() )
                .create();
    }

    
    public class LinkFormField
            implements IFormField {

        private IFormFieldSite          site;

        private CLabel                  link;

        private Object                  loadedValue;

        public void init( IFormFieldSite _site ) {
            this.site = _site;
        }

        public void dispose() {
            link.dispose();
        }

        @Override
        public Control createControl( Composite parent, IFormToolkit toolkit ) {
            link = new CLabel( parent, SWT.LEFT );
            link.setData( RWT.MARKUP_ENABLED, Boolean.TRUE );
            link.setData( RWT.TOOLTIP_MARKUP_ENABLED, Boolean.TRUE );
            //link.setData( MarkupValidator.MARKUP_VALIDATION_DISABLED, Boolean.TRUE );
            link.setBackground( parent.getBackground() );
            return link;
        }

        @Override
        public IFormField setEnabled( boolean enabled ) {
            return this;
        }

        @Override
        public void store() throws Exception {
        }

        @Override
        public void load() throws Exception {
            loadedValue = site.getFieldValue();

            String url = DownloadServlet.ALIAS + "/" + loadedValue + "/" + DownloadServlet.downloadZip( config );
            link.setText( "<a target=\"_blank\" href=\"" + url + "\" "
                    + "style=\"font-size: 14px;\""
                    + ">..." + url + "</a>" );
        }

        @Override
        public IFormField setValue( Object value ) {
            link.setText( value != null ? (String)value : "" );
            return this;
        }

    }

}
