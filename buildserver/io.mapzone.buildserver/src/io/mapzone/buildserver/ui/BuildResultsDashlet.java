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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import org.polymap.core.runtime.Polymap;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.BatikApplication;
import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.dashboard.DashletSite;
import org.polymap.rhei.batik.toolkit.md.ActionProvider;
import org.polymap.rhei.batik.toolkit.md.FunctionalLabelProvider;
import org.polymap.rhei.batik.toolkit.md.ListTreeContentProvider;
import org.polymap.rhei.batik.toolkit.md.MdListViewer;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfig;
import io.mapzone.buildserver.BuildManager;
import io.mapzone.buildserver.BuildManager.BuildProcess;
import io.mapzone.buildserver.BuildResult;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildResultsDashlet
        extends BuildConfigDashlet {

    private static final Log log = LogFactory.getLog( BuildResultsDashlet.class );

    private DateFormat          df = SimpleDateFormat.getDateInstance( DateFormat.LONG, Polymap.getSessionLocale() );

    private DateFormat          tf = SimpleDateFormat.getTimeInstance( DateFormat.DEFAULT, Polymap.getSessionLocale() );

    /** Outbound: */
    @Scope( BsPlugin.ID )
    protected Context<BuildResult> buildResult;
    
    private BuildConfig         config;
    
    private MdListViewer        resultsList;

    private Button              buildBtn;
    
    private BuildManager        buildManager;


    public BuildResultsDashlet( BuildConfig config ) {
        this.config = config;        
        this.buildManager = BuildManager.of( config );
    }

    
    @Override
    public void init( DashletSite site ) {
        super.init( site );
        getSite().title.set( "Build results" );        
    }


    @Override
    public void createContents( Composite parent ) {
        createResultsList( parent );
        createBuildButton( parent );
        
        parent.setLayout( FormLayoutFactory.defaults().spacing( 8 ).margins( 0, 0, 3, 0 ).create() );
        FormDataFactory.on( resultsList.getControl() ).fill().noBottom().height( 100 );
        FormDataFactory.on( buildBtn ).bottom( 100 ).left( 30 ).right( 70 ).top( resultsList.getControl() );
    }
    
    
    public void createResultsList( Composite parent ) {
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
        resultsList.setSorter( new ViewerSorter() {
            @Override public int compare( Viewer viewer, Object e1, Object e2 ) {
                return ((BuildResult)e2).started.get().compareTo( ((BuildResult)e1).started.get() );
            }
        });
        resultsList.addOpenListener( ev -> {
            buildResult.set( UIUtils.selection( resultsList.getSelection() ).first( BuildResult.class ).get() );
            BatikApplication.instance().getContext().openPanel( site().panelSite().getPath(), BuildResultPanel.ID );
        });
        resultsList.setContentProvider( new ListTreeContentProvider() );
        refreshResultsList();
    }


    protected void createBuildButton( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 5 ).margins( 0, 3 ).create() );        
        buildBtn = tk().createButton( parent, "Build Now", SWT.PUSH );
        buildBtn.setImage( BsPlugin.images().svgImage( "play-circle-outline.svg", SvgImageRegistryHelper.WHITE24 ) );
    
        buildBtn.setEnabled( !buildManager.running().isPresent() );
        buildBtn.addSelectionListener( UIUtils.selectionListener( ev -> {
            buildBtn.setEnabled( false );
    
            BuildProcess job = buildManager.startNewBuild();
            job.addJobChangeListener( new JobChangeAdapter() {
                @Override public void running( IJobChangeEvent ev2 ) {
                    if (!buildBtn.isDisposed()) {
                        buildBtn.getDisplay().asyncExec( () -> {
                            refreshResultsList();
                        });
                    }
                }
                @Override public void done( IJobChangeEvent ev2 ) {
                    if (!buildBtn.isDisposed()) {
                        buildBtn.getDisplay().asyncExec( () -> {
                            buildBtn.setEnabled( true );
                            refreshResultsList();
                        });
                    }
                }
            });
        }));
        FormDataFactory.on( buildBtn ).fill().left( 30 ).right( 70 );
    }


    protected void refreshResultsList() {
        resultsList.setInput( config.buildResults.stream().collect( Collectors.toList() ) );        
    }

}
