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
package io.mapzone.ide.apiclient;

import java.util.Date;

import net.sf.json.JSONObject;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class PluginEntity
        extends JsonEntity {

    public Property<String>         id = new Property( "id" );
    
    public Property<String>         title = new Property( "title" );

    public Property<String>         description = new Property( "description" );
    
    public Property<Date>           created = new Property( "created", Date.class );
    
    public Property<Date>           updated = new Property( "updated", Date.class );

    public Property<String>         vendor = new Property( "vendor" );;
    
    public Property<String>         vendorUrl = new Property( "vendorUrl" );
    
    public Property<String>         projectUrl = new Property( "projectUrl" );

    public Property<Boolean>        isReleased = new Property( "isReleased" );

    public Property<Float>          fee = new Property( "fee" );

    public PluginEntity( JSONObject json ) {
        super( json );
    }

}
