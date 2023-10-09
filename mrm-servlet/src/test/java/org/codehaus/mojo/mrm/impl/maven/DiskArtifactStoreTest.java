package org.codehaus.mojo.mrm.impl.maven;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiskArtifactStoreTest extends AbstractTestSupport {

    // MMOCKRM-10
    @Test
    void testArchetypeCatalog() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/mmockrm-10"));
        ArchetypeCatalog catalog = artifactStore.getArchetypeCatalog();
        assertNotNull(catalog);
        assertEquals(1, catalog.getArchetypes().size());
        Archetype archetype = catalog.getArchetypes().get(0);
        assertEquals("archetypes", archetype.getGroupId());
        assertEquals("fileset", archetype.getArtifactId());
        assertEquals("1.0", archetype.getVersion());
        assertEquals("Fileset test archetype", archetype.getDescription());
        assertEquals("file://${basedir}/target/test-classes/repositories/central", archetype.getRepository());
    }

    @Test
    void sizeShouldBeGreaterThanZero() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        long size = artifactStore.getSize(new Artifact("org.group1", "artifact1", "1.0.0", "pom"));
        assertTrue(size > 0);
    }

    @Test
    void sizeShouldBeGreaterThanZeroForSnapshot() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        long size = artifactStore.getSize(new Artifact("org.group2", "artifact2", "1.0.0-SNAPSHOT", "pom"));
        assertTrue(size > 0);
    }

    @Test
    void sizeShouldBeGreaterThanZeroForSnapshotTimeStamped() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        long size = artifactStore.getSize(
                new Artifact("org.group2", "artifact2", "1.0.0-SNAPSHOT", "pom", System.currentTimeMillis(), 9999));
        assertTrue(size > 0);
    }

    @Test
    void metaDataShouldNotExistForReleaseVersion() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        assertThrowsExactly(
                MetadataNotFoundException.class, () -> artifactStore.getMetadata("org/group1/artifact1/1.0.0"));
    }

    @Test
    void metaDataShouldExistForSnapshotVersion() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        Metadata metadata = artifactStore.getMetadata("org/group2/artifact2/1.0.0-SNAPSHOT");

        assertNotNull(metadata);
        assertNotNull(metadata.getGroupId());
        assertNotNull(metadata.getArtifactId());
        assertNotNull(metadata.getVersion());
        assertNotNull(metadata.getVersioning().getSnapshot().getTimestamp());
        assertEquals(9999, metadata.getVersioning().getSnapshot().getBuildNumber());
        assertEquals(3, metadata.getVersioning().getSnapshotVersions().size());
    }

    @Test
    void metaDataShouldExistForSnapshotTimestampVersion() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        Metadata metadata = artifactStore.getMetadata("org/group2/artifact2/2.0.0-SNAPSHOT");

        assertNotNull(metadata);
        assertNotNull(metadata.getGroupId());
        assertNotNull(metadata.getArtifactId());
        assertNotNull(metadata.getVersion());
        assertNotNull(metadata.getVersioning().getSnapshot().getTimestamp());
        assertEquals(1, metadata.getVersioning().getSnapshot().getBuildNumber());
        assertEquals(3, metadata.getVersioning().getSnapshotVersions().size());
    }

    @Test
    void metaDataNotFoundForSnapshotVersion() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(getResourceAsFile("/local-repo-unit"));

        assertThrowsExactly(
                MetadataNotFoundException.class,
                () -> artifactStore.getMetadata("org/group1/artifact1/9.9.9-SNAPSHOT"));
    }
}
