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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.ViewerCell;

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
import io.mapzone.buildserver.BuildConfiguration;
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
    protected Context<BuildConfiguration>   config;
    
    private MdListViewer                    list;

    
    @Override
    public void init() {
        super.init();
        site().title.set( "Buildserver" );
    }


    @Override
    public void createContents( Composite parent ) {
        parent.setLayout( new FillLayout() );
        
        list = tk().createListViewer( parent, SWT.FULL_SELECTION, SWT.SINGLE );
        list.firstLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            BuildConfiguration elm = (BuildConfiguration)cell.getElement();
            cell.setText( elm.name.get() );
        }));
        list.secondLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            BuildConfiguration elm = (BuildConfiguration)cell.getElement();
            cell.setText( elm.type.get() + ": " + elm.productName.get() );
        }));
        list.firstSecondaryActionProvider.set( new ActionProvider() {
            @Override public void update( ViewerCell cell ) {
                cell.setImage( BsPlugin.images().svgImage( "chevron-right.svg", SvgImageRegistryHelper.NORMAL24 ) );
            }
            @Override public void perform( MdListViewer viewer, Object elm ) {
            }
        });
        list.setContentProvider( new ListTreeContentProvider() );
        list.setInput( BuildRepository.session().query( BuildConfiguration.class ).execute() );
        
        list.addOpenListener( ev -> {
            config.set( UIUtils.selection( list.getSelection() ).first( BuildConfiguration.class ).get() );
            getContext().openPanel( site().path(), BuildConfigurationPanel.ID );
        });
    }

}
