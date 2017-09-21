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

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
abstract class APIObject {

    public MapzoneAPIClient     client;
    

    protected APIObject( MapzoneAPIClient client ) {
        this.client = client;
    }

    public MapzoneAPIClient client() {
        return client;
    }

    protected RuntimeException propagate( Throwable e ) {
        return MapzoneAPIClient.propagate( e );
    }

}
