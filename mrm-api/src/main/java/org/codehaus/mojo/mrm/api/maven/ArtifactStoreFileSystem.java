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

package org.codehaus.mojo.mrm.api.maven;

import org.apache.commons.lang.StringUtils;
import org.codehaus.mojo.mrm.api.BaseFileSystem;
import org.codehaus.mojo.mrm.api.DefaultDirectoryEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.Entry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtifactStoreFileSystem
    extends BaseFileSystem
{

    private final ArtifactStore store;

    private static final String GROUP_ID_PATH_REGEX = "/((?:[^/]+/)+)";

    private static final String ARTIFACT_ID_PATH_REGEX = "([^/]+)/";

    private static final String VERSION_REGEX = "([^/]+)/";

    private static final String SNAPSHOT_VERSION_REGEX = "([^/]+)-SNAPSHOT/";

    /*package*/ static final Pattern METADATA = Pattern.compile( GROUP_ID_PATH_REGEX + "(metadata\\.xml)" );

    /*package*/ static final Pattern ARTIFACT = Pattern.compile(
        GROUP_ID_PATH_REGEX + ARTIFACT_ID_PATH_REGEX + VERSION_REGEX + "(\\2-\\3(-[^.]+)?\\.([^/]*))" );

    /*package*/ static final Pattern SNAPSHOT_ARTIFACT = Pattern.compile(
        GROUP_ID_PATH_REGEX + ARTIFACT_ID_PATH_REGEX + SNAPSHOT_VERSION_REGEX
            + "(\\2-(?:\\3-(?:SNAPSHOT|\\d{8}\\.\\d{6}-\\d+))(-[^.]+)?\\.([^/]*))" );

    /*package*/ static final Pattern SNAPSHOT_TIMESTAMP_REGEX =
        Pattern.compile( "(.*(?:-SNAPSHOT|-\\d{8}\\.\\d{6}-\\d+))" );

    public ArtifactStoreFileSystem( ArtifactStore store )
    {
        this.store = store;
    }

    public Entry[] listEntries( DirectoryEntry directory )
    {
        if ( getRoot().equals( directory ) )
        {
            Set rootGroupIds = new TreeSet( store.getGroupIds( "" ) );
            Entry[] result = new Entry[rootGroupIds.size()];
            int index = 0;
            Iterator i = rootGroupIds.iterator();
            while ( i.hasNext() )
            {
                result[index++] = new DefaultDirectoryEntry( this, getRoot(), (String) i.next() );
            }
            return result;
        }
        List/*<Entry>*/ result = new ArrayList();
        Set names = new HashSet();
        String path = toPath( directory ).substring( 1 ); // skip initial '/'

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
        Set groupIds = new TreeSet( store.getGroupIds( groupId ) );
        for ( Iterator i = groupIds.iterator(); i.hasNext(); )
        {
            String name = (String) i.next();
            if ( !names.contains( name ) )
            {
                result.add( new DefaultDirectoryEntry( this, directory, name ) );
                names.add( name );
            }
        }

        // get all the artifactIds that belong to this groupId
        for ( Iterator i = store.getArtifactIds( groupId ).iterator(); i.hasNext(); )
        {
            String name = (String) i.next();
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
            groupId = toPath( parent ).substring( 1 ).replace( '/', '.' );
            String artifactId = directory.getName();
            for ( Iterator i = store.getVersions( groupId, artifactId ).iterator(); i.hasNext(); )
            {
                String name = (String) i.next();
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
                groupId = toPath( grandParent ).substring( 1 ).replace( '/', '.' );
                artifactId = parent.getName();
                String version = directory.getName();
                for ( Iterator i = store.getArtifacts( groupId, artifactId, version ).iterator(); i.hasNext(); )
                {
                    ArtifactFileEntry entry = new ArtifactFileEntry( this, directory, (Artifact) i.next(), store );
                    if ( !names.contains( entry.getName() ) )
                    {
                        result.add( entry );
                        names.add( entry.getName() );
                    }
                }
            }
        }

        // sort
        Collections.sort( result, new Comparator/*<Entry>*/()
        {
            public int compare( Object o1, Object o2 )
            {
                return ( (Entry) o1 ).getName().compareTo( ( (Entry) o2 ).getName() );
            }
        } );
        return (Entry[]) result.toArray( new Entry[result.size()] );
    }

    protected Entry get( DirectoryEntry parent, String name )
    {

        String path = toPath( parent ) + "/" + name;

        if ( "favicon.ico".equals( name ) )
        {
            return null;
        }
        if ( METADATA.matcher( path ).matches() )
        {
            return new MetadataFileEntry( this, parent, toPath( parent ), store );
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
                    return new ArtifactFileEntry( this, parent,
                                                  new Artifact( groupId, artifactId, version, matcher.group( 9 ),
                                                                matcher.group( 10 ) ), store );
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
                    return new ArtifactFileEntry( this, parent,
                                                  new Artifact( groupId, artifactId, version, matcher.group( 9 ),
                                                                matcher.group( 10 ), timestamp, buildNumber ), store );
                }
                catch ( NullPointerException e )
                {
                    return new DefaultDirectoryEntry( this, parent, name );
                }
            }
            else
            {
                Matcher artifact = ARTIFACT.matcher( path );
                if ( artifact.matches() )
                {
                    String groupId = StringUtils.stripEnd( artifact.group( 1 ), "/" ).replace( '/', '.' );
                    String artifactId = artifact.group( 2 );
                    String version = artifact.group( 3 );
                    String classifier = artifact.group( 5 );
                    String type = artifact.group( 6 );
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
                else
                {
                    return new DefaultDirectoryEntry( this, parent, name );
                }
            }
        }
    }

    public long getLastModified( DirectoryEntry entry )
        throws IOException
    {
        return System.currentTimeMillis();
    }

    private String toPath( Entry entry )
    {
        Stack stack = new Stack();
        Entry root = getRoot();
        while ( entry != null && !root.equals( entry ) )
        {
            stack.push( entry.getName() );
            entry = entry.getParent();
        }
        StringBuffer buf = new StringBuffer();
        while ( !stack.empty() )
        {
            buf.append( '/' );
            buf.append( stack.pop() );
        }
        return buf.toString();
    }

}
