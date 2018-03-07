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

import static org.polymap.core.ui.UIUtils.selection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.polymap.core.ui.FormLayoutFactory;

import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.dashboard.DashletSite;
import org.polymap.rhei.batik.dashboard.ISubmitableDashlet;
import org.polymap.rhei.batik.toolkit.md.FunctionalLabelProvider;
import org.polymap.rhei.batik.toolkit.md.ListTreeContentProvider;
import org.polymap.rhei.batik.toolkit.md.MdListViewer;
import org.polymap.rhei.form.batik.BatikFormContainer;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfig;
import io.mapzone.buildserver.BuildConfig.TargetPlatformConfig;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class TargetPlatformDashlet
        extends BuildConfigDashlet
        implements ISubmitableDashlet {

    private static final Log log = LogFactory.getLog( TargetPlatformDashlet.class );
    
    private BuildConfig         config;
    
    private MdListViewer        list;

    private Button              addBtn, clearBtn;

    private BatikFormContainer  dialogForm;

    
    public TargetPlatformDashlet( BuildConfig config ) {
        this.config = config;
    }
    
    
    @Override
    public void init( DashletSite site ) {
        super.init( site );
        getSite().title.set( "Target platform" );
    }

    
    @Override
    public boolean submit( IProgressMonitor monitor ) throws Exception {
        return true;
    }


    @Override
    public void createContents( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 6 ).margins( 0, 8, 3, 0 ).create() );

        // list
        list = tk().createListViewer( parent, SWT.FULL_SELECTION, SWT.SINGLE );
        list.firstLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            TargetPlatformConfig elm = (TargetPlatformConfig)cell.getElement();
            cell.setText( elm.type.get().toString() + " - " + StringUtils.abbreviateMiddle( elm.url.get(), "...", 40 ) );
        }));
        list.iconProvider.set( FunctionalLabelProvider.of( cell -> {
            TargetPlatformConfig elm = (TargetPlatformConfig)cell.getElement();
            switch (elm.type.get()) {
                //case DIRECTORY: cell.setImage( BsPlugin.images().svgImage( "folder.svg", SvgImageRegistryHelper.NORMAL24 ) ); break;
                case ZIP_DOWNLOAD: cell.setImage( BsPlugin.images().svgImage( "folder-download.svg", SvgImageRegistryHelper.NORMAL24 ) ); break;
            }
        }));
        list.setSorter( new ViewerSorter() {
            @Override public int compare( Viewer viewer, Object e1, Object e2 ) {
                return ((TargetPlatformConfig)e2).url.get().compareTo( ((TargetPlatformConfig)e1).url.get() );
            }
        });
        list.addOpenListener( ev -> {
            openDialog( selection( list.getSelection() ).first( TargetPlatformConfig.class ).get() );
        });
        list.setContentProvider( new ListTreeContentProvider() );
        list.setInput( config.targetPlatform );
        
        // add button
        addBtn = createAddButton( parent, ev -> {
            TargetPlatformConfig newElm = config.targetPlatform.createElement( TargetPlatformConfig.defaults() );
            int ok = openDialog( newElm );
            if (ok != Window.OK) {
                config.belongsTo().reload( config );
            }
        });
        
        // clear button
        clearBtn = createClearButton( parent, ev -> {
            config.targetPlatform.clear();
            list.setInput( config.targetPlatform );
            getSite().enableSubmit( true, true );
        });
        
        layout( list, addBtn, clearBtn );
    }

    
    protected int openDialog( TargetPlatformConfig elm ) {
        try {
            return tk().createSimpleDialog( "Target platform" )
                    .setContents( p -> {
                        TargetPlatformForm formPage = new TargetPlatformForm( elm );
                        dialogForm = new BatikFormContainer( formPage );
                        dialogForm.createContents( p );
                    })
                    .addOkAction( () -> {
                        dialogForm.submit( new NullProgressMonitor() );
                        getSite().enableSubmit( true, true );
                        list.setInput( config.targetPlatform );
                        return true;
                    })
                    .addCancelAction()
                    .openAndBlock();
        }
        finally {
            dialogForm = null;
            list.setSelection( new StructuredSelection() );
        }
    }

}
