package org.codehaus.mojo.mrm.impl.maven;

import javax.inject.Inject;

import java.io.File;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.codehaus.mojo.mrm.impl.transform.metadata.MetadataTransformDirectiveFactory;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PlexusTest
class MockArtifactStoreTest extends AbstractTestSupport {

    @Inject
    private ArchiverManager archiverManager;

    @TempDir
    Path temporaryFolder;

    // MMOCKRM-3
    @Test
    void inheritGavFromParent() throws Exception {
        // don't fail
        MockArtifactStore mockArtifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mmockrm-3"));
        assertThat(mockArtifactStore.getArtifactIds("localhost")).hasSize(2);
    }

    // MMOCKRM-6
    @Test
    void classifiers() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mmockrm-7"));

        Artifact pomArtifact = new Artifact("localhost", "mmockrm-7", "1.0", "pom");
        assertThat(artifactStore.get(pomArtifact))
                .hasSameContentAs(Files.newInputStream(Path.of("src/test/resources/mmockrm-7/mmockrm-7-1.0.pom")));

        Artifact siteArtifact = new Artifact("localhost", "mmockrm-7", "1.0", "site", "xml");
        assertThat(artifactStore.get(siteArtifact))
                .hasSameContentAs(Files.newInputStream(Path.of("src/test/resources/mmockrm-7/mmockrm-7-1.0-site.xml")));
    }

    // MMOCKRM-10
    @Test
    void archetypeCatalog() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mmockrm-10"));
        ArchetypeCatalog catalog = artifactStore.getArchetypeCatalog();
        assertThat(catalog).isNotNull();
        assertThat(catalog.getArchetypes()).hasSize(1);
        Archetype archetype = catalog.getArchetypes().get(0);
        assertThat(archetype.getGroupId()).isEqualTo("archetypes");
        assertThat(archetype.getArtifactId()).isEqualTo("fileset");
        assertThat(archetype.getVersion()).isEqualTo("1.0");
        assertThat(archetype.getDescription()).isEqualTo("Fileset test archetype");
        assertThat(archetype.getRepository()).isEqualTo("file://${basedir}/target/test-classes/repositories/central");
    }

    @Test
    void directoryContent() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mrm-15"));

        Artifact pomArtifact = new Artifact("localhost", "mrm-15", "1.0", "pom");
        assertThat(artifactStore.get(pomArtifact))
                .hasSameContentAs(Files.newInputStream(Path.of("target/test-classes/mrm-15/mrm-15-1.0.pom")));

        Artifact mainArtifact = new Artifact("localhost", "mrm-15", "1.0", "jar");
        InputStream inputStreamJar = artifactStore.get(mainArtifact);
        assertThat(inputStreamJar).isNotNull();

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
            assertThat(manifest).isNotNull();
            assertThat(manifest.getMainAttributes()).hasSize(2);
        }

        assertThat(names).contains("README.txt");
    }

    @Test
    void emptyJarContent() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/empty-jar"));

        Artifact pomArtifact = new Artifact("localhost", "mrm-empty-jar", "1.0", "pom");
        assertThat(artifactStore.get(pomArtifact))
                .hasSameContentAs(Files.newInputStream(Path.of("target/test-classes/empty-jar/mrm-empty-jar-1.0.pom")));

        Artifact mainArtifact = new Artifact("localhost", "mrm-empty-jar", "1.0", "jar");
        InputStream inputStreamJar = artifactStore.get(mainArtifact);
        assertThat(inputStreamJar).isNotNull();

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
            assertThat(manifest).isNotNull();
            assertThat(manifest.getMainAttributes()).hasSize(3);
        }
        assertThat(names).contains("META-INF/MANIFEST.MF");
    }

    @Test
    void emptyPluginJarContent() throws Exception {
        MockArtifactStore artifactStore =
                new MockArtifactStore(archiverManager, getResourceAsFile("/empty-plugin-jar"));

        Artifact pomArtifact = new Artifact("localhost", "mrm-empty-plugin-jar", "1.0", "pom");
        assertThat(artifactStore.get(pomArtifact))
                .hasSameContentAs(Files.newInputStream(
                        Path.of("target/test-classes/empty-plugin-jar/mrm-empty-plugin-jar-1.0.pom")));

        Artifact mainArtifact = new Artifact("localhost", "mrm-empty-plugin-jar", "1.0", "jar");
        InputStream inputStreamJar = artifactStore.get(mainArtifact);
        assertThat(inputStreamJar).isNotNull();

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
            assertThat(manifest).isNotNull();
            assertThat(manifest.getMainAttributes()).hasSize(3);
        }
        assertThat(names).contains("META-INF/MANIFEST.MF").contains("META-INF/maven/plugin.xml");
    }

    @Test
    void directoryContentWithTgzArchiver() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/tgz-archiver"));
        assertThat(artifactStore).isNotNull();

        Artifact tgzArtifact = new Artifact("localhost", "tgz-archiver", "1.0", "bin", "tgz");
        InputStream inputStream = artifactStore.get(tgzArtifact);
        assertThat(inputStream).isNotNull();

        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new GZIPInputStream(inputStream))) {
            TarArchiveEntry nextEntry = tarIn.getNextEntry();
            assertThat(nextEntry).isNotNull();
            assertThat(nextEntry.getName()).isEqualTo("README.txt");

            nextEntry = tarIn.getNextEntry();
            assertThat(nextEntry).isNull();
        }
    }

    @Test
    void testsnapshotartifctswithtimestamp() throws Exception {
        MockArtifactStore artifactStore =
                new MockArtifactStore(archiverManager, getResourceAsFile("/timestamp-snapshot"));
        assertThat(artifactStore).isNotNull();

        Artifact pomArtifact = new Artifact("localhost", "timestamp", "1.0-SNAPSHOT", "pom");
        Artifact jarArtifact = new Artifact("localhost", "timestamp", "1.0-SNAPSHOT", "jar");
        Artifact tgzArtifact = new Artifact("localhost", "timestamp", "1.0-SNAPSHOT", "bin", "tgz");
        Artifact zipArtifact = new Artifact("localhost", "timestamp", "1.0-SNAPSHOT", "bin", "zip");

        assertThatNoException().isThrownBy(() -> artifactStore.get(pomArtifact).close());
        assertThatNoException().isThrownBy(() -> artifactStore.get(jarArtifact).close());
        assertThatNoException().isThrownBy(() -> artifactStore.get(tgzArtifact).close());
        assertThatNoException().isThrownBy(() -> artifactStore.get(zipArtifact).close());

        Metadata metadata = artifactStore.getMetadata("localhost/timestamp/1.0-SNAPSHOT");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getVersioning().getLastUpdated()).isEqualTo("20250816121314");
        assertThat(metadata.getVersioning().getSnapshot().getTimestamp()).isEqualTo("20250816.121314");
        assertThat(metadata.getVersioning().getSnapshot().getBuildNumber()).isOne();
        assertThat(metadata.getVersioning().getSnapshotVersions()).hasSize(4);
        for (SnapshotVersion version : metadata.getVersioning().getSnapshotVersions()) {
            assertThat(version.getVersion()).isEqualTo("1.0-20250816.121314-1");
            assertThat(version.getUpdated()).isEqualTo("20250816121314");
        }
    }

    @Test
    void testsnapshotartifctswithtimestampEmptyJar() throws Exception {
        MockArtifactStore artifactStore =
                new MockArtifactStore(archiverManager, getResourceAsFile("/timestamp-snapshot"));
        assertThat(artifactStore).isNotNull();

        Artifact pomArtifactEmptyJar = new Artifact("localhost", "timestamp-empty-jar", "1.0-SNAPSHOT", "pom");
        Artifact jarArtifactEmptyJar = new Artifact("localhost", "timestamp-empty-jar", "1.0-SNAPSHOT", "jar");

        assertThatNoException()
                .isThrownBy(() -> artifactStore.get(pomArtifactEmptyJar).close());
        assertThatNoException()
                .isThrownBy(() -> artifactStore.get(jarArtifactEmptyJar).close());

        Metadata metadata = artifactStore.getMetadata("localhost/timestamp-empty-jar/1.0-SNAPSHOT");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getVersioning().getLastUpdated()).isEqualTo("20250816222324");
        assertThat(metadata.getVersioning().getSnapshot().getTimestamp()).isEqualTo("20250816.222324");
        assertThat(metadata.getVersioning().getSnapshot().getBuildNumber()).isOne();
        assertThat(metadata.getVersioning().getSnapshotVersions()).hasSize(2);
        for (SnapshotVersion version : metadata.getVersioning().getSnapshotVersions()) {
            assertThat(version.getVersion()).isEqualTo("1.0-20250816.222324-1");
            assertThat(version.getUpdated()).isEqualTo("20250816222324");
        }
    }

    @Test
    void testsnapshotartifctswithtimestampPlugin() throws Exception {
        MockArtifactStore artifactStore =
                new MockArtifactStore(archiverManager, getResourceAsFile("/timestamp-snapshot"));
        assertThat(artifactStore).isNotNull();
        Artifact pomArtifactPlugin = new Artifact("localhost", "timestamp-maven-plugin", "1.0-SNAPSHOT", "pom");
        Artifact jarArtifactPlugin = new Artifact("localhost", "timestamp-maven-plugin", "1.0-SNAPSHOT", "jar");

        assertThatNoException()
                .isThrownBy(() -> artifactStore.get(pomArtifactPlugin).close());
        assertThatNoException()
                .isThrownBy(() -> artifactStore.get(jarArtifactPlugin).close());

        Metadata metadata = artifactStore.getMetadata("localhost/timestamp-maven-plugin/1.0-SNAPSHOT");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getVersioning().getLastUpdated()).isEqualTo("20250816232425");
        assertThat(metadata.getVersioning().getSnapshot().getTimestamp()).isEqualTo("20250816.232425");
        assertThat(metadata.getVersioning().getSnapshot().getBuildNumber()).isOne();
        assertThat(metadata.getVersioning().getSnapshotVersions()).hasSize(2);
        for (SnapshotVersion version : metadata.getVersioning().getSnapshotVersions()) {
            assertThat(version.getVersion()).isEqualTo("1.0-20250816.232425-1");
            assertThat(version.getUpdated()).isEqualTo("20250816232425");
        }

        metadata = artifactStore.getMetadata("localhost");
        assertThat(metadata).isNotNull();
        assertThat(metadata.getPlugins()).isNotNull();
        assertThat(metadata.getPlugins()).hasSize(1);
        assertThat(metadata.getPlugins().get(0).getArtifactId()).isEqualTo("timestamp-maven-plugin");
        assertThat(metadata.getPlugins().get(0).getPrefix()).isEqualTo("timestamp");
    }

    @Test
    void lastModifiedWithTimestampSnapshot() throws Exception {
        MockArtifactStore artifactStore =
                new MockArtifactStore(archiverManager, getResourceAsFile("/timestamp-snapshot"));
        assertThat(artifactStore).isNotNull();

        Artifact pomArtifact = new Artifact("localhost", "timestamp", "1.0-SNAPSHOT", "pom");
        Artifact jarArtifact = new Artifact("localhost", "timestamp", "1.0-SNAPSHOT", "jar");
        Artifact tgzArtifact = new Artifact("localhost", "timestamp", "1.0-SNAPSHOT", "bin", "tgz");
        Artifact zipArtifact = new Artifact("localhost", "timestamp", "1.0-SNAPSHOT", "bin", "zip");

        long expected = LocalDateTime.of(2025, 8, 16, 12, 13, 14)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();

        assertThat(artifactStore.getLastModified(pomArtifact)).isEqualTo(expected);
        assertThat(artifactStore.getLastModified(jarArtifact)).isEqualTo(expected);
        assertThat(artifactStore.getLastModified(tgzArtifact)).isEqualTo(expected);
        assertThat(artifactStore.getLastModified(zipArtifact)).isEqualTo(expected);

        Artifact pomArtifactEmptyJar = new Artifact("localhost", "timestamp-empty-jar", "1.0-SNAPSHOT", "pom");
        Artifact jarArtifactEmptyJar = new Artifact("localhost", "timestamp-empty-jar", "1.0-SNAPSHOT", "jar");

        expected = LocalDateTime.of(2025, 8, 16, 22, 23, 24)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();

        assertThat(artifactStore.getLastModified(pomArtifactEmptyJar)).isEqualTo(expected);
        assertThat(artifactStore.getLastModified(jarArtifactEmptyJar)).isEqualTo(expected);

        Artifact pomArtifactPlugin = new Artifact("localhost", "timestamp-maven-plugin", "1.0-SNAPSHOT", "pom");
        Artifact jarArtifactPlugin = new Artifact("localhost", "timestamp-maven-plugin", "1.0-SNAPSHOT", "jar");

        expected = LocalDateTime.of(2025, 8, 16, 23, 24, 25)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();

        assertThat(artifactStore.getLastModified(pomArtifactPlugin)).isEqualTo(expected);
        assertThat(artifactStore.getLastModified(jarArtifactPlugin)).isEqualTo(expected);
    }

    @Test
    void lastModifiedWithTimestampSnapshotMetadata() throws Exception {
        MockArtifactStore artifactStore =
                new MockArtifactStore(archiverManager, getResourceAsFile("/timestamp-snapshot"));
        assertThat(artifactStore).isNotNull();

        long expected = LocalDateTime.of(2025, 8, 16, 23, 24, 25)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
        assertThat(artifactStore.getMetadataLastModified("localhost")).isEqualTo(expected);

        expected = LocalDateTime.of(2025, 8, 16, 12, 13, 14)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
        assertThat(artifactStore.getMetadataLastModified("localhost/timestamp")).isEqualTo(expected);
        assertThat(artifactStore.getMetadataLastModified("localhost/timestamp/1.0-SNAPSHOT"))
                .isEqualTo(expected);

        expected = LocalDateTime.of(2025, 8, 16, 22, 23, 24)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
        assertThat(artifactStore.getMetadataLastModified("localhost/timestamp-empty-jar"))
                .isEqualTo(expected);
        assertThat(artifactStore.getMetadataLastModified("localhost/timestamp-empty-jar/1.0-SNAPSHOT"))
                .isEqualTo(expected);

        expected = LocalDateTime.of(2025, 8, 16, 23, 24, 25)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
        assertThat(artifactStore.getMetadataLastModified("localhost/timestamp-maven-plugin"))
                .isEqualTo(expected);
        assertThat(artifactStore.getMetadataLastModified("localhost/timestamp-maven-plugin/1.0-SNAPSHOT"))
                .isEqualTo(expected);
    }

    @Test
    void directoryContentWithUnknownArchiver() throws Exception {
        assertThatThrownBy(() -> new MockArtifactStore(archiverManager, getResourceAsFile("/unknown-archiver")))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not find archiver for directory");
    }

    @Test
    void directoryWithClassifierContent() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mrm-xx"));

        Artifact pomArtifact = new Artifact("localhost", "mrm-xx", "1.0", "pom");
        assertThat(artifactStore.get(pomArtifact))
                .hasSameContentAs(Files.newInputStream(Path.of("target/test-classes/mrm-xx/mrm-xx-1.0.pom")));

        Artifact classifiedArtifact = new Artifact("localhost", "mrm-xx", "1.0", "javadoc-resources", "jar");
        assertThat(artifactStore.get(classifiedArtifact)).isNotNull();
    }

    @Test
    void sha1Checksum() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mrm-15"));

        Artifact pomArtifact = new Artifact("localhost", "mrm-15", "1.0", "pom");
        assertThat(artifactStore.getSha1Checksum(pomArtifact)).isNotNull();

        Artifact mainArtifact = new Artifact("localhost", "mrm-15", "1.0", "jar");
        String sha1Jar1 = artifactStore.getSha1Checksum(mainArtifact);

        artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/mrm-15"));
        String sha1Jar2 = artifactStore.getSha1Checksum(mainArtifact);

        assertThat(sha1Jar2).isEqualTo(sha1Jar1);
    }

    @Test
    void groupMetaDataShouldNotExistForNoPlugins() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/empty-jar"));

        assertThatThrownBy(() -> artifactStore.getMetadata("localhost"))
                .isExactlyInstanceOf(MetadataNotFoundException.class);
    }

    @Test
    void groupMetaDataShouldExistPlugins() throws Exception {
        MockArtifactStore artifactStore =
                new MockArtifactStore(archiverManager, getResourceAsFile("/empty-plugin-jar"));

        Metadata metadata = artifactStore.getMetadata("localhost");

        assertThat(metadata.getArtifactId()).isNull();
        assertThat(metadata.getGroupId()).isNull();
        assertThat(metadata.getVersion()).isNull();
        assertThat(metadata.getVersioning()).isNull();
        assertThat(metadata.getPlugins()).hasSize(2);

        assertThat(metadata.getPlugins().stream()
                        .filter(p -> "mrm-empty-maven-plugin".equals(p.getArtifactId()))
                        .filter(p -> "mrm-empty".equals(p.getPrefix()))
                        .anyMatch(p -> "Test Plugin 1".equals(p.getName())))
                .as("Plugin 1 not found in metadata")
                .isTrue();

        assertThat(metadata.getPlugins().stream()
                        .filter(p -> "mrm-empty-plugin-jar".equals(p.getArtifactId()))
                        .filter(p -> "mrm-empty-plugin-jar".equals(p.getPrefix()))
                        .anyMatch(p -> "Test Plugin 2".equals(p.getName())))
                .as("Plugin 2 not found in metadata")
                .isTrue();
    }

    @Test
    void artifactMetaDataShouldExist() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/empty-jar"));

        Metadata metadata = artifactStore.getMetadata("localhost/mrm-empty-jar");

        assertThat(metadata.getGroupId()).isEqualTo("localhost");
        assertThat(metadata.getArtifactId()).isEqualTo("mrm-empty-jar");
        assertThat(metadata.getVersion()).isNull();
        assertThat(metadata.getPlugins()).isEmpty();
        assertThat(metadata.getVersioning().getLatest()).isEqualTo("1.0");
        assertThat(metadata.getVersioning().getRelease()).isEqualTo("1.0");
        assertThat(metadata.getVersioning().getSnapshot()).isNull();
        assertThat(metadata.getVersioning().getLastUpdated()).isNotNull();
        assertThat(metadata.getVersioning().getSnapshotVersions()).isEmpty();
        assertThat(metadata.getVersioning().getVersions()).hasSize(1);
        assertThat(metadata.getVersioning().getVersions().get(0)).isEqualTo("1.0");
    }

    @Test
    void artifactVersionMetaDataShouldNotExistForReleaseVersion() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(archiverManager, getResourceAsFile("/empty-jar"));

        assertThatThrownBy(() -> artifactStore.getMetadata("localhost/mrm-empty-jar/1.0"))
                .isExactlyInstanceOf(MetadataNotFoundException.class);
    }

    @Test
    void artifactVersionMetaDataShouldExist() throws Exception {
        MockArtifactStore artifactStore =
                new MockArtifactStore(archiverManager, getResourceAsFile("/empty-jar-snapshot"));

        Metadata metadata = artifactStore.getMetadata("localhost/mrm-empty-jar/1.0-SNAPSHOT");

        assertThat(metadata.getGroupId()).isEqualTo("localhost");
        assertThat(metadata.getArtifactId()).isEqualTo("mrm-empty-jar");
        assertThat(metadata.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(metadata.getPlugins()).isEmpty();
        assertThat(metadata.getVersioning().getLatest()).isNull();
        assertThat(metadata.getVersioning().getRelease()).isNull();
        assertThat(metadata.getVersioning().getVersions()).isEmpty();
        assertThat(metadata.getVersioning().getLastUpdated()).isNotNull();
        assertThat(metadata.getVersioning().getSnapshot().getBuildNumber()).isOne();
        // assertNotNull(metadata.getVersioning().getSnapshot().getTimestamp()); - TODO check and fix
        assertThat(metadata.getVersioning().getSnapshotVersions()).hasSize(2);

        assertThat(metadata.getVersioning().getSnapshotVersions().stream()
                        .filter(v -> "".equals(v.getClassifier()))
                        .filter(v -> "pom".equals(v.getExtension()))
                        .filter(v -> !v.getUpdated().isEmpty())
                        .anyMatch(v -> "1.0-SNAPSHOT".equals(v.getVersion())))
                .isTrue();

        assertThat(metadata.getVersioning().getSnapshotVersions().stream()
                        .filter(v -> "".equals(v.getClassifier()))
                        .filter(v -> "jar".equals(v.getExtension()))
                        .filter(v -> !v.getUpdated().isEmpty())
                        .anyMatch(v -> "1.0-SNAPSHOT".equals(v.getVersion())))
                .isTrue();
    }

    @Test
    void directoryTransform() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(
                archiverManager, getResourceAsFile("/directory-transform"), new MetadataTransformDirectiveFactory());

        Artifact mainArtifact = new Artifact("localhost", "directory-transform", "1.0", "jar");
        InputStream inputStreamJar = artifactStore.get(mainArtifact);
        assertThat(inputStreamJar).isNotNull();

        List<String> names = new ArrayList<>();

        File jarFile = Files.createTempFile(temporaryFolder, "test", ".jar").toFile();
        Files.copy(inputStreamJar, jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ModuleDescriptor descriptor;
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                names.add(entry.getName());
            }

            JarEntry modInfo = jar.getJarEntry("module-info.class");
            try (InputStream in = jar.getInputStream(modInfo)) {
                descriptor = ModuleDescriptor.read(in);
            }
        }

        assertThat(descriptor.name()).isEqualTo("localhost.directory.transform");
        assertThat(descriptor.requires().stream()
                        .anyMatch(r -> "localhost.lib".equals(r.name())
                                && r.modifiers().isEmpty()))
                .isTrue();
        assertThat(descriptor.requires().stream()
                        .anyMatch(r -> "localhost.log.api".equals(r.name())
                                && r.modifiers().equals(Set.of(ModuleDescriptor.Requires.Modifier.STATIC))))
                .isTrue();

        assertThat(names).doesNotContain("module-info.java");
    }
}
