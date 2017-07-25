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
package org.polymap.tutorial.importer.simple;

import java.util.concurrent.atomic.AtomicReference;

import java.io.File;

import org.geotools.data.DataStore;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.type.FeatureType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.catalog.IUpdateableMetadataCatalog.Updater;
import org.polymap.core.catalog.resolve.IMetadataResourceResolver;
import org.polymap.core.catalog.resolve.IResourceInfo;
import org.polymap.core.catalog.resolve.IServiceInfo;
import org.polymap.core.data.shapefile.catalog.ShapefileServiceResolver;
import org.polymap.core.operation.OperationSupport;
import org.polymap.core.project.IMap;
import org.polymap.core.style.DefaultStyle;
import org.polymap.core.style.model.FeatureStyle;

import org.polymap.rhei.batik.BatikApplication;
import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.Mandatory;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;

import org.polymap.p4.P4Plugin;
import org.polymap.p4.catalog.AllResolver;
import org.polymap.p4.data.importer.ContextIn;
import org.polymap.p4.data.importer.ContextOut;
import org.polymap.p4.data.importer.Importer;
import org.polymap.p4.data.importer.ImporterPrompt;
import org.polymap.p4.data.importer.ImporterPrompt.PromptUIBuilder;
import org.polymap.p4.data.importer.ImporterPrompt.Severity;
import org.polymap.p4.data.importer.ImporterSite;
import org.polymap.p4.layer.NewLayerOperation;
import org.polymap.p4.project.ProjectRepository;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class SimpleImporter
        implements Importer {

    private static final Log log = LogFactory.getLog( SimpleImporter.class );

    @ContextIn
    private File                file;

    @ContextOut
    private FeatureCollection   features;
    
    private ImporterSite        site;

    private ImporterPrompt      textPrompt;

    private Exception           exc;


    @Override
    public void init( ImporterSite newSite, IProgressMonitor monitor ) throws Exception {
        this.site = newSite;
        
        site.summary.set( "Simple file: " + file.getName() );
        site.description.set( "Simple noop tutorial importer" );
    }


    @Override
    public ImporterSite site() {
        return site;
    }


    @Override
    public void createPrompts( IProgressMonitor monitor ) throws Exception {
        // prompt for a text to input ("ok")
        textPrompt = site.newPrompt( "test" )
                .summary.put( "A text (must be 'ok')" )
                .description.put( "The value must be 'ok'" )
                .value.put( "not ok" )
                .severity.put( Severity.VERIFY )  // user should check and CAN modify
                .extendedUI.put( new PromptUIBuilder() {
                    private String value = "not ok";
                    @Override
                    public void submit( ImporterPrompt prompt ) {
                        prompt.ok.set( true );
                        prompt.value.set( value );                        
                    }
                    @Override
                    public void createContents( ImporterPrompt prompt, Composite parent, IPanelToolkit tk ) {
                        Text text = tk.createText( parent, value, SWT.BORDER );
                        text.addModifyListener( ev -> value = text.getText() );
                    }
                });

    }


    @Override
    public void verify( IProgressMonitor monitor ) {
        try {
            // do some checks and report result
            site.ok.set( "ok".equals( textPrompt.value.get() ) );
            exc = null;
        }
        catch (Exception e) {
            exc = e;
            site.ok.set( false );
        }
    }


    @Override
    public void createResultViewer( Composite parent, IPanelToolkit tk ) {
        if (exc != null) {
            tk.createFlowText( parent,
                    "\nUnable to read the data.\n\n" +
                    "**Reason**: " + exc.getLocalizedMessage() );            
        }
        else {
            tk.createFlowText( parent, "Preview..." );            
        }
    }


    @Override
    public void execute( IProgressMonitor monitor ) throws Exception {        
        throw new RuntimeException( "not yet implemented." );
        
        // choose one of the methods below
    }
    
    
    /**
     * The most simple way to import one single FeatureCollection is to just give it
     * to a {@link ContextOut} member. The framework will scan for it and copy the
     * features to the internal database. It also asks to user to create a new layer
     * for it.
     */
    protected void importFeatureCollection() {
        //this.features = ...
    }
    

    /**
     * Directly link a data source represented by an {@link IServiceInfo} and connected by
     * an {@link IMetadataResourceResolver} to the local catalog. No data is actually transfered.
     */
    protected void importCatalogEntry( IProgressMonitor monitor ) {
        // just an example, wherever this comes from
        DataStore ds = null;
        AtomicReference<IResourceInfo> resource = new AtomicReference();
        
        // create catalog entry
        try (
            Updater update = P4Plugin.localCatalog().prepareUpdate()
        ){
            update.newEntry( metadata -> {
                metadata.setTitle( "..." );
                metadata.setDescription( "..." );
                metadata.setType( "..." );
                metadata.setFormats( Sets.newHashSet( "..." ) );
                
                // actual connection to the data source; just an example
                metadata.setConnectionParams( ShapefileServiceResolver.createParams( "file://..." ) );

                // resolve the new data source, testing the connection params
                // and choose resource to create a new layer for
                try {
                    IServiceInfo serviceInfo = (IServiceInfo)AllResolver.instance().resolve( metadata, monitor );
                    resource.set( FluentIterable.from( serviceInfo.getResources( monitor ) ).first().get() );
                }
                catch (Exception e) {
                    throw new RuntimeException( "Unable to resolve imported data source.", e );
                }
            });
            update.commit();
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
        
        // create new layer(s) for resource(s)        
        FeatureType schema = null;
        FeatureStyle featureStyle = P4Plugin.styleRepo().newFeatureStyle();
        DefaultStyle.create( featureStyle, schema );

        BatikApplication.instance().getContext().propagate( this );
        NewLayerOperation op = new NewLayerOperation()
                .label.put( "New layer" )
                .res.put( resource.get() )
                .featureStyle.put( featureStyle )
                .uow.put( ProjectRepository.unitOfWork() )
                .map.put( map.get() );

        OperationSupport.instance().execute( op, true, false );
    }

    
    /** Only required for {@link #importCatalogEntry()}. */
    @Mandatory
    @Scope(P4Plugin.Scope)
    protected Context<IMap>     map;

}
