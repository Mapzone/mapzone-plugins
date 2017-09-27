/* 
 * polymap.org
 * Copyright (C) 2015, the @autors. All rights reserved.
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
package org.polymap.tutorial.osm.importer.xml;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.polymap.p4.data.importer.ContextIn;
import org.polymap.p4.data.importer.ImporterFactory;

import com.google.common.collect.Sets;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class OsmXmlFileImporterFactory
        implements ImporterFactory {

    private static Log log = LogFactory.getLog( OsmXmlFileImporterFactory.class );
    
    public final static Set<String> supportedTypes = Sets.newHashSet(".org.polymap.tutorial.osm.importer", ".xml"); 
    
    @ContextIn
    protected File                  file;
    
    @ContextIn
    protected List<File>            files;
    

    @Override
    public void createImporters( ImporterBuilder builder ) throws Exception {
        if (isSupported( file )) {
            builder.newImporter( new OsmXmlFileImporter(), file );
        }
    }


    private boolean isSupported(File f) {
        if (f == null) {
            return false;
        }
        for (String type : supportedTypes) {
            if (f.getName().toLowerCase().endsWith( type )) {
                return true;
            }
        }
        return false;
    }
}
