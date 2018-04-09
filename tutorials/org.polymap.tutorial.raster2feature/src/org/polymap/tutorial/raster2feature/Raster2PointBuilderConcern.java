/* 
 * polymap.org
 * Copyright (C) 2018, the @authors. All rights reserved.
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
package org.polymap.tutorial.raster2feature;

import java.util.List;
import java.util.function.Supplier;

import org.geotools.styling.Style;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;

import org.polymap.core.data.feature.DefaultStyles;
import org.polymap.core.data.feature.FeatureRenderProcessor2;
import org.polymap.core.data.pipeline.DataSourceDescriptor;
import org.polymap.core.data.pipeline.Param.ParamsHolder;
import org.polymap.core.data.pipeline.PipelineBuilder;
import org.polymap.core.data.pipeline.PipelineBuilderConcernAdapter;
import org.polymap.core.data.pipeline.PipelineProcessor;
import org.polymap.core.data.pipeline.ProcessorDescriptor;
import org.polymap.core.data.pipeline.TerminalPipelineProcessor;
import org.polymap.core.project.ILayer;

import org.polymap.p4.P4Plugin;
import org.polymap.p4.project.ProjectRepository;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class Raster2PointBuilderConcern
        extends PipelineBuilderConcernAdapter {

    private static final Log log = LogFactory.getLog( Raster2PointBuilderConcern.class );
    
    public static final List<String>            LAYER_LABEL_TRIGGER = Lists.newArrayList( "point", "feature", "generated" );
    
    private DataSourceDescriptor                dsd;

    private Class<? extends PipelineProcessor>  usecase;

    private String                              layerId;

    @Override
    public void preBuild( PipelineBuilder builder, 
            List<Class<? extends TerminalPipelineProcessor>> terminals,
            List<Class<? extends PipelineProcessor>> transformers ) {
        log.info( "Terminals: " + terminals );
        log.info( "Transforms: " + transformers );
    }

    
    @Override
    @SuppressWarnings("hiding")
    public void startBuild( PipelineBuilder builder, String layerId, DataSourceDescriptor dsd,
            Class<? extends PipelineProcessor> usecase, ProcessorDescriptor start ) {
        this.dsd = dsd;
        this.usecase = usecase;
        this.layerId = layerId;
    }


    @Override
    public void terminals( PipelineBuilder builder, List<ProcessorDescriptor<TerminalPipelineProcessor>> terms ) {
        ILayer layer = ProjectRepository.unitOfWork().entity( ILayer.class, layerId );
        if (LAYER_LABEL_TRIGGER.stream().anyMatch( s -> layer.label.get().toLowerCase().contains( s ) )) {
            terms.clear();

            // feature style
            Supplier<Style> styleSupplier = () -> {
                String styleId = layer.styleIdentifier.get();
                return styleId != null
                        ? P4Plugin.styleRepo().serializedFeatureStyle( styleId, Style.class )
                                .orElse( DefaultStyles.createAllStyle() )
                                : DefaultStyles.createAllStyle();
            };
            FeatureRenderProcessor2.STYLE_SUPPLIER.rawput( (ParamsHolder)builder, styleSupplier );
            terms.add( 0, new ProcessorDescriptor( FeatureRenderProcessor2.class, null ) );

            terms.add( 0, new ProcessorDescriptor( Raster2PointProcessor.class, null ) );
        }
    }

}
