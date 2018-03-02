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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.form.batik.BatikFormContainer;
import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfiguration;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildConfigurationPanel
        extends BsPanel {

    private static final Log log = LogFactory.getLog( BuildConfigurationPanel.class );

    public static final PanelIdentifier     ID = PanelIdentifier.parse( "buildconfig" );

    /**
     * Inbound: The config the work with.
     */
    @Scope( BsPlugin.ID )
    protected Context<BuildConfiguration>   config;

    private BatikFormContainer              form;
    
    
    @Override
    public void init() {
        super.init();
        site().title.set( "Build configuration" );
    }


    @Override
    public void createContents( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 5 ).margins( 0, 8 ).create() );

        IPanelSection section = tk().createPanelSection( parent, "Configuration", SWT.BORDER );
        createMainSection( section.getBody() );
        Composite btnContainer = tk().createComposite( parent );
        createBuildButton( btnContainer );
        IPanelSection resultsSection = tk().createPanelSection( parent, "Results", SWT.BORDER );
        createResultsSection( section.getBody() );
        
        FormDataFactory.on( section.getControl() ).fill().noBottom();
        FormDataFactory.on( btnContainer ).fill().top( section.getControl() ).noBottom();
        FormDataFactory.on( resultsSection.getControl() ).fill().top( btnContainer );
    }


    protected void createMainSection( Composite parent ) {
        BuildConfigurationForm formPage = new BuildConfigurationForm( config.get(), false ) {
            @Override
            protected void updateEnabled() {
                //ProjectInfoPanel.this.updateEnabled();
            }
        };
        form = new BatikFormContainer( formPage );
        form.createContents( parent );
    }


    protected void createResultsSection( Composite body ) {
    }
    
    
    protected void createBuildButton( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 5 ).margins( 0, 3 ).create() );        
        Button btn = tk().createButton( parent, "Start Build", SWT.PUSH );
        btn.setImage( BsPlugin.images().svgImage( "play-circle-outline.svg", SvgImageRegistryHelper.WHITE24 ) );
        FormDataFactory.on( btn ).fill().left( 30 ).right( 70 );
    }
    
}
