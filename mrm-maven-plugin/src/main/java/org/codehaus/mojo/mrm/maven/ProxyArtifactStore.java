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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.*;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.mrm.api.ResolverUtils;
import org.codehaus.mojo.mrm.api.maven.*;
import org.codehaus.mojo.mrm.plugin.FactoryHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;

import static java.util.Optional.ofNullable;

/**
 * An {@link org.codehaus.mojo.mrm.api.maven.ArtifactStore} that serves content from a running Maven instance.
 */
public class ProxyArtifactStore extends BaseArtifactStore {

    private final List<RemoteRepository> remoteRepositories;

    /**
     * The remote repositories that we will query.
     */
    private final List<ArtifactRepository> artifactRepositories;

    /**
     * The {@link Log} to log to.
     */
    private final Log log;

    /**
     * A version range that matches any version
     */
    private static final VersionRange ANY_VERSION;

    /**
     * A cache of what artifacts are present.
     */
    private final Map<String, Map<String, Artifact>> children = new HashMap<>();

    private final RepositorySystem repositorySystem;

    private final MavenSession session;

    static {
        try {
            ANY_VERSION = VersionRange.createFromVersionSpec("[0,]");
        } catch (InvalidVersionSpecificationException e) {
            // must never happen... so if it does make sure we stop
            throw new IllegalStateException("[0,] should always be a valid version specification", e);
        }
    }

    private final RepositoryMetadataManager repositoryMetadataManager;

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
        this.repositoryMetadataManager = Objects.requireNonNull(factoryHelper.getRepositoryMetadataManager());
        this.archetypeManager = Objects.requireNonNull(factoryHelper.getArchetypeManager());
        this.log = log;
        this.session = Objects.requireNonNull(session);

        artifactRepositories = Stream.concat(
                        session.getCurrentProject().getRemoteArtifactRepositories().stream(),
                        session.getCurrentProject().getPluginArtifactRepositories().stream())
                .distinct()
                .collect(Collectors.toList());
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
        path = StringUtils.strip(path, "/");
        Metadata metadata = new Metadata();
        boolean foundSomething = false;

        // is this path a groupId:artifactId pair?
        int slashIndex = path.lastIndexOf('/');
        String artifactId = slashIndex == -1 ? null : path.substring(slashIndex + 1);
        String groupId = slashIndex == -1 ? null : path.substring(0, slashIndex).replace('/', '.');
        if (!StringUtils.isEmpty(artifactId) && !StringUtils.isEmpty(groupId)) {
            org.apache.maven.artifact.Artifact artifact = createDependencyArtifact(groupId, artifactId);
            ArtifactRepositoryMetadata artifactRepositoryMetadata = new ArtifactRepositoryMetadata(artifact);
            try {
                repositoryMetadataManager.resolve(
                        artifactRepositoryMetadata, artifactRepositories, session.getLocalRepository());

                final Metadata artifactMetadata = artifactRepositoryMetadata.getMetadata();
                if (artifactMetadata.getVersioning() != null) {
                    foundSomething = true;
                    if (StringUtils.isEmpty(metadata.getGroupId())) {
                        metadata.setGroupId(groupId);
                        metadata.setArtifactId(artifactId);
                    }
                    metadata.merge(artifactMetadata);
                    for (String v : artifactMetadata.getVersioning().getVersions()) {
                        addResolved(path + "/" + v);
                    }
                }
            } catch (RepositoryMetadataResolutionException e) {
                log.debug(e);
            }
        }

        if (!foundSomething) {
            throw new MetadataNotFoundException(path);
        }
        addResolved(path);
        return metadata;
    }

    private org.apache.maven.artifact.Artifact createDependencyArtifact(String groupId, String artifactId) {

        return new org.apache.maven.artifact.DefaultArtifact(
                groupId, artifactId, ANY_VERSION, "compile", "pom", "", null);
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
        return archetypeManager.getLocalCatalog(session.getProjectBuildingRequest());
    }

    @Override
    public long getArchetypeCatalogLastModified() throws ArchetypeCatalogNotFoundException {
        if (archetypeManager.getLocalCatalog(session.getProjectBuildingRequest()) != null) {
            return System.currentTimeMillis();
        } else {
            throw new ArchetypeCatalogNotFoundException();
        }
    }
}
