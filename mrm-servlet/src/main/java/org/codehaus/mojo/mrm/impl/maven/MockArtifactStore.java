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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.codehaus.mojo.mrm.impl.Utils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

public class MockArtifactStore
    implements ArtifactStore
{

    public static final String[] POM_EXTENSIONS = { "pom" };

    private Map/*<String, Map<String, Map<String, Map<Artifact, byte[]>>>>*/ contents = new HashMap();

    public MockArtifactStore( File root )
    {
        this( null, root );
    }

    public MockArtifactStore( Log log, File root )
    {
        if ( root.isDirectory() )
        {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Collection/*<File>*/ poms = FileUtils.listFiles( root, POM_EXTENSIONS, true );
            for ( Iterator i = poms.iterator(); i.hasNext(); )
            {
                File file = (File) i.next();
                FileReader fileReader = null;
                try
                {
                    fileReader = new FileReader( file );
                    Model model = reader.read( fileReader );
                    set( new Artifact( model.getGroupId(), model.getArtifactId(), model.getVersion(), "pom" ),
                         new FileContent( file ) );
                    if ( StringUtils.isEmpty( model.getPackaging() ) || "jar".equals( model.getPackaging() ) )
                    {
                        set( new Artifact( model.getGroupId(), model.getArtifactId(), model.getVersion(), "jar" ),
                             new BytesContent( Utils.newEmptyJarContent() ) );
                    }
                }
                catch ( IOException e )
                {
                    if ( log != null )
                    {
                        log.warn( "Could not read from " + file, e );
                    }
                }
                catch ( XmlPullParserException e )
                {
                    if ( log != null )
                    {
                        log.warn( "Could not parse " + file, e );
                    }
                }
            }
        }
    }

    public synchronized Set getGroupIds( String parentGroupId )
    {
        TreeSet result = new TreeSet();
        if ( StringUtils.isEmpty( parentGroupId ) )
        {
            for ( Iterator iterator = contents.keySet().iterator(); iterator.hasNext(); )
            {
                String groupId = (String) iterator.next();
                int index = groupId.indexOf( '.' );
                result.add( index == -1 ? groupId : groupId.substring( 0, index ) );
            }
        }
        else
        {
            String prefix = parentGroupId + '.';
            int start = prefix.length();
            for ( Iterator iterator = contents.keySet().iterator(); iterator.hasNext(); )
            {
                String groupId = (String) iterator.next();
                if ( groupId.startsWith( prefix ) )
                {
                    int index = groupId.indexOf( '.', start );
                    result.add( index == -1 ? groupId.substring( start ) : groupId.substring( start, index ) );
                }
            }
        }
        return result;
    }

    public synchronized Set getArtifactIds( String groupId )
    {
        Map artifactMap = (Map) contents.get( groupId );
        return new TreeSet( artifactMap == null ? Collections.EMPTY_SET : artifactMap.keySet() );
    }

    public synchronized Set getVersions( String groupId, String artifactId )
    {
        Map artifactMap = (Map) contents.get( groupId );
        Map versionMap = (Map) ( artifactMap == null ? null : artifactMap.get( artifactId ) );
        return new TreeSet( versionMap == null ? Collections.EMPTY_SET : versionMap.keySet() );
    }

    public synchronized Set getArtifacts( String groupId, String artifactId, String version )
    {
        Map artifactMap = (Map) contents.get( groupId );
        Map versionMap = (Map) ( artifactMap == null ? null : artifactMap.get( artifactId ) );
        Map filesMap = (Map) ( versionMap == null ? null : versionMap.get( version ) );

        return new HashSet( filesMap == null ? Collections.EMPTY_SET : filesMap.keySet() );
    }

    public synchronized long getLastModified( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        Map artifactMap = (Map) contents.get( artifact.getGroupId() );
        Map versionMap = (Map) ( artifactMap == null ? null : artifactMap.get( artifact.getArtifactId() ) );
        Map filesMap = (Map) ( versionMap == null ? null : versionMap.get( artifact.getVersion() ) );
        Content content = (Content) ( filesMap == null ? null : filesMap.get( artifact ) );
        if ( content == null )
        {
            if ( artifact.isSnapshot() && artifact.getTimestamp() == null && filesMap != null )
            {
                Artifact best = null;
                for ( Iterator i = filesMap.entrySet().iterator(); i.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) i.next();
                    Artifact a = (Artifact) entry.getKey();
                    if ( artifact.equalSnapshots( a ) && ( best == null || best.compareTo( a ) < 0 ) )
                    {
                        best = a;
                        content = (Content) entry.getValue();
                    }
                }
                if ( content == null )
                {
                    throw new ArtifactNotFoundException( artifact );

                }
            }
            else
            {
                throw new ArtifactNotFoundException( artifact );
            }
        }
        return content.getLastModified();
    }

    public synchronized long getSize( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        Map artifactMap = (Map) contents.get( artifact.getGroupId() );
        Map versionMap = (Map) ( artifactMap == null ? null : artifactMap.get( artifact.getArtifactId() ) );
        Map filesMap = (Map) ( versionMap == null ? null : versionMap.get( artifact.getVersion() ) );
        Content content = (Content) ( filesMap == null ? null : filesMap.get( artifact ) );
        if ( content == null )
        {
            if ( artifact.isSnapshot() && artifact.getTimestamp() == null && filesMap != null )
            {
                Artifact best = null;
                for ( Iterator i = filesMap.entrySet().iterator(); i.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) i.next();
                    Artifact a = (Artifact) entry.getKey();
                    if ( artifact.equalSnapshots( a ) && ( best == null || best.compareTo( a ) < 0 ) )
                    {
                        best = a;
                        content = (Content) entry.getValue();
                    }
                }
                if ( content == null )
                {
                    throw new ArtifactNotFoundException( artifact );

                }
            }
            else
            {
                throw new ArtifactNotFoundException( artifact );
            }
        }
        return content.getLength();
    }

    public synchronized InputStream get( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        Map artifactMap = (Map) contents.get( artifact.getGroupId() );
        Map versionMap = (Map) ( artifactMap == null ? null : artifactMap.get( artifact.getArtifactId() ) );
        Map filesMap = (Map) ( versionMap == null ? null : versionMap.get( artifact.getVersion() ) );
        Content content = (Content) ( filesMap == null ? null : filesMap.get( artifact ) );
        if ( content == null )
        {
            if ( artifact.isSnapshot() && artifact.getTimestamp() == null && filesMap != null )
            {
                Artifact best = null;
                for ( Iterator i = filesMap.entrySet().iterator(); i.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) i.next();
                    Artifact a = (Artifact) entry.getKey();
                    if ( artifact.equalSnapshots( a ) && ( best == null || best.compareTo( a ) < 0 ) )
                    {
                        best = a;
                        content = (Content) entry.getValue();
                    }
                }
                if ( content == null )
                {
                    throw new ArtifactNotFoundException( artifact );

                }
            }
            else
            {
                throw new ArtifactNotFoundException( artifact );
            }
        }
        return content.getInputStream();
    }

    public synchronized void set( Artifact artifact, InputStream content )
        throws IOException
    {
        try
        {
            set( artifact, new BytesContent( IOUtils.toByteArray( content ) ) );
        }
        finally
        {
            IOUtils.closeQuietly( content );
        }
    }

    public synchronized void set( Artifact artifact, Content content )
        throws IOException
    {
        Map artifactMap = (Map) contents.get( artifact.getGroupId() );
        if ( artifactMap == null )
        {
            artifactMap = new HashMap();
            contents.put( artifact.getGroupId(), artifactMap );
        }
        Map versionMap = (Map) artifactMap.get( artifact.getArtifactId() );
        if ( versionMap == null )
        {
            versionMap = new HashMap();
            artifactMap.put( artifact.getArtifactId(), versionMap );
        }
        Map filesMap = (Map) versionMap.get( artifact.getVersion() );
        if ( filesMap == null )
        {
            filesMap = new HashMap();
            versionMap.put( artifact.getVersion(), filesMap );
        }
        filesMap.put( artifact, content );
    }

    public synchronized Metadata getMetadata( String path )
        throws IOException, MetadataNotFoundException
    {
        Metadata metadata = new Metadata();
        boolean foundMetadata = false;
        path = StringUtils.stripEnd( StringUtils.stripStart( path, "/" ), "/" );
        String groupId = path.replace( '/', '.' );
        Set pluginArtifactIds = getArtifactIds( groupId );
        if ( pluginArtifactIds != null )
        {
            List plugins = new ArrayList();
            for ( Iterator i = pluginArtifactIds.iterator(); i.hasNext(); )
            {
                String artifactId = (String) i.next();
                Set pluginVersions = getVersions( groupId, artifactId );
                if ( pluginVersions == null || pluginVersions.isEmpty() )
                {
                    continue;
                }
                String[] versions = (String[]) pluginVersions.toArray( new String[pluginVersions.size()] );
                Arrays.sort( versions, new VersionComparator() );
                MavenXpp3Reader reader = new MavenXpp3Reader();
                for ( int j = versions.length - 1; j >= 0; j-- )
                {
                    InputStream inputStream = null;
                    try
                    {
                        inputStream = get( new Artifact( groupId, artifactId, versions[j], "pom" ) );
                        Model model = reader.read( new XmlStreamReader( inputStream ) );
                        if ( model == null || !"maven-plugin".equals( model.getPackaging() ) )
                        {
                            continue;
                        }
                        Plugin plugin = new Plugin();
                        plugin.setArtifactId( artifactId );
                        plugin.setName( model.getName() );
                        // TODO proper goal-prefix determination
                        // ugh! this is incredibly hacky and does not handle some fool that sets the goal prefix in
                        // a parent pom... ok unlikely, but stupid is as stupid does
                        boolean havePrefix = false;
                        final Build build = model.getBuild();
                        if ( build != null && build.getPlugins() != null )
                        {
                            havePrefix = setPluginGoalPrefixFromConfiguration( plugin, build.getPlugins() );
                        }
                        if ( !havePrefix && build != null && build.getPluginManagement() != null
                            && build.getPluginManagement().getPlugins() != null )
                        {
                            havePrefix = setPluginGoalPrefixFromConfiguration( plugin,
                                                                               build.getPluginManagement().getPlugins() );
                        }
                        if ( !havePrefix && artifactId.startsWith( "maven-" ) && artifactId.endsWith( "-plugin" ) )
                        {
                            plugin.setPrefix(
                                StringUtils.removeStart( StringUtils.removeEnd( artifactId, "-plugin" ), "maven-" ) );
                            havePrefix = true;
                        }
                        if ( !havePrefix && artifactId.endsWith( "-maven-plugin" ) )
                        {
                            plugin.setPrefix( StringUtils.removeEnd( artifactId, "-maven-plugin" ) );
                            havePrefix = true;
                        }
                        if ( !havePrefix )
                        {
                            plugin.setPrefix( artifactId );
                        }
                        plugins.add( plugin );
                        foundMetadata = true;
                        break;
                    }
                    catch ( ArtifactNotFoundException e )
                    {
                        // ignore
                    }
                    catch ( XmlPullParserException e )
                    {
                        // ignore
                    }
                    finally
                    {
                        IOUtils.closeQuietly( inputStream );
                    }
                }
            }
            if ( !plugins.isEmpty() )
            {
                metadata.setPlugins( plugins );
            }
        }
        int index = path.lastIndexOf( '/' );
        groupId = ( index == -1 ? groupId : groupId.substring( 0, index ) ).replace( '/', '.' );
        String artifactId = ( index == -1 ? null : path.substring( index + 1 ) );
        if ( artifactId != null )
        {
            Set artifactVersions = getVersions( groupId, artifactId );
            if ( artifactVersions != null && !artifactVersions.isEmpty() )
            {
                metadata.setGroupId( groupId );
                metadata.setArtifactId( artifactId );
                Versioning versioning = new Versioning();
                List versions = new ArrayList( artifactVersions );
                Collections.sort( versions, new VersionComparator() ); // sort the Maven way
                long lastUpdated = 0;
                for ( Iterator i = versions.iterator(); i.hasNext(); )
                {
                    String version = (String) i.next();
                    try
                    {
                        long lastModified = getLastModified( new Artifact( groupId, artifactId, version, "pom" ) );
                        versioning.addVersion( version );
                        if ( lastModified >= lastUpdated )
                        {
                            lastUpdated = lastModified;
                            versioning.setLastUpdatedTimestamp( new Date( lastModified ) );
                            versioning.setLatest( version );
                            if ( !version.endsWith( "-SNAPSHOT" ) )
                            {
                                versioning.setRelease( version );
                            }
                        }
                    }
                    catch ( ArtifactNotFoundException e )
                    {
                        // ignore
                    }
                }
                metadata.setVersioning( versioning );
                foundMetadata = true;
            }
        }

        int index2 = index == -1 ? -1 : path.lastIndexOf( '/', index - 1 );
        groupId = index2 == -1 ? groupId : groupId.substring( 0, index2 ).replace( '/', '.' );
        artifactId = index2 == -1 ? artifactId : path.substring( index2 + 1, index );
        String version = index2 == -1 ? null : path.substring( index + 1 );
        if ( version != null && version.endsWith( "-SNAPSHOT" ) )
        {
            Map artifactMap = (Map) contents.get( groupId );
            Map versionMap = (Map) ( artifactMap == null ? null : artifactMap.get( artifactId ) );
            Map filesMap = (Map) ( versionMap == null ? null : versionMap.get( version ) );
            if ( filesMap != null )
            {
                List snapshotVersions = new ArrayList();
                int maxBuildNumber = 0;
                long lastUpdated = 0;
                String timestamp = null;
                for ( Iterator i = filesMap.entrySet().iterator(); i.hasNext(); )
                {
                    final Map.Entry entry = (Map.Entry) i.next();
                    final Artifact artifact = (Artifact) entry.getKey();
                    final Content content = (Content) entry.getValue();
                    SnapshotVersion snapshotVersion = new SnapshotVersion();
                    snapshotVersion.setExtension( artifact.getType() );
                    snapshotVersion.setClassifier( artifact.getClassifier() == null ? "" : artifact.getClassifier() );
                    snapshotVersion.setVersion( artifact.getTimestampVersion() );
                    SimpleDateFormat fmt = new SimpleDateFormat( "yyyyMMddHHmmss" );
                    fmt.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
                    snapshotVersion.setUpdated( fmt.format( new Date( content.getLastModified() ) ) );
                    snapshotVersions.add( snapshotVersion );
                    if ( "pom".equals( artifact.getType() ) )
                    {
                        if ( artifact.getBuildNumber() != null
                            && maxBuildNumber < artifact.getBuildNumber().intValue() )
                        {
                            maxBuildNumber = artifact.getBuildNumber().intValue();
                            timestamp = artifact.getTimestampString();
                            lastUpdated = Math.max( lastUpdated, content.getLastModified() );
                        }
                    }
                }

                if ( !snapshotVersions.isEmpty() )
                {
                    Versioning versioning = metadata.getVersioning();
                    if ( versioning == null )
                    {
                        versioning = new Versioning();
                    }
                    metadata.setGroupId( groupId );
                    metadata.setArtifactId( artifactId );
                    metadata.setVersion( version );
                    for ( Iterator i = snapshotVersions.iterator(); i.hasNext(); )
                    {
                        SnapshotVersion snapshotVersion = (SnapshotVersion) i.next();
                        versioning.addSnapshotVersion( snapshotVersion );
                    }
                    if ( maxBuildNumber > 0 )
                    {
                        Snapshot snapshot = new Snapshot();
                        snapshot.setBuildNumber( maxBuildNumber );
                        snapshot.setTimestamp( timestamp );
                        versioning.setSnapshot( snapshot );
                    }
                    versioning.setLastUpdatedTimestamp( new Date( lastUpdated ) );
                    metadata.setVersioning( versioning );
                    foundMetadata = true;
                }
            }

        }
        if ( !foundMetadata )
        {
            throw new MetadataNotFoundException( path );
        }
        return metadata;
    }

    public synchronized long getMetadataLastModified( String path )
        throws IOException, MetadataNotFoundException
    {
        boolean haveResult = false;
        long result = 0;
        path = StringUtils.stripEnd( StringUtils.stripStart( path, "/" ), "/" );
        String groupId = path.replace( '/', '.' );
        Map artifactMap = (Map) contents.get( groupId );
        if ( artifactMap != null )
        {
            for ( Iterator i = artifactMap.values().iterator(); i.hasNext(); )
            {
                Map versionMap = (Map) i.next();
                for ( Iterator j = versionMap.values().iterator(); j.hasNext(); )
                {
                    Map filesMap = (Map) j.next();
                    for ( Iterator k = filesMap.values().iterator(); k.hasNext(); )
                    {
                        Content content = (Content) k.next();
                        haveResult = true;
                        result = Math.max( result, content.getLastModified() );
                    }
                }
            }
        }
        int index = path.lastIndexOf( '/' );
        groupId = index == -1 ? groupId : groupId.substring( 0, index ).replace( '/', '.' );
        String artifactId = ( index == -1 ? null : path.substring( index + 1 ) );
        if ( artifactId != null )
        {
            artifactMap = (Map) contents.get( groupId );
            Map versionMap = (Map) ( artifactMap == null ? null : artifactMap.get( artifactId ) );
            if ( versionMap != null )
            {
                for ( Iterator j = versionMap.values().iterator(); j.hasNext(); )
                {
                    Map filesMap = (Map) j.next();
                    for ( Iterator k = filesMap.values().iterator(); k.hasNext(); )
                    {
                        Content content = (Content) k.next();
                        haveResult = true;
                        result = Math.max( result, content.getLastModified() );
                    }
                }
            }
        }
        int index2 = index == -1 ? -1 : path.lastIndexOf( '/', index - 1 );
        groupId = index2 == -1 ? groupId : groupId.substring( 0, index2 ).replace( '/', '.' );
        artifactId = index2 == -1 ? artifactId : path.substring( index2 + 1, index );
        String version = index2 == -1 ? null : path.substring( index + 1 );
        if ( version != null && version.endsWith( "-SNAPSHOT" ) )
        {
            artifactMap = (Map) contents.get( groupId );
            Map versionMap = (Map) ( artifactMap == null ? null : artifactMap.get( artifactId ) );
            Map filesMap = (Map) ( versionMap == null ? null : versionMap.get( version ) );
            if ( filesMap != null )
            {
                for ( Iterator k = filesMap.values().iterator(); k.hasNext(); )
                {
                    Content content = (Content) k.next();
                    haveResult = true;
                    result = Math.max( result, content.getLastModified() );
                }
            }
        }
        if ( haveResult )
        {
            return result;
        }
        throw new MetadataNotFoundException( path );
    }

    private boolean setPluginGoalPrefixFromConfiguration( Plugin plugin, List pluginConfigs )
    {
        Iterator iterator = pluginConfigs.iterator();
        while ( iterator.hasNext() )
        {
            org.apache.maven.model.Plugin def = (org.apache.maven.model.Plugin) iterator.next();
            if ( ( def.getGroupId() == null || StringUtils.equals( "org.apache.maven.plugins", def.getGroupId() ) )
                && StringUtils.equals( "maven-plugin-plugin", def.getArtifactId() ) )
            {
                Xpp3Dom configuration = (Xpp3Dom) def.getConfiguration();
                if ( configuration != null )
                {
                    final Xpp3Dom goalPrefix = configuration.getChild( "goalPrefix" );
                    if ( goalPrefix != null )
                    {
                        plugin.setPrefix( goalPrefix.getValue() );
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    private static class VersionComparator
        implements Comparator
    {
        public int compare( Object o1, Object o2 )
        {
            ArtifactVersion v1 = new DefaultArtifactVersion( (String) o1 );
            ArtifactVersion v2 = new DefaultArtifactVersion( (String) o2 );
            return v1.compareTo( v2 );
        }
    }

    private static interface Content
        extends Serializable
    {

        long getLastModified();

        InputStream getInputStream()
            throws FileNotFoundException;

        long getLength();
    }

    private static class BytesContent
        implements Content
    {

        private static final long serialVersionUID = 1L;

        private final long lastModified;

        private final byte[] bytes;

        private BytesContent( byte[] bytes )
        {
            this.lastModified = System.currentTimeMillis();
            this.bytes = bytes;
        }

        public long getLastModified()
        {
            return lastModified;
        }

        public InputStream getInputStream()
        {
            return new ByteArrayInputStream( bytes );
        }

        public long getLength()
        {
            return bytes.length;
        }
    }

    private static class FileContent
        implements Content
    {

        private static final long serialVersionUID = 1L;

        private final File file;

        private FileContent( File file )
        {
            this.file = file;
        }

        public long getLastModified()
        {
            return file.lastModified();
        }

        public InputStream getInputStream()
            throws FileNotFoundException
        {
            return new FileInputStream( file );
        }

        public long getLength()
        {
            return file.length();
        }
    }
}
