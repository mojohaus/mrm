package org.codehaus.mojo.mrm.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class AbstractEntryTest
{

    // MMOCKRM-13
    @Test
    public void testPathForRootEntry()
    {
        FileSystem fileSystem = mock( FileSystem.class );
        DefaultDirectoryEntry entry = new DefaultDirectoryEntry( fileSystem, null, "/favicon.ico" );

        when( fileSystem.getRoot() ).thenReturn( entry );

        assertEquals( "/favicon.ico", entry.toPath() );
    }

}
