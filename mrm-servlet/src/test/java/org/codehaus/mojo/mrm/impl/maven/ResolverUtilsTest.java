package org.codehaus.mojo.mrm.impl.maven;

import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResolverUtilsTest {

    @Test
    void unknownType() {
        Artifact mrmArtifact = new Artifact("groupId", "artifactId", "version", "classifier", "type");

        org.eclipse.aether.artifact.Artifact arArtifact = ResolverUtils.createArtifact(t -> null, mrmArtifact);

        assertEquals("groupId", arArtifact.getGroupId());
        assertEquals("artifactId", arArtifact.getArtifactId());
        assertEquals("version", arArtifact.getVersion());
        assertEquals("classifier", arArtifact.getClassifier());
        assertEquals("type", arArtifact.getExtension());
    }

    /**
     * From org.apache.maven.repository.internal.MavenRepositorySystemUtils
     */
    @Test
    void mainType() {
        ArtifactType type = new DefaultArtifactType("maven-plugin", "jar", "", "java");
        Artifact mrmArtifact = new Artifact("groupId", "artifactId", "version", "maven-plugin");

        org.eclipse.aether.artifact.Artifact arArtifact = ResolverUtils.createArtifact(t -> type, mrmArtifact);

        assertEquals("groupId", arArtifact.getGroupId());
        assertEquals("artifactId", arArtifact.getArtifactId());
        assertEquals("version", arArtifact.getVersion());
        assertEquals("", arArtifact.getClassifier());
        assertEquals("jar", arArtifact.getExtension());
    }

    /**
     * From org.apache.maven.repository.internal.MavenRepositorySystemUtils
     */
    @Test
    void classifiedType() {
        ArtifactType type = new DefaultArtifactType("java-source", "jar", "sources", "java", false, false);
        Artifact mrmArtifact = new Artifact("groupId", "artifactId", "version", "java-source");

        org.eclipse.aether.artifact.Artifact arArtifact = ResolverUtils.createArtifact(t -> type, mrmArtifact);

        assertEquals("groupId", arArtifact.getGroupId());
        assertEquals("artifactId", arArtifact.getArtifactId());
        assertEquals("version", arArtifact.getVersion());
        assertEquals("sources", arArtifact.getClassifier());
        assertEquals("jar", arArtifact.getExtension());
    }
}
