/*
 * Copyright 2026 Robert Scholte
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
package org.codehaus.mojo.mrm.jupiter;

import java.util.List;

import org.apache.maven.archetype.ArchetypeManager;
import org.codehaus.mojo.mrm.plugin.FactoryHelper;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

class StandaloneFactoryHelper implements FactoryHelper {

    private final ArchiverManager archiverManager;

    private final RepositorySystem repositorySystem;

    private final RepositorySystemSession repositorySystemSession;

    private final List<RemoteRepository> remoteRepositories;

    public StandaloneFactoryHelper(
            RepositorySystem repositorySystem,
            RepositorySystemSession repositorySystemSession,
            List<RemoteRepository> remoteRepositories,
            ArchiverManager archiverManager) {
        this.repositorySystem = repositorySystem;
        this.repositorySystemSession = repositorySystemSession;
        this.remoteRepositories = remoteRepositories;
        this.archiverManager = archiverManager;
    }

    @Override
    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    @Override
    public RepositorySystemSession getRepositorySystemSession() {
        return repositorySystemSession;
    }

    @Override
    public List<RemoteRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    @Override
    public ArchetypeManager getArchetypeManager() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public ArchiverManager getArchiverManager() {
        return archiverManager;
    }
}
