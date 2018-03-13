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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Joiner;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.rap.rwt.RWT;

import org.polymap.core.runtime.config.Config;
import org.polymap.core.runtime.config.Configurable;
import org.polymap.core.runtime.config.Immutable;
import org.polymap.core.runtime.config.Mandatory;

import org.polymap.rhei.batik.BatikApplication;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;

import io.mapzone.buildserver.BsPlugin;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class OAuth
        extends Configurable {

    private static final Log log = LogFactory.getLog( OAuth.class );
    
    public static final String      COOKIE_NAME = "oauth_state_token";
    public static final String      COOKIE_PATH = "/";
    public static final int         COOKIE_MAX_AGE = (int)TimeUnit.MINUTES.toSeconds( 10 );

    @Immutable
    @Mandatory
    public Config<String>       label;
    
    @Immutable
    @Mandatory
    public Config<String>       baseUri;
    
    @Immutable
    @Mandatory
    public Config<String>       clientId;
    
    @Immutable
    @Mandatory
    public Config<String>       clientSecret;
    
    @Immutable
    public Config<String[]>     scopes;
    
    @Immutable
    public Config<String>       icon;
    
    @Immutable
    public Config<String>       callbackUri;

    @Immutable
    @Mandatory
    public Config<String>       loginUri;

    
    protected abstract API newAPI( String accessToken );

    
    public Optional<Image> icon( String configName ) {
        return icon.map( name -> BsPlugin.images().svgImage( name, configName ) );
    }
    

    public String url() {
        String stateToken = sendNewStateCookie();
        
        StringBuilder buf = new StringBuilder( 256 ).append( baseUri.get() );
        buf.append( '?' ).append( "state=" ).append( stateToken );
        buf.append( '&' ).append( "client_id=" ).append( clientId.get() );
        callbackUri.ifPresent( url -> buf.append( '&' ).append( "redirect_uri=" ).append( url ) );
        scopes.ifPresent( v -> {
            buf.append( "&scope=" ).append( Joiner.on( ',' ).join( v ) );
        });
        return buf.toString();
    }
    
    
    public CLabel createControl( Composite parent ) {
        CLabel control = new CLabel( parent, SWT.CENTER );
        control.setText( "<a href=\"" + url() + "\" "
                + "style=\"font-size: 21px;\""
                + ">" + label.get() + "</a>" );
        icon( SvgImageRegistryHelper.ACTION24 ).ifPresent( i -> control.setImage( i ) );
        control.setAlignment( SWT.CENTER );
        control.setBackground( parent.getBackground() );
        return control;
    }
    

    /**
     * Checks the current {@link RWT#getRequest()} to see if it contains valid authentication
     * information.
     *
     * @param params The HTTP request params
     * @return True if successfully authenticated.
     * @throws IOException 
     */
    public Optional<API> isAuthenticated() throws IOException {
        HttpServletRequest request = RWT.getRequest();
        Optional<Cookie> stateCookie = Arrays.stream( request.getCookies() )
                .filter( c -> c.getName().equals( COOKIE_NAME ) ).findFirst();

        if (stateCookie.isPresent()) {
            Optional<String> state = BatikApplication.instance().getInitRequestParameter( "state" );
            if (state.isPresent()) {
                if (state.get().equals( stateCookie.get().getValue() )) {
                    stateCookie.get().setValue( "" );
                    stateCookie.get().setMaxAge( 0 );
                    RWT.getResponse().addCookie( stateCookie.get() );
                    
                    Optional<String> code = BatikApplication.instance().getInitRequestParameter( "code" );
                    if (code.isPresent()) {
                        return Optional.of( newAPI( requestAccessToken( state.get(), code.get() ) ) );
                    }
                }
            }
        }
        return Optional.empty();
    }
    
    
    protected String requestAccessToken( String state, String code ) throws IOException {
        URL url = new URL( loginUri.get() );
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod( "POST" );
        http.setDoOutput( true );  
        http.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8" );
        String params = Joiner.on( '&' ).withKeyValueSeparator( "=" ).join( new HashMap() {{
            put( "client_id", URLEncoder.encode( clientId.get(), "UTF-8" ) );
            put( "client_secret", URLEncoder.encode( clientSecret.get(), "UTF-8" ) );
            put( "state", state );
            put( "code", code );
        }});
        http.setRequestProperty( "Content-Length", String.valueOf( params.length() ) );
        http.setRequestProperty( "Accept", "application/json" );
        
        try (OutputStream out = http.getOutputStream()) {
            out.write( params.getBytes( "UTF-8" ) );
        }
        try (InputStream in = http.getInputStream()) {
            String response = IOUtils.toString( in, "UTF-8" );
            JSONObject json = new JSONObject( response );
            String accessToken = json.optString( "access_token" );
            if (accessToken != null) {
                return accessToken;
            }
            else {
                // XXX handle error in response
                throw new IOException( "No access_token." );
            }
        }
    }

    
    /**
     * An unguessable random string. It is used to protect against cross-site request
     * forgery attacks.
     */
    public String newStateToken() {
        return RandomStringUtils.random( 24, true, true );
    }


    protected String sendNewStateCookie() {
        String token = newStateToken();
        Cookie cookie = new Cookie( COOKIE_NAME, token );
        cookie.setHttpOnly( true );
        cookie.setPath( COOKIE_PATH );
        cookie.setSecure( false ); // XXX
        cookie.setMaxAge( COOKIE_MAX_AGE );
        RWT.getResponse().addCookie( cookie );
        log.info( "Set: value=" + cookie.getValue() + ", path=" + cookie.getPath() + ", maxAge=" + cookie.getMaxAge() );
        return token;
    }

    /**
     * 
     */
    public static final class Github
            extends OAuth {

        public static final String BASE_URI = "https://api.github.com/user";

        @Override
        protected API newAPI( String accessToken ) {
            return new API( accessToken ) {
                @Override
                public String username() throws IOException {
                    String response = issueRequest( BASE_URI, "application/json", new HashMap() {{
                        put( "access_token", accessToken );
                    }});
                    JSONObject json = new JSONObject( response );
                    //log.info( ">> " + json.toString( 2 ) );
                    return json.getString( "login" ) + "_github";
                }
            };
        }

    }
    
    /**
     * 
     */
    public static abstract class API {
        
        protected String accessToken;

        public API( String accessToken ) {
            this.accessToken = accessToken;
        }

        public abstract String username() throws IOException;

        protected String issueRequest( String baseUri, String accept, Map<String,String> params ) throws IOException {
            URL url = new URL( new StringBuilder( 256 )
                    .append( baseUri ).append( "?" )
                    .append( Joiner.on( '&' ).withKeyValueSeparator( "=" ).join( params ) )
                    .toString() );
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestProperty( "Accept", accept );

            try (InputStream in = http.getInputStream()) {
                return IOUtils.toString( in, "UTF-8" );
            }
        }
    }    
    
}
