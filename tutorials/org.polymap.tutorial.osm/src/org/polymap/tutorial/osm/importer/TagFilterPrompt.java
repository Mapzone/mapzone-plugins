/*
 * polymap.org 
 * Copyright (C) 2015-2017, Falko Bräutigam. All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.tutorial.osm.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import java.io.IOException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Joiner;

import org.polymap.p4.data.importer.ImporterPrompt;
import org.polymap.p4.data.importer.ImporterPrompt.Severity;
import org.polymap.p4.data.importer.ImporterSite;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class TagFilterPrompt {

    private static final Log log = LogFactory.getLog( TagFilterPrompt.class );

    private static final List<Pair<String,String>> DEFAULT = Arrays.asList( Pair.of( "name", "*" ) );

    private ImporterSite                     site;

    private List<Pair<String,String>>        selection;

    private final ImporterPrompt             prompt;


    public TagFilterPrompt( ImporterSite site ) {
        this.site = site;
        selection = new ArrayList<Pair<String,String>>();
        selection.addAll( DEFAULT );

        prompt = site.newPrompt( "tagFilter" )
                .summary.put( "Tag filter" )
                .description.put( "Filters features by their tags" )
                .value.put( getReadable() )
                .severity.put( Severity.VERIFY )
                .ok.put( false )
                .extendedUI.put( new TagFilterPromptUIBuilder() {

                    private Collection<String>        keys;

                    private SortedMap<String,SortedSet<String>> tags;

                    @Override
                    public void submit( ImporterPrompt ip ) {
                        ip.value.put( getReadable() );
                        ip.ok.set( true );
                    }

                    @Override
                    protected SortedMap<String,SortedSet<String>> listItems() {
                        if (tags == null) {
                            tags = new TreeMap<String,SortedSet<String>>();
                            TreeSet<String> star = new TreeSet<String>();
                            star.add( "*" );
                            tags.put( "*", star );
                            try {
                                tags.putAll( TagStaticInfo.getStaticTags() );
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return tags;
                    }

                    @Override
                    protected List<Pair<String,String>> initiallySelectedItems() {
                        return selection;
                    }

                    @Override
                    protected void handleSelection( Pair<String,String> selected ) {
                        selection.add( selected );
                        assert selection != null;
                    }

                    @Override
                    protected void handleUnselection( Pair<String,String> selected ) {
                        selection.remove( selected );
                        assert selection != null;
                    }
                });
    }


    private String getReadable() {
        String readable = Joiner.on( "," ).join(
                selection.stream().map( filter -> filter.getKey() + "=" + filter.getValue() )
                        .collect( Collectors.toList() ) );
        if (readable.length() > 80) {
            readable = readable.substring( 0, 80 ) + " ...";
        }
        return readable;
    }


    /**
     * The selected tags to use as filter.
     */
    public List<Pair<String,String>> selection() {
        return selection;
    }


    public boolean isOk() {
        return prompt.ok.get();
    }
}
