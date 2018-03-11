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

import static org.polymap.core.runtime.event.TypeEventFilter.ifType;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.polymap.core.runtime.UIThreadExecutor;
import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.runtime.event.EventManager;
import org.polymap.core.ui.StatusDispatcher;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.Mandatory;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.contribution.ContributionManager;
import org.polymap.rhei.batik.dashboard.Dashboard;
import org.polymap.rhei.batik.dashboard.ISubmitableDashlet;
import org.polymap.rhei.batik.dashboard.SubmitStatusChangeEvent;
import org.polymap.rhei.batik.toolkit.MinWidthConstraint;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;
import org.polymap.rhei.batik.toolkit.Snackbar.Appearance;
import org.polymap.rhei.field.FormFieldEvent;
import org.polymap.rhei.field.IFormFieldListener;
import org.polymap.rhei.form.batik.BatikFormContainer;

import org.polymap.model2.runtime.UnitOfWork;

import io.mapzone.buildserver.BsPlugin;
import io.mapzone.buildserver.BuildConfig;
import io.mapzone.buildserver.BuildRepository;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class BuildConfigPanel
        extends BsPanel {

    private static final Log log = LogFactory.getLog( BuildConfigPanel.class );

    public static final PanelIdentifier ID = PanelIdentifier.parse( "buildconfig" );
    
    public static final String          DASHBOARD_ID = "build_config_dashboard";

    /**
     * Inbound: The config the work with, or null to create a new one. Use
     * {@link #nestedConfig} to modify!
     */
    @Mandatory
    @Scope( BsPlugin.ID )
    protected Context<BuildConfig>      config;

    /** True if {@link #nested} was created by this panel. */
    private boolean                     created;
    
    private UnitOfWork                  uow, nested;
    
    private BuildConfig                 nestedConfig;
    
    private Button                      fab;
    
    private Dashboard                   dashboard;

    
    @Override
    public void init() {
        super.init();
        site().title.set( "Build Configuration" );

        this.uow = BuildRepository.session();
        this.nested = uow.newUnitOfWork();
        if (!config.isPresent()) {
            // create new
            this.nestedConfig = nested.createEntity( BuildConfig.class, null, BuildConfig.defaults() );
            this.created = true;
        }
        else {
            // edit existing
            assert config.get().belongsTo() == uow;
            this.nestedConfig = nested.entity( config.get() );
            this.created = false;
        }
    }

    
    @Override
    public void dispose() {
        if (nested != null) {
            nested.close();
        }
        super.dispose();
    }


    protected void submit() {
        try {
            dashboard.submit( new NullProgressMonitor() );
            nested.commit();
            uow.commit();
        
            tk().createSnackbar( Appearance.FadeIn, "Saved" );
            UIThreadExecutor.async( () -> fab.setEnabled( false ) );  // XXX fixes race with updateEnables()
        }
        catch (Exception e) {
            StatusDispatcher.handleError( "Unable to save changes.", e );
            uow.rollback();
        }
    }

    
    @EventHandler( display=true, delay=250 )
    protected void updateEnabled( List<SubmitStatusChangeEvent> evs ) {
        fab.setEnabled( dashboard.isValid() && dashboard.isDirty() );
        fab.setVisible( fab.isVisible() || dashboard.isValid() );
    }


    @Override
    public void createContents( Composite parent ) {
        dashboard = new Dashboard( getSite(), DASHBOARD_ID ).defaultExpandable.put( true );
        dashboard.addDashlet( new FormDashlet().setExpanded( true )
                .addConstraint( new PriorityConstraint( 100 ), new MinWidthConstraint( 400, 0 ) ) );
        dashboard.addDashlet( new ScmDashlet( nestedConfig ).setExpanded( false )
                .addConstraint( new PriorityConstraint( 90 ) ) );
        dashboard.addDashlet( new TargetPlatformDashlet( nestedConfig ).setExpanded( false )
                .addConstraint( new PriorityConstraint( 80 ) ) );
        if (config.isPresent()) {
            dashboard.addDashlet( new BuildResultsDashlet( config.get() ).setExpanded( !created )
                    .addConstraint( new PriorityConstraint( 0 ) ) );
        }
        ContributionManager.instance().contributeTo( dashboard, this, DASHBOARD_ID );
        dashboard.createContents( parent );
        
        EventManager.instance().subscribe( this, ifType( SubmitStatusChangeEvent.class, ev -> 
                ev.getDashboard() == dashboard ) );

//        EventManager.instance().subscribe( this, ifType( ExpansionEvent.class, ev -> 
//                dashboard.dashlets().stream().anyMatch( d -> d.site().getPanelSection() == ev.getSource() ) ) );
    
        fab = tk().createFab( "Submit" );
        fab.setEnabled( false );
        fab.setVisible( false );
        fab.addSelectionListener( UIUtils.selectionListener( ev -> submit() ) );
    }


//    @EventHandler( display=true )
//    protected void onDashletExpansion( ExpansionEvent ev ) {
//        if (!isDisposed() && ev.getState()) {
//            for (IDashlet dashlet : dashboard.dashlets()) {
//                if (dashlet.site().isExpanded() && dashlet.site().getPanelSection() != ev.getSource()) {
//                    dashlet.site().setExpanded( false );
//                }
//            }
//        }
//    }
    
    
    /**
     * 
     */
    class FormDashlet
            extends BuildConfigDashlet
            implements IFormFieldListener, ISubmitableDashlet {

        private BatikFormContainer          form;
        
        @Override
        public void createContents( Composite parent ) {
            getSite().title.set( created ? "New configuration" : nestedConfig.name.get() );
            
            parent.setLayout( new FillLayout() );
            BuildConfigForm formPage = new BuildConfigForm( nestedConfig, created );
            form = new BatikFormContainer( formPage );
            form.createContents( parent );
            form.addFieldListener( this );
        }

        @Override
        public void fieldChange( FormFieldEvent ev ) {
            if (ev.getEventCode() == IFormFieldListener.VALUE_CHANGE) {
                getSite().enableSubmit( form.isDirty(), form.isValid() );
            }
        }
        
        @Override
        public boolean submit( IProgressMonitor monitor ) throws Exception {
            form.submit( monitor );
            return true;
        }

    }
    
}
