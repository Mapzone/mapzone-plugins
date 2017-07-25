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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Defines the basic interface of processing modules of this tutorial.
 * <p/>
 * All public <b>members</b> are treated as module <b>fields</b>. If the name of the
 * member starts with "in" or ends with "input" then it is a input field. Otherwise
 * it is an output field.
 * 
 * @author Falko Bräutigam
 */
public abstract class TutorialModuleBase {

    public abstract void execute( IProgressMonitor monitor ) throws OperationCanceledException, Exception;
    
}
