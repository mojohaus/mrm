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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class CompositeArtifactStore
    implements ArtifactStore
{

    private final ArtifactStore[] stores;

    public CompositeArtifactStore( ArtifactStore[] stores )
    {
        stores.getClass();
        this.stores = stores;
    }

    public Set getGroupIds( String prefix )
    {
        Set result = new TreeSet();
        for ( int i = 0; i < stores.length; i++ )
        {
            Set groupIds = stores[i].getGroupIds( prefix );
            if ( groupIds != null )
            {
                result.addAll( groupIds );
            }
        }
        return result;
    }

    public Set getArtifactIds( String groupId )
    {
        Set result = new TreeSet();
        for ( int i = 0; i < stores.length; i++ )
        {
            Set artifactIds = stores[i].getArtifactIds( groupId );
            if ( artifactIds != null )
            {
                result.addAll( artifactIds );
            }
        }
        return result;
    }

    public Set getVersions( String groupId, String artifactId )
    {
        Set result = new TreeSet();
        for ( int i = 0; i < stores.length; i++ )
        {
            Set versions = stores[i].getVersions( groupId, artifactId );
            if ( versions != null )
            {
                result.addAll( versions );
            }
        }
        return result;
    }

    public Set getArtifacts( String groupId, String artifactId, String version )
    {
        Set result = new TreeSet();
        for ( int i = 0; i < stores.length; i++ )
        {
            Set artifacts = stores[i].getArtifacts( groupId, artifactId, version );
            if ( artifacts != null )
            {
                result.addAll( artifacts );
            }
        }
        return result;
    }

    public long getLastModified( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        for ( int i = 0; i < stores.length; i++ )
        {
            try
            {
                return stores[i].getLastModified( artifact );
            }
            catch ( ArtifactNotFoundException e )
            {
                // ignore
            }
        }
        throw new ArtifactNotFoundException( artifact );
    }

    public long getSize( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        for ( int i = 0; i < stores.length; i++ )
        {
            try
            {
                return stores[i].getSize( artifact );
            }
            catch ( ArtifactNotFoundException e )
            {
                // ignore
            }
        }
        throw new ArtifactNotFoundException( artifact );
    }

    public InputStream get( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        for ( int i = 0; i < stores.length; i++ )
        {
            try
            {
                return stores[i].get( artifact );
            }
            catch ( ArtifactNotFoundException e )
            {
                // ignore
            }
        }
        throw new ArtifactNotFoundException( artifact );
    }

    public void set( Artifact artifact, InputStream content )
        throws IOException
    {
        throw new IOException( "Read-only store" );
    }

    public Metadata getMetadata( String path )
        throws IOException, MetadataNotFoundException
    {
        boolean found = false;
        Metadata result = new Metadata();
        Set pluginArtifactIds = new HashSet();
        Set snapshotVersions = new HashSet();
        for ( int i = 0; i < stores.length; i++ )
        {
            try
            {
                Metadata partial = stores[i].getMetadata( path );
                if ( StringUtils.isEmpty( result.getArtifactId() ) && !StringUtils.isEmpty( partial.getArtifactId() ) )
                {
                    result.setArtifactId( partial.getArtifactId() );
                    found = true;
                }
                if ( StringUtils.isEmpty( result.getGroupId() ) && !StringUtils.isEmpty( partial.getGroupId() ) )
                {
                    result.setGroupId( partial.getGroupId() );
                    found = true;
                }
                if ( StringUtils.isEmpty( result.getVersion() ) && !StringUtils.isEmpty( partial.getVersion() ) )
                {
                    result.setVersion( partial.getVersion() );
                    found = true;
                }
                if ( partial.getPlugins() != null && !partial.getPlugins().isEmpty() )
                {
                    for ( Iterator j = partial.getPlugins().iterator(); j.hasNext(); )
                    {
                        Plugin plugin = (Plugin) j.next();
                        if ( !pluginArtifactIds.contains( plugin.getArtifactId() ) )
                        {
                            result.addPlugin( plugin );
                            pluginArtifactIds.add( plugin.getArtifactId() );
                        }
                    }
                    found = true;
                }
                if ( partial.getVersioning() != null )
                {
                    Versioning rVers = result.getVersioning();
                    if ( rVers == null )
                    {
                        rVers = new Versioning();
                    }
                    Versioning pVers = partial.getVersioning();
                    String rLU = found ? rVers.getLastUpdated() : null;
                    String pLU = pVers.getLastUpdated();
                    if ( pLU != null && ( rLU == null || rLU.compareTo( pLU ) < 0 ) )
                    {
                        // partial is newer or only
                        if ( !StringUtils.isEmpty( pVers.getLatest() ) )
                        {
                            rVers.setLatest( pVers.getLatest() );
                        }

                        if ( !StringUtils.isEmpty( pVers.getRelease() ) )
                        {
                            rVers.setLatest( pVers.getRelease() );
                        }
                        rVers.setLastUpdated( pVers.getLastUpdated() );
                    }
                    for ( Iterator j = pVers.getVersions().iterator(); j.hasNext(); )
                    {
                        String version = (String) j.next();
                        if ( !rVers.getVersions().contains( version ) )
                        {
                            rVers.addVersion( version );
                        }
                    }
                    if ( pVers.getSnapshot() != null )
                    {
                        if ( rVers.getSnapshot() == null
                            || pVers.getSnapshot().getBuildNumber() > rVers.getSnapshot().getBuildNumber() )
                        {
                            Snapshot snapshot = new Snapshot();
                            snapshot.setBuildNumber( pVers.getSnapshot().getBuildNumber() );
                            snapshot.setTimestamp( pVers.getSnapshot().getTimestamp() );
                            rVers.setSnapshot( snapshot );
                        }
                    }
                    if ( pVers.getSnapshotVersions() != null && !pVers.getSnapshotVersions().isEmpty() )
                    {
                        for ( Iterator j = pVers.getSnapshotVersions().iterator(); j.hasNext(); )
                        {
                            SnapshotVersion snapshotVersion = (SnapshotVersion) j.next();
                            String key = snapshotVersion.getVersion() + "-" + snapshotVersion.getClassifier() + "."
                                + snapshotVersion.getExtension();
                            if ( !snapshotVersions.contains( key ) )
                            {
                                rVers.addSnapshotVersion( snapshotVersion );
                                snapshotVersions.add( key );
                            }
                        }
                    }

                    result.setVersioning( rVers );
                    found = true;
                }
            }
            catch ( MetadataNotFoundException e )
            {
                // ignore
            }
        }
        if ( !found )
        {
            throw new MetadataNotFoundException( path );
        }
        return result;
    }

    public long getMetadataLastModified( String path )
        throws IOException, MetadataNotFoundException
    {
        boolean found = false;
        long lastModified = 0;
        for ( int i = 0; i < stores.length; i++ )
        {
            try
            {
                if ( !found )
                {
                    lastModified = stores[i].getMetadataLastModified( path );
                    found = true;
                }
                else
                {
                    lastModified = Math.max( lastModified, stores[i].getMetadataLastModified( path ) );
                }
            }
            catch ( MetadataNotFoundException e )
            {
                // ignore
            }
        }
        if ( !found )
        {
            throw new MetadataNotFoundException( path );
        }
        return lastModified;
    }
}
