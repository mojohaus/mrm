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

import java.util.List;

import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.logging.Log;

/**
 * Helper interface that exposes the Maven components that may be required.
 *
 * @see FactoryHelperRequired
 * @since 1.0
 */
public interface FactoryHelper {
    /**
     * Returns the {@link RepositoryMetadataManager} provided by Maven.
     *
     * @return The {@link RepositoryMetadataManager} provided by Maven.
     * @since 1.0
     */
    RepositoryMetadataManager getRepositoryMetadataManager();

    /**
     * Returns the remote plugin repositories provided by Maven.
     *
     * @return The remote plugin repositories provided by Maven.
     * @since 1.0
     */
    List<ArtifactRepository> getRemotePluginRepositories();

    /**
     * Returns the {@link ArtifactRepository} provided by Maven.
     *
     * @return The {@link ArtifactRepository} provided by Maven.
     * @since 1.0
     */
    ArtifactRepository getLocalRepository();

    /**
     * Returns the {@link ArtifactFactory} provided by Maven.
     *
     * @return The {@link ArtifactFactory} provided by Maven.
     * @since 1.0
     */
    ArtifactFactory getArtifactFactory();

    /**
     * Returns the remote repositories that we will query.
     *
     * @return The remote repositories that we will query.
     * @since 1.0
     */
    List<ArtifactRepository> getRemoteArtifactRepositories();

    /**
     * Returns the {@link ArtifactResolver} provided by Maven.
     *
     * @return The {@link ArtifactResolver} provided by Maven.
     * @since 1.0
     */
    ArtifactResolver getArtifactResolver();

    /**
     * Returns the {@link Log} to log to.
     *
     * @return The {@link Log} to log to.
     * @since 1.0
     */
    Log getLog();

    /**
     * Returns the {@link ArchetypeManager}
     *
     * @return The {@link ArchetypeManager}
     * @since 1.0
     */
    ArchetypeManager getArchetypeManager();
}
