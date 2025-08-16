package org.codehaus.mojo.mrm.impl.maven;

import javax.inject.Inject;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PlexusTest
class MockArtifactStoreTest extends AbstractTestSupport {

    @Inject
    private ArchiverManager archiverManager;

    @TempDir
    Path temporaryFolder;

    // MMOCKRM-3
    @Test
    void testInheritGavFromParent() throws Exception {
        // don't fail
        MockArtifactStore mockArtifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mmockrm-3"));
        assertEquals(2, mockArtifactStore.getArtifactIds("localhost").size());
    }

    // MMOCKRM-6
    @Test
    void testClassifiers() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mmockrm-7"));

        Artifact pomArtifact = new Artifact("localhost", "mmockrm-7", "1.0", "pom");
        assertNotNull(artifactStore.get(pomArtifact));
        assertTrue(IOUtils.contentEquals(
                Files.newInputStream(Paths.get("src/test/resources/mmockrm-7/mmockrm-7-1.0.pom")),
                artifactStore.get(pomArtifact)));

        Artifact siteArtifact = new Artifact("localhost", "mmockrm-7", "1.0", "site", "xml");
        assertNotNull(artifactStore.get(siteArtifact));
        assertTrue(IOUtils.contentEquals(
                Files.newInputStream(Paths.get("src/test/resources/mmockrm-7/mmockrm-7-1.0-site.xml")),
                artifactStore.get(siteArtifact)));
    }

    // MMOCKRM-10
    @Test
    void testArchetypeCatalog() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mmockrm-10"));
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

    @Test
    void testDirectoryContent() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mrm-15"));

        Artifact pomArtifact = new Artifact("localhost", "mrm-15", "1.0", "pom");
        assertNotNull(artifactStore.get(pomArtifact));
        assertTrue(IOUtils.contentEquals(
                Files.newInputStream(Paths.get("target/test-classes/mrm-15/mrm-15-1.0.pom")),
                artifactStore.get(pomArtifact)));

        Artifact mainArtifact = new Artifact("localhost", "mrm-15", "1.0", "jar");
        InputStream inputStreamJar = artifactStore.get(mainArtifact);
        assertNotNull(inputStreamJar);

        List<String> names = new ArrayList<>();

        File jarFile = Files.createTempFile(temporaryFolder, "test", ".jar").toFile();
        Files.copy(inputStreamJar, jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                names.add(entry.getName());
            }
            Manifest manifest = jar.getManifest();
            assertNotNull(manifest);
            assertEquals(2, manifest.getMainAttributes().size());
        }

        assertTrue(names.contains("README.txt"));
    }

    @Test
    void testEmptyJarContent() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/empty-jar"));

        Artifact pomArtifact = new Artifact("localhost", "mrm-empty-jar", "1.0", "pom");
        InputStream inputStreamPom = artifactStore.get(pomArtifact);
        assertNotNull(inputStreamPom);
        assertTrue(IOUtils.contentEquals(
                Files.newInputStream(Paths.get("target/test-classes/empty-jar/mrm-empty-jar-1.0.pom")),
                inputStreamPom));

        Artifact mainArtifact = new Artifact("localhost", "mrm-empty-jar", "1.0", "jar");
        InputStream inputStreamJar = artifactStore.get(mainArtifact);
        assertNotNull(inputStreamJar);

        File jarFile = Files.createTempFile(temporaryFolder, "test", ".jar").toFile();
        Files.copy(inputStreamJar, jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        List<String> names = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                names.add(entry.getName());
            }
            Manifest manifest = jar.getManifest();
            assertNotNull(manifest);
            assertEquals(3, manifest.getMainAttributes().size());
        }
        assertTrue(names.contains("META-INF/MANIFEST.MF"));
    }

    @Test
    void testEmptyPluginJarContent() throws Exception {
        MockArtifactStore artifactStore =
                new MockArtifactStore(archiverManager, getResourceAsFile("/empty-plugin-jar"));

        Artifact pomArtifact = new Artifact("localhost", "mrm-empty-plugin-jar", "1.0", "pom");
        InputStream inputStreamPom = artifactStore.get(pomArtifact);
        assertNotNull(inputStreamPom);
        assertTrue(IOUtils.contentEquals(
                Files.newInputStream(Paths.get("target/test-classes/empty-plugin-jar/mrm-empty-plugin-jar-1.0.pom")),
                inputStreamPom));

        Artifact mainArtifact = new Artifact("localhost", "mrm-empty-plugin-jar", "1.0", "jar");
        InputStream inputStreamJar = artifactStore.get(mainArtifact);
        assertNotNull(inputStreamJar);

        File jarFile = Files.createTempFile(temporaryFolder, "test", ".jar").toFile();
        Files.copy(inputStreamJar, jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        List<String> names = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                names.add(entry.getName());
            }
            Manifest manifest = jar.getManifest();
            assertNotNull(manifest);
            assertEquals(3, manifest.getMainAttributes().size());
        }
        assertTrue(names.contains("META-INF/MANIFEST.MF"));
        assertTrue(names.contains("META-INF/maven/plugin.xml"));
    }

    @Test
    void testDirectoryContentWithTgzArchiver() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/tgz-archiver"));
        assertNotNull(artifactStore);

        Artifact tgzArtifact = new Artifact("localhost", "tgz-archiver", "1.0", "bin", "tgz");
        InputStream inputStream = artifactStore.get(tgzArtifact);
        assertNotNull(inputStream);

        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new GZIPInputStream(inputStream))) {
            TarArchiveEntry nextEntry = tarIn.getNextEntry();
            assertNotNull(nextEntry);
            assertEquals("README.txt", nextEntry.getName());

            nextEntry = tarIn.getNextEntry();
            assertNull(nextEntry);
        }
    }

    @Test
    void testDirectoryContentWithUnknownArchiver() throws Exception {
        IllegalStateException exception = assertThrowsExactly(
                IllegalStateException.class,
                () -> new MockArtifactStore(archiverManager, getResourceAsFile("/unknown-archiver")));
        assertTrue(exception.getMessage().contains("Could not find archiver for directory"));
    }

    @Test
    void testDirectoryWithClassifierContent() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mrm-xx"));

        Artifact pomArtifact = new Artifact("localhost", "mrm-xx", "1.0", "pom");
        assertNotNull(artifactStore.get(pomArtifact));
        assertTrue(IOUtils.contentEquals(
                Files.newInputStream(Paths.get("target/test-classes/mrm-xx/mrm-xx-1.0.pom")),
                artifactStore.get(pomArtifact)));

        Artifact classifiedArtifact = new Artifact("localhost", "mrm-xx", "1.0", "javadoc-resources", "jar");
        assertNotNull(artifactStore.get(classifiedArtifact));
    }

    @Test
    void groupMetaDataShouldNotExistForNoPlugins() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/empty-jar"));

        assertThrowsExactly(MetadataNotFoundException.class, () -> artifactStore.getMetadata("localhost"));
    }

    @Test
    void groupMetaDataShouldExistPlugins() throws Exception {
        MockArtifactStore artifactStore =
                new MockArtifactStore(archiverManager, getResourceAsFile("/empty-plugin-jar"));

        Metadata metadata = artifactStore.getMetadata("localhost");

        assertNull(metadata.getArtifactId());
        assertNull(metadata.getGroupId());
        assertNull(metadata.getVersion());
        assertNull(metadata.getVersioning());
        assertEquals(2, metadata.getPlugins().size());

        assertTrue(
                metadata.getPlugins().stream()
                        .filter(p -> "mrm-empty-maven-plugin".equals(p.getArtifactId()))
                        .filter(p -> "mrm-empty".equals(p.getPrefix()))
                        .anyMatch(p -> "Test Plugin 1".equals(p.getName())),
                "Plugin 1 not found in metadata");

        assertTrue(
                metadata.getPlugins().stream()
                        .filter(p -> "mrm-empty-plugin-jar".equals(p.getArtifactId()))
                        .filter(p -> "mrm-empty-plugin-jar".equals(p.getPrefix()))
                        .anyMatch(p -> "Test Plugin 2".equals(p.getName())),
                "Plugin 2 not found in metadata");
    }

    @Test
    void artifactMetaDataShouldExist() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/empty-jar"));

        Metadata metadata = artifactStore.getMetadata("localhost/mrm-empty-jar");

        assertEquals("localhost", metadata.getGroupId());
        assertEquals("mrm-empty-jar", metadata.getArtifactId());
        assertNull(metadata.getVersion());
        assertTrue(metadata.getPlugins().isEmpty());
        assertEquals("1.0", metadata.getVersioning().getLatest());
        assertEquals("1.0", metadata.getVersioning().getRelease());
        assertNull(metadata.getVersioning().getSnapshot());
        assertNotNull(metadata.getVersioning().getLastUpdated());
        assertTrue(metadata.getVersioning().getSnapshotVersions().isEmpty());
        assertEquals(1, metadata.getVersioning().getVersions().size());
        assertEquals("1.0", metadata.getVersioning().getVersions().get(0));
    }

    @Test
    void artifactVersionMetaDataShouldNotExistForReleaseVersion() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/empty-jar"));

        assertThrowsExactly(
                MetadataNotFoundException.class, () -> artifactStore.getMetadata("localhost/mrm-empty-jar/1.0"));
    }

    @Test
    void artifactVersionMetaDataShouldExist() throws Exception {
        MockArtifactStore artifactStore =
                new MockArtifactStore(archiverManager, getResourceAsFile("/empty-jar-snapshot"));

        Metadata metadata = artifactStore.getMetadata("localhost/mrm-empty-jar/1.0-SNAPSHOT");

        assertEquals("localhost", metadata.getGroupId());
        assertEquals("mrm-empty-jar", metadata.getArtifactId());
        assertEquals("1.0-SNAPSHOT", metadata.getVersion());
        assertTrue(metadata.getPlugins().isEmpty());
        assertNull(metadata.getVersioning().getLatest());
        assertNull(metadata.getVersioning().getRelease());
        assertTrue(metadata.getVersioning().getVersions().isEmpty());
        assertNotNull(metadata.getVersioning().getLastUpdated());
        assertEquals(1, metadata.getVersioning().getSnapshot().getBuildNumber());
        // assertNotNull(metadata.getVersioning().getSnapshot().getTimestamp()); - TODO check and fix
        assertEquals(2, metadata.getVersioning().getSnapshotVersions().size());

        assertTrue(metadata.getVersioning().getSnapshotVersions().stream()
                .filter(v -> "".equals(v.getClassifier()))
                .filter(v -> "pom".equals(v.getExtension()))
                .filter(v -> !v.getUpdated().isEmpty())
                .anyMatch(v -> "1.0-SNAPSHOT".equals(v.getVersion())));

        assertTrue(metadata.getVersioning().getSnapshotVersions().stream()
                .filter(v -> "".equals(v.getClassifier()))
                .filter(v -> "jar".equals(v.getExtension()))
                .filter(v -> !v.getUpdated().isEmpty())
                .anyMatch(v -> "1.0-SNAPSHOT".equals(v.getVersion())));
    }
}
