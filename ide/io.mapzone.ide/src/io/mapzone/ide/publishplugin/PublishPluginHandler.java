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
package io.mapzone.ide.publishplugin;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class PublishPluginHandler
        extends AbstractHandler
        implements IHandler {

    @Override
    public Object execute( ExecutionEvent ev ) throws ExecutionException {
        PublishPluginWizard wizard = new PublishPluginWizard();
        WizardDialog dialog = new WizardDialog( Display.getCurrent().getActiveShell(), wizard );
        dialog.open();
        
//        // dialog
//        EditPluginDialog dialog = new EditPluginDialog( pluginEntity );
//        dialog.open();
        
        return null;
    }

}
