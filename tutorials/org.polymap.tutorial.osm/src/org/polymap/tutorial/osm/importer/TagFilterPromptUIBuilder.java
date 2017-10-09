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
import static org.polymap.core.ui.UIUtils.selectionListener;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.FluentIterable;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;

import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.p4.P4Plugin;
import org.polymap.p4.data.importer.ImporterPrompt;
import org.polymap.p4.data.importer.ImporterPrompt.PromptUIBuilder;
import org.polymap.tutorial.osm.importer.taginfo.TagInfo;
import org.polymap.tutorial.osm.importer.taginfo.TagInfo.Sort;

/**
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class TagFilterPromptUIBuilder
        implements PromptUIBuilder {

    private static final Log log = LogFactory.getLog( TagFilterPromptUIBuilder.class );

    public static final int         AUTO_ACTIVATION_DELAY = 750;

    private TagInfo                 tagInfo;
    
    private Text                    keyText, valueText;

    private ListViewer              filterList;
    
    private ContentProposalAdapter  valueProposals;

    /** Start value and result. */
    private List<TagFilter>         filters;
    
    /** Result */
    protected Class<? extends Geometry> geomType = MultiPolygon.class;

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
        Label msg1 = tk.createLabel( parent, "Choose the tags/values to filter.<br/>"
                + "<b>Wildcards</b> allowed in value:"
                + "<ul>"
                + "<li>* : zero or more characters</li>"
                + "<li>+ : one or more characters</li>"
                + "<li>? : zero or one character</li>"
                + "</ul>"
                + "A single * selects all object that do have the tag (with any value). "
                + "Filtered tags appear in the result as attributes. "
                + "An <b>empty value</b> specifies that the tag appears in the result without filtering.", SWT.WRAP );
        msg1.setEnabled( false );
        
        // keyText
        Label keyLabel = tk.createLabel( parent, "Tag" );
        keyText = createKeyText( parent, tk );
        keyText.forceFocus();

        // valueText
        Label opLabel = tk.createLabel( parent, "=" );
        Label valueLabel = tk.createLabel( parent, "Value" );
        valueText = createValueFilter( parent, tk );
        
        // addBtn
        Button addBtn = tk.createButton( parent, null, SWT.PUSH );
        addBtn.setImage( P4Plugin.images().svgImage( "check.svg", SvgImageRegistryHelper.WHITE24 ) );
        addBtn.setToolTipText( "Add a new entry or update the current selection" );
        addBtn.addSelectionListener( new SelectionAdapter() {
            @Override public void widgetSelected( SelectionEvent ev ) {
                Optional<TagFilter> current = filters.stream()
                        .filter( filter -> filter.key().equals( keyText.getText() ) )
                        .findAny();
                if (current.isPresent()) {
                    current.get().setValue( valueText.getText() );
                }
                else {
                    TagFilter filter = TagFilter.of( keyText.getText(), valueText.getText() );
                    filters.add( filter );
                }
                filterList.refresh();
            }
        });
        
        // removeBtn
        Button removeBtn = tk.createButton( parent, null, SWT.PUSH );
        removeBtn.setImage( P4Plugin.images().svgImage( "delete.svg", SvgImageRegistryHelper.WHITE24 ) );
        removeBtn.setToolTipText( "Remove the selected filter from the list" );
        removeBtn.setEnabled( false );
        removeBtn.addSelectionListener( new SelectionAdapter() {
            @Override public void widgetSelected( SelectionEvent ev ) {
                org.polymap.core.ui.SelectionAdapter.on( filterList.getSelection() )
                        .first( TagFilter.class ).ifPresent( selected -> {
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
                
                UIUtils.selection( filterList.getSelection() ).first( TagFilter.class )
                        .ifPresent( selected -> {
                            keyText.setText( selected.key() );
                            valueText.setText( selected.value() );
                            valueText.forceFocus();
                        });
            }
        });
        filterList.setInput( filters );

        // geometry
        Label msg2 = tk.createLabel( parent, "Choose the <b>geometry type</b> of the result. If you choose Polygons, then all closed line strings are imported as polygons. Other lines and points are omitted.", SWT.WRAP );
        msg2.setEnabled( false );
        Button pointBtn = tk.createButton( parent, "Points", SWT.RADIO );
        pointBtn.addSelectionListener( selectionListener( ev -> geomType = Point.class ) );
        Button lineBtn = tk.createButton( parent, "Lines", SWT.RADIO );
        lineBtn.addSelectionListener( selectionListener( ev -> geomType = MultiLineString.class ) );
        Button polygonBtn = tk.createButton( parent, "Polygons", SWT.RADIO );
        polygonBtn.addSelectionListener( selectionListener( ev -> geomType = MultiPolygon.class ) );
        polygonBtn.setSelection( true );
        
        // layout
        parent.setLayout( FormLayoutFactory.defaults().spacing( 5 ).margins( 3 ).create() );
        on( msg1 ).fill().noBottom().width( 200 ).height( 160 );
        
        on( keyLabel ).top( msg1 ).left( 0 );
        on( keyText ).top( keyLabel, -7 ).left( 0 ).width( 90 );
        on( opLabel ).top( keyLabel, -3 ).left( keyText );
        on( valueText ).top( keyLabel, -7 ).left( opLabel ).width( 90 );

        on( addBtn ).top( keyLabel, -9 ).left( valueText ).height( 30 );
        on( removeBtn ).top( keyLabel, -9 ).left( addBtn ).height( 30 ).right( 100 );
        
        on( valueLabel ).top( msg1 ).left( opLabel );

        on( filterList.getList() ).fill().top( addBtn ).noBottom().width( 330 ).height( 150 );
        on( msg2 ).fill().top( filterList.getControl(), 8 ).noBottom().width( 200 ).height( 70 );
        on( polygonBtn ).left( 0 ).top( msg2 );
        on( lineBtn ).left( polygonBtn ).top( msg2 );
        on( pointBtn ).left( lineBtn ).top( msg2 );
    }


    protected Text createKeyText( Composite parent, IPanelToolkit tk ) {
        keyText = tk.createText( parent, null, SWT.BORDER );
        keyText.setToolTipText( "A valid OSM tag that appears in the imported data" );
        
        ContentProposalAdapter proposals = proposal( keyText, () -> {
            return tagInfo.keys( keyText.getText(), Sort.count_all, 50 );
        });
        proposals.addContentProposalListener( ev -> {
            valueText.setText( "" );
            valueText.forceFocus();
        });
        return keyText;
    }

    
    protected Text createValueFilter( Composite parent, IPanelToolkit tk ) {
        valueText = tk.createText( parent, null, SWT.BORDER );
        valueText.setToolTipText( "The value of the given tag<br/>Allowed <b>wildcards</b> are: *, +, ?" );

        valueProposals = proposal( valueText, () -> {
            String text = valueText.getText();
            return !StringUtils.containsAny( text, TagFilter.WILDCARDS )
                    ? tagInfo.values( keyText.getText(), text, Sort.count_all, 50 )
                    : Collections.EMPTY_LIST;
        });
        return valueText;
    }

    
    protected ContentProposalAdapter proposal( Text control, Callable<Iterable> supplier ) {
        IContentProposalProvider proposalProvider = new IContentProposalProvider() {
            @Override public IContentProposal[] getProposals( String text, int pos ) {
                try {
                    if (text.length() < 2) {
                        return new IContentProposal[0];
                    }
                    return (IContentProposal[])FluentIterable.from( supplier.call() )
                            .transform( s -> new ContentProposal( s.toString() ) )
                            .toArray( IContentProposal.class );
                }
                catch (Exception e) {
                    log.warn( "", e );
                    return new IContentProposal[0];
                }
            }
        };
        ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(
                control, new TextContentAdapter(), proposalProvider, null, null );
        proposalAdapter.setPropagateKeys( true );
        proposalAdapter.setAutoActivationDelay( AUTO_ACTIVATION_DELAY );
        proposalAdapter.setProposalAcceptanceStyle( ContentProposalAdapter.PROPOSAL_REPLACE );
        return proposalAdapter;
    }
    
}
