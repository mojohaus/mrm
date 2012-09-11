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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Map;

/**
 * This goal is used in-situ on a Maven project to allow integration tests based on the Maven Invoker to use a custom
 * <code>settings.xml</code> and still work behind a proxy.
 *
 * @author Stephen Connolly
 * @description Stops the mock repository manager started by <code>mrm:start</code> as part of a maven build for use
 * by integration tests.
 */
@Mojo( name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, requiresProject = false, threadSafe = true )
public class StopMojo
    extends AbstractMRMMojo
{
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings( "rawtypes" )
    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        Map pluginContext = session.getPluginContext( pluginDescriptor, project );
        FileSystemServer mrm = (FileSystemServer) pluginContext.get( FileSystemServer.class.getName() );
        if ( mrm == null )
        {
            getLog().info( "Mock Repository Manager was not started" );
            return;
        }
        String url = mrm.getUrl();
        getLog().info( "Stopping Mock Repository Manager on " + url );
        mrm.finish();
        try
        {
            mrm.waitForFinished();
            getLog().info( "Mock Repository Manager " + url + " is stopped." );
            pluginContext.remove( FileSystemServer.class.getName() );
        }
        catch ( InterruptedException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
