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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.IParameterValues;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class UpdateTargetParameterValues
        implements IParameterValues {

    public static final String  PARAMETER_NAME = "io.mapzone.ide.parameter.targetSource";

    /** Parameter value */
    public static final String  INSTANCE = "instance";

    /** Parameter value */
    public static final String  JENKINS = "jenkins";

    @Override
    public Map getParameterValues() {
        Map<String, String> result = new HashMap();
        result.put( "Instance", INSTANCE );
        result.put( "Jenkins", JENKINS );
        return result;
    }
}
