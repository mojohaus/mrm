package org.codehaus.mojo.mrm.impl.maven;

import java.io.File;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DiskArtifactStoreTest {

    // MMOCKRM-10
    @Test
    void testArchetypeCatalog() throws Exception {
        DiskArtifactStore artifactStore = new DiskArtifactStore(new File("src/test/resources/mmockrm-10"));
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
}
