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
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.BaseArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiskArtifactStore
    extends BaseArtifactStore
{
    private final File root;

    public DiskArtifactStore( File root )
    {
        this.root = root;
    }

    /**
     * {@inheritDoc}
     */
    public Set getGroupIds( String parentGroupId )
    {
        File parentDir =
            StringUtils.isEmpty( parentGroupId ) ? root : new File( root, parentGroupId.replace( '.', '/' ) );
        if ( !parentDir.isDirectory() )
        {
            return Collections.EMPTY_SET;
        }
        File[] groupDirs = parentDir.listFiles();
        Set result = new HashSet();
        for ( int i = 0; i < groupDirs.length; i++ )
        {
            if ( groupDirs[i].isDirectory() )
            {
                result.add( groupDirs[i].getName() );
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Set getArtifactIds( String groupId )
    {
        File groupDir = new File( root, groupId.replace( '.', '/' ) );
        if ( !groupDir.isDirectory() )
        {
            return Collections.EMPTY_SET;
        }
        Set result = new HashSet();
        File[] artifactDirs = groupDir.listFiles();
        for ( int i = 0; i < artifactDirs.length; i++ )
        {
            if ( !artifactDirs[i].isDirectory() )
            {
                continue;
            }
            final String artifactId = artifactDirs[i].getName();
            result.add( artifactId );
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Set getVersions( String groupId, String artifactId )
    {
        File groupDir = new File( root, groupId.replace( '.', '/' ) );
        File artifactDir = new File( groupDir, artifactId );
        if ( !artifactDir.isDirectory() )
        {
            return Collections.EMPTY_SET;
        }
        File[] dirs = artifactDir.listFiles();
        Set result = new HashSet();
        for ( int i = 0; i < dirs.length; i++ )
        {
            if ( !dirs[i].isDirectory() )
            {
                continue;
            }
            result.add( dirs[i].getName() );
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Set getArtifacts( final String groupId, final String artifactId, final String version )
    {
        File groupDir = new File( root, groupId.replace( '.', '/' ) );
        File artifactDir = new File( groupDir, artifactId );
        File versionDir = new File( artifactDir, version );
        if ( !versionDir.isDirectory() )
        {
            return Collections.EMPTY_SET;
        }
        final Pattern rule;
        final ArtifactFactory factory;
        if ( version.endsWith( "-SNAPSHOT" ) )
        {
            rule = Pattern.compile( "\\Q" + artifactId + "\\E-(?:\\Q" + StringUtils.removeEnd( version, "-SNAPSHOT" )
                                        + "\\E-(SNAPSHOT|(\\d{4})(\\d{2})(\\d{2})\\.(\\d{2})(\\d{2})(\\d{2})-(\\d+)))(?:-([^.]+))?\\.([^/]*)" );
            factory = new ArtifactFactory()
            {
                public Artifact get( File file )
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
                public Artifact get( File file )
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
        File[] files = versionDir.listFiles();
        Set result = new HashSet( files.length );
        for ( int i = 0; i < files.length; i++ )
        {
            if ( !files[i].isFile() || !rule.matcher( files[i].getName() ).matches() )
            {
                continue;
            }
            Artifact artifact = factory.get( files[i] );
            if ( artifact != null )
            {
                result.add( artifact );
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
        File groupDir = new File( root, artifact.getGroupId().replace( '.', '/' ) );
        File artifactDir = new File( groupDir, artifact.getArtifactId() );
        File versionDir = new File( artifactDir, artifact.getVersion() );
        File file = new File( versionDir, artifact.getName() );
        if ( !file.isFile() )
        {
            throw new ArtifactNotFoundException( artifact );
        }
        return file.lastModified();
    }

    /**
     * {@inheritDoc}
     */
    public long getSize( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        File groupDir = new File( root, artifact.getGroupId().replace( '.', '/' ) );
        File artifactDir = new File( groupDir, artifact.getArtifactId() );
        File versionDir = new File( artifactDir, artifact.getVersion() );
        File file = new File( versionDir, artifact.getName() );
        if ( !file.isFile() )
        {
            throw new ArtifactNotFoundException( artifact );
        }
        return file.length();
    }

    /**
     * {@inheritDoc}
     */
    public InputStream get( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        File groupDir = new File( root, artifact.getGroupId().replace( '.', '/' ) );
        File artifactDir = new File( groupDir, artifact.getArtifactId() );
        File versionDir = new File( artifactDir, artifact.getVersion() );
        File file = new File( versionDir, artifact.getName() );
        if ( !file.isFile() )
        {
            throw new ArtifactNotFoundException( artifact );
        }
        return new FileInputStream( file );
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
        File file = root;
        String[] parts = StringUtils.strip( path, "/" ).split( "/" );
        for ( int i = 0; i < parts.length; i++ )
        {
            file = new File( file, parts[i] );
        }
        file = new File( file, "maven-metadata.xml" );
        if ( !file.isFile() )
        {
            throw new MetadataNotFoundException( path );
        }
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        InputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream( file );
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

    /**
     * {@inheritDoc}
     */
    public long getMetadataLastModified( String path )
        throws IOException, MetadataNotFoundException
    {
        File file = root;
        String[] parts = StringUtils.strip( path, "/" ).split( "/" );
        Stack stack = new Stack();
        for ( int i = 0; i < parts.length; i++ )
        {
            if ( "..".equals( parts[i] ) )
            {
                if ( !stack.isEmpty() )
                {
                    file = (File) stack.pop();
                }
                else
                {
                    file = root;
                }
            }
            else if ( !".".equals( parts[i] ) )
            {
                file = new File( file, parts[i] );
                stack.push( file );
            }
        }
        file = new File( file, "maven-metadata.xml" );
        if ( !file.isFile() )
        {
            throw new MetadataNotFoundException( path );
        }
        return file.lastModified();
    }

    private interface ArtifactFactory
    {
        Artifact get( File file );
    }
}
