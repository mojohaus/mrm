package org.codehaus.mojo.mrm.impl.maven;

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

import org.apache.commons.io.IOUtils;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockArtifactStoreTest {

    @TempDir
    Path temporaryFolder;

    // MMOCKRM-3
    @Test
    void testInheritGavFromParent() {
        // don't fail
        MockArtifactStore mockArtifactStore = new MockArtifactStore(new File("src/test/resources/mmockrm-3"));
        assertEquals(2, mockArtifactStore.getArtifactIds("localhost").size());
    }

    // MMOCKRM-6
    @Test
    void testClassifiers() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(new File("src/test/resources/mmockrm-7"));

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
        MockArtifactStore artifactStore = new MockArtifactStore(new File("src/test/resources/mmockrm-10"));
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
        MockArtifactStore artifactStore = new MockArtifactStore(new File("target/test-classes/mrm-15"));

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
        MockArtifactStore artifactStore = new MockArtifactStore(new File("target/test-classes/empty-jar"));

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
        MockArtifactStore artifactStore = new MockArtifactStore(new File("target/test-classes/empty-plugin-jar"));

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
    void testDirectoryWithClassifierContent() throws Exception {
        MockArtifactStore artifactStore = new MockArtifactStore(new File("target/test-classes/mrm-xx"));

        Artifact pomArtifact = new Artifact("localhost", "mrm-xx", "1.0", "pom");
        assertNotNull(artifactStore.get(pomArtifact));
        assertTrue(IOUtils.contentEquals(
                Files.newInputStream(Paths.get("target/test-classes/mrm-xx/mrm-xx-1.0.pom")),
                artifactStore.get(pomArtifact)));

        Artifact classifiedArtifact = new Artifact("localhost", "mrm-xx", "1.0", "javadoc-resources", "jar");
        assertNotNull(artifactStore.get(classifiedArtifact));
    }
}
