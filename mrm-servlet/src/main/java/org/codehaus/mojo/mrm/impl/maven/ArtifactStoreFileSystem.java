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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.mojo.mrm.api.BaseFileSystem;
import org.codehaus.mojo.mrm.api.DefaultDirectoryEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.Entry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * A {@link org.codehaus.mojo.mrm.api.FileSystem} that delegates to a {@link ArtifactStore}.
 *
 * @see FileSystemArtifactStore for the oposite.
 * @since 1.0
 */
public class ArtifactStoreFileSystem
    extends BaseFileSystem
{

    /**
     * Regex to match the groupId portion of a path.
     *
     * @since 1.0
     */
    private static final String GROUP_ID_PATH_REGEX = "/((?:[^/]+/)+)";

    /**
     * Regex to match the artifactId portion of a path.
     *
     * @since 1.0
     */
    private static final String ARTIFACT_ID_PATH_REGEX = "([^/]+)/";

    /**
     * Regex to match the version portion of a path.
     *
     * @since 1.0
     */
    private static final String VERSION_REGEX = "([^/]+)/";

    /**
     * Regex to match the version portion of a path if the version is a <code>-SNAPSHOT</code>.
     *
     * @since 1.0
     */
    private static final String SNAPSHOT_VERSION_REGEX = "([^/]+)-SNAPSHOT/";

    /**
     * Regex to match a metadata path.
     *
     * @since 1.0
     */
    /*package*/ static final Pattern METADATA = Pattern.compile( GROUP_ID_PATH_REGEX + "(maven-metadata\\.xml)" );

    /*package*/ static final Pattern ARCHETYPE_CATALOG = Pattern.compile( "/archetype-catalog\\.xml" ); 
    
    /**
     * Regex to match a release artifact path.
     *
     * @since 1.0
     */
    /*package*/ static final Pattern ARTIFACT = Pattern.compile(
        GROUP_ID_PATH_REGEX + ARTIFACT_ID_PATH_REGEX + VERSION_REGEX + "(\\2-\\3(-[^.]+)?\\.([^/]*))" );

    /**
     * Regex to match a snapshot artifact path.
     *
     * @since 1.0
     */
    /*package*/ static final Pattern SNAPSHOT_ARTIFACT = Pattern.compile(
        GROUP_ID_PATH_REGEX + ARTIFACT_ID_PATH_REGEX + SNAPSHOT_VERSION_REGEX
            + "(\\2-(?:\\3-(?:SNAPSHOT|\\d{8}\\.\\d{6}-\\d+))(-[^.]+)?\\.([^/]*))" );

    /**
     * The backing {@link ArtifactStore}.
     *
     * @since 1.0
     */
    private final ArtifactStore store;

    /**
     * Creates a {@link org.codehaus.mojo.mrm.api.FileSystem} backed by an {@link ArtifactStore}.
     *
     * @param store the backing artifact store.
     * @since 1.0
     */
    public ArtifactStoreFileSystem( ArtifactStore store )
    {
        this.store = store;
    }

    /**
     * {@inheritDoc}
     */
    public Entry[] listEntries( DirectoryEntry directory )
    {
        if ( getRoot().equals( directory ) )
        {
            Set<String> rootGroupIds = new TreeSet<String>( store.getGroupIds( "" ) );
            Entry[] result = new Entry[rootGroupIds.size()];
            int index = 0;
            for ( String name : rootGroupIds )
            {
                result[index++] = new DefaultDirectoryEntry( this, getRoot(), name );
            }
            return result;
        }
        List<Entry> result = new ArrayList<Entry>();
        Set<String> names = new HashSet<String>();
        String path = directory.toPath();

        try
        {
            store.getMetadataLastModified( path );
            MetadataFileEntry entry = new MetadataFileEntry( this, directory, path, store );
            if ( !names.contains( entry.getName() ) )
            {
                result.add( entry );
                names.add( entry.getName() );
            }
        }
        catch ( MetadataNotFoundException e )
        {
            // ignore
        }
        catch ( IOException e )
        {
            // ignore
        }

        String groupId = path.replace( '/', '.' );

        // get all the groupId's that start with this groupId
        String groupIdPrefix = groupId + ".";
        Set<String> groupIds = new TreeSet<String>( store.getGroupIds( groupId ) );
        for ( String name : groupIds )
        {
            if ( !names.contains( name ) )
            {
                result.add( new DefaultDirectoryEntry( this, directory, name ) );
                names.add( name );
            }
        }

        // get all the artifactIds that belong to this groupId
        for ( String name : store.getArtifactIds( groupId ) )
        {
            if ( !names.contains( name ) )
            {
                result.add( new DefaultDirectoryEntry( this, directory, name ) );
                names.add( name );
            }
        }

        DirectoryEntry parent = directory.getParent();
        if ( parent != null && !getRoot().equals( parent ) )
        {
            // get all the versions that belong to the groupId/artifactId path
            groupId = parent.toPath().replace( '/', '.' );
            String artifactId = directory.getName();
            for ( String name : store.getVersions( groupId, artifactId ) )
            {
                if ( !names.contains( name ) )
                {
                    result.add( new DefaultDirectoryEntry( this, directory, name ) );
                    names.add( name );
                }
            }
            DirectoryEntry grandParent = parent.getParent();
            if ( grandParent != null && !getRoot().equals( grandParent ) )
            {
                // get all the versions that belong to the groupId/artifactId path
                groupId = grandParent.toPath().replace( '/', '.' );
                artifactId = parent.getName();
                String version = directory.getName();
                for ( Artifact a : store.getArtifacts( groupId, artifactId, version ) )
                {
                    ArtifactFileEntry entry = new ArtifactFileEntry( this, directory, a, store );
                    if ( !names.contains( entry.getName() ) )
                    {
                        result.add( entry );
                        names.add( entry.getName() );
                    }
                }
            }
        }

        // sort
        Collections.sort( result, new Comparator<Entry>()
        {
            public int compare( Entry o1, Entry o2 )
            {
                return ( o1 ).getName().compareTo( ( o2 ).getName() );
            }
        } );
        return result.toArray( new Entry[result.size()] );
    }

    /**
     * {@inheritDoc}
     */
    protected Entry get( DirectoryEntry parent, String name )
    {
        String path = "/";
        if ( StringUtils.isNotEmpty( parent.toPath()  ) )
        {
            path += parent.toPath() + "/";
        }
        path += name;

        if ( "favicon.ico".equals( name ) )
        {
            return null;
        }
        if ( METADATA.matcher( path ).matches() )
        {
            MetadataFileEntry entry = new MetadataFileEntry( this, parent, parent.toPath(), store );
            try
            {
                entry.getLastModified();
                return entry;
            }
            catch ( IOException e )
            {
                return null;
            }
        }
        else if ( ARCHETYPE_CATALOG.matcher( path ).matches() )
        {
            ArchetypeCatalogFileEntry entry = new ArchetypeCatalogFileEntry( this, parent, store );
            try
            {
                entry.getLastModified();
                return entry;
            }
            catch ( IOException e )
            {
                return null;
            }
        }
        else
        {
            Matcher snapshotArtifact = SNAPSHOT_ARTIFACT.matcher( path );
            if ( snapshotArtifact.matches() )
            {
                String groupId = StringUtils.stripEnd( snapshotArtifact.group( 1 ), "/" ).replace( '/', '.' );
                String artifactId = snapshotArtifact.group( 2 );
                String version = snapshotArtifact.group( 3 ) + "-SNAPSHOT";
                Pattern rule = Pattern.compile(
                    "\\Q" + artifactId + "\\E-(?:\\Q" + StringUtils.removeEnd( version, "-SNAPSHOT" )
                        + "\\E-(SNAPSHOT|(\\d{4})(\\d{2})(\\d{2})\\.(\\d{2})(\\d{2})(\\d{2})-(\\d+)))(?:-([^.]+))?\\.([^/]*)" );
                Matcher matcher = rule.matcher( name );
                if ( !matcher.matches() )
                {
                    String classifier = snapshotArtifact.group( 5 );
                    String type = snapshotArtifact.group( 6 );
                    if ( classifier != null )
                    {
                        classifier = classifier.substring( 1 );
                    }
                    if ( StringUtils.isEmpty( classifier ) )
                    {
                        classifier = null;
                    }
                    return new ArtifactFileEntry( this, parent,
                                                  new Artifact( groupId, artifactId, version, classifier, type ),
                                                  store );
                }
                if ( matcher.group( 1 ).equals( "SNAPSHOT" ) )
                {
                    Artifact artifact = new Artifact( groupId, artifactId, version, matcher.group( 9 ),
                                                      matcher.group( 10 ) );
                    try
                    {
                        store.get( artifact );
                        return new ArtifactFileEntry( this, parent, artifact, store );
                    }
                    catch ( IOException e )
                    {
                        return null;
                    }
                    catch ( ArtifactNotFoundException e )
                    {
                        return null;
                    }
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
                    
                    Artifact artifact = new Artifact( groupId, artifactId, version, matcher.group( 9 ),
                                                      matcher.group( 10 ), timestamp, buildNumber );
                    try
                    {
                        store.get( artifact );
                        return new ArtifactFileEntry( this, parent, artifact, store );
                    }
                    catch ( IOException e )
                    {
                        return null;
                    }
                    catch ( ArtifactNotFoundException e )
                    {
                        return null;
                    }
                }
                catch ( NullPointerException e )
                {
                    return new DefaultDirectoryEntry( this, parent, name );
                }
            }
            else
            {
                Matcher matcher = ARTIFACT.matcher( path );
                if ( matcher.matches() )
                {
                    String groupId = StringUtils.stripEnd( matcher.group( 1 ), "/" ).replace( '/', '.' );
                    String artifactId = matcher.group( 2 );
                    String version = matcher.group( 3 );
                    String classifier = matcher.group( 5 );
                    String type = matcher.group( 6 );
                    if ( classifier != null )
                    {
                        classifier = classifier.substring( 1 );
                    }
                    if ( StringUtils.isEmpty( classifier ) )
                    {
                        classifier = null;
                    }
                    
                    Artifact artifact = new Artifact( groupId, artifactId, version, classifier, type );
                    try
                    {
                        store.get( artifact );
                        return new ArtifactFileEntry( this, parent, artifact, store );
                    }
                    catch ( ArtifactNotFoundException e )
                    {
                        return null;
                    }
                    catch ( IOException e )
                    {
                        return null;
                    }
                }
                else
                {
                    return new DefaultDirectoryEntry( this, parent, name );
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified( DirectoryEntry entry )
        throws IOException
    {
        return System.currentTimeMillis();
    }
    
    @Override
    public FileEntry put( DirectoryEntry parent, String name, InputStream content )
        throws IOException
    {
        String path = "/";
        if ( StringUtils.isNotEmpty( parent.toPath()  ) )
        {
            path += parent.toPath() + "/";
        }

        if ( "maven-metadata.xml".equals( name ) )
        {
            MetadataXpp3Reader reader = new MetadataXpp3Reader();
            try
            {
                Metadata metadata = reader.read( content );
                
                if ( metadata == null )
                {
                    return null;
                }
                
                store.setMetadata( path, metadata );
            }
            catch ( XmlPullParserException e1 )
            {
                throw new IOException();
            }
            
            return new MetadataFileEntry( this, parent, path, store );
        }
        
        Artifact artifact = getArtifact( parent, name );
        
        if ( artifact == null )
        {
            return null;
        }
        
        store.set( artifact , content );
        
        return new ArtifactFileEntry( this, parent, artifact, store );
    }

    private Artifact getArtifact( DirectoryEntry parent, String name )
    {
        String path = "/";
        if ( StringUtils.isNotEmpty( parent.toPath()  ) )
        {
            path += parent.toPath() + "/";
        }
        path += name;
        
        Matcher snapshotArtifact = SNAPSHOT_ARTIFACT.matcher( path );
        if ( snapshotArtifact.matches() )
        {
            String groupId = StringUtils.stripEnd( snapshotArtifact.group( 1 ), "/" ).replace( '/', '.' );
            String artifactId = snapshotArtifact.group( 2 );
            String version = snapshotArtifact.group( 3 ) + "-SNAPSHOT";
            Pattern rule = Pattern.compile(
                "\\Q" + artifactId + "\\E-(?:\\Q" + StringUtils.removeEnd( version, "-SNAPSHOT" )
                    + "\\E-(SNAPSHOT|(\\d{4})(\\d{2})(\\d{2})\\.(\\d{2})(\\d{2})(\\d{2})-(\\d+)))(?:-([^.]+))?\\.([^/]*)" );
            Matcher matcher = rule.matcher( name );
            if ( !matcher.matches() )
            {
                String classifier = snapshotArtifact.group( 5 );
                String type = snapshotArtifact.group( 6 );
                if ( classifier != null )
                {
                    classifier = classifier.substring( 1 );
                }
                if ( StringUtils.isEmpty( classifier ) )
                {
                    classifier = null;
                }
                return new Artifact( groupId, artifactId, version, classifier, type );
            }
            if ( matcher.group( 1 ).equals( "SNAPSHOT" ) )
            {
                return new Artifact( groupId, artifactId, version, matcher.group( 9 ),
                                                  matcher.group( 10 ) );
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
                
                return new Artifact( groupId, artifactId, version, matcher.group( 9 ),
                                                  matcher.group( 10 ), timestamp, buildNumber );
            }
            catch ( NullPointerException e )
            {
                return null;
            }
        }
        else
        {
            Matcher matcher = ARTIFACT.matcher( path );
            if ( matcher.matches() )
            {
                String groupId = StringUtils.stripEnd( matcher.group( 1 ), "/" ).replace( '/', '.' );
                String artifactId = matcher.group( 2 );
                String version = matcher.group( 3 );
                String classifier = matcher.group( 5 );
                String type = matcher.group( 6 );
                if ( classifier != null )
                {
                    classifier = classifier.substring( 1 );
                }
                if ( StringUtils.isEmpty( classifier ) )
                {
                    classifier = null;
                }
                
                return new Artifact( groupId, artifactId, version, classifier, type );
            }
            else
            {
                return null;
            }
        }
    }
}
