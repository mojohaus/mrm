package org.codehaus.mojo.mrm.impl.maven;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileSystem;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArchetypeCatalogFileEntryTest {

    @Test
    void cleanArchetypeCatalogFileEntry() throws Exception {
        ArchetypeCatalogFileEntry entry = new ArchetypeCatalogFileEntry(null, null, null);
        assertThat(entry.getFileSystem()).isNull();
        assertThat(entry.getParent()).isNull();
        assertThat(entry.getName()).isEqualTo("archetype-catalog.xml");
    }

    @Test
    void fileSystem() throws Exception {
        FileSystem fileSystem = mock(FileSystem.class);
        DirectoryEntry root = mock(DirectoryEntry.class);
        when(fileSystem.getRoot()).thenReturn(root);
        ArchetypeCatalogFileEntry entry = new ArchetypeCatalogFileEntry(fileSystem, null, null);
        assertThat(entry.getFileSystem()).isEqualTo(fileSystem);
        assertThat(entry.getParent()).isNull();
        assertThat(entry.getName()).isEqualTo("archetype-catalog.xml");
        assertThat(entry.toPath()).isEqualTo("archetype-catalog.xml");
    }

    @Test
    void parent() throws Exception {
        FileSystem fileSystem = mock(FileSystem.class);
        DirectoryEntry parent = mock(DirectoryEntry.class);
        when(fileSystem.getRoot()).thenReturn(parent);
        ArchetypeCatalogFileEntry entry = new ArchetypeCatalogFileEntry(fileSystem, parent, null);
        assertThat(entry.getFileSystem()).isEqualTo(fileSystem);
        assertThat(entry.getParent()).isEqualTo(parent);
        assertThat(entry.getName()).isEqualTo("archetype-catalog.xml");
        assertThat(entry.toPath()).isEqualTo("archetype-catalog.xml");
    }

    @Test
    void artifactStore() throws Exception {
        final long lastModified = System.currentTimeMillis();
        FileSystem fileSystem = mock(FileSystem.class);
        DirectoryEntry parent = mock(DirectoryEntry.class);
        when(fileSystem.getRoot()).thenReturn(parent);
        ArtifactStore store = mock(ArtifactStore.class);
        when(store.getArchetypeCatalog()).thenReturn(new ArchetypeCatalog());
        when(store.getArchetypeCatalogLastModified()).thenReturn(lastModified);
        ArchetypeCatalogFileEntry entry = new ArchetypeCatalogFileEntry(fileSystem, parent, store);
        assertThat(entry.getFileSystem()).isEqualTo(fileSystem);
        assertThat(entry.getParent()).isEqualTo(parent);
        assertThat(entry.getName()).isEqualTo("archetype-catalog.xml");
        assertThat(entry.toPath()).isEqualTo("archetype-catalog.xml");
        assertThat(entry.getLastModified()).isEqualTo(lastModified);
        assertThat(entry.getSize()).isGreaterThan(0);
        assertThat(entry.getInputStream()).isNotNull();
    }
}
