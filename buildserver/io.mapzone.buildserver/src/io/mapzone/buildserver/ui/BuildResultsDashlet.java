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

import static org.polymap.core.runtime.event.TypeEventFilter.isType;

import java.util.List;
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

import org.polymap.core.runtime.Polymap;
import org.polymap.core.runtime.event.Event;
import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.runtime.event.EventManager;
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

import org.polymap.model2.runtime.UnitOfWork;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfig;
import io.mapzone.buildserver.BuildManager;
import io.mapzone.buildserver.BuildObjectCommittedEvent;
import io.mapzone.buildserver.BuildRepository;
import io.mapzone.buildserver.BuildResult;
import io.mapzone.buildserver.PrintProgressMonitor.ProgressEvent;

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


    /**
     * 
     * @param config The {@link BuildConfig} that belongs to
     *        {@link BuildRepository#session()}. {@link BuildManager} creates its own
     *        nested {@link UnitOfWork}.
     */
    public BuildResultsDashlet( BuildConfig config ) {
        assert config.belongsTo() == BuildRepository.session();
        this.config = config;        
        this.buildManager = BuildManager.of( config );
    }

    
    @Override
    public void init( DashletSite site ) {
        super.init( site );
        getSite().title.set( "Build results" );        
    }


    @Override
    public void dispose() {
        super.dispose();
        EventManager.instance().unsubscribe( this );
    }


    @Override
    public void createContents( Composite parent ) {
        createResultsList( parent );
        createBuildButton( parent );
        
        parent.setLayout( FormLayoutFactory.defaults().spacing( 8 ).margins( 0, 0, 3, 0 ).create() );
        FormDataFactory.on( resultsList.getControl() ).fill().noBottom().height( 95 );
        FormDataFactory.on( buildBtn ).bottom( 100 ).left( 30 ).right( 70 ).top( resultsList.getControl() );
        
        EventManager.instance().subscribe( this, isType( BuildObjectCommittedEvent.class, ev ->
                ev.getSource() instanceof BuildResult) );
        EventManager.instance().subscribe( this, ev -> ev instanceof ProgressEvent );
    }
    
    
    public void createResultsList( Composite parent ) {
        resultsList = tk().createListViewer( parent, SWT.FULL_SELECTION, SWT.SINGLE );
        resultsList.firstLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            BuildResult elm = (BuildResult)cell.getElement();
            StringBuilder text = new StringBuilder( 256 )
                    .append( df.format( elm.started.get() ) ).append( "   " )
                    .append( tf.format( elm.started.get() ) );
            if (elm.status.get() == BuildResult.Status.RUNNING) {
                BuildManager.of( elm.config.get() ).running().ifPresent( process -> {
                    text.append( "   (" ).append( process.monitor().get().percentDone() ).append( "%)" );
                });
            }
            cell.setText( text.toString() );
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
        refreshResultsList( null );
    }


    protected void createBuildButton( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 5 ).margins( 0, 3 ).create() );        
        buildBtn = tk().createButton( parent, "Build Now", SWT.PUSH );
        buildBtn.setImage( BsPlugin.images().svgImage( "play-circle-outline.svg", SvgImageRegistryHelper.WHITE24 ) );
    
        buildBtn.setEnabled( !buildManager.running().isPresent() );
        buildBtn.addSelectionListener( UIUtils.selectionListener( ev -> {
            buildBtn.setEnabled( false );
            buildManager.startNewBuild( config.belongsTo() );
        }));
        FormDataFactory.on( buildBtn ).fill().left( 30 ).right( 70 );
    }


    @EventHandler( display=true, delay=250, scope=Event.Scope.JVM )
    protected void refreshResultsList( List<BuildObjectCommittedEvent> evs ) {
        if (resultsList != null && !resultsList.getControl().isDisposed()) {
            resultsList.setInput( config.buildResults.stream().collect( Collectors.toList() ) );
        }
        if (buildBtn != null && !buildBtn.isDisposed()) {
            buildBtn.setEnabled( !buildManager.running().isPresent() );
        }
    }

    
    @EventHandler( display=true, delay=1000, scope=Event.Scope.JVM )
    protected void onProgress( List<ProgressEvent> evs ) {
        if (resultsList.getControl().isDisposed()) {
            EventManager.instance().unsubscribe( BuildResultsDashlet.this );
        }
        else {
            List<Object> ids = evs.stream()
                    .map( ev -> ev.getSource().result.get().id() )
                    .collect( Collectors.toList() );
            config.buildResults.stream()
                    .filter( r -> ids.contains( r.id() ) )
                    .findAny().ifPresent( found -> 
                            resultsList.refresh( found ) );
        }
    }

}
