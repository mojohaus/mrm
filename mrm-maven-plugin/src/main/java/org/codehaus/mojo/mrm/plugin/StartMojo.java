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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.api.maven.ArtifactStoreFileSystem;
import org.codehaus.mojo.mrm.impl.digest.AutoDigestFileSystem;
import org.codehaus.mojo.mrm.impl.maven.CompositeArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.DiskArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.MockArtifactStore;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This goal is used in-situ on a Maven project to allow integration tests based on the Maven Invoker to use a custom
 * <code>settings.xml</code> and still work behind a proxy.
 *
 * @author Stephen Connolly
 * @goal start
 * @phase pre-integration-test
 * @requiresProject false
 * @description Starts a mock repository manager as part of a maven build for use by integration tests.
 */
public class StartMojo
    extends AbstractMRMMojo
{
    /**
     * Determines whether or not the server blocks when started. The default
     * behavior (daemon = true) will let the server start and continue running subsequent
     * processes in an automated build environment.
     * <p/>
     * Often, it is desirable to cause the server to pause other processes
     * while it continues to handle web requests. This is useful when starting the
     * server with the intent to work with it interactively. This can be facilitated by setting
     * daemon to false.
     *
     * @parameter expression="${mrm.daemon}" default-value="true"
     */
    private boolean daemon;

    /**
     * The property to set the repository url to.
     *
     * @parameter expression="${mrm.propertyName}"
     */
    private String propertyName;

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

    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        ArtifactStore artifactStore = null;
        try
        {
            if ( repositories == null )
            {
                getLog().info( "Configuring Mock Repository Manager to proxy through this Maven instance" );
                artifactStore = createProxyArtifactStore();
            }
            else
            {
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
                        String pathname = null;
                        try
                        {
                            pathname = type.getValue();
                        }
                        catch ( PlexusConfigurationException e )
                        {
                            throw new MojoExecutionException(
                                "You must specify the root of the mock repository content" );
                        }
                        File root = pathname.startsWith( "/" )
                            ? new File( pathname )
                            : new File( project.getBasedir(), pathname );
                        getLog().info( "  Mock content (root: " + root + ")" );
                        stores.add( new MockArtifactStore( getLog(), root ) );
                    }
                    else if ( "local".equals( type.getName() ) )
                    {
                        String pathname = null;
                        try
                        {
                            pathname = type.getValue();
                        }
                        catch ( PlexusConfigurationException e )
                        {
                            throw new MojoExecutionException(
                                "You must specify the root of the local repository content" );
                        }
                        File root = pathname.startsWith( "/" )
                            ? new File( pathname )
                            : new File( project.getBasedir(), pathname );
                        getLog().info( "  Locally hosted (root: " + root + ")" );
                        stores.add( new DiskArtifactStore( root ) );
                    }
                    else
                    {
                        throw new MojoExecutionException(
                            "Unknown configuration element: repositories/" + type.getName() );
                    }
                }
                ArtifactStore[] artifactStores = (ArtifactStore[]) stores.toArray( new ArtifactStore[stores.size()] );
                artifactStore =
                    artifactStores.length == 1 ? artifactStores[0] : new CompositeArtifactStore( artifactStores );
            }
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        MRMThread mrm = new MRMThread( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ),
                                       Math.max( 0, Math.min( port, 65535 ) ),
                                       new AutoDigestFileSystem( new ArtifactStoreFileSystem( artifactStore ) ) );
        getLog().info( "Starting Mock Repository Manager" );
        try
        {
            mrm.ensureStarted();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        String url = mrm.getUrl();
        getLog().info( "Mock Repository Manager " + url + " is started." );
        if ( !daemon )
        {
            ConsoleScanner consoleScanner = new ConsoleScanner();
            consoleScanner.start();
            getLog().info( "Hit ENTER on the console to stop the Mock Repository Manager and continue the build." );
            try
            {
                consoleScanner.waitForFinished();
            }
            catch ( InterruptedException e )
            {
                // ignore
            }
            finally
            {
                getLog().info( "Stopping Mock Repository Manager " + url );
                mrm.finish();
                try
                {
                    mrm.waitForFinished();
                    getLog().info( "Mock Repository Manager " + url + " is stopped." );
                }
                catch ( InterruptedException e )
                {
                    // ignore
                }
            }
        }
        else
        {
            if ( !StringUtils.isEmpty( propertyName ) )
            {
                getLog().info( "Setting property '" + propertyName + "' to '" + url + "'." );
                project.getProperties().setProperty( propertyName, url );
            }
            Map pluginContext = session.getPluginContext( pluginDescriptor, project );
            pluginContext.put( MRMThread.class.getName(), mrm );
        }
    }

}
