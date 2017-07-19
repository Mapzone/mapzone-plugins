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
package io.mapzone.ide.util;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Simple input form with {@link Layouter predefined layouts}. 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class InputForm {

    private Composite           container;
    
    private Layouter            layouter;
    

    public InputForm( Composite container ) {
        this.container = container;
        this.layouter = new InputFormVerticalLayouter( container );
    }
    
    
    public Control createLabel( String label, String value, int... styles ) {
        Text control = new Text( container, styleBits( styles ) );
        control.setText( value );
        control.setEditable( false );
        return layouter.addField( label, control );
    }
    
    
    /**
     * Creates a {@link Text} with style bit {@link SWT#BORDER}. 
     *
     * @param label
     * @param value The default value, or null.
     * @param styles The style of the control to create.
     * @return The newly created control.
     */
    public Text createText( String label, String value, int... styles ) {
        Text control = new Text( container, SWT.BORDER | styleBits( styles ) );
        if (value != null) {
            control.setText( value );
        }
        return layouter.addField( label, control );
    }

    
    protected int styleBits( int[] styles ) {
        return Arrays.stream( styles ).reduce( SWT.NONE, (result,style) -> result | style );
    }
    
    
    /**
     * 
     */
    public static abstract class Layouter {

        protected Composite         container;

        public Layouter( Composite container ) {
            this.container = container;
        }

        public abstract <C extends Control> C addField( String label, C control );
    }
    

    /**
     * 
     */
    public static class InputFormVerticalLayouter
            extends Layouter {

        protected Control       last;
        
        public InputFormVerticalLayouter( Composite container ) {
            super( container );
            container.setLayout( FormLayoutFactory.defaults().spacing( 1 ).create() );
        }

        @Override
        public <C extends Control> C addField( String label, C control ) {
            Label l = new Label( container, SWT.NONE );
            l.setText( label );
            
            FormDataFactory.on( control ).fill().top( l ).noBottom();
            FormDataFactory lfd = FormDataFactory.on( l ).fill().left( 0, 3 ).noBottom();
            if (last != null) {
                lfd.top( last, 5 );
            }
            last = control;
            return control;
        }
    }
    
}
