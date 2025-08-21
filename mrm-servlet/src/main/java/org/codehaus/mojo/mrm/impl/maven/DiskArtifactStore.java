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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.mojo.mrm.api.maven.ArchetypeCatalogNotFoundException;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.BaseArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * An artifact store backed by a directory on the local disk.
 *
 * @since 1.0
 */
public class DiskArtifactStore extends BaseArtifactStore {
    /**
     * The root of the artifact store.
     *
     * @since 1.0
     */
    private final File root;

    private boolean canWrite;

    /**
     * Creates a new artifact store hosted at the supplied root directory.
     *
     * @param root the root directory of the artifact store.
     * @since 1.0
     */
    public DiskArtifactStore(File root) {
        this.root = root;
    }

    public DiskArtifactStore canWrite(boolean canWrite) {
        this.canWrite = canWrite;
        return this;
    }

    @Override
    public Set<String> getGroupIds(String parentGroupId) {
        File parentDir = StringUtils.isEmpty(parentGroupId) ? root : new File(root, parentGroupId.replace('.', '/'));
        if (!parentDir.isDirectory()) {
            return Collections.emptySet();
        }
        File[] groupDirs = parentDir.listFiles();
        if (groupDirs == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(groupDirs)
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getArtifactIds(String groupId) {
        File groupDir = new File(root, groupId.replace('.', '/'));
        if (!groupDir.isDirectory()) {
            return Collections.emptySet();
        }

        File[] artifactDirs = groupDir.listFiles();
        if (artifactDirs == null) {
            return Collections.emptySet();
        }

        return Arrays.stream(artifactDirs)
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getVersions(String groupId, String artifactId) {
        File groupDir = new File(root, groupId.replace('.', '/'));
        File artifactDir = new File(groupDir, artifactId);
        if (!artifactDir.isDirectory()) {
            return Collections.emptySet();
        }
        File[] dirs = artifactDir.listFiles();
        if (dirs == null) {
            return Collections.emptySet();
        }

        return Arrays.stream(dirs).filter(File::isDirectory).map(File::getName).collect(Collectors.toSet());
    }

    @Override
    public Set<Artifact> getArtifacts(final String groupId, final String artifactId, final String version) {
        File groupDir = new File(root, groupId.replace('.', '/'));
        File artifactDir = new File(groupDir, artifactId);
        File versionDir = new File(artifactDir, version);
        if (!versionDir.isDirectory()) {
            return Collections.emptySet();
        }
        final Pattern rule;

        abstract class ArtifactFactory {
            abstract Artifact get(File file);
        }

        final ArtifactFactory factory;
        if (version.endsWith("-SNAPSHOT")) {
            rule = Pattern.compile("\\Q" + artifactId + "\\E-(?:\\Q" + StringUtils.removeEnd(version, "-SNAPSHOT")
                    + "\\E-(SNAPSHOT|(\\d{4})(\\d{2})(\\d{2})\\.(\\d{2})(\\d{2})(\\d{2})-(\\d+)))(?:-([^.]+))?"
                    + "\\.([^/]*)");
            factory = new ArtifactFactory() {
                public Artifact get(File file) {
                    Matcher matcher = rule.matcher(file.getName());
                    if (!matcher.matches()) {
                        return null;
                    }
                    if (matcher.group(1).equals("SNAPSHOT")) {
                        return new Artifact(groupId, artifactId, version, matcher.group(9), matcher.group(10));
                    }
                    try {
                        Calendar cal = new GregorianCalendar();
                        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
                        cal.set(Calendar.YEAR, Integer.parseInt(matcher.group(2)));
                        cal.set(Calendar.MONTH, Integer.parseInt(matcher.group(3)) - 1);
                        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(matcher.group(4)));
                        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(matcher.group(5)));
                        cal.set(Calendar.MINUTE, Integer.parseInt(matcher.group(6)));
                        cal.set(Calendar.SECOND, Integer.parseInt(matcher.group(7)));
                        long timestamp = cal.getTimeInMillis();
                        int buildNumber = Integer.parseInt(matcher.group(8));
                        return new Artifact(
                                groupId,
                                artifactId,
                                version,
                                matcher.group(9),
                                matcher.group(10),
                                timestamp,
                                buildNumber);
                    } catch (NullPointerException e) {
                        return null;
                    }
                }
            };
        } else {
            rule = Pattern.compile("\\Q" + artifactId + "\\E-\\Q" + version + "\\E(?:-([^.]+))?\\.(.+)");
            factory = new ArtifactFactory() {
                public Artifact get(File file) {
                    Matcher matcher = rule.matcher(file.getName());
                    if (!matcher.matches()) {
                        return null;
                    }
                    return new Artifact(groupId, artifactId, version, matcher.group(1), matcher.group(2));
                }
            };
        }
        File[] files = versionDir.listFiles();
        Set<Artifact> result = new HashSet<>(files.length);
        for (File file : files) {
            if (!file.isFile() || !rule.matcher(file.getName()).matches()) {
                continue;
            }
            Artifact artifact = factory.get(file);
            if (artifact != null) {
                result.add(artifact);
            }
        }
        return result;
    }

    @Override
    public long getLastModified(Artifact artifact) throws IOException, ArtifactNotFoundException {
        File file = getFileByBasename(artifact);
        return file.lastModified();
    }

    @Override
    public long getSize(Artifact artifact) throws IOException, ArtifactNotFoundException {
        File file = getFileByBasename(artifact);
        return file.length();
    }

    @Override
    public String getSha1Checksum(Artifact artifact) throws IOException, ArtifactNotFoundException {
        File file = getFileByBasename(artifact);
        File sha1File = new File(file.getPath() + ".sha1");
        if (sha1File.isFile()) {
            return new String(Files.readAllBytes(sha1File.toPath()), StandardCharsets.US_ASCII);
        } else {
            try (InputStream is = Files.newInputStream(file.toPath())) {
                return DigestUtils.sha1Hex(is);
            }
        }
    }

    @Override
    public InputStream get(Artifact artifact) throws IOException, ArtifactNotFoundException {
        File file = getFileByBasename(artifact);
        return Files.newInputStream(file.toPath());
    }

    @Override
    public void set(Artifact artifact, InputStream content) throws IOException {
        if (!canWrite) {
            throw new UnsupportedOperationException("Read-only store");
        }

        File targetFile = getFile(artifact);

        if (!targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs()) {
            throw new IOException(
                    "Failed to create " + targetFile.getParentFile().getPath());
        }

        try (OutputStream output = Files.newOutputStream(targetFile.toPath())) {
            IOUtils.copy(content, output);
        } finally {
            IOUtils.closeQuietly(content);
        }
    }

    @Override
    public Metadata getMetadata(String path) throws MetadataNotFoundException {
        MetadataInfo metadataInfo = prepareMetadata(path);
        if (metadataInfo != null) {
            return metadataInfo.metadata;
        } else {
            throw new MetadataNotFoundException(path);
        }
    }

    @Override
    public void setMetadata(String path, Metadata metadata) throws IOException {
        if (!canWrite) {
            throw new UnsupportedOperationException("Read-only store");
        }

        File file = root;
        String[] parts = StringUtils.strip(path, "/").split("/");
        for (String part : parts) {
            file = new File(file, part);
        }

        file = new File(file, "maven-metadata.xml");

        try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
            new MetadataXpp3Writer().write(outputStream, metadata);
        }
    }

