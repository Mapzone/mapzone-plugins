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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.polymap.core.runtime.Polymap;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.Mandatory;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.md.MdListViewer;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildResult;
import io.mapzone.buildserver.BuildResult.LogEntry;
import io.mapzone.buildserver.BuildResult.LogEntry.Severity;
import io.mapzone.buildserver.BuildResult.Status;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildResultPanel
        extends BsPanel {

    private static final Log log = LogFactory.getLog( BuildResultPanel.class );
    
    public static final PanelIdentifier     ID = PanelIdentifier.parse( "buildresult" );

    private DateFormat                      df = SimpleDateFormat.getDateInstance( DateFormat.LONG, Polymap.getSessionLocale() );

    private DateFormat                      tf = SimpleDateFormat.getTimeInstance( DateFormat.DEFAULT, Polymap.getSessionLocale() );
    
    /**
     * Inbound: The config the work with.
     */
    @Mandatory
    @Scope( BsPlugin.ID )
    protected Context<BuildResult>          buildResult;

    private MdListViewer                    resultsList;
    
    
    @Override
    public void init() {
        super.init();
        site().title.set( "Build Result" );
    }


    @Override
    public void createContents( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 5 ).margins( 3, 8 ).create() );
        
        if (buildResult.get().status.get() == Status.RUNNING) {
            Label l = tk().createFlowText( parent, "Running ..." );
            FormDataFactory.on( l ).left( 0 ).top( 0 );
        }
        else {
            IPanelSection consoleSection = tk().createPanelSection( parent, "Console", SWT.NONE );
            createConsoleSection( consoleSection.getBody() );
            IPanelSection logsSection = tk().createPanelSection( parent, "Errors / Warnings", SWT.NONE );
            createLogsSection( logsSection.getBody() );

            FormDataFactory.on( logsSection.getControl() ).fill().bottom( 50 ).height( 100 );
            FormDataFactory.on( consoleSection.getControl() ).fill().top( logsSection.getControl() ).height( 100 );
        }
    }


    protected void createConsoleSection( Composite parent ) {
        parent.setLayout( new FillLayout() );
        Text text = tk().createText( parent, "", SWT.MULTI, SWT.BORDER );
        text.setText( buildResult.get().console().orElse( "<empty>" ) );
        text.setEnabled( false );
        //text.getDisplay().timerExec( 1000, () -> text.getVerticalBar().setSelection( 100 ) );
    }


    protected void createLogsSection( Composite parent ) {
        parent.setLayout( new FillLayout() );
        StringBuilder buf = new StringBuilder( 4*1024 );
        for (LogEntry entry : buildResult.get().logEntries( 5, Severity.ERROR )) {
            buf.append( "______ " ).append( entry.severity ).append( " ______________________________________________________\n" );
            buf.append( entry.head ).append( "\n" );
            entry.text.forEach( line -> buf.append( "    " ).append( line ).append( "\n" ) );
        }
        
        Text text = tk().createText( parent, buf.length()>0?buf.toString():"<empty>", SWT.MULTI, SWT.BORDER );
        text.setEnabled( false );
    }
    
}
