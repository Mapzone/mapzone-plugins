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

import static org.polymap.rhei.batik.app.SvgImageRegistryHelper.NORMAL24;
import static org.polymap.rhei.batik.app.SvgImageRegistryHelper.WHITE24;

import java.util.stream.Collectors;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;

import org.eclipse.core.runtime.NullProgressMonitor;
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

import org.polymap.model2.runtime.UnitOfWork;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfig;
import io.mapzone.buildserver.BuildConfig.ScmConfig;
import io.mapzone.buildserver.BuildConfig.TargetPlatformConfig;
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
    protected Context<BuildConfig>   config;

    /**
     * Outbound:
     */
    @Scope( BsPlugin.ID )
    protected Context<BuildResult>          buildResult;
    
    private UnitOfWork                      nested;
    
    private BuildConfig              nestedConfig;
    
    private BatikFormContainer              form, scmForm, tpForm;

    private BuildManager                    buildManager;

    private MdListViewer                    resultsList, scmList;

    private Button                          fab;

    
    @Override
    public void init() {
        super.init();
        site().title.set( "Build Configuration" );
        
        nested = config.get().belongsTo().newUnitOfWork();
        nestedConfig = nested.entity( config.get() );
        buildManager = BuildManager.of( nestedConfig );
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

        fab = tk().createFab( "Submit" );
        fab.setEnabled( false );
        fab.setVisible( false );
        
        FormDataFactory.on( section.getControl() ).fill().noBottom();
        FormDataFactory.on( btnContainer ).fill().top( section.getControl() ).noBottom();
        FormDataFactory.on( resultsSection.getControl() ).fill().top( btnContainer );
    }

    
    protected void updateEnabled() {
        fab.setEnabled( true );
        fab.setVisible( true );
    }

    
    protected void createMainSection( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 10 ).create() );
        BuildConfigurationForm formPage = new BuildConfigurationForm( nestedConfig, false ) {
            @Override
            protected void updateEnabled() {
                //ProjectInfoPanel.this.updateEnabled();
            }
        };
        form = new BatikFormContainer( formPage );
        form.createContents( parent );
    
        IPanelSection scmSection = tk().createPanelSection( parent, "Source repositories", SWT.NONE );
        createScmList( scmSection.getBody() );
        IPanelSection tpsSection = tk().createPanelSection( parent, "Target platform", SWT.NONE );
        createTargetPlatformList( tpsSection.getBody() );
        
        // layout
        FormDataFactory.on( form.getContents() ).fill().noBottom();
        FormDataFactory.on( scmSection.getControl() ).fill().top( form.getContents() ).noBottom().height( 170 );
        FormDataFactory.on( tpsSection.getControl() ).fill().top( scmSection.getControl() ).noBottom().height( 120 );
    }

    
    protected void createScmList( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 8 ).margins( 0, 8, 0, 0 ).create() );

        // list
        scmList = tk().createListViewer( parent, SWT.FULL_SELECTION, SWT.SINGLE );
        scmList.firstLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            ScmConfig elm = (ScmConfig)cell.getElement();
            cell.setText( elm.type.get().toString() + " - " + StringUtils.abbreviateMiddle( elm.url.get(), "...", 40 ) );
        }));
        scmList.iconProvider.set( FunctionalLabelProvider.of( cell -> {
            ScmConfig elm = (ScmConfig)cell.getElement();
            switch (elm.type.get()) {
                case GIT: cell.setImage( BsPlugin.images().svgImage( "git.svg", NORMAL24 ) ); break;
                case DIRECTORY: ; break;
            }
        }));
        scmList.setSorter( new ViewerSorter() {
            @Override public int compare( Viewer viewer, Object e1, Object e2 ) {
                return ((ScmConfig)e2).url.get().compareTo( ((ScmConfig)e1).url.get() );
            }
        });
        scmList.addOpenListener( ev -> {
            ScmConfig sel = UIUtils.selection( scmList.getSelection() ).first( ScmConfig.class ).get();
            openScmDialog( sel );
        });
        scmList.setContentProvider( new ListTreeContentProvider() );
        scmList.setInput( nestedConfig.scm );
        
        // add button
        Button addBtn = tk().createButton( parent, null, SWT.PUSH );
        addBtn.setImage( BsPlugin.images().svgImage( "plus-circle-outline.svg", WHITE24 ) );
        addBtn.addSelectionListener( UIUtils.selectionListener( ev -> {
            ScmConfig newElm = nestedConfig.scm.createElement( ScmConfig.defaults() );
            int ok = openScmDialog( newElm );
            if (ok != Window.OK) {
                nested.rollback();
            }
        }));
        
        // clear button
        Button clearBtn = tk().createButton( parent, null, SWT.PUSH );
        clearBtn.setImage( BsPlugin.images().svgImage( "delete.svg", WHITE24 ) );
        clearBtn.addSelectionListener( UIUtils.selectionListener( ev -> {
            throw new RuntimeException( "not yet...");
        }));
        
        // layout
        FormDataFactory.on( addBtn ).right( 100 ).top( 0 ).width( 35 );
        FormDataFactory.on( clearBtn ).right( 100 ).top( addBtn ).width( 35 );
        FormDataFactory.on( scmList.getControl() ).fill().right( addBtn );
    }


    protected int openScmDialog( ScmConfig elm ) {
        try {
            return tk().createSimpleDialog( "Source repository" )
                    .setContents( p -> {
                        ScmForm formPage = new ScmForm( elm );
                        scmForm = new BatikFormContainer( formPage );
                        scmForm.createContents( p );
                    })
                    .addOkAction( () -> {
                        scmForm.submit( new NullProgressMonitor() );
                        scmList.refresh();
                        updateEnabled();
                        return true;
                    })
                    .addCancelAction()
                    .openAndBlock();
        }
        finally {
            scmForm = null;
            scmList.setSelection( new StructuredSelection() );
        }
    }

    
    protected void createTargetPlatformList( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 8 ).margins( 0, 8, 0, 0 ).create() );

        // list
        MdListViewer tpList = tk().createListViewer( parent, SWT.FULL_SELECTION, SWT.SINGLE );
        tpList.firstLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            TargetPlatformConfig elm = (TargetPlatformConfig)cell.getElement();
            cell.setText( elm.type.get().toString() + " - " + StringUtils.abbreviateMiddle( elm.url.get(), "...", 40 ) );
        }));
        tpList.iconProvider.set( FunctionalLabelProvider.of( cell -> {
            TargetPlatformConfig elm = (TargetPlatformConfig)cell.getElement();
            switch (elm.type.get()) {
                case DIRECTORY: cell.setImage( BsPlugin.images().svgImage( "folder.svg", SvgImageRegistryHelper.NORMAL24 ) ); break;
                case ZIP_DOWNLOAD: cell.setImage( BsPlugin.images().svgImage( "folder-download.svg", SvgImageRegistryHelper.NORMAL24 ) ); break;
            }
        }));
