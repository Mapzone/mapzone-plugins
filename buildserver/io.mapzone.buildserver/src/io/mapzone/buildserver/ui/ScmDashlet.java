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

import org.eclipse.core.runtime.NullProgressMonitor;

import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.dashboard.DashletSite;
import org.polymap.rhei.batik.toolkit.md.FunctionalLabelProvider;
import org.polymap.rhei.batik.toolkit.md.ListTreeContentProvider;
import org.polymap.rhei.batik.toolkit.md.MdListViewer;
import org.polymap.rhei.form.batik.BatikFormContainer;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfig;
import io.mapzone.buildserver.BuildConfig.ScmConfig;


/**
 * 
 *
 * @author Falko Bräutigam
 */
public class ScmDashlet
        extends BuildConfigDashlet {

    private static final Log log = LogFactory.getLog( ScmDashlet.class );
    
    private BuildConfig         config;
    
    private MdListViewer        list;

    private Button              addBtn, clearBtn;

    private BatikFormContainer  dialogForm;

    
    public ScmDashlet( BuildConfig config ) {
        this.config = config;
    }


    @Override
    public void init( DashletSite site ) {
        super.init( site );
        getSite().title.set( "Source repositories" );
    }


    @Override
    public void createContents( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 8 ).margins( 0, 8, 0, 0 ).create() );

        // list
        list = tk().createListViewer( parent, SWT.FULL_SELECTION, SWT.SINGLE );
        list.firstLineLabelProvider.set( FunctionalLabelProvider.of( cell -> {
            ScmConfig elm = (ScmConfig)cell.getElement();
            cell.setText( elm.type.get().toString() + " - " + StringUtils.abbreviateMiddle( elm.url.get(), "...", 40 ) );
        }));
        list.iconProvider.set( FunctionalLabelProvider.of( cell -> {
            ScmConfig elm = (ScmConfig)cell.getElement();
            switch (elm.type.get()) {
                case GIT: cell.setImage( BsPlugin.images().svgImage( "git.svg", NORMAL24 ) ); break;
                case DIRECTORY: ; break;
            }
        }));
        list.setSorter( new ViewerSorter() {
            @Override public int compare( Viewer viewer, Object e1, Object e2 ) {
                return ((ScmConfig)e2).url.get().compareTo( ((ScmConfig)e1).url.get() );
            }
        });
        list.addOpenListener( ev -> {
            openDialog( UIUtils.selection( list.getSelection() ).first( ScmConfig.class ).get() );
        });
        list.setContentProvider( new ListTreeContentProvider() );
        list.setInput( config.scm );
        
        // add button
        addBtn = createAddButton( parent, ev -> {
            ScmConfig newElm = config.scm.createElement( ScmConfig.defaults() );
            int ok = openDialog( newElm );
            if (ok != Window.OK) {
                config.belongsTo().rollback();
            }
        });
        
        // clear button
        clearBtn = createClearButton( parent, ev -> {
            throw new RuntimeException( "not yet...");
        });
        
        layout( list, addBtn, clearBtn );
    }


    protected int openDialog( ScmConfig elm ) {
        try {
            return tk().createSimpleDialog( "Source repository" )
                    .setContents( p -> {
                        ScmForm formPage = new ScmForm( elm );
                        dialogForm = new BatikFormContainer( formPage );
                        dialogForm.createContents( p );
                    })
                    .addOkAction( () -> {
                        dialogForm.submit( new NullProgressMonitor() );
                        list.refresh();
                        getSite().enableSubmit( true, true );
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
