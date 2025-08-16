package org.codehaus.mojo.mrm.plugin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.eclipse.aether.RepositorySystem;

/**
 * Our implementation of {@link FactoryHelper}.
 *
 * @since 1.0
 */
@Named
@Singleton
public class DefaultFactoryHelper implements FactoryHelper {
    private final RepositorySystem repositorySystem;

    private final ArchetypeManager archetypeManager;

    private final Provider<MavenSession> mavenSessionProvider;

    private final ArchiverManager archiverManager;

    @Inject
    public DefaultFactoryHelper(
            RepositorySystem repositorySystem,
            ArchetypeManager archetypeManager,
            Provider<MavenSession> mavenSessionProvider,
            ArchiverManager archiverManager) {
        this.repositorySystem = repositorySystem;
        this.archetypeManager = archetypeManager;
        this.mavenSessionProvider = mavenSessionProvider;
        this.archiverManager = archiverManager;
    }

    @Override
    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    @Override
    public ArchetypeManager getArchetypeManager() {
        return archetypeManager;
    }

    @Override
    public MavenSession getMavenSession() {
        return mavenSessionProvider.get();
    }

    @Override
    public ArchiverManager getArchiverManager() {
        return archiverManager;
    }
}
