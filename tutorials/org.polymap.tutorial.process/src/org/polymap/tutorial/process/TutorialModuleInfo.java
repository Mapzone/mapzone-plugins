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
package org.polymap.tutorial.process;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.polymap.core.data.process.FieldInfo;
import org.polymap.core.data.process.ModuleInfo;

/**
 * Provides information about a processing module.
 * <p/>
 * In this tutorial the SPI of modules is very simple: {@link TutorialModuleBase}. This info
 * class gathers information just from the module class. For real world applications
 * that module may use annotations ot the like.
 *
 * @author Falko Bräutigam
 */
public class TutorialModuleInfo
        implements ModuleInfo {

    private Class<? extends TutorialModuleBase>       moduleType;
    
    
    protected TutorialModuleInfo( Class<? extends TutorialModuleBase> moduleType ) {
        this.moduleType = moduleType;
    }

    @Override
    public String name() {
        return moduleType.getName();
    }

    @Override
    public String label() {
        return moduleType.getSimpleName();
    }

    @Override
    public Optional<String> description() {
        return Optional.of( "A tutorial module" );
    }

    @Override
    public Class<?> type() {
        return moduleType;
    }

    @Override
    public List<FieldInfo> inputFields() {
        return Arrays.stream( moduleType.getDeclaredFields() )
                .filter( f -> !Modifier.isStatic( f.getModifiers() ) )
                .map( f -> new TutorialFieldInfo( f ) )
                .filter( f -> f.isInput() )
                .collect( Collectors.toList() );
    }

    @Override
    public List<FieldInfo> outputFields() {
        return Arrays.stream( moduleType.getDeclaredFields() )
                .filter( f -> !Modifier.isStatic( f.getModifiers() ) )
                .map( f -> new TutorialFieldInfo( f ) )
                .filter( f -> !f.isInput() )
                .collect( Collectors.toList() );
    }

    @Override
    public Object createModuleInstance() {
        try {
            return moduleType.newInstance();
        }
        catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void execute( Object module, IProgressMonitor monitor ) throws OperationCanceledException, Exception {
        ((TutorialModuleBase)module).execute( monitor );
    }


    /**
     * Gather information from the plan {@link Field} object which represents
     * a field of a {@link TutorialModuleBase processing module}.
     */
    class TutorialFieldInfo 
            implements FieldInfo {

        private Field           f;
        
        
        protected TutorialFieldInfo( Field f ) {
            this.f = f;
        }

        @Override
        public String name() {
            return f.getName();
        }

        @Override
        public String label() {
            return f.getName();
        }

        @Override
        public Optional<String> description() {
            return Optional.of( "Field: " + label() );
        }

        @Override
        public Class type() {
            return f.getType();
        }

        @Override
        public boolean isInput() {
            return f.getName().startsWith( "in" ) 
                    || f.getName().toLowerCase().endsWith( "input" );
        }

        @Override
        public Object getValue( Object module ) {
            try {
                return f.get( module );
            }
            catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException( e );
            }
        }

        @Override
        public FieldInfo setValue( Object module, Object value ) {
            try {
                f.set( module, value );
                return this;
            }
            catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException( e );
            }
        }
    }
    
}
