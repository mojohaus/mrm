package org.codehaus.mojo.mrm.impl.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.junit.Test;

public class MockArtifactStoreTest
{

    // MMOCKRM-3
    @Test
    public void testInheritGavFromParent()
    {
        // don't fail
        MockArtifactStore mockArtifactStore = new MockArtifactStore( new File( "src/test/resources/mmockrm-3" ) );
        assertEquals( 2, mockArtifactStore.getArtifactIds( "localhost" ).size() );
    }
    
    // MMOCKRM-6
    @Test
    public void testClassifiers() throws Exception
    {
        MockArtifactStore artifactStore = new MockArtifactStore( new File( "src/test/resources/mmockrm-7" ) );

        Artifact pomArtifact = new Artifact( "localhost", "mmockrm-7", "1.0", "pom" );
        assertNotNull( artifactStore.get( pomArtifact ) );
        assertTrue( "Content equals",  IOUtils.contentEquals( new FileInputStream( "src/test/resources/mmockrm-7/mmockrm-7-1.0.pom" ), artifactStore.get( pomArtifact ) ) );

        Artifact siteArtifact = new Artifact( "localhost", "mmockrm-7", "1.0", "site", "xml" );
        assertNotNull( artifactStore.get( siteArtifact ) );
        assertTrue( "Content equals",  IOUtils.contentEquals( new FileInputStream( "src/test/resources/mmockrm-7/mmockrm-7-1.0-site.xml" ), artifactStore.get( siteArtifact ) ) );
    }
    
    // MMOCKRM-10
    @Test
    public void testArchetypeCatalog() throws Exception
    {
        MockArtifactStore artifactStore = new MockArtifactStore( new File( "src/test/resources/mmockrm-10" ) );
        ArchetypeCatalog catalog = artifactStore.getArchetypeCatalog();
        assertNotNull( catalog );
        assertEquals( 1, catalog.getArchetypes().size() );
        Archetype archetype = catalog.getArchetypes().get( 0 );
        assertEquals( "archetypes",  archetype.getGroupId() );
        assertEquals( "fileset",  archetype.getArtifactId() );
        assertEquals( "1.0",  archetype.getVersion() );
        assertEquals( "Fileset test archetype",  archetype.getDescription() );
        assertEquals( "file://${basedir}/target/test-classes/repositories/central", archetype.getRepository() );
    }
}
