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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.mrm.maven.ProxyArtifactStore;

import java.util.List;

public abstract class AbstractMRMMojo
    extends AbstractMojo
{
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @component
     * @required
     * @readonly
     */
    private RepositoryMetadataManager repositoryMetadataManager;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     */
    protected List remoteArtifactRepositories;

    /**
     * @parameter expression="${project.pluginArtifactRepositories}"
     * @readonly
     */
    protected List remotePluginRepositories;

    /**
     * @parameter expression="${localRepository}"
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * @component
     */
    protected ArtifactResolver artifactResolver;

    /**
     * The Maven Session Object
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * This plugins descriptor.
     *
     * @parameter expression="${plugin}"
     * @readonly
     */
    protected PluginDescriptor pluginDescriptor;

    /**
     * This mojo's execution.
     *
     * @parameter expression="${mojoExecution}"
     * @required
     * @readonly
     */
    protected MojoExecution mojoExecution;

    /**
     * If true, execution of the plugin is skipped.
     *
     * @parameter expression="${mrm.skip}" default-value="false"
     */
    protected boolean skip;

    public final void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            getLog().info( "Skipping invocation per configuration."
                               + " If this is incorrect, ensure the skip parameter is not set to true." );
            return;
        }
        if ( pluginDescriptor == null )
        {
            pluginDescriptor = mojoExecution.getMojoDescriptor().getPluginDescriptor();
        }
        doExecute();
    }

    protected abstract void doExecute()
        throws MojoExecutionException, MojoFailureException;

    protected ProxyArtifactStore createProxyArtifactStore()
        throws InvalidVersionSpecificationException
    {
        return new ProxyArtifactStore( repositoryMetadataManager, remoteArtifactRepositories, remotePluginRepositories,
                                       localRepository, artifactFactory, artifactResolver, getLog() );
    }

}
