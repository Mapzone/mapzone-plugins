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
package org.polymap.tutorial.importer.simple;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import org.polymap.p4.data.importer.ContextIn;
import org.polymap.p4.data.importer.ImporterFactory;

/**
 * A minimal importer factory: checks for one, single File with particular extansion
 * and creates an importer for it.
 *
 * @author Falko Bräutigam
 */
public class SimpleImporterFactory
        implements ImporterFactory {

    @ContextIn
    private File                file;

    @Override
    public void createImporters( ImporterBuilder builder ) throws Exception {
        // check if upstream importers provide a File
        // and if it is suited for us
        if (file != null && !FilenameUtils.getExtension( file.getName() ).isEmpty()) {
            builder.newImporter( new SimpleImporter(), file );
        }
    }
    
}
