package org.codehaus.mojo.mrm.impl.maven;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.FileSystem;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArchetypeCatalogFileEntryTest {

    @Test
    void cleanArchetypeCatalogFileEntry() throws Exception {
        ArchetypeCatalogFileEntry entry = new ArchetypeCatalogFileEntry(null, null, null);
        assertNull(entry.getFileSystem());
        assertNull(entry.getParent());
        assertEquals("archetype-catalog.xml", entry.getName());
    }

    @Test
    void fileSystem() throws Exception {
        FileSystem fileSystem = mock(FileSystem.class);
        DirectoryEntry root = mock(DirectoryEntry.class);
        when(fileSystem.getRoot()).thenReturn(root);
        ArchetypeCatalogFileEntry entry = new ArchetypeCatalogFileEntry(fileSystem, null, null);
        assertEquals(fileSystem, entry.getFileSystem());
        assertNull(entry.getParent());
        assertEquals("archetype-catalog.xml", entry.getName());
        assertEquals("archetype-catalog.xml", entry.toPath());
    }

    @Test
    void parent() throws Exception {
        FileSystem fileSystem = mock(FileSystem.class);
        DirectoryEntry parent = mock(DirectoryEntry.class);
        when(fileSystem.getRoot()).thenReturn(parent);
        ArchetypeCatalogFileEntry entry = new ArchetypeCatalogFileEntry(fileSystem, parent, null);
        assertEquals(fileSystem, entry.getFileSystem());
        assertEquals(parent, entry.getParent());
        assertEquals("archetype-catalog.xml", entry.getName());
        assertEquals("archetype-catalog.xml", entry.toPath());
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
        assertEquals(fileSystem, entry.getFileSystem());
        assertEquals(parent, entry.getParent());
        assertEquals("archetype-catalog.xml", entry.getName());
        assertEquals("archetype-catalog.xml", entry.toPath());
        assertEquals(entry.getLastModified(), lastModified);
        assertTrue(entry.getSize() > 0);
        assertNotNull(entry.getInputStream());
    }
}
