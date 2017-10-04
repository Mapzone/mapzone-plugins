/*
 * polymap.org Copyright (C) 2015 individual contributors as indicated by the
 * 
 * @authors tag. All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.tutorial.osm.importer;

import static org.polymap.core.ui.FormDataFactory.on;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.runtime.UIThreadExecutor;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.StatusDispatcher;

import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.TextProposalDecorator;

import org.polymap.p4.data.importer.ImporterPrompt;
import org.polymap.p4.data.importer.ImporterPrompt.PromptUIBuilder;
import org.polymap.tutorial.osm.importer.taginfo.TagInfo;
import org.polymap.tutorial.osm.importer.taginfo.TagInfo.ResultSet;
import org.polymap.tutorial.osm.importer.taginfo.TagInfo.Sort;

/**
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class TagFilterPromptUIBuilder
        implements PromptUIBuilder {

    private static final Log log = LogFactory.getLog( TagFilterPromptUIBuilder.class );

    private static final String LCL  = "abcdefghijklmnopqrstuvwxyz";

    private static final String UCL  = LCL.toUpperCase();

    private static final String NUMS = "0123456789";
    
    public static final int     AUTO_ACTIVATION_DELAY = 1750;

    // this logic is from swt addons project
    static char[] getAutoactivationChars() {
        // To enable content proposal on deleting a char
        String delete = new String( new char[] { 8 } );
        String allChars = LCL + UCL + NUMS + delete;
        return allChars.toCharArray();
    }

    static KeyStroke getActivationKeystroke() {
        KeyStroke instance = KeyStroke.getInstance(
                new Integer( SWT.CTRL ).intValue(), new Integer( ' ' ).intValue() );
        return instance;
    }

    // instance *******************************************

    private TagInfo                 tagInfo;
    
    private Text                    keyText, valueText;

    private ListViewer              filterList;
    
    private List<TagFilter>         filters;

    private TextProposalDecorator   valueProposals;

    /**
     * 
     * 
     * @param tagInfo
     * @param filters The list to display and manipulate.
     */
    protected TagFilterPromptUIBuilder( TagInfo tagInfo, List<TagFilter> filters ) {
        this.tagInfo = tagInfo;
        this.filters = filters;
    }

    
    @Override
    public void createContents( ImporterPrompt prompt, Composite parent, IPanelToolkit tk ) {
        // keyText
        Label keyLabel = tk.createLabel( parent, "Key" );
        keyText = createKeyFilter( parent, tk );

        // valueText
        Label opLabel = tk.createLabel( parent, "=" );
        Label valueLabel = tk.createLabel( parent, "Value" );
        valueText = createValueFilter( parent );
        
        // addBtn
        Button addBtn = tk.createButton( parent, "Add", SWT.PUSH );
        addBtn.setToolTipText( "Add the above key/value pair the list of filters" );
        addBtn.addSelectionListener( new SelectionAdapter() {
            @Override public void widgetSelected( SelectionEvent ev ) {
                TagFilter filter = TagFilter.of( keyText.getText(), valueText.getText() );
                filters.add( filter );
                filterList.refresh();
            }
        });
        
        // removeBtn
        Button removeBtn = tk.createButton( parent, "Remove", SWT.PUSH );
        addBtn.setToolTipText( "Remove the selected filter from the list" );
        removeBtn.setEnabled( false );
        removeBtn.addSelectionListener( new SelectionAdapter() {
            @Override public void widgetSelected( SelectionEvent ev ) {
                org.polymap.core.ui.SelectionAdapter.on( filterList.getSelection() )
                        .first( Pair.class ).ifPresent( selected -> {
                            filters.remove( selected );
                            filterList.refresh();
                        });
                removeBtn.setEnabled( filterList.getSelection() != null );
            }
        });
        
        // filterList
        filterList = new ListViewer( parent, SWT.BORDER );
        filterList.setContentProvider( ArrayContentProvider.getInstance() );
        filterList.setLabelProvider( new LabelProvider() {
            @Override public String getText( Object elm ) {
                TagFilter filter = (TagFilter)elm;
                return filter.key() + " = " + filter.value();
            }
        });
        filterList.getList().addSelectionListener( new SelectionAdapter() {
            @Override public void widgetSelected( SelectionEvent ev ) {
                removeBtn.setEnabled( true );
            }
        });
        filterList.setInput( filters );

        // layout
        parent.setLayout( FormLayoutFactory.defaults().spacing( 8 ).margins( 3 ).create() );
        on( keyLabel ).top( 0 ).left( 0 );
        on( keyText ).top( keyLabel, -8 ).left( 0 ).right( 47 );
        on( opLabel ).top( keyLabel, -3 ).left( keyText );
        on( valueLabel ).top( 0 ).left( opLabel );
        on( valueText ).top( keyLabel, -8 ).left( opLabel ).right( 100 );
        on( addBtn ).top( keyText, 8 ).left( 0 ).right( 50 );
        on( removeBtn ).top( keyText, 8 ).left( addBtn ).right( 100 );
        on( filterList.getList() ).fill().top( addBtn ).width( 300 ).height( 150 );
    }


    protected Text createKeyFilter( Composite parent, IPanelToolkit tk ) {
        keyText = tk.createText( parent, null, SWT.BORDER );
        
        new TextProposalDecorator( keyText ) {
            @Override
            protected String[] proposals( String text, int maxResults, IProgressMonitor monitor ) {
                try {
                    if (text.length() < 2) {
                        return new String[0];
                    }
                    ResultSet<String> rs = tagInfo.keys( text, Sort.count_all, 50 );
                    return rs.stream().toArray( String[]::new );
                }
                catch (Exception e) {
                    UIThreadExecutor.async( () -> StatusDispatcher.handleError( "", e ) );
                    return null;
                }
            }
        };
        keyText.addKeyListener( new KeyAdapter() {
            @Override public void keyReleased( KeyEvent ev ) {
                log.info( "code: " + ev.keyCode );
                if (ev.keyCode == 13) {
                    keyText.setText( keyText.getText().trim() );  // FIXME
                    
                    valueText.setText( "" );
                    valueText.forceFocus();
                    //valueProposals.open()
                }
            }
        });

        
        //proposals.activationDelayMillis.put( AUTO_ACTIVATION_DELAY );
        
//        IContentProposalProvider proposalProvider = new IContentProposalProvider() {
//            @Override public IContentProposal[] getProposals( String text, int pos ) {
//                try {
//                    if (text.length() < 2) {
//                        return new IContentProposal[0];
//                    }
//                    ResultSet<String> rs = tagInfo.keys( text, Sort.count_all, 50 );
//                    return rs.stream().map( s -> new SimpleContentProposal( s ) )
//                            .toArray( IContentProposal[]::new );
//                }
//                catch (Exception e) {
//                    StatusDispatcher.handleError( "", e );
//                    return null;
//                }
//            }
//        };
//        ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(
//                keyText, new TextContentAdapter(), proposalProvider,
//                getActivationKeystroke(), getAutoactivationChars() );
//        proposalAdapter.setPropagateKeys( true );
//        proposalAdapter.setAutoActivationDelay( AUTO_ACTIVATION_DELAY );
//        proposalAdapter.setProposalAcceptanceStyle( ContentProposalAdapter.PROPOSAL_REPLACE );
//        proposalAdapter.addContentProposalListener( prop -> onKeySelected( prop.getContent() ) );
        return keyText;
    }

    
    protected Text createValueFilter( Composite parent ) {
        valueText = new Text( parent, SWT.BORDER );

        valueProposals = new TextProposalDecorator( valueText ) {
            @Override
            protected String[] proposals( String text, int maxResults, IProgressMonitor monitor ) {
                try {
//                    if (text.length() < 2) {
//                        return new String[0];
//                    }
                    String key = UIThreadExecutor.sync( () -> keyText.getText() ).get();
                    ResultSet<String> rs = tagInfo.values( key, text, Sort.count_all, 50 );
                    return rs.stream().toArray( String[]::new );
                }
                catch (Exception e) {
                    UIThreadExecutor.async( () -> StatusDispatcher.handleError( "", e ) );
                    return null;
                }
            }
        };

//        valueProposalProvider = new SimpleContentProposalProvider( new String[] {"*"} );
//        ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(
//                valueText, new TextContentAdapter(), valueProposalProvider,
//                getActivationKeystroke(), getAutoactivationChars() );
//        valueProposalProvider.setFiltering( true );
//        proposalAdapter.setPropagateKeys( true );
//        proposalAdapter.setAutoActivationDelay( AUTO_ACTIVATION_DELAY );
//        proposalAdapter.setProposalAcceptanceStyle( ContentProposalAdapter.PROPOSAL_REPLACE );
        return valueText;
    }


