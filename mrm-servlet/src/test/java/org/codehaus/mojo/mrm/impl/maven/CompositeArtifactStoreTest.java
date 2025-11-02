package org.codehaus.mojo.mrm.impl.maven;

import java.util.Collections;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeArtifactStoreTest {

    @Test
    void getArchetypeCatalog() throws Exception {
        ArtifactStore store = mock(ArtifactStore.class);
        when(store.getArchetypeCatalog()).thenReturn(new ArchetypeCatalog());
        ArtifactStore[] stores = new ArtifactStore[] {store};

        CompositeArtifactStore artifactStore = new CompositeArtifactStore(stores);
        ArchetypeCatalog catalog = artifactStore.getArchetypeCatalog();

        assertNotNull(catalog);
    }

    @Test
    void metadataForSnapshotShouldBeMerged() throws Exception {

        Metadata metadata1 = new Metadata();
        metadata1.setArtifactId("artifactId");
        metadata1.setGroupId("groupId");
        metadata1.setVersion("1.0.0-SNAPSHOT");
        metadata1.setVersioning(aVersioning("20231008", "175511", 1));

        Metadata metadata2 = new Metadata();
        metadata2.setArtifactId("artifactId");
        metadata2.setGroupId("groupId");
        metadata2.setVersion("1.0.0-SNAPSHOT");
        metadata2.setVersioning(aVersioning("20231008", "235511", 9999));

        ArtifactStore store1 = mock(ArtifactStore.class);
        ArtifactStore store2 = mock(ArtifactStore.class);

        when(store1.getMetadata(anyString())).thenReturn(metadata1);
        when(store2.getMetadata(anyString())).thenReturn(metadata2);
        ArtifactStore[] stores = new ArtifactStore[] {store1, store2};

        CompositeArtifactStore artifactStore = new CompositeArtifactStore(stores);

        Metadata metadata = artifactStore.getMetadata("path");

        assertNotNull(metadata);
    }

    private Versioning aVersioning(String timeStampDate, String timeStampTime, int buildNr) {

        Versioning versioning = new Versioning();
        versioning.setLastUpdated(timeStampDate + timeStampTime);

        Snapshot snapshot = new Snapshot();
        snapshot.setTimestamp(timeStampDate + "." + timeStampTime);
        snapshot.setBuildNumber(buildNr);
        versioning.setSnapshot(snapshot);

        SnapshotVersion snapshotVersion = new SnapshotVersion();
        snapshotVersion.setVersion("1.0.0-" + timeStampDate + "." + timeStampTime + "-" + buildNr);
        snapshotVersion.setExtension("jar");
        snapshotVersion.setClassifier("test");
        snapshotVersion.setUpdated(timeStampDate + timeStampTime);

        versioning.setSnapshotVersions(Collections.singletonList(snapshotVersion));
        return versioning;
    }
}
