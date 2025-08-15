package org.codehaus.mojo.mrm.plugin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.execution.MavenSession;
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

    private ArchetypeManager archetypeManager;

    private final Provider<MavenSession> mavenSessionProvider;

    @Inject
    public DefaultFactoryHelper(
            RepositorySystem repositorySystem,
            ArchetypeManager archetypeManager,
            Provider<MavenSession> mavenSessionProvider) {
        this.repositorySystem = repositorySystem;
        this.archetypeManager = archetypeManager;
        this.mavenSessionProvider = mavenSessionProvider;
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
}
