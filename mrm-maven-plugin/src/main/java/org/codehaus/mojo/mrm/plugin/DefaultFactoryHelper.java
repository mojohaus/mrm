package org.codehaus.mojo.mrm.plugin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.artifact.factory.ArtifactFactory;
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

    private ArtifactFactory artifactFactory;

    private ArchetypeManager archetypeManager;

    @Inject
    public DefaultFactoryHelper(
            RepositorySystem repositorySystem,
            RepositoryMetadataManager repositoryMetadataManager,
            ArtifactFactory artifactFactory,
            ArchetypeManager archetypeManager) {
        this.repositorySystem = repositorySystem;
        this.repositoryMetadataManager = repositoryMetadataManager;
        this.artifactFactory = artifactFactory;
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
    public ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    @Override
    public ArchetypeManager getArchetypeManager() {
        return archetypeManager;
    }
}
