package org.codehaus.mojo.mrm.impl.maven;

import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResolverUtilsTest {

    @Test
    void unknownType() {
        Artifact mrmArtifact = new Artifact("groupId", "artifactId", "version", "classifier", "type");

        org.eclipse.aether.artifact.Artifact arArtifact = ResolverUtils.createArtifact(t -> null, mrmArtifact);

        assertThat(arArtifact.getGroupId()).isEqualTo("groupId");
        assertThat(arArtifact.getArtifactId()).isEqualTo("artifactId");
        assertThat(arArtifact.getVersion()).isEqualTo("version");
        assertThat(arArtifact.getClassifier()).isEqualTo("classifier");
        assertThat(arArtifact.getExtension()).isEqualTo("type");
    }

    /**
     * From org.apache.maven.repository.internal.MavenRepositorySystemUtils
     */
    @Test
    void mainType() {
        ArtifactType type = new DefaultArtifactType("maven-plugin", "jar", "", "java");
        Artifact mrmArtifact = new Artifact("groupId", "artifactId", "version", "maven-plugin");

        org.eclipse.aether.artifact.Artifact arArtifact = ResolverUtils.createArtifact(t -> type, mrmArtifact);

        assertThat(arArtifact.getGroupId()).isEqualTo("groupId");
        assertThat(arArtifact.getArtifactId()).isEqualTo("artifactId");
        assertThat(arArtifact.getVersion()).isEqualTo("version");
        assertThat(arArtifact.getClassifier()).isEmpty();
        assertThat(arArtifact.getExtension()).isEqualTo("jar");
    }

    /**
     * From org.apache.maven.repository.internal.MavenRepositorySystemUtils
     */
    @Test
    void classifiedType() {
        ArtifactType type = new DefaultArtifactType("java-source", "jar", "sources", "java", false, false);
        Artifact mrmArtifact = new Artifact("groupId", "artifactId", "version", "java-source");

        org.eclipse.aether.artifact.Artifact arArtifact = ResolverUtils.createArtifact(t -> type, mrmArtifact);

        assertThat(arArtifact.getGroupId()).isEqualTo("groupId");
        assertThat(arArtifact.getArtifactId()).isEqualTo("artifactId");
        assertThat(arArtifact.getVersion()).isEqualTo("version");
        assertThat(arArtifact.getClassifier()).isEqualTo("sources");
        assertThat(arArtifact.getExtension()).isEqualTo("jar");
    }
}
