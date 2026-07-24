package org.codehaus.mojo.mrm.plugin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

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
    public ArchiverManager getArchiverManager() {
        return archiverManager;
    }

    @Override
    public RepositorySystemSession getRepositorySystemSession() {
        return mavenSessionProvider.get().getRepositorySession();
    }

    @Override
    public List<RemoteRepository> getRemoteRepositories() {
        MavenProject currentProject = mavenSessionProvider.get().getCurrentProject();

        return Stream.concat(
                        currentProject.getRemoteProjectRepositories().stream(),
                        currentProject.getRemotePluginRepositories().stream())
                .distinct()
                .collect(Collectors.toList());
    }
}
