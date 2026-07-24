package org.codehaus.mojo.mrm.impl.maven;

import java.util.Optional;

import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactType;

import static java.util.Optional.ofNullable;

/**
 * Miscellaneous utilities for manipulating Resolver entities
 */
final class ResolverUtils {

    private ResolverUtils() {
        // utility class
    }

    /**
     * <p>Creates a new {@link org.eclipse.aether.artifact.Artifact} based on an {@link Artifact} object.</p>
     * <p>Future deprecation: This method will be replaced with the new Maven 4
     * {@code org.apache.maven.api.services.ArtifactFactory} once it becomes available.</p>
     *
     * @param repositorySystemSession {@link RepositorySystemSession} instance, may not be {@code null}
     * @param artifact object to read the data from, may not be {@code null}
     * @return new {@link org.eclipse.aether.artifact.Artifact} instance
     */
    static org.eclipse.aether.artifact.Artifact createArtifact(
            RepositorySystemSession repositorySystemSession, Artifact artifact) {
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getTimestampVersion();

        Optional<ArtifactType> artifactType =
                ofNullable(artifact.getType()).map(repositorySystemSession.getArtifactTypeRegistry()::get);
        return new org.eclipse.aether.artifact.DefaultArtifact(
                groupId,
                artifactId,
                ofNullable(artifact.getClassifier())
                        .orElse(artifactType.map(ArtifactType::getClassifier).orElse(null)),
                artifactType.map(ArtifactType::getExtension).orElse(null),
                version,
                artifactType.orElse(null));
    }
}
