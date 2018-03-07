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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;

import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.md.ActionProvider;
import org.polymap.rhei.batik.toolkit.md.FunctionalLabelProvider;
import org.polymap.rhei.batik.toolkit.md.ListTreeContentProvider;
import org.polymap.rhei.batik.toolkit.md.MdListViewer;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfig;
import io.mapzone.buildserver.BuildRepository;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class StartPanel
        extends BsPanel {

    private static final Log log = LogFactory.getLog( StartPanel.class );

    public static final PanelIdentifier     ID = PanelIdentifier.parse( "start" );

    /**
     * Outbound: The config the work with.
     */
    @Scope( BsPlugin.ID )
    protected Context<BuildConfig>   config;
    
    private MdListViewer                    list;

    private Button                          addBtn;

    private Button                          removeBtn;

    
    @Override
    public void init() {
        super.init();
        site().title.set( "Buildserver" );
    }


    @Override
    public void createContents( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 8 ).margins( 0, 8 ).create() );
        
        createList( parent );
        createButtons( parent );
        
        FormDataFactory.on( list.getControl() ).fill().noBottom();
        FormDataFactory.on( addBtn ).left( 35 ).right( 65 ).top( list.getControl() ).noBottom();
//        FormDataFactory.on( removeBtn ).left( addBtn ).right( 75 ).top( list.getControl() ).noBottom();
    }

    
    protected void createButtons( Composite parent ) {
        addBtn = tk().createButton( parent, "New...", SWT.PUSH );
        addBtn.setImage( BsPlugin.images().svgImage( "plus-circle-outline.svg", SvgImageRegistryHelper.WHITE24 ) );
        addBtn.addSelectionListener( UIUtils.selectionListener( ev -> {
            
        }));

//        removeBtn = tk().createButton( parent, "Remove", SWT.PUSH );
//        removeBtn.setImage( BsPlugin.images().svgImage( "delete-circle.svg", SvgImageRegistryHelper.WHITE24 ) );
//        removeBtn.addSelectionListener( UIUtils.selectionListener( ev -> {
//            
//        }));
//        removeBtn.setEnabled( false );
    }
    
    
    protected void createList( Composite parent ) {
        list = tk().createListViewer( parent, SWT.FULL_SELECTION, SWT.SINGLE );
        list.iconProvider.set( FunctionalLabelProvider.of( cell -> {
            cell.setImage( BsPlugin.images().svgImage( "package-variant.svg", SvgImageRegistryHelper.NORMAL24 ) );
        }));
        list.firstLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            BuildConfig elm = (BuildConfig)cell.getElement();
            cell.setText( elm.name.get() );
        }));
        list.secondLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            BuildConfig elm = (BuildConfig)cell.getElement();
            cell.setText( elm.productName.get() + " -- " + elm.type.get() );
        }));
        list.secondSecondaryActionProvider.set( new ActionProvider() {
            @Override public void update( ViewerCell cell ) {
                cell.setImage( BsPlugin.images().svgImage( "delete-circle.svg", SvgImageRegistryHelper.ACTION24 ) );
            }
            @Override public void perform( MdListViewer viewer, Object elm ) {
            }
        });
        list.firstSecondaryActionProvider.set( new ActionProvider() {
            @Override public void update( ViewerCell cell ) {
                cell.setImage( BsPlugin.images().svgImage( "chevron-right.svg", SvgImageRegistryHelper.NORMAL24 ) );
            }
            @Override public void perform( MdListViewer viewer, Object elm ) {
                config.set( UIUtils.selection( list.getSelection() ).first( BuildConfig.class ).get() );
                getContext().openPanel( site().path(), BuildConfigurationPanel.ID );
            }
        });
        list.setSorter( new ViewerSorter() {
            @Override public int compare( Viewer viewer, Object e1, Object e2 ) {
                return ((BuildConfig)e1).name.get().compareTo( ((BuildConfig)e2).name.get() );
            }
        });
        list.setContentProvider( new ListTreeContentProvider() );
        list.setInput( BuildRepository.session().query( BuildConfig.class ).execute() );
        
        list.addOpenListener( ev -> {
            config.set( UIUtils.selection( list.getSelection() ).first( BuildConfig.class ).get() );
            getContext().openPanel( site().path(), BuildConfigurationPanel.ID );
        });
    }

}
