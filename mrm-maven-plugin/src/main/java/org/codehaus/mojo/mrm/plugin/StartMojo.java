package org.codehaus.mojo.mrm.plugin;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Map;

/**
 * This goal is used in-situ on a Maven project to allow integration tests based on the Maven Invoker to use a custom
 * <code>settings.xml</code> and still work behind a proxy.
 *
 * @author Stephen Connolly
 *
 * Starts a mock repository manager as part of a maven build for use by integration tests.
 */
@Mojo( name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresProject = false, threadSafe = true )
public class StartMojo
    extends AbstractStartMojo
{
    /**
     * The property to set the repository url to.
     */
    @Parameter( property = "mrm.propertyName", defaultValue = "mrm.repository.url" )
    private String propertyName;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        FileSystemServer mrm = createFileSystemServer( createArtifactStore() );
        getLog().info( "Starting Mock Repository Manager" );
        mrm.ensureStarted();
        String url = mrm.getUrl();
        getLog().info( "Mock Repository Manager " + url + " is started." );
        if ( !StringUtils.isEmpty( propertyName ) )
        {
            getLog().info( "Setting property '" + propertyName + "' to '" + url + "'." );
            project.getProperties().setProperty( propertyName, url );
        }
        Map pluginContext = session.getPluginContext( pluginDescriptor, project );
        pluginContext.put( getFileSystemServerKey( getMojoExecution() ), mrm );
    }
    
    protected static String getFileSystemServerKey( MojoExecution mojoExecution )
    {
        return FileSystemServer.class.getName() + "@" + mojoExecution.getExecutionId(); 
    }

}
