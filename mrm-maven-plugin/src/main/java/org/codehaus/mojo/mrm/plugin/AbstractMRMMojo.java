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

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.mrm.maven.ProxyArtifactStore;

/**
 * Base class for all the Mock Repository Manager's Mojos.
 *
 * @since 1.0
 */
public abstract class AbstractMRMMojo extends AbstractMojo {
    protected final ArtifactStoreFactory proxyRepo;

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The remote repositories.
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    protected List<ArtifactRepository> remoteArtifactRepositories;

    /**
     * The remote pluginRepositories.
     */
    @Parameter(defaultValue = "${project.pluginArtifactRepositories}", readonly = true)
    protected List<ArtifactRepository> remotePluginRepositories;

    /**
     * The local repository.
     */
    @Parameter(defaultValue = "${localRepository}", readonly = true)
    protected ArtifactRepository localRepository;

    /**
     * The Maven Session Object
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    protected MavenSession session;

    /**
     * This plugins descriptor.
     */
    @Parameter(defaultValue = "${plugin}", required = true, readonly = true)
    protected PluginDescriptor pluginDescriptor;

    /**
     * This mojo's execution.
     */
    @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
    protected MojoExecution mojoExecution;

    /**
     * If true, execution of the plugin is skipped.
     */
    @Parameter(property = "mrm.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * Creates a new instance
     * @param proxyRepo injected proxyRepo
     */
    public AbstractMRMMojo(ArtifactStoreFactory proxyRepo) {
        this.proxyRepo = proxyRepo;
    }

    /**
     * Executes the plugin goal (if the plugin is not skipped)
     *
     * @throws MojoExecutionException If there is an exception occuring during the execution of the plugin.
     * @throws MojoFailureException If there is an exception occuring during the execution of the plugin.
     */
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping invocation per configuration."
                    + " If this is incorrect, ensure the skip parameter is not set to true.");
            return;
        }
        if (pluginDescriptor == null) {
            pluginDescriptor = mojoExecution.getMojoDescriptor().getPluginDescriptor();
        }
        doExecute();
    }

    protected MojoExecution getMojoExecution() {
        return mojoExecution;
    }

    /**
     * Performs this plugin's action.
     *
     * @throws MojoExecutionException If there is an exception occuring during the execution of the plugin.
     * @throws MojoFailureException If there is an exception occuring during the execution of the plugin.
     */
    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    /**
     * Creates an {@link org.codehaus.mojo.mrm.api.maven.ArtifactStore} that fetches from the repositories available to
     * Maven itself.
     *
     * @return an {@link org.codehaus.mojo.mrm.api.maven.ArtifactStore} that fetches from the repositories available to
     *         Maven itself.
     */
    protected ProxyArtifactStore createProxyArtifactStore() {
        return (ProxyArtifactStore) proxyRepo.newInstance(session, getLog());
    }
}
