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

import static org.polymap.rhei.batik.app.SvgImageRegistryHelper.WHITE24;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.dashboard.DefaultDashlet;
import org.polymap.rhei.batik.toolkit.md.MdListViewer;
import org.polymap.rhei.batik.toolkit.md.MdToolkit;

import io.mapzone.buildserver.BsPlugin;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class BuildConfigDashlet
        extends DefaultDashlet {

    protected MdToolkit tk() {
        return (MdToolkit)getSite().toolkit();
    }
    
    
    protected Button createAddButton( Composite parent, Consumer<SelectionEvent> listener ) {
        Button addBtn = tk().createButton( parent, null, SWT.PUSH );
        addBtn.setImage( BsPlugin.images().svgImage( "plus-circle-outline.svg", WHITE24 ) );
        addBtn.addSelectionListener( UIUtils.selectionListener( listener ) );
        return addBtn;
    }
    
    
    protected Button createClearButton( Composite parent, Consumer<SelectionEvent> listener  ) {
        Button clearBtn = tk().createButton( parent, null, SWT.PUSH );
        clearBtn.setImage( BsPlugin.images().svgImage( "delete.svg", WHITE24 ) );
        clearBtn.addSelectionListener( UIUtils.selectionListener( listener ) );
        return clearBtn;
    }
    
    
    protected void layout( MdListViewer list, Button addBtn, Button clearBtn ) {
        FormDataFactory.on( addBtn ).right( 100 ).top( 0 ).width( 35 );
        FormDataFactory.on( clearBtn ).right( 100 ).top( addBtn ).width( 35 );
        FormDataFactory.on( list.getControl() ).fill().right( addBtn );
    }

}
