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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.apache.maven.artifact.repository.metadata.Metadata;
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
public class DiskArtifactStore
    extends BaseArtifactStore
{
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
    public DiskArtifactStore( File root )
    {
        this.root = root;
    }

    public DiskArtifactStore canWrite( boolean canWrite )
    {
        this.canWrite = canWrite;
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    public Set<String> getGroupIds( String parentGroupId )
    {
        File parentDir =
            StringUtils.isEmpty( parentGroupId ) ? root : new File( root, parentGroupId.replace( '.', '/' ) );
        if ( !parentDir.isDirectory() )
        {
            return Collections.emptySet();
        }
        File[] groupDirs = parentDir.listFiles();
        Set<String> result = new HashSet<String>();
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
    public Set<String> getArtifactIds( String groupId )
    {
        File groupDir = new File( root, groupId.replace( '.', '/' ) );
        if ( !groupDir.isDirectory() )
        {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<String>();
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
    public Set<String> getVersions( String groupId, String artifactId )
    {
        File groupDir = new File( root, groupId.replace( '.', '/' ) );
        File artifactDir = new File( groupDir, artifactId );
        if ( !artifactDir.isDirectory() )
        {
            return Collections.emptySet();
        }
        File[] dirs = artifactDir.listFiles();
        Set<String> result = new HashSet<String>();
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
    public Set<Artifact> getArtifacts( final String groupId, final String artifactId, final String version )
    {
        File groupDir = new File( root, groupId.replace( '.', '/' ) );
        File artifactDir = new File( groupDir, artifactId );
        File versionDir = new File( artifactDir, version );
        if ( !versionDir.isDirectory() )
        {
            return Collections.emptySet();
        }
        final Pattern rule;

        abstract class ArtifactFactory
        {
            abstract Artifact get( File file );
        }

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
        Set<Artifact> result = new HashSet<Artifact>( files.length );
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
        File file = getFile( artifact );
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
        File file = getFile( artifact );
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
        File file = getFile( artifact );
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
        if ( !canWrite )
        {
            throw new UnsupportedOperationException( "Read-only store" );
        }
        
        File targetFile = getFile( artifact );
        
        if( !targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs() )
        {
            throw new IOException( "Failed to create " + targetFile.getParentFile().getPath() );
        }

        OutputStream output = null;
        try {
            output = new FileOutputStream( targetFile );
            IOUtils.copy( content, output );
        }
        finally {
            IOUtils.closeQuietly( content );
            IOUtils.closeQuietly( output );
        }
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
    
    @Override
    public void setMetadata( String path, Metadata metadata )
        throws IOException
    {
        if ( !canWrite )
        {
            throw new UnsupportedOperationException( "Read-only store" );
        }
        
        File file = root;
        String[] parts = StringUtils.strip( path, "/" ).split( "/" );
        for ( int i = 0; i < parts.length; i++ )
        {
            file = new File( file, parts[i] );
        }
        
        file = new File( file, "maven-metadata.xml" );

        MetadataXpp3Writer writer = new MetadataXpp3Writer();
        OutputStream outputStream = null;
        try
        {
            outputStream = new FileOutputStream( file );
            writer.write( outputStream, metadata );
        }
        finally {
            IOUtils.closeQuietly( outputStream );
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
        Stack<File> stack = new Stack<File>();
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
    
    public ArchetypeCatalog getArchetypeCatalog()
        throws IOException, ArchetypeCatalogNotFoundException
    {
        File file = new File( root, "archetype-catalog.xml" );
        if ( !file.isFile() )
        {
            throw new ArchetypeCatalogNotFoundException();
        }
        ArchetypeCatalogXpp3Reader reader = new ArchetypeCatalogXpp3Reader();
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

    public long getArchetypeCatalogLastModified()
        throws IOException, ArchetypeCatalogNotFoundException
    {
        File file = new File( root, "archetype-catalog.xml" );
        if ( !file.isFile() )
        {
            throw new ArchetypeCatalogNotFoundException();
        }
        return file.lastModified();
    }
    
    private File getFile( Artifact artifact )
    {
        File groupDir = new File( root, artifact.getGroupId().replace( '.', '/' ) );
        File artifactDir = new File( groupDir, artifact.getArtifactId() );
        File versionDir = new File( artifactDir, artifact.getVersion() );
        File targetFile = new File( versionDir, artifact.getName() );
        return targetFile;
    }
}
