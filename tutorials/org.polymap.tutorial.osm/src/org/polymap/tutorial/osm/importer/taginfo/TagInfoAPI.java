/* 
 * polymap.org
 * Copyright (C) 2017, the @authors. All rights reserved.
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
package org.polymap.tutorial.osm.importer.taginfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.io.BufferedInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Joiner;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class TagInfoAPI
        extends TagInfo {

    private static final Log log = LogFactory.getLog( TagInfoAPI.class );

    //public static final JsonFactory JACKSON = new JsonFactory();

    private String                  baseUrl = "https://taginfo.openstreetmap.org/api/4/";
    
    /**
     * @see <a href="https://taginfo.openstreetmap.org/taginfo/apidoc#api_4_keys_all">TagInfo Doc</a>
     */
    @Override
    public ResultSet<String> keys( String query, Sort sort, int maxResults ) throws Exception {
        Map<String,String> params = new HashMap(); 
        if (query != null) {
            params.put( "query", query );
        }
        params.put( "page", "1" );
        params.put( "rp", String.valueOf( maxResults ) );
        params.put( "filter", "in_wiki" );
        params.put( "sortname", sort.name() );
        params.put( "sortorder", "desc" );
        String url = baseUrl + "keys/all?" + Joiner.on( '&' ).withKeyValueSeparator( "=" ).join( params );
        log.info( url );
        
        HttpsURLConnection conn = allTrustingHttpConnection( new URL( url ) );
        try (
            InputStream in = new BufferedInputStream( conn.getInputStream() );
        ){
            Reader reader = new InputStreamReader( in, "UTF-8" );
            //reader = new LogReader( reader );
            JSONObject json = new JSONObject( new JSONTokener( reader ) );
            return new ResultSet<String>() {
                private JSONArray array = json.getJSONArray( "data" );
                @Override
                public Iterator<String> iterator() {
                    return new Iterator<String>() {
                        private int index;
                        @Override
                        public boolean hasNext() {
                            return index < array.length();
                        }
                        @Override
                        public String next() {
                            JSONObject data = array.getJSONObject( index++ );
                            return data.getString( "key" );
                        }
                    };
                }
                @Override
                public int size() {
                    return array.length();  //json.getInt( "total" );
                }
            };
        }
    }

    
    @Override
    public ResultSet<String> values( String key, Sort sort, int maxResults ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    /**
     * FIXME the certificate of https://taginfo.openstreetmap.org cannot be verified
     * by the JRE, so we trust everything for this connection
     *
     * @param url
     * @return
     */
    protected HttpsURLConnection allTrustingHttpConnection( URL url ) throws Exception {
        HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted( X509Certificate[] certs, String authType ) { }
            public void checkServerTrusted( X509Certificate[] certs, String authType ) { }
        }};
        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance( "SSL" );
        sc.init( null, trustAllCerts, new java.security.SecureRandom() );
        conn.setSSLSocketFactory( sc.getSocketFactory() );
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { return true; }
        };
        // Install the all-trusting host verifier
        conn.setHostnameVerifier( allHostsValid );
        return conn;
    }
    
    /**
     * 
     */
    protected static class LogReader
            extends FilterReader {

        protected LogReader( Reader in ) {
            super( in );
        }

        @Override
        public int read( char[] cbuf, int off, int len ) throws IOException {
            int result = super.read( cbuf, off, len );
            System.out.println( cbuf );
            return result;
        }
    }
    
}
