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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.Entry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.FileSystem;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileSystemArtifactStore
    implements ArtifactStore
{
    private final FileSystem backing;

    public FileSystemArtifactStore( FileSystem backing )
    {
        this.backing = backing;
    }

    public Set getGroupIds( String parentGroupId )
    {
        Entry parentEntry =
            StringUtils.isEmpty( parentGroupId ) ? backing.getRoot() : backing.get( parentGroupId.replace( '.', '/' ) );
        if ( !( parentEntry instanceof DirectoryEntry ) )
        {
            return Collections.EMPTY_SET;
        }
        DirectoryEntry parentDir = (DirectoryEntry) parentEntry;
        Entry[] entries = backing.listEntries( parentDir );
        Set result = new HashSet();
        for ( int i = 0; i < entries.length; i++ )
        {
            if ( entries[i] instanceof DirectoryEntry )
            {
                result.add( entries[i].getName() );
            }
        }
        return result;
    }

    public Set getArtifactIds( String groupId )
    {
        Entry parentEntry = backing.get( groupId.replace( '.', '/' ) );
        if ( !( parentEntry instanceof DirectoryEntry ) )
        {
            return Collections.EMPTY_SET;
        }
        DirectoryEntry parentDir = (DirectoryEntry) parentEntry;
        Entry[] entries = backing.listEntries( parentDir );
        Set result = new HashSet();
        for ( int i = 0; i < entries.length; i++ )
        {
            if ( entries[i] instanceof DirectoryEntry )
            {
                result.add( entries[i].getName() );
            }
        }
        return result;
    }

    public Set getVersions( String groupId, String artifactId )
    {
        Entry parentEntry = backing.get( groupId.replace( '.', '/' ) + "/" + artifactId );
        if ( !( parentEntry instanceof DirectoryEntry ) )
        {
            return Collections.EMPTY_SET;
        }
        DirectoryEntry parentDir = (DirectoryEntry) parentEntry;
        Entry[] entries = backing.listEntries( parentDir );
        Set result = new HashSet();
        for ( int i = 0; i < entries.length; i++ )
        {
            if ( entries[i] instanceof DirectoryEntry )
            {
                result.add( entries[i].getName() );
            }
        }
        return result;
    }

    public Set getArtifacts( final String groupId, final String artifactId, final String version )
    {
        Entry parentEntry = backing.get( groupId.replace( '.', '/' ) + "/" + artifactId + "/" + version );
        if ( !( parentEntry instanceof DirectoryEntry ) )
        {
            return Collections.EMPTY_SET;
        }
        DirectoryEntry parentDir = (DirectoryEntry) parentEntry;
        Entry[] entries = backing.listEntries( parentDir );
        final Pattern rule;
        final ArtifactFactory factory;
        if ( version.endsWith( "-SNAPSHOT" ) )
        {
            rule = Pattern.compile( "\\Q" + artifactId + "\\E-(?:\\Q" + StringUtils.removeEnd( version, "-SNAPSHOT" )
                                        + "\\E-(SNAPSHOT|(\\d{4})(\\d{2})(\\d{2})\\.(\\d{2})(\\d{2})(\\d{2})-(\\d+)))(?:-([^.]+))?\\.([^/]*)" );
            factory = new ArtifactFactory()
            {
                public Artifact get( Entry file )
                {
                    Matcher matcher = rule.matcher( file.getName() );
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
                public Artifact get( Entry file )
                {
                    Matcher matcher = rule.matcher( file.getName() );
                    if ( !matcher.matches() )
                    {
                        return null;
                    }
                    return new Artifact( groupId, artifactId, version, matcher.group( 1 ), matcher.group( 2 ) );
                }
            };
        }
        Set result = new HashSet( entries.length );
        for ( int i = 0; i < entries.length; i++ )
        {
            if ( !( entries[i] instanceof FileEntry ) || !rule.matcher( entries[i].getName() ).matches() )
            {
                continue;
            }
            Artifact artifact = factory.get( entries[i] );
            if ( artifact != null )
            {
                result.add( artifact );
            }
        }
        return result;
    }

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

    public void set( Artifact artifact, InputStream content )
        throws IOException
    {
        throw new UnsupportedOperationException( "Read-only store" );
    }

    public Metadata getMetadata( String path )
        throws IOException, MetadataNotFoundException
    {
        Entry entry = backing.get(
            StringUtils.join( StringUtils.split( StringUtils.strip( path, "/" ), "/" ), "/" ) + "/maven-metadata.xml" );
        if ( !( entry instanceof FileEntry ) )
        {
            throw new MetadataNotFoundException( path );
        }
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        InputStream inputStream = null;
        try
        {
            inputStream = ( (FileEntry) entry ).getInputStream();
            return reader.read( inputStream );
        }
        catch ( XmlPullParserException e )
        {
            IOException ioe = new IOException( e.getMessage() );
            ioe.initCause( e );
            throw ioe;
        }
        finally
        {
            IOUtils.closeQuietly( inputStream );
        }
    }

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

    private interface ArtifactFactory
    {
        Artifact get( Entry file );
    }
}
