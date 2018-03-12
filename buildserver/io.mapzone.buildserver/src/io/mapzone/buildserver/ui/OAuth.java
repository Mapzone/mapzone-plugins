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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

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
     */
    public boolean isAuthenticated() {
        HttpServletRequest request = RWT.getRequest();
        Optional<Cookie> stateCookie = Arrays.stream( request.getCookies() )
                .filter( c -> c.getName().equals( COOKIE_NAME ) ).findFirst();

        if (stateCookie.isPresent()) {
            Optional<String> state = BatikApplication.instance().getInitRequestParameter( "state" );
            if (state.isPresent()) {
                if (state.get().equals( stateCookie.get().getValue() )) {
                    stateCookie.get().setValue( "" );
                    stateCookie.get().setMaxAge( 0 );
                    return true;
                }
            }
            BatikApplication.instance().getInitRequestParameter( "code" );
        }
        return false;
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
    public static class Github
            extends OAuth {

//        @Override
//        public Optional<Image> icon( String configName ) {
//            return Optional.of( BsPlugin.images().svgImage( "github-circle.svg", configName ) );
//        }
//
//        @Override
//        public String label() {
//            return "GitHub";
//        }
//
//        @Override
//        public String baseUrl() {
//            return "https://github.com/login/oauth/authorize";
//        }
    }
    
}