//        tpList.firstSecondaryActionProvider.set( new ActionProvider() {
//            @Override public void update( ViewerCell cell ) {
//                cell.setImage( BsPlugin.images().svgImage( "delete-circle.svg", SvgImageRegistryHelper.ACTION24 ) );
//            }
//            @Override public void perform( MdListViewer viewer, Object elm ) {
//            }
//        });
        tpList.setSorter( new ViewerSorter() {
            @Override public int compare( Viewer viewer, Object e1, Object e2 ) {
                return ((TargetPlatformConfig)e2).url.get().compareTo( ((TargetPlatformConfig)e1).url.get() );
            }
        });
        tpList.addOpenListener( ev -> {
            TargetPlatformConfig sel = UIUtils.selection( tpList.getSelection() ).first( TargetPlatformConfig.class ).get();
            tk().createSimpleDialog( "Target platform" )
                .setContents( p -> {
                    TargetPlatformForm formPage = new TargetPlatformForm( sel );
                    tpForm = new BatikFormContainer( formPage );
                    tpForm.createContents( p );
                })
                .addOkAction( () -> {
                    tpForm.submit( new NullProgressMonitor() );
                    return true;
                })
                .addCancelAction()
                .openAndBlock();
            tpForm = null;
            tpList.setSelection( new StructuredSelection() );
        });
        tpList.setContentProvider( new ListTreeContentProvider() );
        tpList.setInput( nestedConfig.targetPlatform );
        
        // add button
        Button addBtn = tk().createButton( parent, null, SWT.PUSH );
        addBtn.setImage( BsPlugin.images().svgImage( "plus-circle-outline.svg", SvgImageRegistryHelper.WHITE24 ) );
        
        // layout
        FormDataFactory.on( addBtn ).right( 100 ).top( 0 ).width( 35 );
        FormDataFactory.on( tpList.getControl() ).fill().right( addBtn );
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
        resultsList.setSorter( new ViewerSorter() {
            @Override public int compare( Viewer viewer, Object e1, Object e2 ) {
                return ((BuildResult)e2).started.get().compareTo( ((BuildResult)e1).started.get() );
            }
        });
        resultsList.addOpenListener( ev -> {
            buildResult.set( UIUtils.selection( resultsList.getSelection() ).first( BuildResult.class ).get() );
            getContext().openPanel( site().path(), BuildResultPanel.ID );
        });
        resultsList.setContentProvider( new ListTreeContentProvider() );
        refreshResultsList();
    }
    
    
    protected void refreshResultsList() {
        resultsList.setInput( nestedConfig.buildResults.stream().collect( Collectors.toList() ) );        
    }
    
}
