package org.codehaus.mojo.mrm.impl.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
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
}
