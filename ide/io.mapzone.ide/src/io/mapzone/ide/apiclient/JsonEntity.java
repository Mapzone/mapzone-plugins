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

import org.json.JSONObject;

import org.eclipse.core.databinding.observable.value.AbstractObservableValue;

/**
 * An entity with its data stored in an {@link JSONObject}. {@link Property} members
 * are observable values and can be connected to UI.
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public abstract class JsonEntity {

    private JSONObject          json;
    
    public JsonEntity( JSONObject json ) {
        this.json = json;
        
    }
    
    public String toJsonString( int indent ) {
        return json.toString( indent );
    }
    
    /**
     * A property of an {@link JsonEntity}.
     */
    public class Property<T> {
        
        private String      name;
        
        private Class<T>    type;

        /** Constructs a {@link String} property. */
        protected Property( String name ) {
            this( name, (Class<T>)String.class );
        }
        
        protected Property( String name, Class<T> type ) {
            this.name = name;
            this.type = type;
        }
        
        public String name() {
            return name;
        }
        
        public T get() {
            Object result = json.get( name );
            if (result.equals( JSONObject.NULL ) ) {
                return null;
            }
            else {
                return (T)result;
            }
        }

        public void set( T value ) {
            json.put( name, value );
        }

        public AbstractObservableValue<T> newObservable() {
            return new AbstractObservableValue<T>() {
                @Override
                protected T doGetValue() {
                    System.out.println( "GET: " + name() + ": " + get() );
                    return get();
                }
                @Override
                protected void doSetValue( T value ) {
                    System.out.println( "SET: " + name() + ": " + value );
                    set( value );
                }
                @Override
                public Object getValueType() {
                    return type;
                }
            };
        }
    }
    
}
