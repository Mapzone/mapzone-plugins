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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.viewers.ComboViewer;

import org.polymap.core.ui.FormLayoutFactory;

import org.polymap.rhei.batik.toolkit.IPanelToolkit;

import org.polymap.p4.data.importer.ImporterPrompt;
import org.polymap.p4.data.importer.ImporterPrompt.PromptUIBuilder;

/**
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class TagFilterPromptUIBuilder
        implements PromptUIBuilder {

    private Combo                         keyList, valueList;

    private SimpleContentProposalProvider valueProposalProvider;


    @Override
    public void createContents( ImporterPrompt prompt, Composite parent, IPanelToolkit tk ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 0 ).create() );

        Collection<String> keys = keys();
        Pair<String,String> initiallySelectedItem = null;
        if(!keys.isEmpty()) {
            initiallySelectedItem = Pair.of( keys.iterator().next(), "*" );
        } else {
            initiallySelectedItem = Pair.of( "*", "*" );
        }
        Collection<String> values = values(initiallySelectedItem.getKey());
        if (values == null) {
            values = new TreeSet<String>();
        }
        values.add( "*" );

        Composite filterComposite = new Composite( parent, SWT.NONE );
        filterComposite.setLayout( FormLayoutFactory.defaults().spacing( 0 ).create() );
        Composite tagComposite = new Composite( filterComposite, SWT.NONE );
        tagComposite.setLayout( FormLayoutFactory.defaults().spacing( 0 ).create() );
        Label tagLabel = on( new Label( tagComposite, SWT.NONE ) ).fill().noRight().noBottom().control();
        tagLabel.setText( "Tag:" );
        keyList = on( createKeyFilter( tagComposite, keys.toArray( new String[keys.size()] ),
                initiallySelectedItem.getKey() ) ).top( tagLabel, 5 ).width( 150 ).noRight().control();
        Label opLabel = on( new Label( filterComposite, SWT.NONE ) ).left( tagComposite, 10 ).noRight().bottom( 90 )
                .control();
        opLabel.setText( "=" );
        Composite valueComposite = on( new Composite( filterComposite, SWT.NONE ) ).left( opLabel, 10 ).control();
        valueComposite.setLayout( FormLayoutFactory.defaults().spacing( 0 ).create() );
        Label valueLabel = on( new Label( valueComposite, SWT.NONE ) ).noBottom().control();
        valueLabel.setText( "Value:" );
        valueList = on( createValueFilter( valueComposite, values.toArray( new String[values.size()] ),
                initiallySelectedItem.getValue() ) ).top( valueLabel, 5 ).width( 150 ).right( 100 ).control();

        Composite buttonBar = new Composite( parent, SWT.NONE );
        buttonBar.setLayout( FormLayoutFactory.defaults().spacing( 0 ).create() );

        org.eclipse.swt.widgets.List selectedFilters = on(
                new org.eclipse.swt.widgets.List( parent, SWT.V_SCROLL ) )
                .fill().top( buttonBar, 10 ).width( 250 ).height( 150 ).control();
        for (Pair<String,String> item : initiallySelectedItems()) {
            if (!"*".equals( item.getKey() )) {
                selectedFilters.add( item.getKey() + "=" + item.getValue() );
            }
        }

        Button addButton = on( new Button( buttonBar, SWT.NONE ) ).left( 0 ).right( 50 ).control();
        addButton.setText( "Add" );
        addButton.addSelectionListener( new SelectionAdapter() {

            public void widgetSelected( SelectionEvent e ) {
                Pair<String,String> selection = Pair.of( getSelectedItem( keyList ), getSelectedItem( valueList ) );
                String newItem = selection.getKey() + "=" + selection.getValue();
                if (!Arrays.asList( selectedFilters.getItems() ).contains( newItem )) {
                    selectedFilters.add( newItem );
                    handleSelection( selection );
                }
            }
        } );
        Button removeButton = on( new Button( buttonBar, SWT.NONE ) ).left( addButton, 10 ).right( 100 ).control();
        removeButton.setText( "Remove" );
        removeButton.setEnabled( false );
        removeButton.addSelectionListener( new SelectionAdapter() {

            public void widgetSelected( SelectionEvent e ) {
                for (String selectedItem : selectedFilters.getSelection()) {
                    selectedFilters.remove( selectedItem );
                    String[] parts = selectedItem.split( "=" );
                    handleUnselection( Pair.of( parts[0], parts[1] ) );
                }
                removeButton.setEnabled( selectedFilters.getSelection() != null );
            }
        } );
        selectedFilters.addSelectionListener( new SelectionAdapter() {

            @Override
            public void widgetSelected( SelectionEvent e ) {
                removeButton.setEnabled( true );
            }
        } );

        on( filterComposite ).left( 0 ).right( 100 );
        on( buttonBar ).left( 0 ).right( 100 ).top( filterComposite, 20 );

        parent.pack();
    }


    private Combo createKeyFilter( Composite filterComposite, String[] listItems, String initiallySelectedItem ) {

        ComboViewer comboViewer = new ComboViewer( filterComposite, SWT.DROP_DOWN );
        Combo keys = comboViewer.getCombo();
        keys.setItems( listItems );
        keys.select( Arrays.asList( listItems ).indexOf( initiallySelectedItem ) );

        SimpleContentProposalProvider proposalProvider = new SimpleContentProposalProvider( keys.getItems() );
        ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(
                keys,
                new ComboContentAdapter(),
                proposalProvider,
                getActivationKeystroke(),
                getAutoactivationChars() );
        proposalProvider.setFiltering( true );
        proposalAdapter.setPropagateKeys( true );
        proposalAdapter.setProposalAcceptanceStyle( ContentProposalAdapter.PROPOSAL_REPLACE );
        proposalAdapter.addContentProposalListener( prop -> handleKeySelection( prop.getContent() ) );
        return keys;
    }

    private static final String LCL  = "abcdefghijklmnopqrstuvwxyz";

    private static final String UCL  = LCL.toUpperCase();

    private static final String NUMS = "0123456789";


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


    private Combo createValueFilter( Composite filterComposite, String[] listItems, String initiallySelectedItem ) {
        ComboViewer comboViewer = new ComboViewer( filterComposite, SWT.DROP_DOWN );
        Combo values = comboViewer.getCombo();
        values.setItems( listItems );
        values.select( Arrays.asList( listItems ).indexOf( initiallySelectedItem ) );

        valueProposalProvider = new SimpleContentProposalProvider( listItems );
        ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(
                values,
                new ComboContentAdapter(),
                valueProposalProvider,
                getActivationKeystroke(),
                getAutoactivationChars() );
        valueProposalProvider.setFiltering( true );
        proposalAdapter.setPropagateKeys( true );
        proposalAdapter.setProposalAcceptanceStyle( ContentProposalAdapter.PROPOSAL_REPLACE );
        return values;
    }


    protected void handleKeySelection( String selectedItem ) {
        valueList.removeAll();
        valueList.add( "*" );
        for (String value : listItems().get( selectedItem )) {
            valueList.add( value );
        }
        valueList.setText( "*" );
        valueProposalProvider.setProposals( valueList.getItems() );
    }


    protected List<String> filterSelectableKeys( String text ) {
        return listItems().keySet().stream()
                .filter( item -> item.toLowerCase().contains( text.toLowerCase() ) )
                .collect( Collectors.toList() );
    }


    protected List<String> filterSelectableValues( String text ) {
        return listItems().get( getSelectedItem( keyList ) ).stream()
                .filter( item -> item.toLowerCase().contains( text.toLowerCase() ) )
                .collect( Collectors.toList() );
    }


    private String getSelectedItem( Combo combo ) {
        return combo.getItem( combo.getSelectionIndex() );
    }


    protected Collection<String> keys() {
        return listItems().keySet();
    }


    protected Collection<String> values( String key ) {
        return listItems().get( key );
    }


    protected abstract SortedMap<String,SortedSet<String>> listItems();


    protected abstract List<Pair<String,String>> initiallySelectedItems();


    protected abstract void handleSelection( Pair<String,String> selectedItem );


    protected abstract void handleUnselection( Pair<String,String> selectedItem );
}
