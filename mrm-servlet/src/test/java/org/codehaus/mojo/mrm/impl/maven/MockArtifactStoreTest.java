package org.codehaus.mojo.mrm.impl.maven;

import java.io.File;

import junit.framework.TestCase;

public class MockArtifactStoreTest
    extends TestCase
{

    // MMOCKRM-3
    public void testInheritGavFromParent()
    {
        // don't fail
        new MockArtifactStore( new File( "src/test/resources/mmockrm-3" ) );
    }
}
