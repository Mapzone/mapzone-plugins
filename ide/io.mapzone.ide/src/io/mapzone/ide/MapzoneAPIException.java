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
package io.mapzone.ide;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class MapzoneAPIException
        extends RuntimeException {

    protected MapzoneAPIException() {
        super();
    }

    protected MapzoneAPIException( String message, Throwable cause ) {
        super( message, cause );
    }

    protected MapzoneAPIException( String message ) {
        super( message );
    }

    protected MapzoneAPIException( Throwable cause ) {
        super( cause );
    }

}
