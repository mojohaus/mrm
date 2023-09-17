package org.codehaus.mojo.mrm.impl.maven;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeArtifactStoreTest {

    @Test
    void testGetArchetypeCatalog() throws Exception {
        ArtifactStore store = mock(ArtifactStore.class);
        when(store.getArchetypeCatalog()).thenReturn(new ArchetypeCatalog());
        ArtifactStore[] stores = new ArtifactStore[] {store};

        CompositeArtifactStore artifactStore = new CompositeArtifactStore(stores);
        ArchetypeCatalog catalog = artifactStore.getArchetypeCatalog();

        assertNotNull(catalog);
    }
}
