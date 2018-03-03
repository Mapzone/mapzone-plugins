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

import java.util.stream.Collectors;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.ViewerCell;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import org.polymap.core.runtime.Polymap;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.Mandatory;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.md.ActionProvider;
import org.polymap.rhei.batik.toolkit.md.FunctionalLabelProvider;
import org.polymap.rhei.batik.toolkit.md.ListTreeContentProvider;
import org.polymap.rhei.batik.toolkit.md.MdListViewer;
import org.polymap.rhei.form.batik.BatikFormContainer;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfiguration;
import io.mapzone.buildserver.BuildManager;
import io.mapzone.buildserver.BuildManager.BuildProcess;
import io.mapzone.buildserver.BuildResult;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildConfigurationPanel
        extends BsPanel {

    private static final Log log = LogFactory.getLog( BuildConfigurationPanel.class );

    public static final PanelIdentifier     ID = PanelIdentifier.parse( "buildconfig" );

    private DateFormat                      df = SimpleDateFormat.getDateInstance( DateFormat.LONG, Polymap.getSessionLocale() );

    private DateFormat                      tf = SimpleDateFormat.getTimeInstance( DateFormat.DEFAULT, Polymap.getSessionLocale() );
    
    /**
     * Inbound: The config the work with.
     */
    @Mandatory
    @Scope( BsPlugin.ID )
    protected Context<BuildConfiguration>   config;

    /**
     * Outbound:
     */
    @Scope( BsPlugin.ID )
    protected Context<BuildResult>          buildResult;
    
    private BatikFormContainer              form;

    private BuildManager                    buildManager;

    private MdListViewer                    resultsList;
    
    
    @Override
    public void init() {
        super.init();
        site().title.set( "Build Configuration" );
        buildManager = BuildManager.of( config.get() );
    }


    @Override
    public void createContents( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 5 ).margins( 3, 8 ).create() );

        IPanelSection section = tk().createPanelSection( parent, "Configuration", SWT.BORDER );
        createMainSection( section.getBody() );
        Composite btnContainer = tk().createComposite( parent );
        createBuildButton( btnContainer );
        IPanelSection resultsSection = tk().createPanelSection( parent, "Results", SWT.NONE );
        createResultsSection( resultsSection.getBody() );
        
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


    protected void createBuildButton( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 5 ).margins( 0, 3 ).create() );        
        Button btn = tk().createButton( parent, "Build Now", SWT.PUSH );
        btn.setImage( BsPlugin.images().svgImage( "play-circle-outline.svg", SvgImageRegistryHelper.WHITE24 ) );
    
        btn.setEnabled( !buildManager.running().isPresent() );
        btn.addSelectionListener( UIUtils.selectionListener( ev -> {
            btn.setEnabled( false );
    
            BuildProcess job = buildManager.startNewBuild();
            job.addJobChangeListener( new JobChangeAdapter() {
                @Override public void running( IJobChangeEvent ev2 ) {
                    if (!btn.isDisposed()) {
                        btn.getDisplay().asyncExec( () -> {
                            refreshResultsList();
                        });
                    }
                }
                @Override public void done( IJobChangeEvent ev2 ) {
                    if (!btn.isDisposed()) {
                        btn.getDisplay().asyncExec( () -> {
                            btn.setEnabled( true );
                            refreshResultsList();
                        });
                    }
                }
            });
        }));
        FormDataFactory.on( btn ).fill().left( 30 ).right( 70 );
    }


    protected void createResultsSection( Composite parent ) {
        resultsList = tk().createListViewer( parent, SWT.FULL_SELECTION, SWT.SINGLE );
        resultsList.firstLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            BuildResult elm = (BuildResult)cell.getElement();
            cell.setText( df.format( elm.started.get() ) + "   " + tf.format( elm.started.get() ) );
        }));
        resultsList.firstSecondaryActionProvider.set( new ActionProvider() {
            @Override public void update( ViewerCell cell ) {
                cell.setImage( BsPlugin.images().svgImage( "chevron-right.svg", SvgImageRegistryHelper.NORMAL24 ) );
            }
            @Override public void perform( MdListViewer viewer, Object elm ) {
            }
        });
        resultsList.iconProvider.set( FunctionalLabelProvider.of( cell -> {
            BuildResult elm = (BuildResult)cell.getElement();
            switch (elm.status.get()) {
                case RUNNING: cell.setImage( BsPlugin.images().svgImage( "run.svg", SvgImageRegistryHelper.NORMAL24 ) ); break;
                case FAILED: cell.setImage( BsPlugin.images().svgImage( "alert.svg", SvgImageRegistryHelper.ERROR24 ) ); break;
                case OK: cell.setImage( BsPlugin.images().svgImage( "check.svg", SvgImageRegistryHelper.OK24 ) ); break;
            }
        }));
        resultsList.addOpenListener( ev -> {
            buildResult.set( UIUtils.selection( resultsList.getSelection() ).first( BuildResult.class ).get() );
            getContext().openPanel( site().path(), BuildResultPanel.ID );
        });
        resultsList.setContentProvider( new ListTreeContentProvider() );
        refreshResultsList();
    }
    
    
    protected void refreshResultsList() {
        resultsList.setInput( config.get().buildResults.stream().collect( Collectors.toList() ) );        
    }
    
}