    @Override
    public long getMetadataLastModified(String path) throws MetadataNotFoundException {
        MetadataInfo metadataInfo = prepareMetadata(path);
        if (metadataInfo != null) {
            return metadataInfo.lastModified;
        } else {
            throw new MetadataNotFoundException(path);
        }
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() throws IOException, ArchetypeCatalogNotFoundException {
        File file = new File(root, "archetype-catalog.xml");
        if (!file.isFile()) {
            throw new ArchetypeCatalogNotFoundException();
        }

        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return new ArchetypeCatalogXpp3Reader().read(inputStream);
        } catch (XmlPullParserException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public long getArchetypeCatalogLastModified() throws IOException, ArchetypeCatalogNotFoundException {
        File file = new File(root, "archetype-catalog.xml");
        if (!file.isFile()) {
            throw new ArchetypeCatalogNotFoundException();
        }
        return file.lastModified();
    }

    private File getFileByBasename(Artifact artifact) throws ArtifactNotFoundException {
        File groupDir = new File(root, artifact.getGroupId().replace('.', '/'));
        File artifactDir = new File(groupDir, artifact.getArtifactId());
        File versionDir = new File(artifactDir, artifact.getVersion());
        File file = new File(versionDir, artifact.getName());
        if (!file.exists()) {
            file = new File(versionDir, artifact.getBaseVersionName());
        }
        if (!file.isFile()) {
            throw new ArtifactNotFoundException(artifact);
        }
        return file;
    }

    private File getFile(Artifact artifact) {
        File groupDir = new File(root, artifact.getGroupId().replace('.', '/'));
        File artifactDir = new File(groupDir, artifact.getArtifactId());
        File versionDir = new File(artifactDir, artifact.getVersion());
        return new File(versionDir, artifact.getName());
    }

    private MetadataInfo prepareMetadata(String path) {
        File file = root;
        String[] parts = StringUtils.strip(path, "/").split("/");
        for (String part : parts) {
            file = new File(file, part);
        }

        MetadataInfo metadataInfo = null;
        try {
            metadataInfo = getMetadataFromLocalPath(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (metadataInfo == null) {
            try {
                metadataInfo = getMetadataFromSnapshotVersion(path);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return metadataInfo;
    }

    private MetadataInfo getMetadataFromLocalPath(File path) throws IOException {

        File file = new File(path, "maven-metadata.xml");

        if (!file.isFile()) {
            return null;
        }

        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return new MetadataInfo(new MetadataXpp3Reader().read(inputStream), file.lastModified());
        } catch (XmlPullParserException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private MetadataInfo getMetadataFromSnapshotVersion(String path) throws IOException {

        if (!path.endsWith("-SNAPSHOT")) {
            return null;
        }

        Path artifactPath = root.toPath().resolve(path);
        if (!Files.exists(artifactPath)) {
            return null;
        }

        LinkedList<String> pathItems =
                new LinkedList<>(Arrays.asList(StringUtils.strip(path, "/").split("/")));

        if (pathItems.size() < 3) {
            return null;
        }

        String version = pathItems.pollLast();
        String baseVersion = StringUtils.removeEnd(version, "-SNAPSHOT");
        String artifactId = pathItems.pollLast();

        String groupId = String.join(".", pathItems);

        List<String> artifactsList = getArtifactsListFromPath(artifactPath, artifactId, version);
        if (artifactsList.isEmpty()) {
            artifactsList = getArtifactsListFromPath(artifactPath, artifactId, baseVersion);
        }

        if (artifactsList.isEmpty()) {
            return null;
        }

        DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));
        DateTimeFormatter formatTime = DateTimeFormatter.ofPattern("HHmmss").withZone(ZoneId.of("UTC"));
        DateTimeFormatter formatDateTime =
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("UTC"));

        Metadata metadata = new Metadata();
        metadata.setGroupId(groupId);
        metadata.setArtifactId(artifactId);
        metadata.setVersion(version);

        List<SnapshotVersion> snapshotVersions = new ArrayList<>();

        int buildNr = -1;
        Instant dateTime = null;

        // first collect items and discover the latest timestamps
        for (String artifact : artifactsList) {
            Pattern pattern = Pattern.compile("\\Q" + artifactId + "\\E-\\Q" + baseVersion + "\\E-"
                    + "(SNAPSHOT|(\\d{8})\\.(\\d{6})-(\\d+))(?:-([^.]+))?\\.(.+)");

            Matcher matcher = pattern.matcher(artifact);
            if (matcher.find()) {
                SnapshotVersion snapshotVersion = new SnapshotVersion();
                if ("SNAPSHOT".equals(matcher.group(1))) {
                    buildNr = 9999;
                    Instant aDateTime = Files.getLastModifiedTime(artifactPath.resolve(artifact))
                            .toInstant();
                    if (dateTime == null || dateTime.isBefore(aDateTime)) {
                        dateTime = aDateTime;
                    }
                } else {
                    buildNr = Integer.parseInt(matcher.group(4));
                    dateTime = formatDateTime.parse(matcher.group(2) + matcher.group(3), Instant::from);
                }
                if (matcher.group(5) != null) {
                    snapshotVersion.setClassifier(matcher.group(5));
                }
                snapshotVersion.setExtension(matcher.group(6));
                snapshotVersions.add(snapshotVersion);
            }
        }

        if (snapshotVersions.isEmpty()) {
            // no items in directory
            return null;
        }

        String snapshotVersionText =
                baseVersion + "-" + formatDate.format(dateTime) + "." + formatTime.format(dateTime) + "-" + buildNr;
        String snapshotVersionUpdated = formatDateTime.format(dateTime);

        // next populate version and update time
        snapshotVersions.forEach(snapshotVersion -> {
            snapshotVersion.setVersion(snapshotVersionText);
            snapshotVersion.setUpdated(snapshotVersionUpdated);
        });

        Versioning versioning = new Versioning();
        versioning.setLastUpdated(formatDateTime.format(dateTime));

        versioning.setSnapshotVersions(snapshotVersions);

        Snapshot snapshot = new Snapshot();
        snapshot.setTimestamp(formatDate.format(dateTime) + "." + formatTime.format(dateTime));
        snapshot.setBuildNumber(buildNr);
        versioning.setSnapshot(snapshot);

        metadata.setVersioning(versioning);
        return new MetadataInfo(metadata, System.currentTimeMillis());
    }

    private List<String> getArtifactsListFromPath(Path artifactPath, String artifactId, String version)
            throws IOException {

        List<String> artifactsList;
        try (Stream<Path> pathStream = Files.walk(artifactPath, 1)) {
            String artifactVersion = artifactId + "-" + version;
            artifactsList = pathStream
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith(artifactVersion))
                    .filter(name -> !name.endsWith(".lastUpdated"))
                    .collect(Collectors.toList());
        }

        return artifactsList;
    }

    private static class MetadataInfo {

        private final Metadata metadata;
        private final long lastModified;

        private MetadataInfo(Metadata metadata, long lastModified) {
            this.metadata = metadata;
            this.lastModified = lastModified;
        }
    }
}
