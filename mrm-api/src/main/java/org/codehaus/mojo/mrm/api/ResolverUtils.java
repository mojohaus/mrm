package org.codehaus.mojo.mrm.api;

import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;

import static java.util.Optional.ofNullable;

/**
 * Miscellaneous utilities for manipulating Resolver entities
 */
public class ResolverUtils {

    private ResolverUtils() {
        // utility class
    }

    /**
     * <p>Creates a new {@link org.eclipse.aether.artifact.Artifact} based on an {@link Artifact} object.</p>
     * <p>Future deprecation: This method will be replaced with the new Maven 4
     * {@code org.apache.maven.api.services.ArtifactFactory} once it becomes available.</p>
     *
     * @param mavenSession {@link MavenSession} instance, may not be {@code null}
     * @param artifact object to read the data from, may not be {@code null}
     * @return new {@link org.eclipse.aether.artifact.Artifact} instance
     */
    public static org.eclipse.aether.artifact.Artifact createArtifact(MavenSession mavenSession, Artifact artifact) {
        return createArtifact(mavenSession.getRepositorySession().getArtifactTypeRegistry(), artifact);
    }

    /**
     * Creates a new {@link org.eclipse.aether.artifact.Artifact} based on an {@link Artifact} object. A type that
     * the registry does not know keeps its name as the extension.
     *
     * @param artifactTypeRegistry registry of artifact types, may not be {@code null}
     * @param artifact object to read the data from, may not be {@code null}
     * @return new {@link org.eclipse.aether.artifact.Artifact} instance
     * @since 1.7.2
     */
    public static org.eclipse.aether.artifact.Artifact createArtifact(
            ArtifactTypeRegistry artifactTypeRegistry, Artifact artifact) {
        Optional<ArtifactType> artifactType = ofNullable(artifact.getType()).map(artifactTypeRegistry::get);
        return new org.eclipse.aether.artifact.DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                ofNullable(artifact.getClassifier())
                        .orElse(artifactType.map(ArtifactType::getClassifier).orElse(null)),
                artifactType.map(ArtifactType::getExtension).orElse(artifact.getType()),
                artifact.getTimestampVersion(),
                artifactType.orElse(null));
    }
}
