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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.codehaus.mojo.mrm.api.maven.ArchetypeCatalogNotFoundException;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.api.maven.BaseArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;

/**
 * An artifact store that serves as a union of multiple artifact stores.
 *
 * @since 1.0
 */
public class CompositeArtifactStore extends BaseArtifactStore {

    /**
     * The backing artifact stores, in order of priority.
     *
     * @since 1.0
     */
    private final ArtifactStore[] stores;

    /**
     * Creates a new artifact store resulting from the union of multiple artifact stores.
     *
     * @param stores the artifact stores.
     * @since 1.0
     */
    public CompositeArtifactStore(ArtifactStore[] stores) {
        this.stores = stores;
    }

    @Override
    public Set<String> getGroupIds(String parentGroupId) {
        Set<String> result = new TreeSet<>();
        for (ArtifactStore store : stores) {
            Set<String> groupIds = store.getGroupIds(parentGroupId);
            if (groupIds != null) {
                result.addAll(groupIds);
            }
        }

        return result;
    }

    @Override
    public Set<String> getArtifactIds(String groupId) {
        Set<String> result = new TreeSet<>();
        for (ArtifactStore store : stores) {
            Set<String> artifactIds = store.getArtifactIds(groupId);
            if (artifactIds != null) {
                result.addAll(artifactIds);
            }
        }
        return result;
    }

    @Override
    public Set<String> getVersions(String groupId, String artifactId) {
        Set<String> result = new TreeSet<>();
        for (ArtifactStore store : stores) {
            Set<String> versions = store.getVersions(groupId, artifactId);
            if (versions != null) {
                result.addAll(versions);
            }
        }
        return result;
    }

    @Override
    public Set<Artifact> getArtifacts(String groupId, String artifactId, String version) {
        Set<Artifact> result = new TreeSet<>();
        for (ArtifactStore store : stores) {
            Set<Artifact> artifacts = store.getArtifacts(groupId, artifactId, version);
            if (artifacts != null) {
                result.addAll(artifacts);
            }
        }
        return result;
    }

    @Override
    public long getLastModified(Artifact artifact) throws IOException, ArtifactNotFoundException {
        for (ArtifactStore store : stores) {
            try {
                return store.getLastModified(artifact);
            } catch (ArtifactNotFoundException e) {
                // ignore
            }
        }
        throw new ArtifactNotFoundException(artifact);
    }

    @Override
    public long getSize(Artifact artifact) throws IOException, ArtifactNotFoundException {
        for (ArtifactStore store : stores) {
            try {
                return store.getSize(artifact);
            } catch (ArtifactNotFoundException e) {
                // ignore
            }
        }
        throw new ArtifactNotFoundException(artifact);
    }

    @Override
    public InputStream get(Artifact artifact) throws IOException, ArtifactNotFoundException {
        for (ArtifactStore store : stores) {
            try {
                return store.get(artifact);
            } catch (ArtifactNotFoundException e) {
                // ignore
            }
        }
        throw new ArtifactNotFoundException(artifact);
    }

    @Override
    public void set(Artifact artifact, InputStream content) throws IOException {
        throw new IOException("Read-only store");
    }

    @Override
    public Metadata getMetadata(String path) throws IOException, MetadataNotFoundException {
        Metadata result = null;

        for (ArtifactStore store : stores) {
            try {
                Metadata metadata = store.getMetadata(path);
                if (result == null) {
                    result = metadata;
                } else {
                    result.merge(metadata);
                }
            } catch (MetadataNotFoundException e) {
                // ignore
            }
        }

        if (result == null) {
            throw new MetadataNotFoundException(path);
        }
        return result;
    }

    @Override
    public long getMetadataLastModified(String path) throws IOException, MetadataNotFoundException {
        boolean found = false;
        long lastModified = 0;
        for (ArtifactStore store : stores) {
            try {
                if (!found) {
                    lastModified = store.getMetadataLastModified(path);
                    found = true;
                } else {
                    lastModified = Math.max(lastModified, store.getMetadataLastModified(path));
                }
            } catch (MetadataNotFoundException e) {
                // ignore
            }
        }
        if (!found) {
            throw new MetadataNotFoundException(path);
        }
        return lastModified;
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() throws IOException, ArchetypeCatalogNotFoundException {
        boolean found = false;
        ArchetypeCatalog result = new ArchetypeCatalog();
        for (ArtifactStore store : stores) {
            try {
                ArchetypeCatalog partial = store.getArchetypeCatalog();
                result.getArchetypes().addAll(partial.getArchetypes());
                found = true;
            } catch (ArchetypeCatalogNotFoundException e) {
                // ignore
            }
        }
        if (!found) {
            throw new ArchetypeCatalogNotFoundException();
        }
        return result;
    }

    @Override
    public long getArchetypeCatalogLastModified() throws IOException, ArchetypeCatalogNotFoundException {
        boolean found = false;
        long lastModified = 0;
        for (ArtifactStore store : stores) {
            try {
                if (!found) {
                    lastModified = store.getArchetypeCatalogLastModified();
                    found = true;
                } else {
                    lastModified = Math.max(lastModified, store.getArchetypeCatalogLastModified());
                }
            } catch (ArchetypeCatalogNotFoundException e) {
                // ignore
            }
        }
        if (!found) {
            throw new ArchetypeCatalogNotFoundException();
        }
        return lastModified;
    }
}
