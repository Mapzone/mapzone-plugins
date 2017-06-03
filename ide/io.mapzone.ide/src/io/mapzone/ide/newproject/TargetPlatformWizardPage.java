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
package io.mapzone.ide.newproject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.wizard.WizardPage;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class TargetPlatformWizardPage
        extends WizardPage {

    protected TargetPlatformWizardPage() {
        super( "platformPage" );
        setTitle( "Target platform" );
        setDescription( "Installing the target platform." );
    }

    @Override
    public void createControl( Composite parent ) {
        Composite container = new Composite( parent, SWT.NULL );
        setControl( container );
    }

}
