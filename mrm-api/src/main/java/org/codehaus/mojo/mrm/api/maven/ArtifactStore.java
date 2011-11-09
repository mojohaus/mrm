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

import org.apache.maven.artifact.repository.metadata.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public interface ArtifactStore
{

    Set/*<String>*/ getGroupIds( String prefix );

    Set/*<String>*/ getArtifactIds( String groupId );

    Set/*<String>*/ getVersions( String groupId, String artifactId );

    Set/*<Artifact>*/ getArtifacts( String groupId, String artifactId, String version );

    long getLastModified( Artifact artifact )
        throws IOException, ArtifactNotFoundException;

    long getSize( Artifact artifact )
        throws IOException, ArtifactNotFoundException;

    InputStream get( Artifact artifact )
        throws IOException, ArtifactNotFoundException;

    void set( Artifact artifact, InputStream content )
        throws IOException;

    Metadata getMetadata( String path )
        throws IOException, MetadataNotFoundException;

    long getMetadataLastModified( String path )
        throws IOException, MetadataNotFoundException;


}
