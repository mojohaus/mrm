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
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
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
public class CompositeArtifactStore
    extends BaseArtifactStore
{

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
    public CompositeArtifactStore( ArtifactStore[] stores )
    {
        this.stores = stores;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getGroupIds( String parentGroupId )
    {
        Set<String> result = new TreeSet<>();
        for (ArtifactStore store : stores)
        {
            Set<String> groupIds = store.getGroupIds(parentGroupId);
            if (groupIds != null)
            {
                result.addAll(groupIds);
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getArtifactIds( String groupId )
    {
        Set<String> result = new TreeSet<>();
        for (ArtifactStore store : stores)
        {
            Set<String> artifactIds = store.getArtifactIds(groupId);
            if (artifactIds != null)
            {
                result.addAll(artifactIds);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getVersions( String groupId, String artifactId )
    {
        Set<String> result = new TreeSet<>();
        for (ArtifactStore store : stores)
        {
            Set<String> versions = store.getVersions(groupId, artifactId);
            if (versions != null)
            {
                result.addAll(versions);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Artifact> getArtifacts( String groupId, String artifactId, String version )
    {
        Set<Artifact> result = new TreeSet<>();
        for (ArtifactStore store : stores)
        {
            Set<Artifact> artifacts = store.getArtifacts(groupId, artifactId, version);
            if (artifacts != null)
            {
                result.addAll(artifacts);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        for (ArtifactStore store : stores)
        {
            try
            {
                return store.getLastModified(artifact);
            }
            catch (ArtifactNotFoundException e)
            {
                // ignore
            }
        }
        throw new ArtifactNotFoundException( artifact );
    }

    /**
     * {@inheritDoc}
     */
    public long getSize( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        for (ArtifactStore store : stores)
        {
            try
            {
                return store.getSize(artifact);
            } catch (ArtifactNotFoundException e)
            {
                // ignore
            }
        }
        throw new ArtifactNotFoundException( artifact );
    }

    /**
     * {@inheritDoc}
     */
    public InputStream get( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        for (ArtifactStore store : stores)
        {
            try
            {
                return store.get(artifact);
            } catch (ArtifactNotFoundException e) {
                // ignore
            }
        }
        throw new ArtifactNotFoundException( artifact );
    }

    /**
     * {@inheritDoc}
     */
    public void set( Artifact artifact, InputStream content )
        throws IOException
    {
        throw new IOException( "Read-only store" );
    }

    /**
     * {@inheritDoc}
     */
    public Metadata getMetadata( String path )
        throws IOException, MetadataNotFoundException
    {
        boolean found = false;
        Metadata result = new Metadata();
        Set<String> pluginArtifactIds = new HashSet<>();
        Set<String> snapshotVersions = new HashSet<>();
        for (ArtifactStore store : stores)
        {
            try {
                Metadata partial = store.getMetadata(path);
                if (StringUtils.isEmpty(result.getArtifactId()) && !StringUtils.isEmpty(partial.getArtifactId()))
                {
                    result.setArtifactId(partial.getArtifactId());
                    found = true;
                }
                if (StringUtils.isEmpty(result.getGroupId()) && !StringUtils.isEmpty(partial.getGroupId()))
                {
                    result.setGroupId(partial.getGroupId());
                    found = true;
                }
                if (StringUtils.isEmpty(result.getVersion()) && !StringUtils.isEmpty(partial.getVersion()))
                {
                    result.setVersion(partial.getVersion());
                    found = true;
                }
                if (partial.getPlugins() != null && !partial.getPlugins().isEmpty())
                {
                    for (Plugin plugin : partial.getPlugins())
                    {
                        if (!pluginArtifactIds.contains(plugin.getArtifactId()))
                        {
                            result.addPlugin(plugin);
                            pluginArtifactIds.add(plugin.getArtifactId());
                        }
                    }
                    found = true;
                }
                if (partial.getVersioning() != null)
                {
                    Versioning rVers = result.getVersioning();
                    if (rVers == null) {
                        rVers = new Versioning();
                    }
                    Versioning pVers = partial.getVersioning();
                    String rLU = found ? rVers.getLastUpdated() : null;
                    String pLU = pVers.getLastUpdated();
                    if (pLU != null && (rLU == null || rLU.compareTo(pLU) < 0)) {
                        // partial is newer or only
                        if (!StringUtils.isEmpty(pVers.getLatest())) {
                            rVers.setLatest(pVers.getLatest());
                        }

                        if (!StringUtils.isEmpty(pVers.getRelease())) {
                            rVers.setLatest(pVers.getRelease());
                        }
                        rVers.setLastUpdated(pVers.getLastUpdated());
                    }
                    for (String version : pVers.getVersions()) {
                        if (!rVers.getVersions().contains(version)) {
                            rVers.addVersion(version);
                        }
                    }
                    if (pVers.getSnapshot() != null) {
                        if (rVers.getSnapshot() == null
                                || pVers.getSnapshot().getBuildNumber() > rVers.getSnapshot().getBuildNumber()) {
                            Snapshot snapshot = new Snapshot();
                            snapshot.setBuildNumber(pVers.getSnapshot().getBuildNumber());
                            snapshot.setTimestamp(pVers.getSnapshot().getTimestamp());
                            rVers.setSnapshot(snapshot);
                        }
                    }
                    try {
                        if (pVers.getSnapshotVersions() != null && !pVers.getSnapshotVersions().isEmpty()) {
                            for (SnapshotVersion snapshotVersion : pVers.getSnapshotVersions()) {
                                String key = snapshotVersion.getVersion() + "-" + snapshotVersion.getClassifier() + "."
                                        + snapshotVersion.getExtension();
                                if (!snapshotVersions.contains(key)) {
                                    rVers.addSnapshotVersion(snapshotVersion);
                                    snapshotVersions.add(key);
                                }
                            }
                        }
                    } catch (NoSuchMethodError e) {
                        // Maven 2
                    }

                    result.setVersioning(rVers);
                    found = true;
                }
            } catch (MetadataNotFoundException e) {
                // ignore
            }
        }
        if ( !found )
        {
            throw new MetadataNotFoundException( path );
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getMetadataLastModified( String path )
        throws IOException, MetadataNotFoundException
    {
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
        if ( !found )
        {
            throw new MetadataNotFoundException( path );
        }
        return lastModified;
    }
    
    public ArchetypeCatalog getArchetypeCatalog()
        throws IOException, ArchetypeCatalogNotFoundException
    {
        boolean found = false;
        ArchetypeCatalog result = new ArchetypeCatalog();
        for ( ArtifactStore store : stores )
        {
            try
            {
                ArchetypeCatalog partial = store.getArchetypeCatalog();
                result.getArchetypes().addAll( partial.getArchetypes() );
                found = true;
            }
            catch ( ArchetypeCatalogNotFoundException e )
            {
                // ignore
            }
        }
        if ( !found )
        {
            throw new ArchetypeCatalogNotFoundException();
        }
        return result;
    }
    
    public long getArchetypeCatalogLastModified()
        throws IOException, ArchetypeCatalogNotFoundException
    {
        boolean found = false;
        long lastModified = 0;
        for ( ArtifactStore store : stores )
        {
            try
            {
                if ( !found )
                {
                    lastModified = store.getArchetypeCatalogLastModified();
                    found = true;
                }
                else
                {
                    lastModified = Math.max( lastModified, store.getArchetypeCatalogLastModified() );
                }
            }
            catch ( ArchetypeCatalogNotFoundException e )
            {
                // ignore
            }
        }
        if ( !found )
        {
            throw new ArchetypeCatalogNotFoundException( );
        }
        return lastModified;
    }
}
