package org.codehaus.mojo.mrm.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractEntryTest {

    // MMOCKRM-13
    @Test
    void pathForRootEntry() {
        FileSystem fileSystem = mock(FileSystem.class);
        DefaultDirectoryEntry entry = new DefaultDirectoryEntry(fileSystem, null, "/favicon.ico");

        when(fileSystem.getRoot()).thenReturn(entry);

        assertEquals("/favicon.ico", entry.toPath());
    }
}
