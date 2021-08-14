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
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.Entry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.FileSystem;
import org.codehaus.mojo.mrm.api.maven.ArchetypeCatalogNotFoundException;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.BaseArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An artifact store based off a {@link FileSystem}.
 *
 * @see ArtifactStoreFileSystem for the oposite.
 * @since 1.0
 */
public class FileSystemArtifactStore
    extends BaseArtifactStore
{
    /**
     * The backing file system.
     *
     * @since 1.0
     */
    private final FileSystem backing;

    /**
     * Creates a new artifact store hosted at the supplied root directory.
     *
     * @param backing the backing file system.
     * @since 1.0
     */
    public FileSystemArtifactStore( FileSystem backing )
    {
        this.backing = backing;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getGroupIds( String parentGroupId )
    {
        Entry parentEntry =
            StringUtils.isEmpty( parentGroupId ) ? backing.getRoot() : backing.get( parentGroupId.replace( '.', '/' ) );
        if ( !( parentEntry instanceof DirectoryEntry ) )
        {
            return Collections.emptySet();
        }
        DirectoryEntry parentDir = (DirectoryEntry) parentEntry;
        Entry[] entries = backing.listEntries( parentDir );
        return Arrays.stream(entries).filter(entry -> entry instanceof DirectoryEntry)
                .map(Entry::getName)
                .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getArtifactIds( String groupId )
    {
        Entry parentEntry = backing.get( groupId.replace( '.', '/' ) );
        if ( !( parentEntry instanceof DirectoryEntry ) )
        {
            return Collections.emptySet();
        }
        DirectoryEntry parentDir = (DirectoryEntry) parentEntry;
        Entry[] entries = backing.listEntries( parentDir );
        return Arrays.stream(entries).filter(entry -> entry instanceof DirectoryEntry)
                .map(Entry::getName)
                .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getVersions( String groupId, String artifactId )
    {
        Entry parentEntry = backing.get( groupId.replace( '.', '/' ) + "/" + artifactId );
        if ( !( parentEntry instanceof DirectoryEntry ) )
        {
            return Collections.emptySet();
        }
        DirectoryEntry parentDir = (DirectoryEntry) parentEntry;
        Entry[] entries = backing.listEntries( parentDir );
        return Arrays.stream(entries).filter(entry -> entry instanceof DirectoryEntry)
                .map(Entry::getName)
                .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    public Set<Artifact> getArtifacts( final String groupId, final String artifactId, final String version )
    {
        Entry parentEntry = backing.get( groupId.replace( '.', '/' ) + "/" + artifactId + "/" + version );
        if ( !( parentEntry instanceof DirectoryEntry ) )
        {
            return Collections.emptySet();
        }
        DirectoryEntry parentDir = (DirectoryEntry) parentEntry;
        Entry[] entries = backing.listEntries( parentDir );
        final Pattern rule;

        abstract class ArtifactFactory
        {
            abstract Artifact get( Entry entry );
        }

        final ArtifactFactory factory;
        if ( version.endsWith( "-SNAPSHOT" ) )
        {
            rule = Pattern.compile( "\\Q" + artifactId + "\\E-(?:\\Q" + StringUtils.removeEnd( version, "-SNAPSHOT" )
                                        + "\\E-(SNAPSHOT|(\\d{4})(\\d{2})(\\d{2})\\.(\\d{2})(\\d{2})(\\d{2})-(\\d+)))(?:-([^.]+))?\\.([^/]*)" );
            factory = new ArtifactFactory()
            {
                public Artifact get( Entry entry )
                {
                    Matcher matcher = rule.matcher( entry.getName() );
                    if ( !matcher.matches() )
                    {
                        return null;
                    }
                    if ( matcher.group( 1 ).equals( "SNAPSHOT" ) )
                    {
                        return new Artifact( groupId, artifactId, version, matcher.group( 9 ), matcher.group( 10 ) );
                    }
                    try
                    {
                        Calendar cal = new GregorianCalendar();
                        cal.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
                        cal.set( Calendar.YEAR, Integer.parseInt( matcher.group( 2 ) ) );
                        cal.set( Calendar.MONTH, Integer.parseInt( matcher.group( 3 ) ) - 1 );
                        cal.set( Calendar.DAY_OF_MONTH, Integer.parseInt( matcher.group( 4 ) ) );
                        cal.set( Calendar.HOUR_OF_DAY, Integer.parseInt( matcher.group( 5 ) ) );
                        cal.set( Calendar.MINUTE, Integer.parseInt( matcher.group( 6 ) ) );
                        cal.set( Calendar.SECOND, Integer.parseInt( matcher.group( 7 ) ) );
                        long timestamp = cal.getTimeInMillis();
                        int buildNumber = Integer.parseInt( matcher.group( 8 ) );
                        return new Artifact( groupId, artifactId, version, matcher.group( 9 ), matcher.group( 10 ),
                                             timestamp, buildNumber );
                    }
                    catch ( NullPointerException e )
                    {
                        return null;
                    }
                }
            };
        }
        else
        {
            rule = Pattern.compile( "\\Q" + artifactId + "\\E-\\Q" + version + "\\E(?:-([^.]+))?\\.(.+)" );
            factory = new ArtifactFactory()
            {
                public Artifact get( Entry entry )
                {
                    Matcher matcher = rule.matcher( entry.getName() );
                    if ( !matcher.matches() )
                    {
                        return null;
                    }
                    return new Artifact( groupId, artifactId, version, matcher.group( 1 ), matcher.group( 2 ) );
                }
            };
        }
        Set<Artifact> result = new HashSet<>( entries.length );
        for (Entry entry : entries) {
            if (!(entry instanceof FileEntry) || !rule.matcher(entry.getName()).matches()) {
                continue;
            }
            Artifact artifact = factory.get(entry);
            if (artifact != null) {
                result.add(artifact);
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
        Entry entry = backing.get(
            artifact.getGroupId().replace( '.', '/' ) + "/" + artifact.getArtifactId() + "/" + artifact.getVersion()
                + "/" + artifact.getName() );
        if ( !( entry instanceof FileEntry ) )
        {
            throw new ArtifactNotFoundException( artifact );
        }
        return entry.getLastModified();
    }

    /**
     * {@inheritDoc}
     */
    public long getSize( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        Entry entry = backing.get(
            artifact.getGroupId().replace( '.', '/' ) + "/" + artifact.getArtifactId() + "/" + artifact.getVersion()
                + "/" + artifact.getName() );
        if ( !( entry instanceof FileEntry ) )
        {
            throw new ArtifactNotFoundException( artifact );
        }
        return ( (FileEntry) entry ).getSize();
    }

    /**
     * {@inheritDoc}
     */
    public InputStream get( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        Entry entry = backing.get(
            artifact.getGroupId().replace( '.', '/' ) + "/" + artifact.getArtifactId() + "/" + artifact.getVersion()
                + "/" + artifact.getName() );
        if ( !( entry instanceof FileEntry ) )
        {
            throw new ArtifactNotFoundException( artifact );
        }
        return ( (FileEntry) entry ).getInputStream();
    }

    /**
     * {@inheritDoc}
     */
    public void set( Artifact artifact, InputStream content )
        throws IOException
    {
        throw new UnsupportedOperationException( "Read-only store" );
    }

    /**
     * {@inheritDoc}
     */
    public Metadata getMetadata( String path )
        throws IOException, MetadataNotFoundException
    {
        Entry entry = backing.get(
            StringUtils.join( StringUtils.split( StringUtils.strip( path, "/" ), "/" ), "/" ) + "/maven-metadata.xml" );
        if ( !( entry instanceof FileEntry ) )
        {
            throw new MetadataNotFoundException( path );
        }

        try (InputStream inputStream = ( (FileEntry) entry ).getInputStream())
        {
            return new MetadataXpp3Reader().read( inputStream );
        }
        catch ( XmlPullParserException e )
        {
            throw new IOException( e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMetadataLastModified( String path )
        throws IOException, MetadataNotFoundException
    {
        Entry entry = backing.get(
            StringUtils.join( StringUtils.split( StringUtils.strip( path, "/" ), "/" ), "/" ) + "/maven-metadata.xml" );
        if ( !( entry instanceof FileEntry ) )
        {
            throw new MetadataNotFoundException( path );
        }
        return entry.getLastModified();
    }

    public ArchetypeCatalog getArchetypeCatalog()
        throws IOException, ArchetypeCatalogNotFoundException
    {
        Entry entry = backing.get( "archetype-catalog.xml" );
        if ( !( entry instanceof FileEntry ) )
        {
            throw new ArchetypeCatalogNotFoundException();
        }
        try (InputStream inputStream = ( (FileEntry) entry ).getInputStream())
        {

            return new ArchetypeCatalogXpp3Reader().read( inputStream );
        }
        catch ( XmlPullParserException e )
        {
            throw new IOException( e.getMessage(), e);
        }
    }
    
    public long getArchetypeCatalogLastModified()
        throws IOException, ArchetypeCatalogNotFoundException
    {
        Entry entry = backing.get( "archetype-catalog.xml" );
        if ( !( entry instanceof FileEntry ) )
        {
            throw new ArchetypeCatalogNotFoundException();
        }
        return entry.getLastModified();
    }
}
