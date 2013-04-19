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
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.api.maven.BaseArtifactStore;
import org.codehaus.mojo.mrm.api.maven.MetadataNotFoundException;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

/**
 * An artifact store that keeps all its artifacts in memory.
 *
 * @since 1.0
 */
public class MemoryArtifactStore
    extends BaseArtifactStore
    implements Serializable
{
    /**
     * Ensure consistent serialization.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * The contents of this artifact store.
     *
     * @since 1.0
     */
    private Map<String, Map<String, Map<String, Map<Artifact, Content>>>> contents =
        new HashMap<String, Map<String, Map<String, Map<Artifact, Content>>>>();

    /**
     * {@inheritDoc}
     */
    public synchronized Set<String> getGroupIds( String parentGroupId )
    {
        Set<String> result = new TreeSet<String>();
        if ( StringUtils.isEmpty( parentGroupId ) )
        {
            for ( String groupId : contents.keySet() )
            {
                int index = groupId.indexOf( '.' );
                result.add( index == -1 ? groupId : groupId.substring( 0, index ) );
            }
        }
        else
        {
            String prefix = parentGroupId + '.';
            int start = prefix.length();
            for ( String groupId : contents.keySet() )
            {
                if ( groupId.startsWith( prefix ) )
                {
                    int index = groupId.indexOf( '.', start );
                    result.add( index == -1 ? groupId.substring( start ) : groupId.substring( start, index ) );
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Set<String> getArtifactIds( String groupId )
    {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get( groupId );
        return new TreeSet<String>( artifactMap == null ? Collections.<String> emptySet() : artifactMap.keySet() );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Set<String> getVersions( String groupId, String artifactId )
    {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get( groupId );
        Map<String, Map<Artifact, Content>> versionMap = ( artifactMap == null ? null : artifactMap.get( artifactId ) );
        return new TreeSet<String>( versionMap == null ? Collections.<String> emptySet() : versionMap.keySet() );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Set<Artifact> getArtifacts( String groupId, String artifactId, String version )
    {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get( groupId );
        Map<String, Map<Artifact, Content>> versionMap = ( artifactMap == null ? null : artifactMap.get( artifactId ) );
        Map<Artifact, Content> filesMap = ( versionMap == null ? null : versionMap.get( version ) );

        return new HashSet<Artifact>( filesMap == null ? Collections.<Artifact> emptySet() : filesMap.keySet() );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized long getLastModified( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get( artifact.getGroupId() );
        Map<String, Map<Artifact, Content>> versionMap = ( artifactMap == null ? null : artifactMap.get( artifact.getArtifactId() ) );
        Map<Artifact, Content> filesMap = ( versionMap == null ? null : versionMap.get( artifact.getVersion() ) );
        Content content = ( filesMap == null ? null : filesMap.get( artifact ) );
        if ( content == null )
        {
            if ( artifact.isSnapshot() && artifact.getTimestamp() == null && filesMap != null )
            {
                Artifact best = null;
                for ( Map.Entry<Artifact, Content> entry : filesMap.entrySet() )
                {
                    Artifact a = entry.getKey();
                    if ( artifact.equalSnapshots( a ) && ( best == null || best.compareTo( a ) < 0 ) )
                    {
                        best = a;
                        content = entry.getValue();
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

    /**
     * {@inheritDoc}
     */
    public synchronized long getSize( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get( artifact.getGroupId() );
        Map<String, Map<Artifact, Content>> versionMap = ( artifactMap == null ? null : artifactMap.get( artifact.getArtifactId() ) );
        Map<Artifact, Content> filesMap = ( versionMap == null ? null : versionMap.get( artifact.getVersion() ) );
        Content content = ( filesMap == null ? null : filesMap.get( artifact ) );
        if ( content == null )
        {
            if ( artifact.isSnapshot() && artifact.getTimestamp() == null && filesMap != null )
            {
                Artifact best = null;
                for ( Map.Entry<Artifact, Content> entry : filesMap.entrySet() )
                {
                    Artifact a = entry.getKey();
                    if ( artifact.equalSnapshots( a ) && ( best == null || best.compareTo( a ) < 0 ) )
                    {
                        best = a;
                        content = entry.getValue();
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
        return content.getBytes().length;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized InputStream get( Artifact artifact )
        throws IOException, ArtifactNotFoundException
    {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get( artifact.getGroupId() );
        Map<String, Map<Artifact, Content>> versionMap = ( artifactMap == null ? null : artifactMap.get( artifact.getArtifactId() ) );
        Map<Artifact, Content> filesMap = ( versionMap == null ? null : versionMap.get( artifact.getVersion() ) );
        Content content = ( filesMap == null ? null : filesMap.get( artifact ) );
        if ( content == null )
        {
            if ( artifact.isSnapshot() && artifact.getTimestamp() == null && filesMap != null )
            {
                Artifact best = null;
                for ( Map.Entry<Artifact, Content> entry : filesMap.entrySet() )
                {
                    Artifact a = entry.getKey();
                    if ( artifact.equalSnapshots( a ) && ( best == null || best.compareTo( a ) < 0 ) )
                    {
                        best = a;
                        content = entry.getValue();
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
        return new ByteArrayInputStream( content.getBytes() );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void set( Artifact artifact, InputStream content )
        throws IOException
    {
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get( artifact.getGroupId() );
        if ( artifactMap == null )
        {
            artifactMap = new HashMap<String, Map<String, Map<Artifact, Content>>>();
            contents.put( artifact.getGroupId(), artifactMap );
        }
        Map<String, Map<Artifact, Content>> versionMap = artifactMap.get( artifact.getArtifactId() );
        if ( versionMap == null )
        {
            versionMap = new HashMap<String, Map<Artifact, Content>>();
            artifactMap.put( artifact.getArtifactId(), versionMap );
        }
        Map<Artifact, Content> filesMap = versionMap.get( artifact.getVersion() );
        if ( filesMap == null )
        {
            filesMap = new HashMap<Artifact, Content>();
            versionMap.put( artifact.getVersion(), filesMap );
        }
        try
        {
            filesMap.put( artifact, new Content( IOUtils.toByteArray( content ) ) );
        }
        finally
        {
            IOUtils.closeQuietly( content );
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Metadata getMetadata( String path )
        throws IOException, MetadataNotFoundException
    {
        Metadata metadata = new Metadata();
        boolean foundMetadata = false;
        path = StringUtils.stripEnd( StringUtils.stripStart( path, "/" ), "/" );
        String groupId = path.replace( '/', '.' );
        Set<String> pluginArtifactIds = getArtifactIds( groupId );
        if ( pluginArtifactIds != null )
        {
            List<Plugin> plugins = new ArrayList<Plugin>();
            for ( String artifactId : pluginArtifactIds )
            {
                Set<String> pluginVersions = getVersions( groupId, artifactId );
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
            Set<String> artifactVersions = getVersions( groupId, artifactId );
            if ( artifactVersions != null && !artifactVersions.isEmpty() )
            {
                metadata.setGroupId( groupId );
                metadata.setArtifactId( artifactId );
                Versioning versioning = new Versioning();
                List<String> versions = new ArrayList<String>( artifactVersions );
                Collections.sort( versions, new VersionComparator() ); // sort the Maven way
                long lastUpdated = 0;
                for ( String version : versions )
                {
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
            Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get( groupId );
            Map<String, Map<Artifact, Content>> versionMap = ( artifactMap == null ? null : artifactMap.get( artifactId ) );
            Map<Artifact, Content> filesMap = ( versionMap == null ? null : versionMap.get( version ) );
            if ( filesMap != null )
            {
                List<SnapshotVersion> snapshotVersions = new ArrayList<SnapshotVersion>();
                int maxBuildNumber = 0;
                long lastUpdated = 0;
                String timestamp = null;
                boolean found = false;
                for ( final Map.Entry<Artifact, Content> entry : filesMap.entrySet() )
                {
                    final Artifact artifact = entry.getKey();
                    final Content content = entry.getValue();
                    SimpleDateFormat fmt = new SimpleDateFormat( "yyyyMMddHHmmss" );
                    fmt.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
                    String lastUpdatedTime = fmt.format( new Date( content.getLastModified() ) );
                    try
                    {
                        Maven3.addSnapshotVersion( snapshotVersions, artifact, lastUpdatedTime );
                    }
                    catch ( LinkageError e )
                    {
                        // Maven 2
                    }
                    if ( "pom".equals( artifact.getType() ) )
                    {
                        if ( artifact.getBuildNumber() != null
                            && maxBuildNumber < artifact.getBuildNumber().intValue() )
                        {
                            maxBuildNumber = artifact.getBuildNumber().intValue();
                            timestamp = artifact.getTimestampString();
                        }
                        else
                        {
                            maxBuildNumber = Math.max( 1, maxBuildNumber );
                        }
                        lastUpdated = Math.max( lastUpdated, content.getLastModified() );
                        found = true;
                    }
                }

                if ( !snapshotVersions.isEmpty() || found )
                {
                    Versioning versioning = metadata.getVersioning();
                    if ( versioning == null )
                    {
                        versioning = new Versioning();
                    }
                    metadata.setGroupId( groupId );
                    metadata.setArtifactId( artifactId );
                    metadata.setVersion( version );
                    try
                    {
                        Maven3.addSnapshotVersions( versioning, snapshotVersions );
                    }
                    catch ( LinkageError e )
                    {
                        // Maven 2
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

    /**
     * {@inheritDoc}
     */
    public synchronized long getMetadataLastModified( String path )
        throws IOException, MetadataNotFoundException
    {
        boolean haveResult = false;
        long result = 0;
        path = StringUtils.stripEnd( StringUtils.stripStart( path, "/" ), "/" );
        String groupId = path.replace( '/', '.' );
        Map<String, Map<String, Map<Artifact, Content>>> artifactMap = contents.get( groupId );
        if ( artifactMap != null )
        {
            for ( Map<String, Map<Artifact, Content>> versionMap : artifactMap.values() )
            {
                for ( Map<Artifact, Content> filesMap : versionMap.values() )
                {
                    for ( Content content : filesMap.values() )
                    {
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
            artifactMap = contents.get( groupId );
            Map<String, Map<Artifact, Content>> versionMap = ( artifactMap == null ? null : artifactMap.get( artifactId ) );
            if ( versionMap != null )
            {
                for ( Map<Artifact, Content> filesMap : versionMap.values() )
                {
                    for ( Content content : filesMap.values() )
                    {
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
            artifactMap = contents.get( groupId );
            Map<String, Map<Artifact, Content>> versionMap = ( artifactMap == null ? null : artifactMap.get( artifactId ) );
            Map<Artifact, Content> filesMap = ( versionMap == null ? null : versionMap.get( version ) );
            if ( filesMap != null )
            {
                for ( Content content : filesMap.values() )
                {
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

    /**
     * If the plugin configurations contain a reference to the <code>maven-plugin-plugin</code> and that contains
     * configuration of the <code>goalPrefix</code>, update the supplied plugin with that prefix.
     *
     * @param plugin        the plugin to update.
     * @param pluginConfigs the configurations of {@link org.apache.maven.model.Plugin} to search.
     * @return <code>true</code> if the prefix has been set.
     * @since 1.0
     */
    private boolean setPluginGoalPrefixFromConfiguration( Plugin plugin, List<org.apache.maven.model.Plugin> pluginConfigs )
    {
        for ( org.apache.maven.model.Plugin def : pluginConfigs )
        {
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

    /**
     * Compares two versions using Maven's version comparison rules.
     *
     * @since 1.0
     */
    private static class VersionComparator
        implements Comparator<String>
    {
        /**
         * {@inheritDoc}
         */
        public int compare( String o1, String o2 )
        {
            ArtifactVersion v1 = new DefaultArtifactVersion( o1 );
            ArtifactVersion v2 = new DefaultArtifactVersion( o2 );
            return v1.compareTo( v2 );
        }
    }

    /**
     * Holds the contents of an artifact.
     *
     * @since 1.0
     */
    private static class Content
        implements Serializable
    {

        /**
         * Ensure consistent serialization.
         *
         * @since 1.0
         */
        private static final long serialVersionUID = 1L;

        /**
         * The last modified timestamp.
         *
         * @since 1.0
         */
        private final long lastModified;

        /**
         * The actual content.
         *
         * @since 1.0
         */
        private final byte[] bytes;

        /**
         * Creates new content from the supplied content.
         *
         * @param bytes the content.
         * @since 1.0
         */
        private Content( byte[] bytes )
        {
            this.lastModified = System.currentTimeMillis();
            this.bytes = bytes;
        }

        /**
         * Returns the last modified timestamp.
         *
         * @return the last modified timestamp.
         * @since 1.0
         */
        public long getLastModified()
        {
            return lastModified;
        }

        /**
         * Returns the content.
         *
         * @return the content.
         * @since 1.0
         */
        public byte[] getBytes()
        {
            return bytes;
        }
    }

    /**
     * In order to allow the use of Maven 3 methods from a plugin running in Maven 2, we need to encapsulate all the
     * Maven 3 method signatures in a separate class so that we can catch the {@link LinkageError} that will be thrown
     * when the class is attempted to load. If we didn't do it this way then our class could not load either.
     *
     * @since 1.0
     */
    private static class Maven3
    {
        /**
         * Adds a snapshot version to the list of snapshot versions.
         *
         * @param snapshotVersions the list of snapshot versions.
         * @param artifact         the artifact to add details of.
         * @param lastUpdatedTime  the time to flag for last updated.
         * @since 1.0
         */
        private static void addSnapshotVersion( List<SnapshotVersion> snapshotVersions, Artifact artifact, String lastUpdatedTime )
        {
            try
            {
                SnapshotVersion snapshotVersion = new SnapshotVersion();
                snapshotVersion.setExtension( artifact.getType() );
                snapshotVersion.setClassifier( artifact.getClassifier() == null ? "" : artifact.getClassifier() );
                snapshotVersion.setVersion( artifact.getTimestampVersion() );
                snapshotVersion.setUpdated( lastUpdatedTime );
                snapshotVersions.add( snapshotVersion );
            }
            catch ( NoClassDefFoundError e )
            {
                // Maven 2
            }
        }

        /**
         * Add the list of {@link SnapshotVersion}s to the {@link Versioning}.
         *
         * @param versioning       the versionioning to add to.
         * @param snapshotVersions the snapshot versions to add.
         * @since 1.0
         */
        private static void addSnapshotVersions( Versioning versioning, List<SnapshotVersion> snapshotVersions )
        {
            try
            {
                for ( SnapshotVersion snapshotVersion : snapshotVersions )
                {
                    versioning.addSnapshotVersion( snapshotVersion );
                }
            }
            catch ( NoClassDefFoundError e )
            {
                // Maven 2
            }
        }
    }
}
