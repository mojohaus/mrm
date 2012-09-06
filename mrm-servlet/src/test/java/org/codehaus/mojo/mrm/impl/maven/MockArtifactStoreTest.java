package org.codehaus.mojo.mrm.impl.maven;

import static org.junit.Assert.assertEquals;

import java.io.File;

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
}
