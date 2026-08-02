package org.codehaus.mojo.mrm.impl.maven;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiskArtifactStoreTest extends AbstractTestSupport {

    // MMOCKRM-10
    @Test
    void archetypeCatalog() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/mmockrm-10"));
        ArchetypeCatalog catalog = artifactStore.getArchetypeCatalog();
        assertThat(catalog).isNotNull();
        assertThat(catalog.getArchetypes()).hasSize(1);
        Archetype archetype = catalog.getArchetypes().get(0);
        assertThat(archetype.getGroupId()).isEqualTo("archetypes");
        assertThat(archetype.getArtifactId()).isEqualTo("fileset");
        assertThat(archetype.getVersion()).isEqualTo("1.0");
        assertThat(archetype.getDescription()).isEqualTo("Fileset test archetype");
        assertThat(archetype.getRepository()).isEqualTo("file://${basedir}/target/test-classes/repositories/central");
    }

    @Test
    void sizeShouldBeGreaterThanZero() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        long size = artifactStore.getSize(new Artifact("org.group1", "artifact1", "1.0.0", "pom"));
        assertThat(size).isGreaterThan(0);
    }

    @Test
    void sizeShouldBeGreaterThanZeroForSnapshot() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        long size = artifactStore.getSize(new Artifact("org.group2", "artifact2", "1.0.0-SNAPSHOT", "pom"));
        assertThat(size).isGreaterThan(0);
    }

    @Test
    void sizeShouldBeGreaterThanZeroForSnapshotTimeStamped() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        long size = artifactStore.getSize(new Artifact(
                "org.group2", "artifact2", "1.0.0-SNAPSHOT", null, "pom", System.currentTimeMillis(), 9999));
        assertThat(size).isGreaterThan(0);
    }

    @Test
    void sha1Checksum() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        String sha1Checksum1 = artifactStore.getSha1Checksum(new Artifact("org.group1", "artifact1", "1.0.0", "pom"));
        assertThat(sha1Checksum1).isNotNull();

        String sha1Checksum2 = artifactStore.getSha1Checksum(new Artifact("org.group1", "artifact1", "2.0.0", "pom"));
        assertThat(sha1Checksum2).isEqualTo("unit-test-ca766ba229dd04820042c13d24ef9fc76ceb2914");
    }

    @Test
    void artifactNotFound() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        Artifact artifact = new Artifact("org.groupXXXX", "artifactXXX", "1.0.0", "pom");

        assertThatThrownBy(() -> artifactStore.get(artifact))
                .isExactlyInstanceOf(ArtifactNotFoundException.class)
                .hasMessage("Artifact{org.groupXXXX:artifactXXX:1.0.0:pom}");

        assertThatThrownBy(() -> artifactStore.getLastModified(artifact))
                .isExactlyInstanceOf(ArtifactNotFoundException.class)
                .hasMessage("Artifact{org.groupXXXX:artifactXXX:1.0.0:pom}");

        assertThatThrownBy(() -> artifactStore.getSize(artifact))
                .isExactlyInstanceOf(ArtifactNotFoundException.class)
                .hasMessage("Artifact{org.groupXXXX:artifactXXX:1.0.0:pom}");

        assertThatThrownBy(() -> artifactStore.getSha1Checksum(artifact))
                .isExactlyInstanceOf(ArtifactNotFoundException.class)
                .hasMessage("Artifact{org.groupXXXX:artifactXXX:1.0.0:pom}");
    }

    @Test
    void metaDataShouldNotExistForReleaseVersion() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        assertThatThrownBy(() -> artifactStore.getMetadata("org/group1/artifact1/1.0.0"))
                .isExactlyInstanceOf(MetadataNotFoundException.class);
    }

    @Test
    void metaDataShouldExistForSnapshotVersion() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        Metadata metadata = artifactStore.getMetadata("org/group2/artifact2/1.0.0-SNAPSHOT");

        assertThat(metadata).isNotNull();
        assertThat(metadata.getGroupId()).isNotNull();
        assertThat(metadata.getArtifactId()).isNotNull();
        assertThat(metadata.getVersion()).isNotNull();
        assertThat(metadata.getVersioning().getSnapshot().getTimestamp()).isNotNull();
        assertThat(metadata.getVersioning().getSnapshot().getBuildNumber()).isEqualTo(9999);
        assertThat(metadata.getVersioning().getSnapshotVersions()).hasSize(3);
    }

    @Test
    void metaDataShouldExistForSnapshotTimestampVersion() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        Metadata metadata = artifactStore.getMetadata("org/group2/artifact2/2.0.0-SNAPSHOT");

        assertThat(metadata).isNotNull();
        assertThat(metadata.getGroupId()).isNotNull();
        assertThat(metadata.getArtifactId()).isNotNull();
        assertThat(metadata.getVersion()).isNotNull();
        assertThat(metadata.getVersioning().getSnapshot().getTimestamp()).isNotNull();
        assertThat(metadata.getVersioning().getSnapshot().getBuildNumber()).isOne();
        assertThat(metadata.getVersioning().getSnapshotVersions()).hasSize(3);
    }

    @Test
    void metaDataNotFoundForSnapshotVersion() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        assertThatThrownBy(() -> artifactStore.getMetadata("org/group1/artifact1/9.9.9-SNAPSHOT"))
                .isExactlyInstanceOf(MetadataNotFoundException.class);
    }
}
