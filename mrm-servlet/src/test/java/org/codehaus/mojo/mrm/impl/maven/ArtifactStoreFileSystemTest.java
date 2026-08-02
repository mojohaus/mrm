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

import java.util.regex.Matcher;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.codehaus.mojo.mrm.api.Entry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.maven.ArchetypeCatalogNotFoundException;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArtifactStoreFileSystemTest {

    @Test
    void groupMetadataRegex() {
        Matcher matcher = ArtifactStoreFileSystem.METADATA.matcher("/commons/maven-metadata.xml");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("commons/");
        matcher = ArtifactStoreFileSystem.METADATA.matcher("/org/apache/maven/maven-metadata.xml");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("org/apache/maven/");
        matcher = ArtifactStoreFileSystem.METADATA.matcher("/commons/commons/1.0/commons-1.0.pom");
        assertThat(matcher.matches()).isFalse();
        matcher = ArtifactStoreFileSystem.METADATA.matcher("/org/apache/maven/pom/1.0/pom-1.0.pom");
        assertThat(matcher.matches()).isFalse();
    }

    @Test
    void artifactRegex() {
        Matcher matcher = ArtifactStoreFileSystem.ARTIFACT.matcher("/commons/maven-metadata.xml");
        assertThat(matcher.matches()).isFalse();
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher("/org/apache/maven/maven-metadata.xml");
        assertThat(matcher.matches()).isFalse();
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher("/commons/commons/1.0/commons-1.0.pom");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("commons/");
        assertThat(matcher.group(2)).isEqualTo("commons");
        assertThat(matcher.group(3)).isEqualTo("1.0");
        assertThat(matcher.group(4)).isEqualTo("commons-1.0.pom");
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher("/org/apache/maven/pom/1.0/pom-1.0.pom");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("org/apache/maven/");
        assertThat(matcher.group(2)).isEqualTo("pom");
        assertThat(matcher.group(3)).isEqualTo("1.0");
        assertThat(matcher.group(4)).isEqualTo("pom-1.0.pom");
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher("/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-SNAPSHOT.pom");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("org/apache/maven/");
        assertThat(matcher.group(2)).isEqualTo("pom");
        assertThat(matcher.group(3)).isEqualTo("1.0-SNAPSHOT");
        assertThat(matcher.group(4)).isEqualTo("pom-1.0-SNAPSHOT.pom");
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher(
                "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-20110101.123456-56.pom");
        assertThat(matcher.matches()).isFalse();
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher("/commons/commons/1.0/commons-1.0-tests.jar");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("commons/");
        assertThat(matcher.group(2)).isEqualTo("commons");
        assertThat(matcher.group(3)).isEqualTo("1.0");
        assertThat(matcher.group(4)).isEqualTo("commons-1.0-tests.jar");
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher("/org/apache/maven/pom/1.0/pom-1.0-tests.jar");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("org/apache/maven/");
        assertThat(matcher.group(2)).isEqualTo("pom");
        assertThat(matcher.group(3)).isEqualTo("1.0");
        assertThat(matcher.group(4)).isEqualTo("pom-1.0-tests.jar");
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher(
                "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-SNAPSHOT-tests.jar");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("org/apache/maven/");
        assertThat(matcher.group(2)).isEqualTo("pom");
        assertThat(matcher.group(3)).isEqualTo("1.0-SNAPSHOT");
        assertThat(matcher.group(4)).isEqualTo("pom-1.0-SNAPSHOT-tests.jar");
        matcher = ArtifactStoreFileSystem.ARTIFACT.matcher(
                "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-20110101.123456-56-tests.jar");
        assertThat(matcher.matches()).isFalse();
    }

    @Test
    void snapshotArtifactRegex() {
        Matcher matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher("/commons/maven-metadata.xml");
        assertThat(matcher.matches()).isFalse();
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher("/org/apache/maven/maven-metadata.xml");
        assertThat(matcher.matches()).isFalse();
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher("/commons/commons/1.0/commons-1.0.pom");
        assertThat(matcher.matches()).isFalse();
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher("/org/apache/maven/pom/1.0/pom-1.0.pom");
        assertThat(matcher.matches()).isFalse();
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher(
                "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-SNAPSHOT.pom");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("org/apache/maven/");
        assertThat(matcher.group(2)).isEqualTo("pom");
        assertThat(matcher.group(3)).isEqualTo("1.0");
        assertThat(matcher.group(4)).isEqualTo("pom-1.0-SNAPSHOT.pom");
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher(
                "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-20110101.123456-56.pom");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("org/apache/maven/");
        assertThat(matcher.group(2)).isEqualTo("pom");
        assertThat(matcher.group(3)).isEqualTo("1.0");
        assertThat(matcher.group(4)).isEqualTo("pom-1.0-20110101.123456-56.pom");
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher("/commons/commons/1.0/commons-1.0-tests.jar");
        assertThat(matcher.matches()).isFalse();
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher("/org/apache/maven/pom/1.0/pom-1.0-tests.jar");
        assertThat(matcher.matches()).isFalse();
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher(
                "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-SNAPSHOT-tests.jar");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("org/apache/maven/");
        assertThat(matcher.group(2)).isEqualTo("pom");
        assertThat(matcher.group(3)).isEqualTo("1.0");
        assertThat(matcher.group(4)).isEqualTo("pom-1.0-SNAPSHOT-tests.jar");
        matcher = ArtifactStoreFileSystem.SNAPSHOT_ARTIFACT.matcher(
                "/org/apache/maven/pom/1.0-SNAPSHOT/pom-1.0-20110101.123456-56-tests.jar");
        assertThat(matcher.matches()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("org/apache/maven/");
        assertThat(matcher.group(2)).isEqualTo("pom");
        assertThat(matcher.group(3)).isEqualTo("1.0");
        assertThat(matcher.group(4)).isEqualTo("pom-1.0-20110101.123456-56-tests.jar");
    }

    // MMOCKRM-5
    @Test
    void siteXmlReleaseVersion() throws Exception {
        ArtifactStore store = mock(ArtifactStore.class);
        when(store.getSize(isA(Artifact.class))).thenThrow(ArtifactNotFoundException.class);
        ArtifactStoreFileSystem system = new ArtifactStoreFileSystem(store);
        FileEntry entry = (FileEntry) system.get("/localhost/mmockrm-5/1/mmockrm-5-1-site_en.xml");
        assertThat(entry).isNull();
    }

    @Test
    void siteXmlSnapshotVersion() throws Exception {
        ArtifactStore store = mock(ArtifactStore.class);
        when(store.getSize(isA(Artifact.class))).thenThrow(ArtifactNotFoundException.class);
        ArtifactStoreFileSystem system = new ArtifactStoreFileSystem(store);
        FileEntry entry =
                (FileEntry) system.get("/localhost/mmockrm-5/1.0-SNAPSHOT/mmockrm-5-1.0-SNAPSHOT-site_en.xml");
        assertThat(entry).isNull();
    }

    @Test
    void archetypeCatalogNotFound() throws Exception {
        ArtifactStore store = mock(ArtifactStore.class);
        when(store.getArchetypeCatalogLastModified()).thenThrow(ArchetypeCatalogNotFoundException.class);
        ArtifactStoreFileSystem system = new ArtifactStoreFileSystem(store);
        Entry entry = system.get("/archetype-catalog.xml");
        assertThat(entry).isNull();
    }

    @Test
    void archetypeCatalog() throws Exception {
        ArtifactStore store = mock(ArtifactStore.class);
        when(store.getArchetypeCatalog()).thenReturn(new ArchetypeCatalog());
        ArtifactStoreFileSystem system = new ArtifactStoreFileSystem(store);
        FileEntry entry = (FileEntry) system.get("archetype-catalog.xml");
        assertThat(entry.getName()).isEqualTo("archetype-catalog.xml");
    }
}
