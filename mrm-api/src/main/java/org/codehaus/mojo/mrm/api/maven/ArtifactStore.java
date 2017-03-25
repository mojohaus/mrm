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

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.metadata.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Set;

/**
 * An artifact store holds Maven {@link Artifact}s and can provide {@link Metadata} about the artifacts that it holds.
 *
 * @since 1.0
 */
public interface ArtifactStore
    extends Serializable
{

    /**
     * Returns the set of groupIds that begin with the specified prefix. Some implementations may be lazy caching
     * implementations, in which case it is permitted to return either the empty set, or only those entries which
     * have been loaded into the cache, so consumers should not assume that a missing entry implies non-existence.
     * The only way to be sure that an artifact does not exist is to call the {@link #get(Artifact)} method.
     * </p>
     * If there are known to be groupIds: org.codehaus.mojo, org.apache.maven and commons-io then
     * <pre>
     * assertEquals(new HashSet&lt;String>(Arrays.asList("commons-io","org")), getGroupIds("")); // Query root level
     * assertEquals(new HashSet&lt;String>(Arrays.asList("org.codehaus", "org.apache")), getGroupIds("org")); // query with a prefix
     * assertEquals(new HashSet&lt;String>(Arrays.asList("org.codehaus.mojo")), getGroupIds("org.codehaus"));
     * </pre>
     * Note that while the existence of groupId <code>org.codehaus.mojo</code> implies that there must be groupIds
     * <code>org</code> and <code>org.codehaus</code> there is no requirement that an implementation should
     * report these inferred parent groupIds, it is just strongly encouraged.
     * </p>
     * Note that where an implementation cannot differentiate between artifactIds, child groupIds and perhaps even
     * versions, it is permitted to return all three.
     *
     * @param parentGroupId The prefix to query or the empty string to query the root, cannot be <code>null</code>.
     * @return A set (with all elements of type {@link String}) of groupIds that are known to have
     *         {@code parentGroupId} as their prefix. All returned elements must start with {@code parentGroupId} and must have
     *         one and only one additional segment.
     * @since 1.0
     */
    Set<String> getGroupIds( String parentGroupId );

    /**
     * Returns the set of artifactIds that belong in the specified groupId. Some implementations may be lazy caching
     * implementations, in which case it is permitted to return either the empty set, or only those entries which
     * have been loaded into the cache, so consumers should not assume that a missing entry implies non-existence.
     * The only way to be sure that an artifact does not exist is to call the {@link #get(Artifact)} method.
     * </p>
     * Note that where an implementation cannot differentiate between artifactIds, child groupIds and perhaps even
     * versions, it is permitted to return all three.
     *
     * @param groupId The groupId to query cannot be empty or <code>null</code>.
     * @return A set (with all elements of type {@link String}) of artifactIds that are known to belong to
     *         {@code groupId}.
     * @since 1.0
     */
    Set<String> getArtifactIds( String groupId );

    /**
     * Returns the set of versions of the specified groupId:artifactId. Some implementations may be lazy caching
     * implementations, in which case it is permitted to return either the empty set, or only those entries which
     * have been loaded into the cache, so consumers should not assume that a missing entry implies non-existence.
     * The only way to be sure that an artifact does not exist is to call the {@link #get(Artifact)} method.
     * </p>
     * Note that where an implementation cannot differentiate between artifactIds, child groupIds and perhaps even
     * versions, it is permitted to return all three.
     *
     * @param groupId    The groupId to query cannot be empty or <code>null</code>.
     * @param artifactId The artifactId to query cannot be empty or <code>null</code>.
     * @return A set (with all elements of type {@link String}) of versions that are known to exist for
     *         {@code groupId:artifactId}.
     * @since 1.0
     */
    Set<String> getVersions( String groupId, String artifactId );

    /**
     * Returns the set of artifacts at the specified groupId:artifactId:version. Some implementations may be lazy caching
     * implementations, in which case it is permitted to return either the empty set, or only those entries which
     * have been loaded into the cache, so consumers should not assume that a missing entry implies non-existence.
     * The only way to be sure that an artifact does not exist is to call the {@link #get(Artifact)} method.
     *
     * @param groupId    The groupId to query cannot be empty or <code>null</code>.
     * @param artifactId The artifactId to query cannot be empty or <code>null</code>.
     * @param version    The version to query cannot be empty or <code>null</code>.
     * @return A set (with all elements of type {@link Artifact} of artifacts that are known to exist of the
     *         specified {@code groupId:artifactId:version}.
     * @since 1.0
     */
    Set<Artifact> getArtifacts( String groupId, String artifactId, String version );

    /**
     * Returns the time that the specified artifact was last modified.
     *
     * @param artifact the artifact.
     * @return A <code>long</code> value representing the time the file was
     *         last modified, measured in milliseconds since the epoch
     *         (00:00:00 GMT, January 1, 1970), or <code>0L</code> if the
     *         artifact might not exist (where the artifact is known to not exist an
     *         {@link ArtifactNotFoundException} must be thrown.
     * @throws IOException               if an I/O error occurs.
     * @throws ArtifactNotFoundException if the artifact definitely does not exist.
     * @since 1.0
     */
    long getLastModified( Artifact artifact )
        throws IOException, ArtifactNotFoundException;

    /**
     * Returns the size in bytes of the specified artifact.
     *
     * @param artifact the artifact.
     * @return the length of the artifact in bytes or <code>-1L</code> if the length cannot be determined.
     * @throws IOException               if an I/O error occurs.
     * @throws ArtifactNotFoundException if the artifact definitely does not exist.
     * @since 1.0
     */
    long getSize( Artifact artifact )
        throws IOException, ArtifactNotFoundException;

    /**
     * Retrieves the the artifact as an {@link InputStream}. The caller is responsible for closing the stream.
     *
     * @param artifact the artifact.
     * @return the contents of the artifact (caller is responsible for closing).
     * @throws IOException               if the artifact could not be retrieved.
     * @throws ArtifactNotFoundException if the artifact does not exist.
     * @since 1.0
     */
    InputStream get( Artifact artifact )
        throws IOException, ArtifactNotFoundException;

    /**
     * Create/update the specified artifact. This is an optional method for implementers.
     *
     * @param artifact the artifact to create/update.
     * @param content  the stream of contents (implementer is responsible for closing).
     * @throws IOException                   if the content could not be read/written.
     * @throws UnsupportedOperationException if the implementation is a read-only implementation.
     * @since 1.0
     */
    void set( Artifact artifact, InputStream content )
        throws IOException;

    /**
     * Returns the specified metadata.
     *
     * @param path of the metadata (should not include the <code>maven-metadata.xml</code>.
     * @return the metadata, never <code>null</code>.
     * @throws IOException               if an I/O error occurs.
     * @throws MetadataNotFoundException if the metadata does not exist.
     * @since 1.0
     */
    Metadata getMetadata( String path )
        throws IOException, MetadataNotFoundException;

    /**
     * Create/update the specified metadata.
     * 
     * @param path of the metadata (should not include the <code>maven-metadata.xml</code>.
     * @param content the metadata, never <code>null</code>.
     * @throws IOException if an I/O error occurs.
     * @since 1.1.0
     */
    void setMetadata( String path, Metadata content ) throws IOException;

    /**
     * Returns the time that the specified metadata was last modified.
     *
     * @param path of the metadata (should not include the <code>maven-metadata.xml</code>.
     * @return A <code>long</code> value representing the time the file was
     *         last modified, measured in milliseconds since the epoch
     *         (00:00:00 GMT, January 1, 1970), or <code>0L</code> if the
     *         metadata might not exist (where the metadtat is known to not exist a
     *         {@link MetadataNotFoundException} must be thrown.
     * @throws IOException               if an I/O error occurs.
     * @throws MetadataNotFoundException if the metadata definitely does not exist.
     * @since 1.0
     */
    long getMetadataLastModified( String path )
        throws IOException, MetadataNotFoundException;
    
    /**
     * 
     * @return 
     * @throws IOException                       if an I/O error occurs.
     * @throws ArchetypeCatalogNotFoundException if the archetypeCatalog does not exist.
     * @since 1.0
     */
    ArchetypeCatalog getArchetypeCatalog() throws IOException, ArchetypeCatalogNotFoundException;
    
    /**
     * 
     * @return
     * @throws IOException                       if an I/O error occurs.
     * @throws ArchetypeCatalogNotFoundException if the archetypeCatalog does not exist.
     * @since 1.0
     */
    long getArchetypeCatalogLastModified() throws IOException, ArchetypeCatalogNotFoundException;

    /**
     * 
     * @param content
     * @throws IOException                       if an I/O error occurs.
     * @since 1.0
     */
    void setArchetypeCatalog( InputStream content ) throws IOException;

}