//    protected void onKeySelected( String selectedItem ) {
////        valueText.removeAll();
////        valueText.add( "*" );
////        for (String value : listItems().get( selectedItem )) {
////            valueText.add( value );
////        }
//        valueText.setText( "*" );
////        valueProposalProvider.setProposals( valueText.getItems() );
//    }


//    protected List<String> filterSelectableKeys( String text ) {
//        return listItems().keySet().stream()
//                .filter( item -> item.toLowerCase().contains( text.toLowerCase() ) )
//                .collect( Collectors.toList() );
//    }
//
//
//    protected List<String> filterSelectableValues( String text ) {
//        return listItems().get( getSelectedItem( keyText ) ).stream()
//                .filter( item -> item.toLowerCase().contains( text.toLowerCase() ) )
//                .collect( Collectors.toList() );
//    }


//    private String getSelectedItem( Combo combo ) {
//        return combo.getItem( combo.getSelectionIndex() );
//    }


//    protected Collection<String> keys() {
//        return listItems().keySet();
//    }
//
//
//    protected Collection<String> values( String key ) {
//        return listItems().get( key );
//    }
    
    
    /**
     * 
     */
    protected static class SimpleContentProposal
            implements IContentProposal {
        
        private String      content;

        protected SimpleContentProposal( String content ) {
            this.content = content;
        }

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public int getCursorPosition() {
            return 0;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public String getLabel() {
            return content;
        }
    }
    
}
