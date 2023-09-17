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
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.mrm.api.maven.Artifact;
import org.codehaus.mojo.mrm.api.maven.ArtifactNotFoundException;
import org.codehaus.mojo.mrm.plugin.FactoryHelper;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProxyArtifactStoreTest {

    private MavenSession mavenSession;

    @BeforeEach
    void setUp() {
        mavenSession = mock(MavenSession.class);
        when(mavenSession.getCurrentProject()).thenReturn(new MavenProject() {
            {
                setRemoteArtifactRepositories(Collections.emptyList());
                setPluginArtifactRepositories(Collections.emptyList());
            }
        });
        when(mavenSession.getRepositorySession()).thenReturn(new DefaultRepositorySystemSession());
    }

    @Test
    void verifyArtifactNotFoundExceptionOnGet() throws Exception {
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        doThrow(ArtifactResolutionException.class).when(repositorySystem).resolveArtifact(any(), any());
        FactoryHelper factoryHelper = mock(FactoryHelper.class);
        when(factoryHelper.getRepositorySystem()).thenReturn(repositorySystem);
        when(factoryHelper.getRepositoryMetadataManager()).then(i -> mock(RepositoryMetadataManager.class));
        when(factoryHelper.getArchetypeManager()).then(i -> mock(ArchetypeManager.class));

        ProxyArtifactStore store = new ProxyArtifactStore(factoryHelper, mavenSession, null);

        assertThrowsExactly(
                ArtifactNotFoundException.class,
                () -> store.get(new Artifact("localhost", "test", "1.0-SNAPSHOT", "pom")));
    }

    @Test
    void verifyArtifactResolutionExceptionOnGet() throws Exception {
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        doThrow(new RuntimeException("test123")).when(repositorySystem).resolveArtifact(any(), any());
        FactoryHelper factoryHelper = mock(FactoryHelper.class);
        when(factoryHelper.getRepositorySystem()).thenReturn(repositorySystem);
        when(factoryHelper.getRepositoryMetadataManager()).then(i -> mock(RepositoryMetadataManager.class));
        when(factoryHelper.getArchetypeManager()).then(i -> mock(ArchetypeManager.class));

        ProxyArtifactStore store = new ProxyArtifactStore(factoryHelper, mavenSession, null);

        RuntimeException exception = assertThrowsExactly(
                RuntimeException.class, () -> store.get(new Artifact("localhost", "test", "1.0-SNAPSHOT", "pom")));
        assertEquals("test123", exception.getMessage());
    }

    @Test
    void verifyArchetypeCatalogNotFoundException() throws Exception {
        ArchetypeManager archetypeManager = mock(ArchetypeManager.class);
        doThrow(new RuntimeException("test123")).when(archetypeManager).getLocalCatalog(any());
        FactoryHelper factoryHelper = mock(FactoryHelper.class);
        when(factoryHelper.getRepositorySystem()).then(i -> mock(RepositorySystem.class));
        when(factoryHelper.getRepositoryMetadataManager()).then(i -> mock(RepositoryMetadataManager.class));
        when(factoryHelper.getArchetypeManager()).thenReturn(archetypeManager);
        ProxyArtifactStore store = new ProxyArtifactStore(factoryHelper, mavenSession, null);

        RuntimeException exception = assertThrowsExactly(RuntimeException.class, store::getArchetypeCatalog);
        assertEquals("test123", exception.getMessage());
    }
}
