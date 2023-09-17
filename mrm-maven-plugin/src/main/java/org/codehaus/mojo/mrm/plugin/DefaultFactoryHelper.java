package org.codehaus.mojo.mrm.plugin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.eclipse.aether.RepositorySystem;

/**
 * Our implementation of {@link FactoryHelper}.
 *
 * @since 1.0
 */
@Named
@Singleton
public class DefaultFactoryHelper implements FactoryHelper {
    private RepositorySystem repositorySystem;

    private RepositoryMetadataManager repositoryMetadataManager;

    private ArchetypeManager archetypeManager;

    @Inject
    public DefaultFactoryHelper(
            RepositorySystem repositorySystem,
            RepositoryMetadataManager repositoryMetadataManager,
            ArchetypeManager archetypeManager) {
        this.repositorySystem = repositorySystem;
        this.repositoryMetadataManager = repositoryMetadataManager;
        this.archetypeManager = archetypeManager;
    }

    @Override
    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    @Override
    public RepositoryMetadataManager getRepositoryMetadataManager() {
        return repositoryMetadataManager;
    }

    @Override
    public ArchetypeManager getArchetypeManager() {
        return archetypeManager;
    }
}
