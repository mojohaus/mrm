/*
 * Copyright 2011 Stephen Connolly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.mojo.mrm.plugin;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.digest.AutoDigestFileSystem;
import org.codehaus.mojo.mrm.impl.maven.ArtifactStoreFileSystem;
import org.codehaus.mojo.mrm.impl.maven.CompositeArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.DiskArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.MockArtifactStore;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Common base class for the mojos that start a repository.
 */
public abstract class AbstractStartMojo
    extends AbstractMRMMojo
{
    /**
     * The port to serve the remote repository on.
     *
     * @parameter expression="${mrm.port}"
     */
    private int port;

    /**
     * The repositories to serve if none specified then a proxy of the invoking maven's repositories will be served.
     *
     * @parameter
     */
    private PlexusConfiguration repositories;

    /**
     * Creates a file system server from an artifact store.
     *
     * @param artifactStore the artifact store to serve.
     * @return the file system server.
     */
    protected FileSystemServer createFileSystemServer( ArtifactStore artifactStore )
    {
        return new FileSystemServer( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ),
                                     Math.max( 0, Math.min( port, 65535 ) ),
                                     new AutoDigestFileSystem( new ArtifactStoreFileSystem( artifactStore ) ) );
    }

    /**
     * Creates an artifact store from the {@link #repositories} configuration.
     *
     * @return an artifact store.
     * @throws MojoExecutionException if the configuration is invalid.
     */
    protected ArtifactStore createArtifactStore()
        throws MojoExecutionException
    {
        if ( repositories == null )
        {
            getLog().info( "Configuring Mock Repository Manager to proxy through this Maven instance" );
            return createProxyArtifactStore();
        }
        getLog().info( "Configuring Mock Repository Manager..." );
        List stores = new ArrayList();
        int count = repositories.getChildCount();
        for ( int i = 0; i < count; i++ )
        {
            PlexusConfiguration type = repositories.getChild( i );
            if ( "proxy".equals( type.getName() ) )
            {
                getLog().info( "  Proxy (through this Maven instance)" );
                stores.add( createProxyArtifactStore() );
            }
            else if ( "mock".equals( type.getName() ) )
            {
                String path;
                try
                {
                    path = type.getValue();
                }
                catch ( PlexusConfigurationException e )
                {
                    throw new MojoExecutionException( "You must specify the root of the mock repository content" );
                }
                File root = path.startsWith( "/" ) ? new File( path ) : new File( project.getBasedir(), path );
                getLog().info( "  Mock content (root: " + root + ")" );
                stores.add( new MockArtifactStore( getLog(), root ) );
            }
            else if ( "local".equals( type.getName() ) )
            {
                String path;
                try
                {
                    path = type.getValue();
                }
                catch ( PlexusConfigurationException e )
                {
                    throw new MojoExecutionException( "You must specify the root of the local repository content" );
                }
                File root = path.startsWith( "/" ) ? new File( path ) : new File( project.getBasedir(), path );
                getLog().info( "  Locally hosted (root: " + root + ")" );
                stores.add( new DiskArtifactStore( root ) );
            }
            else
            {
                throw new MojoExecutionException( "Unknown configuration element: repositories/" + type.getName() );
            }
        }
        ArtifactStore[] artifactStores = (ArtifactStore[]) stores.toArray( new ArtifactStore[stores.size()] );
        return artifactStores.length == 1 ? artifactStores[0] : new CompositeArtifactStore( artifactStores );
    }
}
