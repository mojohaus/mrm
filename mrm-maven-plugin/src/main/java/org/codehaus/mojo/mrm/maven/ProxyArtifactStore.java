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

package org.codehaus.mojo.mrm.maven;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProxyArtifactStore
    implements ArtifactStore
{
    private final RepositoryMetadataManager repositoryMetadataManager;

    private final List remoteArtifactRepositories;

    private final List remotePluginRepositories;

    private final ArtifactRepository localRepository;

    private final ArtifactFactory artifactFactory;

    private final List remoteRepositories;

    private final VersionRange anyVersion;

    private final ArtifactResolver artifactResolver;

    private final Log log;

    private final Map/*<String,Map<String,Artifact>>*/ children = new HashMap();

    public ProxyArtifactStore( RepositoryMetadataManager repositoryMetadataManager, List remoteArtifactRepositories,
                               List remotePluginRepositories, ArtifactRepository localRepository,
                               ArtifactFactory artifactFactory, ArtifactResolver artifactResolver, Log log )
        throws InvalidVersionSpecificationException
    {
        this.repositoryMetadataManager = repositoryMetadataManager;
        this.remoteArtifactRepositories = remoteArtifactRepositories;
        this.remotePluginRepositories = remotePluginRepositories;
        this.localRepository = localRepository;
        this.artifactFactory = artifactFactory;
        this.artifactResolver = artifactResolver;
        this.log = log;
        remoteRepositories = new ArrayList();
        remoteRepositories.addAll( remoteArtifactRepositories );
        remoteRepositories.addAll( remotePluginRepositories );
        anyVersion = VersionRange.createFromVersionSpec( "[0,]" );
    }

    private synchronized void addResolved( Artifact artifact )
    {
        String path =
            artifact.getGroupId().replace( '.', '/' ) + '/' + artifact.getArtifactId() + "/" + artifact.getVersion();
        Map children = (Map) this.children.get( path );
        if ( children == null )
        {
            children = new HashMap();
            this.children.put( path, children );
        }
        children.put( artifact.getName(), artifact );
        addResolved( path );
    }

    private synchronized void addResolved( String path )
    {
        for ( int index = path.lastIndexOf( '/' ); index > 0; index = path.lastIndexOf( '/' ) )
        {
            String name = path.substring( index + 1 );
            path = path.substring( 0, index );
            Map children = (Map) this.children.get( path );
            if ( children == null )
            {
                children = new HashMap();
                this.children.put( path, children );
            }
            children.put( name, null );
        }
        if ( !StringUtils.isEmpty( path ) )
        {
            Map children = (Map) this.children.get( "" );
            if ( children == null )
            {
                children = new HashMap();
                this.children.put( "", children );
            }
            children.put( path, null );
        }
    }

    public synchronized Set getGroupIds( String prefix )
    {
        String path = prefix.replace( '.', '/' );
        Map children = (Map) this.children.get( path );
        if ( children == null )
        {
            return Collections.EMPTY_SET;
        }
        Set result = new HashSet();
        for ( Iterator i = children.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry e = (Map.Entry) i.next();
            if ( e.getValue() == null )
            {
                result.add( e.getKey() );
            }
        }
        return result;
    }

    public synchronized Set getArtifactIds( String groupId )
    {
        String path = groupId.replace( '.', '/' );
        Map children = (Map) this.children.get( path );
        if ( children == null )
        {
            return Collections.EMPTY_SET;
        }
        Set result = new HashSet();
        for ( Iterator i = children.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry e = (Map.Entry) i.next();
            if ( e.getValue() == null )
            {
                result.add( e.getKey() );
            }
        }
        return result;
    }

    public synchronized Set getVersions( String groupId, String artifactId )
    {
        String path = groupId.replace( '.', '/' ) + '/' + artifactId;
        Map children = (Map) this.children.get( path );
        if ( children == null )
        {
            return Collections.EMPTY_SET;
        }
        Set result = new HashSet();
        for ( Iterator i = children.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry e = (Map.Entry) i.next();
            if ( e.getValue() == null )
            {
                result.add( e.getKey() );
            }
        }
        return result;
    }

    public synchronized Set getArtifacts( String groupId, String artifactId, String version )
    {
        String path = groupId.replace( '.', '/' ) + '/' + artifactId + "/" + version;
        Map children = (Map) this.children.get( path );
        if ( children == null )
        {
            return Collections.EMPTY_SET;
        }
        Set result = new HashSet();
        for ( Iterator i = children.values().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
            if ( a != null )
            {
                result.add( a );
            }
        }
        return result;
    }

    public long getLastModified( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        org.apache.maven.artifact.Artifact mavenArtifact =
            artifactFactory.createArtifactWithClassifier( artifact.getGroupId(), artifact.getArtifactId(),
                                                          artifact.getTimestampVersion(), artifact.getType(),
                                                          artifact.getClassifier() );
        try
        {
            artifactResolver.resolve( mavenArtifact, remoteRepositories, localRepository );
            final File file = mavenArtifact.getFile();
            if ( file != null && file.isFile() )
            {
                addResolved( artifact );
                return file.lastModified();
            }
            throw new ArtifactNotFoundException( artifact );
        }
        catch ( org.apache.maven.artifact.resolver.ArtifactNotFoundException e )
        {
            ArtifactNotFoundException anfe = new ArtifactNotFoundException( artifact );
            anfe.initCause( e );
            throw anfe;
        }
        catch ( ArtifactResolutionException e )
        {
            IOException ioe = new IOException( e.getMessage() );
            ioe.initCause( e );
            throw ioe;
        }
    }

    public long getSize( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        org.apache.maven.artifact.Artifact mavenArtifact =
            artifactFactory.createArtifactWithClassifier( artifact.getGroupId(), artifact.getArtifactId(),
                                                          artifact.getTimestampVersion(), artifact.getType(),
                                                          artifact.getClassifier() );
        try
        {
            artifactResolver.resolve( mavenArtifact, remoteRepositories, localRepository );
            final File file = mavenArtifact.getFile();
            if ( file != null && file.isFile() )
            {
                addResolved( artifact );
                return file.length();
            }
            throw new ArtifactNotFoundException( artifact );
        }
        catch ( org.apache.maven.artifact.resolver.ArtifactNotFoundException e )
        {
            ArtifactNotFoundException anfe = new ArtifactNotFoundException( artifact );
            anfe.initCause( e );
            throw anfe;
        }
        catch ( ArtifactResolutionException e )
        {
            IOException ioe = new IOException( e.getMessage() );
            ioe.initCause( e );
            throw ioe;
        }
    }

    public InputStream get( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        org.apache.maven.artifact.Artifact mavenArtifact =
            artifactFactory.createArtifactWithClassifier( artifact.getGroupId(), artifact.getArtifactId(),
                                                          artifact.getTimestampVersion(), artifact.getType(),
                                                          artifact.getClassifier() );
        try
        {
            artifactResolver.resolve( mavenArtifact, remoteRepositories, localRepository );
            final File file = mavenArtifact.getFile();
            if ( file != null && file.isFile() )
            {
                addResolved( artifact );
                return new FileInputStream( file );
            }
            throw new ArtifactNotFoundException( artifact );
        }
        catch ( org.apache.maven.artifact.resolver.ArtifactNotFoundException e )
        {
            ArtifactNotFoundException anfe = new ArtifactNotFoundException( artifact );
            anfe.initCause( e );
            throw anfe;
        }
        catch ( ArtifactResolutionException e )
        {
            IOException ioe = new IOException( e.getMessage() );
            ioe.initCause( e );
            throw ioe;
        }
    }

    public void set( Artifact artifact, InputStream content )
        throws IOException
    {
        throw new UnsupportedOperationException();
    }

    public Metadata getMetadata( String path )
        throws IOException, MetadataNotFoundException
    {
        path = StringUtils.strip( path, "/" );
        int index = path.lastIndexOf( '/' );
        int index2 = index == -1 ? -1 : path.lastIndexOf( '/', index - 1 );

        String version = index2 == -1 ? null : path.substring( index + 1 );
        String artifactId = index2 == -1 ? null : path.substring( index2 + 1, index );
        String groupId = index2 == -1 ? null : path.substring( 0, index2 ).replace( '/', '.' );

        Metadata metadata = new Metadata();

        boolean foundSomething = false;

        // is this path a groupId:artifactId pair?
        if ( version != null && version.endsWith( "-SNAPSHOT" ) && !StringUtils.isEmpty( artifactId )
            && !StringUtils.isEmpty( groupId ) )
        {
            final org.apache.maven.artifact.Artifact artifact =
                artifactFactory.createDependencyArtifact( groupId, artifactId,
                                                          VersionRange.createFromVersion( version ), "pom", null,
                                                          "compile" );
            final SnapshotArtifactRepositoryMetadata artifactRepositoryMetadata =
                new SnapshotArtifactRepositoryMetadata( artifact );
            try
            {
                repositoryMetadataManager.resolve( artifactRepositoryMetadata, remoteRepositories, localRepository );

                final Metadata artifactMetadata = artifactRepositoryMetadata.getMetadata();
                if ( artifactMetadata.getVersioning() != null
                    && artifactMetadata.getVersioning().getSnapshot() != null )
                {
                    foundSomething = true;
                    metadata.setGroupId( groupId );
                    metadata.setArtifactId( artifactId );
                    metadata.setVersion( version );
                    metadata.merge( artifactMetadata );
                }
                try
                {
                    if ( artifactMetadata.getVersioning() != null
                        && !artifactMetadata.getVersioning().getSnapshotVersions().isEmpty() )
                    {
                        // TODO up to and including Maven 3.0.3 we do not get a populated SnapshotVersions
                        for ( Iterator i = artifactMetadata.getVersioning().getSnapshotVersions().iterator();
                              i.hasNext(); )
                        {
                            SnapshotVersion v = (SnapshotVersion) i.next();
                            metadata.getVersioning().addSnapshotVersion( v );
                            if ( v.getVersion().endsWith( "-SNAPSHOT" ) )
                            {
                                addResolved(
                                    new Artifact( groupId, artifactId, version, v.getClassifier(), v.getExtension() ) );
                            }
                        }
                    }
                }
                catch ( NoSuchMethodError e )
                {
                    // ignore Maven 2.x doesn't give us the info
                }
            }
            catch ( RepositoryMetadataResolutionException e )
            {
                log.debug( e );
            }
        }

        // is this path a groupId:artifactId pair?
        artifactId = index == -1 ? null : path.substring( index + 1 );
        groupId = index == -1 ? null : path.substring( 0, index ).replace( '/', '.' );
        if ( !StringUtils.isEmpty( artifactId ) && !StringUtils.isEmpty( groupId ) )
        {
            final org.apache.maven.artifact.Artifact artifact =
                artifactFactory.createDependencyArtifact( groupId, artifactId, anyVersion, "pom", null, "compile" );
            final ArtifactRepositoryMetadata artifactRepositoryMetadata = new ArtifactRepositoryMetadata( artifact );
            try
            {
                repositoryMetadataManager.resolve( artifactRepositoryMetadata, remoteRepositories, localRepository );

                final Metadata artifactMetadata = artifactRepositoryMetadata.getMetadata();
                if ( artifactMetadata.getVersioning() != null )
                {
                    foundSomething = true;
                    if ( StringUtils.isEmpty( metadata.getGroupId() ) )
                    {
                        metadata.setGroupId( groupId );
                        metadata.setArtifactId( artifactId );
                    }
                    metadata.merge( artifactMetadata );
                    for ( Iterator i = artifactMetadata.getVersioning().getVersions().iterator(); i.hasNext(); )
                    {
                        addResolved( path + "/" + (String) i.next() );
                    }
                }
            }
            catch ( RepositoryMetadataResolutionException e )
            {
                log.debug( e );
            }
        }

        // if this path a groupId on its own?
        groupId = path.replace( '/', '.' );
        final GroupRepositoryMetadata groupRepositoryMetadata = new GroupRepositoryMetadata( groupId );
        try
        {
            repositoryMetadataManager.resolve( groupRepositoryMetadata, remotePluginRepositories, localRepository );
            foundSomething = true;
            metadata.merge( groupRepositoryMetadata.getMetadata() );
            for ( Iterator i = groupRepositoryMetadata.getMetadata().getPlugins().iterator(); i.hasNext(); )
            {
                Plugin plugin = (Plugin) i.next();
                addResolved( path + "/" + plugin.getArtifactId() );
            }
        }
        catch ( RepositoryMetadataResolutionException e )
        {
            log.debug( e );
        }

        if ( !foundSomething )
        {
            throw new MetadataNotFoundException( path );
        }
        addResolved( path );
        return metadata;
    }

    public long getMetadataLastModified( String path )
        throws IOException, MetadataNotFoundException
    {
        Metadata metadata = getMetadata( path );
        if ( metadata != null )
        {
            if ( !StringUtils.isEmpty( metadata.getGroupId() ) || !StringUtils.isEmpty( metadata.getArtifactId() )
                || !StringUtils.isEmpty( metadata.getVersion() )
                || ( metadata.getPlugins() != null && !metadata.getPlugins().isEmpty() ) || (
                metadata.getVersioning() != null && ( !StringUtils.isEmpty( metadata.getVersioning().getLastUpdated() )
                    || !StringUtils.isEmpty( metadata.getVersioning().getLatest() )
                    || !StringUtils.isEmpty( metadata.getVersioning().getRelease() )
                    || ( metadata.getVersioning().getVersions() != null
                    && !metadata.getVersioning().getVersions().isEmpty() ) || ( metadata.getVersioning().getSnapshot()
                    != null ) ) ) )
            {
                return System.currentTimeMillis();
            }
        }
        throw new MetadataNotFoundException( path );
    }
}
