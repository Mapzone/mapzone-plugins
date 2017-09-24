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

import static com.google.common.base.Suppliers.memoize;
import static org.apache.commons.lang3.ArrayUtils.contains;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Supplier;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import io.mapzone.ide.apiclient.MapzoneAPIClient;
import io.mapzone.ide.apiclient.MapzoneProject;

/**
 * 
 *
 * @author <a href="http://mapzone.io">Falko Bräutigam</a>
 */
public class MapzonePluginProject {

    public static final QualifiedName PROP_HOSTNAME = new QualifiedName( IdePlugin.ID, "hostname" );

    public static final QualifiedName PROP_PORT = new QualifiedName( IdePlugin.ID, "port" );

    public static final QualifiedName PROP_USERNAME = new QualifiedName( IdePlugin.ID, "username" );

    public static final QualifiedName PROP_PROJECTNAME = new QualifiedName( IdePlugin.ID, "projectname" );

    /** Caches projects and HTTP connections ({@link MapzoneAPIClient}). */
    private static Cache<String,MapzonePluginProject> cache = CacheBuilder.newBuilder().initialCapacity( 16 ).softValues().build();
    
    /**
     * 
     */
    public static MapzonePluginProject of( IProject project ) {
        try {
            return cache.get( project.getName(), () -> new MapzonePluginProject( project ) );
        }
        catch (ExecutionException e) {
            throw new RuntimeException( e );
        }
    }
    
    // instance *******************************************
    
    private IProject                project;
    
    private Supplier<ProjectScope>  projectScope = memoize( () -> new ProjectScope( project ) );
    
    private Supplier<IEclipsePreferences> projectNode = memoize( () -> projectScope.get().getNode( IdePlugin.ID ) );
    
    private MapzoneAPIClient        client;
    
    private MapzoneProject          clientProject;
    
    
    protected MapzonePluginProject( IProject project ) {
        assert project != null;
        this.project = project;
    }
    
    
    public IProject project() {
        return project;
    }

    
    /**
     * Connect to the server and create the internal {@link MapzoneAPIClient}.
     *
     * @param username
     * @param pwd
     * @return Possibly cached {@link MapzoneAPIClient} instance.
     * @throws RuntimeException If login failed.
     */
    public MapzoneAPIClient connectServer( String username, String pwd ) {
        if (client == null) {
            client = new MapzoneAPIClient( username, pwd );

            // update username after successfully connected
            try {
                if (!Objects.equals( username(), username )) {
                    projectNode.get().put( PROP_USERNAME.getLocalName(), username );
                    projectNode.get().flush();
                }
            }
            catch (BackingStoreException e) {
                IdePlugin.logException( e );
            }
        }
        return client;
    }

    
    /**
     * 
     *
     * @param username
     * @param pwd
     * @return Possibly cached {@link MapzoneProject} instance.
     * @throws RuntimeException If login failed.
     */
    public MapzoneProject connect( String username, String pwd ) {
        if (clientProject == null) {
            connectServer( username, pwd );
            
            clientProject = client.findProjects( username() )
                    .stream().filter( p -> p.name().equals( projectname() ) )
                    .findAny().orElseThrow( () -> new IllegalStateException( "Project not found on server: " + projectname() ) );
        }
        return clientProject;
    }

    
    public void addNature( IProgressMonitor monitor ) throws CoreException {
        IProjectDescription description = project.getDescription();
        assert !contains( description.getNatureIds(), MapzoneProjectNature.ID ) : "Nature is already there.";
        String[] newNatures = ArrayUtils.add( description.getNatureIds(), MapzoneProjectNature.ID );
        description.setNatureIds( newNatures );
        project.setDescription( description, monitor );    
    }
    
    
    @SuppressWarnings( "hiding" )
    public void connectTo( MapzoneProject clientProject ) throws CoreException, BackingStoreException {
        this.clientProject = clientProject;
        MapzoneAPIClient client = clientProject.client();
//        project.setPersistentProperty( PROP_HOSTNAME, client.hostname() );
//        project.setPersistentProperty( PROP_PORT, client.port().toString() );
//        project.setPersistentProperty( PROP_USERNAME, client.username() );
//        project.setPersistentProperty( PROP_PROJECTNAME, clientProject.name() );
        
        projectNode.get().put( PROP_HOSTNAME.getLocalName(), client.hostname() );
        projectNode.get().put( PROP_PORT.getLocalName(), client.port().toString() );
        projectNode.get().put( PROP_USERNAME.getLocalName(), client.username() );
        projectNode.get().put( PROP_PROJECTNAME.getLocalName(), clientProject.name() );
        projectNode.get().flush();
    }

    
    protected String property( QualifiedName name ) {
        return projectNode.get().get( name.getLocalName(), null );
    }

//    public String hostname() {
//        return property( PROP_HOSTNAME );
//    }
//    
//    public Integer port() {
//        String result = property( PROP_PORT );
//        return result != null ? Integer.valueOf( result ) : null;
//    }
    
    public String username() {
        return property( PROP_USERNAME );
    }
    
    public String projectname() {
        return property( PROP_PROJECTNAME );
    }
    
}
