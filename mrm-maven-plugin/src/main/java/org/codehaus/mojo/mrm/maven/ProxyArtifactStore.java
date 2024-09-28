package org.codehaus.mojo.mrm.maven;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.mrm.api.ResolverUtils;
import org.codehaus.mojo.mrm.api.maven.ArchetypeCatalogNotFoundException;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.BaseArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.codehaus.mojo.mrm.plugin.FactoryHelper;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;

import static java.util.Optional.ofNullable;

/**
 * An {@link org.codehaus.mojo.mrm.api.maven.ArtifactStore} that serves content from a running Maven instance.
 */
public class ProxyArtifactStore extends BaseArtifactStore {

    private final List<RemoteRepository> remoteRepositories;

    /**
     * The {@link Log} to log to.
     */
    private final Log log;

    /**
     * A cache of what artifacts are present.
     */
    private final Map<String, Map<String, Artifact>> children = new HashMap<>();

    private final RepositorySystem repositorySystem;

    private final MavenSession session;

    private final ArchetypeManager archetypeManager;

    /**
     * Creates a new instance.
     *
     * @param factoryHelper injected {@link FactoryHelper} instance
     * @param session {@link MavenSession} instance from Maven execution
     * @param log {@link Log} instance from {@link AbstractMojo}
     */
    public ProxyArtifactStore(FactoryHelper factoryHelper, MavenSession session, Log log) {
        this.repositorySystem = Objects.requireNonNull(factoryHelper.getRepositorySystem());
        this.archetypeManager = Objects.requireNonNull(factoryHelper.getArchetypeManager());
        this.log = log;
        this.session = Objects.requireNonNull(session);

        remoteRepositories = Stream.concat(
                        session.getCurrentProject().getRemoteProjectRepositories().stream(),
                        session.getCurrentProject().getRemotePluginRepositories().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Update the {@link #children} with a resolved artifact.
     *
     * @param artifact the artifact that was resolved.
     */
    private synchronized void addResolved(Artifact artifact) {
        String path =
                artifact.getGroupId().replace('.', '/') + '/' + artifact.getArtifactId() + "/" + artifact.getVersion();
        Map<String, Artifact> artifactMapper = this.children.computeIfAbsent(path, k -> new HashMap<>());
        artifactMapper.put(artifact.getName(), artifact);
        addResolved(path);
    }

    /**
     * Update the {@link #children} with a resolved path.
     *
     * @param path the path that was resolved.
     */
    private synchronized void addResolved(String path) {
        for (int index = path.lastIndexOf('/'); index > 0; index = path.lastIndexOf('/')) {
            String name = path.substring(index + 1);
            path = path.substring(0, index);
            Map<String, Artifact> artifactMapper = this.children.computeIfAbsent(path, k -> new HashMap<>());
            artifactMapper.put(name, null);
        }
        if (!StringUtils.isEmpty(path)) {
            Map<String, Artifact> artifactMapper = this.children.computeIfAbsent("", k -> new HashMap<>());
            artifactMapper.put(path, null);
        }
    }

    @Override
    public synchronized Set<String> getGroupIds(String parentGroupId) {
        String path = parentGroupId.replace('.', '/');
        Map<String, Artifact> artifactMapper = this.children.get(path);
        if (artifactMapper == null) {
            return Collections.emptySet();
        }
        return artifactMapper.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public synchronized Set<String> getArtifactIds(String groupId) {
        String path = groupId.replace('.', '/');
        Map<String, Artifact> artifactMapper = this.children.get(path);
        if (artifactMapper == null) {
            return Collections.emptySet();
        }
        return artifactMapper.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public synchronized Set<String> getVersions(String groupId, String artifactId) {
        String path = groupId.replace('.', '/') + '/' + artifactId;
        Map<String, Artifact> artifactMapper = this.children.get(path);
        if (artifactMapper == null) {
            return Collections.emptySet();
        }
        return artifactMapper.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public synchronized Set<Artifact> getArtifacts(String groupId, String artifactId, String version) {
        String path = groupId.replace('.', '/') + '/' + artifactId + "/" + version;
        Map<String, Artifact> artifactMapper = this.children.get(path);
        if (artifactMapper == null) {
            return Collections.emptySet();
        }
        return artifactMapper.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private File resolveArtifactFile(Artifact artifact) throws ArtifactNotFoundException {
        try {
            File file = ofNullable(repositorySystem
                            .resolveArtifact(
                                    session.getRepositorySession(),
                                    new ArtifactRequest(
                                            ResolverUtils.createArtifact(session, artifact),
                                            remoteRepositories,
                                            getClass().getSimpleName()))
                            .getArtifact())
                    .map(org.eclipse.aether.artifact.Artifact::getFile)
                    .filter(File::isFile)
                    .map(f -> {
                        try {
                            return f;
                        } finally {
                            addResolved(artifact);
                        }
                    })
                    .orElseThrow(() -> new ArtifactNotFoundException(artifact));
            log.debug("resolveArtifactFile(" + artifact + ") = " + file.getAbsolutePath());
            return file;
        } catch (org.eclipse.aether.resolution.ArtifactResolutionException e) {
            throw new ArtifactNotFoundException(artifact, e);
        }
    }

    @Override
    public long getLastModified(Artifact artifact) throws ArtifactNotFoundException {
        return resolveArtifactFile(artifact).lastModified();
    }

    @Override
    public long getSize(Artifact artifact) throws ArtifactNotFoundException {
        return resolveArtifactFile(artifact).length();
    }

    @Override
    public InputStream get(Artifact artifact) throws IOException, ArtifactNotFoundException {
        return Files.newInputStream(resolveArtifactFile(artifact).toPath());
    }

    @Override
    public void set(Artifact artifact, InputStream content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Metadata getMetadata(String path) throws MetadataNotFoundException {
        LinkedList<String> pathItems =
                new LinkedList<>(Arrays.asList(StringUtils.strip(path, "/").split("/")));

        String version;
        String artifactId;
        String groupId;

        org.eclipse.aether.metadata.Metadata.Nature metadataNature;

        if (pathItems.getLast().endsWith("-SNAPSHOT")) {
            // V level metadata request
            if (pathItems.size() < 3) {
                // at least we need G:A:V
                throw new MetadataNotFoundException(path);
            }
            metadataNature = org.eclipse.aether.metadata.Metadata.Nature.SNAPSHOT;
            version = pathItems.pollLast();
            artifactId = pathItems.pollLast();
        } else {
            // A or G level metadata request
            metadataNature = org.eclipse.aether.metadata.Metadata.Nature.RELEASE_OR_SNAPSHOT;
            version = null;
            artifactId = null;
        }

        groupId = String.join(".", pathItems);

        org.eclipse.aether.metadata.Metadata requestedMetadata =
                new DefaultMetadata(groupId, artifactId, version, "maven-metadata.xml", metadataNature);
        List<MetadataRequest> requests = new ArrayList<>();
        for (RemoteRepository repo : remoteRepositories) {
            MetadataRequest request = new MetadataRequest();
            request.setMetadata(requestedMetadata);
            request.setRepository(repo);
            requests.add(request);
        }

        List<MetadataResult> metadataResults =
                repositorySystem.resolveMetadata(session.getRepositorySession(), requests);

        Metadata resultMetadata = null;
        for (MetadataResult result : metadataResults) {
            if (!result.isResolved()) {
                continue;
            }
            Metadata metadata = readMetadata(result.getMetadata().getFile());
            if (metadata != null) {
                if (resultMetadata == null) {
                    resultMetadata = metadata;
                } else {
                    resultMetadata.merge(metadata);
                }
            }
        }

        if (resultMetadata == null) {
            throw new MetadataNotFoundException(path);
        }

        addResolved(path);
        return resultMetadata;
    }

    private Metadata readMetadata(File file) {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return new MetadataXpp3Reader().read(in);
        } catch (IOException | XmlPullParserException e) {
            log.warn("Error reading metadata from file: " + file, e);
        }
        return null;
    }

    @Override
    public long getMetadataLastModified(String path) throws MetadataNotFoundException {
        Metadata metadata = getMetadata(path);
        if (metadata != null) {
            if (!StringUtils.isEmpty(metadata.getGroupId())
                    || !StringUtils.isEmpty(metadata.getArtifactId())
                    || !StringUtils.isEmpty(metadata.getVersion())
                    || (metadata.getPlugins() != null && !metadata.getPlugins().isEmpty())
                    || (metadata.getVersioning() != null
                            && (!StringUtils.isEmpty(metadata.getVersioning().getLastUpdated())
                                    || !StringUtils.isEmpty(
                                            metadata.getVersioning().getLatest())
                                    || !StringUtils.isEmpty(
                                            metadata.getVersioning().getRelease())
                                    || (metadata.getVersioning().getVersions() != null
                                            && !metadata.getVersioning()
                                                    .getVersions()
                                                    .isEmpty())
                                    || (metadata.getVersioning().getSnapshot() != null)))) {
                return System.currentTimeMillis();
            }
        }
        throw new MetadataNotFoundException(path);
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() {
        return archetypeManager.getLocalCatalog(session.getRepositorySession());
    }

    @Override
    public long getArchetypeCatalogLastModified() throws ArchetypeCatalogNotFoundException {
        if (archetypeManager.getLocalCatalog(session.getRepositorySession()) != null) {
            return System.currentTimeMillis();
        } else {
            throw new ArchetypeCatalogNotFoundException();
        }
    }
}
