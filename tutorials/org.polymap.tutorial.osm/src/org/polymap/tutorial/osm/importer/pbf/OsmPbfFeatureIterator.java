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
package org.polymap.tutorial.osm.importer.pbf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.ParseException;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import org.polymap.tutorial.osm.importer.Fileformat;
import org.polymap.tutorial.osm.importer.Osmformat;
import org.polymap.tutorial.osm.importer.Osmformat.PrimitiveGroup;
import org.polymap.tutorial.osm.importer.Osmformat.StringTable;
import org.polymap.tutorial.osm.importer.TagFilter;

/**
 * This class uses {@link org.polymap.tutorial.osm.importer.Fileformat} and
 * {@link org.polymap.tutorial.osm.importer.Osmformat}, which were generated out of
 * fileformat.proto resp. osmformat.proto (also in this package) by using Google
 * ProtoBuffers Compiler (protoc) in version 2.4.1 that have to be installed and
 * executed separately (but only if the <a
 * href="wiki.openstreetmap.org/wiki/PBF_Format">PBF format</a> changes).
 * 
 */
class OsmPbfFeatureIterator
        implements Iterator<SimpleFeature> {

    private static final int                      HEAD_MAX_SIZE                  = 64 * 1024;

    private static final int                      BODY_MAX_SIZE                  = 32 * 1024 * 1024;

    private final OsmPbfIterableFeatureCollection iterableFeatureCollection;

    private final SimpleFeatureBuilder            featureBuilder;

    private final InputStream                     input;

    final List<String>                            keys;

    private final int                             limit;

    private int                                   index       = 0;

    private Double                                currentLon                     = null;

    private Double                                currentLat                     = null;

    private Map<String,String>                    currentTags                    = null;

    private int                                   size                           = -1;

    private Osmformat.PrimitiveBlock              currentPrimblock               = null;

    private int                                   currentPrimblockPosition       = -1;

    private int                                   currentGranularity             = -1;

    private long                                  currentLatOffset               = -1;

    private long                                  currentLonOffset               = -1;

    private int                                   currentNextDenseNodePosition   = -1;

    private String[]                              currentStrings                 = null;

    private int                                   currentTagIndex                = -1;

    private int                                   currentPrimitivegroupListIndex = -1;

    private int                                   currentInputStreamPosition     = 0;

    public OsmPbfFeatureIterator( OsmPbfIterableFeatureCollection iterableFeatureCollection, int limit ) throws IOException {
        this.iterableFeatureCollection = iterableFeatureCollection;
        this.limit = limit;
        featureBuilder = new SimpleFeatureBuilder( iterableFeatureCollection.getSchema() );
        keys = OsmPbfIterableFeatureCollection.getKeys( this.iterableFeatureCollection.getFilters() );
        input = this.iterableFeatureCollection.getUrl().openStream();
    }


    @Override
    public boolean hasNext() {
        return hasNext( false );
    }


    private boolean hasNext( boolean countMode ) {
        if (limit >= 0 && index > limit) {
            return false;
        }
        try {
            if (!countMode && currentLon != null) {
                return true;
            }
            while (currentPrimitivegroupListIndex != -1) {
                handlePrimitivegroupList( currentPrimitivegroupListIndex, countMode );
                if (!countMode && currentLon != null) {
                    return true;
                }
            }
            while (currentInputStreamPosition != -1) {
                handlePbfInputStream( countMode );
                if (currentPrimitivegroupListIndex != -1) {
                    return true;
                }
            }
        }
        catch (IOException | ParseException e) {
            this.iterableFeatureCollection.setException( e );
        }
        complete();
        return false;
    }


    @Override
    public SimpleFeature next() {
        double longitude = currentLon;
        double latitude = currentLat;
        GeometryFactory gf = new GeometryFactory();
        Point point = gf.createPoint( new Coordinate( longitude, latitude ) );
        featureBuilder.add( point );
        Map<String,String> attributes = new HashMap<String,String>();
        attributes.putAll( currentTags );
        iterableFeatureCollection.updateBBOX( longitude, latitude );
        for (String key : keys) {
            featureBuilder.add( attributes.get( key ) );
        }
        currentLon = null;
        currentTags = null;
        if (!hasNext()) {
            complete();
        }
        return featureBuilder.buildFeature( null );
    }


    public void complete() {
        try {
            input.close();
        }
        catch (IOException e) {
            this.iterableFeatureCollection.setException( e );
        }
    }


    public int size() {
        if (size == -1) {
            try {
                // by this a new input stream is created for the URL
                // trade-off: two (API/file) requests (with stream same content)
                // (this is the current way) vs.
                // one (API/file) request and then storing node objects while
                // counting an reusing them when building feature
                OsmPbfFeatureIterator osmFeatureIterator = new OsmPbfFeatureIterator( this.iterableFeatureCollection, -1 );
                int count = 0;
                while (osmFeatureIterator.hasNext( true )) {
                    count++;
                }
                size = count;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return size;
    }


    private void handlePbfInputStream( boolean countMode ) throws IOException, ParseException {
        DataInputStream dataIn = new DataInputStream( input );
        byte headerSizeBuffer[] = new byte[4];
        if (input.read( headerSizeBuffer, 0, 1 ) == -1) {
            currentInputStreamPosition = -1;
            return;
        }
        dataIn.readFully( headerSizeBuffer, 1, 3 );
        int headerDataSize = ByteBuffer.wrap( headerSizeBuffer ).getInt();
        currentInputStreamPosition += 4;
        if (headerDataSize > HEAD_MAX_SIZE) {
            throw new ParseException( "Header is with " + headerDataSize + " longer than " + HEAD_MAX_SIZE + " bytes.",
                    currentInputStreamPosition );
        }
        byte headerBuffer[] = new byte[headerDataSize];
        if (input.read( headerBuffer, 0, 1 ) == -1) {
            currentInputStreamPosition = -1;
            return;
        }
        dataIn.readFully( headerBuffer, 1, headerDataSize - 1 );
        currentInputStreamPosition += headerDataSize;
        Fileformat.BlobHeader header = Fileformat.BlobHeader.parseFrom( headerBuffer );
        String type = header.getType();
        int contentDataSize = header.getDatasize();
        if (contentDataSize > BODY_MAX_SIZE) {
            throw new ParseException( "Body is with " + contentDataSize + " longer than " + BODY_MAX_SIZE + " bytes.",
                    currentInputStreamPosition );
        }
        if (type.equals( "OSMData" )) {
            byte contentBuffer[] = new byte[contentDataSize];
            if (input.read( contentBuffer, 0, 1 ) == -1) {
                currentInputStreamPosition = -1;
                return;
            }
            dataIn.readFully( contentBuffer, 1, contentDataSize - 1 );
            Fileformat.Blob blob = Fileformat.Blob.parseFrom( contentBuffer );
            ByteString contentData = null;
            if (blob.hasRaw()) {
                contentData = blob.getRaw();
            }
            else if (blob.hasZlibData()) {
                byte zlibDataBuffer[] = new byte[blob.getRawSize()];
                Inflater decompresser = new Inflater();
                decompresser.setInput( blob.getZlibData().toByteArray() );
                try {
                    decompresser.inflate( zlibDataBuffer );
                }
                catch (DataFormatException e) {
                    throw new Error( e );
                }
                decompresser.end();
                contentData = ByteString.copyFrom( zlibDataBuffer );
            }
            handlePrimitiveBlock( countMode, contentData );
            contentData = null;
        }
        else {
            input.skip( contentDataSize );
        }
        currentInputStreamPosition += contentDataSize;
    }


    private void handlePrimitiveBlock( boolean countMode, ByteString contentData )
            throws InvalidProtocolBufferException, IOException {
        currentPrimblock = Osmformat.PrimitiveBlock.parseFrom( contentData );
        contentData = null;
        StringTable stringTable = currentPrimblock.getStringtable();
        currentStrings = new String[stringTable.getSCount()];
        for (int i = 0; i < currentStrings.length; i++) {
            currentStrings[i] = stringTable.getS( i ).toStringUtf8();
        }
        currentGranularity = currentPrimblock.getGranularity();
        currentLatOffset = currentPrimblock.getLatOffset();
        currentLonOffset = currentPrimblock.getLonOffset();
        handlePrimitivegroupList( 0, countMode );
    }


    private void handlePrimitivegroupList( int primitivegroupListIndex, boolean countMode ) throws IOException {
        boolean matches = false;
        List<PrimitiveGroup> groupList = currentPrimblock.getPrimitivegroupList();
        Osmformat.PrimitiveGroup groupmessage = null;
        for (int i = primitivegroupListIndex; i < groupList.size(); i++) {
            groupmessage = currentPrimblock.getPrimitivegroupList().get( i );
            if (groupmessage.hasDense()) {
                currentTagIndex = currentNextDenseNodePosition == -1 ? 0 : currentTagIndex-1;
                matches = handleDenseNodes( groupmessage.getDense(), currentNextDenseNodePosition, countMode );
                if (matches) {
                    if(currentNextDenseNodePosition != -1) {
                        currentPrimitivegroupListIndex = i;
                    } else {
                        currentPrimitivegroupListIndex = currentPrimitivegroupListIndex + 1 == groupList.size() ? -1
                                : currentPrimitivegroupListIndex + 1;
                    }
                    return;
                }
            }
        }
        currentPrimitivegroupListIndex = -1;
    }


    private boolean handleDenseNodes( org.polymap.tutorial.osm.importer.Osmformat.DenseNodes nodes,
            int nextDenseNodePosition, boolean countMode )
            throws IOException {
        boolean matches = false;
        for (int i = nextDenseNodePosition; i < nodes.getIdCount(); i++) {
            matches = handleDenseNode( nodes, countMode, i );
            if (matches) {
                return true;
            }
        }
        return false;
    }


    private boolean handleDenseNode( org.polymap.tutorial.osm.importer.Osmformat.DenseNodes nodes, boolean countMode,
            int denseNodesIndex ) {
        String key;
        String value;
        Map<String,String> tagMap = new HashMap<String,String>();
        if (nodes.getKeysValsCount() > 0) {
            while (nodes.getKeysVals( currentTagIndex ) != 0) {
                int keyid = nodes.getKeysVals( currentTagIndex++ );
                int valid = nodes.getKeysVals( currentTagIndex++ );
                key = currentStrings[keyid];
                value = currentStrings[valid];
                tagMap.put( key, value );
            }
            currentTagIndex++;
        }
        boolean matches = this.iterableFeatureCollection.getFilters().size() > 0;
        for (TagFilter filter : this.iterableFeatureCollection.getFilters()) {
            if (!(filter.key() == "*"
            || (tagMap.containsKey( filter.key() ) && (filter.value() == "*") || (filter
                    .value() != null && filter.value().equals(
                    (tagMap.get( filter.key() )) ))))) {
                matches = false;
                break;
            }
        }
        if (matches) {
            if (!countMode) {
                currentLat = parseLon( nodes.getLat( denseNodesIndex ) );
                currentLon = parseLat( nodes.getLon( denseNodesIndex ) );
                currentTags = tagMap;
                currentNextDenseNodePosition = denseNodesIndex >= nodes.getIdCount() ? -1 : denseNodesIndex + 1;
                index++;
            }
            return true;
        }
        return false;
    }


    public double parseLat( long lat ) {
        return (currentGranularity * lat + currentLatOffset) * 0.000000001;
    }


    public double parseLon( long lon ) {
        return (currentGranularity * lon + currentLonOffset) * 0.000000001;
    }
}