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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.mojo.mrm.api.maven.ArchetypeCatalogNotFoundException;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.BaseArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.codehaus.mojo.mrm.impl.Utils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An artifact store that keeps all its artifacts in memory.
 *
 * @since 1.0
 */
public class MockArtifactStore extends BaseArtifactStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockArtifactStore.class);

    private static final Pattern POM_TIMESTAMP_PATTERN = Pattern.compile("^.*(\\d{8})\\.(\\d{6})-(\\d+)$");
    /**
     * The extensions to search for when looking for POMs to mock.
     *
     * @since 1.0
     */
    public static final String[] POM_EXTENSIONS = {"pom"};

    /**
     * The contents of this artifact store.
     *
     * @since 1.0
     */
    private Map<String, Map<String, Map<String, Map<Artifact, Content>>>> contents = new HashMap<>();

    private Content archetypeCatalog;

    /**
     * Create a mock artifact store by scanning for POMs within the specified root.
     *
     * @param root the root to search for POMs within.
     * @since 1.0
     */
    public MockArtifactStore(ArchiverManager archiverManager, File root) {
        this(archiverManager, root, true);
    }

    /**
     * Create a mock artifact store by scanning for POMs within the specified root.
     *
     * @param root the root to search for POMs within.
     * @param archiverManager
     * @since 1.0
     */
    public MockArtifactStore(ArchiverManager archiverManager, File root, boolean lazyArchiver) {

        if (!root.isDirectory()) {
            throw new IllegalArgumentException("The specified root must be a directory: " + root);
        }

        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Collection<File> poms = FileUtils.listFiles(root, POM_EXTENSIONS, true);
        for (File file : poms) {
            try (FileReader fileReader = new FileReader(file)) {
                Model model = pomReader.read(fileReader);
                String groupId = model.getGroupId() != null
                        ? model.getGroupId()
                        : model.getParent().getGroupId();
                String artifactId = model.getArtifactId();
                String version = model.getVersion() != null
                        ? model.getVersion()
                        : model.getParent().getVersion();

                Artifact pomArtifact = createPomArtifact(groupId, artifactId, version, file);
                set(pomArtifact, new FileContent(file, pomArtifact.getTimestamp()));

                final String pomBasename = FilenameUtils.getBaseName(file.getName());

                if (StringUtils.isEmpty(model.getPackaging()) || "jar".equals(model.getPackaging())) {
                    File mainFile = new File(file.getParentFile(), pomBasename + ".jar");

                    Content content;
                    if (mainFile.isDirectory()) {
                        content = new DirectoryContent(
                                archiverManager, mainFile, lazyArchiver, pomArtifact.getTimestamp());
                    } else {
                        content = new BytesContent(Utils.newEmptyJarContent(), pomArtifact.getTimestamp());
                    }

                    set(
                            new Artifact(
                                    groupId,
                                    model.getArtifactId(),
                                    version,
                                    null,
                                    "jar",
                                    pomArtifact.getTimestamp(),
                                    pomArtifact.getBuildNumber()),
                            content);
                } else if ("maven-plugin".equals(model.getPackaging())) {
                    set(
                            new Artifact(
                                    groupId,
                                    model.getArtifactId(),
                                    version,
                                    null,
                                    "jar",
                                    pomArtifact.getTimestamp(),
                                    pomArtifact.getBuildNumber()),
                            new BytesContent(
                                    Utils.newEmptyMavenPluginJarContent(groupId, model.getArtifactId(), version),
                                    pomArtifact.getTimestamp()));
                }

                File[] classifiedFiles = file.getParentFile().listFiles((dir, name) -> FilenameUtils.getBaseName(name)
                        .startsWith(pomBasename + '-'));

                for (File classifiedFile : classifiedFiles) {
                    String type = org.codehaus.plexus.util.FileUtils.extension(classifiedFile.getName());
                    String classifier =
                            FilenameUtils.getBaseName(classifiedFile.getName()).substring(pomBasename.length() + 1);

                    Content content;
                    if (classifiedFile.isDirectory()) {
                        content = new DirectoryContent(
                                archiverManager, classifiedFile, lazyArchiver, pomArtifact.getTimestamp());
                    } else {
                        content = new FileContent(classifiedFile, pomArtifact.getTimestamp());
                    }

                    set(
                            new Artifact(
                                    groupId,
                                    model.getArtifactId(),
                                    version,
                                    classifier,
                                    type,
                                    pomArtifact.getTimestamp(),
                                    pomArtifact.getBuildNumber()),
                            content);
                }
            } catch (IOException e) {
                if (LOGGER != null) {
                    LOGGER.warn("Could not read from " + file, e);
                }
            } catch (XmlPullParserException e) {
                if (LOGGER != null) {
                    LOGGER.warn("Could not parse " + file, e);
                }
            }
        }

        File archetypeCatalogFile = new File(root, "archetype-catalog.xml");
        if (archetypeCatalogFile.isFile()) {
            archetypeCatalog = new FileContent(archetypeCatalogFile, null);
        }
    }

    private Artifact createPomArtifact(String groupId, String artifactId, String version, File file) {

        Integer buildNumber = null;
        Long timestamp = null;

        if (version.endsWith("-SNAPSHOT")) {
            String basename = FilenameUtils.getBaseName(file.getName());
            Matcher matcher = POM_TIMESTAMP_PATTERN.matcher(basename);
            if (matcher.matches()) {
                String dateString = matcher.group(1);
                String timeString = matcher.group(2);
                buildNumber = Integer.parseInt(matcher.group(3));
                timestamp = LocalDateTime.parse(
                                dateString + "-" + timeString, DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli();
            }
        }
        return new Artifact(groupId, artifactId, version, null, "pom", timestamp, buildNumber);
    }

    @Override
    public Set<String> getGroupIds(String parentGroupId) {
        TreeSet<String> result = new TreeSet<>();
        if (StringUtils.isEmpty(parentGroupId)) {
            for (String groupId : contents.keySet()) {
                int index = groupId.indexOf('.');
                result.add(index == -1 ? groupId : groupId.substring(0, index));
            }
        } else {
            String prefix = parentGroupId + '.';
            int start = prefix.length();
            for (String groupId : contents.keySet()) {
                if (groupId.startsWith(prefix)) {
                    int index = groupId.indexOf('.', start);
                    result.add(index == -1 ? groupId.substring(start) : groupId.substring(start, index));
                }
            }
        }
        return result;
    }

    @Override
    public Set<String> getArtifactIds(String groupId) {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get(groupId);
        return artifactMap == null ? Collections.emptySet() : new TreeSet<>(artifactMap.keySet());
    }

    @Override
    public Set<String> getVersions(String groupId, String artifactId) {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get(groupId);
        Map<String, Map<Artifact, Content>> versionMap = (artifactMap == null ? null : artifactMap.get(artifactId));
        return versionMap == null ? Collections.emptySet() : new TreeSet<>(versionMap.keySet());
    }

    @Override
    public Set<Artifact> getArtifacts(String groupId, String artifactId, String version) {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get(groupId);
        Map<String, Map<Artifact, Content>> versionMap = (artifactMap == null ? null : artifactMap.get(artifactId));
        Map<Artifact, Content> filesMap = (versionMap == null ? null : versionMap.get(version));

        return filesMap == null ? Collections.emptySet() : new HashSet<>(filesMap.keySet());
    }

    @Override
    public long getLastModified(Artifact artifact) throws IOException, ArtifactNotFoundException {
        return getContent(artifact).getLastModified();
    }

    @Override
    public long getSize(Artifact artifact) throws IOException, ArtifactNotFoundException {
        return getContent(artifact).getSize();
    }

    @Override
    public InputStream get(Artifact artifact) throws IOException, ArtifactNotFoundException {
        return getContent(artifact).getInputStream();
    }

    @Override
    public String getSha1Checksum(Artifact artifact) throws IOException, ArtifactNotFoundException {
        return getContent(artifact).getSha1Checksum();
    }

    private Content getContent(Artifact artifact) throws ArtifactNotFoundException {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get(artifact.getGroupId());
        Map<String, Map<Artifact, Content>> versionMap =
                (artifactMap == null ? null : artifactMap.get(artifact.getArtifactId()));
        Map<Artifact, Content> filesMap = (versionMap == null ? null : versionMap.get(artifact.getVersion()));
        Content content = (filesMap == null ? null : filesMap.get(artifact));
        if (content == null) {
            if (artifact.isSnapshot() && artifact.getTimestamp() == null && filesMap != null) {
                Artifact best = null;
                for (Map.Entry<Artifact, Content> entry : filesMap.entrySet()) {
                    Artifact a = entry.getKey();
                    if (artifact.equalSnapshots(a) && (best == null || best.compareTo(a) < 0)) {
                        best = a;
                        content = entry.getValue();
                    }
                }
                if (content == null) {
                    throw new ArtifactNotFoundException(artifact);
                }
            } else {
                throw new ArtifactNotFoundException(artifact);
            }
        }
        return content;
    }

    /**
     * Sets the content for a specified artifact.
     *
     * @param artifact the artifact.
     * @param content the content.
     * @since 1.0
     */
    private synchronized void set(Artifact artifact, Content content) {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap =
                contents.computeIfAbsent(artifact.getGroupId(), k -> new HashMap<>());
        Map<String, Map<Artifact, Content>> versionMap =
                artifactMap.computeIfAbsent(artifact.getArtifactId(), k -> new HashMap<>());
        Map<Artifact, Content> filesMap = versionMap.computeIfAbsent(artifact.getVersion(), k -> new HashMap<>());
        filesMap.put(artifact, content);
    }

    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public synchronized Metadata getMetadata(String path) throws IOException, MetadataNotFoundException {
        Metadata metadata = new Metadata();
        boolean foundMetadata = false;
        path = StringUtils.stripEnd(StringUtils.stripStart(path, "/"), "/");
        String groupId = path.replace('/', '.');
        Set<String> pluginArtifactIds = getArtifactIds(groupId);
        if (pluginArtifactIds != null) {
            List<Plugin> plugins = new ArrayList<>();
            for (String artifactId : pluginArtifactIds) {
                Set<String> pluginVersions = getVersions(groupId, artifactId);
                if (pluginVersions == null || pluginVersions.isEmpty()) {
                    continue;
                }
                String[] versions = pluginVersions.toArray(new String[0]);
                Arrays.sort(versions, INSTANCE);
                for (int j = versions.length - 1; j >= 0; j--) {
                    try (InputStream inputStream = get(new Artifact(groupId, artifactId, versions[j], "pom"))) {
                        Model model = new MavenXpp3Reader().read(new XmlStreamReader(inputStream));
                        if (model == null || !"maven-plugin".equals(model.getPackaging())) {
                            continue;
                        }
                        Plugin plugin = new Plugin();
                        plugin.setArtifactId(artifactId);
                        plugin.setName(model.getName());
                        // TODO proper goal-prefix determination
                        // ugh! this is incredibly hacky and does not handle some fool that sets the goal prefix in
                        // a parent pom... ok unlikely, but stupid is as stupid does
                        boolean havePrefix = false;
                        final Build build = model.getBuild();
                        if (build != null && build.getPlugins() != null) {
                            havePrefix = setPluginGoalPrefixFromConfiguration(plugin, build.getPlugins());
                        }
                        if (!havePrefix
                                && build != null
                                && build.getPluginManagement() != null
                                && build.getPluginManagement().getPlugins() != null) {
                            havePrefix = setPluginGoalPrefixFromConfiguration(
                                    plugin, build.getPluginManagement().getPlugins());
                        }
                        if (!havePrefix && artifactId.startsWith("maven-") && artifactId.endsWith("-plugin")) {
                            plugin.setPrefix(
                                    StringUtils.removeStart(StringUtils.removeEnd(artifactId, "-plugin"), "maven-"));
                            havePrefix = true;
                        }
                        if (!havePrefix && artifactId.endsWith("-maven-plugin")) {
                            plugin.setPrefix(StringUtils.removeEnd(artifactId, "-maven-plugin"));
                            havePrefix = true;
                        }
                        if (!havePrefix) {
                            plugin.setPrefix(artifactId);
                        }
                        plugins.add(plugin);
                        foundMetadata = true;
                        break;
                    } catch (ArtifactNotFoundException | XmlPullParserException e) {
                        // ignore
                    }
                }
            }
            if (!plugins.isEmpty()) {
                metadata.setPlugins(plugins);
            }
        }
        int index = path.lastIndexOf('/');
        groupId = (index == -1 ? groupId : groupId.substring(0, index)).replace('/', '.');
        String artifactId = (index == -1 ? null : path.substring(index + 1));
        if (artifactId != null) {
            Set<String> artifactVersions = getVersions(groupId, artifactId);
            if (artifactVersions != null && !artifactVersions.isEmpty()) {
                metadata.setGroupId(groupId);
                metadata.setArtifactId(artifactId);
                Versioning versioning = new Versioning();
                List<String> versions = new ArrayList<>(artifactVersions);
                versions.sort(INSTANCE); // sort the Maven way
                long lastUpdated = 0;
                for (String version : versions) {
                    try {
                        long lastModified = getLastModified(new Artifact(groupId, artifactId, version, "pom"));
                        versioning.addVersion(version);
                        if (lastModified >= lastUpdated) {
                            lastUpdated = lastModified;
                            versioning.setLastUpdatedTimestamp(new Date(lastModified));
                            versioning.setLatest(version);
                            if (!version.endsWith("-SNAPSHOT")) {
                                versioning.setRelease(version);
                            }
                        }
                    } catch (ArtifactNotFoundException e) {
                        // ignore
                    }
                }
                metadata.setVersioning(versioning);
                foundMetadata = true;
            }
        }

        int index2 = index == -1 ? -1 : path.lastIndexOf('/', index - 1);
        groupId = index2 == -1 ? groupId : groupId.substring(0, index2).replace('/', '.');
        artifactId = index2 == -1 ? artifactId : path.substring(index2 + 1, index);
        String version = index2 == -1 ? null : path.substring(index + 1);
        if (version != null && version.endsWith("-SNAPSHOT")) {
            Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get(groupId);
            Map<String, Map<Artifact, Content>> versionMap = (artifactMap == null ? null : artifactMap.get(artifactId));
            Map<Artifact, Content> filesMap = (versionMap == null ? null : versionMap.get(version));
            if (filesMap != null) {
                List<SnapshotVersion> snapshotVersions = new ArrayList<>();
                int maxBuildNumber = 0;
                long lastUpdated = 0;
                String timestamp = null;
                boolean found = false;
                for (final Map.Entry<Artifact, Content> entry : filesMap.entrySet()) {
                    final Artifact artifact = entry.getKey();
                    final Content content = entry.getValue();
                    addSnapshotVersion(snapshotVersions, artifact, content.getLastModified());
                    if ("pom".equals(artifact.getType())) {
                        if (artifact.getBuildNumber() != null && maxBuildNumber < artifact.getBuildNumber()) {
                            maxBuildNumber = artifact.getBuildNumber();
                            timestamp = artifact.getTimestampString();
                        } else {
                            maxBuildNumber = Math.max(1, maxBuildNumber);
                        }
                        lastUpdated = Math.max(lastUpdated, content.getLastModified());
                        found = true;
                    }
                }

                if (!snapshotVersions.isEmpty() || found) {
                    Versioning versioning = metadata.getVersioning();
                    if (versioning == null) {
                        versioning = new Versioning();
                    }
                    metadata.setGroupId(groupId);
                    metadata.setArtifactId(artifactId);
                    metadata.setVersion(version);
                    addSnapshotVersions(versioning, snapshotVersions);
                    if (maxBuildNumber > 0) {
                        Snapshot snapshot = new Snapshot();
                        snapshot.setBuildNumber(maxBuildNumber);
                        snapshot.setTimestamp(timestamp);
                        versioning.setSnapshot(snapshot);
                    }
                    versioning.setLastUpdatedTimestamp(new Date(lastUpdated));
                    metadata.setVersioning(versioning);
                    foundMetadata = true;
                }
            }
        }
        if (!foundMetadata) {
            throw new MetadataNotFoundException(path);
        }
        return metadata;
    }

    @Override
    public synchronized long getMetadataLastModified(String path) throws IOException, MetadataNotFoundException {
        boolean haveResult = false;
        long result = 0;
        path = StringUtils.stripEnd(StringUtils.stripStart(path, "/"), "/");
        String groupId = path.replace('/', '.');
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get(groupId);
        if (artifactMap != null) {
            for (Map<String, Map<Artifact, Content>> versionMap : artifactMap.values()) {
                for (Map<Artifact, Content> filesMap : versionMap.values()) {
                    for (Content content : filesMap.values()) {
                        haveResult = true;
                        result = Math.max(result, content.getLastModified());
                    }
                }
            }
        }
        int index = path.lastIndexOf('/');
        groupId = index == -1 ? groupId : groupId.substring(0, index).replace('/', '.');
        String artifactId = (index == -1 ? null : path.substring(index + 1));
        if (artifactId != null) {
            artifactMap = contents.get(groupId);
            Map<String, Map<Artifact, Content>> versionMap = (artifactMap == null ? null : artifactMap.get(artifactId));
            if (versionMap != null) {
                for (Map<Artifact, Content> filesMap : versionMap.values()) {
                    for (Content content : filesMap.values()) {
                        haveResult = true;
                        result = Math.max(result, content.getLastModified());
                    }
                }
            }
        }
        int index2 = index == -1 ? -1 : path.lastIndexOf('/', index - 1);
        groupId = index2 == -1 ? groupId : groupId.substring(0, index2).replace('/', '.');
        artifactId = index2 == -1 ? artifactId : path.substring(index2 + 1, index);
        String version = index2 == -1 ? null : path.substring(index + 1);
        if (version != null && version.endsWith("-SNAPSHOT")) {
            artifactMap = contents.get(groupId);
            Map<String, Map<Artifact, Content>> versionMap = (artifactMap == null ? null : artifactMap.get(artifactId));
            Map<Artifact, Content> filesMap = (versionMap == null ? null : versionMap.get(version));
            if (filesMap != null) {
                for (Content content : filesMap.values()) {
                    haveResult = true;
                    result = Math.max(result, content.getLastModified());
                }
            }
        }
        if (haveResult) {
            return result;
        }
        throw new MetadataNotFoundException(path);
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() throws IOException, ArchetypeCatalogNotFoundException {
        if (archetypeCatalog != null) {
            ArchetypeCatalogXpp3Reader reader = new ArchetypeCatalogXpp3Reader();
            try (InputStream inputStream = archetypeCatalog.getInputStream()) {
                return reader.read(inputStream);
            } catch (IOException e) {
                if (LOGGER != null) {
                    LOGGER.warn("Could not read from archetype-catalog.xml", e);
                }
            } catch (XmlPullParserException e) {
                if (LOGGER != null) {
                    LOGGER.warn("Could not parse archetype-catalog.xml", e);
                }
            }
        }
        throw new ArchetypeCatalogNotFoundException();
    }

    @Override
    public long getArchetypeCatalogLastModified() throws ArchetypeCatalogNotFoundException {
        if (archetypeCatalog != null) {
            return archetypeCatalog.getLastModified();
        } else {
            throw new ArchetypeCatalogNotFoundException();
        }
    }

    private boolean setPluginGoalPrefixFromConfiguration(
            Plugin plugin, List<org.apache.maven.model.Plugin> pluginConfigs) {
        for (org.apache.maven.model.Plugin def : pluginConfigs) {
            if ((def.getGroupId() == null || StringUtils.equals("org.apache.maven.plugins", def.getGroupId()))
                    && StringUtils.equals("maven-plugin-plugin", def.getArtifactId())) {
                Xpp3Dom configuration = (Xpp3Dom) def.getConfiguration();
                if (configuration != null) {
                    final Xpp3Dom goalPrefix = configuration.getChild("goalPrefix");
                    if (goalPrefix != null) {
                        plugin.setPrefix(goalPrefix.getValue());
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    private static final Comparator<String> INSTANCE = new VersionComparator();

    /**
     * Compares two versions using Maven's version comparison rules.
     *
     * @since 1.0
     */
    private static class VersionComparator implements Comparator<String> {
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public int compare(String o1, String o2) {
            ArtifactVersion v1 = new DefaultArtifactVersion(o1);
            ArtifactVersion v2 = new DefaultArtifactVersion(o2);
            return v1.compareTo(v2);
        }
    }

    /**
     * Holds the contents of an artifact.
     *
     * @since 1.0
     */
    private interface Content {

        /**
         * Returns the last modified timestamp.
         *
         * @return the last modified timestamp.
         * @since 1.0
         */
        long getLastModified();

        /**
         * Returns the content.
         *
         * @return the content.
         * @throws IOException if something went wrong.
         */
        InputStream getInputStream() throws IOException;

        /**
         * Returns the size of the content.
         *
         * @return the size of the content.
         */
        long getSize();

        /**
         * Returns the SHA-1 checksum of the content, if available.
         */
        String getSha1Checksum();
    }

    /**
     * Content held in memory.
     *
     * @since 1.0
     */
    private static class BytesContent implements Content {

        /**
         * The last modified timestamp.
         *
         * @since 1.0
         */
        private final long lastModified;

        /**
         * The content.
         *
         * @since 1.0
         */
        private final byte[] bytes;

        private final String sha1Checksum;

        /**
         * Creates a new instance from the specified content.
         *
         * @param bytes the content.
         * @param lastModified the last modified timestamp, or {@code null} to use the current time.
         * @since 1.0
         */
        private BytesContent(byte[] bytes, Long lastModified) {
            this.lastModified = lastModified != null ? lastModified : System.currentTimeMillis();
            this.bytes = bytes;
            this.sha1Checksum = DigestUtils.sha1Hex(bytes);
        }

        @Override
        public long getLastModified() {
            return lastModified;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public String getSha1Checksum() {
            return sha1Checksum;
        }
    }

    /**
     * Content held on disk.
     *
     * @since 1.0
     */
    private static class FileContent implements Content {

        /**
         * The backing file.
         *
         * @since 1.0
         */
        private final File file;

        private final Long lastModified;

        private final String sha1Checksum;

        /**
         * Creates a new instance.
         *
         * @param file the backing file.
         * @param lastModified the last modified timestamp, or {@code null} to use the file's last modified time.
         * @since 1.0
         */
        private FileContent(File file, Long lastModified) {
            this.file = file;
            this.lastModified = lastModified != null ? lastModified : file.lastModified();
            try (InputStream in = getInputStream()) {
                this.sha1Checksum = DigestUtils.sha1Hex(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public long getLastModified() {
            return lastModified;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(file.toPath());
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public String getSha1Checksum() {
            return sha1Checksum;
        }
    }

    private static class DirectoryContent implements Content {

        private final File directory;

        private final Long lastModified;

        private final Archiver archiver;

        private File archivedFile;

        private String sha1Checksum;

        /**
         * @param archiverManager the archiver manager to use for creating the archive
         * @param directory the directory to archive
         * @param lazy      {@code false} if the archive should be created immediately
         * @param lastModified the last modified timestamp, or {@code null} to use the directory's last modified time
         * @since 1.0
         */
        private DirectoryContent(ArchiverManager archiverManager, File directory, boolean lazy, Long lastModified) {

            this.directory = directory;
            this.lastModified = lastModified != null ? lastModified : directory.lastModified();

            try {
                this.archiver = archiverManager.getArchiver(directory);
            } catch (NoSuchArchiverException e) {
                throw new IllegalStateException(
                        "Could not find archiver for directory: " + directory + " available archivers: "
                                + archiverManager.getAvailableArchivers(),
                        e);
            }

            if (!lazy) {
                createArchive();
            }
        }

        private void createArchive() {
            synchronized (directory) {
                archivedFile = new File(directory.getParentFile(), "_" + directory.getName());
                archiver.setDestFile(archivedFile);
                archiver.addFileSet(DefaultFileSet.fileSet(directory));

                try {
                    archiver.setLastModifiedTime(Files.getLastModifiedTime(directory.toPath()));
                    archiver.createArchive();
                } catch (ArchiverException | IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }

        @Override
        public long getLastModified() {
            return lastModified;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (archivedFile == null) {
                createArchive();
            }
            return Files.newInputStream(archivedFile.toPath());
        }

        @Override
        public long getSize() {
            if (archivedFile == null) {
                createArchive();
            }
            return archivedFile.length();
        }

        @Override
        public String getSha1Checksum() {
            if (sha1Checksum == null) {
                try (InputStream inputStream = getInputStream()) {
                    sha1Checksum = DigestUtils.sha1Hex(inputStream);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            return sha1Checksum;
        }
    }

    /**
     * Adds a snapshot version to the list of snapshot versions.
     *
     * @param snapshotVersions the list of snapshot versions.
     * @param artifact         the artifact to add details of.
     * @param lastUpdatedTime  the time to flag for last updated.
     * @since 1.0
     */
    private static void addSnapshotVersion(
            List<SnapshotVersion> snapshotVersions, Artifact artifact, long lastUpdatedTime) {

        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));

        SnapshotVersion snapshotVersion = new SnapshotVersion();
        snapshotVersion.setExtension(artifact.getType());
        snapshotVersion.setClassifier(artifact.getClassifier() == null ? "" : artifact.getClassifier());
        snapshotVersion.setVersion(artifact.getTimestampVersion());
        snapshotVersion.setUpdated(fmt.format(new Date(lastUpdatedTime)));
        snapshotVersions.add(snapshotVersion);
    }

    /**
     * Add the list of {@link SnapshotVersion}s to the {@link Versioning}.
     *
     * @param versioning       the versionioning to add to.
     * @param snapshotVersions the snapshot versions to add.
     * @since 1.0
     */
    private static void addSnapshotVersions(Versioning versioning, List<SnapshotVersion> snapshotVersions) {
        for (SnapshotVersion snapshotVersion : snapshotVersions) {
            versioning.addSnapshotVersion(snapshotVersion);
        }
    }
}
