package org.codehaus.mojo.mrm.maven;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collections;

import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class ProxyArtifactStoreTest
{

    @Test( expected = ArtifactNotFoundException.class )
    public void verifyArtifactNotFoundExceptionOnGet()
        throws Exception
    {
        ArtifactFactory artifactFactory = mock( ArtifactFactory.class );
        ArtifactResolver artifactResolver = mock( ArtifactResolver.class );
        ProxyArtifactStore store =
            new ProxyArtifactStore( null, Collections.emptyList(),
                                    Collections.emptyList(), null, artifactFactory,
                                    artifactResolver, null, null );

        doThrow( org.apache.maven.artifact.resolver.ArtifactNotFoundException.class )
            .when( artifactResolver )
            .resolve( isNull(), eq( Collections.emptyList() ), any() );

        Artifact artifact = new Artifact( "localhost", "test", "1.0-SNAPSHOT", "pom" );
        store.get( artifact );
    }

    @Test( expected = RuntimeException.class )
    public void verifyArtifactResolutionExceptionOnGet()
        throws Exception
    {
        ArtifactFactory artifactFactory = mock( ArtifactFactory.class );
        ArtifactResolver artifactResolver = mock( ArtifactResolver.class );
        ProxyArtifactStore store =
            new ProxyArtifactStore( null, Collections.emptyList(),
                                    Collections.emptyList(), null, artifactFactory,
                                    artifactResolver, null, null );

        doThrow( RuntimeException.class )
            .when( artifactResolver )
            .resolve( isNull(), eq( Collections.emptyList() ), any() );

        Artifact artifact = new Artifact( "localhost", "test", "1.0-SNAPSHOT", "pom" );
        store.get( artifact );
    }

    @Test( expected = RuntimeException.class )
    public void verifyArchetypeCatalogNotFoundException()
        throws Exception
    {
        ArchetypeManager archetypeManager = mock( ArchetypeManager.class );
        ProxyArtifactStore store =
            new ProxyArtifactStore( null, Collections.emptyList(),
                                    Collections.emptyList(), null, null, null, archetypeManager,
                                    null );
        doThrow( RuntimeException.class ).when( archetypeManager ).getDefaultLocalCatalog();
        store.getArchetypeCatalog();
    }

}
