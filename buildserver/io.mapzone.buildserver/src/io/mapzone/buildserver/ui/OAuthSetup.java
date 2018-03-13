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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.mapzone.buildserver.ui.OAuth.API;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class OAuthSetup
        implements Iterable<OAuth> {

    private static final Log log = LogFactory.getLog( OAuthSetup.class );
    
    public static OAuthSetup readJsonConfig( String json ) throws Exception {
        OAuthSetup result = new OAuthSetup();
        
        JSONObject config = new JSONObject( json );
        String callbackUri = config.optString( "callbackUri" );
        
        JSONArray a = config.getJSONArray( "auths" );
        for (int i=0; i<a.length(); i++) {
            JSONObject j = a.getJSONObject( i );
            Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass( j.getString( "class" ) );
            OAuth auth = (OAuth)cl.newInstance();
            auth.callbackUri.set( callbackUri );
            auth.label.set( j.optString( "label" ) );
            auth.icon.set( j.optString( "icon" ) );
            auth.baseUri.set( j.getString( "baseUri" ) );
            auth.clientId.set( j.getString( "clientId" ) );
            auth.clientSecret.set( j.getString( "clientSecret" ) );
            auth.loginUri.set( j.getString( "loginUri" ) );
            
            result.auths.add( auth );
        }
        return result;
    }

    
    // instance *******************************************  
    
    private List<OAuth>             auths = new ArrayList();
    
    
//    private Cache<String,String>    stateTokens = CacheBuilder.newBuilder()
//            .expireAfterAccess( 5, TimeUnit.MINUTES )
//            .initialCapacity( 64 )
//            .concurrencyLevel( 2 )
//            .build();
    
    
    /**
     * Checks the given request params to see if it contains valid authentication
     * information.
     *
     * @param params The HTTP request params
     * @return True if successfully authenticated.
     */
    public Optional<OAuth.API> isAuthenticated() throws IOException {
        for (OAuth auth : auths) {
            Optional<API> api = auth.isAuthenticated();
            if (api.isPresent()) {
                return api;
            }
        }
        return Optional.empty();
        
//        try (ExceptionCollector<IOException> excs = Streams.exceptions()) {
//            return auths.stream().map( auth -> excs.check( () -> auth.isAuthenticated() ) ).findFirst();
//        }
    }


    @Override
    public Iterator<OAuth> iterator() {
        return auths.iterator();
    }

}
