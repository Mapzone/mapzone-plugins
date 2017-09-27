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

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.ParseException;

import com.google.protobuf.ByteString;

/**
 * @author Joerg Reichert <joerg@mapzone.io>
 *
 */
public class TagPbfInfo {

    private static final int HEAD_MAX_SIZE = 64 * 1024;

    private static final int BODY_MAX_SIZE = 32 * 1024 * 1024;


    public static TreeMap<String,SortedSet<String>> getTagsFromContent( InputStream input ) throws Exception {
        TreeMap<String,SortedSet<String>> allTags = new TreeMap<String,SortedSet<String>>();
        int position = 0;
        while (position >= 0) {
            position = handlePbf( allTags, input, position );
        }
        return allTags;
    }


    private static int handlePbf( TreeMap<String,SortedSet<String>> allTags, InputStream input, int position )
            throws IOException, ParseException {
        DataInputStream dataIn = new DataInputStream( input );
        byte headerSizeBuffer[] = new byte[4];
        if (input.read( headerSizeBuffer, 0, 1 ) == -1) {
            return -1;
        }
        dataIn.readFully( headerSizeBuffer, 1, 3 );
        int headerDataSize = ByteBuffer.wrap( headerSizeBuffer ).getInt();
        position += 4;
        if (headerDataSize > HEAD_MAX_SIZE) {
            throw new ParseException( "Header is with " + headerDataSize + " longer than " + HEAD_MAX_SIZE + " bytes.",
                    position );
        }
        byte headerBuffer[] = new byte[headerDataSize];
        if (input.read( headerBuffer, 0, 1 ) == -1) {
            return -1;
        }
        dataIn.readFully( headerBuffer, 1, headerDataSize - 1 );
        position += headerDataSize;
        Fileformat.BlobHeader header = Fileformat.BlobHeader.parseFrom( headerBuffer );
        String type = header.getType();
        int contentDataSize = header.getDatasize();
        if (contentDataSize > BODY_MAX_SIZE) {
            throw new ParseException( "Body is with " + contentDataSize + " longer than " + BODY_MAX_SIZE + " bytes.",
                    position );
        }
        if (type.equals( "OSMData" )) {
            byte contentBuffer[] = new byte[contentDataSize];
            if (input.read( contentBuffer, 0, 1 ) == -1) {
                return -1;
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
            Osmformat.PrimitiveBlock primblock = Osmformat.PrimitiveBlock.parseFrom( contentData );
            Osmformat.StringTable stablemessage = primblock.getStringtable();
            String[] strings = new String[stablemessage.getSCount()];
            for (int i = 0; i < strings.length; i++) {
                strings[i] = stablemessage.getS( i ).toStringUtf8();
            }
            int granularity = primblock.getGranularity();
            long lat_offset = primblock.getLatOffset();
            long lon_offset = primblock.getLonOffset();
            for (Osmformat.PrimitiveGroup groupmessage : primblock.getPrimitivegroupList()) {
                if (groupmessage.hasDense()) {
                    handleDenseNodes( allTags, groupmessage.getDense(), strings, granularity, lat_offset, lon_offset );
                }
            }
        }
        else {
            input.skip( contentDataSize );
        }
        position += contentDataSize;
        return position;
    }

    private static void handleDenseNodes( TreeMap<String,SortedSet<String>> allTags,
            org.polymap.tutorial.osm.importer.Osmformat.DenseNodes nodes, String[] stringTable, int granularity,
            long lat_offset, long lon_offset ) throws IOException {
        String key = null;
        String value = null;
        int j = 0;
        for (int i = 0; i < nodes.getIdCount(); i++) {
            if (nodes.getKeysValsCount() > 0) {
                while (nodes.getKeysVals( j ) != 0) {
                    int keyId = nodes.getKeysVals( j++ );
                    int valueId = nodes.getKeysVals( j++ );
                    key = stringTable[keyId];
                    value = stringTable[valueId];
                    collectTags( allTags, key, value );
                }
                j++;
            }
        }
    }

    public static double parseLat( int granularity, long lat_offset, long lat ) {
        return (granularity * lat + lat_offset) * 0.000000001;
    }


    public static double parseLon( int granularity, long lon_offset, long lon ) {
        return (granularity * lon + lon_offset) * 0.000000001;
    }


    private static void collectTags( SortedMap<String,SortedSet<String>> tags, String key, String value ) {
        SortedSet<String> values = tags.get( key );
        if (values == null) {
            values = new TreeSet<String>();
            tags.put( key, values );
        }
        if (value != null && !values.contains( value )) {
            values.add( value );
        }
    }
}
