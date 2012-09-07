package org.codehaus.mojo.mrm.impl.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

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
        
        assertNotNull( artifactStore.get( new Artifact( "localhost", "mmockrm-7", "1.0", "pom" ) ) );
        assertNotNull( artifactStore.get( new Artifact( "localhost", "mmockrm-7", "1.0", "site", "xml" ) ) );
    }
}
