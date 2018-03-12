/* 
 * mapzone.io
 * Copyright (C) 2016, the @authors. All rights reserved.
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

import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.polymap.core.ui.FormDataFactory.on;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.polymap.core.runtime.UIThreadExecutor;
import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.BatikApplication;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.PropertyAccessEvent;
import org.polymap.rhei.batik.dashboard.DashboardPanel;
import org.polymap.rhei.batik.toolkit.ConstraintData;
import org.polymap.rhei.batik.toolkit.ConstraintLayout;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.LayoutSupplier;

import org.polymap.cms.ContentProvider;
import org.polymap.cms.ContentProvider.ContentObject;

import io.mapzone.buildserver.BsPlugin;

/**
 * Landing page or open {@link DashboardPanel} if {@link LoginCookie} is set.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class StartPanel
        extends BsPanel {

    private static final Log log = LogFactory.getLog( StartPanel.class );

    public static final PanelIdentifier     ID = PanelIdentifier.parse( "start" );

    private Composite                       parent;

    
    @Override
    public boolean wantsToBeShown() {
        return getSite().getPath().size() == 1;
    }
    
    
    @Override
    public void createContents( @SuppressWarnings("hiding") Composite parent ) {
        this.parent = parent;
        site().setSize( SIDE_PANEL_WIDTH2, 550, Integer.MAX_VALUE );
        site().title.set( "Mapzone Buildserver" );

        if (BsPlugin.instance().oauth.authenticated().isPresent()) {
            openDashboard();            
        }
        else {
            createFrontpageContents();
        }
    }


    protected void createFrontpageContents() {
        int shellWith = UIUtils.shellToParentOn().getSize().x;
        parent.setLayout( FormLayoutFactory.defaults().spacing( 10 ).margins( 10, 0, 10, 0 ).create() );        
        
        // welcome
        ContentProvider cp = ContentProvider.instance();
        Label welcome = tk().createFlowText( parent, cp.findContent( "buildserver/1welcome.md" ).content() );

        CLabel oauth = BsPlugin.instance().oauth.iterator().next()
                .createControl( parent );
        tk().adapt( oauth, false, false );

        // layout
        FormDataFactory.on( welcome ).fill().noBottom();
        FormDataFactory.on( oauth ).fill().top( welcome ).left( 40 ).right( 60 );
    }

    
    protected void createArticleSection( Composite grid, ContentObject co ) {
        String content = co.content();
        String title = co.title();
        if (content.startsWith( "#" )) {
            title = substringBefore( content, "\n" ).substring( 1 );
            content = content.substring( title.length() + 2 );
        }

        IPanelSection section = tk().createPanelSection( grid, title, SWT.BORDER );
        
        int priority = 100 - Integer.parseInt( co.name().substring( 0, 2 ) );
        section.getControl().setLayoutData( new ConstraintData()
                .prio( priority ).minHeight( 300 ).minWidth( 350 ) ); //.maxWidth( 450 ) );
                //ColumnDataFactory.defaults().heightHint( 300 ).widthHint( 350 ).create() );
        
        section.getBody().setLayout( FormLayoutFactory.defaults().create() );

        // this generates an iFrame with proper size; this allows to load
        // scripts/CSS in content *and* is better than a Label which always has
        // its own idea of its size depending on fonts in content
        Browser b = new Browser( section.getBody(), SWT.NONE );
        on( b ).fill().width( 380 );
        
        // XXX moved <head> elements in a <p> which generates a margin on top
        String html = tk().markdownToHtml( content, b );
        b.setText( html );
    }
    
    
    /**
     * Page layout: 800px width    
     */
    protected void createPageLayout() {
        ((ConstraintLayout)parent.getLayout()).setMargins( new LayoutSupplier() {
            LayoutSupplier layoutPrefs = site().layoutPreferences();
            LayoutSupplier appLayoutPrefs = BatikApplication.instance().getAppDesign().getAppLayoutSettings();
            @Override
            public int getSpacing() {
                return 0; //layoutPrefs.getSpacing() * 2;
            }
            protected int margins() {
                Rectangle bounds = parent.getParent().getBounds();
                int availWidth = bounds.width-(appLayoutPrefs.getMarginLeft()+appLayoutPrefs.getMarginRight());
                return Math.max( (availWidth-800)/2, layoutPrefs.getMarginLeft());
            }
            @Override
            public int getMarginTop() { return layoutPrefs.getMarginTop(); }
            @Override
            public int getMarginRight() { return margins(); }
            @Override
            public int getMarginLeft() { return margins(); }
            @Override
            public int getMarginBottom() { return layoutPrefs.getMarginBottom() /*+ 10*/; }
        });
    }
    
    
    protected void openDashboard() {
        // make StartPanel/frontpage to big to be shown beside the dashboard
        site().preferredWidth.set( Integer.MAX_VALUE );
        site().minWidth.set( Integer.MAX_VALUE );

        UIThreadExecutor.async( () -> getContext().openPanel( getSite().getPath(), BuildConfigsPanel.ID ) );
    }


    /** 
     * Triggered by {@link StartPanel} and {@link RegisterPanel}.
     */
    @EventHandler( display=true )
    protected void userLogedIn( PropertyAccessEvent ev ) {
        openDashboard();    
    }
    
}
