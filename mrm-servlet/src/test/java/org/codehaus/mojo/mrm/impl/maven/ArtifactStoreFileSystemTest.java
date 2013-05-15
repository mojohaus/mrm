/*
 * Copyright 2011 Stephen Connolly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.mojo.mrm.impl.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.regex.Matcher;

import org.codehaus.mojo.mrm.api.Entry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.maven.ArchetypeCatalogNotFoundException;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.junit.Test;

public class ArtifactStoreFileSystemTest
{

    @Test
    public void testGroupMetadataRegex()
        throws Exception
    {
        Matcher matcher = ArtifactStoreFileSystem.METADATA.matcher( "/commons/maven-metadata.xml" );
        assertTrue( matcher.matches() );
        assertEquals( "commons/", matcher.group( 1 ) );
        matcher = ArtifactStoreFileSystem.METADATA.matcher( "/org/apache/maven/maven-metadata.xml" );
        assertTrue( matcher.matches() );
        assertEquals( "org/apache/maven/", matcher.group( 1 ) );
        matcher = ArtifactStoreFileSystem.METADATA.matcher( "/commons/commons/1.0/commons-1.0.pom" );
        assertFalse( matcher.matches() );
        matcher = ArtifactStoreFileSystem.METADATA.matcher( "/org/apache/maven/pom/1.0/pom-1.0.pom" );
        assertFalse( matcher.matches() );
    }

    @Test
    public void testArtifactRegex()
        throws Exception
    {
        Matcher matcher = ArtifactStoreFileSystem.ARTIFACT.matcher( "/commons/maven-metadata.xml" );
        assertFalse( matcher.matches() );
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher( "/org/apache/maven/maven-metadata.xml" );
        assertFalse( matcher.matches() );
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher( "/commons/commons/1.0/commons-1.0.pom" );
        assertTrue( matcher.matches() );
        assertEquals( "commons/", matcher.group( 1 ) );
        assertEquals( "commons", matcher.group( 2 ) );
        assertEquals( "1.0", matcher.group( 3 ) );
        assertEquals( "commons-1.0.pom", matcher.group( 4 ) );
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher( "/org/apache/maven/pom/1.0/pom-1.0.pom" );
        assertTrue( matcher.matches() );
        assertEquals( "org/apache/maven/", matcher.group( 1 ) );
        assertEquals( "pom", matcher.group( 2 ) );
        assertEquals( "1.0", matcher.group( 3 ) );
        assertEquals( "pom-1.0.pom", matcher.group( 4 ) );
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher( "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-SNAPSHOT.pom" );
        assertTrue( matcher.matches() );
        assertEquals( "org/apache/maven/", matcher.group( 1 ) );
        assertEquals( "pom", matcher.group( 2 ) );
        assertEquals( "1.0-SNAPSHOT", matcher.group( 3 ) );
        assertEquals( "pom-1.0-SNAPSHOT.pom", matcher.group( 4 ) );
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher(
            "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-20110101.123456-56.pom" );
        assertFalse( matcher.matches() );
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher( "/commons/commons/1.0/commons-1.0-tests.jar" );
        assertTrue( matcher.matches() );
        assertEquals( "commons/", matcher.group( 1 ) );
        assertEquals( "commons", matcher.group( 2 ) );
        assertEquals( "1.0", matcher.group( 3 ) );
        assertEquals( "commons-1.0-tests.jar", matcher.group( 4 ) );
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher( "/org/apache/maven/pom/1.0/pom-1.0-tests.jar" );
        assertTrue( matcher.matches() );
        assertEquals( "org/apache/maven/", matcher.group( 1 ) );
        assertEquals( "pom", matcher.group( 2 ) );
        assertEquals( "1.0", matcher.group( 3 ) );
        assertEquals( "pom-1.0-tests.jar", matcher.group( 4 ) );
        matcher =
            ArtifactStoreFileSystem.ARTIFACT.matcher( "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-SNAPSHOT-tests.jar" );
        assertTrue( matcher.matches() );
        assertEquals( "org/apache/maven/", matcher.group( 1 ) );
        assertEquals( "pom", matcher.group( 2 ) );
        assertEquals( "1.0-SNAPSHOT", matcher.group( 3 ) );
        assertEquals( "pom-1.0-SNAPSHOT-tests.jar", matcher.group( 4 ) );
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher(
            "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-20110101.123456-56-tests.jar" );
        assertFalse( matcher.matches() );
    }

    @Test
    public void testSnapshotArtifactRegex()
        throws Exception
    {
        Matcher matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher( "/commons/maven-metadata.xml" );
        assertFalse( matcher.matches() );
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher( "/org/apache/maven/maven-metadata.xml" );
        assertFalse( matcher.matches() );
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher( "/commons/commons/1.0/commons-1.0.pom" );
        assertFalse( matcher.matches() );
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher( "/org/apache/maven/pom/1.0/pom-1.0.pom" );
        assertFalse( matcher.matches() );
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher(
            "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-SNAPSHOT.pom" );
        assertTrue( matcher.matches() );
        assertEquals( "org/apache/maven/", matcher.group( 1 ) );
        assertEquals( "pom", matcher.group( 2 ) );
        assertEquals( "1.0", matcher.group( 3 ) );
        assertEquals( "pom-1.0-SNAPSHOT.pom", matcher.group( 4 ) );
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher(
            "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-20110101.123456-56.pom" );
        assertTrue( matcher.matches() );
        assertEquals( "org/apache/maven/", matcher.group( 1 ) );
        assertEquals( "pom", matcher.group( 2 ) );
        assertEquals( "1.0", matcher.group( 3 ) );
        assertEquals( "pom-1.0-20110101.123456-56.pom", matcher.group( 4 ) );
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher( "/commons/commons/1.0/commons-1.0-tests.jar" );
        assertFalse( matcher.matches() );
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher( "/org/apache/maven/pom/1.0/pom-1.0-tests.jar" );
        assertFalse( matcher.matches() );
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher(
            "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-SNAPSHOT-tests.jar" );
        assertTrue( matcher.matches() );
        assertEquals( "org/apache/maven/", matcher.group( 1 ) );
        assertEquals( "pom", matcher.group( 2 ) );
        assertEquals( "1.0", matcher.group( 3 ) );
        assertEquals( "pom-1.0-SNAPSHOT-tests.jar", matcher.group( 4 ) );
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher(
            "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-20110101.123456-56-tests.jar" );
        assertTrue( matcher.matches() );
        assertEquals( "org/apache/maven/", matcher.group( 1 ) );
        assertEquals( "pom", matcher.group( 2 ) );
        assertEquals( "1.0", matcher.group( 3 ) );
        assertEquals( "pom-1.0-20110101.123456-56-tests.jar", matcher.group( 4 ) );
    }
    
    
    // MMOCKRM-5
    @Test
    public void testSiteXmlReleaseVersion() throws Exception
    {
        ArtifactStore store = mock( ArtifactStore.class );
        when( store.get( isA( Artifact.class ) ) ).thenThrow( ArtifactNotFoundException.class );
        ArtifactStoreFileSystem system = new ArtifactStoreFileSystem( store );
        FileEntry entry = (FileEntry) system.get( "/localhost/mmockrm-5/1/mmockrm-5-1-site_en.xml" );
        assertNull( entry );
    }
    
    @Test
    public void testSiteXmlSnapshotVersion() throws Exception
    {
        ArtifactStore store = mock( ArtifactStore.class );
        when( store.get( isA( Artifact.class ) ) ).thenThrow( ArtifactNotFoundException.class );
        ArtifactStoreFileSystem system = new ArtifactStoreFileSystem( store );
        FileEntry entry = (FileEntry) system.get( "/localhost/mmockrm-5/1.0-SNAPSHOT/mmockrm-5-1.0-SNAPSHOT-site_en.xml" );
        assertNull( entry );
    }
    
    public void testArchetypeCatalog() throws Exception
    {
        ArtifactStore store = mock( ArtifactStore.class );
        when( store.getArchetypeCatalog() ).thenThrow( ArchetypeCatalogNotFoundException.class );
        ArtifactStoreFileSystem system = new ArtifactStoreFileSystem( store );
        Entry entry = system.get( "/archetype-catalog.xml" );
        assertNull( entry );
    }

}
